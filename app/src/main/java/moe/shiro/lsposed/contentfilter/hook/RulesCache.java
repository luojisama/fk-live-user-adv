package moe.shiro.lsposed.contentfilter.hook;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;

import moe.shiro.lsposed.contentfilter.config.RuleKeys;
import moe.shiro.lsposed.contentfilter.config.RulesSnapshot;

import de.robv.android.xposed.XposedBridge;

final class RulesCache {
    private static final long CACHE_TTL_MS = 3000L;
    private static RulesSnapshot cached = RulesSnapshot.empty();
    private static long lastLoadedAt;
    private static boolean providerFailureLogged;

    private RulesCache() {
    }

    static void warmUp(Context context) {
        load(context, true);
    }

    static RulesSnapshot get(Context context) {
        return load(context, false);
    }

    private static synchronized RulesSnapshot load(Context context, boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastLoadedAt < CACHE_TTL_MS) {
            return cached;
        }
        lastLoadedAt = now;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse(RuleKeys.RULES_URI), null, null, null, null);
            cached = RulesSnapshot.fromCursor(cursor);
            providerFailureLogged = false;
        } catch (Throwable throwable) {
            if (!providerFailureLogged) {
                providerFailureLogged = true;
                XposedBridge.log("[LCF] failed to load rules from provider: " + throwable);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return cached;
    }
}

