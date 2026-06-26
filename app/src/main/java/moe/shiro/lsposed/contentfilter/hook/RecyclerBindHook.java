package moe.shiro.lsposed.contentfilter.hook;

import android.content.Context;
import android.view.View;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class RecyclerBindHook {
    private RecyclerBindHook() {
    }

    static void install(
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget
    ) {
        installForRecyclerView(
                "androidx.recyclerview.widget.RecyclerView",
                "androidx.recyclerview.widget.RecyclerView$Adapter",
                "androidx.recyclerview.widget.RecyclerView$ViewHolder",
                classLoader,
                appContext,
                currentTarget
        );
        installForRecyclerView(
                "android.support.v7.widget.RecyclerView",
                "android.support.v7.widget.RecyclerView$Adapter",
                "android.support.v7.widget.RecyclerView$ViewHolder",
                classLoader,
                appContext,
                currentTarget
        );
    }

    private static void installForRecyclerView(
            String recyclerViewClassName,
            String adapterClassName,
            String viewHolderClassName,
            ClassLoader classLoader,
            Context appContext,
            CurrentTarget currentTarget
    ) {
        try {
            Class<?> recyclerViewClass = Class.forName(recyclerViewClassName, false, classLoader);
            Class<?> viewHolderClass = Class.forName(viewHolderClassName, false, classLoader);
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
            XposedBridge.log("[LCF] RecyclerView bind hook installed: " + adapterClassName);
            installAdapterAttachHook(adapterClassName, recyclerViewClass, classLoader);
        } catch (Throwable throwable) {
            XposedBridge.log("[LCF] RecyclerView bind hook unavailable for " + adapterClassName + ": " + throwable);
        }
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
