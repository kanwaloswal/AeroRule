import os
import json
import yaml
import logging
from typing import List
from .models import Rule

logger = logging.getLogger(__name__)

class FileSystemProvider:
    """
    A rule provider that loads rules from a local filesystem directory.
    Supports parsing both JSON and YAML rule files.
    """
    def __init__(self, directory_path: str):
        self.directory_path = directory_path

    def load_rules(self) -> List[Rule]:
        rules = []
        if not os.path.isdir(self.directory_path):
            return rules

        for filename in os.listdir(self.directory_path):
            file_path = os.path.join(self.directory_path, filename)
            
            if filename.endswith(".json"):
                try:
                    with open(file_path, 'r') as f:
                        data = json.load(f)
                        if isinstance(data, list):
                            rules.extend([Rule(**d) for d in data])
                        else:
                            rules.append(Rule(**data))
                except Exception as e:
                    logger.error(f"Failed to load rule from {filename}: {e}")
            elif filename.endswith(".yaml") or filename.endswith(".yml"):
                try:
                    with open(file_path, 'r') as f:
                        data = yaml.safe_load(f)
                        if isinstance(data, list):
                            rules.extend([Rule(**d) for d in data])
                        else:
                            rules.append(Rule(**data))
                except Exception as e:
                    logger.error(f"Failed to load rule from {filename}: {e}")
        return rules
