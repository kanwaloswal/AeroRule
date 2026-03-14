# Python AeroRule Samples

This directory contains standalone Python scripts that demonstrate how to evaluate AeroRule rules using the Python decorator `@aerorule`. They evaluate two financial services scenarios.

## Setup Instructions

1. Ensure you have Python 3.10+ installed.
2. In this directory, create a virtual environment, activate it, and install dependencies. Notice we are using a relative path to reference the local `aero-python` library.

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Running the Samples

Run the python files to see outputs driven by the JSON rules in `Samples/rules`.

```bash
python loan_origination.py
python aml_transaction.py
```
