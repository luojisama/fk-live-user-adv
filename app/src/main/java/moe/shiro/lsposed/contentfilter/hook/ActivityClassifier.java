package moe.shiro.lsposed.contentfilter.hook;

final class ActivityClassifier {
    private ActivityClassifier() {
    }

    static boolean isContentActivity(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        String lower = className.toLowerCase();
        if (lower.contains("setting")
                || lower.contains("preference")
                || lower.contains("permission")
                || lower.contains("account")
                || lower.contains("feedback")
                || lower.contains("message")
                || lower.contains("notice")
                || lower.contains("notification")
                || lower.contains("conversation")
                || lower.contains("privateletter")
                || lower.contains("chat")
                || lower.contains("contact")
                || lower.contains("friend")
                || lower.contains("relation")) {
            return false;
        }
        return lower.contains("mainactivity")
                || lower.contains("feed")
                || lower.contains("detail")
                || lower.contains("video")
                || lower.contains("pegasus")
                || lower.contains("live");
    }

    static boolean isTargetSettingsActivity(String className) {
        return "com.ss.android.ugc.aweme.setting.ui.DouYinSettingNewVersionActivity".equals(className)
                || "com.bilibili.app.preferences.BiliPreferencesActivity".equals(className);
    }
}
