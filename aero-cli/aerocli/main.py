import typer
from rich.console import Console
from .commands import init, gen, verify, test, config, models, run

app = typer.Typer(
    help="AeroRule CLI - The Command Center for the AeroRule ecosystem",
    rich_markup_mode="rich"
)
console = Console()

app.add_typer(init.app, name="init")
app.add_typer(gen.app, name="gen")
app.add_typer(verify.app, name="verify")
app.add_typer(test.app, name="test")
app.add_typer(config.app, name="config")
app.add_typer(models.app, name="models")
app.add_typer(run.app, name="run")

@app.command()
def version():
    """Show the version of aero-cli."""
    console.print("[bold blue]aero-cli[/bold blue] version [green]0.1.0[/green]")

if __name__ == "__main__":
    app()
