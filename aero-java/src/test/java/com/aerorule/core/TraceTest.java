package com.aerorule.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceTest {

    @Test
    void defaultValues() {
        Trace trace = new Trace();
        assertNull(trace.getRuleId());
        assertNull(trace.getCondition());
        assertFalse(trace.isMatched());
        assertNull(trace.getEvaluationError());
        assertNull(trace.getExecutionTimeMs());
        assertNull(trace.getActionTaken());
    }

    @Test
    void setAndGetRuleId() {
        Trace trace = new Trace();
        trace.setRuleId("KYC-VERIFY-001");
        assertEquals("KYC-VERIFY-001", trace.getRuleId());
    }

    @Test
    void setAndGetCondition() {
        Trace trace = new Trace();
        trace.setCondition("customer.kycVerified && customer.accountAge > 90");
        assertEquals("customer.kycVerified && customer.accountAge > 90", trace.getCondition());
    }

    @Test
    void setAndGetMatched() {
        Trace trace = new Trace();
        assertFalse(trace.isMatched());

        trace.setMatched(true);
        assertTrue(trace.isMatched());

        trace.setMatched(false);
        assertFalse(trace.isMatched());
    }

    @Test
    void setAndGetEvaluationError() {
        Trace trace = new Trace();
        trace.setEvaluationError("Undeclared reference to 'customer'");
        assertEquals("Undeclared reference to 'customer'", trace.getEvaluationError());
    }

    @Test
    void setAndGetExecutionTimeMs() {
        Trace trace = new Trace();
        trace.setExecutionTimeMs(42L);
        assertEquals(42L, trace.getExecutionTimeMs());
    }

    @Test
    void setAndGetActionTaken() {
        Trace trace = new Trace();
        trace.setActionTaken("FREEZE_ACCOUNT");
        assertEquals("FREEZE_ACCOUNT", trace.getActionTaken());
    }

    @Test
    void setAndGetInputs() {
        Trace trace = new Trace();
        java.util.Map<String, Object> inputs = new java.util.HashMap<>();
        inputs.put("amount", 500);
        trace.setInputs(inputs);
        assertEquals(500, trace.getInputs().get("amount"));
    }

    @Test
    void setAndGetEvaluatedExpressions() {
        Trace trace = new Trace();
        java.util.Map<String, Object> evals = new java.util.HashMap<>();
        evals.put("amount > 100", true);
        trace.setEvaluatedExpressions(evals);
        assertEquals(true, trace.getEvaluatedExpressions().get("amount > 100"));
    }

    @Test
    void fullTraceForComplianceAudit() {
        Trace trace = new Trace();
        trace.setRuleId("AML-TX-001");
        trace.setCondition("transaction.amount > 10000.0");
        trace.setMatched(true);
        trace.setActionTaken("SUBMIT_STR");
        trace.setExecutionTimeMs(5L);

        assertEquals("AML-TX-001", trace.getRuleId());
        assertTrue(trace.isMatched());
        assertEquals("SUBMIT_STR", trace.getActionTaken());
        assertNull(trace.getEvaluationError());
        assertNotNull(trace.getExecutionTimeMs());
        assertTrue(trace.getExecutionTimeMs() >= 0);
    }
}
