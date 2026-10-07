#!/usr/bin/env python3
import os
import sys
import json

def main():
    method = os.environ.get("REQUEST_METHOD", "GET")
    query_string = os.environ.get("QUERY_STRING", "")
    content_length = os.environ.get("CONTENT_LENGTH", "0")
    content_type = os.environ.get("CONTENT_TYPE", "")
    
    body = ""
    if method == "POST":
        try:
            length = int(content_length)
            if length > 0:
                body = sys.stdin.read(length)
            else:
                body = sys.stdin.read()
        except Exception:
            body = sys.stdin.read()

    # Collect interesting CGI environment variables
    env_vars = {k: v for k, v in os.environ.items() if k.startswith("HTTP_") or k.startswith("SERVER_") or k.startswith("REQUEST_") or k in ("QUERY_STRING", "PATH_INFO", "CONTENT_LENGTH", "CONTENT_TYPE")}

    response_data = {
        "status": "success",
        "message": "Hello from Python CGI!",
        "method": method,
        "query_string": query_string,
        "body_received": body,
        "environment": env_vars
    }

    # Print standard CGI headers
    sys.stdout.write("Status: 200 OK\r\n")
    sys.stdout.write("Content-Type: application/json\r\n")
    sys.stdout.write("\r\n")
    sys.stdout.write(json.dumps(response_data, indent=2) + "\n")

if __name__ == "__main__":
    main()
