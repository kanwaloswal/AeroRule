package com.aerorule.core.provider;

import com.aerorule.core.Rule;
import java.util.List;

public interface RuleProvider {
    /**
     * Retrieves all available rules from the provider.
     * @return a list of rules
     */
    List<Rule> getRules();

    /**
     * Retrieves a specific rule by its ID.
     * @param id the rule identifier
     * @return the rule, or null if not found
     */
    Rule getRule(String id);

    /**
     * Signals the provider to reload rules from its source.
     * This will clear any internal caches.
     */
    void refresh();
}
