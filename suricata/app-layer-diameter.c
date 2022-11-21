/* Copyright (C) 2015-2022 Open Information Security Foundation
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

/**
 * \file
 *
 * \author Ma Duc <mavietduc@gmail.com>
 *
 * Diameter application layer detector and parser for learning and
 * diameter purposes.
 *
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
    DIAMETER_DECODER_EVENT_ERROR_MESSAGE,
    DIAMETER_SENDING_MESSAGE,
    DIAMETER_RECIVE_SUCCESS_MESSAGE
};

SCEnumCharMap diameter_decoder_event_table[] = {
    {"EMPTY_MESSAGE", DIAMETER_DECODER_EVENT_EMPTY_MESSAGE},
    {"ERROR_MESSAGE", DIAMETER_DECODER_EVENT_ERROR_MESSAGE},
    {"DIAMETER_SENDING",DIAMETER_SENDING_MESSAGE},
    {"DIAMETER_SUCESS",DIAMETER_RECIVE_SUCCESS_MESSAGE},

    // event table must be NULL-terminated
    { NULL, -1 },
};

static uint8_t toBinaryAt(uint8_t a, uint8_t point) {
    uint8_t i,j=0;
    uint8_t result[8];
    for(i=0x80;i!=0;i>>=1) {
        result[j] = ((a&i)? 1:0); j++;
    }
    return result[point];
}
DiameterMessageHeader ReadDiameterHeaderData(uint8_t *data, uint32_t data_len) {
    DiameterMessageHeader message;
    if (data_len < 20) return message;
    message.Version = data[0];
    message.Length = data[1]*256*256 + data[2]*256 + data[3];
    message.Flags = data[4];
    message.CommandCode = data[5]*256*256 + data[6]*256 + data[7];
    message.ApplicationId = data[8]*256*256*256 + data[9]*256*256 + data[10]*256 + data[11];
    message.HopbyHopId = data[12]*256*256*256 + data[13]*256*256 + data[14]*256 + data[15];
    message.EndtoEndId = data[16]*256*256*256 + data[17]*256*256 + data[18]*256 + data[19];
    return message;
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

static DiameterTransaction *DiameterTxAlloc(DiameterState *state)
{
    DiameterTransaction *tx = SCCalloc(1, sizeof(DiameterTransaction));
    if (unlikely(tx == NULL)) {
        return NULL;
    }

    tx->tx_id = state->transaction_max++;

    TAILQ_INSERT_TAIL(&state->tx_list, tx, next);

    return tx;
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

static void *DiameterGetTx(void *statev, uint64_t tx_id)
{
    DiameterState *d_state = (DiameterState *)statev;
    DiameterTransaction *tx;

    // SCLogNotice("Requested tx ID %08x", tx_id);

    TAILQ_FOREACH(tx, &d_state->tx_list, next) {
        if (tx->tx_id == tx_id) {
            // SCLogNotice("Transaction %08x found, returning tx object %p.", tx_id, tx);
            return tx;
        }
    }

    // SCLogNotice("Transaction ID %08x not found.", tx_id);
    return NULL;
}

static int DiameterStateGetEventInfo(const char *event_name, int *event_id, AppLayerEventType *event_type)
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

static int DiameterStateGetEventInfoById(int event_id, const char **event_name, AppLayerEventType *event_type)
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
 * \brief Khảo sát xem data đến có là Diameter không.
 *
 * \retval ALPROTO_DIAMETER nếu giống như Diameter,
 *     ALPROTO_FAILED, nếu rõ ràng không phải ALPROTO_DIAMETER,
 *     nếu không thì ALPROTO_UNKNOWN.
 */
static AppProto DiameterProbingParser(Flow *f, uint8_t direction,
        const uint8_t *input, uint32_t input_len, uint8_t *rdir)
{
    /* Kiểm tra Diameter ở đây. */
    if (input_len == 0) {
        SCLogNotice("Detected as TCP");
        return ALPROTO_DIAMETER;
    }
    else if (input[0] == 0x01 && input_len > DIAMETER_MIN_FRAME_LEN) {
        SCLogNotice("Detected as ALPROTO_DIAMETER");
        return ALPROTO_DIAMETER;
    }

    SCLogInfo("Protocol not detected as ALPROTO_DIAMETER.");
    return ALPROTO_FAILED;
}

/* Decode bản tin đọc header ở đây */
/* static AppLayerResult DiameterDecode(Flow *f, uint8_t direction, void *alstate,
        AppLayerParserState *pstate, StreamSlice stream_slice)
{
    DiameterState *d_state = (DiameterState *)alstate;
    const uint8_t *input = StreamSliceGetData(&stream_slice);
    uint32_t input_len = StreamSliceGetDataLen(&stream_slice);
    const uint8_t flags = StreamSliceGetFlags(&stream_slice);

    if (input == NULL &&
        ((direction == 0 && AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TS)) ||
                (direction == 1 &&
                        AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TC)))) {
        return APP_LAYER_OK;
    } else if (input == NULL || input_len == 0) {
        return APP_LAYER_ERROR;
    }

    // Check có đúng là Diameter không
    DiameterMessageHeader diameter_header = ReadDiameterHeaderData(input, input_len);
    if (diameter_header.Length != input_len) {
        SCLogNotice("Bản tin Diameter nhận diện không đúng");
        return APP_LAYER_ERROR;
    }
    SCLogNotice("Parsing diameter message: len=%"PRIu32". CommandCode=%"PRIu32, input_len, diameter_header.CommandCode);

    // Make a copy of the message
    d_state->data = SCCalloc(1, input_len);

    memcpy(d_state->data, input, input_len);
    d_state->data_len = input_len;

    return APP_LAYER_OK;
} */

static AppLayerResult DiameterParseRequest(Flow *f, void *alstate, AppLayerParserState *pstate,
        StreamSlice stream_slice, void *local_data)
{
    DiameterState *d_state = (DiameterState *)alstate;
    const uint8_t *input = StreamSliceGetData(&stream_slice);
    uint32_t input_len = StreamSliceGetDataLen(&stream_slice);
    const uint8_t flags = StreamSliceGetFlags(&stream_slice);

    if (input == NULL) {
        if (AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TS)) {
            SCReturnStruct(APP_LAYER_OK);
        } else if (flags & STREAM_GAP) {
            SCReturnStruct(APP_LAYER_OK);
        }
        DEBUG_VALIDATE_BUG_ON(true);
        SCReturnStruct(APP_LAYER_ERROR);
    }

    /* Check có đúng là Diameter không */
    DiameterMessageHeader diameter_header = ReadDiameterHeaderData(input, input_len);
    if (diameter_header.Length != input_len) {
        SCLogNotice("Bản tin Diameter nhận diện không đúng");
        return APP_LAYER_ERROR;
    }
    SCLogNotice("Parsing diameter request: len=%"PRIu32". CommandCode=%"PRIu32, input_len, diameter_header.CommandCode);

    DiameterTransaction *tx = DiameterTxAlloc(d_state);

    if (unlikely(tx == NULL)) {
        SCLogNotice("Failed to allocate new Diameter tx.");
        goto end;
    }
    tx->tx_id = diameter_header.EndtoEndId;
    tx->response_done = 1;
    SCLogNotice("Allocated Diameter tx id %08x.", tx->tx_id);

    /* Make a copy of the message. */
    tx->request_buffer = SCCalloc(1, input_len);
    if (unlikely(tx->request_buffer == NULL)) {
        SCLogInfo("Failed to calloc request buffer");
        goto end;
    }
    memcpy(tx->request_buffer, input, input_len);
    tx->request_buffer_len = input_len;

end:
    SCReturnStruct(APP_LAYER_OK);
    // return DiameterDecode(f, 0 /* toserver */, alstate, pstate, stream_slice);
}


static AppLayerResult DiameterParseResponse(Flow *f, void *alstate, AppLayerParserState *pstate,
        StreamSlice stream_slice, void *local_data)
{
    DiameterState *d_state = (DiameterState *)alstate;
    DiameterTransaction *tx = NULL, *ttx;
    const uint8_t *input = StreamSliceGetData(&stream_slice);
    uint32_t input_len = StreamSliceGetDataLen(&stream_slice);
    const uint8_t flags = StreamSliceGetFlags(&stream_slice);

    if ((input == NULL || input_len == 0) &&
        AppLayerParserStateIssetFlag(pstate, APP_LAYER_PARSER_EOF_TC)) {
        SCReturnStruct(APP_LAYER_OK);
    }

    if (input == NULL || input_len == 0) {
        SCReturnStruct(APP_LAYER_OK);
    }

    /* Check có đúng là Diameter không */
    DiameterMessageHeader diameter_header = ReadDiameterHeaderData(input, input_len);
    if (diameter_header.Length != input_len) {
        SCLogNotice("Bản tin Diameter nhận diện không đúng");
        return APP_LAYER_ERROR;
    }
    SCLogNotice("Parsing diameter response: len=%"PRIu32". CommandCode=%"PRIu32". Endtoend Id=%08x", input_len, diameter_header.CommandCode, diameter_header.EndtoEndId);

    /* TAILQ_FOREACH(tx, &d_state->tx_list, next) {
        if (tx->tx_id == diameter_header.EndtoEndId)
            SCLogNotice("Found transaction %08x for response on state %p.", tx->tx_id, d_state);
    } */

    tx = (DiameterTransaction *) DiameterGetTx(d_state, diameter_header.EndtoEndId);

    if (tx == NULL) {
        SCLogNotice("Transaction ID %08x not found.", diameter_header.EndtoEndId);
        goto end;
    }

    SCLogNotice("Transaction %08x found, returning tx object %p.", diameter_header.EndtoEndId, tx);
    /* Make a copy of the response. */
    tx->response_buffer = SCCalloc(1, input_len);
    if (unlikely(tx->response_buffer == NULL)) {
        SCLogInfo("Failed to calloc response buffer");
        goto end;
    }
    memcpy(tx->response_buffer, input, input_len);
    tx->response_buffer_len = input_len;

    tx->response_done = 1;

end:
    SCReturnStruct(APP_LAYER_OK);
    //return DiameterDecode(f, 1 /* toclient */, alstate, pstate, stream_slice);
}

static void DiameterStateTxFree(void *statev, uint64_t tx_id)
{
    DiameterState *state = statev;
    DiameterTransaction *tx = NULL, *ttx;

    SCLogNotice("Freeing transaction %"PRIu64, tx_id);

    TAILQ_FOREACH_SAFE(tx, &state->tx_list, next, ttx) {
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

static uint64_t DiameterGetTxCnt(void *statev)
{
    const DiameterState *state = statev;
    // SCLogNotice("Current tx count is %"PRIu64". in object %p.", state->transaction_max, state);
    return state->transaction_max;
}

static int DiameterGetStateProgress(void *txv, uint8_t direction)
{
    DiameterTransaction *tx = txv;

    SCLogInfo("Transaction progress requested for tx ID %"PRIu32
        ", direction=0x%02x", tx->tx_id, direction);

    if (direction & STREAM_TOCLIENT) {
        return 1;
    }
    else if (direction & STREAM_TOSERVER) {
        return 1;
    }

    return 0;
}

/**
 * \brief retrieve the tx data used for logging, config, detection
 */
static AppLayerTxData *DiameterGetTxData(void *vtx)
{
    DiameterTransaction *tx = (DiameterTransaction *)vtx;
    return &tx->tx_data;
}

/**
 * \brief retrieve the state data
 */
static AppLayerStateData *DiameterGetStateData(void *vstate)
{
    DiameterState *state = (DiameterState *)vstate;
    return &state->state_data;
}

void RegisterDiameterParsers(void)
{
    const char *proto_name = "diameter";

    /* Check if Diameter TCP detection is enabled. If it does not exist in
     * the configuration file then it will be disabled by default. */
    if (AppLayerProtoDetectConfProtoDetectionEnabled("tcp", proto_name)) {
        AppLayerProtoDetectRegisterProtocol(ALPROTO_DIAMETER, proto_name);
        SCLogDebug("Diameter TCP protocol detection enabled.");

        if (RunmodeIsUnittests()) {
            SCLogNotice("Unittest mode, registering default configuration.");
            AppLayerProtoDetectPPRegister(IPPROTO_TCP, DIAMETER_DEFAULT_PORT, ALPROTO_DIAMETER, 0, DIAMETER_MIN_FRAME_LEN, STREAM_TOSERVER, DiameterProbingParser, DiameterProbingParser);
        }
        else {
            if (!AppLayerProtoDetectPPParseConfPorts("tcp", IPPROTO_TCP, proto_name, ALPROTO_DIAMETER, 0, DIAMETER_MIN_FRAME_LEN, DiameterProbingParser, DiameterProbingParser)) {
                SCLogDebug("No diameter app-layer configuration, enabling echo detection TCP detection on port %s.", DIAMETER_DEFAULT_PORT);
                AppLayerProtoDetectPPRegister(IPPROTO_TCP, DIAMETER_DEFAULT_PORT, ALPROTO_DIAMETER, 0, DIAMETER_MIN_FRAME_LEN, STREAM_TOSERVER, DiameterProbingParser, DiameterProbingParser);
            }
        }
    }

    else {
        SCLogDebug("Protocol detector and parser disabled for Diameter.");
        return;
    }

    if (AppLayerParserConfParserEnabled("tcp", proto_name)) {

        SCLogNotice("Registering Diameter protocol parser.");

        AppLayerParserRegisterStateFuncs(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterStateAlloc, DiameterStateFree);

        AppLayerParserRegisterParser(IPPROTO_TCP, ALPROTO_DIAMETER, STREAM_TOSERVER, DiameterParseRequest);
        AppLayerParserRegisterParser(IPPROTO_TCP, ALPROTO_DIAMETER, STREAM_TOCLIENT, DiameterParseResponse);

        AppLayerParserRegisterTxFreeFunc(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterStateTxFree);
        AppLayerParserRegisterGetTxCnt(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetTxCnt);

        AppLayerParserRegisterParserAcceptableDataDirection(IPPROTO_TCP, ALPROTO_DIAMETER, STREAM_TOSERVER);

        /* Transaction handling. */
        AppLayerParserRegisterStateProgressCompletionStatus(ALPROTO_DIAMETER, 1, 1);
        AppLayerParserRegisterGetStateProgressFunc(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetStateProgress);
        AppLayerParserRegisterGetTx(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetTx);
        AppLayerParserRegisterTxDataFunc(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetTxData);
        AppLayerParserRegisterStateDataFunc(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterGetStateData);
        AppLayerParserRegisterGetEventInfo(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterStateGetEventInfo);
        AppLayerParserRegisterGetEventInfoById(IPPROTO_TCP, ALPROTO_DIAMETER, DiameterStateGetEventInfoById);

        /* Leave this is if your parser can handle gaps, otherwise remove. */
        // AppLayerParserRegisterOptionFlags(IPPROTO_TCP, ALPROTO_DIAMETER, APP_LAYER_PARSER_OPT_ACCEPT_GAPS);
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
