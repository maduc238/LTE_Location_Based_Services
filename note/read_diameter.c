#include <bits/types.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
typedef __uint8_t uint8_t;
typedef __uint32_t uint32_t;
typedef struct AVP {
    uint32_t AvpCode;
    uint8_t AvpFlags;
    uint32_t AvpLength;
    uint32_t VendorId;
    uint8_t *AvpData;
    uint32_t AvpDataLength;
    struct AVP* next;
} AVP;
typedef struct DiameterMessage {
    uint8_t Version;
    uint32_t Length;
    uint8_t Flags;
    uint32_t CommandCode;
    uint32_t ApplicationId;
    uint32_t HopbyHopId;
    uint32_t EndtoEndId;
    uint32_t n_avp;     // Số AVP
    struct AVP *avp;
} DiameterMessage;
uint32_t Read3Byte(uint8_t *data, uint8_t point);
uint32_t Read4Byte(uint8_t *data, uint8_t point);
uint8_t toBinaryAt(uint8_t a, uint8_t point);
void addLast(struct AVP **avp, uint32_t AvpCode, uint8_t AvpFlags, uint32_t AvpLength, 
    uint32_t VendorId, uint8_t *AvpData, uint32_t AvpDataLength);

DiameterMessage ReadDiameterData(uint8_t *data, uint32_t data_len) {
    DiameterMessage message;
    if (data_len < 20) return message;
    message.Version = data[0];
    message.Length = Read3Byte(data, 1);
    message.Flags = data[4];
    message.CommandCode = Read3Byte(data, 5);
    message.ApplicationId = Read4Byte(data, 8);
    message.HopbyHopId = Read4Byte(data, 12);
    message.EndtoEndId = Read4Byte(data, 16);
    message.n_avp = 0;
    uint32_t point = 20;
    struct AVP *avp = NULL;
    while (point < data_len) {
        uint32_t AvpCode = Read4Byte(data, point); point += 4;
        uint8_t AvpFlags = data[point]; point++;
        uint32_t AvpLength = Read3Byte(data, point); point += 3;
        uint32_t VendorId, AvpDataLength;
        if (AvpLength > message.Length) break;
        if (toBinaryAt(AvpFlags,0) == 1) {      // Vendor Flag
            VendorId = Read4Byte(data, point); point += 4;
            AvpDataLength = AvpLength - 12;
        } else {
            AvpDataLength = AvpLength - 8;
        }
        uint8_t AvpData[AvpDataLength];
        for (int i=0; i<AvpDataLength; i++) {
            AvpData[i] = data[point];
            // printf("%c",AvpData[i]);
            point ++;
        }
        uint8_t padding = 0;
        if (AvpDataLength % 4 != 0) {
            padding = 4 - AvpDataLength % 4;
        }
        point += padding;
        addLast(&avp, AvpCode, AvpFlags, AvpLength, VendorId, AvpData, AvpDataLength);
        message.n_avp++;

        if(point >= message.Length) break;
    }
    message.avp = avp;
    return message;
}

uint32_t Read3Byte(uint8_t *data, uint8_t point) {
    return data[point]*256*256 + data[point+1]*256 + data[point+2];
}

uint32_t Read4Byte(uint8_t *data, uint8_t point) {
    return data[point]*256*256*256 + data[point+1]*256*256 + data[point+2]*256 + data[point+3];
}

uint8_t toBinaryAt(uint8_t a, uint8_t point) {
    uint8_t i,j=0;
    uint8_t result[8];
    for(i=0x80;i!=0;i>>=1) {
        result[j] = ((a&i)? 1:0); j++;
    }
    return result[point];
}

void addLast(struct AVP **avp, uint32_t AvpCode, uint8_t AvpFlags, uint32_t AvpLength, 
            uint32_t VendorId, uint8_t *AvpData, uint32_t AvpDataLength) {
    struct AVP *newAvp = malloc(sizeof(struct AVP));
    newAvp->AvpCode = AvpCode;
    newAvp->AvpFlags = AvpFlags;
    newAvp->AvpLength = AvpLength;
    newAvp->VendorId = VendorId;
    newAvp->AvpData = malloc(AvpDataLength);
    for (int i=0; i<AvpDataLength; i++) {
        newAvp->AvpData[i] = AvpData[i];
    }
    newAvp->AvpDataLength = AvpDataLength;

    if (*avp == NULL)
        *avp = newAvp;
    else {
        struct AVP *lastAvp = *avp;
        while (lastAvp->next != NULL)
            lastAvp = lastAvp->next;
        lastAvp->next = newAvp;
    }
}

/* Print dữ liệu bản tin Diameter */
void PrintDiameterData(DiameterMessage mess) {
    /* Print Diameter message */
    printf("Version: %d\n",mess.Version);
    printf("Length: %d\n",mess.Length);
    printf("Command Code: %d\n",mess.CommandCode);
    printf("ApplicationId: %d\n",mess.ApplicationId);
    printf("Hop-by-Hop Id: 0x%08x\n",mess.HopbyHopId);
    printf("End-to-End Id: 0x%08x\n",mess.EndtoEndId);
    while (mess.avp != NULL) {
        printf("****************************************\n");
        printf("Avp Code: %d\n", mess.avp->AvpCode);        
        printf("Avp Flags: %02x\n", mess.avp->AvpFlags);
        printf("Avp Length: %d\n", mess.avp->AvpLength);
        if (toBinaryAt(mess.avp->AvpFlags,0) == 1) printf("Avp Vendor Id: %d\n", mess.avp->VendorId);
        printf("Avp Data: ");
        uint8_t *data = mess.avp->AvpData;
        for (int i=0; i<mess.avp->AvpDataLength; i++) {
            printf("%c", data[i]);
        }
        printf("\n");
        mess.avp = mess.avp->next;
    }
}

/* Print dữ liệu bản tin Diameter */
void PrintDiameterDataRaw(uint8_t *data, uint32_t data_len) {
    DiameterMessage mess = ReadDiameterData(data, data_len);
    PrintDiameterData(mess);
}

/**
 * \brief Kiểm tra xem AVP Code có tồn tại trong dữ liệu này không
 * 
 * \param mess Struct DiameterMessage
 * \param AvpCode AVP Code
 * 
 * \retval Nếu tồn tại AVP Code đó sẽ trả về true, ngược lại thì false
*/
bool CheckAvpCode(DiameterMessage mess, uint32_t AvpCode) {
    while (mess.avp != NULL) {
        if (mess.avp->AvpCode == AvpCode) return true;
        mess.avp = mess.avp->next;
    }
    return false;
}

/**
 * \brief Kiểm tra xem AVP Code có tồn tại trong dữ liệu này không
 * 
 * \param mess Struct DiameterMessage
 * \param AvpCode AVP Code
 * \param VendorId Vendor Id của AVP Code này
 * 
 * \retval Nếu tồn tại AVP Code cùng với Vendor Id đó sẽ trả về true, ngược lại thì false
*/
bool CheckAvpCode(DiameterMessage mess, uint32_t AvpCode, uint32_t VendorId) {
    while (mess.avp != NULL) {
        if (toBinaryAt(mess.avp->AvpFlags,0) == 1) {
            if (mess.avp->AvpCode == AvpCode && mess.avp->VendorId == VendorId) return true;
        }
        mess.avp = mess.avp->next;
    }
    return false;
}

/**
 * Next step:
 * Đọc sub-AVP từ data thuần
 * Tích hợp vào suricata
*/

/* Tét */
int main() {
    uint8_t data[] =
        {0x01, 0x00, 0x00, 0xe4, 0x80, 0x00, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x33, 0x8c, 0x98, 0xd7,
        0x22, 0x30, 0x00, 0x00, 0x00, 0x00, 0x01, 0x08, 0x40, 0x00, 0x00, 0x30, 0x6c, 0x62, 0x73, 0x61,
        0x6e, 0x6d, 0x2e, 0x69, 0x6d, 0x73, 0x2e, 0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63,
        0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67, 0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b,
        0x2e, 0x6f, 0x72, 0x67, 0x00, 0x00, 0x01, 0x28, 0x40, 0x00, 0x00, 0x29, 0x69, 0x6d, 0x73, 0x2e,
        0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63, 0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67,
        0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b, 0x2e, 0x6f, 0x72, 0x67, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x01, 0x40, 0x00, 0x00, 0x0e, 0x00, 0x01, 0xc0, 0xa8, 0x0e, 0x23, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x0a, 0x40, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x01, 0x0d,
        0x00, 0x00, 0x00, 0x11, 0x6a, 0x44, 0x69, 0x61, 0x6d, 0x65, 0x74, 0x65, 0x72, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x09, 0x40, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x01, 0x04,
        0x40, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x0a, 0x40, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x28, 0xaf,
        0x00, 0x00, 0x01, 0x02, 0x40, 0x00, 0x00, 0x0c, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x0b,
        0x00, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x16, 0x40, 0x00, 0x00, 0x0c,
        0x33, 0x8c, 0x98, 0xfd};
    uint32_t data_len = 224;
    DiameterMessage mess = ReadDiameterData(data, data_len);
    PrintDiameterDataRaw(data, data_len);
    // if (CheckAvpCode(mess, 260)) printf("True");
}
