import pytest
import json
import os
from sqlalchemy import create_engine, text
from aerorule.provider import DatabaseProvider
from aerorule.models import Rule

@pytest.fixture
def db_url():
    db_file = "test_pytest_rules.db"
    if os.path.exists(db_file):
        os.remove(db_file)
    return f"sqlite:///{db_file}"

@pytest.fixture
def provider(db_url):
    engine = create_engine(db_url)
    with engine.connect() as conn:
        conn.execute(text("""
            CREATE TABLE aero_rules (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255),
                version VARCHAR(50),
                description TEXT,
                priority INT,
                condition TEXT NOT NULL,
                source_quote TEXT,
                source_document VARCHAR(255),
                on_success_action VARCHAR(255),
                on_success_metadata TEXT,
                on_failure_action VARCHAR(255),
                on_failure_metadata TEXT
            )
        """))
        conn.execute(text("""
            INSERT INTO aero_rules (id, name, version, condition, on_success_action, on_success_metadata)
            VALUES ('RULE-001', 'Test Rule', 'v1', 'input.value > 10', 'NOTIFY', '{"channel": "email"}')
        """))
        conn.commit()
    return DatabaseProvider(db_url)

def test_load_rules(provider):
    rules = provider.load_rules()
    assert len(rules) == 1
    rule = rules[0]
    assert rule.id == "RULE-001"
    assert rule.version == "v1"
    assert rule.condition == "input.value > 10"
    assert rule.onSuccess.action == "NOTIFY"
    assert rule.onSuccess.metadata["channel"] == "email"

def test_get_rule(provider):
    rule = provider.get_rule("RULE-001")
    assert rule is not None
    assert rule.name == "Test Rule"

def test_refresh(provider, db_url):
    assert len(provider.load_rules()) == 1
    
    engine = create_engine(db_url)
    with engine.connect() as conn:
        conn.execute(text("""
            INSERT INTO aero_rules (id, name, condition, on_success_action)
            VALUES ('RULE-002', 'Second Rule', 'true', 'APPROVE')
        """))
        conn.commit()
    
    # Still cached
    assert len(provider.load_rules()) == 1
    
    provider.refresh()
    assert len(provider.load_rules()) == 2
    assert provider.get_rule("RULE-002") is not None
