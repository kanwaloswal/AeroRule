import pytest
import json
import os
import sys
import tempfile

# Ensure the aero-python package is importable
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from aerorule.models import RuleSet, RuleSetTrace
from aerorule.engine import RuleSetEngine


# ── Helpers ──────────────────────────────────────────────────────────

def make_ruleset(strategy="ALL"):
    """Build a 3-rule loan-origination-style ruleset dict."""
    return {
        "id": "test-ruleset",
        "name": "Test RuleSet",
        "executionStrategy": strategy,
        "rules": [
            {
                "id": "R1",
                "condition": "score >= 700",
                "onSuccess": {"action": "PASS"},
                "onFailure": {"action": "FAIL"},
            },
            {
                "id": "R2",
                "condition": "income > 50000",
                "onSuccess": {"action": "PASS"},
                "onFailure": {"action": "FAIL"},
            },
            {
                "id": "R3",
                "condition": "debt < 10000",
                "onSuccess": {"action": "APPROVE"},
                "onFailure": {"action": "DENY"},
            },
        ],
    }


# ── ALL strategy ─────────────────────────────────────────────────────

class TestAllStrategy:

    def test_all_pass(self):
        rs = RuleSet(**make_ruleset("ALL"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})

        assert result.passed is True
        assert len(result.traces) == 3
        assert result.summary == "3/3 rules passed"

    def test_one_fails_all_still_evaluated(self):
        """In ALL mode, every rule runs even if one fails."""
        rs = RuleSet(**make_ruleset("ALL"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 600, "income": 80000, "debt": 5000})

        assert result.passed is False
        assert len(result.traces) == 3  # all 3 evaluated
        assert result.traces[0].matched is False
        assert result.traces[1].matched is True
        assert result.traces[2].matched is True
        assert result.summary == "2/3 rules passed"


# ── GATED strategy ───────────────────────────────────────────────────

class TestGatedStrategy:

    def test_all_pass_gated(self):
        rs = RuleSet(**make_ruleset("GATED"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})

        assert result.passed is True
        assert len(result.traces) == 3

    def test_second_rule_fails_stops_early(self):
        """In GATED mode, evaluation stops at the first failure."""
        rs = RuleSet(**make_ruleset("GATED"))
        engine = RuleSetEngine(rs)
        # score passes, income fails → R3 should NOT be evaluated
        result = engine.evaluate({"score": 750, "income": 30000, "debt": 5000})

        assert result.passed is False
        assert len(result.traces) == 2  # R3 not evaluated
        assert result.traces[0].matched is True
        assert result.traces[1].matched is False
        assert result.summary == "1/3 rules passed"

    def test_first_rule_fails_stops_immediately(self):
        rs = RuleSet(**make_ruleset("GATED"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 500, "income": 80000, "debt": 5000})

        assert result.passed is False
        assert len(result.traces) == 1  # Only R1 evaluated
        assert result.summary == "0/3 rules passed"


# ── FIRST_MATCH strategy ─────────────────────────────────────────────

class TestFirstMatchStrategy:

    def test_stops_on_first_match(self):
        rs = RuleSet(**make_ruleset("FIRST_MATCH"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})

        assert result.passed is True
        assert len(result.traces) == 1
        assert result.traces[0].actionTaken == "PASS"

    def test_tries_until_match(self):
        rs = RuleSet(**make_ruleset("FIRST_MATCH"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 500, "income": 30000, "debt": 5000})

        assert result.passed is True
        assert len(result.traces) == 3
        assert result.traces[0].matched is False
        assert result.traces[1].matched is False
        assert result.traces[2].matched is True

    def test_none_match_returns_false(self):
        rs = RuleSet(**make_ruleset("FIRST_MATCH"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 500, "income": 30000, "debt": 50000})

        assert result.passed is False
        assert len(result.traces) == 3


# ── PRIORITY_ORDERED strategy ──────────────────────────────────────────

class TestPriorityOrderedStrategy:

    def test_sorts_descending(self):
        data = make_ruleset("PRIORITY_ORDERED")
        data["rules"][0]["priority"] = 10 # R1
        data["rules"][1]["priority"] = 50 # R2
        data["rules"][2]["priority"] = 30 # R3
        
        rs = RuleSet(**data)
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})

        assert result.passed is True
        assert len(result.traces) == 3
        assert result.traces[0].ruleId == "R2"
        assert result.traces[1].ruleId == "R3"
        assert result.traces[2].ruleId == "R1"

    def test_all_evaluated_even_on_failure(self):
        rs = RuleSet(**make_ruleset("PRIORITY_ORDERED"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 600, "income": 80000, "debt": 5000})

        assert result.passed is False
        assert len(result.traces) == 3

    def test_null_priority_is_zero(self):
        data = make_ruleset("PRIORITY_ORDERED")
        data["rules"][0]["priority"] = 10     # R1
        # R2 priority left out (defaults to None -> 0)
        data["rules"][2]["priority"] = -5     # R3
        
        rs = RuleSet(**data)
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})

        assert len(result.traces) == 3
        assert result.traces[0].ruleId == "R1"
        assert result.traces[1].ruleId == "R2"
        assert result.traces[2].ruleId == "R3"


# ── FLOW strategy ────────────────────────────────────────────────────

def make_flow_ruleset():
    return {
        "id": "flow",
        "executionStrategy": "FLOW",
        "rules": [
            {
                "id": "R1", "condition": "x > 0",
                "onSuccess": {"action": "P1", "next": "R2"},
                "onFailure": {"action": "F1", "next": "R3"}
            },
            {
                "id": "R2", "condition": "y > 0",
                "onSuccess": {"action": "P2"},
                "onFailure": {"action": "F2", "next": "R3"}
            },
            {
                "id": "R3", "condition": "z > 0",
                "onSuccess": {"action": "P3"},
                "onFailure": {"action": "F3"}
            }
        ]
    }

class TestFlowStrategy:

    def test_follows_success_chain(self):
        rs = RuleSet(**make_flow_ruleset())
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"x": 1, "y": 1, "z": 1})

        assert result.passed is True
        assert len(result.traces) == 2
        assert result.traces[0].ruleId == "R1"
        assert result.traces[1].ruleId == "R2"
        assert result.traces[1].actionTaken == "P2"

    def test_branches_on_failure(self):
        rs = RuleSet(**make_flow_ruleset())
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"x": -1, "y": 1, "z": 1})

        assert result.passed is True
        assert len(result.traces) == 2
        assert result.traces[0].ruleId == "R1"
        assert result.traces[1].ruleId == "R3"
        assert result.traces[1].actionTaken == "P3"

    def test_cycle_detection_throws(self):
        data = make_flow_ruleset()
        data["rules"][2]["onSuccess"]["next"] = "R1"
        rs = RuleSet(**data)
        engine = RuleSetEngine(rs)

        with pytest.raises(RuntimeError, match="cycle detected"):
            engine.evaluate({"x": -1, "y": 1, "z": 1})

    def test_missing_next_throws(self):
        data = make_flow_ruleset()
        data["rules"][0]["onSuccess"]["next"] = "MISSING"
        rs = RuleSet(**data)
        engine = RuleSetEngine(rs)

        with pytest.raises(RuntimeError, match="missing rule"):
            engine.evaluate({"x": 1, "y": 1, "z": 1})


# ── from_file ────────────────────────────────────────────────────────

class TestFromFile:

    def test_load_from_json_file(self):
        data = make_ruleset("ALL")
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            json.dump(data, f)
            path = f.name

        try:
            engine = RuleSetEngine.from_file(path)
            assert engine.ruleset.id == "test-ruleset"
            assert len(engine.ruleset.rules) == 3
            result = engine.evaluate({"score": 750, "income": 80000, "debt": 5000})
            assert result.passed is True
        finally:
            os.unlink(path)


# ── Edge cases ───────────────────────────────────────────────────────

class TestEdgeCases:

    def test_empty_ruleset(self):
        rs = RuleSet(id="empty", rules=[])
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"x": 1})

        assert result.passed is True  # vacuously true
        assert len(result.traces) == 0
        assert result.summary == "0/0 rules passed"

    def test_single_rule_pass(self):
        rs = RuleSet(
            id="single",
            rules=[{"id": "ONLY", "condition": "x > 0", "onSuccess": {"action": "OK"}, "onFailure": {"action": "NO"}}],
        )
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"x": 5})

        assert result.passed is True
        assert len(result.traces) == 1
        assert result.traces[0].actionTaken == "OK"
