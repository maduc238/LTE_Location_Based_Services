# GMLC build trên jDiameter

File `GMLC.java` và `config.xml` chứa code ứng dụng để tạo khối GMLC và cấu hình cho GMLC để kết nối với HSS và MME. Tất cả được chứa trong folder Open5gs`

Các interface sử dụng để trao đổi bản tin Diameter:
- SLh: Kết nối với HSS (127.0.0.8)
- SLg: Kết nối với MME (127.0.0.2)

Sử dụng: Bật `Open5gs` để tạo HSS và MME. Sau đó chạy `GMLC`
