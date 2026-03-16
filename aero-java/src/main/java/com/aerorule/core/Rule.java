package com.aerorule.core;

/**
 * Represents a business rule definition compatible with the AeroRule schema.
 * <p>
 * A Rule consists of a CEL condition that evaluates against a context,
 * and associated success or failure actions.
 */
public class Rule {
    private String id;
    private String name;
    private String description;
    private Integer priority;
    private String condition;
    private String sourceQuote;
    private String sourceDocument;
    private Action onSuccess;
    private Action onFailure;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getSourceQuote() { return sourceQuote; }
    public void setSourceQuote(String sourceQuote) { this.sourceQuote = sourceQuote; }
    public String getSourceDocument() { return sourceDocument; }
    public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }
    public Action getOnSuccess() { return onSuccess; }
    public void setOnSuccess(Action onSuccess) { this.onSuccess = onSuccess; }
    public Action getOnFailure() { return onFailure; }
    public void setOnFailure(Action onFailure) { this.onFailure = onFailure; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(name, that.name) &&
                java.util.Objects.equals(description, that.description) &&
                java.util.Objects.equals(priority, that.priority) &&
                java.util.Objects.equals(condition, that.condition) &&
                java.util.Objects.equals(sourceQuote, that.sourceQuote) &&
                java.util.Objects.equals(sourceDocument, that.sourceDocument) &&
                java.util.Objects.equals(onSuccess, that.onSuccess) &&
                java.util.Objects.equals(onFailure, that.onFailure);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, description, priority, condition, sourceQuote, sourceDocument, onSuccess, onFailure);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", priority='" + priority + '\'' +
                ", condition='" + condition + '\'' +
                ", sourceQuote='" + sourceQuote + '\'' +
                ", sourceDocument='" + sourceDocument + '\'' +
                ", onSuccess='" + onSuccess + '\'' +
                ", onFailure='" + onFailure + '\'' +
                '}';
    }
}
