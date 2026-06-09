import os
import sys

# CGI requires printing headers first, then an empty line (\r\n)
print("Content-Type: text/html\r\n\r\n", end="")

print("<html><body>")
print("<h2>🚀 Hello from Python CGI!</h2>")

# Print Environment Variables sent by Java
print("<h3>Environment Variables:</h3>")
print("<ul>")
print(f"<li><b>REQUEST_METHOD:</b> {os.environ.get('REQUEST_METHOD', 'N/A')}</li>")
print(f"<li><b>PATH_INFO:</b> {os.environ.get('PATH_INFO', 'N/A')}</li>")
print(f"<li><b>HTTP_USER_AGENT:</b> {os.environ.get('HTTP_USER_AGENT', 'N/A')}</li>")
print("</ul>")

# Print the body if it's a POST request
body = sys.stdin.read()
if body:
    print("<h3>Received Body:</h3>")
    print(f"<p>{body}</p>")

print("</body></html>")