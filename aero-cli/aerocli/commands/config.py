import typer
from rich.console import Console
from rich.table import Table
from ..config import load_config, CONFIG_FILE

app = typer.Typer(help="Manage AeroRule CLI configuration")
console = Console()

@app.command()
def show():
    """Show the current AeroRule CLI configuration."""
    config = load_config()
    if not config:
        console.print("[bold red]Error:[/bold red] No configuration found. Run [bold blue]aero init[/bold blue] first.")
        raise typer.Exit(1)
    
    table = Table(title="AeroRule CLI Configuration")
    table.add_column("Setting", style="cyan")
    table.add_column("Value", style="magenta")
    
    table.add_row("Config Path", str(CONFIG_FILE))
    table.add_row("LLM Provider", config.provider)
    table.add_row("Model", config.model)
    table.add_row("API Key", "********" if config.api_key else "None")
    table.add_row("Base URL", config.base_url or "Default")
    table.add_row("Rules Directory", config.rules_dir)
    
    console.print(table)

if __name__ == "__main__":
    app()
