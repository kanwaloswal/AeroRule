from functools import wraps
from typing import Any, Callable, Dict
from .models import Rule
from .evaluator import RuleEvaluator

def aerorule(rule_def: Dict[str, Any]):
    """
    Decorator to wrap a function with an AeroRule evaluation.
    Context is derived from kwargs passed to the function.
    """
    rule = Rule(**rule_def)
    evaluator = RuleEvaluator(rule)
    
    def decorator(func: Callable):
        @wraps(func)
        def wrapper(*args, **kwargs):
            trace = evaluator.evaluate(kwargs)
            if trace.matched:
                return func(*args, **kwargs)
            else:
                return {
                    "error": "AeroRule evaluation failed or denied access",
                    "trace": trace.model_dump()
                }
        return wrapper
    return decorator
