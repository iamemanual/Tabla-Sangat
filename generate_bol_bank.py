import subprocess
import os
import json
import re

OUT_DIR = "/storage/emulated/0/Workspace/Tabla App/assets/audio/bols"
SCRATCH_DIR = "/data/data/com.termux/files/home/.gemini/antigravity-cli/brain/59bf1512-feb8-4a77-a2e1-767849f92052/scratch/bol_gen"
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(SCRATCH_DIR, exist_ok=True)

VOICE = "hi-IN-MadhurNeural"

# Bol definitions: (filename_key, devanagari_text, speed_rate, aliases)
BOL_SPECS = [
    # Core single bols calibrated to classical Padhant acoustics
    ("dha", "धा!", "+18%", ["dha", "dhaa", "धा", "dha."]),
    ("dhin", "धिन्", "+16%", ["dhin", "धिन"]),
    ("tin", "तिन्", "+18%", ["tin", "तिन", "teen"]),
    ("ta", "ता!", "+20%", ["ta", "taa", "ता"]),
    ("na", "ना!", "+20%", ["na", "naa", "ना"]),
    ("ge", "गे", "+15%", ["ge", "gay", "गे", "घे", "ghe"]),
    ("ghe", "घे", "+15%", ["ghe", "घे"]),
    ("kat", "कत!", "+24%", ["kat", "कत", "qat"]),
    ("ke", "के!", "+24%", ["ke", "kay", "के"]),
    ("te", "ते", "+25%", ["te", "tay", "ते", "टे"]),
    ("re", "रे", "+25%", ["re", "ray", "रे"]),
    ("ti", "ति", "+25%", ["ti", "tee", "ति"]),
    ("tu", "तू", "+18%", ["tu", "too", "तू", "तु"]),
    ("dhi", "धी", "+15%", ["dhi", "dhee", "धी"]),
    ("dheet", "धीट", "+15%", ["dheet", "dhit", "धीट"]),
    ("tak", "तक", "+25%", ["tak", "तक"]),
    ("ka", "का", "+25%", ["ka", "का"]),
    ("nak", "नक", "+25%", ["nak", "नक"]),
    
    # Compound & fast phrases
    ("tite", "तिटे", "+22%", ["tite", "ti-te", "तिटे", "tete", "तेते"]),
    ("tirakita", "तिरकिट", "+25%", ["tirakita", "tirkita", "तिरकिट", "तिरकिटा", "tirakita2"]),
    ("dhage", "धागे", "+18%", ["dhage", "धागे"]),
    ("dhagena", "धागेना", "+18%", ["dhagena", "धागेना"]),
    ("dhagenete", "धागेनेते", "+22%", ["dhagenete", "धागेनेते"]),
    ("dhakita", "धाकिटा", "+20%", ["dhakita", "धाकिटा"]),
    ("dhati", "धाती", "+18%", ["dhati", "धाती"]),
    ("dhina", "धीना", "+18%", ["dhina", "धीना"]),
    ("dhinna", "धिन्ना", "+18%", ["dhinna", "धिन्ना"]),
    ("dhilang", "धिलांग", "+18%", ["dhilang", "धिलांग"]),
    ("gadigana", "गदिगन", "+22%", ["gadigana", "गदिगन"]),
    ("gena", "गेना", "+20%", ["gena", "गेना"]),
    ("katte", "कत्ते", "+22%", ["katte", "कत्ते"]),
    ("kattete", "कततेते", "+22%", ["kattete", "कततेते"]),
    ("katta", "कतता", "+22%", ["katta", "कतता"]),
    ("kata", "कत", "+22%", ["kata", "कत"]),
    ("kena", "केना", "+20%", ["kena", "केना"]),
    ("kittak", "किटतक", "+22%", ["kittak", "किटतक"]),
    ("kran", "क्रान", "+15%", ["kran", "kraan", "क्रान"]),
    ("nadha", "नाधा", "+20%", ["nadha", "नाधा"]),
    ("nake", "नाके", "+20%", ["nake", "नाके"]),
    ("nati", "नाती", "+20%", ["nati", "नाती"]),
    ("naka", "नाका", "+20%", ["naka", "नाका"]),
    ("takita", "ताकिटा", "+22%", ["takita", "ताकिटा"]),
    ("take", "ताके", "+22%", ["take", "ताके"]),
    ("takena", "ताकेना", "+20%", ["takena", "ताकेना"]),
    ("tati", "ताती", "+20%", ["tati", "ताती"]),
    ("tatirakita", "तातिरकिट", "+25%", ["tatirakita", "तातिरकिट"]),
    ("taktirakita", "तकतिरकिट", "+25%", ["taktirakita", "तकतिरकिट"]),
    ("tina", "तीना", "+20%", ["tina", "तीना"]),
    ("tinak", "तिनक", "+20%", ["tinak", "तिनक"]),
    ("dhinak", "धिनक", "+18%", ["dhinak", "धिनक"]),
    ("traang", "त्रांग", "+15%", ["traang", "त्रांग"]),
]

manifest = {
    "engine": "VoiceStudio-Neural",
    "voice": VOICE,
    "version": "1.0.0",
    "mappings": {},
    "durations": {}
}

print(f"Generating {len(BOL_SPECS)} VoiceStudio bol master files...")

for key, devanagari, rate, aliases in BOL_SPECS:
    raw_path = os.path.join(SCRATCH_DIR, f"raw_{key}.mp3")
    final_path = os.path.join(OUT_DIR, f"{key}.mp3")
    
    # Generate speech with edge-tts
    cmd_tts = [
        "edge-tts",
        "--voice", VOICE,
        f"--rate={rate}",
        "--text", devanagari,
        "--write-media", raw_path
    ]
    subprocess.run(cmd_tts, capture_output=True)
    
    # Silence trimming and audio mastering (-14 LUFS) with ffmpeg
    cmd_ffmpeg = [
        "ffmpeg", "-y", "-i", raw_path,
        "-af", "silenceremove=start_periods=1:start_duration=0.01:start_threshold=-45dB:detection=peak,areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-45dB:detection=peak,areverse,loudnorm=I=-14:TP=-1.5:LRA=7",
        final_path
    ]
    subprocess.run(cmd_ffmpeg, capture_output=True)
    
    # Probe duration
    cmd_probe = [
        "ffprobe", "-v", "quiet",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        final_path
    ]
    res = subprocess.run(cmd_probe, capture_output=True, text=True)
    dur = float(res.stdout.strip() or 0)
    manifest["durations"][key] = round(dur, 3)
    
    # Map all aliases
    for alias in aliases:
        manifest["mappings"][alias.lower()] = f"{key}.mp3"
        manifest["mappings"][alias] = f"{key}.mp3"
        
    print(f"  ✓ {key:<12} ({devanagari}): {dur:.3f}s -> {key}.mp3")

manifest_file = os.path.join(OUT_DIR, "manifest.json")
with open(manifest_file, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)

print(f"\nCompleted! Generated {len(BOL_SPECS)} files and saved manifest to {manifest_file}.")
