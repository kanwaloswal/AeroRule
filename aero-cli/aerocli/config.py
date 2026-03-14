import os
import json
from pathlib import Path
from typing import Optional
from pydantic import BaseModel, Field

CONFIG_DIR = Path.home() / ".aerorule"
CONFIG_FILE = CONFIG_DIR / "config"

class AeroConfig(BaseModel):
    provider: str = Field(..., description="LLM Provider (openai, anthropic, gemini, grok, ollama)")
    api_key: Optional[str] = Field(None, description="API Key for the provider")
    base_url: Optional[str] = Field(None, description="Base URL (especially for Ollama or Grok)")
    rules_dir: str = Field("./rules", description="Local rules directory")

def load_config() -> Optional[AeroConfig]:
    if not CONFIG_FILE.exists():
        return None
    
    try:
        with open(CONFIG_FILE, "r") as f:
            data = json.load(f)
            return AeroConfig(**data)
    except Exception:
        return None

def save_config(config: AeroConfig):
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    with open(CONFIG_FILE, "w") as f:
        json.dump(config.model_dump(), f, indent=4)
