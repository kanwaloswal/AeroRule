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
                makeRule("R3", "debt < 10000", "APPROVE", "DENY")));
        return rs;
    }

    // ════════════════════════════════════════════════════════════════════
    // ALL strategy
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
    // GATED strategy
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
            assertEquals("1/3 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("First rule fails — stops immediately")
        void firstRuleFailsStopsImmediately() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.GATED));
            // fails on score
            RuleSetTrace result = engine.evaluate(ctx("score", 500, "income", 80000, "debt", 5000));

            assertFalse(result.isPassed());
            assertEquals(1, result.getTraces().size());
            assertEquals("0/3 rules passed", result.getSummary());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════════════
    // FIRST_MATCH strategy
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FIRST_MATCH Strategy")
    class FirstMatchStrategy {

        @Test
        @DisplayName("Stops on first match")
        void stopsOnFirstMatch() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.FIRST_MATCH));
            // score passes -> stops immediately
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertTrue(result.isPassed());
            assertEquals(1, result.getTraces().size());
            assertEquals("PASS", result.getTraces().get(0).getActionTaken());
            assertEquals("1/3 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("Tries until match")
        void triesUntilMatch() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.FIRST_MATCH));
            // score fails, income fails, debt passes
            RuleSetTrace result = engine.evaluate(ctx("score", 500, "income", 30000, "debt", 5000));

            assertTrue(result.isPassed());
            assertEquals(3, result.getTraces().size());
            assertFalse(result.getTraces().get(0).isMatched());
            assertFalse(result.getTraces().get(1).isMatched());
            assertTrue(result.getTraces().get(2).isMatched());
            assertEquals("1/3 rules passed", result.getSummary());
        }

        @Test
        @DisplayName("None match returns false")
        void noneMatch() {
            RuleSetEngine engine = new RuleSetEngine(buildRuleSet(ExecutionStrategy.FIRST_MATCH));
            // all fail
            RuleSetTrace result = engine.evaluate(ctx("score", 500, "income", 30000, "debt", 50000));

            assertFalse(result.isPassed());
            assertEquals(3, result.getTraces().size());
            assertEquals("0/3 rules passed", result.getSummary());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PRIORITY_ORDERED strategy
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PRIORITY_ORDERED Strategy")
    class PriorityOrderedStrategy {

        @Test
        @DisplayName("Sorts by priority descending")
        void sortsByPriorityDesc() {
            RuleSet rs = buildRuleSet(ExecutionStrategy.PRIORITY_ORDERED);
            rs.getRules().get(0).setPriority(10); // R1
            rs.getRules().get(1).setPriority(50); // R2
            rs.getRules().get(2).setPriority(30); // R3

            RuleSetEngine engine = new RuleSetEngine(rs);
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertTrue(result.isPassed());
            assertEquals(3, result.getTraces().size());
            // Execution order should be R2(50), R3(30), R1(10)
            assertEquals("R2", result.getTraces().get(0).getRuleId());
            assertEquals("R3", result.getTraces().get(1).getRuleId());
            assertEquals("R1", result.getTraces().get(2).getRuleId());
        }

        @Test
        @DisplayName("Evaluates all rules even on failure")
        void allEvaluated() {
            RuleSet rs = buildRuleSet(ExecutionStrategy.PRIORITY_ORDERED);
            RuleSetEngine engine = new RuleSetEngine(rs);
            RuleSetTrace result = engine.evaluate(ctx("score", 600, "income", 80000, "debt", 5000));

            assertFalse(result.isPassed());
            assertEquals(3, result.getTraces().size());
        }

        @Test
        @DisplayName("Null priority defaults to zero")
        void nullPriorityIsZero() {
            RuleSet rs = buildRuleSet(ExecutionStrategy.PRIORITY_ORDERED);
            rs.getRules().get(0).setPriority(10); // R1
            rs.getRules().get(1).setPriority(null); // R2 (defaults to 0)
            rs.getRules().get(2).setPriority(-5); // R3

            RuleSetEngine engine = new RuleSetEngine(rs);
            RuleSetTrace result = engine.evaluate(ctx("score", 750, "income", 80000, "debt", 5000));

            assertEquals(3, result.getTraces().size());
            assertEquals("R1", result.getTraces().get(0).getRuleId()); // 10
            assertEquals("R2", result.getTraces().get(1).getRuleId()); // 0
            assertEquals("R3", result.getTraces().get(2).getRuleId()); // -5
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // FLOW strategy
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FLOW Strategy")
    class FlowStrategy {

        private RuleSet buildFlowRuleSet() {
            RuleSet rs = new RuleSet();
            rs.setId("flow");
            rs.setExecutionStrategy(ExecutionStrategy.FLOW);

            // R1 passes -> goes to R2. fails -> goes to R3
            Rule r1 = makeRule("R1", "x > 0", "P1", "F1");
            r1.getOnSuccess().setNext("R2");
            r1.getOnFailure().setNext("R3");

            // R2 passes -> terminal. fails -> goes to R3
            Rule r2 = makeRule("R2", "y > 0", "P2", "F2");
            r2.getOnFailure().setNext("R3"); // success has null next (terminal)

            // R3 is terminal
            Rule r3 = makeRule("R3", "z > 0", "P3", "F3");

            rs.setRules(List.of(r1, r2, r3));
            return rs;
        }

        @Test
        @DisplayName("Follows success chain")
        void followsSuccessChain() {
            RuleSetEngine engine = new RuleSetEngine(buildFlowRuleSet());
            // R1(x=1) passes -> R2(y=1) passes -> Terminal
            RuleSetTrace result = engine.evaluate(ctx("x", 1, "y", 1, "z", 1));

            assertTrue(result.isPassed());
            assertEquals(2, result.getTraces().size());
            assertEquals("R1", result.getTraces().get(0).getRuleId());
            assertEquals("R2", result.getTraces().get(1).getRuleId());
            assertEquals("P2", result.getTraces().get(1).getActionTaken());
        }

        @Test
        @DisplayName("Branches on failure")
        void branchesOnFailure() {
            RuleSetEngine engine = new RuleSetEngine(buildFlowRuleSet());
            // R1(x=-1) fails -> R3(z=1) passes -> Terminal
            RuleSetTrace result = engine.evaluate(ctx("x", -1, "y", 1, "z", 1));

            assertTrue(result.isPassed()); // passed because the LAST rule (R3) passed
            assertEquals(2, result.getTraces().size());
            assertEquals("R1", result.getTraces().get(0).getRuleId());
            assertEquals("R3", result.getTraces().get(1).getRuleId());
            assertEquals("P3", result.getTraces().get(1).getActionTaken());
        }

        @Test
        @DisplayName("Cycle detection throws")
        void cycleDetectionThrows() {
            RuleSet rs = buildFlowRuleSet();
            // Create cycle: R3 success -> R1
            rs.getRules().get(2).getOnSuccess().setNext("R1");
            RuleSetEngine engine = new RuleSetEngine(rs);

            // R1(x=-1) fails -> R3(z=1) passes -> R1 -> CYCLE
            assertThrows(IllegalStateException.class, () -> {
                engine.evaluate(ctx("x", -1, "y", 1, "z", 1));
            });
        }

        @Test
        @DisplayName("Missing next ID throws")
        void missingNextThrows() {
            RuleSet rs = buildFlowRuleSet();
            rs.getRules().get(0).getOnSuccess().setNext("MISSING");
            RuleSetEngine engine = new RuleSetEngine(rs);

            assertThrows(IllegalStateException.class, () -> {
                engine.evaluate(ctx("x", 1, "y", 1, "z", 1));
            });
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Edge cases
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
