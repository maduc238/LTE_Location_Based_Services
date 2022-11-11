/* Copyright (C) 2015-2021 Open Information Security Foundation
 *
 * You can copy, redistribute or modify this Program under the terms of
 * the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

/*
 * TODO: Triển khai logic lớp ứng dụng của bạn với các unit tests.
 * TODO: Loại bỏ các câu lệnh SCLogNotice hoặc chuyển đổi để gỡ lỗi.
 */

/**
 * \file
 *
 * \author Ma Duc <mavietduc@gmail.com>
 *
 * Diameter application layer detector and parser for learning and
 * diameter purposes.
 *
 * This diameter implements a simple application layer for something
 * like the echo protocol running on port 7.
 */

#include "suricata-common.h"
#include "suricata.h"
#include "stream.h"
#include "conf.h"
#include "app-layer.h"
#include "app-layer-detect-proto.h"
#include "app-layer-parser.h"
#include "app-layer-diameter.h"

#include "util-unittest.h"
#include "util-validate.h"
#include "util-enum.h"

/* The default port to probe for echo traffic if not provided in the
 * configuration file. */
#define DIAMETER_DEFAULT_PORT "3868"

/* The minimum size for a message. For some protocols this might
 * be the size of a header. */
#define DIAMETER_MIN_FRAME_LEN 20

/**
 * Tổng hợp các event của lớp application cho protocol.
 * Thông thường, có thể xảy ra các event lỗi khi phân tích cú pháp
 * dữ liệu, như dữ liệu được nhận không mong muốn. Với Diameter,
 * chúng ta sẽ tạo ra một thứ nào đó và log lại alert lớp app-layer
 * nếu nhận được một bản tin trống
 * 
 * Ví dụ rule:
 * alert diameter any any -> any any (msg:"SURICATA Diameter empty message"; \
 *    app-layer-event:diameter.empty_message; sid:X; rev:Y;)
*/
enum {
    DIAMETER_DECODER_EVENT_EMPTY_MESSAGE,
};

SCEnumCharMap diameter_decoder_event_table[] = {
    {"EMPTY_MESSAGE", DIAMETER_DECODER_EVENT_EMPTY_MESSAGE},

    // event table must be NULL-terminated
    { NULL, -1 },
};

static DiameterTransaction *DiameterTxAlloc(DiameterState *state)
{
    DiameterTransaction *tx = SCCalloc(1, sizeof(DiameterTransaction));
    if (unlikely(tx == NULL)) {
        return NULL;
    }

    /* Increment the transaction ID on the state each time one is
     * allocated. */
    tx->tx_id = state->transaction_max++;

    TAILQ_INSERT_TAIL(&state->tx_list, tx, next);

    return tx;
}

static void DiameterTxFree(void *txv)
{
    DiameterTransaction *tx = txv;

    if (tx->request_buffer != NULL) {
        SCFree(tx->request_buffer);
    }

    if (tx->response_buffer != NULL) {
        SCFree(tx->response_buffer);
    }

    AppLayerDecoderEventsFreeEvents(&tx->tx_data.events);

    SCFree(tx);
}

static void *DiameterStateAlloc(void *orig_state, AppProto proto_orig)
{
    SCLogNotice("Allocating diameter state.");
    DiameterState *state = SCCalloc(1, sizeof(DiameterState));
    if (unlikely(state == NULL)) {
        return NULL;
    }
    TAILQ_INIT(&state->tx_list);
    return state;
}

static void DiameterStateFree(void *state)
{
    DiameterState *diameter_state = state;
    DiameterTransaction *tx;
    SCLogNotice("Freeing diameter state.");
    while ((tx = TAILQ_FIRST(&diameter_state->tx_list)) != NULL) {
        TAILQ_REMOVE(&diameter_state->tx_list, tx, next);
        DiameterTxFree(tx);
    }
    SCFree(diameter_state);
}

/**
 * \brief Callback lớp application để giải phóng (have a transaction freed).
 *
 * \param state a void pointer to the DiameterState object.
 * \param tx_id the transaction ID to free.
 */
static void DiameterStateTxFree(void *statev, uint64_t tx_id)
{
    DiameterState *state = statev;
    DiameterTransaction *tx = NULL, *ttx;

    SCLogNotice("Freeing transaction %"PRIu64, tx_id);

    TAILQ_FOREACH_SAFE(tx, &state->tx_list, next, ttx) {

        /* Continue if this is not the transaction we are looking
         * for. */
        if (tx->tx_id != tx_id) {
            continue;
        }

        /* Remove and free the transaction. */
        TAILQ_REMOVE(&state->tx_list, tx, next);
        DiameterTxFree(tx);
        return;
    }

    SCLogNotice("Transaction %"PRIu64" not found.", tx_id);
}

static int DiameterStateGetEventInfo(const char *event_name, int *event_id,
    AppLayerEventType *event_type)
{
    *event_id = SCMapEnumNameToValue(event_name, diameter_decoder_event_table);
    if (*event_id == -1) {
        SCLogError(SC_ERR_INVALID_ENUM_MAP, "event \"%s\" not present in "
                   "diameter enum map table.",  event_name);
        /* This should be treated as fatal. */
        return -1;
    }

    *event_type = APP_LAYER_EVENT_TYPE_TRANSACTION;

    return 0;
}

static int DiameterStateGetEventInfoById(int event_id, const char **event_name,
                                         AppLayerEventType *event_type)
{
    *event_name = SCMapEnumValueToName(event_id, diameter_decoder_event_table);
    if (*event_name == NULL) {
        SCLogError(SC_ERR_INVALID_ENUM_MAP, "event \"%d\" not present in "
                   "diameter enum map table.",  event_id);
        /* This should be treated as fatal. */
        return -1;
    }

    *event_type = APP_LAYER_EVENT_TYPE_TRANSACTION;

    return 0;
}

/**
 * \brief Khảo sát xem input đến server có giống Diameter không.
 *
 * \retval ALPROTO_DIAMETER nếu giống như Diameter,
 *     ALPROTO_FAILED, nếu rõ ràng không phải ALPROTO_DIAMETER,
 *     nếu không thì ALPROTO_UNKNOWN.
 */
static AppProto DiameterProbingParserTs(Flow *f, uint8_t direction,
        const uint8_t *input, uint32_t input_len, uint8_t *rdir)
{
    /* Nếu có input, đây là Diameter. */
    if (input_len >= DIAMETER_MIN_FRAME_LEN) {
        SCLogNotice("Detected as ALPROTO_DIAMETER.");
        return ALPROTO_DIAMETER;
    }

    SCLogNotice("Protocol not detected as ALPROTO_DIAMETER.");
    return ALPROTO_UNKNOWN;
}

/**
 * \brief Khảo sát xem input tới client xem có giống Diameter
 * hay không. DiameterProbingParserTs có thể được sử dụng thay
 * thế nếu giao thức là symmetric (là loại mã hóa trong đó chỉ
 * có một key bí mật được sử dụng vừa để mã hóa, vừa để giải)
 *
 * \retval ALPROTO_DIAMETER nếu giống như Diameter,
 *     ALPROTO_FAILED, nếu rõ ràng không phải ALPROTO_DIAMETER,
 *     nếu không thì ALPROTO_UNKNOWN.
 */
static AppProto DiameterProbingParserTc(Flow *f, uint8_t direction,
        const uint8_t *input, uint32_t input_len, uint8_t *rdir)
{
    /* Nếu có input, đây là Diameter. */
    if (input_len >= DIAMETER_MIN_FRAME_LEN) {
        SCLogNotice("Detected as ALPROTO_DIAMETER.");
        return ALPROTO_DIAMETER;
    }

    SCLogNotice("Protocol not detected as ALPROTO_DIAMETER.");
    return ALPROTO_UNKNOWN;
}

static AppLayerResult DiameterParseRequest(Flow *f, void *statev, AppLayerParserState *pstate,
        StreamSlice stream_slice, void *local_data)
{
    DiameterState *state = statev;
    const uint8_t *input = StreamSliceGetData(&stream_slice);
    uint32_t input_len = StreamSliceGetDataLen(&stream_slice);
    const uint8_t flags = StreamSliceGetFlags(&stream_slice);

    SCLogNotice("Parsing diameter request: len=%"PRIu32, input_len);

    if (input == NULL) {
        if (AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TS)) {
            /* This is a signal that the stream is done. Do any
             * cleanup if needed. Usually nothing is required here. */
            SCReturnStruct(APP_LAYER_OK);
        } else if (flags & STREAM_GAP) {
            /* This is a signal that there has been a gap in the
             * stream. This only needs to be handled if gaps were
             * enabled during protocol registration. The input_len
             * contains the size of the gap. */
            SCReturnStruct(APP_LAYER_OK);
        }
        /* This should not happen. If input is NULL, one of the above should be
         * true. */
        DEBUG_VALIDATE_BUG_ON(true);
        SCReturnStruct(APP_LAYER_ERROR);
    }

    /* Thông thường việc phân tích dữ liệu ở đây và lưu trữ nó trong
     * transaction object, nhưng vì đây là echo, chúng ta sẽ chỉ ghi
     * lại request data.
     */

    /* Ngoài ra, nếu protocol này có một "protocol data unit" được phân
     * ra nhiều chunk dữ liệu, điều này hay có trong TCP, bạn có thể cần
     * thực hiện vài thao tác buffering ở đây.
     * 
     * Để nó đơn giản, buffering được bỏ qua ở đây, nhưng ngay cả với
     * một echo protocol, chúng ta cos thể muốn buffer cho đến khi một 
     * dòng mới được tìm thấy, giả sử dựa trên text based của nó.
    */

    /* Allocate a transaction.
     *
     * Nhưng lưu ý rằng nếu một "protocol data unit" không được nhận
     * trong một chuck của data, và quá trình buffer được thực hiện
     * trên transaction, chúng ta có thể cần tìm transaction mà data
     * mới nhận này thuộc về.
     */
    DiameterTransaction *tx = DiameterTxAlloc(state);
    if (unlikely(tx == NULL)) {
        SCLogNotice("Failed to allocate new Diameter tx.");
        goto end;
    }
    SCLogNotice("Allocated Diameter tx %"PRIu64".", tx->tx_id);

    /* Make a copy of the request. */
    tx->request_buffer = SCCalloc(1, input_len);
    if (unlikely(tx->request_buffer == NULL)) {
        goto end;
    }
    memcpy(tx->request_buffer, input, input_len);
    tx->request_buffer_len = input_len;

    /* Here we check for an empty message and create an app-layer
     * event. */
    if ((input_len == 1 && tx->request_buffer[0] == '\n') ||
        (input_len == 2 && tx->request_buffer[0] == '\r')) {
        SCLogNotice("Creating event for empty message.");
        AppLayerDecoderEventsSetEventRaw(&tx->tx_data.events, DIAMETER_DECODER_EVENT_EMPTY_MESSAGE);
    }

end:
    SCReturnStruct(APP_LAYER_OK);
}

static AppLayerResult DiameterParseResponse(Flow *f, void *statev, AppLayerParserState *pstate,
        StreamSlice stream_slice, void *local_data)
{
    DiameterState *state = statev;
    DiameterTransaction *tx = NULL, *ttx;
    const uint8_t *input = StreamSliceGetData(&stream_slice);
    uint32_t input_len = StreamSliceGetDataLen(&stream_slice);

    SCLogNotice("Parsing Diameter response.");

    /* Likely connection closed, we can just return here. */
    if ((input == NULL || input_len == 0) &&
        AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TC)) {
        SCReturnStruct(APP_LAYER_OK);
    }

    /* Probably don't want to create a transaction in this case
     * either. */
    if (input == NULL || input_len == 0) {
        SCReturnStruct(APP_LAYER_OK);
    }

    /* Tra cứu transaction đang tồn tại cho response này. Trong trường
     * hợp echo, nó sẽ là transation gần đây nhất trên DiameterState object. */

    /* Chúng ta chỉ nên lấy transaction cuối cùng. */
    TAILQ_FOREACH(ttx, &state->tx_list, next) {
        tx = ttx;
    }

    if (tx == NULL) {
        SCLogNotice("Failed to find transaction for response on state %p.",
            state);
        goto end;
    }

    SCLogNotice("Found transaction %"PRIu64" for response on state %p.",
        tx->tx_id, state);

    /* Nếu protocol yêu cầu nhiều chunk của data để hoàn thành, bạn có thể
     * gặp phải trường hợp có response data đã tồn tại.
     *
     * In this case, chúng ta chỉ cần log lại rằng có data và free nó. Nhưng
     * có thể muốn phân bổ lại buffer và append the data.
     */
    if (tx->response_buffer != NULL) {
        SCLogNotice("WARNING: Transaction already has response data, "
            "existing data will be overwritten.");
        SCFree(tx->response_buffer);
    }

    /* Make a copy of the response. */
    tx->response_buffer = SCCalloc(1, input_len);
    if (unlikely(tx->response_buffer == NULL)) {
        goto end;
    }
    memcpy(tx->response_buffer, input, input_len);
    tx->response_buffer_len = input_len;

    /* Set the response_done flag for transaction state checking in
     * DiameterGetStateProgress(). */
    tx->response_done = 1;

end:
    SCReturnStruct(APP_LAYER_OK);
}

static uint64_t DiameterGetTxCnt(void *statev)
{
    const DiameterState *state = statev;
    SCLogNotice("Current tx count is %"PRIu64".", state->transaction_max);
    return state->transaction_max;
}

static void *DiameterGetTx(void *statev, uint64_t tx_id)
{
    DiameterState *state = statev;
    DiameterTransaction *tx;

    SCLogDebug("Requested tx ID %" PRIu64 ".", tx_id);

    TAILQ_FOREACH(tx, &state->tx_list, next) {
        if (tx->tx_id == tx_id) {
            SCLogDebug("Transaction %" PRIu64 " found, returning tx object %p.", tx_id, tx);
            return tx;
        }
    }

    SCLogDebug("Transaction ID %" PRIu64 " not found.", tx_id);
    return NULL;
}

/**
 * \brief Trả lại state của một transaction theo một direction nhất định.
 *
 * Trong trường hợp echo protocol, sừ tồn tại của một transaction nghĩa là
 * có request đã thực hiện xong. Tuy nhiên, một số protocols có thể cần nhiều
 * chunk data hơn để hoàn thành request, có thể cần nhiều hơn là sự tồn tại của
 * một transaction để request được coi là hoàn thành.
 *
 * Để response được coi là đã hoàn thành, cần phải xem response cho một request
 * flag response_done được đặt trên response để kiểm tra ở đây.
 */
static int DiameterGetStateProgress(void *txv, uint8_t direction)
{
    DiameterTransaction *tx = txv;

    SCLogNotice("Transaction progress requested for tx ID %"PRIu64
        ", direction=0x%02x", tx->tx_id, direction);

    if (direction & STREAM_TOCLIENT && tx->response_done) {
        return 1;
    }
    else if (direction & STREAM_TOSERVER) {
        /* Với Diameter, chỉ cần sự tồn tại của transaction nghĩa là request đã được thực hiện. */
        return 1;
    }

    return 0;
}

/**
 * \brief retrieve the tx data used for logging, config, detection
 */
static AppLayerTxData *DiameterGetTxData(void *vtx)
{
    DiameterTransaction *tx = vtx;
    return &tx->tx_data;
}

/**
 * \brief retrieve the state data
 */
static AppLayerStateData *DiameterGetStateData(void *vstate)
{
    DiameterState *state = vstate;
    return &state->state_data;
}

void RegisterDiameterParsers(void)
{
    const char *proto_name = "diameter";

    /* Check if Diameter TCP detection is enabled. If it does not exist in
     * the configuration file then it will be disabled by default. */
    if (AppLayerProtoDetectConfProtoDetectionEnabledDefault("tcp", proto_name, false)) {

        SCLogDebug("Diameter TCP protocol detection enabled.");

        AppLayerProtoDetectRegisterProtocol(ALPROTO_DIAMETER, proto_name);

        if (RunmodeIsUnittests()) {

            SCLogNotice("Unittest mode, registering default configuration.");
            AppLayerProtoDetectPPRegister(IPPROTO_TCP, DIAMETER_DEFAULT_PORT,
                ALPROTO_DIAMETER, 0, DIAMETER_MIN_FRAME_LEN, STREAM_TOSERVER,
                DiameterProbingParserTs, DiameterProbingParserTc);

        }
        else {

            if (!AppLayerProtoDetectPPParseConfPorts("tcp", IPPROTO_TCP,
                    proto_name, ALPROTO_DIAMETER, 0, DIAMETER_MIN_FRAME_LEN,
                    DiameterProbingParserTs, DiameterProbingParserTc)) {
                SCLogDebug("No diameter app-layer configuration, enabling echo"
                           " detection TCP detection on port %s.",
                        DIAMETER_DEFAULT_PORT);
                AppLayerProtoDetectPPRegister(IPPROTO_TCP,
                    DIAMETER_DEFAULT_PORT, ALPROTO_DIAMETER, 0,
                    DIAMETER_MIN_FRAME_LEN, STREAM_TOSERVER,
                    DiameterProbingParserTs, DiameterProbingParserTc);
            }

        }

    }

    else {
        SCLogDebug("Protocol detector and parser disabled for Diameter.");
        return;
    }

    if (AppLayerParserConfParserEnabled("tcp", proto_name)) {

        SCLogNotice("Registering Diameter protocol parser.");

        /* Register functions for state allocation and freeing. A
         * state is allocated for every new Diameter flow. */
        AppLayerParserRegisterStateFuncs(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterStateAlloc, DiameterStateFree);

        /* Register request parser for parsing frame from server to client. */
        AppLayerParserRegisterParser(IPPROTO_TCP, ALPROTO_DIAMETER,
            STREAM_TOSERVER, DiameterParseRequest);

        /* Register response parser for parsing frames from server to client. */
        AppLayerParserRegisterParser(IPPROTO_TCP, ALPROTO_DIAMETER,
            STREAM_TOCLIENT, DiameterParseResponse);

        /* Register a function to be called by the application layer
         * when a transaction is to be freed. */
        AppLayerParserRegisterTxFreeFunc(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterStateTxFree);

        /* Register a function to return the current transaction count. */
        AppLayerParserRegisterGetTxCnt(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterGetTxCnt);

        /* Transaction handling. */
        AppLayerParserRegisterStateProgressCompletionStatus(ALPROTO_DIAMETER, 1, 1);
        AppLayerParserRegisterGetStateProgressFunc(IPPROTO_TCP,
            ALPROTO_DIAMETER, DiameterGetStateProgress);
        AppLayerParserRegisterGetTx(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterGetTx);
        AppLayerParserRegisterTxDataFunc(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterGetTxData);
        AppLayerParserRegisterStateDataFunc(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetStateData);

        AppLayerParserRegisterGetEventInfo(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterStateGetEventInfo);
        AppLayerParserRegisterGetEventInfoById(IPPROTO_TCP, ALPROTO_DIAMETER,
            DiameterStateGetEventInfoById);

        /* Leave this is if your parser can handle gaps, otherwise
         * remove. */
        AppLayerParserRegisterOptionFlags(IPPROTO_TCP, ALPROTO_DIAMETER,
            APP_LAYER_PARSER_OPT_ACCEPT_GAPS);
    }
    else {
        SCLogDebug("Diameter protocol parsing disabled.");
    }

#ifdef UNITTESTS
    AppLayerParserRegisterProtocolUnittests(IPPROTO_TCP, ALPROTO_DIAMETER,
        DiameterParserRegisterTests);
#endif
}

#ifdef UNITTESTS
#endif

void DiameterParserRegisterTests(void)
{
#ifdef UNITTESTS
#endif
}
