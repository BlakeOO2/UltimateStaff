package com.smokypeaks.server.automod;

import java.util.EnumMap;

/**
 * Tracks violation history for a player
 */
public class PlayerViolationRecord {
    private final EnumMap<ViolationType, Integer> violations;

    /**
     * Create a new violation record
     */
    public PlayerViolationRecord() {
        this.violations = new EnumMap<>(ViolationType.class);
    }

    /**
     * Add a violation of the specified type
     * @param type The type of violation
     */
    public void addViolation(ViolationType type) {
        violations.put(type, getViolationCount(type) + 1);
    }

    /**
     * Get the number of violations of a specific type
     * @param type The type of violation
     * @return The number of violations
     */
    public int getViolationCount(ViolationType type) {
        return violations.getOrDefault(type, 0);
    }

    /**
     * Clear all violations
     */
    public void clearViolations() {
        violations.clear();
    }

    /**
     * Clear violations of a specific type
     * @param type The type of violation to clear
     */
    public void clearViolations(ViolationType type) {
        violations.remove(type);
    }
}
