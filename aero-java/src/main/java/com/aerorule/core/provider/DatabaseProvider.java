package com.aerorule.core.provider;

import com.aerorule.core.Action;
import com.aerorule.core.Rule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A RuleProvider that loads rules from a relational database.
 * <p>
 * Expects a table named 'aero_rules' with columns mapping to Rule properties.
 */
public class DatabaseProvider implements RuleProvider {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DataSource dataSource;
    private final Map<String, Rule> cache = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public DatabaseProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Rule> getRules() {
        if (!initialized) {
            refresh();
        }
        return new ArrayList<>(cache.values());
    }

    @Override
    public Rule getRule(String id) {
        if (!initialized) {
            refresh();
        }
        return cache.get(id);
    }

    @Override
    public void refresh() {
        logger.info("Refreshing rules from database...");
        String sql = "SELECT id, name, version, description, priority, condition, source_quote, source_document, " +
                     "on_success_action, on_success_metadata, on_failure_action, on_failure_metadata FROM aero_rules";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            Map<String, Rule> newCache = new HashMap<>();
            while (rs.next()) {
                Rule rule = mapRowToRule(rs);
                newCache.put(rule.getId(), rule);
            }

            cache.clear();
            cache.putAll(newCache);
            initialized = true;
            logger.info("Loaded {} rules from database", cache.size());

        } catch (SQLException e) {
            logger.error("Failed to load rules from database", e);
            throw new RuntimeException("Rules database load failure", e);
        }
    }

    private Rule mapRowToRule(ResultSet rs) throws SQLException {
        Rule rule = new Rule();
        rule.setId(rs.getString("id"));
        rule.setName(rs.getString("name"));
        rule.setVersion(rs.getString("version"));
        rule.setDescription(rs.getString("description"));
        rule.setPriority(rs.getObject("priority") != null ? rs.getInt("priority") : null);
        rule.setCondition(rs.getString("condition"));
        rule.setSourceQuote(rs.getString("source_quote"));
        rule.setSourceDocument(rs.getString("source_document"));

        String successActionName = rs.getString("on_success_action");
        if (successActionName != null) {
            Action success = new Action();
            success.setAction(successActionName);
            success.setMetadata(parseMetadata(rs.getString("on_success_metadata")));
            rule.setOnSuccess(success);
        }

        String failureActionName = rs.getString("on_failure_action");
        if (failureActionName != null) {
            Action failure = new Action();
            failure.setAction(failureActionName);
            failure.setMetadata(parseMetadata(rs.getString("on_failure_metadata")));
            rule.setOnFailure(failure);
        }

        return rule;
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse metadata JSON: {}", json, e);
            return null;
        }
    }
}
