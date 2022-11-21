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
 */

#ifndef __APP_LAYER_DIAMETER_H__
#define __APP_LAYER_DIAMETER_H__

#include "rust.h"

void RegisterDiameterParsers(void);
void DiameterParserRegisterTests(void);

typedef struct DiameterMessageHeader {
    /* Version of Diameter */
    uint8_t Version;
    /* Diameter Length, để  kiểm tra data nhận được có dài đúng như header đọc được không*/
    uint32_t Length;
    /* Diameter Flags - Khác của suricata */
    uint8_t Flags;
    uint32_t CommandCode;
    uint32_t ApplicationId;
    uint32_t HopbyHopId;
    uint32_t EndtoEndId;
} DiameterMessageHeader;
DiameterMessageHeader ReadDiameterHeaderData(uint8_t *data, uint32_t data_len);

typedef struct DiameterTransaction
{
    AppLayerTxData tx_data;
    // end to end id
    uint32_t tx_id;

    uint8_t *request_buffer;
    uint32_t request_buffer_len;

    uint8_t *response_buffer;
    uint32_t response_buffer_len;

    uint8_t response_done;

    TAILQ_ENTRY(DiameterTransaction) next;

} DiameterTransaction;

typedef struct DiameterState {
    AppLayerStateData state_data;
    /** List of Diameter transactions associated with this
     *  state. */
    TAILQ_HEAD(, DiameterTransaction) tx_list;

    /** A count of the number of transactions created. The
     *  transaction ID for each transaction is allocated
     *  by incrementing this value. */
    uint64_t transaction_max;
} DiameterState;

#endif /* __APP_LAYER_DIAMETER_H__ */
