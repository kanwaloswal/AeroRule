import typer
import json
from pathlib import Path
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
import sys

app = typer.Typer(help="Show a visual summary of an AeroRule ruleset")
console = Console()

@app.command()
def ruleset(
    ruleset_path: Path = typer.Argument(..., help="Path to the ruleset JSON file"),
):
    """Render a visual summary of a ruleset and its execution strategy."""

    if not ruleset_path.exists():
        console.print(f"[bold red]Error:[/bold red] Ruleset file not found: [cyan]{ruleset_path}[/cyan]")
        raise typer.Exit(1)

    try:
        with open(ruleset_path, "r") as f:
            rs = json.load(f)
    except Exception as e:
        console.print(f"[bold red]Error loading ruleset:[/bold red] {e}")
        raise typer.Exit(1)

    rs_id = rs.get("id", "Unknown")
    rs_name = rs.get("name", "")
    strategy = rs.get("executionStrategy", "ALL")
    rules = rs.get("rules", [])

    # Header Panel
    console.print(
        Panel(
            f"[bold]{rs_name or rs_id}[/bold]\n"
            f"Strategy: [cyan]{strategy}[/cyan]  |  "
            f"Rules: {len(rules)}",
            title="[bold blue]AeroRule RuleSet Summary[/bold blue]",
            border_style="blue",
        )
    )

    # Table of rules
    table = Table(show_header=True, header_style="bold magenta")
    table.add_column("#", style="dim", width=4)
    table.add_column("Rule ID", style="cyan")
    table.add_column("Priority", justify="right")
    table.add_column("Condition", style="white")
    table.add_column("On Success", style="green")
    table.add_column("On Failure", style="red")

    if strategy == "PRIORITY_ORDERED":
        ordered_rules = sorted(rules, key=lambda r: r.get("priority", 0) if r.get("priority") is not None else 0, reverse=True)
        display_rules = ordered_rules
        console.print("\n[dim]Rules are shown in execution order (highest priority first).[/dim]")
    else:
        display_rules = rules

    for idx, r in enumerate(display_rules, 1):
        condition = r.get("condition", "")
        if len(condition) > 50:
            condition = condition[:47] + "..."
            
        success = r.get("onSuccess", {})
        success_str = success.get("action", "-")
        if success.get("next"):
            success_str += f" ➔ [cyan]{success['next']}[/cyan]"

        failure = r.get("onFailure", {})
        failure_str = failure.get("action", "-")
        if failure.get("next"):
            failure_str += f" ➔ [cyan]{failure['next']}[/cyan]"

        priority = str(r.get("priority", "-"))

        table.add_row(
            str(idx),
            r.get("id", ""),
            priority,
            condition,
            success_str,
            failure_str,
        )

    console.print(table)

    # ascii flowchart for FLOW
    if strategy == "FLOW" and rules:
        console.print("\n[bold cyan]Execution Flowchart:[/bold cyan]\n")
        
        rule_map = {r["id"]: r for r in rules}
        
        def render_chain(rule_id, indent=""):
            if not rule_id or rule_id not in rule_map:
                return

            rule = rule_map[rule_id]
            sid = rule["id"]
            
            s_action = rule.get("onSuccess", {})
            f_action = rule.get("onFailure", {})
            s_next = s_action.get("next")
            f_next = f_action.get("next")
            
            # Simple rendering for now to avoid complex cycle rendering in CLI layout
            console.print(f"{indent}[bold cyan]{sid}[/bold cyan]")
            
            # Success branch
            if s_next:
                console.print(f"{indent}  ├── [green]✅ pass[/green] ➔ [cyan]{s_next}[/cyan]")
                render_chain(s_next, indent + "  │   ")
            else:
                console.print(f"{indent}  ├── [green]✅ pass[/green] ➔ [dim](terminal: {s_action.get('action', '-')})[/dim]")
                
            # Failure branch
            if f_next:
                console.print(f"{indent}  └── [red]✖ fail[/red] ➔ [cyan]{f_next}[/cyan]")
                render_chain(f_next, indent + "      ")
            else:
                console.print(f"{indent}  └── [red]✖ fail[/red] ➔ [dim](terminal: {f_action.get('action', '-')})[/dim]")

        # Render tree from first rule
        first_id = rules[0].get("id")
        render_chain(first_id)
        console.print("")
