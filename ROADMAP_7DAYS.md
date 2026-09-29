# 🚀 Java HTTP Server (Localhost) — 7-Day Roadmap & Todo List

Plan m-fssel l 2 développeurs (**Dev A** w **Asta**) bach t-bniw l-Web Server dialkom mn zero f **7 Iyam** w t-jibo **100% f l-Audit**.

---

## 👥 Taqsim dial les Rôles

* **Dev A (Network & Core I/O Engine):**
  * Responsable 3la: Sockets, NIO Selector, Non-blocking event loop, State machine dial les connections, File streaming, ProcessBuilder (CGI).
* **Asta (HTTP Protocol, Routing & Data Logic):**
  * Responsable 3la: Config parsing (JSON), HTTP Request Parser, HTTP Response Builder, Routes & Static Files, Directory Listing, Cookies/Sessions.

---

## 📅 Nhar 1: Sockets, Selector & Config File

> **🎯 L-Hadaf:** Server kay-tla3 f multiple ports (NIO Selector) w kay-qra `config.json` bla ma y-crachi.

### Dev A (Network Core)
- [ ] Créer `src/server/Server.java`
- [ ] Fte7 `Selector` wa7ed: `Selector.open()`
- [ ] Fte7 `ServerSocketChannel` l kolla port f config
- [ ] Rdd les channels Non-Blocking: `ssc.configureBlocking(false)`
- [ ] Enregistrer les channels f l-selector b `SelectionKey.OP_ACCEPT`
- [ ] Bni l-event loop: `while (running) { selector.select(); ... }`
- [ ] Implementer `handleAccept()`: mlli yji client jdid, rddo non-blocking w registersih b `OP_READ`

### Asta (Config & Models)
- [x] Gérer `config.json` (host, ports, routes, error pages, client_max_body_size)
- [x] Créer `src/config/ServerConfig.java` w `src/config/RouteConfig.java` (POJO data models)
- [x] Créer `src/config/SimpleJsonParser.java` (Parser JSON b Java standard bla external libraries)
- [x] Créer `src/config/ConfigLoader.java` (Charger les configs w y-validihom)
- [x] Créer `src/Main.java` li kay-3eyet l `ConfigLoader` w kay-lansi `Server.start()`

🧪 **Milestone Check Nhar 1:**
```bash
./build.sh
java -cp bin Main config.json
# F terminal akhor:
nc -zv localhost 8080   # Connection succeeded!
nc -zv localhost 8081   # Connection succeeded!
```

---

## 📅 Nhar 2: HTTP Parser & Response Builder

> **🎯 L-Hadaf:** Client kay-sift request b `curl`, l-server kay-fhemha w kay-rejje3 response HTTP/1.1 s7i7a.

### Dev A (Connection State & Reading)
- [ ] Créer classe `ClientConnection` (Attachement f `SelectionKey`):
  - Buffer dial l-qraya (`ByteBuffer`)
  - Ch7al dial data t-qrat
  - State: headers kamlin wla baqin
- [ ] Implementer `handleRead(SelectionKey key)`:
  - Dir `socketChannel.read(buffer)`
  - Handle `read == -1` (Client disconnect) -> sdd socket w cancel key
  - Handle `IOException` -> sdd socket w cancel key
- [ ] Mlli t-tsala request, beddel interest dial key l `OP_WRITE`
- [ ] Implementer `handleWrite(SelectionKey key)`:
  - `socketChannel.write(responseBuffer)`

### Asta (HTTP Request & Response)
- [x] Créer `src/http/HttpRequest.java`:
  - Fields: `method`, `uri`, `path`, `httpVersion`, `headers` (Map), `queryParams` (Map)
- [ ] Créer `src/http/HttpParser.java`:
  - Detecter fin kay-salew les headers: `\r\n\r\n`
  - Parser Request-Line: `GET /index.html HTTP/1.1`
  - Parser les Headers: `Host`, `User-Agent`, `Connection`, etc.
  - Parser Query String: `?name=alice&age=20`
- [x] Créer `src/http/HttpResponse.java`:
  - Fields: `statusCode`, `statusMessage`, `headers`, `body`
  - Method `toBytes()` li kat-formati:
    `HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 12\r\n\r\nHello World!`

🧪 **Milestone Check Nhar 2:**
```bash
curl -i http://localhost:8080/
# Output khasso y-koun:
# HTTP/1.1 200 OK
# Content-Length: 12
# Hello World!
```

---

## 📅 Nhar 3: Routing, Static Files & Directory Listing (GET)

> **🎯 L-Hadaf:** Browser y-telle3 site web static kamel b CSS w tsawer, w y-listi l-fichiers f dossier.

### Dev A (Router & Security)
- [ ] Créer `src/router/Router.java`:
  - Matcher request path m3a les routes f `ServerConfig` (Longest prefix match)
  - Ila l-path ma kaynx f 7tta route -> Return `404 Not Found`
  - Ila l-method ma kaynach f `route.methods` -> Return `405 Method Not Allowed`
- [ ] Sécurité Traversal Attack:
  - Check `canonicalPath.startsWith(canonicalRoot)` bach t-mne3 `../../etc/passwd` -> Return `403 Forbidden`

### Asta (Static Files & Directory Listing)
- [ ] Créer `src/handlers/StaticFileHandler.java`:
  - Map dial MIME types (`.html`, `.css`, `.js`, `.png`, `.jpg`, `.json`, `.pdf`...)
  - Ila kan l-path dossier: qleb 3la `default_file` (index.html)
  - Ila ma kanx `default_file` w `directory_listing: true`: généri page HTML fiha la liste dial les fichiers m3a links
- [ ] Créer `src/error/ErrorHandler.java`:
  - Support dial custom error pages mn `config.json` (`400`, `403`, `404`, `405`, `413`, `500`)
  - Fallback l default HTML error page ila ma kanx custom page

🧪 **Milestone Check Nhar 3:**
```bash
# 1. Test Static Page:
curl -i http://localhost:8080/index.html   # 200 OK + Content-Type: text/html
# 2. Test 404:
curl -i http://localhost:8080/notfound     # 404 Not Found
# 3. Test Directory Listing:
curl -i http://localhost:8080/upload/      # 200 OK + HTML Directory Listing
# 4. Test Method not allowed:
curl -i -X DELETE http://localhost:8080/   # 405 Method Not Allowed
```

---

## 📅 Nhar 4: File Uploads & Delete (POST & DELETE)

> **🎯 L-Hadaf:** Client kay-lo7 fichier b POST, kay-t-kteb f disk bla corruption, w kay-t-mseh b DELETE.

### Dev A (Body Parsing & Body Limit)
- [ ] F `HttpParser.java`:
  - Qra `Content-Length` header
  - Ma t-golx "Request Complete" 7tta y-koun `dataLength >= headerEnd + contentLength`
- [ ] Handle `client_max_body_size`:
  - Check la taille dial body m3a limit f config
  - Ila fat l-limit -> Return **`413 Payload Too Large`** direct bla ma t-qra l-baqi
- [ ] F `Server.java:handleWrite`:
  - Streaming dial les fichiers l-kbar b `FileChannel.transferTo` (Zero-copy)
  - ⚠️ **Audit Rule:** Max 1 write call per select iteration!

### Asta (Upload & Delete Endpoints)
- [ ] F `Router.java` method `handlePost()`:
  - Extracti smia dial l-fichier mn `Content-Disposition: filename="..."`
  - Ila ma kanx header, dir default name `upload_<timestamp>.dat`
  - Kteb raw bytes f dossier dial l-upload: `Files.write(targetPath, body)`
  - Jawb b **`201 Created`** m3a `Location: /upload/<filename>`
- [ ] F `Router.java` method `handleDelete()`:
  - Mseh l-fichier b `Files.delete(filePath)`
  - Ila t-mseh -> Return **`204 No Content`**
  - Ila l-fichier aslan ma kaynx -> Return **`404 Not Found`**
  - Ila ma 3ndkx permissions -> Return **`403 Forbidden`**

🧪 **Milestone Check Nhar 4:**
```bash
# 1. Upload
echo "Localhost Audit Test" > test.txt
curl -i -X POST http://localhost:8080/upload \
  -H "Content-Disposition: attachment; filename=test.txt" \
  --data-binary @test.txt     # 201 Created

# 2. Retrieve without corruption
curl http://localhost:8080/upload/test.txt   # Must output: Localhost Audit Test

# 3. Delete
curl -i -X DELETE http://localhost:8080/upload/test.txt   # 204 No Content
```

---

## 📅 Nhar 5: Cookies, Sessions & Chunked Requests

> **🎯 L-Hadaf:** Server kay-3qel 3la l-browser b Sessions w Cookies, w kay-fhem stream chunked.

### Dev A (Chunked Transfer Encoding)
- [ ] F `HttpParser.java`:
  - Detecter `Transfer-Encoding: chunked`
  - Implementer loop dial parsing:
    1. Qra chunk size en hex (e.g. `1f\r\n`)
    2. Converti hex l integer
    3. Ila `chunkSize == 0` m3a `\r\n\r\n` -> Request salat!
    4. Qra chunk data + verify `\r\n`
    5. Jm3 les chunks f `ByteArrayOutputStream`
  - Check body size limit f wst l-chunks (ila fat limit -> 413)

### Asta (Cookies & Session Management)
- [ ] Créer `src/utils/Session.java`:
  - `id` (UUID), `creationTime`, `lastAccessedTime`, `attributes` (Map)
- [ ] Créer `src/utils/SessionManager.java`:
  - `ConcurrentHashMap<String, Session>`
  - Method `createSession()`, `getSession(id)`, `removeSession(id)`
  - Expiration dial session mor 30 minutes d'inactivité
- [ ] F `Server.java`:
  - Qra header `Cookie: session_id=...`
  - Ila ma kanx wla expire -> dir `createSession()`
  - F response zid: `Set-Cookie: session_id=<UUID>; Path=/`
  - Incrementi visits counter f session: `session.setAttribute("visits", count + 1)`

🧪 **Milestone Check Nhar 5:**
```bash
# 1. Test Chunked Request:
curl -i -X POST -H "Transfer-Encoding: chunked" \
  --data-binary "Testing Chunked Request Body" \
  http://localhost:8080/upload

# 2. Test Cookies in Browser:
# 7ell http://localhost:8080/ f browser -> f DevTools > Application > Cookies ghatlqa session_id
```

---

## 📅 Nhar 6: CGI Engine (Python & Bash Scripts)

> **🎯 L-Hadaf:** Executer des scripts dynamiques (`.py`, `.sh`) b `ProcessBuilder` bla ma y-t-blocka l-server.

### Dev A (CGI Process Runner & Asynchronous Polling)
- [ ] Créer `src/cgi/CgiHandler.java`:
  - Configurer `ProcessBuilder(interpreter, scriptFile.getAbsolutePath())`
  - Set working directory: `pb.directory(scriptFile.getParentFile())`
  - Injecter CGI Environment Variables:
    - `REQUEST_METHOD`, `PATH_INFO`, `QUERY_STRING`, `CONTENT_LENGTH`, `CONTENT_TYPE`
    - `SERVER_NAME`, `SERVER_PORT`, `SERVER_PROTOCOL`
    - Headers HTTP f format: `HTTP_HEADER_NAME`
- [ ] Sift body l stdin dial script (ila kan POST): `process.getOutputStream().write(body)`
- [ ] F `Server.java`: **Non-blocking CGI Monitoring**:
  - `conn.isWaitingForCgi()`: qra output mn `stdout.available()` bla ma t-blloki
  - Timeout dial 5s: ila فات 5 ثواني dir `process.destroyForcibly()` w rdd **`504 Gateway Timeout`**

### Asta (CGI Scripts & Response Parser)
- [ ] Créer `cgi-bin/hello.py`:
  - Script Python kay-qra `sys.stdin` ila kan POST
  - Kay-tbe3 headers: `Status: 200 OK\r\nContent-Type: application/json\r\n\r\n`
  - Kay-tbe3 JSON fih server info w l-body li wsel
- [ ] Créer `cgi-bin/info.sh` (Bonus dial 2nd CGI interpreter - Bash)
- [ ] F `CgiHandler.java` method `parseCgiResponse()`:
  - Ferreq CGI headers mn CGI body
  - Parse `Status: <code>` w `Content-Type: <type>`

🧪 **Milestone Check Nhar 6:**
```bash
# 1. Test Python GET:
curl -i http://localhost:8080/cgi-bin/hello.py
# 2. Test Python POST (Unchunked):
curl -i -X POST -d "Hello CGI" http://localhost:8080/cgi-bin/hello.py
# 3. Test Python POST (Chunked):
curl -i -X POST -H "Transfer-Encoding: chunked" --data-binary "Chunked CGI" http://localhost:8080/cgi-bin/hello.py
# 4. Test Shell CGI (2nd CGI system):
curl -i http://localhost:8080/cgi-bin/info.sh
```

---

## 📅 Nhar 7: Virtual Hosting, Siege & Audit Defense

> **🎯 L-Hadaf:** Passer Siege b 99.5%+ availability, tester les conflits dial ports, w t-kouno wajdin 100% l l-Audit.

### Dev A (Virtual Hosting & Port Conflict Safety)
- [ ] Implementer Virtual Hosting f `resolveVirtualHost`:
  - Matcher request `Host:` header m3a `server_name`
  - Fallback l `default_server` ila ma kanx match
- [ ] Gérer les conflits dial ports:
  - Ila port t-3awed f nfs l-block -> Affiche warning w zido mra we7da
  - Ila port t-3awed f multiple blocks m3a hostnames mokhtalifin -> Partager socket (Virtual Hosting)
  - Ila OS bind failed f port wa7ed -> Affiche warning w kmml f les autres ports bla crash!
- [ ] Check timeout dial stale connections:
  - F `cleanupTimedOutConnections()`, sdd sockets li fatou 30s bla activity bach ma ybqawx hanging.

### Asta (Admin Dashboard, Automated Test & Siege)
- [ ] Créer `src/utils/Metrics.java`:
  - `totalRequests`, `activeConnections`, `uptime`, `statusCodes` Map
- [ ] Créer `/admin` w `/metrics` endpoints f `Router.java`:
  - `/metrics` -> JSON telemetry
  - `/admin` -> HTML dashboard fih les stats w les sessions actives m3a bouton Invalidate
- [ ] Créer `audit_test.sh`:
  - Script bash automatisé kay-testi ga3 les requirements dial l-audit sheet
- [ ] Stress Test m3a **Siege**:
  ```bash
  siege -b -c 50 -t 30S http://localhost:8080/
  ```
  - Taaked anah: **Availability >= 99.5%** w **Failed transactions: 0**

---

## 📋 Checklist dial l-Audit Sheet (Taakdo mn hado 100%)

### 1. Functional
- [ ] Nqder n-chr7: Kifach HTTP server kay-khdem (Request/Response over TCP)?
- [ ] Nqder n-chr7: Ina function sta3melna f Multiplexing? (`java.nio.channels.Selector` / `epoll`).
- [ ] Wach l-server fih ghir **select wa7ed**? (Iyeh, f `Server.java:mainLoop`).
- [ ] 3lach mohim ghir select wa7ed? (Single thread, zero race conditions, low CPU/RAM, event-driven).
- [ ] Wach kayn **max 1 read w max 1 write** per client per select? (Iyeh, vérifié f `handleRead` w `handleWrite`).
- [ ] Wach les return values dial I/O m-verifyin? (`bytesRead == -1`, `IOException`).
- [ ] Ila wqe3 error f socket, wach l-client kay-t-supprima? (Iyeh, `closeConnection()`).

### 2. Configuration File
- [ ] Single server b single port kheddam?
- [ ] Multiple servers b different ports kheddamin?
- [ ] Multiple servers f nfs l-port b different hostnames (Virtual Hosting) kheddamin?
- [ ] Custom error pages kheddamin?
- [ ] Client max body size limit kheddam (returns 413)?
- [ ] Routes w accepted methods kheddamin (e.g. 405 f DELETE root)?
- [ ] Default file (index.html) kay-t-serva ila kan path dossier?
- [ ] Directory listing toggle kheddam (true/false)?

### 3. Methods & Cookies
- [ ] GET request kheddama (200, 404)?
- [ ] POST request kheddama (201 Created)?
- [ ] DELETE request kheddama (204 No Content)?
- [ ] Wrong request / Garbage data (e.g. `nc`) ma kay-crachich l-server (returns 400)?
- [ ] Fichier uploadé kay-rje3 b GET bla ay corruption?
- [ ] Session w cookies system kheddam (`Set-Cookie`, `session_id`)?

### 4. Browser Interaction
- [ ] L-browser kay-connecta bla 7tta mouchkil?
- [ ] Headers dial Request w Response s7a7 (Keep-Alive, Content-Length, Content-Type)?
- [ ] Wrong URL kay-rdd 404 page?
- [ ] Directory listing bayna f browser?
- [ ] Redirection 301 kheddama f browser?
- [ ] CGI kheddam b chunked w unchunked data?

### 5. Port Issues & Siege
- [ ] Configure nfs l-port multiple times -> l-server kay-lqa l-error w ma kay-crachich?
- [ ] Siege availability >= 99.5% (`siege -b [IP]:[PORT]`)?
- [ ] Zero hanging connections mor stress test?

### 6. Bonuses
- [ ] Kayn kter mn 1 CGI system (Python `.py` + Bash `.sh`)?
- [ ] Kayn Admin Dashboard / Server Metrics endpoint (`/admin`, `/metrics`)?

---

## 🛠️ Commandes dial Test li ghay-nfe3okom

```bash
# Compilation
./build.sh

# Run server
java -cp bin Main config.json

# Automated test suite
./test.sh

# Audit verification test
./audit_test.sh

# Stress test (Siege)
siege -b -c 50 -t 30S http://localhost:8080/
```
