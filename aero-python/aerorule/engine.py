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
        strategy = self.ruleset.executionStrategy
        logger.info("Evaluating ruleset [%s] with strategy=%s, rules=%d", self.ruleset.id, strategy, len(self.ruleset.rules))
        start_time = time.time()
        traces: list[Trace] = []
        all_passed = True

        def _evaluate_rule(rule) -> Trace:
            cache_key = f"{rule.id}@{rule.version or 'latest'}"
            if cache_key not in self._evaluator_cache:
                self._evaluator_cache[cache_key] = RuleEvaluator(rule)
            return self._evaluator_cache[cache_key].evaluate(context)

        if strategy == "ALL":
            for rule in self.ruleset.rules:
                trace = _evaluate_rule(rule)
                traces.append(trace)
                if not trace.matched:
                    all_passed = False

        elif strategy == "FIRST_MATCH":
            all_passed = False
            for rule in self.ruleset.rules:
                trace = _evaluate_rule(rule)
                traces.append(trace)
                if trace.matched:
                    all_passed = True
                    break

        elif strategy == "PRIORITY_ORDERED":
            sorted_rules = sorted(
                self.ruleset.rules, 
                key=lambda r: r.priority if r.priority is not None else 0, 
                reverse=True
            )
            for rule in sorted_rules:
                trace = _evaluate_rule(rule)
                traces.append(trace)
                if not trace.matched:
                    all_passed = False

        elif strategy == "GATED":
            for rule in self.ruleset.rules:
                trace = _evaluate_rule(rule)
                traces.append(trace)
                if not trace.matched:
                    all_passed = False
                    logger.info("GATED strategy: stopping at failed rule [%s]", rule.id)
                    break

        elif strategy == "FLOW":
            if not self.ruleset.rules:
                all_passed = True
            else:
                rule_map = {r.id: r for r in self.ruleset.rules}
                visited = set()
                current_rule = self.ruleset.rules[0]
                last_match = False

                while current_rule:
                    if current_rule.id in visited:
                        raise RuntimeError(f"FLOW strategy cycle detected: Rule [{current_rule.id}] was visited multiple times.")
                    visited.add(current_rule.id)

                    trace = _evaluate_rule(current_rule)
                    traces.append(trace)
                    last_match = trace.matched

                    # determine next rule id
                    if last_match and current_rule.onSuccess and current_rule.onSuccess.next:
                        next_id = current_rule.onSuccess.next
                    elif not last_match and current_rule.onFailure and current_rule.onFailure.next:
                        next_id = current_rule.onFailure.next
                    else:
                        next_id = None

                    if not next_id:
                        break  # terminal node

                    if next_id not in rule_map:
                        raise RuntimeError(f"FLOW strategy missing rule: 'next' pointer specifies [{next_id}] but it does not exist in the RuleSet.")
                    
                    current_rule = rule_map[next_id]
                
                all_passed = last_match

        total_time = int((time.time() - start_time) * 1000)
        passed_count = sum(1 for t in traces if t.matched)
        total_evaluated = max(len(traces), len(self.ruleset.rules))

        result = RuleSetTrace(
            ruleSetId=self.ruleset.id,
            passed=all_passed,
            strategy=strategy,
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
