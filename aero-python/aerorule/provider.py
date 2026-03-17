import os
import json
import yaml
import logging
from abc import ABC, abstractmethod
from typing import List, Optional
from .models import Rule

logger = logging.getLogger(__name__)

class RuleProvider(ABC):
    """
    Abstract base class for rule providers.
    """
    @abstractmethod
    def load_rules(self) -> List[Rule]:
        """Retrieves all available rules."""
        pass

    @abstractmethod
    def get_rule(self, rule_id: str) -> Optional[Rule]:
        """Retrieves a specific rule by ID."""
        pass

    @abstractmethod
    def refresh(self):
        """Signals the provider to reload rules from its source."""
        pass

class FileSystemProvider(RuleProvider):
    """
    A rule provider that loads rules from a local filesystem directory.
    Supports parsing both JSON and YAML rule files.
    """
    def __init__(self, directory_path: str):
        self.directory_path = directory_path
        self._cache = {}
        self._initialized = False

    def load_rules(self) -> List[Rule]:
        if not self._initialized:
            self.refresh()
        return list(self._cache.values())

    def get_rule(self, rule_id: str) -> Optional[Rule]:
        if not self._initialized:
            self.refresh()
        return self._cache.get(rule_id)

    def refresh(self):
        logger.info(f"Refreshing rules from directory: {self.directory_path}")
        new_cache = {}
        if not os.path.isdir(self.directory_path):
            self._cache = new_cache
            self._initialized = True
            return

        for filename in os.listdir(self.directory_path):
            file_path = os.path.join(self.directory_path, filename)
            
            data = None
            if filename.endswith(".json"):
                try:
                    with open(file_path, 'r') as f:
                        data = json.load(f)
                except Exception as e:
                    logger.error(f"Failed to load rule from {filename}: {e}")
            elif filename.endswith(".yaml") or filename.endswith(".yml"):
                try:
                    with open(file_path, 'r') as f:
                        data = yaml.safe_load(f)
                except Exception as e:
                    logger.error(f"Failed to load rule from {filename}: {e}")
            
            if data:
                try:
                    if isinstance(data, list):
                        for d in data:
                            r = Rule(**d)
                            new_cache[r.id] = r
                    else:
                        r = Rule(**data)
                        new_cache[r.id] = r
                except Exception as e:
                    logger.error(f"Validation failed for rule in {filename}: {e}")

        self._cache = new_cache
        self._initialized = True
        logger.info(f"Loaded {len(self._cache)} rules from filesystem")

class DatabaseProvider(RuleProvider):
    """
    A rule provider that loads rules from a relational database using SQLAlchemy.
    """
    def __init__(self, connection_url: str):
        """
        :param connection_url: SQLAlchemy connection URL (e.g. 'sqlite:///rules.db')
        """
        from sqlalchemy import create_engine, text
        self.engine = create_engine(connection_url)
        self.text = text
        self._cache = {}
        self._initialized = False

    def load_rules(self) -> List[Rule]:
        if not self._initialized:
            self.refresh()
        return list(self._cache.values())

    def get_rule(self, rule_id: str) -> Optional[Rule]:
        if not self._initialized:
            self.refresh()
        return self._cache.get(rule_id)

    def refresh(self):
        logger.info("Refreshing rules from database...")
        new_cache = {}
        query = self.text("""
            SELECT id, name, version, description, priority, condition, source_quote, source_document,
                   on_success_action, on_success_metadata, on_failure_action, on_failure_metadata 
            FROM aero_rules
        """)
        
        try:
            with self.engine.connect() as conn:
                result = conn.execute(query)
                for row in result:
                    # row is a Row object, can be accessed by index or mapping
                    row_dict = row._asdict()
                    
                    # Construct success/failure actions if they exist
                    rule_data = {
                        "id": row_dict["id"],
                        "name": row_dict["name"],
                        "version": row_dict["version"],
                        "description": row_dict["description"],
                        "priority": row_dict["priority"],
                        "condition": row_dict["condition"],
                        "sourceQuote": row_dict["source_quote"],
                        "sourceDocument": row_dict["source_document"]
                    }
                    
                    if row_dict["on_success_action"]:
                        rule_data["onSuccess"] = {
                            "action": row_dict["on_success_action"],
                            "metadata": self._parse_json(row_dict["on_success_metadata"])
                        }
                    
                    if row_dict["on_failure_action"]:
                        rule_data["onFailure"] = {
                            "action": row_dict["on_failure_action"],
                            "metadata": self._parse_json(row_dict["on_failure_metadata"])
                        }
                    
                    r = Rule(**rule_data)
                    new_cache[r.id] = r
            
            self._cache = new_cache
            self._initialized = True
            logger.info(f"Loaded {len(self._cache)} rules from database")
            
        except Exception as e:
            logger.error(f"Failed to load rules from database: {e}")
            raise

    def _parse_json(self, value):
        if not value:
            return None
        if isinstance(value, dict):
            return value
        try:
            return json.loads(value)
        except Exception:
            return None
