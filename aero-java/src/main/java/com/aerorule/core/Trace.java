package com.aerorule.core;

import java.util.Map;

/**
 * Represents the execution trace and result of a single Rule evaluation.
 * <p>
 * Traces capture inputs, outputs, the boolean result of the match, 
 * execution duration, and the resulting action. Useful for auditing.
 */
public class Trace {
    private String ruleId;
    private String condition;
    private boolean matched;
    private String evaluationError;
    private Long executionTimeMs;
    private String actionTaken;
    private Map<String, Object> inputs;
    private Map<String, Object> evaluatedExpressions;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public String getEvaluationError() { return evaluationError; }
    public void setEvaluationError(String evaluationError) { this.evaluationError = evaluationError; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public Map<String, Object> getInputs() { return inputs; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }
    public Map<String, Object> getEvaluatedExpressions() { return evaluatedExpressions; }
    public void setEvaluatedExpressions(Map<String, Object> evaluatedExpressions) { this.evaluatedExpressions = evaluatedExpressions; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trace that = (Trace) o;
        return java.util.Objects.equals(ruleId, that.ruleId) &&
                java.util.Objects.equals(condition, that.condition) &&
                java.util.Objects.equals(matched, that.matched) &&
                java.util.Objects.equals(evaluationError, that.evaluationError) &&
                java.util.Objects.equals(executionTimeMs, that.executionTimeMs) &&
                java.util.Objects.equals(actionTaken, that.actionTaken) &&
                java.util.Objects.equals(inputs, that.inputs) &&
                java.util.Objects.equals(evaluatedExpressions, that.evaluatedExpressions);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ruleId, condition, matched, evaluationError, executionTimeMs, actionTaken, inputs, evaluatedExpressions);
    }

    @Override
    public String toString() {
        return "Trace{" +
                "ruleId='" + ruleId + '\'' +
                ", condition='" + condition + '\'' +
                ", matched='" + matched + '\'' +
                ", evaluationError='" + evaluationError + '\'' +
                ", executionTimeMs='" + executionTimeMs + '\'' +
                ", actionTaken='" + actionTaken + '\'' +
                ", inputs='" + inputs + '\'' +
                ", evaluatedExpressions='" + evaluatedExpressions + '\'' +
                '}';
    }
}
