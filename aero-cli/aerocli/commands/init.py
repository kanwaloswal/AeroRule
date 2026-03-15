import typer
import click
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from ..config import AeroConfig, save_config, CONFIG_FILE, load_config

app = typer.Typer(help="Initialize the AeroRule environment")
console = Console()

@app.callback(invoke_without_command=True)
def main():
    """Initialize the AeroRule CLI configuration."""
    console.print(Panel("[bold green]AeroRule Command Center Initializer[/bold green]"))
    
    current_config = load_config()
    if current_config:
        console.print(f"Current configuration found at [cyan]{CONFIG_FILE}[/cyan].")
        if not typer.confirm("Do you want to re-initialize?"):
             raise typer.Exit()

    provider = typer.prompt(
        "Select LLM Provider", 
        default=current_config.provider if current_config else "openai",
        type=click.Choice(["openai", "anthropic", "gemini", "grok", "ollama"])
    )
    
    # Model defaults based on provider
    model_default = "gpt-4o"
    if provider == "anthropic":
        model_default = "claude-3-5-sonnet-20240620"
    elif provider == "gemini":
        model_default = "gemini-1.5-pro"
    elif provider == "grok":
        model_default = "grok-1"
    elif provider == "ollama":
        model_default = "llama3"

    model = typer.prompt(
        "Select Model Name",
        default=current_config.model if current_config and current_config.provider == provider else model_default
    )
    
    api_key = current_config.api_key if current_config else None
    base_url = current_config.base_url if current_config else None
    
    if provider != "ollama":
        api_key = typer.prompt("Enter your API Key", default=api_key or "", hide_input=True, show_default=False)
    else:
        base_url = typer.prompt("Enter Ollama Base URL", default=base_url or "http://localhost:11434")

    if provider == "grok":
         base_url = typer.prompt("Enter Grok Base URL", default=base_url or "https://api.x.ai/v1")

    rules_dir = typer.prompt("Local rules directory", default=current_config.rules_dir if current_config else "./rules")
    
    config = AeroConfig(
        provider=provider,
        model=model,
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
