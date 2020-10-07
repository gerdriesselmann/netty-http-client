# netty-http-client

A simple command line HTTP client build upon Netty

Basically a port of Netty's Snoop client (https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/snoop) example to Scala.

Takes URL to download as parameter and outputs both HTTP heades and content

Example using sbt:

```
sbt "httpclient/run http://www.example.com"
```
