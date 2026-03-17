package com.aerorule.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    // ── Helper to build a Rule quickly ──────────────────────────────────
    private Rule makeRule(String id, String condition, String successAction, String failureAction) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setCondition(condition);
        if (successAction != null) {
            Action s = new Action();
            s.setAction(successAction);
            rule.setOnSuccess(s);
        }
        if (failureAction != null) {
            Action f = new Action();
            f.setAction(failureAction);
            rule.setOnFailure(f);
        }
        return rule;
    }

    // ── Helper: flat context from varargs key-value pairs ───────────────
    private Map<String, Object> ctx(Object... kvs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((String) kvs[i], kvs[i + 1]);
        }
        return map;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Basic evaluation
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Basic CEL Evaluation")
    class BasicEvaluation {

        @Test
        @DisplayName("Age eligibility – adult is ALLOWED")
        void ageCheckPass() {
            Rule rule = makeRule("AGE-001", "user_age >= 18", "ALLOW", "DENY");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("user_age", 25));

            assertTrue(trace.isMatched());
            assertEquals("ALLOW", trace.getActionTaken());
            assertEquals("AGE-001", trace.getRuleId());
        }

        @Test
        @DisplayName("Age eligibility – minor is DENIED")
        void ageCheckFail() {
            Rule rule = makeRule("AGE-001", "user_age >= 18", "ALLOW", "DENY");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("user_age", 16));

            assertFalse(trace.isMatched());
            assertEquals("DENY", trace.getActionTaken());
        }

        @Test
        @DisplayName("Simple boolean true literal")
        void trueLiteral() {
            Rule rule = makeRule("LITERAL-T", "true", "YES", "NO");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx());

            assertTrue(trace.isMatched());
            assertEquals("YES", trace.getActionTaken());
        }

        @Test
        @DisplayName("Simple boolean false literal")
        void falseLiteral() {
            Rule rule = makeRule("LITERAL-F", "false", "YES", "NO");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx());

            assertFalse(trace.isMatched());
            assertEquals("NO", trace.getActionTaken());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Complex financial-services expressions
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Financial Services – Complex Expressions")
    class FinancialServices {

        @Test
        @DisplayName("Credit score AND income – loan approved")
        void creditScoreAndIncomePass() {
            Rule rule = makeRule("CREDIT-001",
                    "credit_score >= 700 && annual_income > 50000",
                    "APPROVE_LOAN", "DENY_LOAN");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("credit_score", 750, "annual_income", 85000));

            assertTrue(trace.isMatched());
            assertEquals("APPROVE_LOAN", trace.getActionTaken());
        }

        @Test
        @DisplayName("Credit score AND income – denied (low score)")
        void creditScoreAndIncomeFail() {
            Rule rule = makeRule("CREDIT-001",
                    "credit_score >= 700 && annual_income > 50000",
                    "APPROVE_LOAN", "DENY_LOAN");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("credit_score", 620, "annual_income", 85000));

            assertFalse(trace.isMatched());
            assertEquals("DENY_LOAN", trace.getActionTaken());
        }

        @Test
        @DisplayName("AML – high-value transaction flagged (> $10,000)")
        void amlHighValueFlagged() {
            Rule rule = makeRule("AML-TX-001",
                    "transaction_amount > 10000.0",
                    "SUBMIT_STR", "LOG_COMPLIANT");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("transaction_amount", 15000.0));

            assertTrue(trace.isMatched());
            assertEquals("SUBMIT_STR", trace.getActionTaken());
        }

        @Test
        @DisplayName("AML – normal transaction passes")
        void amlNormalTransaction() {
            Rule rule = makeRule("AML-TX-001",
                    "transaction_amount > 10000.0",
                    "SUBMIT_STR", "LOG_COMPLIANT");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("transaction_amount", 5000.0));

            assertFalse(trace.isMatched());
            assertEquals("LOG_COMPLIANT", trace.getActionTaken());
        }

        @Test
        @DisplayName("KYC – verified customer with sufficient account age")
        void kycVerifiedPass() {
            Rule rule = makeRule("KYC-001",
                    "kyc_verified && account_age > 90",
                    "GRANT_ACCESS", "REQUIRE_VERIFICATION");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("kyc_verified", true, "account_age", 180));

            assertTrue(trace.isMatched());
            assertEquals("GRANT_ACCESS", trace.getActionTaken());
        }

        @Test
        @DisplayName("KYC – unverified customer denied")
        void kycUnverifiedFail() {
            Rule rule = makeRule("KYC-001",
                    "kyc_verified && account_age > 90",
                    "GRANT_ACCESS", "REQUIRE_VERIFICATION");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("kyc_verified", false, "account_age", 180));

            assertFalse(trace.isMatched());
            assertEquals("REQUIRE_VERIFICATION", trace.getActionTaken());
        }

        @Test
        @DisplayName("KYC – verified but new account denied")
        void kycNewAccountFail() {
            Rule rule = makeRule("KYC-001",
                    "kyc_verified && account_age > 90",
                    "GRANT_ACCESS", "REQUIRE_VERIFICATION");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("kyc_verified", true, "account_age", 30));

            assertFalse(trace.isMatched());
            assertEquals("REQUIRE_VERIFICATION", trace.getActionTaken());
        }

        @Test
        @DisplayName("Risk scoring with OR – high risk flagged")
        void riskScoreOrSanctions() {
            Rule rule = makeRule("RISK-001",
                    "risk_score > 8 || sanctions_hit == true",
                    "FREEZE_ACCOUNT", "CONTINUE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            // High risk score triggers
            Trace t1 = eval.evaluate(ctx("risk_score", 9, "sanctions_hit", false));
            assertTrue(t1.isMatched());
            assertEquals("FREEZE_ACCOUNT", t1.getActionTaken());
        }

        @Test
        @DisplayName("Risk scoring with OR – sanctions hit flagged")
        void sanctionsHitOnly() {
            Rule rule = makeRule("RISK-002",
                    "risk_score > 8 || sanctions_hit == true",
                    "FREEZE_ACCOUNT", "CONTINUE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace trace = eval.evaluate(ctx("risk_score", 3, "sanctions_hit", true));
            assertTrue(trace.isMatched());
            assertEquals("FREEZE_ACCOUNT", trace.getActionTaken());
        }

        @Test
        @DisplayName("Risk scoring with OR – clean customer continues")
        void riskClean() {
            Rule rule = makeRule("RISK-003",
                    "risk_score > 8 || sanctions_hit == true",
                    "FREEZE_ACCOUNT", "CONTINUE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace trace = eval.evaluate(ctx("risk_score", 3, "sanctions_hit", false));
            assertFalse(trace.isMatched());
            assertEquals("CONTINUE", trace.getActionTaken());
        }

        @Test
        @DisplayName("String comparison – account type check")
        void accountTypeCheck() {
            Rule rule = makeRule("ACCT-001",
                    "account_type == \"SAVINGS\"",
                    "APPLY_INTEREST", "SKIP");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace t1 = eval.evaluate(ctx("account_type", "SAVINGS"));
            assertTrue(t1.isMatched());
            assertEquals("APPLY_INTEREST", t1.getActionTaken());

            // Re-create evaluator for fresh compilation with different context shape
            RuleEvaluator eval2 = new RuleEvaluator(rule);
            Trace t2 = eval2.evaluate(ctx("account_type", "CHEQUING"));
            assertFalse(t2.isMatched());
        }

        @Test
        @DisplayName("Currency check – CAD transactions only")
        void currencyCheck() {
            Rule rule = makeRule("FX-001",
                    "currency == \"CAD\"",
                    "PROCESS_DOMESTIC", "PROCESS_FOREIGN");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace t1 = eval.evaluate(ctx("currency", "CAD"));
            assertTrue(t1.isMatched());
            assertEquals("PROCESS_DOMESTIC", t1.getActionTaken());
        }

        @Test
        @DisplayName("Compound AND/OR – age band and positive balance")
        void ageBandAndBalance() {
            Rule rule = makeRule("ELIG-001",
                    "(customer_age >= 18 && customer_age <= 65) && account_balance > 0",
                    "ELIGIBLE", "INELIGIBLE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            // Within band, positive balance
            Trace t1 = eval.evaluate(ctx("customer_age", 30, "account_balance", 5000));
            assertTrue(t1.isMatched());
            assertEquals("ELIGIBLE", t1.getActionTaken());
        }

        @Test
        @DisplayName("Compound AND/OR – too old, ineligible")
        void ageBandTooOld() {
            Rule rule = makeRule("ELIG-002",
                    "(customer_age >= 18 && customer_age <= 65) && account_balance > 0",
                    "ELIGIBLE", "INELIGIBLE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace trace = eval.evaluate(ctx("customer_age", 70, "account_balance", 5000));
            assertFalse(trace.isMatched());
            assertEquals("INELIGIBLE", trace.getActionTaken());
        }

        @Test
        @DisplayName("Compound AND/OR – zero balance, ineligible")
        void zeroBalance() {
            Rule rule = makeRule("ELIG-003",
                    "(customer_age >= 18 && customer_age <= 65) && account_balance > 0",
                    "ELIGIBLE", "INELIGIBLE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace trace = eval.evaluate(ctx("customer_age", 30, "account_balance", 0));
            assertFalse(trace.isMatched());
            assertEquals("INELIGIBLE", trace.getActionTaken());
        }

        @Test
        @DisplayName("Arithmetic – monthly payment exceeds limit")
        void monthlyPaymentExceedsLimit() {
            // loan_amount / term_months > 5000
            Rule rule = makeRule("PAYMENT-001",
                    "loan_amount / term_months > 5000",
                    "HIGH_PAYMENT_WARNING", "STANDARD");
            RuleEvaluator eval = new RuleEvaluator(rule);

            // 120000 / 12 = 10000 > 5000
            Trace t1 = eval.evaluate(ctx("loan_amount", 120000, "term_months", 12));
            assertTrue(t1.isMatched());
            assertEquals("HIGH_PAYMENT_WARNING", t1.getActionTaken());
        }

        @Test
        @DisplayName("Arithmetic – monthly payment within limit")
        void monthlyPaymentWithinLimit() {
            Rule rule = makeRule("PAYMENT-002",
                    "loan_amount / term_months > 5000",
                    "HIGH_PAYMENT_WARNING", "STANDARD");
            RuleEvaluator eval = new RuleEvaluator(rule);

            // 48000 / 60 = 800 <= 5000
            Trace trace = eval.evaluate(ctx("loan_amount", 48000, "term_months", 60));
            assertFalse(trace.isMatched());
            assertEquals("STANDARD", trace.getActionTaken());
        }

        @Test
        @DisplayName("Comparison with negation – not a PEP")
        void negationNotPep() {
            Rule rule = makeRule("PEP-001",
                    "!is_pep",
                    "STANDARD_ONBOARDING", "ENHANCED_DUE_DILIGENCE");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace t1 = eval.evaluate(ctx("is_pep", false));
            assertTrue(t1.isMatched());
            assertEquals("STANDARD_ONBOARDING", t1.getActionTaken());

            RuleEvaluator eval2 = new RuleEvaluator(rule);
            Trace t2 = eval2.evaluate(ctx("is_pep", true));
            assertFalse(t2.isMatched());
            assertEquals("ENHANCED_DUE_DILIGENCE", t2.getActionTaken());
        }

        @Test
        @DisplayName("Comparison operators – less-than-or-equal (debt ratio)")
        void debtRatioCheck() {
            Rule rule = makeRule("DTI-001",
                    "debt_to_income <= 43",
                    "QUALIFIES", "DOES_NOT_QUALIFY");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace t1 = eval.evaluate(ctx("debt_to_income", 35));
            assertTrue(t1.isMatched());

            RuleEvaluator eval2 = new RuleEvaluator(rule);
            Trace t2 = eval2.evaluate(ctx("debt_to_income", 55));
            assertFalse(t2.isMatched());
            assertEquals("DOES_NOT_QUALIFY", t2.getActionTaken());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Trace output validation
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Trace Validation")
    class TraceValidation {

        @Test
        @DisplayName("Trace captures ruleId and condition")
        void traceCarriesRuleMetadata() {
            String condition = "credit_score >= 700";
            Rule rule = makeRule("TRACE-META", condition, "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("credit_score", 800));

            assertEquals("TRACE-META", trace.getRuleId());
            assertEquals(condition, trace.getCondition());
        }

        @Test
        @DisplayName("Trace has non-negative execution time")
        void traceExecutionTime() {
            Rule rule = makeRule("TIME-001", "amount > 0", "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("amount", 100));

            assertNotNull(trace.getExecutionTimeMs());
            assertTrue(trace.getExecutionTimeMs() >= 0,
                    "Execution time must be non-negative");
        }

        @Test
        @DisplayName("Validation fails when onSuccess/onFailure are null")
        void failsWhenActionsNull() {
            Rule rule = makeRule("NOACT-001", "val > 0", null, null);
            RuleEvaluator eval = new RuleEvaluator(rule);

            Trace t1 = eval.evaluate(ctx("val", 10));
            assertFalse(t1.isMatched());
            assertNotNull(t1.getEvaluationError());
            assertTrue(t1.getEvaluationError().contains("at least one of 'onSuccess' or 'onFailure' must be defined"));
        }

        @Test
        @DisplayName("Successful evaluation has no error")
        void noErrorOnSuccess() {
            Rule rule = makeRule("NO-ERR", "x > 0", "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx("x", 5));

            assertNull(trace.getEvaluationError());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Error handling
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Non-boolean result sets evaluationError")
        void nonBooleanResult() {
            // 1 + 1 = 2, which is an int, not boolean
            Rule rule = makeRule("ERR-001", "1 + 1", "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx());

            assertFalse(trace.isMatched());
            assertNotNull(trace.getEvaluationError());
            assertTrue(trace.getEvaluationError().contains("Condition did not evaluate to a boolean"));
        }

        @Test
        @DisplayName("Invalid CEL expression sets evaluationError")
        void invalidCelExpression() {
            Rule rule = makeRule("ERR-002", "&&& invalid !!!", "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            Trace trace = eval.evaluate(ctx());

            assertFalse(trace.isMatched());
            assertNotNull(trace.getEvaluationError());
        }

        @Test
        @DisplayName("Missing context variable sets evaluationError")
        void missingContextVariable() {
            Rule rule = makeRule("ERR-003", "unknown_var > 0", "OK", "FAIL");
            RuleEvaluator eval = new RuleEvaluator(rule);
            // Provide empty context – variable 'unknown_var' not declared
            Trace trace = eval.evaluate(ctx("other_var", 1));

            assertFalse(trace.isMatched());
            assertNotNull(trace.getEvaluationError());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Compilation behaviour
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Compilation Behaviour")
    class CompilationBehaviour {

        @Test
        @DisplayName("JIT compile on first evaluate()")
        void jitCompileOnFirstEvaluate() {
            Rule rule = makeRule("JIT-001", "balance > 0", "ACTIVE", "DORMANT");
            RuleEvaluator eval = new RuleEvaluator(rule);

            // No explicit compile() call – evaluate should JIT-compile
            Trace trace = eval.evaluate(ctx("balance", 100));
            assertTrue(trace.isMatched());
            assertEquals("ACTIVE", trace.getActionTaken());
            assertNull(trace.getEvaluationError());
        }

        @Test
        @DisplayName("Pre-compile then evaluate")
        void preCompileAndEvaluate() throws Exception {
            Rule rule = makeRule("PRE-001", "limit > 1000", "HIGH_LIMIT", "STANDARD");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Map<String, Object> context = ctx("limit", 5000);
            eval.compile(context);
            Trace trace = eval.evaluate(context);

            assertTrue(trace.isMatched());
            assertEquals("HIGH_LIMIT", trace.getActionTaken());
        }

        @Test
        @DisplayName("Pre-compiled evaluator reuses program for same-shape context")
        void reuseCompiledProgram() throws Exception {
            Rule rule = makeRule("REUSE-001", "score >= 650", "PRIME", "SUBPRIME");
            RuleEvaluator eval = new RuleEvaluator(rule);

            Map<String, Object> ctx1 = ctx("score", 700);
            eval.compile(ctx1);

            Trace t1 = eval.evaluate(ctx1);
            assertTrue(t1.isMatched());

            // Same key, different value – program is already compiled
            Trace t2 = eval.evaluate(ctx("score", 600));
            assertFalse(t2.isMatched());
            assertEquals("SUBPRIME", t2.getActionTaken());
        }
    }
}
