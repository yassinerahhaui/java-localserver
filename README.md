# java-localserver

```
/JAVA-SERVER
├── /src
│   ├── /http                      <-- (1. Everything related to the HTTP protocol)
│   │   ├── HttpMethod.java        (Enum: GET, POST...)
│   │   ├── ParserState.java       (Enum: For Non-blocking network I/O)
│   │   ├── HttpRequest.java       (Request container: Components + Body)
│   │   ├── HttpResponse.java      (Response container: Status + Body Bytes)
│   │   └── HttpParser.java        (Parsing engine: Strings ➔ HttpRequest)
│   │
│   ├── /utils                     <-- (2. Helper utilities)
│   │   ├── RouteResult.java       (DTO: The Router result we just created)
│   │   ├── Cookie.java            (For cookie management)
│   │   └── Session.java           (For user session management)
│   │
│   ├── /core                      <-- (3. Core server engines) 🆕
│   │   ├── Router.java            (Routing requests to files or CGI)
│   │   ├── CGIHandler.java        (Executing PHP/Python scripts)
│   │   ├── ConfigLoader.java      (Reading server settings from config.json)
│   │   └── Server.java            (Network layer: NIO, Sockets, Threads)
│   │
│   ├── /errors                    <-- (4. Custom error pages) (Optional)
│   │   └── ErrorHandler.java      (Replaces your error.java, better naming convention)
│   │
│   └── Main.java                  <-- (Entry point: Reads Config and starts Server)
│
├── /test                          <-- (5. Unit Tests)
│   ├── /http
│   │   ├── HttpParserTest.java    (Tests for Day 1 & 2)
│   │   └── RouterTest.java        (Routing logic tests)
│
├── /wwwroot                       <-- (6. Public files directory - Document Root) 🆕
│   ├── index.html                 (Home page)
│   ├── style.css
│   └── script.py                  (Example of a CGI script)
│
├── config.json                    (Server settings: Port, DocumentRoot...)
├── README.md                      
└── .gitignore                     (To prevent pushing the /out test folder to Git)
```