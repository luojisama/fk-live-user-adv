param(
    [string] $SdkDir = "",
    [string] $Platform = "android-36.1",
    [string] $BuildTools = "36.1.0"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($SdkDir)) {
    $SdkDir = Join-Path $env:LOCALAPPDATA "Android\sdk"
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$app = Join-Path $root "app"
$stubs = Join-Path $root "xposed-stubs"
$build = Join-Path $root "build\manual"
$generated = Join-Path $build "generated"
$resCompiled = Join-Path $build "compiled-res"
$stubClasses = Join-Path $build "stub-classes"
$appClasses = Join-Path $build "app-classes"
$stubJar = Join-Path $build "xposed-stubs.jar"
$appJar = Join-Path $build "app-classes.jar"
$dexDir = Join-Path $build "dex"
$unsignedApk = Join-Path $build "content-filter-unsigned.apk"
$alignedApk = Join-Path $build "content-filter-aligned.apk"
$finalApk = Join-Path $build "content-filter-debug.apk"
$manifestIn = Join-Path $app "src\main\AndroidManifest.xml"
$manifestOut = Join-Path $build "AndroidManifest.xml"
$gradleProperties = Join-Path $root "gradle.properties"

$appVersionCode = "1"
$appVersionName = "0.1.0"
if (Test-Path -LiteralPath $gradleProperties) {
    foreach ($line in Get-Content -LiteralPath $gradleProperties) {
        if ($line -match '^appVersionCode=(.+)$') {
            $appVersionCode = $Matches[1].Trim()
        } elseif ($line -match '^appVersionName=(.+)$') {
            $appVersionName = $Matches[1].Trim()
        }
    }
}

$androidJar = Join-Path $SdkDir "platforms\$Platform\android.jar"
$tools = Join-Path $SdkDir "build-tools\$BuildTools"
$aapt2 = Join-Path $tools "aapt2.exe"
$zipalign = Join-Path $tools "zipalign.exe"
$d8 = Join-Path $tools "lib\d8.jar"
$apksigner = Join-Path $tools "lib\apksigner.jar"

foreach ($path in @($androidJar, $aapt2, $zipalign, $d8, $apksigner)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required Android SDK file not found: $path"
    }
}

Remove-Item -LiteralPath $build -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $generated, $resCompiled, $stubClasses, $appClasses, $dexDir | Out-Null

$manifestText = (Get-Content -LiteralPath $manifestIn -Raw).
    Replace('${applicationId}', 'moe.shiro.lsposed.contentfilter').
    Replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="moe.shiro.lsposed.contentfilter" android:versionCode="' + $appVersionCode + '" android:versionName="' + $appVersionName + '">')
$manifestText | Set-Content -LiteralPath $manifestOut -Encoding UTF8

& $aapt2 compile --dir (Join-Path $app "src\main\res") -o (Join-Path $resCompiled "resources.zip")
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

& $aapt2 link `
    -o $unsignedApk `
    -I $androidJar `
    --manifest $manifestOut `
    -R (Join-Path $resCompiled "resources.zip") `
    --java $generated `
    -A (Join-Path $app "src\main\assets") `
    --min-sdk-version 23 `
    --target-sdk-version 35 `
    --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

$stubSources = Get-ChildItem -LiteralPath (Join-Path $stubs "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -source 17 -target 17 -classpath $androidJar -d $stubClasses @stubSources
if ($LASTEXITCODE -ne 0) { throw "javac stubs failed" }
Push-Location $stubClasses
try {
    & jar cf $stubJar .
    if ($LASTEXITCODE -ne 0) { throw "jar stubs failed" }
} finally {
    Pop-Location
}

$appSources = @()
$appSources += Get-ChildItem -LiteralPath (Join-Path $app "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$appSources += Get-ChildItem -LiteralPath $generated -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$classpath = "$androidJar;$stubJar"
& javac -encoding UTF-8 -source 17 -target 17 -classpath $classpath -d $appClasses @appSources
if ($LASTEXITCODE -ne 0) { throw "javac app failed" }
Push-Location $appClasses
try {
    & jar cf $appJar .
    if ($LASTEXITCODE -ne 0) { throw "jar app failed" }
} finally {
    Pop-Location
}

& java -cp $d8 com.android.tools.r8.D8 `
    --min-api 23 `
    --classpath $androidJar `
    --classpath $stubJar `
    --output $dexDir `
    $appJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Push-Location $dexDir
try {
    & jar uf $unsignedApk classes.dex
    if ($LASTEXITCODE -ne 0) { throw "jar update failed" }
} finally {
    Pop-Location
}

& $zipalign -p -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
if (-not (Test-Path -LiteralPath $debugKeystore)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $debugKeystore) | Out-Null
    & keytool -genkeypair `
        -keystore $debugKeystore `
        -storepass android `
        -keypass android `
        -alias androiddebugkey `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US"
    if ($LASTEXITCODE -ne 0) { throw "debug keystore generation failed" }
}

& java -jar $apksigner sign `
    --ks $debugKeystore `
    --ks-pass pass:android `
    --key-pass pass:android `
    --out $finalApk `
    $alignedApk
if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed" }

& java -jar $apksigner verify --verbose $finalApk
if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed" }

Write-Output "APK: $finalApk"

