# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0]

### Added
- `FIRST_MATCH`, `PRIORITY_ORDERED`, and `FLOW` execution strategies for RuleSets.
- `next` field on `Action` to support decision-tree rule chaining in `FLOW` strategy.
- `aero show` CLI command for visualizing rulesets and ASCII flowcharts.

### Changed
- Version bumped to 0.2.0 across Java, Python, and CLI.

### Added (earlier features)
- Polyglot execution engine (Java and Python) with CEL-based condition evaluation.
- `GATED` and `ALL` execution strategies for RuleSets.
- Natural language rule generation via `aero-cli` and LLM integration.
- `aero-cli` generator for Pydantic and Java POJOs from JSON schema.
- Java and Python sample applications (Loan Origination, AML Transaction).
- Streamlit web demo for policy rule generation.
