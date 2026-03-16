import os
import sys

# Ensure aerorule can be imported directly from the source tree
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../aero-python')))

from aerorule.engine import RuleSetEngine

def main():
    ruleset_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../rulesets/loan-origination-v1.json'))
    
    # Check if ruleset exists
    if not os.path.exists(ruleset_path):
        print(f"Error: RuleSet not found at {ruleset_path}")
        sys.exit(1)

    print(f"Loading RuleSet from: {ruleset_path}")
    engine = RuleSetEngine.from_file(ruleset_path)

    # 1st Test: Strong Candidate (Should Pass All Gates)
    print("\n--- Evaluation 1: Strong Candidate (Should Pass) ---")
    context_strong = {
        "customer": {
            "creditScore": 750,
            "annualIncome": 80000
        },
        "loan": {
            "amount": 200000
        }
    }
    print("Evaluating context:", context_strong)
    
    result = engine.evaluate(context_strong)
    
    print(f"Overall Result: {'PASSED' if result.passed else 'FAILED'}")
    print(f"Strategy used: {result.strategy}")
    print(f"Summary: {result.summary}")
    print("Individual Trace Actions Taken:")
    for t in result.traces:
        print(f"  - [{t.ruleId}]: Matched={t.matched}, Action={t.actionTaken}")


    # 2nd Test: Weak Candidate (Fails Second Gate - Income)
    print("\n--- Evaluation 2: Weak Candidate (Should Fail Income Gate) ---")
    context_weak = {
        "customer": {
            "creditScore": 710,     # Meets 680
            "annualIncome": 35000   # Fails 40000
        },
        "loan": {
            "amount": 100000        # Would pass LTV, but gate stops at Income
        }
    }
    print("Evaluating context:", context_weak)
    
    result2 = engine.evaluate(context_weak)
    
    print(f"Overall Result: {'PASSED' if result2.passed else 'FAILED'}")
    print(f"Strategy used: {result2.strategy}")
    print(f"Summary: {result2.summary}")
    print("Individual Trace Actions Taken:")
    for t in result2.traces:
        print(f"  - [{t.ruleId}]: Matched={t.matched}, Action={t.actionTaken}")

if __name__ == "__main__":
    main()
