import streamlit as st
import json
import os
import logging
try:
    from pypdf import PdfReader
except ImportError:
    PdfReader = None
import openai
from dotenv import load_dotenv

# --- Setup Logging ---
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("app_debug.log")
    ]
)
logger = logging.getLogger(__name__)

load_dotenv()
logger.info("AeroRule Policy Generator application started.")

st.set_page_config(page_title="Policy to AeroRule Generator", layout="wide", page_icon="✈️")

# --- CUSTOM CSS ---
st.markdown("""
<style>
/* Modern styling */
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
    <p>AI-powered business rule extraction from natural language policies to executable Aero Rules. You can then feed this into AeroRule engine to enforce your business rules 🛩️</p>
    <small><i>Note: This is a sample application and NOT meant for production use.</i></small>
</div>
""", unsafe_allow_html=True)

st.sidebar.title("⚙️ LLM Configuration")
api_key = st.sidebar.text_input("LLM API Key (or empty for local)", value=os.environ.get("LLM_API_KEY", ""), type="password")
base_url = st.sidebar.text_input("Base URL (e.g., Ollama or OpenAI)", value="", placeholder="https://api.openai.com/v1")
model_name = st.sidebar.text_input("Model Name", value="", placeholder="gpt-4o")

def extract_text(uploaded_file):
    logger.info(f"Extracting text from: {uploaded_file.name}")
    if uploaded_file.name.endswith('.pdf'):
        if PdfReader is None:
            logger.error("pypdf is not installed. PDF extraction failed.")
            return "Please install pypdf to read PDF files."
        pdf = PdfReader(uploaded_file)
        text = ""
        for page in pdf.pages:
            text += page.extract_text() + "\n"
        logger.info(f"Successfully extracted {len(text)} characters from PDF.")
        return text
    else:
        text = uploaded_file.getvalue().decode('utf-8')
        logger.info(f"Successfully extracted {len(text)} characters from text file.")
        return text

# --- Session State ---
if 'policy_text' not in st.session_state:
    st.session_state.policy_text = ""
if 'pojo_text' not in st.session_state:
    st.session_state.pojo_text = ""
if 'generated_rules' not in st.session_state:
    st.session_state.generated_rules = []
if 'selected_rule_idx' not in st.session_state:
    st.session_state.selected_rule_idx = None

# --- Top Section: Uploads ---
with st.container():
    st.subheader("1. Knowledge Base")
    st.markdown("Upload your policy document and Domain Objects (Java / JSON Config).")
    
    col_upload1, col_upload2 = st.columns(2)
    with col_upload1:
        policy_files = st.file_uploader("Upload Policy Documents", accept_multiple_files=True, type=['pdf', 'txt', 'md'])
    with col_upload2:
        pojo_files = st.file_uploader(
            "Upload Domain Objects",
            accept_multiple_files=True, 
            type=['java', 'json'],
            help="Upload standard Java POJOs or JSON Schema documents defining your objects."
        )

    generate_btn = st.button("Generate AeroRule JSON", type="primary", use_container_width=True)

# --- Generation Logic ---
if generate_btn:
    if not policy_files or not pojo_files:
        st.warning("Please upload at least one policy document and one domain object.")
    else:
        with st.spinner("Analyzing policies and code to generate rules..."):
            try:
                # Client setup with fallbacks if empty
                llm_base_url = base_url if base_url else "https://api.openai.com/v1"
                llm_model = model_name if model_name else "gpt-4o"
                
                client = openai.OpenAI(
                    api_key=api_key or "sk-local",
                    base_url=llm_base_url
                )
                
                # Store text in session state
                st.session_state.policy_text = "\n\n".join([f"--- {f.name} ---\n{extract_text(f)}" for f in policy_files])
                st.session_state.pojo_text = "\n\n".join([f"--- {f.name} ---\n{extract_text(f)}" for f in pojo_files])
                
                logger.info("Building LLM prompt with extracted knowledge...")
                schema_prompt = '''
{
  "rules": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "priority": "integer",
      "condition": "string matching CEL syntax. Use only fields in the provided domain objects.",
      "sourceQuote": "Exact sentence or paragraph extracted verbatim from the policy document that inspired this rule.",
      "sourceDocument": "Name of the policy document (e.g., 'CreditPolicy.pdf v1.0').",
      "onSuccess": {
        "action": "string (e.g., APPROVE, ALLOW)",
        "metadata": {
          "message": "string explaining the success outcome"
        }
      },
      "onFailure": {
        "action": "string (e.g., DECLINE, FLAG_FOR_REVIEW)",
        "metadata": {
          "reason": "string explaining exactly why the rule failed"
        }
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
{st.session_state.pojo_text}
```

## Business Policy to Enforce:
{st.session_state.policy_text}

Task: Output ONLY a JSON object containing a `rules` array based on the provided schema. The `condition` must be valid CEL (Common Expression Language). DO NOT invent properties. Use variables named exactly according to standard lowerCamelCase of the root generic types (e.g., `customer` for Customer Schema/Class). Ensure conditions are executable expressions.
Crucially, populate `sourceQuote` with the EXACT text from the Business Policy that justifies this rule, and `sourceDocument` with the name of the document.
"""
                
                logger.info(f"Sending request to LLM (Model: {llm_model})...")
                response = client.chat.completions.create(
                    model=llm_model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    response_format={"type": "json_object"} if llm_model.startswith("gpt-") else None,
                    temperature=0.1
                )
                
                raw_content = response.choices[0].message.content
                logger.info("Received raw response from LLM.")
                
                if "```json" in raw_content:
                    raw_content = raw_content.split("```json")[-1].split("```")[0].strip()
                    
                parsed_json = json.loads(raw_content)
                st.session_state.generated_rules = parsed_json.get("rules", parsed_json)
                st.session_state.selected_rule_idx = None # Reset selection
                logger.info(f"Successfully parsed {len(st.session_state.generated_rules)} rules.")
                st.success("Successfully generated executable rules! Check the panes below.")
                
            except Exception as e:
                logger.error(f"Generation error: {str(e)}", exc_info=True)
                st.error(f"Error during generation: {str(e)}")

st.divider()

# --- Bottom Section: Two-Pane View ---
if st.session_state.policy_text and st.session_state.generated_rules:
    col_left, col_right = st.columns([1, 1])
    
    with col_right:
        st.subheader("🚀 3. Extracted Rules (Interactive)")
        st.write("Click a rule to highlight its source in the policy. You can download the rules in AeroRule JSON format for your rules engine.")
        
        # Interactive Rule Selector
        for idx, rule in enumerate(st.session_state.generated_rules):
            is_selected = st.session_state.selected_rule_idx == idx
            
            # Simple button mechanism. st.button returns True on the click event.
            if st.button(f"{'✅ ' if is_selected else '📄 '} {rule.get('name', 'Rule')} (Priority {rule.get('priority', 0)})", key=f"btn_{idx}", use_container_width=True):
                st.session_state.selected_rule_idx = idx
                st.rerun()
                
            # If selected, show details
            if is_selected:
                with st.expander("Rule Details", expanded=True):
                    st.json(rule)
                    st.markdown("**Source Quote:**")
                    st.info(f"_{rule.get('sourceQuote', 'Not found')}_")
                    if rule.get('sourceDocument'):
                        st.caption(f"Source: {rule.get('sourceDocument')}")
                    
                    # Individual Download Button
                    st.download_button(
                        label=f"⬇️ Download {rule.get('id', f'rule_{idx}')}.json",
                        data=json.dumps(rule, indent=2),
                        file_name=f"{rule.get('id', f'rule_{idx}')}.json",
                        mime="application/json",
                        use_container_width=True,
                        key=f"dl_btn_{idx}"
                    )
                    
        st.divider()
        st.download_button(
            label="⬇️ Download ALL rules.json",
            data=json.dumps(st.session_state.generated_rules, indent=2),
            file_name="generated-rules.json",
            mime="application/json",
            use_container_width=True,
            type="primary"
        )
    
    with col_left:
        st.subheader("📖 2. Policy Source Document")
        
        display_text = st.session_state.policy_text
        
        # Highlight logic
        if st.session_state.selected_rule_idx is not None:
            rule = st.session_state.generated_rules[st.session_state.selected_rule_idx]
            quote = rule.get("sourceQuote", "")
            
            # Simple substring replacement to inject <mark> tag
            if quote and quote in display_text:
                # Use a prominent yellow highlight
                highlighted_quote = f'<mark style="background-color: #ffd700; color: #000; padding: 2px 4px; border-radius: 4px;">{quote}</mark>'
                display_text = display_text.replace(quote, highlighted_quote)
            elif quote:
                st.warning(f"Could not find exact quote match to highlight: '{quote[:50]}...'")
        
        # Render the text with HTML enabled
        st.markdown(
            f"""
            <div style="padding: 1rem; border: 1px solid #ddd; border-radius: 8px; height: 600px; overflow-y: scroll; white-space: pre-wrap; font-family: monospace;">
                {display_text}
            </div>
            """, 
            unsafe_allow_html=True
        )
