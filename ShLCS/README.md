# Sh Interface

## Phía client, gửi bản tin Diamter User-Data Request:
- `ShLCS.java`: Code Java Client
- `LCS_config.xml`: File xml config cho client
- Chạy `.jar`:
```
java -classpath Aothatday.jar org.example.client.ShLCS
```

## Phía server, gửi phản hồi lại bằng bản tin Diameter User-Data Answer:
- `ShServer.java`: Code Java Server
- `serverconfig.xml`: File xml config cho server
- Chạy `.jar`:
```
java -classpath Aothatday.jar org.example.server.ShServer
```
