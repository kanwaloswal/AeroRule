package com.aerorule.core;

public class Rule {
    private String id;
    private String description;
    private Integer priority;
    private String condition;
    private Action onSuccess;
    private Action onFailure;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public Action getOnSuccess() { return onSuccess; }
    public void setOnSuccess(Action onSuccess) { this.onSuccess = onSuccess; }
    public Action getOnFailure() { return onFailure; }
    public void setOnFailure(Action onFailure) { this.onFailure = onFailure; }
}
