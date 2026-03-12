from pydantic import BaseModel
from typing import Optional, Dict, Any

class Action(BaseModel):
    action: str
    metadata: Optional[Dict[str, Any]] = None

class Rule(BaseModel):
    id: str
    description: Optional[str] = None
    priority: Optional[int] = None
    condition: str
    onSuccess: Optional[Action] = None
    onFailure: Optional[Action] = None

class Trace(BaseModel):
    ruleId: str
    condition: str
    matched: bool
    evaluationError: Optional[str] = None
    executionTimeMs: Optional[int] = None
    actionTaken: Optional[str] = None
