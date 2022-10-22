# jDiameter

Các hàm quan trọng cần quan tâm:

- log.error() và log.info()
- createAnswer()
- processRequest(Request request)

Quan trọng nhất là file Client :))

```
Class Stack:
|-- getMetaData()
   |-- getLocalPeer()
      |-- getCommonApplications()
      |-- getUri()
         |-- getFQDN()
      |-- getRealmName()
   |-- getMinorVersion()
   |-- getStackType()
   |-- destroy()

Class StackType:

Class Request: Chứa các request nhận được
|-- Request.getCommandCode(): Nhận về Command Code của Diameter
|-- Request.getAvps()->AvpSet - Nhận lấy các AVP có được trong bản tin đó
|-- Request.createAnswer(resultCode)->Answer - Tạo answer từ cái resultcode được chỉ định, nó sẽ tạo header và systemAVP đã được sao chép để answer

Class Answer: Chứa các trường thông tin kết quả sẽ được gửi đi
|-- Answer.getAvps()->AvpSet - Nhận lấy các AVP trong trường answer
|-- Answer.addAvp()->AvpSet hoặc kệ nó
   |-- avpCode: Code của AVP mới đó
   |-- value: Data của AVP
   |-- các flags...
   |-- asOctetString: 

Class AvpSet: Chứa các AVP của một thông tin bản tin
|-- AvpSet.getAvp(`Avp code`, `vendor ID hay là vnd`)->Avp - Nhận lấy một AVP cụ thể từ AVP Code và vendor ID của nó, có thể đặt là 10415 (3GPP)

Class Avp: Chứa một AVP cụ thể
|-- Avp.getUnsigned32()->long - Data dưới dạng số nguyên dương
|-- Avp.getUTF8String()->String - Data dưới dạng String (Use UTF-8 code page)
|-- Avp.ORIGIN_HOST - AVP: Origin-Host(264)
|-- Avp.ORIGIN_REALM - AVP: Origin-Realm(296)

Class Session: Tạo một phiên request - answer
|-- Session.createRequest(commandCode, ...) - Những cái được sử dụng là 8388620 đến 8388622
```

Application ID:
- 3GPP SLh: 16777291
- 3GPP SLg: 16777255
Command Code:
- name="3GPP-Provide-Location" code="8388620"
- name="3GPP-Location-Report" code="8388621"
- name="3GPP-LCS-Routing-Info" code="8388622"
