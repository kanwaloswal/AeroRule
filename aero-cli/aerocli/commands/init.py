import typer
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from ..config import AeroConfig, save_config, CONFIG_FILE

app = typer.Typer(help="Initialize the AeroRule environment")
console = Console()

@app.callback(invoke_without_command=True)
def main():
    """Initialize the AeroRule CLI configuration."""
    console.print(Panel("[bold green]AeroRule Command Center Initialization[/bold green]"))
    
    provider = typer.prompt(
        "Select LLM Provider", 
        default="openai",
        type=typer.Choice(["openai", "anthropic", "gemini", "grok", "ollama"])
    )
    
    api_key = None
    base_url = None
    
    if provider != "ollama":
        api_key = typer.prompt("Enter your API Key", hide_input=True)
    else:
        base_url = typer.prompt("Enter Ollama Base URL", default="http://localhost:11434")

    if provider == "grok":
         base_url = typer.prompt("Enter Grok Base URL", default="https://api.x.ai/v1")

    rules_dir = typer.prompt("Local rules directory", default="./rules")
    
    config = AeroConfig(
        provider=provider,
        api_key=api_key,
        base_url=base_url,
        rules_dir=rules_dir
    )
    
    save_config(config)
    
    # Ensure rules directory exists
    rules_path = Path(rules_dir)
    if not rules_path.exists():
        rules_path.mkdir(parents=True)
        console.print(f"Created rules directory: [cyan]{rules_dir}[/cyan]")
    
    console.print(f"\n[bold green]Success![/bold green] Configuration saved to [cyan]{CONFIG_FILE}[/cyan]")
    console.print("You can now run [bold blue]aero gen[/bold blue] to generate rules.")
