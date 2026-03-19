## System Architecture

AeroRule is built on a polyglot design supporting Java and Python with identical execution semantics.

╔════════════════════════════�═══════════════════════════════════════════════════╗
║                         AERORULE SYSTEM ARCHITECTURE                           ║
║              A Polyglot Rules Engine for LLM Integration                       ║
╚════════════════════════════════════════════════════════════════════════════════╝

┌─ USER INTERFACE & TOOLS ──────────────────────────────────────────────────────┐
│                                                                               │
│  ┌──────────────────────┐    ┌──────────────────────┐   ┌─────────────────┐   │
│  │   CLI Tools          │    │  Python Decorator    │   │  LLM Integration│   │
│  │  (aero-cli)          │    │  (@aerorule)         │   │  (OpenAI, etc)  │   │
│  ├──────────────────────┤    ├──────────────────────┤   ├─────────────────┤   │
│  │ • aero init          │    │ @aerorule(rule_def)  │   │ • Generate CEL  │   │
│  │ • aero gen           │    │ def process_data()   │   │ • Validate JSON │   │
│  │ • aero verify        │    │    ...               │   │ • Config Mgmt   │   │
│  │ • aero run           │    │ Automatic rule eval  │   │ • Code Gen      │   │
│  │ • aero test          │    │ on function call     │   │                 │   │
│  │ • aero models        │    │                      │   │                 │   │
│  └──────────────────────┘    └──────────────────────┘   └─────────────────┘   │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ CORE EVALUATION ENGINE ──────────────────────────────────────────────────────┐
│                                                                               │
│  Single Rule Flow              RuleSet Flow                                   │
│  ──────────────────────        ─────────────                                  │
│                                                                               │
│  Rule JSON/YAML                RuleSet Definition                             │
│       ↓                              ↓                                        │
│  RuleEvaluator                 RuleSetEngine                                  │
│  ├─ Compile CEL               ├─ Group Rules                                  │
│  ├─ Bind Context              ├─ Apply Strategy:                              │
│  ├─ Evaluate                  │  • ALL (evaluate all)                         │
│  └─ Return Trace              │  • GATED (stop on fail)                       │
│                                └─ Aggregate Results                           │
│  ↓                                  ↓                                         │
│  Trace {                       RuleSetTrace {                                 │
│   ruleId                        ruleSetId                                     │
│   condition                     strategy: "ALL"|"GATED"                       │
│   matched: bool                 passed: bool                                  │
│   actionTaken                   traces: [Trace]                               │
│   executionTimeMs               summary: "3/5 rules passed"                   │
│   evaluationError               executionTimeMs                               │
│  }                              }                                             │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ CEL RUNTIME (Expression Evaluator) ──────────────────────────────────────────┐
│                                                                               │
│  • Google CEL - Safe, non-Turing complete expression language                 │
│  • Fast condition evaluation on context data                                  │
│  • Type-safe variable binding                                                 │
│  • Java: dev.cel.* | Python: celpy                                            │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ POLYGLOT IMPLEMENTATIONS ────────────────────────────────────────────────────┐
│                                                                               │
│  ┌─────────────────────────────┐  ┌────────────────────────────────────────┐  │
│  │ JAVA (aero-java)            │  │ PYTHON (aero-python)                   │  │
│  ├─────────────────────────────┤  ├────────────────────────────────────────┤  │
│  │ • Language: Java 21+        │  │ • Language: Python 3.10+               │  │
│  │ • Build: Maven              │  │ • Package: Poetry                      │  │
│  │ • Location: src/main/java   │  │ • Location: aerorule/                  │  │
│  │ • Classes:                  │  │ • Modules:                             │  │
│  │   - Rule, Trace, Action     │  │   - RuleEvaluator, RuleSetEngine       │  │
│  │   - RuleEvaluator           │  │   - TraceModel (Pydantic), decorators  │  │
│  │   - RuleSetEngine           │  │                                        │  │
│  │   - FileSystemProvider      │  │ • CEL Binding: celpy                   │  │
│  │ • CEL Binding: google-cel   │  │ • Cache: dict[str, RuleEvaluator]      │  │
│  │ • Cache: ConcurrentHashMap  │  │                                        │  │
│  │ • JSON: Jackson ObjectMapper │  │                                       │  │
│  └─────────────────────────────┘  └────────────────────────────────────────┘  │
│                                                                               │
│  ✓ Identical JSON rule format  ✓ Same execution semantics  ✓ Cross-lang       │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ STORAGE & CONFIGURATION ─────────────────────────────────────────────────────┐
│                                                                               │
│  Rule Files (JSON/YAML)        RuleSet Definition      JSON Schemas           │
│  ├─ rule id                    ├─ id                   ├─ Entities            │
│  ├─ description                ├─ name                 ├─ Data models         │
│  ├─ condition (CEL)            ├─ executionStrategy    ├─ Validations         │
│  ├─ onSuccess action           ├─ rules: [Rule]        └─ Code generation     │
│  └─ onFailure action           └─ metadata             (POJOs, Pydantic)      │
│                                                                               │
│  FileSystemProvider Caching                                                   │
│  ├─ Load from directory        ├─ On-demand compilation ├─ Refresh support    │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ DATA FLOW EXAMPLE ──────────────────────────────────────────────��────────────┐
│                                                                                │
│  1. Load Rules                                                                 │
│     rules/ ──→ FileSystemProvider ──→ Rule[]                                   │
│                                                                                │
│  2. Prepare Context                                                            │
│     {user: {age: 25}, ...} ──→ Type binding via CEL                            │
│                                                                                │
│  3. Evaluate                                                                   │
│     Rule + Context ──→ RuleEvaluator ──→ CEL eval ──→ matched: bool            │
│                                                                                │
│  4. Execute Action                                                             │
│     matched=true ──→ onSuccess action ──→ ALLOW / APPROVE / etc.               │
│                                                                                │
│  5. Return Trace                                                               │
│     Trace{ ruleId, condition, matched, actionTaken, executionTimeMs }          │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘

┌─ EXECUTION STRATEGIES ────────────────────────────────────────────────────────┐
│                                                                               │
│  ALL Strategy                          GATED Strategy                         │
│  ─────────────                         ──────────────                         │
│  • Evaluate ALL rules                  • Evaluate rules in order              │
│  • Collect all traces                  • STOP on first failure                │
│  • Return aggregate result             • Return partial traces                │
│  • Use Case: Audit, reporting          • Use Case: Approval gates             │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

┌─ SAMPLES & DOCUMENTATION ─────────────────────────────────────────────────────┐
│                                                                               │
│  /Samples/java/     → LoanOriginationSample, AmlTransactionSample             │
│  /Samples/python/   → loan_origination.py, aml_transaction.py                 │
│  /Samples/rules/    → JSON rule definitions (CREDIT-001, AML-TX-001)          │
│  /docs/             → Technical documentation                                 │
│  /spec/             → JSON Schema definitions                                 │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘

KEY DESIGN PRINCIPLES
═════════════════════════════════════════════════════════════════════════════════

🔒 SECURITY          • CEL is non-Turing complete → prevents infinite loops
                     • No raw code execution → prevents injection attacks

🌍 POLYGLOT          • JSON rule format → language-agnostic
                     • Identical CEL semantics → same behavior in Java & Python

🤖 LLM-NATIVE        • JSON + CEL → LLMs excel at generating these
                     • Safe to execute → no code review risks

⚡ PERFORMANCE       • CEL runtime is extremely fast
                     • Provider caching → minimal file I/O
                     • Concurrent-safe → scales horizontally

📊 OBSERVABLE        • Full execution tracing → audit compliance
                     • Metrics included → timing & decision tracking
                     
