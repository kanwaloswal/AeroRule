import os
import json
import logging
from pathlib import Path
from typing import Optional
from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)

CONFIG_DIR = Path.home() / ".aerorule"
CONFIG_FILE = CONFIG_DIR / "config"

# Environment variable names — all config values can be set via env vars.
# Env vars always take priority over the config file.
ENV_API_KEY   = "AERORULE_API_KEY"
ENV_BASE_URL  = "AERORULE_BASE_URL"
ENV_PROVIDER  = "AERORULE_PROVIDER"
ENV_MODEL     = "AERORULE_MODEL"
ENV_RULES_DIR = "AERORULE_RULES_DIR"

class AeroConfig(BaseModel):
    provider: str = Field(..., description="LLM Provider (openai, anthropic, gemini, grok, ollama)")
    model: str = Field(..., description="Specific model name (e.g., gpt-4o, claude-3-opus, llama3)")
    api_key: Optional[str] = Field(None, description="API Key for the provider")
    base_url: Optional[str] = Field(None, description="Base URL (especially for Ollama or Grok)")
    rules_dir: str = Field("./rules", description="Local rules directory")

    @classmethod
    def from_env(cls) -> Optional["AeroConfig"]:
        """Build a config entirely from environment variables (no config file needed)."""
        provider = os.environ.get(ENV_PROVIDER)
        model = os.environ.get(ENV_MODEL)
        if not provider or not model:
            return None
        return cls(
            provider=provider,
            model=model,
            api_key=os.environ.get(ENV_API_KEY),
            base_url=os.environ.get(ENV_BASE_URL),
            rules_dir=os.environ.get(ENV_RULES_DIR, "./rules"),
        )

def _apply_env_overrides(config: AeroConfig) -> AeroConfig:
    """Override any config values with environment variables if set."""
    if v := os.environ.get(ENV_API_KEY):
        config.api_key = v
        logger.debug("API key loaded from environment variable %s", ENV_API_KEY)
    if v := os.environ.get(ENV_BASE_URL):
        config.base_url = v
    if v := os.environ.get(ENV_PROVIDER):
        config.provider = v
    if v := os.environ.get(ENV_MODEL):
        config.model = v
    if v := os.environ.get(ENV_RULES_DIR):
        config.rules_dir = v
    return config

def load_config() -> Optional[AeroConfig]:
    # 1. Try to build purely from env vars (useful in CI / containers)
    env_config = AeroConfig.from_env()
    if env_config:
        logger.debug("Config loaded entirely from environment variables")
        return env_config

    # 2. Fall back to config file, then apply env overrides on top
    if not CONFIG_FILE.exists():
        return None
    try:
        with open(CONFIG_FILE, "r") as f:
            data = json.load(f)
            config = AeroConfig(**data)
    except Exception:
        return None

    return _apply_env_overrides(config)

def save_config(config: AeroConfig, exclude_api_key: bool = False):
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    data = config.model_dump()
    if exclude_api_key:
        data.pop("api_key", None)
    with open(CONFIG_FILE, "w") as f:
        json.dump(data, f, indent=4)
