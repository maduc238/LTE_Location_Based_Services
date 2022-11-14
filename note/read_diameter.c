#include <bits/types.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
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
uint8_t toBinaryAt(uint8_t a, uint8_t point);
void addLast(struct AVP **avp, uint32_t AvpCode, uint8_t AvpFlags, uint32_t AvpLength, 
    uint32_t VendorId, uint8_t *AvpData, uint32_t AvpDataLength);

DiameterMessage ReadDiameterData(uint8_t *data, uint32_t data_len) {
    DiameterMessage message;
    if (data_len < 20) return message;
    message.Version = data[0];
    message.Length = data[1]*256*256 + data[2]*256 + data[3];
    message.Flags = data[4];
    message.CommandCode = data[5]*256*256 + data[6]*256 + data[7];
    message.ApplicationId = data[8]*256*256*256 + data[9]*256*256 + data[10]*256 + data[11];
    message.HopbyHopId = data[12]*256*256*256 + data[13]*256*256 + data[14]*256 + data[15];
    message.EndtoEndId = data[16]*256*256*256 + data[17]*256*256 + data[18]*256 + data[19];
    message.n_avp = 0;
    uint32_t point = 20;
    struct AVP *avp = NULL;
    while (point < data_len) {
        uint32_t AvpCode = data[point]*256*256*256 + data[point+1]*256*256 + data[point+2]*256 + data[point+3]; point += 4;
        uint8_t AvpFlags = data[point]; ++point;
        uint32_t AvpLength = data[point]*256*256 + data[point+1]*256 + data[point+2]; point += 3;
        uint32_t VendorId, AvpDataLength;
        if (AvpLength > message.Length) break;
        if (toBinaryAt(AvpFlags,0) == 1) {      // Vendor Flag
            VendorId = data[point]*256*256*256 + data[point+1]*256*256 + data[point+2]*256 + data[point+3]; point += 4;
            AvpDataLength = AvpLength - 12;
        } else {
            AvpDataLength = AvpLength - 8;
        }
        uint8_t AvpData[AvpDataLength];
        for (uint32_t i=0; i<AvpDataLength; i++) {
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
    for (uint32_t i=0; i<AvpDataLength; i++) {
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
        for (uint32_t i=0; i<mess.avp->AvpDataLength; i++) {
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
bool CheckAvpCodeWithVendorId(DiameterMessage mess, uint32_t AvpCode, uint32_t VendorId) {
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
    const uint8_t data[] = {0x01, 0x00, 0x01, 0x68, 0xc0, 0x00, 0x01, 0x32, 0x01, 0x00, 0x00, 0x01, 0x33, 0xc6, 0xbf, 0x2c, 0x10, 0x50, 0x00, 0x02, 0x00, 0x00, 0x01, 0x07, 0x40, 0x00, 0x00, 0x3e, 0x6c, 0x62, 0x73, 0x61, 0x6e, 0x6d, 0x2e, 0x69, 0x6d, 0x73, 0x2e, 0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63, 0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67, 0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b, 0x2e, 0x6f, 0x72, 0x67, 0x3b, 0x31, 0x36, 0x36, 0x37, 0x33, 0x31, 0x35, 0x39, 0x37, 0x38, 0x39, 0x36, 0x39, 0x00, 0x00, 0x00, 0x00, 0x01, 0x04, 0x40, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x0a, 0x40, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x01, 0x02, 0x40, 0x00, 0x00, 0x0c, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x1b, 0x40, 0x00, 0x00, 0x29, 0x69, 0x6d, 0x73, 0x2e, 0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63, 0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67, 0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b, 0x2e, 0x6f, 0x72, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x08, 0x40, 0x00, 0x00, 0x30, 0x6c, 0x62, 0x73, 0x61, 0x6e, 0x6d, 0x2e, 0x69, 0x6d, 0x73, 0x2e, 0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63, 0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67, 0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b, 0x2e, 0x6f, 0x72, 0x67, 0x00, 0x00, 0x01, 0x28, 0x40, 0x00, 0x00, 0x29, 0x69, 0x6d, 0x73, 0x2e, 0x6d, 0x6e, 0x63, 0x30, 0x30, 0x34, 0x2e, 0x6d, 0x63, 0x63, 0x34, 0x35, 0x32, 0x2e, 0x33, 0x67, 0x70, 0x70, 0x6e, 0x65, 0x74, 0x77, 0x6f, 0x72, 0x6b, 0x2e, 0x6f, 0x72, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x15, 0x40, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x02, 0xbc, 0xc0, 0x00, 0x00, 0x20, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x02, 0xbd, 0xc0, 0x00, 0x00, 0x12, 0x00, 0x00, 0x28, 0xaf, 0x48, 0x79, 0x66, 0x34, 0x22, 0xf4, 0x00, 0x00, 0x00, 0x00, 0x02, 0xc2, 0xc0, 0x00, 0x00, 0x10, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x02, 0xc9, 0x80, 0x00, 0x00, 0x10, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x02, 0xc3, 0xc0, 0x00, 0x00, 0x10, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xbf, 0xc0, 0x00, 0x00, 0x10, 0x00, 0x00, 0x28, 0xaf, 0x00, 0x00, 0x00, 0x0e};
    uint32_t data_len = 456;
    DiameterMessage mess = ReadDiameterData(data, data_len);
    PrintDiameterDataRaw(data, data_len);
    

}

/**
 * 010000e4800001010000000033c64ca60e80000000000108400000306c6273616e6d2e696d732e6d6e633030342e6d63633435322e336770706e6574776f726b2e6f72670000012840000029696d732e6d6e633030342e6d63633435322e336770706e6574776f726b2e6f7267000000000001014000000e00010000e4800001010000000033c6bf2b1050000000000108400000306c6273616e6d2e696d732e6d6e633030342e6d63633435322e336770706e6574776f726b2e6f720167c000a80e2300000000010a4000000c000028af000000012840000029696d732e010d000000116a4469616d657465720000006d006e0063013030342e096d4063006300340c35322e00330067702870af6e657400000104400000200000010a400077006f720c000028af000001024000000c6b012e6f720067000000010000010b0000000c000000000001000001164000000c33c64ccd0001014000000e0001c0a80e2300000000010a4000000c000028af0000010d000000116a4469616d65746572000000000001094000000c000028af00000104400000200000010a4000000c000028af000001024000000c010000010000010b0000000c00000001000001164000000c33c6bf51
*/
