#!/bin/bash

echo -ne "Status: 200 OK\r\n"
echo -ne "Content-Type: text/html; charset=UTF-8\r\n"
echo -ne "\r\n"

cat << HTML
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CGI Bash System Info</title>
    <style>
        body { font-family: sans-serif; background: #0f172a; color: #f8fafc; padding: 30px; }
        .box { background: #1e293b; padding: 25px; border-radius: 10px; border: 1px solid #334155; }
        h1 { color: #38bdf8; }
        pre { background: #0f172a; padding: 15px; border-radius: 6px; color: #4ade80; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="box">
        <h1>🐚 Bash CGI Script Output</h1>
        <p><strong>Method:</strong> ${REQUEST_METHOD:-GET}</p>
        <p><strong>Path Info:</strong> ${PATH_INFO:-/}</p>
        <p><strong>Query String:</strong> ${QUERY_STRING:-(none)}</p>
        <p><strong>Server Time:</strong> $(date)</p>
        <p><strong>Kernel Info:</strong> $(uname -srm)</p>
        <hr style="border-color: #334155;">
        <h3>Environment Variables:</h3>
        <pre>$(env | grep -E '^(HTTP_|REQUEST_|SERVER_|QUERY_|PATH_|CONTENT_)' | sort)</pre>
    </div>
</body>
</html>
HTML
