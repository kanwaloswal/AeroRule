# AeroRule — Architecture

AeroRule is a polyglot rules engine (Java + Python) that evaluates JSON-defined business rules using Google's Common Expression Language (CEL). This document gives a compact, easy-to-read architecture overview suitable for the project docs.

## High-level diagram

Simple flow (text) to render cleanly on GitHub:

```
User / CLI / LLM
     ↓
[aero-cli]  [@aerorule decorator]
     ↓          ↓
    Management  Invocation
       └─────────┬─────────┘
                 ↓
           Core Evaluation Engine
    ┌─────────────────────────────────┐
    │ • RuleEvaluator (single rule)   │
    │ • RuleSetEngine (grouped rules) │
    │ • FileSystemProvider (rules)    │
    │ • CEL runtime (dev.cel / celpy) │
    └─────────────────────────────────┘
                 ↓
            Outputs: Trace, Action
```

## Components (short)

- **aero-cli (Python)**: CLI for generating, validating, testing and running rules; integrates with LLMs to generate CEL/JSON and for model-driven code generation.
- **Python decorator (@aerorule)**: Evaluate rules automatically around function calls in application code.
- **aero-java**: Java core for JVM apps (Rule, RuleEvaluator, RuleSetEngine, FileSystemProvider).
- **aero-python**: Python runtime (RuleEvaluator, RuleSetEngine, decorator, Pydantic models).
- **CEL runtime**: dev.cel (Java) and celpy (Python) for safe expression evaluation.
- **FileSystemProvider**: Loads JSON/YAML rules from disk and caches them.
- **Trace / RuleSetTrace**: Auditable evaluation results (matched, evaluationError, executionTimeMs, actionTaken, inputs).

## Execution strategies

- **ALL** — Evaluate every rule and collect all traces (audit / reporting).
- **GATED** — Evaluate rules in order and stop at the first failure (approval/gating workflows).

## Typical data flow

1. Rules authored as JSON/YAML (CEL condition + onSuccess/onFailure actions).  
2. Rules loaded by FileSystemProvider (or another provider).  
3. Context (dict/POJO) passed to RuleEvaluator or to RuleSetEngine.  
4. CEL expression is compiled (JIT or precompiled) and evaluated against the context.  
5. RuleEvaluator returns a Trace. RuleSetEngine aggregates traces and produces a RuleSetTrace.

## Polyglot & LLM notes

- Rules are JSON-first and language-agnostic; the same rule yields equivalent results in Java and Python.
- Using CEL (instead of raw code) makes LLM-generated policies safer to execute in production.

