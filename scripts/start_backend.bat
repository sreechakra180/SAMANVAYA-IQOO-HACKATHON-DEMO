@echo off
echo SAMANVAYA — Backend Startup
echo ============================
echo.

cd /d "%~dp0\..\backend"

echo Starting FastAPI backend on http://localhost:8000
echo Dashboard WebSocket: ws://localhost:8000/ws/session
echo Health: http://localhost:8000/health
echo RAG Search: http://localhost:8000/rag/search?q=paracetamol
echo.
echo Press Ctrl+C to stop.
echo.

python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
