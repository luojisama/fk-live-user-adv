# LSPosed Content Filter

面向 LSPosed 的内容屏蔽模块，当前 scope 覆盖本机连接设备上确认存在的三个包：

| 应用 | 包名 | 当前设备版本 |
| --- | --- | --- |
| 抖音 | `com.ss.android.ugc.aweme` | `37.8.0` |
| 哔哩哔哩 | `tv.danmaku.bili` | `8.88.0` |
| bilibili | `com.bilibili.app.in` | `3.20.4` |

## 功能

- 模块包名：`moe.shiro.lsposed.contentfilter`。
- 配置页为分应用页面：顶部切换抖音 / 哔哩哔哩 / bilibili，当前页面只显示当前应用配置。
- 每个应用独立配置：启用应用、屏蔽直播推送、屏蔽广告、屏蔽用户、屏蔽话题、屏蔽分区。
- 广告屏蔽默认启用，内置抖音商城、购物、推广、赞助、下载类广告关键词，并支持按应用补充关键词。
- 关键词通过输入框 + 保存按钮多次追加，支持删除。
- 配置页内置“版本与更新”，从 `luojisama/fk-live-user-adv` 的 GitHub Releases 检查新版本。
- 在目标 App 设置页添加“Shiro 屏蔽助手”入口：
  - 抖音：`DouYinSettingNewVersionActivity` 根布局注入。
  - B 站 / bilibili：`BiliPreferencesActivity$BiliPreferencesFragment` 反射追加 Preference。
- 可打开 Xposed 日志，观察命中原因、目标包、进程、Activity 和容器类名。

## 实现边界

当前采用 legacy Xposed + UI 层增强路径：

1. `assets/xposed_init` 注册 `moe.shiro.lsposed.contentfilter.hook.ModuleEntry`。
2. LSPosed scope 限定 `com.ss.android.ugc.aweme`、`tv.danmaku.bili`、`com.bilibili.app.in`。
3. 在目标进程内 hook `Application.attach` 后安装 Activity、TextView、RecyclerView bind 和设置页入口 hook。
4. 主屏蔽路径 hook `androidx.recyclerview.widget.RecyclerView$Adapter.bindViewHolder`，并尝试兼容 `android.support.v7.widget.RecyclerView$Adapter`。
5. 绑定完成后扫描整张卡片的 `TextView` / contentDescription / tag 文本，以及 View 类名、资源名中的直播和广告特征。
6. 命中用户、直播、广告、分区或话题规则时折叠该卡片。
7. `TextView.setText` 兜底只折叠 RecyclerView 子项；不会再沿 ViewPager 折叠整页，并跳过 Drawer/Menu/Popup/设置类容器，避免抖音三横杠菜单误伤黑屏。
8. 配置 Provider 虽然导出，但只允许模块自身 UID 和三个目标包 UID 读取规则。

这条路径不是数据加载前过滤。如果目标 App 改为自绘文本、Compose/Canvas、非 RecyclerView 容器，或希望完全不预加载被屏蔽视频，需要继续对对应版本做数据层 hook。

## 构建

优先使用 Gradle：

```powershell
.\gradlew.bat :app:assembleDebug
```

如果 Gradle wrapper 下载不可用，可用 Android SDK 本地工具构建：

```powershell
.\scripts\manual-build.ps1
```

本机已验证的 fallback 依赖：

- Android SDK: `$env:LOCALAPPDATA\Android\sdk`
- Platform: `platforms\android-36.1`
- Build tools: `build-tools\36.1.0`

输出 APK：

```text
build\manual\content-filter-debug.apk
```

## 更新与发布

模块内“版本与更新”会请求：

```text
https://api.github.com/repos/luojisama/fk-live-user-adv/releases/latest
```

发现高于当前 `versionName` 的 tag 后，会优先打开 Release 里的 `.apk` 资产下载链接；如果 Release 没有 APK，则打开发布页。

GitHub Actions 配置在 `.github/workflows/build-release.yml`：

- 推送到 `main` / `master` 或创建 PR 时自动构建 APK，并上传 workflow artifact。
- 推送 `v*` tag 时自动构建并创建/更新同名 GitHub Release。
- 也可以在 Actions 页面手动运行 workflow，并勾选 `publish_release`，此时会使用 `gradle.properties` 里的 `appVersionName` 生成 `v<versionName>` release tag。

Release APK 必须使用稳定签名，否则用户从旧版本更新到新版本时可能因为签名不同而无法覆盖安装。当前 workflow 允许分支 / PR 构建 fallback 到 debug 签名，但发布 Release（推送 `v*` tag 或手动勾选 `publish_release`）时会强制要求以下 GitHub Secrets：

| Secret | 说明 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | release keystore 文件的 base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key 密码 |

本地可用脚本生成 keystore 和 base64 secret 内容：

```powershell
.\scripts\create-release-keystore.ps1
```

脚本会在 `signing\` 下生成 keystore 和 `.base64.txt`。这些文件已被 `.gitignore` 排除，不要提交到仓库。把 `.base64.txt` 的内容配置到 `ANDROID_KEYSTORE_BASE64`，并把脚本中输入的密码和 alias 配置到对应 secrets。

## 安装与启用

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\build\manual\content-filter-debug.apk
```

安装后：

1. 在 LSPosed 中启用模块。
2. 勾选 scope：抖音、哔哩哔哩、bilibili。
3. 打开模块 App，切换到对应应用页面，配置开关和关键词。
4. 强行停止目标 App 后重新打开。

查看 hook 日志：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat | Select-String "\[LCF\]"
```
