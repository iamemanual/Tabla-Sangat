# Tabla Artist — Rhythm & Tuning Companion

All-in-one Indian classical Tabla companion application featuring real-time interactive playback, acoustic chromatic tuning, continuous Tanpura drone, and pedagogical theory repository.

---

## 🌟 Core Features

- **Taal Parichaya & Theka Loop Engine:**
  - Full interactive support for canonical taals (Teentaal, Dadra, Keherwa, Rupak, Jhaptal, Ektaal, Deepchandi, Chaukaal).
  - High-precision Web Audio scheduling with adjustable tempo (BPM), matra auditioning, and Sam/Taali/Khali markers.
  - Composition variables support (Kayda, Rela, Tukda, Tihayi, Chakradar) with multi-speed Layakari (Barabar, Dugun, Chaugun).

- **VoiceStudio Neural Bol Voice & Acoustic Drum Stroke Engine:**
  - 51+ classical Hindustani bols generated via VoiceStudio neural TTS with natural Padhant cadence.
  - Zero-latency Web Audio playback with Freesound 8162 authentic acoustic tabla samples (`Dha`, `Dhin`, `Tin`, `Ta`, `Na`, `Ge`, `Kat`, `Te`, `Re`).
  - Three audio playback modes: **AI Voice + Drum**, **AI Voice Only**, **Drum Only**.

- **Tabla Tuner High-Precision Chromatic Tabla Tuner:**
  - Dedicated acoustic filtering modes for **Dayan** (Treble 100–480 Hz), **Bayan** (Bass 28–200 Hz), **Deep Voice / Kharaj** (25–360 Hz), and **Wideband**.
  - Dual autocorrelation and spectral peak tracking capable of capturing low-frequency voice and bass dagga strokes with sub-Hz accuracy.
  - Interactive Pitch Pipe reference tone generator for C, C#, D, D#, E, F, F#, G, G#, A, A#, B.

- **Concert Tanpura Drone:**
  - Phase-continuous additive harmonic synthesis emulating real Indian Jawari resonance (Rich, Concert, Subtle).
  - Tunable to Sa-Pa, Sa-Ma, and Sa-Ni with Kharaj Sa foundation.

---

## 📂 Project Architecture

```
├── index.html               # Web Application entry point
├── preview.html             # Mirror preview build
├── server.js                # Node.js local HTTP server (Port 8080)
├── serve.sh                 # Server lifecycle manager
├── voicestudio_server.py    # Local VoiceStudio Neural Bol API (Port 3900)
├── start_voicestudio.sh     # VoiceStudio backend runner
├── generate_bol_bank.py     # Master bol bank synthesis utility
├── pinpoint.js              # Interactive visual DOM inspector
├── assets/
│   └── audio/
│       ├── bols/            # 51+ Neural Bol master audio files & manifest
│       └── strokes/         # Authentic Freesound 8162 acoustic samples
└── app/                     # Native Android Jetpack Compose app
    └── src/main/java/com/tablaartist/app/
        ├── audio/           # TablaAudioEngine (CarlTune & Tanpura)
        ├── ui/screens/      # Jetpack Compose UI screens
        └── data/            # Taal and composition seed data
```

---

## 🚀 Quick Start (Termux / Linux)

### 1. Launch Web Server
```bash
./serve.sh start
# App is available at http://localhost:8080/
```

### 2. Start VoiceStudio Backend
```bash
./start_voicestudio.sh start
# Backend runs at http://localhost:3900/
```

---

## 📜 License

MIT License. Designed and developed by **iamemanual**.
