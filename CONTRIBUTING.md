# Contributing to AeroRule

Welcome! We are excited that you want to contribute to AeroRule. This document provides guidelines for contributing to this project.

## Code of Conduct
By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started
1. Fork the repository.
2. Clone your fork locally.
3. Check out a new branch for your feature or bug fix.

## Development Setup

### Java (`aero-java`)
- Requirements: Java 21, Maven
- Build and run tests: `mvn clean install`

### Python (`aero-python` and `aero-cli`)
- Requirements: Python 3.10+, Poetry
- Install dependencies: `poetry install`
- Run tests: `poetry run pytest`

## Pull Request Process
1. Ensure your code conforms to the style guidelines of the language (e.g., standard PEP 8 for Python, standard Java conventions).
2. Write tests for any new features or bug fixes.
3. Update relevant documentation (README.md, Javadoc, Python docstrings).
4. Create a Pull Request against the `main` branch. Provide a clear description of the changes.

## CLA / DCO
All contributors must sign a Contributor License Agreement (CLA) or acknowledge the Developer Certificate of Origin (DCO) to contribute to this project. We currently enforce DCO by requiring a `Signed-off-by` line in your commit messages.

Thank you for contributing!
