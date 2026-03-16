# AeroRule Command Line Interface (`aero-cli`)

`aero-cli` is the command-center for managing, validating, testing, and generating AeroRule policies.

## Features

- **Init**: Setup configuration and LLM profiles (`aero init`).
- **Generate**: Generate perfect CEL rules from natural language via LLMs (`aero gen`).
- **Validate**: Verify rule structures (`aero verify file`).
- **Execute**: Run rules or full rulesets locally (`aero run rules`).
- **Test**: Execute policy compliance suites (`aero test run`).
- **Code Gen**: Generate Java POJOs or Python Pydantic models directly from JSON Schemas (`aero models generate`).

## Installation

```bash
poetry install
```

## Quick Start

Initialize your setup (choose your LLM, provide API keys):
```bash
poetry run aero init
```

Generate a rule via LLM:
```bash
poetry run aero gen "Users must be over 18 and reside in Canada"
```

Verify the syntax of a rule:
```bash
poetry run aero verify file my-rule.json
```

Generate Java models from schema:
```bash
poetry run aero models generate ../spec/Customer.schema.json --lang java --out ./models
```
