package com.smokypeaks.server.automod;

/**
 * Represents a rule in the AutoMod system
 */
public class AutoModRule {
    private final ViolationType violationType;
    private final String description;
    private final PunishmentType punishmentType;
    private final int duration; // in minutes, 0 = permanent

    /**
     * Create a new AutoMod rule
     * @param violationType The type of violation
     * @param description A human-readable description of the violation
     * @param punishmentType The type of punishment to apply
     * @param duration The duration of the punishment in minutes (0 for permanent)
     */
    public AutoModRule(ViolationType violationType, String description, PunishmentType punishmentType, int duration) {
        this.violationType = violationType;
        this.description = description;
        this.punishmentType = punishmentType;
        this.duration = duration;
    }

    /**
     * Get the type of violation this rule applies to
     * @return The violation type
     */
    public ViolationType getViolationType() {
        return violationType;
    }

    /**
     * Get the human-readable description of the violation
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the type of punishment to apply
     * @return The punishment type
     */
    public PunishmentType getPunishmentType() {
        return punishmentType;
    }

    /**
     * Get the duration of the punishment in minutes
     * @return The duration in minutes (0 for permanent)
     */
    public int getDuration() {
        return duration;
    }
}
