"""
SAMANVAYA — Model Download Script

Downloads all required local AI model weights to the models/ directory.
Run this BEFORE the hackathon. Do NOT run during the demo.

All models are open-source and run fully offline after download.

Usage:
    python scripts/download_models.py

Requirements:
    pip install huggingface_hub
"""

import os
import sys
from pathlib import Path

try:
    from huggingface_hub import hf_hub_download, snapshot_download
except ImportError:
    print("Install: pip install huggingface_hub")
    sys.exit(1)

MODELS_DIR = Path(__file__).parent.parent / "models"
MODELS_DIR.mkdir(exist_ok=True)

MODELS = [
    {
        "name": "Whisper-tiny GGUF",
        "repo": "ggerganov/whisper.cpp",
        "filename": "ggml-tiny.bin",
        "dest": "whisper",
        "size": "~39MB"
    },
    {
        "name": "all-MiniLM-L6-v2 (embeddings)",
        "repo": "sentence-transformers/all-MiniLM-L6-v2",
        "filename": None,  # full snapshot
        "dest": "minilm",
        "size": "~22MB"
    },
    # Qwen2-1.5B — download the Q4 GGUF manually from HuggingFace
    # URL: https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF
    # File: qwen2-1_5b-instruct-q4_k_m.gguf (~900MB)
    # MiniCPM-V 4.6 — download Q4 GGUF manually
    # URL: https://huggingface.co/openbmb/MiniCPM-V-4.6-gguf
    # Florence-2 ONNX — convert separately or use Android ONNX build
]

def download_model(model: dict):
    dest = MODELS_DIR / model["dest"]
    dest.mkdir(exist_ok=True)
    print(f"\n→ Downloading {model['name']} ({model['size']})...")
    try:
        if model["filename"]:
            path = hf_hub_download(
                repo_id=model["repo"],
                filename=model["filename"],
                local_dir=str(dest)
            )
            print(f"  ✅ Saved: {path}")
        else:
            path = snapshot_download(
                repo_id=model["repo"],
                local_dir=str(dest)
            )
            print(f"  ✅ Saved snapshot: {path}")
    except Exception as e:
        print(f"  ❌ Failed: {e}")
        print(f"  Manual download: https://huggingface.co/{model['repo']}")

if __name__ == "__main__":
    print("SAMANVAYA Model Downloader")
    print("=" * 40)
    print(f"Target directory: {MODELS_DIR}")
    print("\nModels to download:")
    for m in MODELS:
        print(f"  - {m['name']} ({m['size']})")

    print("\nFor large models, download manually from HuggingFace:")
    print("  Qwen2-1.5B Q4: https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF")
    print("  MiniCPM-V 4.6: https://huggingface.co/openbmb/MiniCPM-V-4.6-gguf")
    print("  Florence-2:    https://huggingface.co/microsoft/Florence-2-base")
    print()

    confirm = input("Download small models now? [y/N]: ").strip().lower()
    if confirm == "y":
        for model in MODELS:
            download_model(model)
        print("\n✅ Download complete. Place large GGUF files in models/ before the hackathon.")
    else:
        print("Skipped. Run again when ready.")
