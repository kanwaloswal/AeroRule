package com.aerorule.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSystemProvider {
    private final File directory;
    private final ObjectMapper objectMapper;

    public FileSystemProvider(String directoryPath) {
        this.directory = new File(directoryPath);
        this.objectMapper = new ObjectMapper();
    }

    public List<Rule> loadRules() {
        List<Rule> rules = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return rules;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    Rule rule = objectMapper.readValue(file, Rule.class);
                    rules.add(rule);
                } catch (IOException e) {
                    System.err.println("Failed to load rule from " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        return rules;
    }
}
