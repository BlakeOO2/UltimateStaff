package com.smokypeaks.global.automod;

/**
 * Represents the different types of rule violations that can be detected
 */
public enum ViolationType {
    // Chat Behavior
    HARASSMENT_DISRESPECT,
    CHAT_ABUSE,
    THREATS,
    IMPERSONATION,

    // Gameplay Violations
    UNFAIR_ADVANTAGE,
    CHEATING,
    BUG_ABUSE,
    NON_CONSENSUAL_PVP,
    GRIEFING,
    BUILDING_TOO_CLOSE,
    LAG_INDUCING,

    // Economy & Trading
    SCAMMING,
    IRL_TRADING,
    THIRD_PARTY_TRADING,
    SHOP_INACTIVITY,

    // Inappropriate Content
    INAPPROPRIATE_APPEARANCE,
    INAPPROPRIATE_CONTENT
}
