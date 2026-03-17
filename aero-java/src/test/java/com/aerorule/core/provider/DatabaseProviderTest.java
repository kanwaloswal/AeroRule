package com.aerorule.core.provider;

import com.aerorule.core.Rule;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseProviderTest {
    private JdbcDataSource dataSource;
    private DatabaseProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS aero_rules");
            st.execute("CREATE TABLE aero_rules (" +
                       "id VARCHAR(255) PRIMARY KEY, " +
                       "name VARCHAR(255), " +
                       "version VARCHAR(50), " +
                       "description TEXT, " +
                       "priority INT, " +
                       "condition TEXT NOT NULL, " +
                       "source_quote TEXT, " +
                       "source_document VARCHAR(255), " +
                       "on_success_action VARCHAR(255), " +
                       "on_success_metadata TEXT, " +
                       "on_failure_action VARCHAR(255), " +
                       "on_failure_metadata TEXT" +
                       ")");
            
            st.execute("INSERT INTO aero_rules (id, name, version, condition, on_success_action, on_success_metadata) " +
                       "VALUES ('RULE-001', 'Test Rule', 'v1', 'input.value > 10', 'NOTIFY', '{\"channel\": \"email\"}')");
        }

        provider = new DatabaseProvider(dataSource);
    }

    @Test
    void testGetRules() {
        List<Rule> rules = provider.getRules();
        assertEquals(1, rules.size());
        Rule rule = rules.get(0);
        assertEquals("RULE-001", rule.getId());
        assertEquals("input.value > 10", rule.getCondition());
        assertNotNull(rule.getOnSuccess());
        assertEquals("NOTIFY", rule.getOnSuccess().getAction());
        assertEquals("email", rule.getOnSuccess().getMetadata().get("channel"));
    }

    @Test
    void testGetRule() {
        Rule rule = provider.getRule("RULE-001");
        assertNotNull(rule);
        assertEquals("Test Rule", rule.getName());
        assertEquals("v1", rule.getVersion());
    }

    @Test
    void testRefresh() throws Exception {
        assertEquals(1, provider.getRules().size());

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("INSERT INTO aero_rules (id, name, condition, on_success_action) " +
                       "VALUES ('RULE-002', 'Second Rule', 'true', 'APPROVE')");
        }

        // Before refresh, should still have 1 (cached)
        assertEquals(1, provider.getRules().size());

        provider.refresh();

        // After refresh, should have 2
        assertEquals(2, provider.getRules().size());
        assertNotNull(provider.getRule("RULE-002"));
    }
}
