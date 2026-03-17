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
import java.util.concurrent.ConcurrentHashMap;

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

        for (Rule rule : ruleSet.getRules()) {
            // Get or create cached evaluator
            String cacheKey = rule.getId() + "@" + (rule.getVersion() != null ? rule.getVersion() : "latest");
            RuleEvaluator evaluator = evaluatorCache.computeIfAbsent(cacheKey, k -> new RuleEvaluator(rule));

            Trace trace = evaluator.evaluate(context);
            traces.add(trace);

            if (!trace.isMatched()) {
                allPassed = false;
                if (ruleSet.getExecutionStrategy() == ExecutionStrategy.GATED) {
                    logger.info("GATED strategy: stopping at failed rule [{}]", rule.getId());
                    break;
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

        logger.info("Ruleset [{}] completed: passed={}, summary='{}', time={}ms", ruleSet.getId(), allPassed, result.getSummary(), totalTime);
        return result;
    }

    /**
     * Clears the internal RuleEvaluator cache.
     */
    public void clearCache() {
        evaluatorCache.clear();
        logger.debug("RuleEvaluator cache cleared for ruleset [{}]", ruleSet.getId());
    }
}
