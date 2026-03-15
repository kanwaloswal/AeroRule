package com.aerorule.core.engine;

import com.aerorule.core.Rule;
import com.aerorule.core.RuleEvaluator;
import com.aerorule.core.Trace;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RuleSet ruleSet;

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
        long startTime = System.currentTimeMillis();
        List<Trace> traces = new ArrayList<>();
        boolean allPassed = true;

        for (Rule rule : ruleSet.getRules()) {
            RuleEvaluator evaluator = new RuleEvaluator(rule);
            Trace trace = evaluator.evaluate(context);
            traces.add(trace);

            if (!trace.isMatched()) {
                allPassed = false;
                if (ruleSet.getExecutionStrategy() == ExecutionStrategy.GATED) {
                    break; // Stop on first failure
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        long passedCount = traces.stream().filter(Trace::isMatched).count();

        RuleSetTrace result = new RuleSetTrace();
        result.setRuleSetId(ruleSet.getId());
        result.setPassed(allPassed);
        result.setStrategy(ruleSet.getExecutionStrategy().name());
        result.setTraces(traces);
        result.setExecutionTimeMs(totalTime);
        result.setSummary(passedCount + "/" + traces.size() + " rules passed");

        return result;
    }
}
