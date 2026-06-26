package moe.shiro.lsposed.contentfilter.config;

public final class RuleKeys {
    public static final String MODULE_PACKAGE = "moe.shiro.lsposed.contentfilter";
    public static final String CONFIG_ACTIVITY =
            "moe.shiro.lsposed.contentfilter.config.MainActivity";
    public static final String AUTHORITY = MODULE_PACKAGE + ".rules";
    public static final String RULES_URI = "content://" + AUTHORITY + "/rules";
    public static final String PREFS_NAME = "content_filter_rules";

    public static final String RULES_JSON = "rules_json";
    public static final String DEBUG_LOG = "debug_log";
    public static final String BLOCK_LIVE = "block_live";
    public static final String UPDATED_AT = "updated_at";

    public static final String APP_ENABLED_SUFFIX = ".enabled";
    public static final String USER_ENABLED_SUFFIX = ".user.enabled";
    public static final String TOPIC_ENABLED_SUFFIX = ".topic.enabled";
    public static final String SECTION_ENABLED_SUFFIX = ".section.enabled";
    public static final String LIVE_ENABLED_SUFFIX = ".live.enabled";
    public static final String AD_ENABLED_SUFFIX = ".ad.enabled";
    public static final String USER_KEYWORDS_SUFFIX = ".user.keywords";
    public static final String TOPIC_KEYWORDS_SUFFIX = ".topic.keywords";
    public static final String SECTION_KEYWORDS_SUFFIX = ".section.keywords";
    public static final String AD_KEYWORDS_SUFFIX = ".ad.keywords";

    public static final String[] TARGET_PACKAGES = {
            "com.ss.android.ugc.aweme",
            "tv.danmaku.bili",
            "com.bilibili.app.in"
    };

    public static final String[] TARGET_LABELS = {
            "抖音",
            "哔哩哔哩",
            "bilibili"
    };

    public static final String[] LIVE_KEYWORDS = {
            "直播",
            "正在直播",
            "直播间",
            "进入直播间",
            "看直播",
            "直播中",
            "主播",
            "开播",
            "连麦",
            "live now",
            "live",
            "living",
            "anchor",
            "room live"
    };

    public static final String[] AD_KEYWORDS = {
            "广告",
            "赞助",
            "推广",
            "推广中",
            "品牌推广",
            "商业推广",
            "立即购买",
            "去购买",
            "去看看",
            "查看详情",
            "了解详情",
            "点击下载",
            "立即下载",
            "下载应用",
            "打开应用",
            "advertise",
            "advertisement",
            "sponsor",
            "sponsored",
            "promotion"
    };

    public static final String[] DOUYIN_AD_KEYWORDS = {
            "抖音商城",
            "抖音电商",
            "商城",
            "购物",
            "商品",
            "好物",
            "小店",
            "店铺",
            "同款",
            "橱窗",
            "团购",
            "优惠券",
            "券后",
            "下单",
            "领券",
            "抢购",
            "商家",
            "企业号",
            "douyin mall",
            "ecommerce",
            "commerce"
    };

    public static final String[] BILI_AD_KEYWORDS = {
            "创作推广",
            "会员购",
            "游戏中心",
            "打开淘宝",
            "打开京东",
            "bilibili广告",
            "ad_av",
            "ad_web",
            "ad_inline",
            "adcard",
            "cm_mark",
            "creative_ad",
            "pegasus_ad"
    };

    private RuleKeys() {
    }

    public static String appEnabledKey(String packageName) {
        return packageName + APP_ENABLED_SUFFIX;
    }

    public static String userEnabledKey(String packageName) {
        return packageName + USER_ENABLED_SUFFIX;
    }

    public static String topicEnabledKey(String packageName) {
        return packageName + TOPIC_ENABLED_SUFFIX;
    }

    public static String sectionEnabledKey(String packageName) {
        return packageName + SECTION_ENABLED_SUFFIX;
    }

    public static String liveEnabledKey(String packageName) {
        return packageName + LIVE_ENABLED_SUFFIX;
    }

    public static String adEnabledKey(String packageName) {
        return packageName + AD_ENABLED_SUFFIX;
    }

    public static String userKeywordsKey(String packageName) {
        return packageName + USER_KEYWORDS_SUFFIX;
    }

    public static String topicKeywordsKey(String packageName) {
        return packageName + TOPIC_KEYWORDS_SUFFIX;
    }

    public static String sectionKeywordsKey(String packageName) {
        return packageName + SECTION_KEYWORDS_SUFFIX;
    }

    public static String adKeywordsKey(String packageName) {
        return packageName + AD_KEYWORDS_SUFFIX;
    }

    public static String[] defaultAdKeywordsForPackage(String packageName) {
        if ("com.ss.android.ugc.aweme".equals(packageName)) {
            return DOUYIN_AD_KEYWORDS;
        }
        if ("tv.danmaku.bili".equals(packageName) || "com.bilibili.app.in".equals(packageName)) {
            return BILI_AD_KEYWORDS;
        }
        return new String[0];
    }
}
