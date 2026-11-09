#!/usr/bin/env python3
"""TV box kumanda köprüsü: box'tan gelen HTTP isteğini adb input'a çevirir.
/back -> DPAD_LEFT (geri sar), /fwd -> DPAD_RIGHT (ileri sar)"""
import subprocess, time
from http.server import BaseHTTPRequestHandler, HTTPServer

BOX_IP = "192.168.1.101"
ADB_TARGET = "192.168.1.101:5555"
KEYMAP = {"/back": "21", "/fwd": "22"}

class H(BaseHTTPRequestHandler):
    def do_GET(self):
        with open("/tmp/tvbox/hits.log", "a") as f:
            f.write(f"{time.strftime('%H:%M:%S')} {self.client_address[0]} {self.path}\n")
        if self.client_address[0] != BOX_IP or self.path not in KEYMAP:
            self.send_response(403); self.end_headers(); return
        subprocess.Popen(["adb", "-s", ADB_TARGET, "shell", "input", "keyevent", KEYMAP[self.path]],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.send_response(200); self.end_headers()
    def log_message(self, *a): pass

HTTPServer(("0.0.0.0", 7575), H).serve_forever()
