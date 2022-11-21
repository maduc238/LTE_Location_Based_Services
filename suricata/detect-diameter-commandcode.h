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
 * \file detect-diameter-commandcode.h
 *
 * \author Ma Duc <mavietduc@gmail.com>
 */

#include "queue.h"
#include <bits/types.h>


typedef __uint8_t uint8_t;
typedef __uint16_t uint16_t;
typedef __uint32_t uint32_t;
typedef __uint64_t uint64_t;

#ifndef __DETECT_DIAMETER_COMMANDCODE_H__
#define __DETECT_DIAMETER_COMMANDCODE_H__

typedef struct CommandCode {
    uint32_t commandcode;
    TAILQ_ENTRY(CommandCode) next;
} CommandCode;

typedef struct DetectDiameterCommandcodeData {
    TAILQ_HEAD(, CommandCode) commandcode_list;
    uint64_t de_max;
} DetectDiameterCommandcodeData;

void DetectDiameterCommandCodeRegister(void);

#endif /* __DETECT_DIAMETER_COMMANDCODE_H__ */
