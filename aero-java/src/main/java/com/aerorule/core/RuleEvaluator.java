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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Core evaluation engine for a single Rule. 
 * <p>
 * Compiles a CEL expression and evaluates it against a given context.
 * Thread-safe for evaluation, but recompiles CEL dynamically if not pre-compiled.
 */
public class RuleEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluator.class);
    private final Rule rule;
    private CelRuntime.Program program;
    private static final ObjectMapper mapper = new ObjectMapper();

    public RuleEvaluator(Rule rule) {
        this.rule = rule;
    }

    public void compile(Map<String, Object> contextDefinition) throws CelValidationException, CelEvaluationException {
        logger.debug("Compiling CEL expression for rule [{}]: {}", rule.getId(), rule.getCondition());
        dev.cel.compiler.CelCompilerBuilder compilerBuilder = CelCompilerFactory.standardCelCompilerBuilder();
        for (String key : contextDefinition.keySet()) {
            compilerBuilder.addVar(key, SimpleType.DYN);
        }
        
        CelCompiler compiler = compilerBuilder.build();
        CelAbstractSyntaxTree ast = compiler.compile(rule.getCondition()).getAst();
        
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
        this.program = runtime.createProgram(ast);
        logger.debug("Successfully compiled rule [{}]", rule.getId());
    }



    public Trace evaluate(Map<String, Object> context) {
        logger.debug("Evaluating rule [{}] with context keys: {}", rule.getId(), context.keySet());
        long startTime = System.currentTimeMillis();
        Trace trace = new Trace();
        trace.setRuleId(rule.getId() != null ? rule.getId() : "<unknown>");
        trace.setCondition(rule.getCondition() != null ? rule.getCondition() : "");
        trace.setInputs(context);
        trace.setEvaluatedExpressions(new java.util.HashMap<>());

        try {
            rule.validate();
        } catch (IllegalStateException e) {
            trace.setMatched(false);
            trace.setEvaluationError(e.getMessage());
            logger.error(e.getMessage());
            trace.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return trace;
        }

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
                logger.debug("Rule [{}] evaluated: matched={}, action={}", rule.getId(), matched, trace.getActionTaken());
            } else {
                trace.setMatched(false);
                trace.setEvaluationError("Condition did not evaluate to a boolean: " + result);
                logger.warn("Rule [{}] condition returned non-boolean: {}", rule.getId(), result);
            }
        } catch (Exception e) {
            trace.setMatched(false);
            trace.setEvaluationError(e.getMessage());
            logger.error("Rule [{}] evaluation failed: {}", rule.getId(), e.getMessage(), e);
        }

        trace.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        logger.debug("Rule [{}] completed in {}ms", rule.getId(), trace.getExecutionTimeMs());
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
