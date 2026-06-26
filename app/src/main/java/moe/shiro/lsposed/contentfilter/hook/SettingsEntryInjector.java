package moe.shiro.lsposed.contentfilter.hook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import moe.shiro.lsposed.contentfilter.config.RuleKeys;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class SettingsEntryInjector {
    private static final String ENTRY_TAG = "moe.shiro.lsposed.contentfilter.settings.entry";
    private static final String PREF_KEY = "moe.shiro.lsposed.contentfilter.settings";
    private static final AtomicBoolean BILI_PREF_HOOKED = new AtomicBoolean(false);

    private SettingsEntryInjector() {
    }

    static void install(ClassLoader classLoader, Context appContext, CurrentTarget currentTarget) {
        if ("tv.danmaku.bili".equals(currentTarget.packageName)
                || "com.bilibili.app.in".equals(currentTarget.packageName)) {
            installBiliPreferenceEntry(classLoader, appContext, currentTarget);
        }
    }

    static void injectActivityEntry(Activity activity, CurrentTarget currentTarget) {
        if (activity == null || !ActivityClassifier.isTargetSettingsActivity(activity.getClass().getName())) {
            return;
        }
        if ("com.bilibili.app.preferences.BiliPreferencesActivity".equals(activity.getClass().getName())) {
            return;
        }
        ViewGroup target = findTargetContainer(activity);
        if (target == null || hasInjectedEntry(target)) {
            return;
        }
        View row = createActivityRow(activity, currentTarget.packageName);
        ViewGroup.LayoutParams params = layoutParamsFor(target);
        if (target instanceof LinearLayout) {
            target.addView(row, Math.min(1, target.getChildCount()), params);
        } else {
            target.addView(row, params);
        }
    }

    private static void installBiliPreferenceEntry(
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget
    ) {
        if (!BILI_PREF_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> prefClass = Class.forName("androidx.preference.Preference", false, classLoader);
            XposedHelpers.findAndHookMethod(
                    "com.bilibili.app.preferences.BiliPreferencesActivity$BiliPreferencesFragment",
                    classLoader,
                    "onCreatePreferences",
                    Bundle.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            addBiliPreference(param.thisObject, prefClass, appContext, currentTarget.packageName);
                        }
                    }
            );
            hookPreferenceClick(
                    "androidx.preference.PreferenceFragmentCompat",
                    classLoader,
                    prefClass,
                    appContext,
                    currentTarget
            );
            hookPreferenceClick(
                    "com.bilibili.app.preferences.BiliPreferencesActivity$BiliPreferencesFragment",
                    classLoader,
                    prefClass,
                    appContext,
                    currentTarget
            );
            XposedBridge.log("[LCF] Bili settings preference hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log("[LCF] Bili settings preference hook unavailable: " + throwable);
        }
    }

    private static void hookPreferenceClick(
            String fragmentClassName,
            ClassLoader classLoader,
            Class<?> prefClass,
            Context appContext,
            CurrentTarget currentTarget
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                    fragmentClassName,
                    classLoader,
                    "onPreferenceTreeClick",
                    prefClass,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object preference = param.args[0];
                            if (!PREF_KEY.equals(invokeString(preference, "getKey"))) {
                                return;
                            }
                            Context context = contextFromFragment(param.thisObject);
                            launchConfig(context == null ? appContext : context, currentTarget.packageName);
                            param.setResult(Boolean.TRUE);
                        }
                    }
            );
        } catch (Throwable ignored) {
        }
    }

    private static void addBiliPreference(
            Object fragment,
            Class<?> prefClass,
            Context fallbackContext,
            String packageName
    ) {
        try {
            Object existing = invoke(fragment, "findPreference", new Class<?>[]{CharSequence.class}, PREF_KEY);
            if (existing != null) {
                return;
            }
            Context context = contextFromFragment(fragment);
            if (context == null) {
                context = fallbackContext;
            }
            Object screen = invoke(fragment, "getPreferenceScreen", new Class<?>[0]);
            if (screen == null || context == null) {
                return;
            }
            Object preference = prefClass.getConstructor(Context.class).newInstance(context);
            prefClass.getMethod("setKey", String.class).invoke(preference, PREF_KEY);
            prefClass.getMethod("setTitle", CharSequence.class).invoke(preference, "Shiro 屏蔽助手");
            prefClass.getMethod("setSummary", CharSequence.class).invoke(preference, "用户、话题、分区、直播");
            invoke(screen, "addPreference", new Class<?>[]{prefClass}, preference);
            XposedBridge.log("[LCF] Bili settings entry added for " + packageName);
        } catch (Throwable throwable) {
            XposedBridge.log("[LCF] failed to add Bili settings entry: " + throwable);
        }
    }

    private static View createActivityRow(Context context, String packageName) {
        LinearLayout row = new LinearLayout(context);
        row.setTag(ENTRY_TAG);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 18), dp(context, 12), dp(context, 18), dp(context, 12));
        row.setBackgroundColor(Color.rgb(240, 248, 248));
        row.setOnClickListener(v -> launchConfig(context, packageName));

        TextView title = new TextView(context);
        title.setText("Shiro 屏蔽助手");
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(0, 86, 91));
        row.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView summary = new TextView(context);
        summary.setText("用户、话题、分区、直播");
        summary.setTextSize(13);
        summary.setTextColor(Color.rgb(88, 99, 106));
        row.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private static ViewGroup findTargetContainer(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return null;
        }
        ViewGroup contentGroup = (ViewGroup) content;
        ViewGroup linear = findVerticalLinearLayout(contentGroup, 0);
        return linear == null ? contentGroup : linear;
    }

    private static ViewGroup findVerticalLinearLayout(View view, int depth) {
        if (depth > 10 || !(view instanceof ViewGroup)) {
            return null;
        }
        if (view instanceof LinearLayout
                && ((LinearLayout) view).getOrientation() == LinearLayout.VERTICAL
                && ((LinearLayout) view).getChildCount() >= 2) {
            return (ViewGroup) view;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup result = findVerticalLinearLayout(group.getChildAt(i), depth + 1);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static boolean hasInjectedEntry(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (ENTRY_TAG.equals(group.getChildAt(i).getTag())) {
                return true;
            }
        }
        return false;
    }

    private static ViewGroup.LayoutParams layoutParamsFor(ViewGroup parent) {
        if (parent instanceof LinearLayout) {
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;
        return params;
    }

    private static void launchConfig(Context context, String packageName) {
        Intent intent = new Intent();
        intent.setClassName(RuleKeys.MODULE_PACKAGE, RuleKeys.CONFIG_ACTIVITY);
        intent.putExtra("target_package", packageName);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private static Context contextFromFragment(Object fragment) {
        Object context = invoke(fragment, "getContext", new Class<?>[0]);
        return context instanceof Context ? (Context) context : null;
    }

    private static Object invoke(Object receiver, String method, Class<?>[] types, Object... args) {
        if (receiver == null) {
            return null;
        }
        try {
            Method m = receiver.getClass().getMethod(method, types);
            return m.invoke(receiver, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String invokeString(Object receiver, String method) {
        Object value = invoke(receiver, method, new Class<?>[0]);
        return value == null ? null : value.toString();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
