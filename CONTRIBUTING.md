# Contributing to AeroRule

Welcome! We are excited that you want to contribute to AeroRule. This document provides guidelines for contributing to this project.

## Code of Conduct
By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started
1. Fork the repository.
2. Clone your fork locally.
3. Check out a new branch for your feature or bug fix from the `main` branch.

## Branching Strategy

To keep the repository organized, please use the following branch naming conventions:

- `feat/feature-name`: New features or enhancements.
- `fix/bug-name`: Bug fixes.
- `docs/topic-name`: Documentation updates.
- `refactor/component-name`: Code refactoring without functional changes.
- `test/feature-name`: Adding or updating tests.

Always create your work branches from the `main` branch.

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
5. Use [Conventional Commits](https://www.conventionalcommits.org/) for your commit messages (e.g., `feat: add new engine strategy`, `fix: resolve null pointer in evaluator`).

## Developer Certificate of Origin (DCO)

To ensure that contributors have the right to submit their code, we use the Developer Certificate of Origin (DCO). By contributing to this project, you agree to the terms of the DCO.

We enforce this by requiring a `Signed-off-by` line in your commit messages. You can do this automatically by using the `-s` flag when committing:

```bash
git commit -s -m "feat: add new engine strategy"
```

Thank you for contributing!
