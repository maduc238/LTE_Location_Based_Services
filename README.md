# GMLC build trên jDiameter

Source code của jDiamete: https://github.com/RestComm/jdiameter

File `GMLC.java` và `config.xml` chứa code ứng dụng để tạo khối GMLC và cấu hình cho GMLC để kết nối với HSS và MME. Tất cả được chứa trong folder Open5gs`

Các interface sử dụng để trao đổi bản tin Diameter:
- SLh: Kết nối với HSS (127.0.0.8)
- SLg: Kết nối với MME (127.0.0.2)

```
            --------------
            |    GMLC    |
            | 127.0.0.10 |
            --------------
       SLg /              \ SLh
          /                \
  -------------         -------------
  |    MME    |         |    HSS    |
  | 127.0.0.2 |         | 127.0.0.8 |
  -------------         -------------

```

Sử dụng: Bật `Open5gs` để tạo HSS và MME. Sau đó chạy `GMLC`

Tự build:
```
mvn install -f "pom.xml" -Dcheckstyle.skip
```

Ném file `example1-1.7.0-SNAPSHOT-jar-with-dependencies.jar` vào đường dẫn `.../target/`
```
java -classpath target/example1-1.7.0-SNAPSHOT-jar-with-dependencies.jar org.example.server.ExampleServer
```
Chạy và bật wireshark trên `lo` để xem kết quả

# LCS application trên Sh interface

Folder `ShLCS` chứa khối Sh client, gửi bản tin User-Data Request tới server

Folder `Server` chứa khối Sh server, gửi bản tin User-Data Answer v
