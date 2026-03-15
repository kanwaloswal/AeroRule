import json
import time
from typing import Dict, Any
from .models import RuleSet, RuleSetTrace, Trace
from .evaluator import RuleEvaluator


class RuleSetEngine:
    """Evaluates a group of rules using the ruleset's execution strategy."""

    def __init__(self, ruleset: RuleSet):
        self.ruleset = ruleset

    @classmethod
    def from_file(cls, path: str) -> "RuleSetEngine":
        """Load a RuleSet from a JSON file and return a ready engine."""
        with open(path, "r") as f:
            data = json.load(f)
        ruleset = RuleSet(**data)
        return cls(ruleset)

    def evaluate(self, context: Dict[str, Any]) -> RuleSetTrace:
        """Evaluate all rules according to the execution strategy."""
        start_time = time.time()
        traces: list[Trace] = []
        all_passed = True

        for rule in self.ruleset.rules:
            evaluator = RuleEvaluator(rule)
            trace = evaluator.evaluate(context)
            traces.append(trace)

            if not trace.matched:
                all_passed = False
                if self.ruleset.executionStrategy == "GATED":
                    break  # Stop on first failure

        total_time = int((time.time() - start_time) * 1000)
        passed_count = sum(1 for t in traces if t.matched)
        total_evaluated = len(traces)

        return RuleSetTrace(
            ruleSetId=self.ruleset.id,
            passed=all_passed,
            strategy=self.ruleset.executionStrategy,
            traces=traces,
            executionTimeMs=total_time,
            summary=f"{passed_count}/{total_evaluated} rules passed",
        )
