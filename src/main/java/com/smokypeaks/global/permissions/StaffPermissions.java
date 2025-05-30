// com.smokypeaks.global.permissions.StaffPermissions.java
package com.smokypeaks.global.permissions;

public class StaffPermissions {
    private static final String BASE = "ultimatestaff.";

    public static class Admin {
        private static final String ADMIN_BASE = BASE + "admin.";
        public static final String UPDATE_NOTIFY = ADMIN_BASE + "updates";
        public static final String CHAT = ADMIN_BASE + "chat";
        public static final String RELOAD = ADMIN_BASE + "reload";
        public static final String RESTART = ADMIN_BASE + "restart"; // Moved here
    }

    public static class Staff {
        private static final String STAFF_BASE = BASE + "staff.";
        public static final String CHAT = STAFF_BASE + "chat";
        public static final String MODE = STAFF_BASE + "mode";
        public static final String VANISH = STAFF_BASE + "vanish";
        public static final String FREEZE = STAFF_BASE + "freeze";
        public static final String INVSEE = STAFF_BASE + "invsee";
        public static final String ENDERSEE = STAFF_BASE + "endersee";
        public static final String TELEPORT = STAFF_BASE + "teleport";
    }

    public static class Alerts {
        private static final String ALERTS_BASE = BASE + "alerts.";
        public static final String MINING = ALERTS_BASE + "mining";
    }
}
