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
                || lower.contains("relation")
                || lower.contains("shopping")
                || lower.contains("commerce")
                || lower.contains("ecommerce")
                || lower.contains("douyinmall")
                || lower.contains("ecmall")
                || lower.contains("shoppingcart")
                || lower.contains("shopdetail")
                || lower.contains("productdetail")
                || lower.contains("orderdetail")
                || lower.contains("sku_panel")
                || lower.contains("couponpanel")) {
            return false;
        }
        if ((lower.contains("liveroom")
                || lower.contains("live_room")
                || lower.contains("liveplay")
                || lower.contains("live_play")
                || lower.contains("broadcast")
                || lower.contains("chatroom"))
                && !lower.contains("feed")) {
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
