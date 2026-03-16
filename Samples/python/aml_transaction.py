import os
import json
from aerorule import aerorule

base_dir = os.path.dirname(os.path.abspath(__file__))
rule_path = os.path.join(base_dir, '../rules/AML-TX-001.json')

with open(rule_path, 'r') as f:
    rule_def = json.load(f)

@aerorule(rule_def)
def flag_aml(transaction: dict, customer: dict):
    print(f"Transaction AML rule matched. Submit STR for tx: {transaction['id']}")
    return {"status": "FLAGGED", "transaction_id": transaction['id']}

if __name__ == "__main__":
    print("Evaluating AML Rule...\n")
    
    # 1. Matching evaluation
    tx_high = {"id": "TX-999", "amount": 15000.0, "currency": "USD"}
    cust_commercial = {"type": "COMMERCIAL", "id": "CUST-300"}
    
    try:
        print("Scenario 1: $15,000 USD transaction for Commercial Customer.")
        result = flag_aml(transaction=tx_high, customer=cust_commercial)
        print("Result:", result)
    except Exception as e:
        print(e)
        
    print("-" * 40)
    
    # 2. Non-matching evaluation
    tx_low = {"id": "TX-111", "amount": 5000.0, "currency": "USD"}
    try:
        print("Scenario 2: $5,000 USD transaction for Commercial Customer.")
        result = flag_aml(transaction=tx_low, customer=cust_commercial)
        print("Result:", result)
    except Exception as e:
        print(e)
