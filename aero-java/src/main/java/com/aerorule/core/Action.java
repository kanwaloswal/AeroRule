package com.aerorule.core;

import java.util.Map;

public class Action {
    private String action;
    private Map<String, Object> metadata;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
