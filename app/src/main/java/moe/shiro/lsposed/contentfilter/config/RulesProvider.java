package moe.shiro.lsposed.contentfilter.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class RulesProvider extends ContentProvider {
    private static final Set<String> ALLOWED_CALLERS =
            new HashSet<>(Arrays.asList(RuleKeys.TARGET_PACKAGES));
    private static final String[] COLUMNS = {
            RuleKeys.RULES_JSON
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        enforceCallerAllowed();
        SharedPreferences prefs = requirePrefs();
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        cursor.addRow(new Object[]{
                RulesSnapshot.fromPreferences(prefs).toJson()
        });
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.moe.shiro.lsposed.contentfilter.rules";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private SharedPreferences requirePrefs() {
        if (getContext() == null) {
            throw new IllegalStateException("RulesProvider has no context");
        }
        return getContext().getSharedPreferences(RuleKeys.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void enforceCallerAllowed() {
        if (getContext() == null) {
            throw new SecurityException("RulesProvider has no context");
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            return;
        }
        String[] packages = getContext().getPackageManager().getPackagesForUid(callingUid);
        if (packages != null) {
            for (String packageName : packages) {
                if (ALLOWED_CALLERS.contains(packageName)) {
                    return;
                }
            }
        }
        throw new SecurityException("Caller is not allowed to read content filter rules");
    }
}

