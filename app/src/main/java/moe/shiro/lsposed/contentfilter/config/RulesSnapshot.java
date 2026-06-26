package moe.shiro.lsposed.contentfilter.config;

import android.content.SharedPreferences;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RulesSnapshot {
    public final Map<String, AppRules> apps;
    public final boolean blockLive;
    public final boolean debugLog;
    public final long updatedAt;

    public RulesSnapshot(Map<String, AppRules> apps, boolean blockLive, boolean debugLog, long updatedAt) {
        this.apps = Collections.unmodifiableMap(new LinkedHashMap<>(apps));
        this.blockLive = blockLive;
        this.debugLog = debugLog;
        this.updatedAt = updatedAt;
    }

    public static RulesSnapshot empty() {
        LinkedHashMap<String, AppRules> apps = new LinkedHashMap<>();
        for (String packageName : RuleKeys.TARGET_PACKAGES) {
            apps.put(packageName, AppRules.defaults(packageName));
        }
        return new RulesSnapshot(apps, true, false, 0L);
    }

    public static RulesSnapshot fromPreferences(SharedPreferences prefs) {
        LinkedHashMap<String, AppRules> apps = new LinkedHashMap<>();
        boolean legacyBlockLive = prefs.getBoolean(RuleKeys.BLOCK_LIVE, true);
        for (String packageName : RuleKeys.TARGET_PACKAGES) {
            apps.put(packageName, AppRules.fromPreferences(prefs, packageName, legacyBlockLive));
        }
        return new RulesSnapshot(
                apps,
                legacyBlockLive,
                prefs.getBoolean(RuleKeys.DEBUG_LOG, false),
                prefs.getLong(RuleKeys.UPDATED_AT, 0L)
        );
    }

    public static RulesSnapshot fromCursor(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            return empty();
        }
        int index = cursor.getColumnIndex(RuleKeys.RULES_JSON);
        if (index < 0 || cursor.isNull(index)) {
            return empty();
        }
        return fromJson(cursor.getString(index));
    }

    public static RulesSnapshot fromJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return empty();
        }
        try {
            JSONObject root = new JSONObject(raw);
            LinkedHashMap<String, AppRules> apps = new LinkedHashMap<>();
            JSONObject jsonApps = root.optJSONObject("apps");
            boolean legacyBlockLive = root.optBoolean(RuleKeys.BLOCK_LIVE, true);
            for (String packageName : RuleKeys.TARGET_PACKAGES) {
                JSONObject app = jsonApps == null ? null : jsonApps.optJSONObject(packageName);
                apps.put(packageName, AppRules.fromJson(packageName, app, legacyBlockLive));
            }
            return new RulesSnapshot(
                    apps,
                    legacyBlockLive,
                    root.optBoolean(RuleKeys.DEBUG_LOG, false),
                    root.optLong(RuleKeys.UPDATED_AT, 0L)
            );
        } catch (Throwable ignored) {
            return empty();
        }
    }

    public String toJson() {
        try {
            JSONObject root = new JSONObject();
            root.put(RuleKeys.BLOCK_LIVE, blockLive);
            root.put(RuleKeys.DEBUG_LOG, debugLog);
            root.put(RuleKeys.UPDATED_AT, updatedAt);
            JSONObject jsonApps = new JSONObject();
            for (Map.Entry<String, AppRules> entry : apps.entrySet()) {
                jsonApps.put(entry.getKey(), entry.getValue().toJson());
            }
            root.put("apps", jsonApps);
            return root.toString();
        } catch (Throwable ignored) {
            return "{}";
        }
    }

    public AppRules forPackage(String packageName) {
        AppRules rules = apps.get(packageName);
        return rules == null ? AppRules.defaults(packageName) : rules;
    }

    public boolean hasActiveRules(String packageName) {
        AppRules rules = forPackage(packageName);
        return rules.enabled && (rules.blockLive || rules.blockAds || rules.hasActiveCategory());
    }

    public static List<String> parseLines(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = raw.split("[\\r\\n,，]+");
        ArrayList<String> result = new ArrayList<>();
        for (String line : lines) {
            String normalized = normalize(line);
            if (!normalized.isEmpty() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String joinLines(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    public static String normalize(CharSequence value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toString(), Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ');
    }

    private static JSONArray toArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array;
    }

    private static List<String> fromArray(JSONArray array) {
        if (array == null || array.length() == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = normalize(array.optString(i, ""));
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }

    public static final class AppRules {
        public final String packageName;
        public final boolean enabled;
        public final boolean blockUsers;
        public final boolean blockTopics;
        public final boolean blockSections;
        public final boolean blockLive;
        public final boolean blockAds;
        public final List<String> userKeywords;
        public final List<String> topicKeywords;
        public final List<String> sectionKeywords;
        public final List<String> adKeywords;

        AppRules(
                String packageName,
                boolean enabled,
                boolean blockUsers,
                boolean blockTopics,
                boolean blockSections,
                boolean blockLive,
                boolean blockAds,
                List<String> userKeywords,
                List<String> topicKeywords,
                List<String> sectionKeywords,
                List<String> adKeywords
        ) {
            this.packageName = packageName;
            this.enabled = enabled;
            this.blockUsers = blockUsers;
            this.blockTopics = blockTopics;
            this.blockSections = blockSections;
            this.blockLive = blockLive;
            this.blockAds = blockAds;
            this.userKeywords = immutableCopy(userKeywords);
            this.topicKeywords = immutableCopy(topicKeywords);
            this.sectionKeywords = immutableCopy(sectionKeywords);
            this.adKeywords = immutableCopy(adKeywords);
        }

        static AppRules defaults(String packageName) {
            return new AppRules(packageName, true, true, true, true, true, true,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        static AppRules fromPreferences(SharedPreferences prefs, String packageName, boolean legacyBlockLive) {
            return new AppRules(
                    packageName,
                    prefs.getBoolean(RuleKeys.appEnabledKey(packageName), true),
                    prefs.getBoolean(RuleKeys.userEnabledKey(packageName), true),
                    prefs.getBoolean(RuleKeys.topicEnabledKey(packageName), true),
                    prefs.getBoolean(RuleKeys.sectionEnabledKey(packageName), true),
                    prefs.getBoolean(RuleKeys.liveEnabledKey(packageName), legacyBlockLive),
                    prefs.getBoolean(RuleKeys.adEnabledKey(packageName), true),
                    parseLines(prefs.getString(RuleKeys.userKeywordsKey(packageName), "")),
                    parseLines(prefs.getString(RuleKeys.topicKeywordsKey(packageName), "")),
                    parseLines(prefs.getString(RuleKeys.sectionKeywordsKey(packageName), "")),
                    parseLines(prefs.getString(RuleKeys.adKeywordsKey(packageName), ""))
            );
        }

        static AppRules fromJson(String packageName, JSONObject json, boolean legacyBlockLive) {
            if (json == null) {
                return new AppRules(packageName, true, true, true, true, legacyBlockLive, true,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }
            return new AppRules(
                    packageName,
                    json.optBoolean("enabled", true),
                    json.optBoolean("block_users", true),
                    json.optBoolean("block_topics", true),
                    json.optBoolean("block_sections", true),
                    json.optBoolean("block_live", legacyBlockLive),
                    json.optBoolean("block_ads", true),
                    fromArray(json.optJSONArray("user_keywords")),
                    fromArray(json.optJSONArray("topic_keywords")),
                    fromArray(json.optJSONArray("section_keywords")),
                    fromArray(json.optJSONArray("ad_keywords"))
            );
        }

        JSONObject toJson() throws Throwable {
            JSONObject json = new JSONObject();
            json.put("enabled", enabled);
            json.put("block_users", blockUsers);
            json.put("block_topics", blockTopics);
            json.put("block_sections", blockSections);
            json.put("block_live", blockLive);
            json.put("block_ads", blockAds);
            json.put("user_keywords", toArray(userKeywords));
            json.put("topic_keywords", toArray(topicKeywords));
            json.put("section_keywords", toArray(sectionKeywords));
            json.put("ad_keywords", toArray(adKeywords));
            return json;
        }

        public boolean hasActiveCategory() {
            return (blockUsers && !userKeywords.isEmpty())
                    || (blockTopics && !topicKeywords.isEmpty())
                    || (blockSections && !sectionKeywords.isEmpty());
        }

        private static List<String> immutableCopy(List<String> values) {
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}
