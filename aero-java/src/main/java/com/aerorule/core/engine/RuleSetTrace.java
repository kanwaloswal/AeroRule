package com.aerorule.core.engine;

import com.aerorule.core.Trace;

import java.util.List;

/**
 * Aggregated result of evaluating a {@link RuleSet}.
 * Contains every individual {@link Trace} produced, plus an overall pass/fail verdict.
 */
public class RuleSetTrace {
    private String ruleSetId;
    private boolean passed;
    private String strategy;
    private List<Trace> traces;
    private long executionTimeMs;
    private String summary;

    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String ruleSetId) { this.ruleSetId = ruleSetId; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public List<Trace> getTraces() { return traces; }
    public void setTraces(List<Trace> traces) { this.traces = traces; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
