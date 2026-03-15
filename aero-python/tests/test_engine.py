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
        assert result.summary == "1/2 rules passed"

    def test_first_rule_fails_stops_immediately(self):
        rs = RuleSet(**make_ruleset("GATED"))
        engine = RuleSetEngine(rs)
        result = engine.evaluate({"score": 500, "income": 80000, "debt": 5000})

        assert result.passed is False
        assert len(result.traces) == 1  # Only R1 evaluated
        assert result.summary == "0/1 rules passed"


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
