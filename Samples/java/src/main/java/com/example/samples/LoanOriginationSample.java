package com.example.samples;

import com.aerorule.core.Rule;
import com.aerorule.core.RuleEvaluator;
import com.aerorule.core.Trace;
import com.aerorule.core.provider.FileSystemProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LoanOriginationSample {

    public static class Customer {
        private String id;
        private int riskScore;
        private long annualRevenue;

        public Customer(String id, int riskScore, long annualRevenue) {
            this.id = id;
            this.riskScore = riskScore;
            this.annualRevenue = annualRevenue;
        }

        public String getId() { return id; }
        public int getRiskScore() { return riskScore; }
        public long getAnnualRevenue() { return annualRevenue; }
    }

    public static class Account {
        private String type;

        public Account(String type) {
            this.type = type;
        }

        public String getType() { return type; }
    }

    public static void main(String[] args) {
        System.out.println("Evaluating Loan Origination Rule...\n");

        // 1. Initialize rules from the shared rules system
        FileSystemProvider provider = new FileSystemProvider("../rules");
        List<Rule> rules = provider.getRules();

        // Find our loan rule
        Optional<Rule> loanRule = rules.stream().filter(r -> r.getId().equals("CREDIT-001")).findFirst();

        if (loanRule.isEmpty()) {
            System.err.println("Could not find rule CREDIT-001");
            return;
        }

        RuleEvaluator evaluator = new RuleEvaluator(loanRule.get());

        // 2. Scenario: Successful loan evaluation
        System.out.println("Scenario 1: Low risk score, high revenue, commercial account.");
        Customer customerSuccess = new Customer("CUST-100", 600, 8000000L);
        Account accountCommercial = new Account("COMMERCIAL");
        
        Map<String, Object> contextSuccess = Map.of(
            "customer", customerSuccess,
            "account", accountCommercial
        );

        Trace traceSuccess = evaluator.evaluateWithObjects(contextSuccess);
        printTrace(traceSuccess);

        System.out.println("----------------------------------------");

        // 3. Scenario: Failed evaluation due to retail account
        System.out.println("Scenario 2: Low risk score, high income, retail account.");
        Customer customerFail = new Customer("CUST-200", 600, 8000000L);
        Account accountRetail = new Account("RETAIL");
        
        Map<String, Object> contextFail = Map.of(
            "customer", customerFail,
            "account", accountRetail
        );

        Trace traceFail = evaluator.evaluateWithObjects(contextFail);
        printTrace(traceFail);
    }

    private static void printTrace(Trace trace) {
        System.out.println("Condition matched: " + trace.isMatched());
        if (trace.getActionTaken() != null) {
            System.out.println("Action taken: " + trace.getActionTaken());
        }
        if (trace.getEvaluationError() != null) {
            System.out.println("Error: " + trace.getEvaluationError());
        }
        System.out.println("Execution time (ms): " + trace.getExecutionTimeMs());
    }
}
