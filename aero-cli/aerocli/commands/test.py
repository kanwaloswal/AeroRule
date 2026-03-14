import typer
import yaml
import json
from pathlib import Path
from typing import List, Dict, Any
from rich.console import Console
from rich.table import Table
import sys

# Importing from aero-python
sys.path.append("/Users/sunny/Documents/work/1000844339_Ontario/AeroRule/aero-python")
from aerorule.models import Rule
from aerorule.evaluator import RuleEvaluator
from ..config import load_config

app = typer.Typer(help="Run compliance tests against local rules")
console = Console()

def load_all_rules(rules_dir: Path, suite_rules: List[Dict[str, Any]]) -> Dict[str, Rule]:
    rules = {}
    
    # Load from suite
    for r_data in suite_rules:
        rule = Rule(**r_data)
        rules[rule.id] = rule
        
    # Load from local rules dir
    if rules_dir.exists():
        for rule_file in rules_dir.glob("*.json"):
            try:
                with open(rule_file, "r") as f:
                    data = json.load(f)
                    rule = Rule(**data)
                    rules[rule.id] = rule
            except Exception:
                continue
                
    return rules

@app.command()
def run(suite_path: Path = typer.Option("/Users/sunny/Documents/work/1000844339_Ontario/AeroRule/spec/compliance_suite.yaml", help="Path to compliance suite")):
    """Run compliance tests."""
    if not suite_path.exists():
        console.print(f"[bold red]Error:[/bold red] Compliance suite not found: [cyan]{suite_path}[/cyan]")
        raise typer.Exit(1)

    config = load_config()
    rules_dir = Path(config.rules_dir) if config else Path("./rules")

    try:
        with open(suite_path, "r") as f:
            suite = yaml.safe_load(f)
    except Exception as e:
        console.print(f"[bold red]Error:[/bold red] Failed to parse suite: {str(e)}")
        raise typer.Exit(1)

    rules = load_all_rules(rules_dir, suite.get("rules", []))
    tests = suite.get("tests", [])

    table = Table(title="AeroRule Compliance Suite Results")
    table.add_column("Test Name", style="cyan")
    table.add_column("Rule ID", style="magenta")
    table.add_column("Status", justify="center")
    table.add_column("Details", style="dim")

    passed = 0
    for test in tests:
        name = test.get("name")
        rule_id = test.get("ruleId")
        context = test.get("context", {})
        expected = test.get("expectedTrace", {})

        rule = rules.get(rule_id)
        if not rule:
            table.add_row(name, rule_id, "[red]FAIL[/red]", f"Rule {rule_id} not found")
            continue

        evaluator = RuleEvaluator(rule)
        trace = evaluator.evaluate(context)

        # Basic check: matched and actionTaken
        is_pass = True
        fail_reason = ""
        
        if trace.matched != expected.get("matched"):
            is_pass = False
            fail_reason += f"Expected matched={expected.get('matched')}, got {trace.matched}. "
            
        if expected.get("actionTaken") and trace.actionTaken != expected.get("actionTaken"):
            is_pass = False
            fail_reason += f"Expected action={expected.get('actionTaken')}, got {trace.actionTaken}. "

        if is_pass:
            table.add_row(name, rule_id, "[green]PASS[/green]", "✓")
            passed += 1
        else:
            table.add_row(name, rule_id, "[red]FAIL[/red]", fail_reason.strip())

    console.print(table)
    console.print(f"\n[bold]Summary: {passed}/{len(tests)} passed[/bold]")
    
    if passed < len(tests):
        raise typer.Exit(1)
