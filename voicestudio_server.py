#!/usr/bin/env python3
"""
VoiceStudio Local Speech Backend
OpenAI-compatible audio speech server running on port 3900.
Generates and serves high-fidelity neural vocalizations for Indian classical tabla bols.
"""

import http.server
import socketserver
import json
import os
import subprocess
import hashlib
import sys
import threading
from urllib.parse import urlparse, parse_qs

PORT = int(os.environ.get("VOICESTUDIO_PORT", 3900))
HOST = "0.0.0.0"

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BOL_DIR = os.path.join(BASE_DIR, "assets", "audio", "bols")
CACHE_DIR = os.path.join(BASE_DIR, "assets", "audio", "cache")
os.makedirs(BOL_DIR, exist_ok=True)
os.makedirs(CACHE_DIR, exist_ok=True)

DEFAULT_VOICE = "hi-IN-MadhurNeural"

VOICE_PROFILES = [
    {"voice_id": "hi-IN-MadhurNeural", "name": "Pandit Madhur (Tabla Maestro)", "language": "hi-IN", "gender": "Male", "category": "Classical"},
    {"voice_id": "hi-IN-SwaraNeural", "name": "Vidushi Swara", "language": "hi-IN", "gender": "Female", "category": "Classical"},
    {"voice_id": "en-IN-PrabhatNeural", "name": "Guru Prabhat", "language": "en-IN", "gender": "Male", "category": "Expressive"},
    {"voice_id": "en-IN-NeerjaExpressiveNeural", "name": "Neerja Expressive", "language": "en-IN", "gender": "Female", "category": "Expressive"},
    # OpenAI compatibility aliases
    {"voice_id": "alloy", "name": "Alloy (Alias -> Madhur)", "language": "hi-IN", "gender": "Male", "category": "Compatibility"},
    {"voice_id": "echo", "name": "Echo (Alias -> Prabhat)", "language": "en-IN", "gender": "Male", "category": "Compatibility"},
    {"voice_id": "shimmer", "name": "Shimmer (Alias -> Swara)", "language": "hi-IN", "gender": "Female", "category": "Compatibility"}
]

VOICE_MAP = {
    "alloy": "hi-IN-MadhurNeural",
    "echo": "en-IN-PrabhatNeural",
    "shimmer": "hi-IN-SwaraNeural",
    "fable": "hi-IN-MadhurNeural",
    "onyx": "en-IN-PrabhatNeural",
    "nova": "hi-IN-SwaraNeural"
}

# Phonetic Devanagari translation for tabla bols calibrated to classical Padhant acoustics
DEV_MAP = {
    "dha": "धा!", "dhaa": "धा!",
    "dhin": "धिन्", "dheen": "धिन्",
    "tin": "तिन्", "teen": "तिन्",
    "ta": "ता!", "taa": "ता!",
    "na": "ना!", "naa": "ना!",
    "ge": "गे", "gay": "गे", "ghe": "घे",
    "kat": "कत!", "qat": "कत!",
    "ke": "के!", "kay": "के!",
    "te": "ते", "tay": "ते",
    "re": "रे", "ray": "रे",
    "ti": "ति", "tee": "ति",
    "tu": "तू", "too": "तू",
    "dhi": "धी", "dhee": "धी",
    "dheet": "धीट", "dhit": "धीट",
    "tak": "तक", "ka": "का", "nak": "नक",
    "tite": "तिटे", "tete": "तेते",
    "tirakita": "तिरकिट", "tirkita": "तिरकिट",
    "dhage": "धागे", "dhagena": "धागेना",
    "dhagenete": "धागेनेते", "dhakita": "धाकिटा",
    "dhati": "धाती", "dhina": "धीना", "dhinna": "धिन्ना",
    "dhilang": "धिलांग", "gadigana": "गदिगन",
    "gena": "गेना", "katte": "कत्ते", "kattete": "कततेते",
    "katta": "कतता", "kata": "कत", "kena": "केना",
    "kittak": "किटतक", "kran": "क्रान", "kraan": "क्रान",
    "nadha": "नाधा", "nake": "नाके", "nati": "नाती",
    "naka": "नाका", "takita": "ताकिटा", "take": "ताके",
    "takena": "ताकेना", "tati": "ताती", "tatirakita": "तातिरकिट",
    "taktirakita": "तकतिरकिट", "tina": "तीना", "tinak": "तिनक",
    "dhinak": "धिनक", "traang": "त्रांग"
}

class ThreadedHTTPServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

class VoiceStudioHandler(http.server.BaseHTTPRequestHandler):
    def _send_cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")

    def do_OPTIONS(self):
        self.send_response(204)
        self._send_cors()
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path in ("/health", "/healthz"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self._send_cors()
            self.end_headers()
            self.wfile.write(json.dumps({
                "status": "ok",
                "service": "VoiceStudio",
                "version": "0.5.8",
                "engine": "Neural-TTS-Edge",
                "default_voice": DEFAULT_VOICE,
                "cached_bols": len([f for f in os.listdir(BOL_DIR) if f.endswith('.mp3')])
            }).encode("utf-8"))
            return

        if path == "/v1/audio/voices":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self._send_cors()
            self.end_headers()
            self.wfile.write(json.dumps({"voices": VOICE_PROFILES}).encode("utf-8"))
            return

        if path == "/manifest":
            manifest_file = os.path.join(BOL_DIR, "manifest.json")
            if os.path.exists(manifest_file):
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self._send_cors()
                self.end_headers()
                with open(manifest_file, "rb") as f:
                    self.wfile.write(f.read())
            else:
                self.send_response(404)
                self._send_cors()
                self.end_headers()
                self.wfile.write(b'{"error": "Manifest not generated yet"}')
            return

        self.send_response(404)
        self._send_cors()
        self.end_headers()
        self.wfile.write(b'{"error": "Endpoint not found"}')

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path not in ("/v1/audio/speech", "/speech"):
            self.send_response(404)
            self._send_cors()
            self.end_headers()
            self.wfile.write(b'{"error": "Endpoint not found"}')
            return

        content_len = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_len)

        try:
            req = json.loads(post_data.decode("utf-8"))
        except Exception:
            self.send_response(400)
            self._send_cors()
            self.end_headers()
            self.wfile.write(b'{"error": "Invalid JSON body"}')
            return

        text = req.get("input", "").strip()
        voice = req.get("voice", DEFAULT_VOICE)
        voice = VOICE_MAP.get(voice, voice)
        speed = req.get("speed", 1.0)
        fmt = req.get("response_format", "mp3")

        if not text:
            self.send_response(400)
            self._send_cors()
            self.end_headers()
            self.wfile.write(b'{"error": "Input text is required"}')
            return

        clean_text = text.lower().replace("-", "").replace(" ", "")
        
        # 1. Check pre-rendered bol bank
        preset_file = os.path.join(BOL_DIR, f"{clean_text}.mp3")
        if os.path.exists(preset_file):
            self.send_response(200)
            self.send_header("Content-Type", "audio/mpeg")
            self.send_header("X-VoiceStudio-Cache", "HIT-PRESET")
            self._send_cors()
            self.end_headers()
            with open(preset_file, "rb") as f:
                self.wfile.write(f.read())
            return

        # 2. Check dynamic runtime cache
        h = hashlib.md5(f"{text}_{voice}_{speed}_{fmt}".encode("utf-8")).hexdigest()
        cache_file = os.path.join(CACHE_DIR, f"{h}.mp3")
        if os.path.exists(cache_file):
            self.send_response(200)
            self.send_header("Content-Type", "audio/mpeg")
            self.send_header("X-VoiceStudio-Cache", "HIT-DYNAMIC")
            self._send_cors()
            self.end_headers()
            with open(cache_file, "rb") as f:
                self.wfile.write(f.read())
            return

        # 3. Generate on-the-fly
        # Determine phonetic articulation
        pronounce_text = DEV_MAP.get(clean_text, text)
        rate_percent = f"{int((speed - 1.0) * 100):+d}%" if speed != 1.0 else "+18%"

        temp_raw = os.path.join(CACHE_DIR, f"temp_{h}.mp3")
        try:
            cmd1 = [
                "edge-tts",
                "--voice", voice,
                f"--rate={rate_percent}",
                "--text", pronounce_text,
                "--write-media", temp_raw
            ]
            subprocess.run(cmd1, capture_output=True, check=True)

            # Silence trimming and normalization
            cmd2 = [
                "ffmpeg", "-y", "-i", temp_raw,
                "-af", "silenceremove=start_periods=1:start_duration=0.01:start_threshold=-45dB:detection=peak,areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-45dB:detection=peak,areverse,loudnorm=I=-14:TP=-1.5:LRA=7",
                cache_file
            ]
            subprocess.run(cmd2, capture_output=True, check=True)
            if os.path.exists(temp_raw):
                os.remove(temp_raw)

            self.send_response(200)
            self.send_header("Content-Type", "audio/mpeg")
            self.send_header("X-VoiceStudio-Cache", "MISS")
            self._send_cors()
            self.end_headers()
            with open(cache_file, "rb") as f:
                self.wfile.write(f.read())
        except Exception as e:
            self.send_response(500)
            self._send_cors()
            self.end_headers()
            self.wfile.write(json.dumps({"error": str(e)}).encode("utf-8"))

def run():
    server = ThreadedHTTPServer((HOST, PORT), VoiceStudioHandler)
    print(f"VoiceStudio server active on http://localhost:{PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

if __name__ == "__main__":
    run()
