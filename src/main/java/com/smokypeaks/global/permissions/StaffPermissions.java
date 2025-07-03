// com.smokypeaks.global.permissions.StaffPermissions.java
package com.smokypeaks.global.permissions;

public class StaffPermissions {
    private static final String BASE = "ultimatestaff.";

    public static class Admin {
        private static final String ADMIN_BASE = BASE + "admin.";
        public static final String UPDATE_NOTIFY = ADMIN_BASE + "updates";
        public static final String CHAT = ADMIN_BASE + "chat";
        public static final String RELOAD = ADMIN_BASE + "reload";
        public static final String RESTART = ADMIN_BASE + "restart";
        public static final String INVENTORY_MODIFY = ADMIN_BASE + "inventory.modify";
        public static final String ALERTS_CONFIG = ADMIN_BASE + "alerts.config";
        public static final String DEBUG = ADMIN_BASE + "debug";
        public static final String DISCORD = ADMIN_BASE + "discord";
        public static final String OVERRIDE = ADMIN_BASE + "override";
        public static final String RESTORE_ITEMS = ADMIN_BASE + "restoreitems";
        public static final String CHAT_FILTER = ADMIN_BASE + "chatfilter";
        public static final String AUTOMOD = ADMIN_BASE + "automod";
        public static final String LAG_DIAGNOSTIC = ADMIN_BASE + "lag";
        public static final String AUTOMOD_DENY = ADMIN_BASE + "automoddeny";
    }

    public static class Staff {
        private static final String STAFF_BASE = BASE + "staff.";
        public static final String CHAT = STAFF_BASE + "chat";
        public static final String MODE = STAFF_BASE + "mode";
        public static final String VANISH = STAFF_BASE + "vanish";
        public static final String SPECTATOR = STAFF_BASE + "spectator";
        public static final String FREEZE = STAFF_BASE + "freeze";
        public static final String INVSEE = STAFF_BASE + "invsee";
        public static final String ENDERSEE = STAFF_BASE + "endersee";
        public static final String TELEPORT = STAFF_BASE + "teleport";
        public static final String INVENTORY_VIEW = STAFF_BASE + "inventory.view";
        public static final String MINING_ALERTS = STAFF_BASE + "alerts.mining";
        public static final String INVSEE_NOTIFY = STAFF_BASE + "invsee.notify";
        public static final String CLEAR_CHAT = STAFF_BASE + "clearchat";
        public static final String BYPASS_CLEAR_CHAT = STAFF_BASE + "clearchat.bypass";
        public static final String PUNISH = STAFF_BASE + "punish";
        public static final String PUNISH_IMMUNITY = STAFF_BASE + "punish.immunity";
        public static final String DEATH_LOCATION = STAFF_BASE + "deathlocation";
        public static final String BYPASS_FILTER = STAFF_BASE + "filter.bypass";
        public static final String FILTER_ALERTS = STAFF_BASE + "filter.alerts";
    }

    public static class Alerts {
        private static final String ALERTS_BASE = BASE + "alerts.";
        public static final String MINING = ALERTS_BASE + "mining";
        public static final String XRAY = ALERTS_BASE + "xray";
        public static final String XRAY_COMMANDS = "ultimatestaff.staff.xray_commands";
        public static final String XRAY_LEARNING = ALERTS_BASE + "xray.learning";
        public static final String AUTOMOD_APPROVE = BASE + ".automod.approve";
        public static final String AUTOMOD_DENY = BASE + ".automod.deny";
    }
}
