import requests
import os

url = "http://localhost:8080/api/v1/documents/upload"
params = {"sourceId": 1}
file_path = "sample.txt"

print("=== REQUEST ===")
print(f"Method: POST")
print(f"URL: {url}")
print(f"Query Params: {params}")
print(f"File: {file_path} (size: {os.path.getsize(file_path)} bytes)")
print("Headers: multipart/form-data boundary will be auto-generated")

try:
    with open(file_path, "rb") as f:
        files = {"file": (file_path, f, "text/plain")}
        response = requests.post(url, params=params, files=files)
        
    print("\n=== RESPONSE ===")
    print(f"HTTP Status: {response.status_code} {response.reason}")
    print(f"Response Headers: {dict(response.headers)}")
    print(f"Response Body: {response.text}")

except Exception as e:
    print(f"Error during request: {e}")
