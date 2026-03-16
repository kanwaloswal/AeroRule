import os
import json
from aerorule import aerorule

base_dir = os.path.dirname(os.path.abspath(__file__))
rule_path = os.path.join(base_dir, '../rules/CREDIT-001.json')

with open(rule_path, 'r') as f:
    rule_def = json.load(f)

# Domain classes are implicitly represented by dictionaries in the kwargs
# The decorator evaluates against the kwargs
@aerorule(rule_def)
def process_loan(customer: dict, account: dict):
    print(f"Loan approved for customer: {customer['id']}")
    return {"status": "SUCCESS", "customer_id": customer['id']}

if __name__ == "__main__":
    print("Evaluating Loan Origination Rule...\n")
    
    # 1. Successful evaluation
    customer_success = {
        "id": "CUST-100",
        "riskScore": 600,
        "annualRevenue": 8000000,
    }
    account_commercial = {
        "type": "COMMERCIAL"
    }

    try:
        print("Scenario 1: Low risk score, high revenue, commercial account.")
        result = process_loan(customer=customer_success, account=account_commercial)
        print("Result:", result)
    except Exception as e:
        print(e)
        
    print("-" * 40)
    
    # 2. Failed evaluation (Retail account)
    account_retail = {"type": "RETAIL"}
    try:
        print("Scenario 2: Low risk score, high income, retail account.")
        result = process_loan(customer=customer_success, account=account_retail)
        print("Result:", result)
    except Exception as e:
        print(e)

