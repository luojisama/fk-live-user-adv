param(
    [string] $OutDir = "signing",
    [string] $KeystoreName = "fk-live-user-adv-release.keystore",
    [string] $Alias = "fk-live-user-adv",
    [int] $ValidityDays = 10000,
    [switch] $Force
)

$ErrorActionPreference = "Stop"

function Convert-SecureStringToPlainText {
    param([Security.SecureString] $Value)
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outputDir = Join-Path $root $OutDir
$keystorePath = Join-Path $outputDir $KeystoreName
$base64Path = "$keystorePath.base64.txt"

if ((Test-Path -LiteralPath $keystorePath) -and -not $Force) {
    throw "Keystore already exists: $keystorePath. Use -Force only if you intentionally want to replace it."
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$storePasswordSecure = Read-Host "Keystore password" -AsSecureString
$keyPasswordSecure = Read-Host "Key password" -AsSecureString
$storePassword = Convert-SecureStringToPlainText $storePasswordSecure
$keyPassword = Convert-SecureStringToPlainText $keyPasswordSecure

try {
    & keytool -genkeypair `
        -keystore $keystorePath `
        -storepass $storePassword `
        -keypass $keyPassword `
        -alias $Alias `
        -keyalg RSA `
        -keysize 4096 `
        -validity $ValidityDays `
        -dname "CN=Shiro LSPosed Content Filter,O=Shiro,C=CN"
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed"
    }

    [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath)) |
        Set-Content -LiteralPath $base64Path -NoNewline -Encoding ASCII

    Write-Output ""
    Write-Output "Created release keystore:"
    Write-Output "  $keystorePath"
    Write-Output "Created GitHub secret payload file:"
    Write-Output "  $base64Path"
    Write-Output ""
    Write-Output "Configure these GitHub Actions secrets:"
    Write-Output "  ANDROID_KEYSTORE_BASE64 = contents of $base64Path"
    Write-Output "  ANDROID_KEYSTORE_PASSWORD = the keystore password you entered"
    Write-Output "  ANDROID_KEY_ALIAS = $Alias"
    Write-Output "  ANDROID_KEY_PASSWORD = the key password you entered"
    Write-Output ""
    Write-Output "Do not commit files under $outputDir. They are ignored by .gitignore."
} finally {
    if ($storePassword) {
        $storePassword = $null
    }
    if ($keyPassword) {
        $keyPassword = $null
    }
}
