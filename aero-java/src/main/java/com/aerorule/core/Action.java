package com.aerorule.core;

import java.util.Map;

/**
 * Describes the action to be taken and metadata to accompany it 
 * when a rule evaluation successfully matches or strictly fails.
 */
public class Action {
    private String action;
    private Map<String, Object> metadata;
    private String next;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action that = (Action) o;
        return java.util.Objects.equals(action, that.action) &&
                java.util.Objects.equals(metadata, that.metadata) &&
                java.util.Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(action, metadata, next);
    }

    @Override
    public String toString() {
        return "Action{" +
                "action='" + action + '\'' +
                ", metadata='" + metadata + '\'' +
                ", next='" + next + '\'' +
                '}';
    }
}
