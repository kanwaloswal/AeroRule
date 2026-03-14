package com.example.samples;

import com.aerorule.core.Rule;
import com.aerorule.core.RuleEvaluator;
import com.aerorule.core.Trace;
import com.aerorule.core.provider.FileSystemProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AmlTransactionSample {

    public static class Transaction {
        private String id;
        private double amount;
        private String currency;

        public Transaction(String id, double amount, String currency) {
            this.id = id;
            this.amount = amount;
            this.currency = currency;
        }

        public String getId() {
            return id;
        }

        public double getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }
    }

    public static class Customer {
        private String id;
        private String type;

        public Customer(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }

    public static void main(String[] args) {
        System.out.println("Evaluating AML Transaction Rule...\n");

        FileSystemProvider provider = new FileSystemProvider("../rules");
        List<Rule> rules = provider.loadRules();

        Optional<Rule> amlRule = rules.stream().filter(r -> r.getId().equals("AML-TX-001")).findFirst();

        if (amlRule.isEmpty()) {
            System.err.println("Could not find rule AML-TX-001");
            return;
        }

        RuleEvaluator evaluator = new RuleEvaluator(amlRule.get());

        System.out.println("Scenario 1: High transaction amount.");
        Transaction tx1 = new Transaction("TX-999", 15000.0, "USD");
        Customer cust1 = new Customer("CUST-300", "RETAIL");

        Map<String, Object> contextHigh = Map.of(
                "transaction", tx1,
                "customer", cust1);

        Trace traceHigh = evaluator.evaluateWithObjects(contextHigh);
        printTrace(traceHigh);

        System.out.println("----------------------------------------");

        System.out.println("Scenario 2: Low transaction amount.");
        Transaction tx2 = new Transaction("TX-111", 5000.0, "USD");
        Customer cust2 = new Customer("CUST-300", "RETAIL");

        Map<String, Object> contextLow = Map.of(
                "transaction", tx2,
                "customer", cust2);

        Trace traceLow = evaluator.evaluateWithObjects(contextLow);
        printTrace(traceLow);
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
