package com.aerorule.core.engine;

/**
 * Defines how a {@link RuleSetEngine} evaluates the rules in a {@link RuleSet}.
 *
 * <ul>
 *   <li>{@code ALL} – Evaluate every rule and collect all traces (audit / compliance mode).</li>
 *   <li>{@code GATED} – Evaluate in order and stop on the first failure (sequential approval gates).</li>
 * </ul>
 */
public enum ExecutionStrategy {
    ALL,
    GATED
}
