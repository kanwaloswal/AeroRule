# AeroRule Integration Guide

AeroRule is a polyglot rules engine designed for high-performance and LLM-centric workflows using CEL (Common Expression Language).

## Java Integration

AeroRule provides a Maven/Gradle-compatible library for Java.

```java
import com.aerorule.core.*;

// 1. Initialize from file
FileSystemProvider provider = new FileSystemProvider("/path/to/rules");
List<Rule> rules = provider.loadRules();

// 2. Evaluate
RuleEvaluator evaluator = new RuleEvaluator(rules.get(0));
Trace trace = evaluator.evaluate(Map.of("user", Map.of("age", 20)));

System.out.println("Matched? " + trace.isMatched());
System.out.println("Action:  " + trace.getActionTaken());
```

## Python Integration

AeroRule offers a Poetry-managed package for Python featuring an intuitive decorator-based API.

```python
from aerorule import aerorule

my_rule = {
    "id": "adult-check",
    "condition": "user.age >= 18",
    "onSuccess": {"action": "ALLOW"},
    "onFailure": {"action": "DENY"}
}

@aerorule(my_rule)
def process_user(user: dict):
    return {"status": "success", "user": user}

# Evaluation happens automatically
result = process_user(user={"age": 20})
```

## LLM System Prompt for Rule Generation

When prompting an LLM to generate rules, use the following context:

> You are an expert system rule generator. Map user requirements to AeroRule JSON structures.
> Output valid JSON matching the `Rule` schema. The `condition` must be written in Google CEL (Common Expression Language).
> 
> Schema context:
> - `id`: String (unique)
> - `priority`: Integer
> - `condition`: String (CEL expression, e.g., `user.age >= 18 && user.status == "ACTIVE"`)
> - `onSuccess`: Object with `action` (String) and `metadata` (Object)
> - `onFailure`: Object with `action` (String) and `metadata` (Object)
