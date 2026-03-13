package com.aerorule.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleTest {

    @Test
    void defaultValuesAreNull() {
        Rule rule = new Rule();
        assertNull(rule.getId());
        assertNull(rule.getDescription());
        assertNull(rule.getPriority());
        assertNull(rule.getCondition());
        assertNull(rule.getOnSuccess());
        assertNull(rule.getOnFailure());
    }

    @Test
    void setAndGetId() {
        Rule rule = new Rule();
        rule.setId("AML-001");
        assertEquals("AML-001", rule.getId());
    }

    @Test
    void setAndGetDescription() {
        Rule rule = new Rule();
        rule.setDescription("Flag transactions exceeding AML threshold of $10,000 CAD");
        assertEquals("Flag transactions exceeding AML threshold of $10,000 CAD", rule.getDescription());
    }

    @Test
    void setAndGetPriority() {
        Rule rule = new Rule();
        rule.setPriority(1);
        assertEquals(1, rule.getPriority());
    }

    @Test
    void setAndGetCondition() {
        Rule rule = new Rule();
        rule.setCondition("transaction.amount > 10000.0 && transaction.currency == \"CAD\"");
        assertEquals("transaction.amount > 10000.0 && transaction.currency == \"CAD\"", rule.getCondition());
    }

    @Test
    void setAndGetOnSuccess() {
        Rule rule = new Rule();
        Action action = new Action();
        action.setAction("SUBMIT_STR");
        action.setMetadata(Map.of("reportType", "Suspicious Transaction Report"));
        rule.setOnSuccess(action);

        assertNotNull(rule.getOnSuccess());
        assertEquals("SUBMIT_STR", rule.getOnSuccess().getAction());
    }

    @Test
    void setAndGetOnFailure() {
        Rule rule = new Rule();
        Action action = new Action();
        action.setAction("LOG_COMPLIANT");
        rule.setOnFailure(action);

        assertNotNull(rule.getOnFailure());
        assertEquals("LOG_COMPLIANT", rule.getOnFailure().getAction());
    }

    @Test
    void fullFinancialRuleConfiguration() {
        Rule rule = new Rule();
        rule.setId("CREDIT-001");
        rule.setDescription("Approve loan if credit score >= 700 and income > $50K");
        rule.setPriority(10);
        rule.setCondition("credit_score >= 700 && annual_income > 50000");

        Action approve = new Action();
        approve.setAction("APPROVE_LOAN");
        approve.setMetadata(Map.of("maxAmount", 250000, "rate", "4.5%"));
        rule.setOnSuccess(approve);

        Action deny = new Action();
        deny.setAction("DENY_LOAN");
        deny.setMetadata(Map.of("reason", "Does not meet credit criteria"));
        rule.setOnFailure(deny);

        assertEquals("CREDIT-001", rule.getId());
        assertEquals(10, rule.getPriority());
        assertEquals("APPROVE_LOAN", rule.getOnSuccess().getAction());
        assertEquals("DENY_LOAN", rule.getOnFailure().getAction());
        assertEquals("Does not meet credit criteria", rule.getOnFailure().getMetadata().get("reason"));
    }
}
