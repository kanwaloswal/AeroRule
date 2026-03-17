package com.aerorule.core.provider;

import com.aerorule.core.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemProvider implements RuleProvider, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemProvider.class);
    private final File directory;
    private final ObjectMapper objectMapper;
    private final Yaml yaml;
    private final Map<String, Rule> rulesCache;
    private final boolean watchEnabled;

    private WatchService watchService;
    private ExecutorService watchExecutor;

    public FileSystemProvider(String directoryPath) {
        this(directoryPath, false);
    }

    public FileSystemProvider(String directoryPath, boolean watchEnabled) {
        this.directory = new File(directoryPath);
        this.objectMapper = new ObjectMapper();
        
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new Constructor(Rule.class, options));
        
        this.rulesCache = new ConcurrentHashMap<>();
        this.watchEnabled = watchEnabled;

        loadAllRules();

        if (this.watchEnabled) {
            startWatching();
        }
    }

    @Override
    public List<Rule> getRules() {
        return new ArrayList<>(rulesCache.values());
    }

    @Override
    public Rule getRule(String id) {
        return rulesCache.get(id);
    }

    @Override
    public void refresh() {
        loadAllRules();
    }

    private void loadAllRules() {
        rulesCache.clear();
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles((dir, name) -> 
            name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml"));
            
        if (files != null) {
            for (File file : files) {
                loadFile(file);
            }
        }
    }

    private void loadFile(File file) {
        try {
            if (file.getName().endsWith(".json")) {
                Rule[] docRules;
                try {
                    // Try parsing as array of rules first
                    docRules = objectMapper.readValue(file, Rule[].class);
                } catch (Exception e) {
                    // Fall back to single rule
                    docRules = new Rule[] { objectMapper.readValue(file, Rule.class) };
                }
                for (Rule rule : docRules) {
                    if (rule != null && rule.getId() != null) {
                        rulesCache.put(rule.getId(), rule);
                    }
                }
            } else if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    Iterable<Object> loaded = yaml.loadAll(inputStream);
                    for (Object obj : loaded) {
                        if (obj instanceof Rule) {
                            Rule rule = (Rule) obj;
                            if (rule.getId() != null) {
                                rulesCache.put(rule.getId(), rule);
                            }
                        } else if (obj instanceof List) { // yaml list of rules
                            List<?> list = (List<?>) obj;
                            for(Object item : list) {
                                if (item instanceof Map) {
                                    Rule rule = objectMapper.convertValue(item, Rule.class);
                                    if(rule != null && rule.getId() != null) {
                                        rulesCache.put(rule.getId(), rule);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load rule from {}: {}", file.getName(), e.getMessage(), e);
        }
    }

    private void startWatching() {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path path = directory.toPath();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            
            this.watchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FileSystemProvider-Watcher");
                t.setDaemon(true);
                return t;
            });
            
            this.watchExecutor.submit(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            // simple approach: reload everything on any relevant change to handle deletes cleanly
                            Path changed = (Path) event.context();
                            String name = changed.toString();
                            if (name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml")) {
                                refresh();
                                break; // only refresh once per event batch
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ClosedWatchServiceException e) {
                    // Watcher closed, exit thread
                }
            });
        } catch (IOException e) {
            logger.error("Failed to initialize watch service: {}", e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }
        if (watchService != null) {
            watchService.close();
        }
    }
}
