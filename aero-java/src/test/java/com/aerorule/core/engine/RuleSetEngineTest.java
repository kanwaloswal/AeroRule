package com.aerorule.core.engine;

import com.aerorule.core.Action;
import com.aerorule.core.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleSetEngineTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    private Rule makeRule(String id, String condition, String successAction, String failureAction) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setCondition(condition);
        if (successAction != null) {
            Action s = new Action();
            s.setAction(successAction);
            rule.setOnSuccess(s);
        }
        if (failureAction != null) {
            Action f = new Action();
            f.setAction(failureAction);
            rule.setOnFailure(f);
        }
        return rule;
    }

    private Map<String, Object> ctx(Object... kvs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((String) kvs[i], kvs[i + 1]);
        }
        return map;
    }

    private RuleSet buildRuleSet(ExecutionStrategy strategy) {
        RuleSet rs = new RuleSet();
        rs.setId("test-ruleset");
        rs.setName("Test RuleSet");
        rs.setExecutionStrategy(strategy);
        rs.setRules(List.of(
            makeRule("R1", "score >= 700", "PASS", "FAIL"),
            makeRule("R2", "income > 50000", "PASS", "FAIL"),
            makeRule("R3", "debt < 10000", "APPROVE", "DENY")
        ));
        return rs;
    }

    // ════════════════════════════════════════════════════════════════════
    //  ALL strategy
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ALL Strategy")
    class AllStrategy {

        @Test
        @DisplayName("All rules pass")
        void allPass() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.ALL));
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertTrue(result.isPassed());
            assertEquals(3, result.getTraces().size());
            assertEquals("3/3 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("One fails but all are still evaluated")
        void oneFailsAllEvaluated() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.ALL));
            // score fails (600 < 700)
            RuleSetTrace result = engine.evaluate(ctx("score", 600, "income", 80000, "debt", 5000));

            assertFalse(result.isPassed());
            assertEquals(3, result.getTraces().size()); // all 3 evaluated
            assertFalse(result.getTraces().get(0).isMatched());
            assertTrue(result.getTraces().get(1).isMatched());
            assertTrue(result.getTraces().get(2).isMatched());
            assertEquals("2/3 rules passed", result.getSummary());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  GATED strategy
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GATED Strategy")
    class GatedStrategy {

        @Test
        @DisplayName("All rules pass through gate")
        void allPassGated() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.GATED));
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertTrue(result.isPassed());
            assertEquals(3, result.getTraces().size());
        }

        @Test
        @DisplayName("Second rule fails — stops early, R3 not evaluated")
        void secondRuleFailsStopsEarly() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.GATED));
            // income fails (30000 <= 50000)
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 30000, "debt", 5000));

            assertFalse(result.isPassed());
            assertEquals(2, result.getTraces().size()); // R3 not evaluated
            assertTrue(result.getTraces().get(0).isMatched());
            assertFalse(result.getTraces().get(1).isMatched());
            assertEquals("1/2 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("First rule fails — stops immediately")
        void firstRuleFailsStopsImmediately() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.GATED));
            RuleSetTrace result = engine.evaluate(ctx("score", 500, "income", 80000, "debt", 5000));

            assertFalse(result.isPassed());
            assertEquals(1, result.getTraces().size());
            assertEquals("0/1 rules passed", result.getSummary());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Edge cases
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty ruleset is vacuously true")
        void emptyRuleSet() {
            RuleSet rs = new RuleSet();
            rs.setId("empty");
            rs.setRules(List.of());
            rs.setExecutionStrategy(ExecutionStrategy.ALL);

            RuleSetEngine engine = new RuleSetEngine(rs);
            RuleSetTrace result = engine.evaluate(ctx("x", 1));

            assertTrue(result.isPassed());
            assertEquals(0, result.getTraces().size());
            assertEquals("0/0 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("Single rule pass")
        void singleRulePass() {
            RuleSet rs = new RuleSet();
            rs.setId("single");
            rs.setRules(List.of(makeRule("ONLY", "x > 0", "OK", "NO")));
            rs.setExecutionStrategy(ExecutionStrategy.ALL);

            RuleSetEngine engine = new RuleSetEngine(rs);
            RuleSetTrace result = engine.evaluate(ctx("x", 5));

            assertTrue(result.isPassed());
            assertEquals(1, result.getTraces().size());
            assertEquals("OK", result.getTraces().get(0).getActionTaken());
        }

        @Test
        @DisplayName("Strategy field is populated in trace")
        void strategyFieldInTrace() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.GATED));
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertEquals("GATED", result.getStrategy());
        }
    }
}
