package com.aerorule.core;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelEvaluationException;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;public class RuleEvaluator {
    private final Rule rule;
    private CelRuntime.Program program;
    private static final ObjectMapper mapper = new ObjectMapper();

    public RuleEvaluator(Rule rule) {
        this.rule = rule;
    }

    public void compile(Map<String, Object> contextDefinition) throws CelValidationException, CelEvaluationException {
        dev.cel.compiler.CelCompilerBuilder compilerBuilder = CelCompilerFactory.standardCelCompilerBuilder();
        for (String key : contextDefinition.keySet()) {
            compilerBuilder.addVar(key, SimpleType.DYN);
        }
        
        CelCompiler compiler = compilerBuilder.build();
        CelAbstractSyntaxTree ast = compiler.compile(rule.getCondition()).getAst();
        
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
        this.program = runtime.createProgram(ast);
    }



    public Trace evaluate(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        Trace trace = new Trace();
        trace.setRuleId(rule.getId());
        trace.setCondition(rule.getCondition());

        try {
            if (this.program == null) {
                compile(context); // JIT compile if not pre-compiled
            }
            
            Object result = program.eval(context);
            if (result instanceof Boolean) {
                boolean matched = (Boolean) result;
                trace.setMatched(matched);
                
                Action action = matched ? rule.getOnSuccess() : rule.getOnFailure();
                if (action != null) {
                    trace.setActionTaken(action.getAction());
                }
            } else {
                trace.setMatched(false);
                trace.setEvaluationError("Condition did not evaluate to a boolean: " + result);
            }
        } catch (Exception e) {
            trace.setMatched(false);
            trace.setEvaluationError(e.getMessage());
        }

        trace.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return trace;
    }

    public Trace evaluateWithObjects(Map<String, Object> objectContext) {
        // Convert all POJOs in the map into their Map representations
        Map<String, Object> executionContext = new java.util.HashMap<>();
        
        for (Map.Entry<String, Object> entry : objectContext.entrySet()) {
            // Convert each POJO (e.g., Customer, Approver) to a Map
            Map<String, Object> objectAsMap = mapper.convertValue(entry.getValue(), new TypeReference<Map<String, Object>>() {});
            executionContext.put(entry.getKey(), objectAsMap);
        }
        
        // If not compiled yet, JIT compile with the mapped definitions
        if (this.program == null) {
            try {
                compile(executionContext);
            } catch (Exception e) {
                Trace trace = new Trace();
                trace.setRuleId(rule.getId());
                trace.setCondition(rule.getCondition());
                trace.setMatched(false);
                trace.setEvaluationError("Failed to JIT compile rule for objects: " + e.getMessage());
                return trace;
            }
        }

        // Defer to the original Map-based evaluate logic
        return evaluate(executionContext);
    }
}
