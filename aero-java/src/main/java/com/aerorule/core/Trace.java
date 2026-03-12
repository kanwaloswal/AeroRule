package com.aerorule.core;

public class Trace {
    private String ruleId;
    private String condition;
    private boolean matched;
    private String evaluationError;
    private Long executionTimeMs;
    private String actionTaken;

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
}
