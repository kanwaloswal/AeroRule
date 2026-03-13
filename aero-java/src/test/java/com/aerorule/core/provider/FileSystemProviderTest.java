package com.aerorule.core.provider;

import com.aerorule.core.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemProviderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Load valid rule JSON and YAML files from directory")
    void loadValidRules(@TempDir Path tempDir) throws IOException {
        writeRuleJson(tempDir, "aml_rule.json", "AML-001", "Flag large transactions", "transaction_amount > 10000.0");
        writeRuleJson(tempDir, "kyc_rule.json", "KYC-001", "Verify KYC status", "kyc_verified == true");
        
        String yamlRule = "id: YML-001\ndescription: YAML test\ncondition: test == true\n";
        Files.writeString(tempDir.resolve("yaml_rule.yaml"), yamlRule);

        try (FileSystemProvider provider = new FileSystemProvider(tempDir.toString())) {
            List<Rule> rules = provider.getRules();
            assertEquals(3, rules.size());
            
            assertNotNull(provider.getRule("AML-001"));
            assertNotNull(provider.getRule("KYC-001"));
            assertNotNull(provider.getRule("YML-001"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Non-existent directory returns empty list")
    void nonExistentDirectory() throws Exception {
        try (FileSystemProvider provider = new FileSystemProvider("/tmp/non_existent_dir_xyz_" + System.nanoTime())) {
            List<Rule> rules = provider.getRules();
            assertNotNull(rules);
            assertTrue(rules.isEmpty());
        }
    }

    @Test
    @DisplayName("Directory with no valid files returns empty list")
    void noConfiguredFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("readme.txt"), "Not a rule file");
        Files.writeString(tempDir.resolve("bad_rule.json"), "{ invalid }");

        try (FileSystemProvider provider = new FileSystemProvider(tempDir.toString())) {
            assertTrue(provider.getRules().isEmpty());
        }
    }

    @Test
    @DisplayName("Refresh clears and reloads cache")
    void testRefresh(@TempDir Path tempDir) throws Exception {
        writeRuleJson(tempDir, "rule1.json", "R-1", "Test", "true");
        FileSystemProvider provider = new FileSystemProvider(tempDir.toString());
        assertEquals(1, provider.getRules().size());

        writeRuleJson(tempDir, "rule2.json", "R-2", "Test2", "false");
        
        // Cache hasn't been refreshed yet!
        assertEquals(1, provider.getRules().size());
        
        provider.refresh();
        assertEquals(2, provider.getRules().size());
        
        provider.close();
    }

    private void writeRuleJson(Path dir, String filename, String id,
                               String description, String condition) throws IOException {
        Map<String, Object> rule = Map.of(
                "id", id,
                "description", description,
                "condition", condition
        );
        Files.writeString(dir.resolve(filename), mapper.writeValueAsString(rule));
    }
}
