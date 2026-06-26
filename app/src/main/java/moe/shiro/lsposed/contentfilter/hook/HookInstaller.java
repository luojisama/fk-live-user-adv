package moe.shiro.lsposed.contentfilter.hook;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class HookInstaller {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final CurrentTarget CURRENT = new CurrentTarget();

    private HookInstaller() {
    }

    static void install(String targetPackageName, String targetProcessName, Context context, ClassLoader classLoader) {
        CURRENT.packageName = targetPackageName;
        CURRENT.processName = targetProcessName == null ? "" : targetProcessName;
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            appContext = context;
        }
        RulesCache.warmUp(appContext);
        hookActivityResume();
        hookTextUpdates(appContext);
        hookResourceBadges(appContext);
        RecyclerBindHook.install(classLoader, appContext, CURRENT);
        SettingsEntryInjector.install(classLoader, appContext, CURRENT);
        XposedBridge.log("[LCF] hooks installed for " + CURRENT.packageName + " process=" + CURRENT.processName);
    }

    private static void hookActivityResume() {
        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        CURRENT.currentActivity = param.thisObject.getClass().getName();
                        if (param.thisObject instanceof Activity) {
                            SettingsEntryInjector.injectActivityEntry((Activity) param.thisObject, CURRENT);
                        }
                    }
                }
        );
    }

    private static void hookTextUpdates(Context appContext) {
        XposedHelpers.findAndHookMethod(
                TextView.class,
                "setText",
                CharSequence.class,
                TextView.BufferType.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof TextView)) {
                            return;
                        }
                        TextView textView = (TextView) param.thisObject;
                        UiFilter.handleTextChanged(
                                appContext,
                                textView,
                                textView.getText(),
                                CURRENT.packageName,
                                CURRENT.processName,
                                CURRENT.currentActivity
                        );
                    }
                }
        );
    }

    private static void hookResourceBadges(Context appContext) {
        XposedHelpers.findAndHookMethod(
                ImageView.class,
                "setImageResource",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof ImageView)
                                || param.args == null
                                || param.args.length == 0
                                || !(param.args[0] instanceof Integer)) {
                            return;
                        }
                        UiFilter.handleResourceBadgeSet(
                                appContext,
                                (ImageView) param.thisObject,
                                (Integer) param.args[0],
                                "image",
                                CURRENT.packageName,
                                CURRENT.processName,
                                CURRENT.currentActivity
                        );
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                View.class,
                "setBackgroundResource",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)
                                || param.args == null
                                || param.args.length == 0
                                || !(param.args[0] instanceof Integer)) {
                            return;
                        }
                        UiFilter.handleResourceBadgeSet(
                                appContext,
                                (View) param.thisObject,
                                (Integer) param.args[0],
                                "background",
                                CURRENT.packageName,
                                CURRENT.processName,
                                CURRENT.currentActivity
                        );
                    }
                }
        );
    }
}

