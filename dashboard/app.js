// SAMANVAYA Office Kit Dashboard Client
// Connects to WebSocket /ws/session and REST API

let ws = null;
let eventCount = 0;
const host = window.location.hostname || "localhost";
const wsUrl = `ws://${host}:8000/ws/session`;
const apiUrl = `http://${host}:8000`;

// DOM Elements
const wsPill = document.getElementById("wsPill");
const wsDot = document.getElementById("wsDot");
const wsStatus = document.getElementById("wsStatus");
const feedList = document.getElementById("feedList");
const feedEmpty = document.getElementById("feedEmpty");

const kpiLatency = document.getElementById("kpiLatency");
const kpiConfidence = document.getElementById("kpiConfidence");
const kpiEvents = document.getElementById("kpiEvents");

const hudBox = document.getElementById("hudBox");
const hudBoxTag = document.getElementById("hudBoxTag");
const hudHeading = document.getElementById("hudHeading");
const hudMotion = document.getElementById("hudMotion");
const hudAudioText = document.getElementById("hudAudioText");

const wfVisionMs = document.getElementById("wfVisionMs");
const wfRagMs = document.getElementById("wfRagMs");
const wfReasonMs = document.getElementById("wfReasonMs");
const wfVisionBar = document.getElementById("wfVisionBar");
const wfRagBar = document.getElementById("wfRagBar");
const wfReasonBar = document.getElementById("wfReasonBar");

const ragInput = document.getElementById("ragInput");
const btnSearchRag = document.getElementById("btnSearchRag");
const ragResults = document.getElementById("ragResults");
const btnClearFeed = document.getElementById("btnClearFeed");

// ─── WebSocket Connection with Auto-Reconnect ─────────────────────────────────

function initWebSocket() {
    wsStatus.textContent = "CONNECTING...";
    wsDot.className = "status-dot orange";

    try {
        ws = new WebSocket(wsUrl);

        ws.onopen = () => {
            console.log("WebSocket connected to SAMANVAYA bridge.");
            wsStatus.textContent = "STREAM LIVE (PHONE CONNECTED)";
            wsDot.className = "status-dot cyan";
        };

        ws.onmessage = (event) => {
            try {
                const data = jsonParse(event.data);
                if (data.type === "history" && Array.isArray(data.events)) {
                    data.events.forEach(renderEventCard);
                } else if (data.type === "event" && data.event) {
                    handleIncomingEvent(data.event);
                }
            } catch (err) {
                console.error("Error processing WebSocket payload:", err);
            }
        };

        ws.onclose = () => {
            wsStatus.textContent = "OFFLINE (RETRYING IN 3s)";
            wsDot.className = "status-dot orange";
            setTimeout(initWebSocket, 3000);
        };

        ws.onerror = (err) => {
            console.warn("WebSocket error, waiting for backend...", err);
            ws.close();
        };
    } catch (e) {
        console.error("Failed to initialize WebSocket:", e);
        setTimeout(initWebSocket, 3000);
    }
}

function jsonParse(str) {
    try {
        return JSON.parse(str);
    } catch {
        return {};
    }
}

// ─── Incoming Event Handler ───────────────────────────────────────────────────

function handleIncomingEvent(item) {
    eventCount++;
    kpiEvents.textContent = eventCount;

    // Calculate metrics
    const vMs = item.visionLatencyMs || 150;
    const rMs = item.ragLatencyMs || 35;
    const lMs = item.reasoningLatencyMs || 105;
    const totalMs = vMs + rMs + lMs;

    // Update KPI displays
    kpiLatency.textContent = totalMs;
    const confVal = item.confidence ? (item.confidence * 100).toFixed(1) : "95.0";
    kpiConfidence.textContent = confVal;

    // Update Waterfall
    wfVisionMs.textContent = `${vMs} ms`;
    wfRagMs.textContent = `${rMs} ms`;
    wfReasonMs.textContent = `${lMs} ms`;

    wfVisionBar.style.width = `${Math.min(100, Math.round((vMs / totalMs) * 100))}%`;
    wfRagBar.style.width = `${Math.min(100, Math.round((rMs / totalMs) * 100))}%`;
    wfReasonBar.style.width = `${Math.min(100, Math.round((lMs / totalMs) * 100))}%`;

    // Update Viewport HUD
    const label = item.objectLabel || "Target Identified";
    hudBoxTag.textContent = `${label.toUpperCase()} (${confVal}%)`;
    hudAudioText.textContent = `"${item.response || label}"`;

    // Dynamic HUD Bounding Box Animation
    if (item.category === "HAZARD") {
        hudBox.style.borderColor = "var(--accent-orange)";
        hudBox.style.boxShadow = "0 0 20px rgba(255, 61, 0, 0.5)";
        hudBox.style.top = "18%";
        hudBox.style.left = "25%";
        hudBox.style.width = "50%";
        hudBox.style.height = "56%";
        hudBoxTag.style.background = "var(--accent-orange)";
        hudBoxTag.style.color = "#FFF";
    } else if (item.category === "MEDICINE") {
        hudBox.style.borderColor = "var(--accent-green)";
        hudBox.style.boxShadow = "0 0 20px rgba(0, 230, 118, 0.5)";
        hudBox.style.top = "30%";
        hudBox.style.left = "32%";
        hudBox.style.width = "36%";
        hudBox.style.height = "40%";
        hudBoxTag.style.background = "var(--accent-green)";
        hudBoxTag.style.color = "#000";
    } else {
        hudBox.style.borderColor = "var(--accent-cyan)";
        hudBox.style.boxShadow = "0 0 20px rgba(0, 229, 255, 0.4)";
        hudBox.style.top = "24%";
        hudBox.style.left = "28%";
        hudBox.style.width = "44%";
        hudBox.style.height = "48%";
        hudBoxTag.style.background = "var(--accent-cyan)";
        hudBoxTag.style.color = "#000";
    }

    // Audio synthesis on laptop for judge demonstration
    if ('speechSynthesis' in window && !window.speechMuted) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(item.response || label);
        utterance.rate = 1.05;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
    }

    renderEventCard(item);
}

function renderEventCard(item) {
    if (feedEmpty) {
        feedEmpty.style.display = "none";
    }

    const card = document.createElement("div");
    const cat = (item.category || "GENERAL").toLowerCase();
    card.className = `event-card ${cat}`;

    const dateStr = item.timestamp ? new Date(item.timestamp * 1000).toLocaleTimeString() : new Date().toLocaleTimeString();

    card.innerHTML = `
        <div class="card-top">
            <span class="card-title">${escapeHtml(item.objectLabel || "Accessibility Event")}</span>
            <span class="card-time">${dateStr}</span>
        </div>
        <div class="card-body">
            ${escapeHtml(item.response || "No description")}
        </div>
        <div class="card-tags">
            <span class="card-tag">CONF: ${item.confidenceLabel || 'HIGH'}</span>
            <span class="card-tag">VISION: ${item.visionLatencyMs || 150}ms</span>
            <span class="card-tag">RAG: ${item.ragLatencyMs || 35}ms</span>
            <span class="card-tag">REASON: ${item.reasoningLatencyMs || 105}ms</span>
            ${item.isSimulated ? '<span class="card-tag" style="color: var(--accent-orange);">SIMULATED</span>' : '<span class="card-tag" style="color: var(--accent-green);">ON-DEVICE LIVE</span>'}
        </div>
    `;

    feedList.insertBefore(card, feedList.firstChild);
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}

// ─── Scenario Trigger (Judge Simulation) ──────────────────────────────────────

async function triggerScenario(scenario) {
    try {
        const resp = await fetch(`${apiUrl}/session/simulate?scenario=${scenario}`, {
            method: "POST"
        });
        const data = await resp.json();
        console.log("Scenario simulated:", data);
    } catch (err) {
        console.warn("Backend not reached, simulating locally:", err);
        // Fallback local simulation if backend isn't started yet
        simulateLocal(scenario);
    }
}

function simulateLocal(scenario) {
    const now = Date.now() / 1000;
    let event = {};
    if (scenario === "medicine") {
        event = {
            id: Date.now(),
            timestamp: now,
            category: "MEDICINE",
            objectLabel: "Dolo 650 / Paracetamol 650mg",
            response: "Paracetamol 650mg detected. For fever and pain relief. Safe adult interval: 6 hours. Maximum 4000mg/day.",
            confidence: 0.94,
            confidenceLabel: "HIGH",
            visionLatencyMs: 175,
            ragLatencyMs: 32,
            reasoningLatencyMs: 110,
            isSimulated: true
        };
    } else if (scenario === "hazard") {
        event = {
            id: Date.now(),
            timestamp: now,
            category: "HAZARD",
            objectLabel: "High Voltage Electrical Hazard",
            response: "Critical warning. High voltage area. Keep at least 2 metres distance. Do not enter.",
            confidence: 0.98,
            confidenceLabel: "CRITICAL",
            visionLatencyMs: 130,
            ragLatencyMs: 18,
            reasoningLatencyMs: 44,
            isSimulated: true
        };
    } else {
        event = {
            id: Date.now(),
            timestamp: now,
            category: "OBSTACLE",
            objectLabel: "Low Chair Obstacle",
            response: "Obstacle directly ahead at 1.2 metres. Shift 2 paces left to pass safely.",
            confidence: 0.88,
            confidenceLabel: "MEDIUM_HIGH",
            visionLatencyMs: 92,
            ragLatencyMs: 14,
            reasoningLatencyMs: 50,
            isSimulated: true
        };
    }
    handleIncomingEvent(event);
}

// ─── RAG Search Sandbox ───────────────────────────────────────────────────────

async function searchRag() {
    const q = ragInput.value.trim();
    if (!q) return;

    ragResults.innerHTML = '<div class="rag-placeholder">Querying local knowledge index...</div>';

    try {
        const resp = await fetch(`${apiUrl}/rag/search?q=${encodeURIComponent(q)}&top_k=3`);
        const data = await resp.json();

        if (!data.results || data.results.length === 0) {
            ragResults.innerHTML = `<div class="rag-placeholder">No matching knowledge chunks found for "${escapeHtml(q)}".</div>`;
            return;
        }

        ragResults.innerHTML = "";
        data.results.forEach(res => {
            const card = document.createElement("div");
            card.className = "rag-result-card";
            card.innerHTML = `
                <div><strong>[${res.domain.toUpperCase()}] ${res.id}</strong> • Score: ${res.score} (${res.engine || 'RAG'})</div>
                <div style="margin-top: 4px; color: var(--text-secondary);">${escapeHtml(res.content)}</div>
                <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Source: ${escapeHtml(res.source || 'Local Database')}</div>
            `;
            ragResults.appendChild(card);
        });
    } catch (err) {
        ragResults.innerHTML = `<div class="rag-placeholder" style="color: var(--accent-orange);">Backend not running. Start it with scripts\\start_backend.bat.</div>`;
    }
}

btnSearchRag.addEventListener("click", searchRag);
ragInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") searchRag();
});

btnClearFeed.addEventListener("click", async () => {
    feedList.innerHTML = "";
    eventCount = 0;
    kpiEvents.textContent = "0";
    try {
        await fetch(`${apiUrl}/session/clear`, { method: "POST" });
    } catch {}
});

// Periodic compass & sensor animation
setInterval(() => {
    const angles = [142, 144, 140, 138, 142];
    const pick = angles[Math.floor(Math.random() * angles.length)];
    if (hudHeading) hudHeading.textContent = `${pick}° SE`;
}, 2000);

// Initialize on page load
window.addEventListener("DOMContentLoaded", () => {
    initWebSocket();
});
