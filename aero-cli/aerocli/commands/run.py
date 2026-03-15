import typer
import json
from pathlib import Path
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
import sys

sys.path.append(str(Path(__file__).resolve().parent.parent.parent.parent / "aero-python"))
from aerorule.engine import RuleSetEngine

app = typer.Typer(help="Run an AeroRule ruleset against a context")
console = Console()


@app.command()
def ruleset(
    ruleset_path: Path = typer.Argument(..., help="Path to the ruleset JSON file"),
    context: Path = typer.Option(..., "--context", "-c", help="Path to the context JSON file"),
):
    """Evaluate a ruleset against a JSON context and display the results."""

    if not ruleset_path.exists():
        console.print(f"[bold red]Error:[/bold red] Ruleset file not found: [cyan]{ruleset_path}[/cyan]")
        raise typer.Exit(1)

    if not context.exists():
        console.print(f"[bold red]Error:[/bold red] Context file not found: [cyan]{context}[/cyan]")
        raise typer.Exit(1)

    try:
        engine = RuleSetEngine.from_file(str(ruleset_path))
    except Exception as e:
        console.print(f"[bold red]Error loading ruleset:[/bold red] {e}")
        raise typer.Exit(1)

    try:
        with open(context, "r") as f:
            ctx = json.load(f)
    except Exception as e:
        console.print(f"[bold red]Error loading context:[/bold red] {e}")
        raise typer.Exit(1)

    result = engine.evaluate(ctx)

    # Header
    status_color = "green" if result.passed else "red"
    status_text = "PASSED ✅" if result.passed else "FAILED ❌"
    console.print(
        Panel(
            f"[bold]{engine.ruleset.name or engine.ruleset.id}[/bold]\n"
            f"Strategy: [cyan]{result.strategy}[/cyan]  |  "
            f"Result: [{status_color}]{status_text}[/{status_color}]  |  "
            f"{result.summary}  |  "
            f"{result.executionTimeMs}ms",
            title="[bold blue]AeroRule Engine[/bold blue]",
            border_style="blue",
        )
    )

    # Trace table
    table = Table(show_header=True, header_style="bold magenta")
    table.add_column("#", style="dim", width=4)
    table.add_column("Rule ID", style="cyan")
    table.add_column("Name", style="white")
    table.add_column("Result", justify="center")
    table.add_column("Action", style="yellow")
    table.add_column("Time", justify="right", style="dim")

    for idx, trace in enumerate(result.traces, 1):
        # Find original rule for the name
        rule_name = ""
        for r in engine.ruleset.rules:
            if r.id == trace.ruleId:
                rule_name = r.name or ""
                break

        if trace.matched:
            result_cell = "[green]PASS[/green]"
        elif trace.evaluationError:
            result_cell = f"[red]ERROR[/red]"
        else:
            result_cell = "[red]FAIL[/red]"

        table.add_row(
            str(idx),
            trace.ruleId,
            rule_name,
            result_cell,
            trace.actionTaken or "-",
            f"{trace.executionTimeMs or 0}ms",
        )

    console.print(table)
