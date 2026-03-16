import typer
import json
from pathlib import Path
from rich.console import Console
from rich.table import Table
from pydantic import ValidationError
import json
from aerorule.models import Rule
import celpy

app = typer.Typer(help="Verify an AeroRule file")
console = Console()

@app.command()
def file(path: Path = typer.Argument(..., help="Path to the rule JSON file")):
    """Verify a rule file matches the schema and has valid CEL logic."""
    if not path.exists():
        console.print(f"[bold red]Error:[/bold red] File not found: [cyan]{path}[/cyan]")
        raise typer.Exit(1)

    try:
        with open(path, "r") as f:
            data = json.load(f)
    except Exception as e:
        console.print(f"[bold red]Error:[/bold red] Failed to parse JSON: {str(e)}")
        raise typer.Exit(1)

    table = Table(title=f"Verification Report: {path.name}")
    table.add_column("Check", style="cyan")
    table.add_column("Status", justify="center")
    table.add_column("Details", style="magenta")

    # 1. Schema Validation
    try:
        Rule(**data)
        table.add_row("JSON Schema", "[green]PASS[/green]", "Matches AeroRule spec")
    except ValidationError as e:
        table.add_row("JSON Schema", "[red]FAIL[/red]", str(e))
    except Exception as e:
        table.add_row("JSON Schema", "[red]FAIL[/red]", str(e))

    # 2. CEL Logic Validation
    condition = data.get("condition")
    if condition:
        try:
            env = celpy.Environment()
            env.compile(condition)
            table.add_row("CEL Logic", "[green]PASS[/green]", "Compiles successfully")
        except Exception as e:
            table.add_row("CEL Logic", "[red]FAIL[/red]", str(e))
    else:
        table.add_row("CEL Logic", "[yellow]SKIP[/yellow]", "Condition field missing")

    console.print(table)

    if "[red]FAIL[/red]" in str(table):
        raise typer.Exit(1)
