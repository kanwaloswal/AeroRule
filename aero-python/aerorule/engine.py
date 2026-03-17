import json
import time
import logging
from typing import Dict, Any
from .models import RuleSet, RuleSetTrace, Trace
from .evaluator import RuleEvaluator

logger = logging.getLogger(__name__)


class RuleSetEngine:
    """Evaluates a group of rules using the ruleset's execution strategy."""

    def __init__(self, ruleset: RuleSet):
        self.ruleset = ruleset
        self._evaluator_cache = {}

    @classmethod
    def from_file(cls, path: str) -> "RuleSetEngine":
        """Load a RuleSet from a JSON file and return a ready engine."""
        with open(path, "r") as f:
            data = json.load(f)
        ruleset = RuleSet(**data)
        return cls(ruleset)

    def evaluate(self, context: Dict[str, Any]) -> RuleSetTrace:
        """Evaluate all rules according to the execution strategy."""
        logger.info("Evaluating ruleset [%s] with strategy=%s, rules=%d", self.ruleset.id, self.ruleset.executionStrategy, len(self.ruleset.rules))
        start_time = time.time()
        traces: list[Trace] = []
        all_passed = True

        for rule in self.ruleset.rules:
            cache_key = f"{rule.id}@{rule.version or 'latest'}"
            if cache_key not in self._evaluator_cache:
                self._evaluator_cache[cache_key] = RuleEvaluator(rule)
            evaluator = self._evaluator_cache[cache_key]
            
            trace = evaluator.evaluate(context)
            traces.append(trace)

            if not trace.matched:
                all_passed = False
                if self.ruleset.executionStrategy == "GATED":
                    logger.info("GATED strategy: stopping at failed rule [%s]", rule.id)
                    break

        total_time = int((time.time() - start_time) * 1000)
        passed_count = sum(1 for t in traces if t.matched)
        total_evaluated = len(traces)

        result = RuleSetTrace(
            ruleSetId=self.ruleset.id,
            passed=all_passed,
            strategy=self.ruleset.executionStrategy,
            traces=traces,
            executionTimeMs=total_time,
            summary=f"{passed_count}/{total_evaluated} rules passed",
        )
        logger.info("Ruleset [%s] completed: passed=%s, summary='%s', time=%dms", self.ruleset.id, all_passed, result.summary, total_time)
        return result

    def clear_cache(self):
        """Clears the internal RuleEvaluator cache."""
        self._evaluator_cache.clear()
        logger.debug("RuleEvaluator cache cleared for ruleset [%s]", self.ruleset.id)
