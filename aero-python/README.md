# AeroRule Python Package (`aerorule`)

This package provides the Python runtime for AeroRule, allowing you to load JSON rules and evaluate them using Google's Common Expression Language (CEL).

## Installation

Using Poetry (from the project root if managing locally):
```bash
poetry install
```

## Quick Start

### Using the Engine directly

```python
from aerorule import RuleSetEngine

# Load from ruleset definition
engine = RuleSetEngine.from_file("path/to/ruleset.json")

# Evaluate against business data
result = engine.evaluate({
    "user": {"age": 20, "kycVerified": True}
})

print("Passed:", result.passed)
```

### Using the Decorator

```python
from aerorule import aerorule

adult_rule = {
    "id": "adult-check",
    "condition": "user.age >= 18",
    "onSuccess": {"action": "ALLOW"},
    "onFailure": {"action": "DENY"}
}

@aerorule(adult_rule)
def onboard_user(user: dict):
    return {"status": "Onboarded!"}

# Fails if condition is not met
onboard_user(user={"age": 16})
```
