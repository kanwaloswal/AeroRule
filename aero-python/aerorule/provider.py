import os
import json
from typing import List
from .models import Rule

class FileSystemProvider:
    def __init__(self, directory_path: str):
        self.directory_path = directory_path

    def load_rules(self) -> List[Rule]:
        rules = []
        if not os.path.isdir(self.directory_path):
            return rules

        for filename in os.listdir(self.directory_path):
            if filename.endswith(".json"):
                file_path = os.path.join(self.directory_path, filename)
                try:
                    with open(file_path, 'r') as f:
                        data = json.load(f)
                        rules.append(Rule(**data))
                except Exception as e:
                    print(f"Failed to load rule from {filename}: {e}")
        return rules
