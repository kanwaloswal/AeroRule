import time
import celpy
from typing import Dict, Any
from .models import Rule, Trace

class RuleEvaluator:
    def __init__(self, rule: Rule):
        self.rule = rule
        
        self.env = celpy.Environment()
        try:
            ast = self.env.compile(self.rule.condition)
            self.program = self.env.program(ast)
        except Exception as e:
            self.program = None
            self.compile_error = str(e)

    def evaluate(self, context: Dict[str, Any]) -> Trace:
        start_time = time.time()
        
        trace = Trace(
            ruleId=self.rule.id,
            condition=self.rule.condition,
            matched=False
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

        except Exception as e:
            trace.evaluationError = str(e)

        trace.executionTimeMs = int((time.time() - start_time) * 1000)
        return trace
