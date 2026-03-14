# AeroRule 🛩️

AeroRule is a performant, polyglot rules engine focused on LLM compatibility, CEL (Common Expression Language) logic, and cross-language traceability.

## The Problem It Solves

Modern applications require dynamic, configurable business logic. However:
1. **Hardcoded logic** requires frequent redeployments and is difficult for non-engineers to understand or audit.
2. **Traditional Rule Engines** are often heavy, language-specific, and have complex, proprietary formats.
3. **LLM Integration is Risky:** Asking an LLM to generate raw executable code (like Java or Python) for dynamic business rules introduces immense security, reliability, and injection risks.

**AeroRule's Solution:** 
AeroRule completely decouples rules from your codebase using a standardized **JSON Schema** where conditions are evaluated safely via **CEL (Common Expression Language)**.
- **Secure:** CEL is secure, non-Turing complete, and incredibly fast.
- **Polyglot:** Rule definitions and trace evaluations behave identically across Java and Python.
- **LLM-Native:** LLMs are exceptionally good at generating JSON and CEL, making it trivial to build natural-language-to-rule pipelines that are safe to execute in production.

## How to Use It

AeroRule currently provides full support for **Java** and **Python**.

### Java Integration (`aero-core`)

AeroRule provides a Maven/Gradle-compatible library for Java.

```java
import com.aerorule.core.*;
import java.util.Map;
import java.util.List;

// 1. Initialize rules from your file system (or database)
FileSystemProvider provider = new FileSystemProvider("/path/to/rules");
List<Rule> rules = provider.loadRules();

// 2. Evaluate a rule against an execution context
RuleEvaluator evaluator = new RuleEvaluator(rules.get(0));
Trace trace = evaluator.evaluate(Map.of(
    "user", Map.of("age", 20, "status", "ACTIVE")
));

System.out.println("Condition matched: " + trace.isMatched());
System.out.println("Action taken: " + trace.getActionTaken());
System.out.println("Execution time (ms): " + trace.getExecutionTimeMs());

// 3. (Optional) Evaluate using POJOs directly
Customer customer = new Customer("CUST-100", 600, 8000000L);
Trace pojoTrace = evaluator.evaluateWithObjects(Map.of("customer", customer));
```

### Python Integration (`aero-python`)

AeroRule offers a Poetry-managed package for Python featuring an intuitive decorator-based API.

```python
from aerorule import aerorule

my_rule = {
    "id": "adult-check",
    "condition": "user.age >= 18",
    "onSuccess": {"action": "ALLOW"},
    "onFailure": {"action": "DENY"}
}

# The decorator automatically evaluates the rule against the function's arguments
@aerorule(my_rule)
def process_user(user: dict):
    return {"status": "Processing allowed user", "user": user}

# Evaluation happens automatically when called
result = process_user(user={"age": 20})
```

## Financial Services Use Cases

AeroRule is exceptionally well-suited for Financial Services where audibility, complex arithmetic, and strict condition evaluations are required without complex deployment cycles.

Here are a few ways AeroRule can be applied in the financial sector:

### 1. Loan Origination (Credit & Income Eligibility)
Evaluate if a customer meets dual requirements for loan approval.
```json
{
  "id": "CREDIT-001",
  "condition": "credit_score >= 700 && annual_income > 50000",
  "onSuccess": { "action": "APPROVE_LOAN" },
  "onFailure": { "action": "DENY_LOAN" }
}
```

### 2. Anti-Money Laundering (AML)
Flag large transactions exceeding regulatory thresholds.
```json
{
  "id": "AML-TX-001",
  "condition": "transaction_amount > 10000.0",
  "onSuccess": { "action": "SUBMIT_STR" },
  "onFailure": { "action": "LOG_COMPLIANT" }
}
```

### 3. Know Your Customer (KYC)
Ensure a customer is verified and their account has existed long enough before granting sensitive access.
```json
{
  "id": "KYC-001",
  "condition": "kyc_verified == true && account_age > 90",
  "onSuccess": { "action": "GRANT_ACCESS" },
  "onFailure": { "action": "REQUIRE_VERIFICATION" }
}
```

### 4. Dynamic Risk Scoring & Sanctions
Immediately halt accounts if a sanctions hit is detected or their compiled risk score is critically high.
```json
{
  "id": "RISK-001",
  "condition": "risk_score > 8 || sanctions_hit == true",
  "onSuccess": { "action": "FREEZE_ACCOUNT" },
  "onFailure": { "action": "CONTINUE" }
}
```

## Connecting AeroRule with an LLM

Because AeroRule uses strict JSON schemas and Google's Common Expression Language, you can easily use an LLM (such as OpenAI, Anthropic, Gemini, etc.) to generate or modify your business rules dynamically.

Just provide the LLM with the following system prompt and schema context:

```text
You are an expert system rule generator. Map user requirements to AeroRule JSON structures.
Output valid JSON matching the `Rule` schema. The `condition` must be written in Google CEL (Common Expression Language).

Schema context:
- `id`: String (unique identifier)
- `description`: String (human-readable purpose)
- `priority`: Integer (higher executes earlier)
- `condition`: String (CEL expression, e.g., `user.age >= 18 && user.status == "ACTIVE"`)
- `onSuccess`: Object with `action` (String) and `metadata` (Object)
- `onFailure`: Object with `action` (String) and `metadata` (Object)
```

### Example

**User Prompt:**
>"Create a rule that denies the transaction if the cart total is greater than $500 and the user is unverified."

**LLM Output:**
```json
{
  "id": "high-value-unverified-deny",
  "description": "Deny transaction if cart total > 500 and user is unverified",
  "priority": 100,
  "condition": "cart.total > 500 && user.verified == false",
  "onSuccess": {
    "action": "DENY"
  },
  "onFailure": {
    "action": "ALLOW"
  }
}
```

This JSON rule can immediately be loaded into your Java or Python application and safely evaluated using AeroRule without writing any custom parsing!
