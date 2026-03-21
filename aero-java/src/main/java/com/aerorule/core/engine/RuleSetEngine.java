package com.aerorule.core.engine;

import com.aerorule.core.Rule;
import com.aerorule.core.RuleEvaluator;
import com.aerorule.core.Trace;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates all rules in a {@link RuleSet} according to its {@link ExecutionStrategy}.
 *
 * <pre>
 * RuleSetEngine engine = RuleSetEngine.fromFile("rules/loan-origination.json");
 * RuleSetTrace result = engine.evaluate(Map.of(
 *     "customer", Map.of("creditScore", 720, "annualIncome", 85000)
 * ));
 * System.out.println(result.isPassed());   // true / false
 * System.out.println(result.getSummary()); // "3/3 rules passed"
 * </pre>
 */
public class RuleSetEngine {
    private static final Logger logger = LoggerFactory.getLogger(RuleSetEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RuleSet ruleSet;
    
    // Cache of RuleEvaluators keyed by Rule ID + Version 
    // This makes RuleSetEngine safe for concurrent reuse
    private final Map<String, RuleEvaluator> evaluatorCache = new ConcurrentHashMap<>();

    public RuleSetEngine(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Load a {@link RuleSet} from a JSON file and return a ready-to-use engine.
     */
    public static RuleSetEngine fromFile(String path) throws IOException {
        RuleSet ruleSet = MAPPER.readValue(new File(path), RuleSet.class);
        return new RuleSetEngine(ruleSet);
    }

    /**
     * Evaluate every rule in the set according to the configured execution strategy.
     *
     * @param context the variables available to CEL expressions
     * @return a {@link RuleSetTrace} with all individual traces and an overall verdict
     */
    public RuleSetTrace evaluate(Map<String, Object> context) {
        logger.info("Evaluating ruleset [{}] with strategy={}, rules={}", ruleSet.getId(), ruleSet.getExecutionStrategy(), ruleSet.getRules().size());
        long startTime = System.currentTimeMillis();
        List<Trace> traces = new ArrayList<>();
        boolean allPassed = true;

        switch (ruleSet.getExecutionStrategy()) {
            case ALL:
                allPassed = evaluateAll(context, traces);
                break;
            case FIRST_MATCH:
                allPassed = evaluateFirstMatch(context, traces);
                break;
            case PRIORITY_ORDERED:
                allPassed = evaluatePriorityOrdered(context, traces);
                break;
            case GATED:
                allPassed = evaluateGated(context, traces);
                break;
            case FLOW:
                allPassed = evaluateFlow(context, traces);
                break;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        long passedCount = traces.stream().filter(Trace::isMatched).count();

        RuleSetTrace result = new RuleSetTrace();
        result.setRuleSetId(ruleSet.getId());
        result.setPassed(allPassed);
        result.setStrategy(ruleSet.getExecutionStrategy().name());
        result.setTraces(traces);
        result.setExecutionTimeMs(totalTime);
        result.setSummary(passedCount + "/" + Math.max(traces.size(), ruleSet.getRules().size()) + " rules passed");

        logger.info("Ruleset [{}] completed: passed={}, summary='{}', time={}ms", ruleSet.getId(), allPassed, result.getSummary(), totalTime);
        return result;
    }

    private Trace evaluateRule(Rule rule, Map<String, Object> context) {
        String cacheKey = rule.getId() + "@" + (rule.getVersion() != null ? rule.getVersion() : "latest");
        RuleEvaluator evaluator = evaluatorCache.computeIfAbsent(cacheKey, k -> new RuleEvaluator(rule));
        return evaluator.evaluate(context);
    }

    private boolean evaluateAll(Map<String, Object> context, List<Trace> traces) {
        boolean allPassed = true;
        for (Rule rule : ruleSet.getRules()) {
            Trace trace = evaluateRule(rule, context);
            traces.add(trace);
            if (!trace.isMatched()) {
                allPassed = false;
            }
        }
        return allPassed;
    }

    private boolean evaluateFirstMatch(Map<String, Object> context, List<Trace> traces) {
        for (Rule rule : ruleSet.getRules()) {
            Trace trace = evaluateRule(rule, context);
            traces.add(trace);
            if (trace.isMatched()) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluatePriorityOrdered(Map<String, Object> context, List<Trace> traces) {
        List<Rule> sortedRules = new ArrayList<>(ruleSet.getRules());
        sortedRules.sort((r1, r2) -> {
            int p1 = r1.getPriority() != null ? r1.getPriority() : 0;
            int p2 = r2.getPriority() != null ? r2.getPriority() : 0;
            return Integer.compare(p2, p1); // descending
        });

        boolean allPassed = true;
        for (Rule rule : sortedRules) {
            Trace trace = evaluateRule(rule, context);
            traces.add(trace);
            if (!trace.isMatched()) {
                allPassed = false;
            }
        }
        return allPassed;
    }

    private boolean evaluateGated(Map<String, Object> context, List<Trace> traces) {
        for (Rule rule : ruleSet.getRules()) {
            Trace trace = evaluateRule(rule, context);
            traces.add(trace);
            if (!trace.isMatched()) {
                logger.info("GATED strategy: stopping at failed rule [{}]", rule.getId());
                return false;
            }
        }
        return true;
    }

    private boolean evaluateFlow(Map<String, Object> context, List<Trace> traces) {
        if (ruleSet.getRules().isEmpty()) {
            return true;
        }

        Map<String, Rule> ruleMap = ruleSet.getRules().stream()
                .collect(Collectors.toMap(Rule::getId, Function.identity()));

        Set<String> visited = new HashSet<>();
        Rule currentRule = ruleSet.getRules().get(0);
        boolean lastMatch = false;

        while (currentRule != null) {
            if (!visited.add(currentRule.getId())) {
                throw new IllegalStateException("FLOW strategy cycle detected: Rule [" + currentRule.getId() + "] was visited multiple times.");
            }

            Trace trace = evaluateRule(currentRule, context);
            traces.add(trace);
            lastMatch = trace.isMatched();

            String nextRuleId;
            if (lastMatch) {
                nextRuleId = currentRule.getOnSuccess() != null ? currentRule.getOnSuccess().getNext() : null;
            } else {
                nextRuleId = currentRule.getOnFailure() != null ? currentRule.getOnFailure().getNext() : null;
            }

            if (nextRuleId == null) {
                break; // Terminal node
            }

            currentRule = ruleMap.get(nextRuleId);
            if (currentRule == null) {
                throw new IllegalStateException("FLOW strategy missing rule: 'next' pointer specifies [" + nextRuleId + "] but it does not exist in the RuleSet.");
            }
        }
        return lastMatch;
    }

    /**
     * Clears the internal RuleEvaluator cache.
     */
    public void clearCache() {
        evaluatorCache.clear();
        logger.debug("RuleEvaluator cache cleared for ruleset [{}]", ruleSet.getId());
    }
}
