import time
import logging
import celpy
from typing import Dict, Any
from .models import Rule, Trace

logger = logging.getLogger(__name__)

class RuleEvaluator:
    """
    Core evaluation engine for a single Rule. 
    Compiles a CEL expression and evaluates it against a given context.
    """
    def __init__(self, rule: Rule):
        self.rule = rule
        
        self.env = celpy.Environment()
        try:
            logger.debug("Compiling CEL expression for rule [%s]: %s", rule.id, rule.condition)
            ast = self.env.compile(self.rule.condition)
            self.program = self.env.program(ast)
            logger.debug("Successfully compiled rule [%s]", rule.id)
        except Exception as e:
            self.program = None
            self.compile_error = str(e)
            logger.warning("Failed to compile rule [%s]: %s", rule.id, e)

    def evaluate(self, context: Dict[str, Any]) -> Trace:
        logger.debug("Evaluating rule [%s] with context keys: %s", self.rule.id, list(context.keys()))
        start_time = time.time()
        
        trace = Trace(
            ruleId=self.rule.id,
            condition=self.rule.condition,
            matched=False,
            inputs=context,
            evaluatedExpressions={}
        )

        if self.program is None:
            trace.evaluationError = f"Compile error: {self.compile_error}"
            trace.executionTimeMs = int((time.time() - start_time) * 1000)
            return trace

        try:
            activation = celpy.json_to_cel(context)
            result = self.program.evaluate(activation)
            
            trace.matched = bool(result)
            
            action = self.rule.onSuccess if trace.matched else self.rule.onFailure
            if action:
                trace.actionTaken = action.action
            logger.debug("Rule [%s] evaluated: matched=%s, action=%s", self.rule.id, trace.matched, trace.actionTaken)

        except Exception as e:
            trace.evaluationError = str(e)
            logger.error("Rule [%s] evaluation failed: %s", self.rule.id, e)

        trace.executionTimeMs = int((time.time() - start_time) * 1000)
        logger.debug("Rule [%s] completed in %dms", self.rule.id, trace.executionTimeMs)
        return trace
