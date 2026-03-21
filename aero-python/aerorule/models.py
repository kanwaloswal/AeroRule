from pydantic import BaseModel, Field, model_validator
from typing import Optional, Dict, Any, List, Literal

class Action(BaseModel):
    """
    Describes the action to be taken and metadata to accompany it 
    when a rule evaluation successfully matches or strictly fails.
    """
    action: str
    metadata: Optional[Dict[str, Any]] = None
    next: Optional[str] = None

class Rule(BaseModel):
    """
    Represents a business rule definition compatible with the AeroRule schema.
    A Rule consists of a CEL condition that evaluates against a context,
    and associated success or failure actions.
    """
    id: str
    name: Optional[str] = None
    version: Optional[str] = None
    description: Optional[str] = None
    priority: Optional[int] = None
    condition: str
    sourceQuote: Optional[str] = None
    sourceDocument: Optional[str] = None
    onSuccess: Optional[Action] = None
    onFailure: Optional[Action] = None

    @model_validator(mode="after")
    def validate_rule(self) -> "Rule":
        """Enforce that required fields are present and at least one action is defined."""
        if not self.id or not self.id.strip():
            raise ValueError("Rule validation failed: 'id' is required and cannot be blank")
        if not self.condition or not self.condition.strip():
            raise ValueError(f"Rule validation failed [{self.id}]: 'condition' is required and cannot be blank")
        if self.onSuccess is None and self.onFailure is None:
            raise ValueError(
                f"Rule validation failed [{self.id}]: "
                "at least one of 'onSuccess' or 'onFailure' must be defined"
            )
        return self

class Trace(BaseModel):
    """
    Represents the execution trace and result of a single Rule evaluation.
    Traces capture inputs, outputs, the boolean result of the match, 
    execution duration, and the resulting action. Useful for auditing.
    """
    ruleId: str
    condition: str
    matched: bool
    evaluationError: Optional[str] = None
    executionTimeMs: Optional[int] = None
    actionTaken: Optional[str] = None
    inputs: Optional[Dict[str, Any]] = None
    evaluatedExpressions: Optional[Dict[str, Any]] = None

class RuleSet(BaseModel):
    id: str
    name: Optional[str] = None
    description: Optional[str] = None
    executionStrategy: Literal["ALL", "FIRST_MATCH", "PRIORITY_ORDERED", "GATED", "FLOW"] = "ALL"
    rules: List[Rule]

class RuleSetTrace(BaseModel):
    ruleSetId: str
    passed: bool
    strategy: str
    traces: List[Trace]
    executionTimeMs: int
    summary: str

