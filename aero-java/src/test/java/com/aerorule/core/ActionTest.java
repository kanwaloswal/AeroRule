package com.aerorule.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionTest {

    @Test
    void defaultValuesAreNull() {
        Action action = new Action();
        assertNull(action.getAction());
        assertNull(action.getMetadata());
    }

    @Test
    void setAndGetAction() {
        Action action = new Action();
        action.setAction("APPROVE_LOAN");
        assertEquals("APPROVE_LOAN", action.getAction());
    }

    @Test
    void setAndGetMetadata() {
        Action action = new Action();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", "Credit score exceeds threshold");
        metadata.put("approvedLimit", 50000);
        action.setMetadata(metadata);

        assertNotNull(action.getMetadata());
        assertEquals(2, action.getMetadata().size());
        assertEquals("Credit score exceeds threshold", action.getMetadata().get("reason"));
        assertEquals(50000, action.getMetadata().get("approvedLimit"));
    }

    @Test
    void overwriteActionValue() {
        Action action = new Action();
        action.setAction("FLAG_TRANSACTION");
        action.setAction("ESCALATE_TO_COMPLIANCE");
        assertEquals("ESCALATE_TO_COMPLIANCE", action.getAction());
    }

    @Test
    void metadataCanBeEmpty() {
        Action action = new Action();
        action.setMetadata(new HashMap<>());
        assertNotNull(action.getMetadata());
        assertTrue(action.getMetadata().isEmpty());
    }
}
