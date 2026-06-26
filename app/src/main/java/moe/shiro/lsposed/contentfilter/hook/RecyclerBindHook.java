package moe.shiro.lsposed.contentfilter.hook;

import android.content.Context;
import android.view.View;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class RecyclerBindHook {
    private static final AtomicBoolean DEFERRED_HOOK_INSTALLED = new AtomicBoolean(false);
    private static final Set<String> INSTALLED_ADAPTERS = new HashSet<>();

    private RecyclerBindHook() {
    }

    static void install(
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget
    ) {
        installForClassLoader(classLoader, appContext, currentTarget, true);
        installDeferredClassLoadHook(appContext, currentTarget);
    }

    private static void installForClassLoader(
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget,
            boolean noisy
    ) {
        installForRecyclerView(
                "androidx.recyclerview.widget.RecyclerView",
                "androidx.recyclerview.widget.RecyclerView$Adapter",
                "androidx.recyclerview.widget.RecyclerView$ViewHolder",
                classLoader,
                appContext,
                currentTarget,
                noisy
        );
        installForRecyclerView(
                "android.support.v7.widget.RecyclerView",
                "android.support.v7.widget.RecyclerView$Adapter",
                "android.support.v7.widget.RecyclerView$ViewHolder",
                classLoader,
                appContext,
                currentTarget,
                noisy
        );
    }

    private static synchronized boolean installForRecyclerView(
            String recyclerViewClassName,
            String adapterClassName,
            String viewHolderClassName,
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget,
            boolean noisy
    ) {
        String installKey = System.identityHashCode(classLoader) + ":" + adapterClassName;
        if (INSTALLED_ADAPTERS.contains(installKey)) {
            return true;
        }
        try {
            Class<?> recyclerViewClass = Class.forName(recyclerViewClassName, false, classLoader);
            Class<?> viewHolderClass = resolveViewHolderClass(recyclerViewClass, viewHolderClassName, classLoader);
            XposedHelpers.findAndHookMethod(
                    adapterClassName,
                    classLoader,
                    "bindViewHolder",
                    viewHolderClass,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            View itemView = itemViewFromHolder(param.args[0]);
                            UiFilter.handleBoundItem(
                                    appContext,
                                    itemView,
                                    param.thisObject,
                                    param.args[0],
                                    currentTarget.packageName,
                                    currentTarget.processName,
                                    currentTarget.currentActivity
                            );
                        }
                    }
            );
            INSTALLED_ADAPTERS.add(installKey);
            XposedBridge.log("[LCF] RecyclerView bind hook installed: "
                    + adapterClassName + " holder=" + viewHolderClass.getName());
            installAdapterAttachHook(adapterClassName, recyclerViewClass, classLoader);
            return true;
        } catch (Throwable throwable) {
            if (noisy) {
                XposedBridge.log("[LCF] RecyclerView bind hook unavailable for " + adapterClassName + ": " + throwable);
            }
            return false;
        }
    }

    private static Class<?> resolveViewHolderClass(
            Class<?> recyclerViewClass,
            String viewHolderClassName,
            ClassLoader classLoader
    ) throws ClassNotFoundException {
        try {
            return Class.forName(viewHolderClassName, false, classLoader);
        } catch (ClassNotFoundException ignored) {
        }
        for (Class<?> innerClass : recyclerViewClass.getDeclaredClasses()) {
            if (hasItemViewField(innerClass)) {
                return innerClass;
            }
        }
        throw new ClassNotFoundException(viewHolderClassName);
    }

    private static boolean hasItemViewField(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField("itemView");
                return View.class.isAssignableFrom(field.getType());
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    private static void installDeferredClassLoadHook(
            Context appContext,
            CurrentTarget currentTarget
    ) {
        if (!DEFERRED_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(
                    ClassLoader.class,
                    "loadClass",
                    String.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof ClassLoader)
                                    || param.args == null
                                    || param.args.length == 0
                                    || !(param.args[0] instanceof String)) {
                                return;
                            }
                            String className = (String) param.args[0];
                            if (!isRecyclerClassName(className)) {
                                return;
                            }
                            try {
                                installForClassLoader(
                                        (ClassLoader) param.thisObject,
                                        appContext,
                                        currentTarget,
                                        false
                                );
                            } catch (Throwable throwable) {
                                XposedBridge.log("[LCF] deferred RecyclerView hook failed: " + throwable);
                            }
                        }
                    }
            );
            XposedBridge.log("[LCF] deferred RecyclerView class-load hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log("[LCF] deferred RecyclerView class-load hook unavailable: " + throwable);
        }
    }

    private static boolean isRecyclerClassName(String className) {
        return className != null
                && (className.startsWith("androidx.recyclerview.widget.RecyclerView")
                || className.startsWith("android.support.v7.widget.RecyclerView"));
    }

    private static void installAdapterAttachHook(
            String adapterClassName,
            Class<?> recyclerViewClass,
            ClassLoader classLoader
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                    adapterClassName,
                    classLoader,
                    "onAttachedToRecyclerView",
                    recyclerViewClass,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args.length > 0 && param.args[0] instanceof View) {
                                UiFilter.rememberRecyclerAdapter(param.thisObject, (View) param.args[0]);
                            }
                        }
                    }
            );
            XposedBridge.log("[LCF] RecyclerView adapter attach hook installed: " + adapterClassName);
        } catch (Throwable throwable) {
            XposedBridge.log("[LCF] RecyclerView adapter attach hook unavailable for "
                    + adapterClassName + ": " + throwable);
        }
    }

    private static View itemViewFromHolder(Object holder) {
        if (holder == null) {
            return null;
        }
        try {
            Field field = holder.getClass().getField("itemView");
            Object value = field.get(holder);
            return value instanceof View ? (View) value : null;
        } catch (Throwable ignored) {
        }
        Class<?> type = holder.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("itemView");
                field.setAccessible(true);
                Object value = field.get(holder);
                return value instanceof View ? (View) value : null;
            } catch (Throwable ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }
}
