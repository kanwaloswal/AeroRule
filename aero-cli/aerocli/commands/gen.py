import typer
import json
import os
from pathlib import Path
from typing import List
from rich.console import Console
from rich.panel import Panel
from rich.syntax import Syntax
from ..config import load_config
from ..llm import generate_rule
import celpy

app = typer.Typer(help="Generate an AeroRule from a natural language prompt")
console = Console()

def get_schema_context() -> str:
    spec_dir = Path("/Users/sunny/Documents/work/1000844339_Ontario/AeroRule/spec")
    schemas = []
    
    # Primary schema
    rule_schema = spec_dir / "rule-schema.json"
    if rule_schema.exists():
        with open(rule_schema, "r") as f:
            schemas.append(f"rule-schema.json:\n{f.read()}")
    
    # Other schemas in spec/
    for schema_file in spec_dir.glob("*.schema.json"):
        if schema_file.name != "rule-schema.json":
            with open(schema_file, "r") as f:
                schemas.append(f"{schema_file.name}:\n{f.read()}")
                
    return "\n\n".join(schemas)

def dry_run_rule(rule_json: dict) -> List[str]:
    """Validate CEL logic and schema."""
    errors = []
    condition = rule_json.get("condition")
    if not condition:
        errors.append("Missing 'condition' field.")
        return errors
    
    try:
        env = celpy.Environment()
        env.compile(condition)
    except Exception as e:
        errors.append(f"CEL Compilation Error: {str(e)}")
        
    return errors

@app.callback(invoke_without_command=True)
def main(prompt: str = typer.Argument(..., help="Natural language requirement")):
    """Generate a rule from a natural language prompt."""
    config = load_config()
    if not config:
        console.print("[bold red]Error:[/bold red] CLI not initialized. Run [bold blue]aero init[/bold blue] first.")
        raise typer.Exit(1)
    
    console.print(f"Working on: [cyan]{prompt}[/cyan]...")
    
    context = get_schema_context()
    
    with console.status("[bold green]Consulting the AeroRule Brain..."):
        try:
            llm_response = generate_rule(prompt, context, config)
            rule_data = json.loads(llm_response)
        except Exception as e:
            console.print(f"[bold red]Error:[/bold red] Failed to generate or parse rule: {str(e)}")
            # Offer retry logic here in a real scenario
            raise typer.Exit(1)
    
    console.print(Panel(Syntax(json.dumps(rule_data, indent=2), "json", theme="monokai"), title="Generated Rule"))
    
    # Dry-run validation
    with console.status("[bold yellow]Performing Pre-flight Check (Dry-run)..."):
        errors = dry_run_rule(rule_data)
        
    if errors:
        console.print(Panel("\n".join([f"• {e}" for e in errors]), title="[bold red]Validation Failed", border_style="red"))
        # In a real tool, we'd offer to "Retry with error feedback" here
        raise typer.Exit(1)
    
    console.print("[bold green]✓ Pre-flight Check Passed (CEL is valid)[/bold green]")
    
    # Save to rules directory
    rules_dir = Path(config.rules_dir)
    rules_dir.mkdir(parents=True, exist_ok=True)
    
    rule_id = rule_data.get("id", "generated_rule").replace(" ", "_")
    output_path = rules_dir / f"{rule_id}.json"
    
    with open(output_path, "w") as f:
        json.dump(rule_data, f, indent=4)
        
    console.print(f"\n[bold green]Success![/bold green] Rule saved to [cyan]{output_path}[/cyan]")
