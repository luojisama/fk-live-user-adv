package moe.shiro.lsposed.contentfilter.hook;

import android.app.Application;
import android.content.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import moe.shiro.lsposed.contentfilter.config.RuleKeys;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ModuleEntry implements IXposedHookLoadPackage {
    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            RuleKeys.TARGET_PACKAGES
    ));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !TARGET_PACKAGES.contains(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("[LCF] target package loaded: " + lpparam.packageName
                + " process=" + lpparam.processName);

        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        HookInstaller.install(lpparam.packageName, lpparam.processName, context, lpparam.classLoader);
                    }
                }
        );
    }
}

