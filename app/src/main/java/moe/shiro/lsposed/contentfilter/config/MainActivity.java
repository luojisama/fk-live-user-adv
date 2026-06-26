package moe.shiro.lsposed.contentfilter.config;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String RELEASES_PAGE_URL = "https://github.com/luojisama/fk-live-user-adv/releases";
    private static final String LATEST_RELEASE_API_URL = "https://api.github.com/repos/luojisama/fk-live-user-adv/releases/latest";
    private static final int HTTP_TIMEOUT_MS = 8000;
    private static final int COLOR_BG = Color.rgb(243, 247, 247);
    private static final int COLOR_CARD = Color.rgb(255, 255, 255);
    private static final int COLOR_TEXT = Color.rgb(24, 32, 38);
    private static final int COLOR_SUBTEXT = Color.rgb(91, 104, 111);
    private static final int COLOR_ACCENT = Color.rgb(0, 86, 91);
    private static final int COLOR_ACCENT_LIGHT = Color.rgb(230, 246, 246);
    private static final int COLOR_BORDER = Color.rgb(219, 228, 228);
    private static final int COLOR_CHIP = Color.rgb(239, 247, 247);
    private static final int COLOR_CHIP_TEXT = Color.rgb(36, 45, 52);

    private final List<Button> tabButtons = new ArrayList<>();

    private LinearLayout root;
    private LinearLayout pageHost;
    private String selectedPackage = RuleKeys.TARGET_PACKAGES[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Shiro 屏蔽助手");
        String targetPackage = getIntent() == null ? null : getIntent().getStringExtra("target_package");
        if (isTargetPackage(targetPackage)) {
            selectedPackage = targetPackage;
        }
        buildUi();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(createHeaderCard(), matchWrap());
        root.addView(createAppTabs(), matchWrap());
        root.addView(createUpdateCard(), matchWrapWithBottomMargin());

        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageHost, matchWrap());

        setContentView(scrollView);
        showPackagePage(selectedPackage);
    }

    private View createHeaderCard() {
        LinearLayout card = createCard();

        TextView title = new TextView(this);
        title.setText("Shiro 屏蔽助手");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        card.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("分应用配置，直播与广告独立开关，关键词按类目保存");
        subtitle.setTextSize(14);
        subtitle.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(6);
        card.addView(subtitle, subtitleParams);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams badgeParams = matchWrap();
        badgeParams.topMargin = dp(12);
        card.addView(badgeRow, badgeParams);

        badgeRow.addView(createBadge("抖音", COLOR_ACCENT_LIGHT, COLOR_ACCENT), wrapContent());
        badgeRow.addView(space(dp(8)), new LinearLayout.LayoutParams(dp(8), 1));
        badgeRow.addView(createBadge("哔哩哔哩", COLOR_ACCENT_LIGHT, COLOR_ACCENT), wrapContent());
        badgeRow.addView(space(dp(8)), new LinearLayout.LayoutParams(dp(8), 1));
        badgeRow.addView(createBadge("bilibili", COLOR_ACCENT_LIGHT, COLOR_ACCENT), wrapContent());

        return card;
    }

    private View createAppTabs() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = matchWrap();
        scrollParams.topMargin = dp(4);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(6));
        scrollView.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < RuleKeys.TARGET_PACKAGES.length; i++) {
            final String packageName = RuleKeys.TARGET_PACKAGES[i];
            Button tab = new Button(this);
            tab.setAllCaps(false);
            tab.setText(RuleKeys.TARGET_LABELS[i]);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setTextSize(14);
            tab.setMinWidth(dp(88));
            tab.setPadding(dp(16), dp(10), dp(16), dp(10));
            tab.setOnClickListener(v -> {
                selectedPackage = packageName;
                refreshTabStyles();
                showPackagePage(packageName);
            });
            tabButtons.add(tab);
            row.addView(tab, wrapContentWithRightMargin(i == RuleKeys.TARGET_PACKAGES.length - 1 ? 0 : dp(10)));
        }

        refreshTabStyles();
        return scrollView;
    }

    private void showPackagePage(String packageName) {
        pageHost.removeAllViews();

        LinearLayout summaryCard = createCard();
        TextView title = new TextView(this);
        title.setText(appLabel(packageName));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        summaryCard.addView(title, matchWrap());

        TextView packageNameText = new TextView(this);
        packageNameText.setText(packageName);
        packageNameText.setTextSize(12);
        packageNameText.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams packageParams = matchWrap();
        packageParams.topMargin = dp(4);
        summaryCard.addView(packageNameText, packageParams);

        TextView tip = new TextView(this);
        tip.setText("当前页面只管理这个应用的屏蔽项。直播、广告、用户、话题、分区都分开保存。");
        tip.setTextSize(13);
        tip.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams tipParams = matchWrap();
        tipParams.topMargin = dp(10);
        summaryCard.addView(tip, tipParams);

        pageHost.addView(summaryCard, matchWrapWithBottomMargin());

        LinearLayout switchesCard = createCard();
        addCardTitle(switchesCard, "应用开关");
        addSwitchRow(switchesCard, "启用此应用", RuleKeys.appEnabledKey(packageName), true, null);
        addSwitchRow(switchesCard, "屏蔽直播推送", RuleKeys.liveEnabledKey(packageName), true,
                "命中直播相关文本时直接折叠该应用内的推送卡片");
        addSwitchRow(switchesCard, "写入 Xposed 日志", RuleKeys.DEBUG_LOG, false,
                "排查命中与误伤时再打开");
        pageHost.addView(switchesCard, matchWrapWithBottomMargin());

        pageHost.addView(createFeatureCard(
                packageName,
                "广告屏蔽",
                "屏蔽推广、赞助、抖音商城、购物和下载类广告卡片",
                RuleKeys.adEnabledKey(packageName),
                RuleKeys.adKeywordsKey(packageName),
                false
        ), matchWrapWithBottomMargin());

        pageHost.addView(createFeatureCard(
                packageName,
                "用户屏蔽",
                "屏蔽用户名中包含指定关键词的作品",
                RuleKeys.userEnabledKey(packageName),
                RuleKeys.userKeywordsKey(packageName),
                true
        ), matchWrapWithBottomMargin());

        pageHost.addView(createFeatureCard(
                packageName,
                "话题屏蔽",
                "屏蔽标题、话题、标签中的指定关键词",
                RuleKeys.topicEnabledKey(packageName),
                RuleKeys.topicKeywordsKey(packageName),
                false
        ), matchWrapWithBottomMargin());

        pageHost.addView(createFeatureCard(
                packageName,
                "分区屏蔽",
                "屏蔽分区、栏目、频道中的指定关键词",
                RuleKeys.sectionEnabledKey(packageName),
                RuleKeys.sectionKeywordsKey(packageName),
                false
        ), matchWrapWithBottomMargin());

        LinearLayout footerCard = createCard();
        TextView footer = new TextView(this);
        footer.setText("关键词可重复添加，保存后立即生效。");
        footer.setTextSize(13);
        footer.setTextColor(COLOR_SUBTEXT);
        footerCard.addView(footer, matchWrap());
        pageHost.addView(footerCard, matchWrapWithBottomMargin());
    }

    private View createUpdateCard() {
        LinearLayout card = createCard();
        addCardTitle(card, "版本与更新");

        TextView summary = new TextView(this);
        summary.setText("当前版本 " + currentVersionName() + "，从 GitHub Releases 获取更新。");
        summary.setTextSize(13);
        summary.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(6);
        card.addView(summary, summaryParams);

        TextView status = new TextView(this);
        status.setText("未检查更新");
        status.setTextSize(12);
        status.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(8);
        card.addView(status, statusParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = matchWrap();
        actionsParams.topMargin = dp(10);
        card.addView(actions, actionsParams);

        Button check = new Button(this);
        check.setAllCaps(false);
        check.setText("检查更新");
        check.setTextColor(Color.WHITE);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setBackground(makeFilledButtonBackground(COLOR_ACCENT));
        actions.addView(check, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button releases = new Button(this);
        releases.setAllCaps(false);
        releases.setText("发布页");
        releases.setTextColor(COLOR_ACCENT);
        releases.setTypeface(Typeface.DEFAULT_BOLD);
        releases.setBackground(makePillStrokeBackground());
        LinearLayout.LayoutParams releasesParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        releasesParams.leftMargin = dp(10);
        actions.addView(releases, releasesParams);

        check.setOnClickListener(v -> checkForUpdates(check, status));
        releases.setOnClickListener(v -> openUrl(RELEASES_PAGE_URL));
        return card;
    }

    private void checkForUpdates(Button checkButton, TextView statusView) {
        checkButton.setEnabled(false);
        checkButton.setText("检查中");
        statusView.setText("正在连接 GitHub Releases...");

        new Thread(() -> {
            try {
                UpdateInfo update = fetchLatestRelease();
                String current = currentVersionName();
                boolean hasUpdate = compareVersions(update.tagName, current) > 0;
                runOnUiThread(() -> {
                    checkButton.setEnabled(true);
                    checkButton.setText("检查更新");
                    if (hasUpdate) {
                        statusView.setText("发现新版本 " + update.tagName);
                        showUpdateDialog(update, current);
                    } else {
                        statusView.setText("当前已是最新版本：" + current);
                        Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Throwable throwable) {
                runOnUiThread(() -> {
                    checkButton.setEnabled(true);
                    checkButton.setText("检查更新");
                    statusView.setText("检查失败：" + readableError(throwable));
                    Toast.makeText(this, "检查更新失败", Toast.LENGTH_SHORT).show();
                });
            }
        }, "shiro-update-check").start();
    }

    private UpdateInfo fetchLatestRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_API_URL).openConnection();
        connection.setConnectTimeout(HTTP_TIMEOUT_MS);
        connection.setReadTimeout(HTTP_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "shiro-content-filter/" + currentVersionName());

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readAll(stream);
        connection.disconnect();

        if (code == 404) {
            throw new IllegalStateException("暂无发布版本");
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("GitHub 返回 " + code);
        }

        JSONObject json = new JSONObject(body);
        String tagName = json.optString("tag_name", "");
        String htmlUrl = json.optString("html_url", RELEASES_PAGE_URL);
        String apkUrl = "";
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) {
                    continue;
                }
                String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
                if (name.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "");
                    break;
                }
            }
        }
        if (tagName.isEmpty()) {
            throw new IllegalStateException("未找到 Release tag");
        }
        return new UpdateInfo(tagName, htmlUrl, apkUrl);
    }

    private void showUpdateDialog(UpdateInfo update, String currentVersion) {
        String targetUrl = update.downloadUrl();
        new AlertDialog.Builder(this)
                .setTitle("发现新版本")
                .setMessage("当前版本：" + currentVersion + "\n最新版本：" + update.tagName)
                .setPositiveButton(update.apkUrl.isEmpty() ? "打开发布页" : "下载更新",
                        (dialog, which) -> openUrl(targetUrl))
                .setNegativeButton("稍后", null)
                .show();
    }

    private LinearLayout createFeatureCard(
            String packageName,
            String title,
            String summary,
            String enabledKey,
            String keywordsKey,
            boolean isUserCategory
    ) {
        LinearLayout card = createCard();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        card.addView(header, matchWrap());

        TextView featureTitle = new TextView(this);
        featureTitle.setText(title);
        featureTitle.setTextSize(17);
        featureTitle.setTypeface(Typeface.DEFAULT_BOLD);
        featureTitle.setTextColor(COLOR_TEXT);
        header.addView(featureTitle, matchWrap());

        TextView featureSummary = new TextView(this);
        featureSummary.setText(summary);
        featureSummary.setTextSize(13);
        featureSummary.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(4);
        header.addView(featureSummary, summaryParams);

        LinearLayout controlBlock = new LinearLayout(this);
        controlBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams controlParams = matchWrap();
        controlParams.topMargin = dp(10);
        card.addView(controlBlock, controlParams);

        addSwitchRow(controlBlock, "启用 " + title, enabledKey, true, null);
        addKeywordManager(controlBlock, packageName, keywordsKey, isUserCategory);
        return card;
    }

    private void addKeywordManager(LinearLayout parent, String packageName, String key, boolean isUserCategory) {
        TextView label = new TextView(this);
        label.setText("关键词");
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams labelParams = matchWrap();
        labelParams.topMargin = dp(10);
        parent.addView(label, labelParams);

        TextView hint = new TextView(this);
        hint.setText(isUserCategory ? "输入用户名片段后保存，可多次追加。" : "输入关键词后保存，可多次追加。");
        hint.setTextSize(12);
        hint.setTextColor(COLOR_SUBTEXT);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.topMargin = dp(2);
        parent.addView(hint, hintParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams inputRowParams = matchWrap();
        inputRowParams.topMargin = dp(8);
        parent.addView(inputRow, inputRowParams);

        EditText input = new EditText(this);
        input.setHint("输入关键词");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(14);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(Color.rgb(141, 154, 159));
        input.setBackground(makeFieldBackground());
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputRow.addView(input, inputParams);

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setText("保存");
        save.setTextColor(Color.WHITE);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setBackground(makeFilledButtonBackground(COLOR_ACCENT));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.leftMargin = dp(10);
        saveParams.width = dp(74);
        inputRow.addView(save, saveParams);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(10);
        parent.addView(list, listParams);

        Runnable refresh = () -> renderKeywordRows(list, key);
        save.setOnClickListener(v -> {
            String value = RulesSnapshot.normalize(input.getText());
            if (value.isEmpty()) {
                return;
            }
            List<String> values = new ArrayList<>(RulesSnapshot.parseLines(prefs().getString(key, "")));
            if (!values.contains(value)) {
                values.add(value);
                prefs().edit()
                        .putString(key, RulesSnapshot.joinLines(values))
                        .putLong(RuleKeys.UPDATED_AT, System.currentTimeMillis())
                        .apply();
                notifyRulesChanged();
            }
            input.setText("");
            refresh.run();
        });
        input.setOnEditorActionListener((textView, actionId, event) -> {
            save.performClick();
            return true;
        });

        refresh.run();
    }

    private void renderKeywordRows(LinearLayout list, String key) {
        list.removeAllViews();
        List<String> values = RulesSnapshot.parseLines(prefs().getString(key, ""));
        if (values.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无关键词");
            empty.setTextSize(12);
            empty.setTextColor(COLOR_SUBTEXT);
            empty.setPadding(dp(2), dp(4), dp(2), dp(4));
            list.addView(empty, matchWrap());
            return;
        }
        for (String value : values) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.topMargin = dp(6);
            list.addView(row, rowParams);

            TextView chip = new TextView(this);
            chip.setText(value);
            chip.setTextSize(13);
            chip.setTextColor(COLOR_CHIP_TEXT);
            chip.setMaxLines(1);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(makeChipBackground());
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(chip, chipParams);

            Button delete = new Button(this);
            delete.setAllCaps(false);
            delete.setText("移除");
            delete.setTypeface(Typeface.DEFAULT_BOLD);
            delete.setTextColor(COLOR_ACCENT);
            delete.setBackground(makePillStrokeBackground());
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            deleteParams.leftMargin = dp(8);
            row.addView(delete, deleteParams);

            delete.setOnClickListener(v -> {
                List<String> next = new ArrayList<>(RulesSnapshot.parseLines(prefs().getString(key, "")));
                next.remove(value);
                prefs().edit()
                        .putString(key, RulesSnapshot.joinLines(next))
                        .putLong(RuleKeys.UPDATED_AT, System.currentTimeMillis())
                        .apply();
                notifyRulesChanged();
                renderKeywordRows(list, key);
            });
        }
    }

    private void addSwitchRow(LinearLayout parent, String label, String key, boolean defaultValue, String summary) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(makeInnerSurface());
        LinearLayout.LayoutParams rowParams = matchWrap();
        rowParams.topMargin = dp(10);
        parent.addView(row, rowParams);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(line, matchWrap());

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(15);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        line.addView(text, textParams);

        Switch switchView = new Switch(this);
        switchView.setChecked(prefs().getBoolean(key, defaultValue));
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs().edit()
                    .putBoolean(key, isChecked)
                    .putLong(RuleKeys.UPDATED_AT, System.currentTimeMillis())
                    .apply();
            notifyRulesChanged();
        });
        line.addView(switchView, wrapContent());

        if (summary != null && !summary.isEmpty()) {
            TextView tip = new TextView(this);
            tip.setText(summary);
            tip.setTextSize(12);
            tip.setTextColor(COLOR_SUBTEXT);
            LinearLayout.LayoutParams tipParams = matchWrap();
            tipParams.topMargin = dp(6);
            row.addView(tip, tipParams);
        }
    }

    private void refreshTabStyles() {
        for (int i = 0; i < tabButtons.size(); i++) {
            Button tab = tabButtons.get(i);
            boolean selected = RuleKeys.TARGET_PACKAGES[i].equals(selectedPackage);
            tab.setTextColor(selected ? Color.WHITE : COLOR_TEXT);
            tab.setBackground(selected
                    ? makeFilledButtonBackground(COLOR_ACCENT)
                    : makeOutlinedButtonBackground());
        }
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(makeCardBackground());
        return card;
    }

    private View createBadge(String text, int backgroundColor, int textColor) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(12);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(textColor);
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));
        badge.setBackground(makeBadgeBackground(backgroundColor));
        return badge;
    }

    private void addCardTitle(LinearLayout card, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        card.addView(title, matchWrap());
    }

    private View space(int widthPx) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(widthPx, 1));
        return view;
    }

    private GradientDrawable makeCardBackground() {
        return roundedDrawable(COLOR_CARD, COLOR_BORDER, dp(14), dp(1));
    }

    private GradientDrawable makeInnerSurface() {
        return roundedDrawable(Color.rgb(250, 252, 252), Color.rgb(232, 238, 238), dp(12), dp(1));
    }

    private GradientDrawable makeBadgeBackground(int backgroundColor) {
        return roundedDrawable(backgroundColor, Color.TRANSPARENT, dp(999), 0);
    }

    private GradientDrawable makeFieldBackground() {
        return roundedDrawable(Color.WHITE, COLOR_BORDER, dp(12), dp(1));
    }

    private GradientDrawable makeChipBackground() {
        return roundedDrawable(COLOR_CHIP, Color.TRANSPARENT, dp(999), 0);
    }

    private GradientDrawable makePillStrokeBackground() {
        return roundedDrawable(Color.WHITE, COLOR_ACCENT, dp(999), dp(1));
    }

    private GradientDrawable makeFilledButtonBackground(int color) {
        return roundedDrawable(color, Color.TRANSPARENT, dp(12), 0);
    }

    private GradientDrawable makeOutlinedButtonBackground() {
        return roundedDrawable(Color.WHITE, COLOR_BORDER, dp(12), dp(1));
    }

    private GradientDrawable roundedDrawable(int fillColor, int strokeColor, int radiusPx, int strokePx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            drawable.setStroke(strokePx, strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWrapWithBottomMargin() {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(14);
        return params;
    }

    private LinearLayout.LayoutParams wrapContent() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapContentWithRightMargin(int rightMarginPx) {
        LinearLayout.LayoutParams params = wrapContent();
        params.rightMargin = rightMarginPx;
        return params;
    }

    private void notifyRulesChanged() {
        getContentResolver().notifyChange(Uri.parse(RuleKeys.RULES_URI), null);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    private String currentVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName == null ? "0.0.0" : packageInfo.versionName;
        } catch (Throwable ignored) {
            return "0.0.0";
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            reader.close();
        }
        return builder.toString();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = cleanVersion(left).split("\\.");
        String[] rightParts = cleanVersion(right).split("\\.");
        int count = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < count; i++) {
            int leftValue = i < leftParts.length ? leadingNumber(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? leadingNumber(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return leftValue > rightValue ? 1 : -1;
            }
        }
        return 0;
    }

    private static String cleanVersion(String value) {
        if (value == null) {
            return "0";
        }
        String version = value.trim().toLowerCase(Locale.ROOT);
        if (version.startsWith("v")) {
            version = version.substring(1);
        }
        int suffix = version.indexOf('-');
        return suffix >= 0 ? version.substring(0, suffix) : version;
    }

    private static int leadingNumber(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(0, end));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String readableError(Throwable throwable) {
        String message = throwable == null ? "" : throwable.getMessage();
        return message == null || message.isEmpty() ? "网络不可用" : message;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(RuleKeys.PREFS_NAME, MODE_PRIVATE);
    }

    private boolean isTargetPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String target : RuleKeys.TARGET_PACKAGES) {
            if (target.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private String appLabel(String packageName) {
        for (int i = 0; i < RuleKeys.TARGET_PACKAGES.length; i++) {
            if (RuleKeys.TARGET_PACKAGES[i].equals(packageName)) {
                return RuleKeys.TARGET_LABELS[i];
            }
        }
        return packageName;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class UpdateInfo {
        final String tagName;
        final String htmlUrl;
        final String apkUrl;

        UpdateInfo(String tagName, String htmlUrl, String apkUrl) {
            this.tagName = tagName;
            this.htmlUrl = htmlUrl == null || htmlUrl.isEmpty() ? RELEASES_PAGE_URL : htmlUrl;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
        }

        String downloadUrl() {
            return apkUrl.isEmpty() ? htmlUrl : apkUrl;
        }
    }
}
