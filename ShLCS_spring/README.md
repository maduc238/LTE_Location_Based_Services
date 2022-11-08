# Sh_LCS Client

Ứng dụng này bao gồm: 
- Khối Location Service, gửi bản tin DIAMETER Request tới HSS
- Giao diện web: Xử lý tác vụ nhập thông tin MSISDN; xem thông tin log hoạt động và dữ liệu nhận được từ bản tin DIAMETER Answer

## Cấu hình ứng dụng

Cấu hình `jDiameter` tại file `config.xml`. Chi tiết thông tin cấu hình trong Document của `jDiameter`

Cấu hình web server tại file `application.properties`. Trong đây có địa chỉ ip và port cho web server

## Chạy ứng dụng

Sử dụng Maven để chạy ứng dụng trong file `App.java`

Sau khi chạy thành công, bật Web Server để thực hiện gửi yêu cầu với số MSISDN. Truy cập tại địa chỉ đã cấu hình trước đó, mặc định là http://127.0.0.1:8080

