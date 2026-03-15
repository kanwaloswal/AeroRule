import typer
import subprocess
import json
import re
from pathlib import Path
from rich.console import Console

app = typer.Typer(help="Generate code models from JSON Schema")
console = Console()

def generate_python(schema_path: Path, out_dir: Path):
    """Use datamodel-code-generator to generate Pydantic models."""
    out_file = out_dir / "models.py"
    try:
        subprocess.run([
            "datamodel-codegen",
            "--input", str(schema_path),
            "--input-file-type", "jsonschema",
            "--output", str(out_file),
            "--output-model-type", "pydantic_v2.BaseModel"
        ], check=True, capture_output=True)
        console.print(f"[bold green]✓[/bold green] Python models generated at {out_file}")
    except subprocess.CalledProcessError as e:
        console.print(f"[bold red]Error generating Python models:[/bold red] {e.stderr.decode()}")
        raise typer.Exit(1)
    except FileNotFoundError:
        console.print("[bold red]Error:[/bold red] `datamodel-codegen` not found. Is datamodel-code-generator installed in your environment?")
        raise typer.Exit(1)

def generate_java(schema_path: Path, out_dir: Path):
    """Generates standard Java POJOs from JSON schema definitions."""
    with open(schema_path, "r") as f:
        try:
            schema = json.load(f)
        except json.JSONDecodeError as e:
            console.print(f"[bold red]Error parsing schema JSON:[/bold red] {str(e)}")
            raise typer.Exit(1)
            
    # Naive title extraction, fallback to filename
    title = schema.get("title")
    if not title:
        title = schema_path.stem.split(".")[0]
        title = "".join(word.capitalize() for word in title.split("_"))
        
    properties = schema.get("properties", {})
    if not properties:
        console.print(f"[yellow]Warning:[/yellow] No properties found in {schema_path.name}")
        return
        
    # Map JSON schema types to Java types
    type_map = {
        "string": "String",
        "integer": "int",
        "number": "double",
        "boolean": "boolean"
    }

    fields = []
    assignments = []
    getters_setters = []
    constructor_args = []
    
    for prop_name, prop_def in properties.items():
        java_type = type_map.get(prop_def.get("type", "string"), "Object")
        
        # Field
        fields.append(f"    private {java_type} {prop_name};")
        
        # Constructor args
        constructor_args.append(f"{java_type} {prop_name}")
        assignments.append(f"        this.{prop_name} = {prop_name};")
        
        # Getter & Setter
        capitalized_name = prop_name[0].upper() + prop_name[1:]
        getters_setters.append(f"    public {java_type} get{capitalized_name}() {{ return {prop_name}; }}")
        getters_setters.append(f"    public void set{capitalized_name}({java_type} {prop_name}) {{ this.{prop_name} = {prop_name}; }}")

    # Assemble Java Class
    java_class = f"""public class {title} {{
{chr(10).join(fields)}

    public {title}() {{
    }}

    public {title}({", ".join(constructor_args)}) {{
{chr(10).join(assignments)}
    }}

{chr(10).join(getters_setters)}
}}
"""
    
    out_file = out_dir / f"{title}.java"
    with open(out_file, "w") as f:
        f.write(java_class)
        
    console.print(f"[bold green]✓[/bold green] Java POJO generated at {out_file}")

@app.command("generate")
def generate(
    schema: str = typer.Argument(..., help="Path to the JSON schema file"),
    lang: str = typer.Option(..., "--lang", "-l", help="Language to generate (python or java)"),
    out: str = typer.Option(".", "--out", "-o", help="Output directory")
):
    """Generate domain object code from a JSON schema."""
    schema_path = Path(schema)
    if not schema_path.exists():
        console.print(f"[bold red]Error:[/bold red] Schema file '{schema}' not found.")
        raise typer.Exit(1)
        
    out_dir = Path(out)
    out_dir.mkdir(parents=True, exist_ok=True)
    
    if lang.lower() == "python":
        generate_python(schema_path, out_dir)
    elif lang.lower() == "java":
        generate_java(schema_path, out_dir)
    else:
        console.print(f"[bold red]Error:[/bold red] Unsupported language '{lang}'. Choose 'python' or 'java'.")
        raise typer.Exit(1)
