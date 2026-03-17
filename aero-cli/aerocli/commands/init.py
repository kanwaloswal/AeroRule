import os
import typer
import click
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from ..config import AeroConfig, save_config, CONFIG_FILE, load_config, ENV_API_KEY

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
    
    api_key = None
    base_url = current_config.base_url if current_config else None
    exclude_api_key = False
    
    if provider != "ollama":
        # Check if env var is already set
        env_key = os.environ.get(ENV_API_KEY)
        if env_key:
            console.print(f"[green]✓[/green] API key detected from environment variable [cyan]{ENV_API_KEY}[/cyan].")
            api_key = env_key
            exclude_api_key = True
        else:
            console.print(f"\n[bold yellow]⚠ Security Recommendation:[/bold yellow] Set the [cyan]{ENV_API_KEY}[/cyan] environment variable instead of storing keys in the config file.")
            console.print(f"  Example: [dim]export {ENV_API_KEY}=sk-your-key-here[/dim]\n")
            if typer.confirm("Would you like to enter the API key now (stored in plaintext config)?", default=True):
                api_key = typer.prompt("Enter your API Key", default="", hide_input=True, show_default=False)
                if api_key:
                    console.print("[yellow]⚠ API key will be stored in plaintext.[/yellow] Consider using environment variables for production.")
                else:
                    api_key = None
            else:
                console.print(f"Skipping. Set [cyan]{ENV_API_KEY}[/cyan] before running [bold blue]aero gen[/bold blue].")
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
    
    save_config(config, exclude_api_key=exclude_api_key)
    
    # Ensure rules directory exists
    rules_path = Path(rules_dir)
    if not rules_path.exists():
        rules_path.mkdir(parents=True)
        console.print(f"Created rules directory: [cyan]{rules_dir}[/cyan]")
    
    console.print(f"\n[bold green]Success![/bold green] Configuration saved to [cyan]{CONFIG_FILE}[/cyan]")
    console.print("You can now run [bold blue]aero gen[/bold blue] to generate rules.")

