import os
import json
from typing import Optional, Dict, Any
from .config import AeroConfig

SYSTEM_PROMPT = """You are an AeroRule architect. 
Your task is to translate natural language business requirements into valid AeroRule JSON files.

Core Requirements:
1. Output ONLY raw JSON. No markdown fences, no explanations.
2. Use Google Common Expression Language (CEL) for the 'condition' field.
3. Variables in the CEL condition must match the provided schema.
4. Ensure the JSON conforms exactly to the rule-schema.json.

Rule Structure:
{
  "id": "unique-kebab-case-id",
  "description": "Human readable description",
  "priority": 10,
  "condition": "user.age >= 18",
  "onSuccess": { "action": "ALLOW", "metadata": {} },
  "onFailure": { "action": "DENY", "metadata": {} }
}
"""

def get_llm_client(config: AeroConfig):
    if config.provider == "openai":
        from openai import OpenAI
        return OpenAI(api_key=config.api_key)
    elif config.provider == "anthropic":
        from anthropic import Anthropic
        return Anthropic(api_key=config.api_key)
    elif config.provider == "gemini":
        import google.generativeai as genai
        genai.configure(api_key=config.api_key)
        return genai
    elif config.provider == "ollama":
        from openai import OpenAI
        return OpenAI(api_key="ollama", base_url=config.base_url)
    elif config.provider == "grok":
        from openai import OpenAI
        return OpenAI(api_key=config.api_key, base_url=config.base_url or "https://api.x.ai/v1")
    else:
        raise ValueError(f"Unsupported provider: {config.provider}")

def generate_rule(prompt: str, context: str, config: AeroConfig) -> str:
    client = get_llm_client(config)
    full_prompt = f"Context Schemas:\n{context}\n\nUser Requirement: {prompt}"
    
    if config.provider in ["openai", "ollama", "grok"]:
        response = client.chat.completions.create(
            model=config.model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": full_prompt}
            ],
            temperature=0,
            response_format={"type": "json_object"} if config.provider == "openai" else None
        )
        return response.choices[0].message.content
    
    elif config.provider == "anthropic":
        response = client.messages.create(
            model=config.model,
            max_tokens=1000,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": full_prompt}]
        )
        return response.content[0].text
    
    elif config.provider == "gemini":
        model = client.GenerativeModel(config.model)
        response = model.generate_content(f"{SYSTEM_PROMPT}\n\n{full_prompt}")
        return response.text
    
    return ""
