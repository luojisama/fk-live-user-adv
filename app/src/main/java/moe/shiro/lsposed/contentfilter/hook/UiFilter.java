package moe.shiro.lsposed.contentfilter.hook;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import moe.shiro.lsposed.contentfilter.config.RuleKeys;
import moe.shiro.lsposed.contentfilter.config.RulesSnapshot;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XposedBridge;

final class UiFilter {
    private static final int TAG_COLLAPSED_REASON = 0x7f0f0421;
    private static final int TAG_PENDING_CHECK = 0x7f0f0422;
    private static final int TAG_ORIGINAL_HEIGHT = 0x7f0f0423;
    private static final int MAX_TEXT_LENGTH = 260;
    private static final int MAX_USER_TEXT_LENGTH = 80;
    private static final int MAX_PARENT_DEPTH = 14;
    private static final int MAX_SCAN_TEXT_VIEWS = 80;
    private static final int MAX_MODEL_DEPTH = 3;
    private static final int MAX_MODEL_FIELDS = 96;
    private static final int MAX_MODEL_METHODS = 24;
    private static final int MAX_MODEL_COLLECTION_ITEMS = 8;
    private static final Map<Object, Boolean> PAGER_RECYCLER_ADAPTERS = new WeakHashMap<>();
    private static final Map<Object, Boolean> NON_FEED_RECYCLER_ADAPTERS = new WeakHashMap<>();
    private static final String[] NON_FEED_SURFACE_SIGNATURES = {
            "drawer",
            "navigationview",
            "popup",
            "dialog",
            "menu",
            "setting",
            "preference",
            "comment",
            "comments",
            "reply",
            "discussion",
            "danmaku",
            "danmu",
            "评论",
            "回复",
            "弹幕"
    };
    private static final String[] NON_FEED_BINDING_SIGNATURES = {
            "comment2",
            "comment3",
            "commentv3",
            "commentlist",
            "commentitem",
            "commentcard",
            "commenttree",
            "commentarea",
            "commentdetail",
            "commentfeedlist",
            "commentfragment",
            "commenthalfweb",
            "commentpage",
            "commentsearch",
            "commentadapter",
            "commentsview",
            "replylist",
            "replyitem",
            "mainreply",
            "subreply",
            "replydetail",
            "replyfragment",
            "replypage",
            "replyadapter",
            "discussion",
            "danmaku",
            "danmu",
            "biliresourcecomment",
            "appcommcomment",
            "appcomment",
            "评论",
            "回复",
            "弹幕"
    };
    private static final String[] NON_FEED_MODEL_FIELD_SIGNATURES = {
            "commentlist",
            "commentitem",
            "commentcard",
            "commenttree",
            "commentarea",
            "replylist",
            "replyitem",
            "mainreply",
            "subreply",
            "danmakulist",
            "danmulist"
    };
    private static final String[] LIVE_VIEW_SIGNATURES = {
            "live",
            "living",
            "liveroom",
            "live_room",
            "livestream",
            "live_stream",
            "broadcast",
            "anchor"
    };
    private static final String[] AD_VIEW_SIGNATURES = {
            "feedad",
            "feed_ad",
            "adfeed",
            "ad_feed",
            "adcard",
            "ad_card",
            "nativead",
            "native_ad",
            "commercialize",
            "commercial",
            "advertise",
            "advertisement",
            "sponsor",
            "sponsored",
            "promotion",
            "douyinmall",
            "douyin_mall",
            "ecommerce",
            "commerce",
            "shopping",
            "shop"
    };
    private static final String[] MODEL_LIVE_SIGNATURES = {
            "is_live",
            "islive",
            "live_status",
            "livestatus",
            "live_state",
            "livestate",
            "livebadge",
            "live_badge",
            "livecard",
            "live_card",
            "liveroom",
            "live_room",
            "liveroomid",
            "live_room_id",
            "liveid",
            "live_id",
            "roomid",
            "room_id",
            "livestream",
            "live_stream",
            "livepreview",
            "live_preview",
            "inlinelive",
            "inline_live",
            "righttoplivebadge",
            "right_top_live_badge",
            "anchorinfo",
            "anchor_info"
    };
    private static final String[] MODEL_AD_SIGNATURES = {
            "adcommon",
            "adinfo",
            "ad_info",
            "aditem",
            "ad_item",
            "admark",
            "ad_mark",
            "adroot",
            "ad_root",
            "adtitle",
            "ad_title",
            "adwrapper",
            "ad_wrapper",
            "adreport",
            "ad_report",
            "adcard",
            "ad_card",
            "adfeed",
            "ad_feed",
            "feedad",
            "feed_ad",
            "nativead",
            "native_ad",
            "is_ad",
            "isad",
            "cminfo",
            "cm_info",
            "cmmark",
            "cm_mark",
            "creative_id",
            "creativeid",
            "commercialize",
            "commercial",
            "commerce",
            "ecommerce",
            "ecom",
            "douyinmall",
            "douyin_mall",
            "mallcard",
            "mall_card",
            "shopwindow",
            "shop_window",
            "shopping",
            "promotion",
            "sponsor",
            "downloadcompliance"
    };

    private UiFilter() {
    }

    static void rememberRecyclerAdapter(Object adapter, View recyclerView) {
        if (adapter == null || recyclerView == null) {
            return;
        }
        markNonFeedRecyclerAdapter(adapter);
        if (!markPagerRecyclerAdapter(adapter, recyclerView)) {
            recyclerView.post(() -> markPagerRecyclerAdapter(adapter, recyclerView));
        }
    }

    static void handleTextChanged(
            Context context,
            TextView textView,
            CharSequence text,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (textView instanceof EditText
                || text == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, textView)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        Match match = matchText(rules, packageName, text);
        if (!match.blocked) {
            return;
        }
        scheduleCollapse(context, textView, match, rules, packageName, processName, currentActivity);
    }

    static void handleBoundItem(
            Context context,
            View itemView,
            Object adapter,
            Object holder,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (itemView == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)) {
            return;
        }
        if (isNonFeedBinding(context, itemView, adapter, holder, packageName, currentActivity)) {
            restoreView(itemView);
            return;
        }
        boolean pagerPageItem = isPagerPageItem(itemView, adapter, holder);
        RulesSnapshot rules = RulesCache.get(context);
        if (!rules.hasActiveRules(packageName)) {
            restoreView(itemView);
            return;
        }
        Match match = scanViewTree(context, rules, packageName, itemView, 0, new int[]{0});
        if (!match.blocked) {
            match = scanBoundModel(rules, packageName, itemView, holder);
        }
        if (match.blocked) {
            if (pagerPageItem) {
                concealPagerPage(itemView, match);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager page concealed package=" + packageName
                            + " process=" + processName
                            + " activity=" + currentActivity
                            + " reason=" + match
                            + " container=" + itemView.getClass().getName());
                }
                return;
            }
            collapseView(itemView, match);
            if (rules.debugLog) {
                XposedBridge.log("[LCF] bind collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " container=" + itemView.getClass().getName());
            }
        } else {
            restoreView(itemView);
        }
    }

    private static Match scanViewTree(
            Context context,
            RulesSnapshot rules,
            String packageName,
            View view,
            int depth,
            int[] textViews
    ) {
        if (depth > MAX_PARENT_DEPTH || textViews[0] > MAX_SCAN_TEXT_VIEWS) {
            return Match.none();
        }
        if (view instanceof TextView && !(view instanceof EditText)) {
            textViews[0]++;
            Match match = matchText(rules, packageName, ((TextView) view).getText());
            if (match.blocked) {
                return match;
            }
        }
        Object tag = view.getTag();
        if (tag instanceof CharSequence) {
            Match match = matchText(rules, packageName, (CharSequence) tag);
            if (match.blocked) {
                return match;
            }
        }
        CharSequence description = view.getContentDescription();
        if (description != null) {
            Match match = matchText(rules, packageName, description);
            if (match.blocked) {
                return match;
            }
        }
        Match identifierMatch = matchViewIdentity(context, rules, packageName, view);
        if (identifierMatch.blocked) {
            return identifierMatch;
        }
        if (!(view instanceof ViewGroup)) {
            return Match.none();
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            Match child = scanViewTree(context, rules, packageName, group.getChildAt(i), depth + 1, textViews);
            if (child.blocked) {
                return child;
            }
        }
        return Match.none();
    }

    private static Match scanBoundModel(
            RulesSnapshot rules,
            String packageName,
            View itemView,
            Object holder
    ) {
        if (!rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] budget = new int[]{0};
        Match holderMatch = scanModelObject(rules, packageName, holder, 0, visited, budget, "");
        if (holderMatch.blocked) {
            return holderMatch;
        }
        Object tag = itemView.getTag();
        if (tag != null) {
            return scanModelObject(rules, packageName, tag, 0, visited, budget, "");
        }
        return Match.none();
    }

    private static Match scanModelObject(
            RulesSnapshot rules,
            String packageName,
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget,
            String fieldName
    ) {
        if (value == null || depth > MAX_MODEL_DEPTH || budget[0] > MAX_MODEL_FIELDS) {
            return Match.none();
        }
        Match fieldMatch = matchModelSignal(rules, packageName, fieldName, value);
        if (fieldMatch.blocked) {
            return fieldMatch;
        }
        if (value instanceof CharSequence) {
            return matchText(rules, packageName, (CharSequence) value);
        }
        Class<?> type = value.getClass();
        if (isTerminalModelValue(type)) {
            return Match.none();
        }
        if (visited.containsKey(value)) {
            return Match.none();
        }
        visited.put(value, Boolean.TRUE);
        Match identity = matchModelIdentity(rules, packageName, type.getName());
        if (identity.blocked) {
            return identity;
        }
        if (type.isArray()) {
            return scanModelArray(rules, packageName, value, depth, visited, budget);
        }
        if (value instanceof Iterable) {
            return scanModelIterable(rules, packageName, (Iterable<?>) value, depth, visited, budget);
        }
        if (value instanceof Map) {
            return scanModelMap(rules, packageName, (Map<?, ?>) value, depth, visited, budget);
        }
        if (shouldSkipModelClass(type)) {
            return Match.none();
        }
        Match accessorMatch = scanModelAccessors(rules, packageName, value, type, budget);
        if (accessorMatch.blocked) {
            return accessorMatch;
        }
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= MAX_MODEL_FIELDS) {
            Field[] fields;
            try {
                fields = current.getDeclaredFields();
            } catch (Throwable ignored) {
                break;
            }
            for (Field field : fields) {
                if (budget[0]++ > MAX_MODEL_FIELDS || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object child;
                try {
                    field.setAccessible(true);
                    child = field.get(value);
                } catch (Throwable ignored) {
                    continue;
                }
                Match childMatch = scanModelObject(
                        rules,
                        packageName,
                        child,
                        depth + 1,
                        visited,
                        budget,
                        normalizeIdentityToken(field.getName())
                );
                if (childMatch.blocked) {
                    return childMatch;
                }
            }
            current = current.getSuperclass();
        }
        return Match.none();
    }

    private static Match scanModelAccessors(
            RulesSnapshot rules,
            String packageName,
            Object value,
            Class<?> type,
            int[] budget
    ) {
        int methodCount = 0;
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= MAX_MODEL_FIELDS) {
            Method[] methods;
            try {
                methods = current.getDeclaredMethods();
            } catch (Throwable ignored) {
                break;
            }
            for (Method method : methods) {
                if (!isSignalAccessor(rules, packageName, method)) {
                    continue;
                }
                if (methodCount++ >= MAX_MODEL_METHODS || budget[0]++ > MAX_MODEL_FIELDS) {
                    return Match.none();
                }
                Object child;
                try {
                    method.setAccessible(true);
                    child = method.invoke(value);
                } catch (Throwable ignored) {
                    continue;
                }
                Match signal = matchModelSignal(rules, packageName, method.getName(), child);
                if (signal.blocked) {
                    return signal;
                }
            }
            current = current.getSuperclass();
        }
        return Match.none();
    }

    private static boolean isSignalAccessor(RulesSnapshot rules, String packageName, Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)
                || method.isSynthetic()
                || method.isBridge()
                || method.getParameterTypes().length != 0
                || method.getReturnType() == Void.TYPE) {
            return false;
        }
        String name = method.getName();
        if (!isAccessorName(name)) {
            return false;
        }
        return hasModelSignalName(rules, packageName, normalizeIdentityToken(name));
    }

    private static boolean isAccessorName(String name) {
        if (name == null) {
            return false;
        }
        return (name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2)
                || (name.startsWith("has") && name.length() > 3);
    }

    private static boolean hasModelSignalName(RulesSnapshot rules, String packageName, String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty() || !rules.hasActiveRules(packageName)) {
            return false;
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return false;
        }
        return (appRules.blockAds && containsAny(normalizedName, MODEL_AD_SIGNATURES) != null)
                || (appRules.blockLive && containsAny(normalizedName, MODEL_LIVE_SIGNATURES) != null);
    }

    private static Match scanModelArray(
            RulesSnapshot rules,
            String packageName,
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        int length = Math.min(Array.getLength(value), MAX_MODEL_COLLECTION_ITEMS);
        for (int i = 0; i < length; i++) {
            Match match = scanModelObject(rules, packageName, Array.get(value, i), depth + 1, visited, budget, "");
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match scanModelIterable(
            RulesSnapshot rules,
            String packageName,
            Iterable<?> values,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        int count = 0;
        for (Object child : values) {
            if (count++ >= MAX_MODEL_COLLECTION_ITEMS) {
                break;
            }
            Match match = scanModelObject(rules, packageName, child, depth + 1, visited, budget, "");
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match scanModelMap(
            RulesSnapshot rules,
            String packageName,
            Map<?, ?> values,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        int count = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (count++ >= MAX_MODEL_COLLECTION_ITEMS) {
                break;
            }
            String key = entry.getKey() instanceof CharSequence
                    ? normalizeIdentityToken(entry.getKey().toString())
                    : "";
            Match match = scanModelObject(rules, packageName, entry.getValue(), depth + 1, visited, budget, key);
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match matchModelSignal(
            RulesSnapshot rules,
            String packageName,
            String rawName,
            Object value
    ) {
        if (rawName == null || rawName.isEmpty() || !rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String name = normalizeIdentityToken(rawName);
        if (appRules.blockAds) {
            String signal = containsAny(name, MODEL_AD_SIGNATURES);
            if (signal != null && hasPresentSignalValue(value)) {
                return Match.blocked("ad_model", rawName);
            }
        }
        if (appRules.blockLive) {
            String signal = containsAny(name, MODEL_LIVE_SIGNATURES);
            if (signal != null && hasPresentSignalValue(value)) {
                return Match.blocked("live_model", rawName);
            }
        }
        return Match.none();
    }

    private static Match matchModelIdentity(RulesSnapshot rules, String packageName, String className) {
        if (className == null || className.isEmpty() || !rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String identity = normalizeIdentityToken(className);
        if (appRules.blockAds) {
            String signal = containsAny(identity, MODEL_AD_SIGNATURES);
            if (signal != null) {
                return Match.blocked("ad_model", signal);
            }
        }
        if (appRules.blockLive) {
            String signal = containsAny(identity, MODEL_LIVE_SIGNATURES);
            if (signal != null) {
                return Match.blocked("live_model", signal);
            }
        }
        return Match.none();
    }

    private static boolean hasPresentSignalValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue() != 0L;
        }
        if (value instanceof CharSequence) {
            String normalized = RulesSnapshot.normalize((CharSequence) value);
            return !normalized.isEmpty()
                    && !"0".equals(normalized)
                    && !"false".equals(normalized)
                    && !"null".equals(normalized);
        }
        return true;
    }

    private static boolean isTerminalModelValue(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Enum.class.isAssignableFrom(type);
    }

    private static boolean shouldSkipModelClass(Class<?> type) {
        String name = type.getName();
        return View.class.isAssignableFrom(type)
                || Context.class.isAssignableFrom(type)
                || name.startsWith("android.")
                || name.startsWith("androidx.")
                || name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("kotlin.")
                || name.startsWith("dalvik.")
                || name.startsWith("de.robv.")
                || name.startsWith("moe.shiro.");
    }

    private static String normalizeIdentityToken(String value) {
        return value == null
                ? ""
                : RulesSnapshot.normalize(value).replace(".", "").replace("_", "").replace("-", "");
    }

    private static Match matchText(RulesSnapshot rules, String packageName, CharSequence rawText) {
        if (!rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String text = RulesSnapshot.normalize(rawText);
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            return Match.none();
        }
        if (appRules.blockUsers && text.length() <= MAX_USER_TEXT_LENGTH) {
            String keyword = containsAny(text, appRules.userKeywords);
            if (keyword != null) {
                return Match.blocked("user", keyword);
            }
        }
        if (appRules.blockTopics) {
            String keyword = containsAny(text, appRules.topicKeywords);
            if (keyword != null) {
                return Match.blocked("topic", keyword);
            }
        }
        if (appRules.blockSections) {
            String keyword = containsAny(text, appRules.sectionKeywords);
            if (keyword != null) {
                return Match.blocked("section", keyword);
            }
        }
        if (appRules.blockAds) {
            String ad = containsAny(text, appRules.adKeywords);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
            ad = containsAny(text, RuleKeys.defaultAdKeywordsForPackage(packageName));
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
            ad = containsAny(text, RuleKeys.AD_KEYWORDS);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
        }
        if (appRules.blockLive) {
            String live = containsAny(text, RuleKeys.LIVE_KEYWORDS);
            if (live != null) {
                return Match.blocked("live", live);
            }
        }
        return Match.none();
    }

    private static Match matchViewIdentity(
            Context context,
            RulesSnapshot rules,
            String packageName,
            View view
    ) {
        if (!rules.hasActiveRules(packageName) || view == null) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        StringBuilder identity = new StringBuilder(view.getClass().getName().toLowerCase());
        String resourceName = resourceName(context, view.getId());
        if (!resourceName.isEmpty()) {
            identity.append(' ').append(resourceName);
        }
        Object tag = view.getTag();
        if (tag != null && !(tag instanceof CharSequence)) {
            identity.append(' ').append(tag.getClass().getName().toLowerCase());
        }
        String raw = identity.toString();
        if (appRules.blockAds) {
            String ad = containsAny(raw, AD_VIEW_SIGNATURES);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
        }
        if (appRules.blockLive) {
            String live = containsAny(raw, LIVE_VIEW_SIGNATURES);
            if (live != null) {
                return Match.blocked("live", live);
            }
        }
        return Match.none();
    }

    private static String resourceName(Context context, int id) {
        if (context == null || id == View.NO_ID) {
            return "";
        }
        try {
            return context.getResources().getResourceEntryName(id).toLowerCase();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private static String containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private static void scheduleCollapse(
            Context context,
            TextView textView,
            Match match,
            RulesSnapshot rules,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (textView.getTag(TAG_COLLAPSED_REASON) instanceof String || textView.getTag(TAG_PENDING_CHECK) != null) {
            return;
        }
        textView.setTag(TAG_PENDING_CHECK, Boolean.TRUE);
        textView.post(() -> {
            textView.setTag(TAG_PENDING_CHECK, null);
            View target = findCollapsibleContainer(textView);
            if (target == null) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] matched but no feed container: " + match
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            Object adapter = adapterFromNearestRecyclerView(textView);
            if (isNonFeedBinding(context, target, adapter, null, packageName, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip non-feed text target: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            if (isPagerPageItem(target, null, null)) {
                concealPagerPage(target, match);
                textView.setTag(TAG_COLLAPSED_REASON, match.toString());
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager text target concealed: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            if (looksLikeWholeScreen(target)) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip oversized text target: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            collapseView(target, match);
            textView.setTag(TAG_COLLAPSED_REASON, match.toString());
            if (rules.debugLog) {
                XposedBridge.log("[LCF] text collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " container=" + target.getClass().getName()
                        + " text=" + sanitize(textView.getText()));
            }
        });
    }

    private static View findCollapsibleContainer(View leaf) {
        View current = leaf;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            ViewParent parent = current.getParent();
            if (parent == null) {
                return null;
            }
            String parentClass = parent.getClass().getName();
            if (isRecyclerContainerClass(parentClass)) {
                return current;
            }
            if (!(parent instanceof View)) {
                return null;
            }
            current = (View) parent;
        }
        return null;
    }

    private static boolean isRecyclerContainerClass(String className) {
        return className.contains("RecyclerView");
    }

    private static boolean isNonFeedBinding(
            Context context,
            View itemView,
            Object adapter,
            Object holder,
            String packageName,
            String currentActivity
    ) {
        if (itemView == null) {
            return false;
        }
        if (isInsideNonFeedSurface(context, itemView)
                || isKnownNonFeedAdapter(adapter)
                || containsNonFeedIdentity(adapter)
                || containsNonFeedIdentity(holder)) {
            return true;
        }
        Object tag = itemView.getTag();
        if (tag != null && !(tag instanceof CharSequence) && containsNonFeedIdentity(tag)) {
            return true;
        }
        if (!isBiliPackage(packageName) || !ActivityClassifier.isContentActivity(currentActivity)) {
            return false;
        }
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] budget = new int[]{0};
        return containsNonFeedModelIdentity(holder, 0, visited, budget)
                || containsNonFeedModelIdentity(tag, 0, visited, budget);
    }

    private static boolean isInsideNonFeedSurface(Context context, View view) {
        View current = view;
        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
            String identity = nonFeedIdentity(context, current);
            if (containsAny(identity, NON_FEED_SURFACE_SIGNATURES) != null) {
                return true;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static String nonFeedIdentity(Context context, View view) {
        StringBuilder identity = new StringBuilder(view.getClass().getName().toLowerCase());
        String resourceName = resourceName(context, view.getId());
        if (!resourceName.isEmpty()) {
            identity.append(' ').append(resourceName);
        }
        CharSequence description = view.getContentDescription();
        if (description != null) {
            identity.append(' ').append(RulesSnapshot.normalize(description));
        }
        Object tag = view.getTag();
        if (tag instanceof CharSequence) {
            identity.append(' ').append(RulesSnapshot.normalize((CharSequence) tag));
        } else if (tag != null) {
            identity.append(' ').append(tag.getClass().getName().toLowerCase());
        }
        return identity.toString();
    }

    private static boolean isBiliPackage(String packageName) {
        return "tv.danmaku.bili".equals(packageName) || "com.bilibili.app.in".equals(packageName);
    }

    private static boolean isNonFeedActivity(String currentActivity) {
        return containsAny(normalizeIdentityToken(currentActivity), NON_FEED_BINDING_SIGNATURES) != null;
    }

    private static boolean containsNonFeedIdentity(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value instanceof Class ? (Class<?>) value : value.getClass();
        for (int depth = 0; depth < 5 && type != null && type != Object.class; depth++) {
            if (containsAny(normalizeIdentityToken(type.getName()), NON_FEED_BINDING_SIGNATURES) != null) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean containsNonFeedModelIdentity(
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        if (value == null || depth > 2 || budget[0] > 48) {
            return false;
        }
        Class<?> type = value.getClass();
        if (isTerminalModelValue(type) || View.class.isAssignableFrom(type) || Context.class.isAssignableFrom(type)) {
            return false;
        }
        if (visited.containsKey(value)) {
            return false;
        }
        visited.put(value, Boolean.TRUE);
        if (containsNonFeedIdentity(value)) {
            return true;
        }
        if (shouldSkipModelClass(type)) {
            return false;
        }
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= 48) {
            Field[] fields;
            try {
                fields = current.getDeclaredFields();
            } catch (Throwable ignored) {
                break;
            }
            for (Field field : fields) {
                if (budget[0]++ > 48 || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String fieldIdentity = normalizeIdentityToken(field.getName());
                if (containsAny(fieldIdentity, NON_FEED_MODEL_FIELD_SIGNATURES) != null) {
                    return true;
                }
                Object child;
                try {
                    field.setAccessible(true);
                    child = field.get(value);
                } catch (Throwable ignored) {
                    continue;
                }
                if (containsNonFeedModelIdentity(child, depth + 1, visited, budget)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static void markNonFeedRecyclerAdapter(Object adapter) {
        if (adapter == null || !containsNonFeedIdentity(adapter)) {
            return;
        }
        synchronized (NON_FEED_RECYCLER_ADAPTERS) {
            NON_FEED_RECYCLER_ADAPTERS.put(adapter, Boolean.TRUE);
        }
    }

    private static boolean isKnownNonFeedAdapter(Object adapter) {
        if (adapter == null) {
            return false;
        }
        synchronized (NON_FEED_RECYCLER_ADAPTERS) {
            return NON_FEED_RECYCLER_ADAPTERS.containsKey(adapter);
        }
    }

    private static Object adapterFromNearestRecyclerView(View leaf) {
        View current = leaf;
        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
            ViewParent parent = current.getParent();
            if (parent == null) {
                return null;
            }
            if (parent instanceof View && isRecyclerContainerClass(parent.getClass().getName())) {
                return adapterFromRecyclerView((View) parent);
            }
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private static Object adapterFromRecyclerView(View recyclerView) {
        if (recyclerView == null) {
            return null;
        }
        try {
            Method method = recyclerView.getClass().getMethod("getAdapter");
            return method.invoke(recyclerView);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPagerPageItem(View itemView, Object adapter, Object holder) {
        if (isKnownPagerAdapter(adapter) || containsPagerIdentity(adapter) || containsPagerIdentity(holder)) {
            return true;
        }
        if (itemView == null) {
            return false;
        }
        ViewParent parent = itemView.getParent();
        if (!(parent instanceof View)) {
            return false;
        }
        View parentView = (View) parent;
        ViewParent grandParent = parentView.getParent();
        if (!(grandParent instanceof View)) {
            return false;
        }
        String parentName = parentView.getClass().getName().toLowerCase();
        String grandParentName = grandParent.getClass().getName().toLowerCase();
        return parentName.contains("recyclerview")
                && (grandParentName.contains("viewpager2") || grandParentName.contains("viewpager"));
    }

    private static boolean markPagerRecyclerAdapter(Object adapter, View recyclerView) {
        ViewParent parent = recyclerView.getParent();
        if (parent == null) {
            return false;
        }
        String parentName = parent.getClass().getName().toLowerCase();
        if (!parentName.contains("viewpager2") && !parentName.contains("viewpager")) {
            return false;
        }
        synchronized (PAGER_RECYCLER_ADAPTERS) {
            if (PAGER_RECYCLER_ADAPTERS.containsKey(adapter)) {
                return true;
            }
            PAGER_RECYCLER_ADAPTERS.put(adapter, Boolean.TRUE);
        }
        XposedBridge.log("[LCF] pager RecyclerView adapter marked: " + adapter.getClass().getName());
        return true;
    }

    private static boolean isKnownPagerAdapter(Object adapter) {
        if (adapter == null) {
            return false;
        }
        synchronized (PAGER_RECYCLER_ADAPTERS) {
            return PAGER_RECYCLER_ADAPTERS.containsKey(adapter);
        }
    }

    private static boolean containsPagerIdentity(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value.getClass();
        for (int depth = 0; depth < 4 && type != null; depth++) {
            String name = type.getName().toLowerCase();
            if (name.contains("viewpager2")
                    || name.contains("viewpager")
                    || name.contains("fragmentstateadapter")
                    || name.contains("fragmentviewholder")) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean looksLikeWholeScreen(View view) {
        View root = view.getRootView();
        if (root == null || root == view || root.getWidth() <= 0 || root.getHeight() <= 0) {
            return false;
        }
        return view.getWidth() >= root.getWidth() * 0.85f
                && view.getHeight() >= root.getHeight() * 0.75f;
    }

    private static void collapseView(View view, Match match) {
        if (!(view.getTag(TAG_ORIGINAL_HEIGHT) instanceof Integer)) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            view.setTag(TAG_ORIGINAL_HEIGHT, params == null ? ViewGroup.LayoutParams.WRAP_CONTENT : params.height);
        }
        view.setTag(TAG_COLLAPSED_REASON, match.toString());
        view.setVisibility(View.GONE);
        view.setAlpha(0f);
        view.setMinimumHeight(0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.height != 0) {
            params.height = 0;
            view.setLayoutParams(params);
        }
        view.requestLayout();
    }

    private static void concealPagerPage(View view, Match match) {
        view.setTag(TAG_COLLAPSED_REASON, match.toString());
        view.setVisibility(View.INVISIBLE);
        view.setAlpha(0f);
        view.requestLayout();
    }

    private static void restoreView(View view) {
        if (!(view.getTag(TAG_COLLAPSED_REASON) instanceof String)) {
            return;
        }
        view.setTag(TAG_COLLAPSED_REASON, null);
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        Object originalHeight = view.getTag(TAG_ORIGINAL_HEIGHT);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && originalHeight instanceof Integer) {
            params.height = (Integer) originalHeight;
            view.setLayoutParams(params);
        }
        view.requestLayout();
    }

    private static String sanitize(CharSequence value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > 120) {
            return text.substring(0, 120) + "...";
        }
        return text;
    }

    private static final class Match {
        final boolean blocked;
        final String type;
        final String keyword;

        private Match(boolean blocked, String type, String keyword) {
            this.blocked = blocked;
            this.type = type;
            this.keyword = keyword;
        }

        static Match none() {
            return new Match(false, "", "");
        }

        static Match blocked(String type, String keyword) {
            return new Match(true, type, keyword);
        }

        @Override
        public String toString() {
            return type + ":" + keyword;
        }
    }
}
