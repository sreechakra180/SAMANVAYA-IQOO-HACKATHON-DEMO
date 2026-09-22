# SAMANVAYA
### Offline Multimodal Accessibility Copilot
#### iQOO Hackathon 2026 — Hyderabad | Team Sukshma | Track: HealthTech

---

> **"SAMANVAYA turns your iQOO into an offline AI guide for people who cannot see — no internet required, no data shared, no compromise."**

---

## The Problem

India has **10+ million visually impaired people**. Every smartphone-based accessibility tool that exists today — Google Lookout, Seeing AI, Envision — **requires internet**. They fail silently in:

- Rural primary health centres
- Hospital basements  
- Village streets with no mobile data
- Anywhere network reliability cannot be guaranteed

A visually impaired person picking up a medicine bottle in a rural clinic has no way to know what it is, what the dose is, or if it conflicts with other medications — **unless a sighted person is physically present**.

---

## The Solution

SAMANVAYA uses the iQOO 15's Snapdragon NPU to run a complete multimodal AI pipeline **entirely on-device**, with **zero cloud dependency**.

```
Camera → Vision Model → RAG Retrieval → Reasoning → Voice Response
   ↑                                                        ↑
Sensors (motion/proximity)                          Session Memory
```

**Point. Ask. Understand.** That's the entire interaction.

---

## Architecture

```
┌─────────────────────────────────────────┐
│              iQOO 15 (Phone)            │
│                                         │
│  Camera (50MP) → MiniCPM-V 4.6 (1.3B) │  ← Primary VLM (NPU)
│                → Florence-2 (230MB)    │  ← Fallback VLM (fast)
│                                         │
│  Microphone → Whisper-tiny (39MB)      │  ← Local STT
│                                         │
│  FAISS + MiniLM → JSON KB lookup       │  ← Offline RAG
│                                         │
│  Qwen2-1.5B (GGUF Q4) reasoning        │  ← Local LLM
│  Rule-based fallback                   │  ← Always available
│                                         │
│  Android TTS (Hindi/Telugu/English)    │  ← Offline TTS
│  Accelerometer + Gyroscope             │  ← Motion context
│  Haptics (INFO/WARNING/CRITICAL)       │  ← Non-visual alerts
│  SQLite (Room) session memory          │  ← 5-turn context
└────────────────┬────────────────────────┘
                 │ WebSocket (local Wi-Fi)
                 ▼
┌─────────────────────────────────────────┐
│         Laptop — Office Kit Dashboard   │
│   FastAPI + React + FAISS semantic RAG  │
│   Session timeline, latency, confidence │
└─────────────────────────────────────────┘
```

---

## Core Demo Scenarios

### Scenario A — Medicine Identification
```
User: "Yeh kya hai?" (wearing sleep mask)
System: "This is Paracetamol 500mg. Adult dose: 1-2 tablets every 4-6 hours."
[Airplane mode ON — system still works]
```

### Scenario B — Hazard Navigation  
```
Camera detects: High Voltage sign
System: "Warning. High voltage electrical hazard. Do not touch. Keep 1 metre distance."
[Haptic: CRITICAL vibration]
```

### Scenario C — Contextual Memory
```
User: "Maine tumhe pehle kya dikhaya tha?" (What did I show you earlier?)
System: "In this session I identified Paracetamol 500mg and a High Voltage warning sign."
[Cross-frame reasoning from session memory]
```

---

## Local AI Stack

| Component | Model | Size | Latency | Status |
|-----------|-------|------|---------|--------|
| Primary VLM | MiniCPM-V 4.6 | 1.3B (GGUF Q4) | 2-4s | Milestone 4 |
| Fallback VLM | Florence-2 Base | 230MB | <0.5s | Milestone 4 |
| STT | Whisper-tiny | 39MB | <0.5s | Milestone 3 |
| LLM | Qwen2-1.5B Q4 | 900MB | 0.8-1.5s | Milestone 6 |
| Embeddings | all-MiniLM-L6-v2 | 22MB | <10ms | Backend |
| TTS | Android native | 0MB | Real-time | ✅ Done |
| RAG | JSON keyword | Local | <50ms | ✅ Done |

---

## Project Structure

```
SAMANVAYA/
├── android/                  ← Android app (Kotlin + Compose)
│   └── app/src/main/java/com/sukshma/samanvaya/
│       ├── core/             ← SamanvayaEngine (main pipeline)
│       ├── vision/           ← VisionEngine interface + StubVisionEngine
│       ├── speech/           ← STT + TTS engines
│       ├── rag/              ← JsonRagEngine (on-device)
│       ├── reasoning/        ← ReasoningEngine + RuleBasedFallback
│       ├── memory/           ← Room database + SessionMemoryRepository
│       ├── sensors/          ← SensorContextProvider
│       ├── accessibility/    ← HapticEngine
│       ├── core/             ← NetworkGuard
│       └── ui/               ← 5 screens (Home, LiveAssist, Result, Memory, Diagnostics)
├── backend/                  ← FastAPI + FAISS (Office Kit dashboard)
├── dashboard/                ← React dashboard (TODO Milestone 11)
├── data/                     ← Knowledge bases (medicines, hazards, navigation)
├── models/                   ← Model weights (download separately — too large for git)
├── scripts/                  ← Build and demo scripts
└── docs/                     ← Architecture, setup, safety, demo guide
```

---

## Setup

### Android App

```bash
# 1. Ensure Android SDK 34+ and Java 17 are installed
# 2. Open android/ in Android Studio
# 3. Or build via command line:

cd android
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Backend (Office Kit Dashboard)

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
# Dashboard: http://localhost:8000
```

### Model Downloads (do BEFORE hackathon)

```bash
# Run from project root
python scripts/download_models.py

# Or manually:
# MiniCPM-V 4.6 GGUF Q4: huggingface.co/openbmb/MiniCPM-V-4.6-gguf
# Florence-2: huggingface.co/microsoft/Florence-2-base
# Whisper-tiny: via whisper.cpp Android build
# Qwen2-1.5B Q4: huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF
```

---

## Offline Validation

To verify the system is fully offline:

1. Install APK on iQOO 15
2. Enable Airplane Mode
3. Confirm Wi-Fi OFF, Mobile Data OFF
4. Open SAMANVAYA
5. Point camera at medicine bottle
6. Ask: "What is this?"
7. ✅ Response should arrive in <5 seconds with no network

The Diagnostics screen shows:
- `Internet calls: 0`
- `External connections: BLOCKED`
- `Status: OFFLINE (LOCAL ONLY)`

---

## Safety Disclaimer

**SAMANVAYA is an accessibility prototype, not a medical device.**

- It does NOT provide medical diagnosis
- It does NOT provide treatment recommendations
- It does NOT provide drug interaction analysis
- All medicine information is sourced from the WHO Essential Medicines List and CDSCO

When confidence is below threshold, SAMANVAYA says:  
*"I cannot verify enough information to provide safe guidance. Please consult a pharmacist or doctor."*

**Never use SAMANVAYA as a replacement for professional medical advice.**

---

## Competition Context

- **Competition:** iQOO Hackathon 2026 — Hyderabad City Battle
- **Date:** September 26–27, 2026
- **Track:** HealthTech
- **Team:** Sukshma / SAMANVAYA

---

## Build Milestones

- [x] M1: Project shell, all screens, architecture
- [ ] M2: CameraX live camera preview
- [ ] M3: Voice input (AudioRecord + Whisper STT)
- [ ] M4: Local VLM (MiniCPM-V / Florence-2)
- [ ] M5: Offline RAG integration
- [ ] M6: Local LLM (Qwen2-1.5B)
- [ ] M7: Full audio loop (TTS response)
- [ ] M8: Session memory integration
- [ ] M9: Sensors + haptics
- [ ] M10: Airplane mode validation
- [ ] M11: Office Kit dashboard
- [ ] M12: Demo hardening
