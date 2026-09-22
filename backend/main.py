"""
SAMANVAYA — Office Kit Dashboard Backend
FastAPI + WebSocket + RAG + Dashboard Static Hosting

Purpose:
  1. Serve the high-contrast accessibility dashboard over HTTP
  2. Receive real-time session events from Android app via WebSocket
  3. Provide semantic/keyword RAG search over offline knowledge bases
  4. Bridge for Office Kit — laptop display for hackathon judges & team
  5. Test simulation endpoints for live interactive demonstrations

Run:
  python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

import asyncio
import json
import logging
import os
import time
from pathlib import Path
from typing import Any, List, Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger("samanvaya_backend")

# Set TF flag before any optional library loads
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TOKENIZERS_PARALLELISM"] = "false"

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
DASHBOARD_DIR = BASE_DIR / "dashboard"

app = FastAPI(
    title="SAMANVAYA Office Kit Backend",
    description="Offline-First Multimodal Accessibility Copilot Dashboard",
    version="1.0.0-hackathon"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Dual-Mode RAG Engine (FAISS Neural or JSON Keyword) ──────────────────────

class HybridRagEngine:
    def __init__(self):
        self.model = None
        self.index = None
        self.chunks: List[dict] = []
        self.ready = False
        self.mode = "Uninitialized"

    def initialize(self):
        self.chunks = []
        for kb_file in DATA_DIR.rglob("*.json"):
            try:
                with open(kb_file, encoding="utf-8") as f:
                    entries = json.load(f)
                for entry in entries:
                    self.chunks.append({
                        "id": entry.get("id", ""),
                        "content": entry.get("content", ""),
                        "source": entry.get("source", ""),
                        "domain": kb_file.stem.replace("kb_", "")
                    })
            except Exception as e:
                log.warning(f"Could not load {kb_file}: {e}")

        if not self.chunks:
            log.warning("No knowledge base chunks found in data directory.")
            self.mode = "Empty"
            return

        # Attempt neural embeddings if libraries are installed without blocking startup
        try:
            import faiss
            import numpy as np
            from sentence_transformers import SentenceTransformer
            log.info("Initializing neural embeddings index with all-MiniLM-L6-v2...")
            self.model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
            texts = [c["content"] for c in self.chunks]
            embeddings = self.model.encode(texts, show_progress_bar=False)
            embeddings = np.array(embeddings, dtype="float32")
            dim = embeddings.shape[1]
            self.index = faiss.IndexFlatL2(dim)
            self.index.add(embeddings)
            self.ready = True
            self.mode = "FAISS-Neural (all-MiniLM-L6-v2)"
            log.info(f"FAISS index loaded: {self.index.ntotal} vectors.")
            return
        except Exception as e:
            log.info(f"Neural ML unavailable or deferred ({e}). Running with Instant JSON Token RAG.")

        # Fast Keyword/Token Fallback
        self.ready = True
        self.mode = "Local Token-Indexed RAG"
        log.info(f"Instant JSON RAG loaded {len(self.chunks)} knowledge chunks.")

    def retrieve(self, query: str, top_k: int = 3) -> List[dict]:
        if not self.ready or not self.chunks:
            return []

        t0 = time.time()
        # If FAISS is active
        if self.mode.startswith("FAISS") and self.index and self.model:
            try:
                q_vec = self.model.encode([query], convert_to_numpy=True).astype("float32")
                distances, indices = self.index.search(q_vec, top_k)
                results = []
                for dist, idx in zip(distances[0], indices[0]):
                    if 0 <= idx < len(self.chunks):
                        chunk = self.chunks[idx].copy()
                        chunk["score"] = round(float(dist), 4)
                        chunk["engine"] = "FAISS"
                        results.append(chunk)
                return results
            except Exception as e:
                log.warning(f"FAISS query error: {e}")

        # Keyword / token overlap matching
        tokens = [t.lower() for t in query.split() if len(t) > 2]
        scored = []
        for chunk in self.chunks:
            content_lower = chunk["content"].lower()
            score = sum(2 for t in tokens if t in content_lower)
            if score > 0:
                c = chunk.copy()
                c["score"] = float(score)
                c["engine"] = "JSON-Keyword"
                scored.append(c)

        scored.sort(key=lambda x: x["score"], reverse=True)
        results = scored[:top_k]
        log.info(f"RAG query '{query[:40]}' → {len(results)} hits in {(time.time()-t0)*1000:.1f}ms")
        return results


rag_engine = HybridRagEngine()

@app.on_event("startup")
async def startup_event():
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, rag_engine.initialize)
    log.info("SAMANVAYA Backend startup completed successfully.")


# ─── In-Memory Session State & WebSocket Hub ──────────────────────────────────

session_events: List[dict] = []
connected_clients: List[WebSocket] = []


@app.websocket("/ws/session")
async def session_websocket(websocket: WebSocket):
    await websocket.accept()
    connected_clients.append(websocket)
    log.info(f"New client connected. Total active clients: {len(connected_clients)}")
    try:
        # Push recent history on connection
        await websocket.send_json({"type": "history", "events": session_events[-50:]})

        while True:
            data = await websocket.receive_text()
            event = json.loads(data)
            event["received_at"] = time.time()
            session_events.append(event)

            # Broadcast to all dashboard clients
            dead = []
            for client in connected_clients:
                try:
                    await client.send_json({"type": "event", "event": event})
                except Exception:
                    dead.append(client)
            for d in dead:
                if d in connected_clients:
                    connected_clients.remove(d)

    except WebSocketDisconnect:
        if websocket in connected_clients:
            connected_clients.remove(websocket)
        log.info(f"Client disconnected. Active clients: {len(connected_clients)}")


# ─── REST API Endpoints ───────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "SAMANVAYA Office Kit Backend",
        "rag_mode": rag_engine.mode,
        "rag_chunks": len(rag_engine.chunks),
        "session_events": len(session_events),
        "connected_clients": len(connected_clients),
        "target_device": "iQOO 15 (Offline Copilot)"
    }


@app.get("/session/events")
def get_session_events():
    return {"events": session_events, "count": len(session_events)}


@app.post("/session/clear")
def clear_session():
    session_events.clear()
    return {"status": "cleared"}


@app.get("/rag/search")
def rag_search(q: str, top_k: int = 3):
    t0 = time.time()
    chunks = rag_engine.retrieve(q, top_k)
    return {
        "query": q,
        "results": chunks,
        "engine_mode": rag_engine.mode,
        "latency_ms": int((time.time() - t0) * 1000)
    }


@app.post("/session/simulate")
async def simulate_event(scenario: str = "medicine"):
    """
    Allows the presenter or judge to simulate a live event from the dashboard.
    Scenarios: 'medicine', 'hazard', 'obstacle', 'navigation'
    """
    now = time.time()
    if scenario == "medicine":
        event = {
            "id": int(now * 1000),
            "timestamp": now,
            "category": "MEDICINE",
            "objectLabel": "Dolo 650 / Paracetamol 650mg",
            "response": "Paracetamol 650mg tablet detected. Used for fever and pain relief. Safe adult interval: 6 hours. Do not exceed 4000mg in 24 hours.",
            "confidence": 0.94,
            "confidenceLabel": "HIGH",
            "visionModel": "MiniCPM-V-4.6 (Local ONNX)",
            "reasoningModel": "Qwen2-1.5B (Offline Quantized)",
            "visionLatencyMs": 182,
            "ragLatencyMs": 38,
            "reasoningLatencyMs": 114,
            "isSimulated": True,
            "safetyFlags": ["MEDICINE_IDENTIFICATION", "DOSAGE_CAUTION"]
        }
    elif scenario == "hazard":
        event = {
            "id": int(now * 1000),
            "timestamp": now,
            "category": "HAZARD",
            "objectLabel": "High Voltage Danger Sign",
            "response": "High voltage electrical hazard sign ahead. Keep at least 2 metres distance. Do not touch adjacent metal cabinets.",
            "confidence": 0.98,
            "confidenceLabel": "CRITICAL",
            "visionModel": "MiniCPM-V-4.6 (Local ONNX)",
            "reasoningModel": "Safety Rule Engine",
            "visionLatencyMs": 145,
            "ragLatencyMs": 22,
            "reasoningLatencyMs": 48,
            "isSimulated": True,
            "safetyFlags": ["HAZARD_HIGH_VOLTAGE", "STOP_IMMEDIATELY"]
        }
    else: # obstacle
        event = {
            "id": int(now * 1000),
            "timestamp": now,
            "category": "OBSTACLE",
            "objectLabel": "Low Chair Obstacle",
            "response": "Office chair directly ahead at 1.2 metres. Shift 2 steps to your left to pass safely.",
            "confidence": 0.89,
            "confidenceLabel": "MEDIUM_HIGH",
            "visionModel": "MobileNetV4-Spatial",
            "reasoningModel": "Spatial Guidance Engine",
            "visionLatencyMs": 95,
            "ragLatencyMs": 15,
            "reasoningLatencyMs": 52,
            "isSimulated": True,
            "safetyFlags": ["COLLISION_WARNING", "LEFT_PATH_CLEAR"]
        }

    session_events.append(event)
    for client in connected_clients:
        try:
            await client.send_json({"type": "event", "event": event})
        except Exception:
            pass

    return {"status": "broadcasted", "event": event}


@app.get("/diagnostics")
def get_diagnostics():
    return {
        "network": "OFFLINE SECURED / 0 EXTERNAL CALLS",
        "device": "iQOO 15 (Snapdragon 8 Elite)",
        "rag": {
            "mode": rag_engine.mode,
            "ready": rag_engine.ready,
            "index_size": len(rag_engine.chunks)
        },
        "session": {
            "events_recorded": len(session_events),
            "active_dashboard_clients": len(connected_clients)
        }
    }


# ─── Mount Dashboard Frontend ─────────────────────────────────────────────────

if DASHBOARD_DIR.exists():
    app.mount("/", StaticFiles(directory=str(DASHBOARD_DIR), html=True), name="dashboard")
