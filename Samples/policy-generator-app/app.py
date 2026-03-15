import streamlit as st
import json
import os
try:
    from pypdf import PdfReader
except ImportError:
    PdfReader = None
import openai
from dotenv import load_dotenv

load_dotenv()

st.set_page_config(page_title="Policy to AeroRule Generator", layout="wide", page_icon="✈️")

# --- CUSTOM CSS ---
st.markdown("""
<style>
/* Modern styling */
.stApp {
    background-color: #0E1117;
    color: #FAFAFA;
}
.header-container {
    padding: 2rem 0;
    text-align: center;
    border-bottom: 1px solid #333;
    margin-bottom: 2rem;
}
.header-title {
    font-size: 3rem;
    font-weight: 800;
    background: -webkit-linear-gradient(#f0b3ff, #8A2BE2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}
.stButton>button {
    background-color: #8A2BE2;
    color: white;
    font-weight: bold;
    border-radius: 8px;
    border: none;
    transition: all 0.3s ease;
}
.stButton>button:hover {
    background-color: #9b4bed;
    box-shadow: 0 4px 12px rgba(138,43,226,0.4);
}
</style>
""", unsafe_allow_html=True)

st.markdown("""
<div class="header-container">
    <div class="header-title">AeroRule Policy Generator </div> 
    <p>AI-powered business rule extraction from natural language policies to executable Aero Rules. 🛩️</p>
</div>
""", unsafe_allow_html=True)

st.sidebar.title("⚙️ LLM Configuration")
api_key = st.sidebar.text_input("LLM API Key (or empty for local)", value=os.environ.get("LLM_API_KEY", ""), type="password")
base_url = st.sidebar.text_input("Base URL (e.g., http://localhost:11434/v1 for Ollama)", value=os.environ.get("LLM_BASE_URL", "https://api.openai.com/v1"))
model_name = st.sidebar.text_input("Model Name", value=os.environ.get("LLM_MODEL", "gpt-4o"))

def extract_text(uploaded_file):
    if uploaded_file.name.endswith('.pdf'):
        if PdfReader is None:
            return "Please install pypdf to read PDF files."
        pdf = PdfReader(uploaded_file)
        text = ""
        for page in pdf.pages:
            text += page.extract_text() + "\n"
        return text
    else:
        return uploaded_file.getvalue().decode('utf-8')

col1, col2 = st.columns([1, 1.2])

with col1:
    st.subheader("📄 1. Knowledge Base")
    st.markdown("Upload your lending policy (PDF/TXT) and your Domain Object structures (Java POJOs or JSON Config).")
    
    policy_files = st.file_uploader("Upload Policy Documents", accept_multiple_files=True, type=['pdf', 'txt', 'md'])
    pojo_files = st.file_uploader(
        "Upload Domain Objects",
        accept_multiple_files=True, 
        type=['java', 'json'],
        help="Upload standard Java POJOs or JSON Schema documents defining your objects. Example JSON Schema: `{\"$id\": \"...\", \"title\": \"Customer\", \"type\": \"object\", \"properties\": {\"riskScore\": {\"type\": \"integer\"}}}`"
    )
    
    st.subheader("🧠 2. Generate Rules")
    generate_btn = st.button("Generate AeroRule JSON", use_container_width=True)

with col2:
    st.subheader("🚀 3. Generated Rule Artifacts")
    
    if generate_btn:
        if not policy_files or not pojo_files:
            st.warning("Please upload at least one policy document and one domain object.")
        else:
            with st.spinner("Analyzing policies and code to generate rules..."):
                try:
                    # Client setup
                    client = openai.OpenAI(
                        api_key=api_key or "sk-local",
                        base_url=base_url if base_url else None
                    )
                    
                    # Read inputs
                    policy_text = "\n\n".join([f"--- {f.name} ---\n{extract_text(f)}" for f in policy_files])
                    pojo_text = "\n\n".join([f"--- {f.name} ---\n{extract_text(f)}" for f in pojo_files])
                    
                    # Provide an abbreviated schema for prompt effectiveness
                    schema_prompt = '''
{
  "rules": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "priority": "integer",
      "condition": "string matching CEL syntax. Use only fields in the provided domain objects.",
      "onFailure": {
        "action": "string (e.g., DECLINE, FLAG_FOR_REVIEW)",
        "reason": "string explaining why"
      }
    }
  ]
}'''
                    
                    system_prompt = "You are an expert Rule Engineer. Extract business constraints from natural language and output valid AeroRule CEL rules as JSON."
                    
                    user_prompt = f"""
## Target Schema JSON Format (Array of rules):
```json
{schema_prompt}
```

## Available Domain Objects (Java / JSON Schema):
```
{pojo_text}
```

## Business Policy to Enforce:
{policy_text}

Task: Output ONLY a JSON object containing a `rules` array based on the provided schema. The `condition` must be valid CEL (Common Expression Language). DO NOT invent properties. Use variables named exactly according to standard lowerCamelCase of the root generic types (e.g., `customer` for Customer Schema/Class). Ensure conditions are executable expressions.
"""
                    
                    response = client.chat.completions.create(
                        model=model_name,
                        messages=[
                            {"role": "system", "content": system_prompt},
                            {"role": "user", "content": user_prompt}
                        ],
                        response_format={"type": "json_object"} if model_name.startswith("gpt-") else None,
                        temperature=0.1
                    )
                    
                    raw_content = response.choices[0].message.content
                    
                    # Basic extraction if enclosed in markdown blocks
                    if "```json" in raw_content:
                        raw_content = raw_content.split("```json")[-1].split("```")[0].strip()
                        
                    parsed_json = json.loads(raw_content)
                    rules_array = parsed_json.get("rules", parsed_json)
                        
                    st.success("Successfully generated executable rules!")
                    st.json(rules_array)
                    
                    st.download_button(
                        label="Download rules.json",
                        data=json.dumps(rules_array, indent=2),
                        file_name="generated-rules.json",
                        mime="application/json"
                    )
                except Exception as e:
                    st.error(f"Error during generation: {str(e)}")
