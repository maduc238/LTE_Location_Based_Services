# GMLC build trên jDiameter

Source code của jDiameter: https://github.com/RestComm/jdiameter

Cụ thể:
- Khối GMLC chứa trong folder `GMLC`
- Khối HSS và MME đều chứa trong foler `Server`
  - HSS trong `SlhServer.java`
  - MME trong `SlgServer.java`

File `GMLC.java` và `config.xml` chứa code ứng dụng để tạo khối GMLC và cấu hình cho GMLC để kết nối với HSS và MME

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

Folder `ShLCS` / `ShLCS.java` chứa khối Sh client, gửi bản tin User-Data Request tới server

Folder `Server` / `ShServer.java` chứa khối Sh server, gửi v bản tin User-Data Answer
