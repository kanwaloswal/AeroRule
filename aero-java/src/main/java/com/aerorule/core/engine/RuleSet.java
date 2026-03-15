package com.aerorule.core.engine;

import com.aerorule.core.Rule;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Groups multiple {@link Rule} objects and defines an {@link ExecutionStrategy}
 * for how they should be evaluated together.
 */
public class RuleSet {
    private String id;
    private String name;
    private String description;

    @JsonProperty("executionStrategy")
    private ExecutionStrategy executionStrategy = ExecutionStrategy.ALL;

    private List<Rule> rules;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) { this.executionStrategy = executionStrategy; }

    public List<Rule> getRules() { return rules; }
    public void setRules(List<Rule> rules) { this.rules = rules; }
}
