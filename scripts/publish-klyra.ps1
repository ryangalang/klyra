param(
    [Parameter(Mandatory=$true)][string]$ReleaseDirectory,
    [string]$Version = '1.8-klyra.7',
    [switch]$Publish
)
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^\d+\.\d+-klyra\.\d+$') { throw 'Use a Klyra release version, for example 1.8-klyra.7.' }
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'Install GitHub CLI from https://cli.github.com, then run gh auth login.' }
$releaseRoot = (Resolve-Path -LiteralPath $ReleaseDirectory).Path
$names = @('Klyra-Android-arm64.apk','Klyra-Android-arm32.apk','Klyra-Android-universal.apk','Klyra-Android-x64-emulator.apk','Klyra-Windows-x64-setup.exe','Klyra-Windows-x64.zip','Klyra-corresponding-source.zip','SHA256SUMS.txt')
$checks = Get-Content -LiteralPath (Join-Path $releaseRoot 'SHA256SUMS.txt')
$files = foreach ($name in $names) {
    $path = Join-Path $releaseRoot $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing release asset: $name" }
    if ($name -ne 'SHA256SUMS.txt') {
        $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($checks -notcontains "$hash  $name") { throw "Checksum mismatch: $name" }
    }
    $path
}
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Sign in with gh auth login first.' }
$tag = "v$Version"
$notes = Join-Path $releaseRoot 'RELEASE_NOTES.md'
if (-not (Test-Path -LiteralPath $notes -PathType Leaf)) { throw 'Release notes are missing.' }
foreach ($document in @('LICENSE','KLYRA_BUILD.md','KLYRA_TEST_REPORT.md','RELEASE_NOTES.md')) {
    $documentPath = Join-Path $releaseRoot $document
    if (-not (Test-Path -LiteralPath $documentPath -PathType Leaf)) { throw "Missing release documentation: $document" }
    $files += $documentPath
}
& gh release create $tag @files --repo ryangalang/klyra --draft --title "Klyra $Version" --notes-file $notes
if ($LASTEXITCODE -ne 0) { throw 'Release creation failed. Existing releases are not overwritten.' }
if ($Publish) {
    & gh release edit $tag --repo ryangalang/klyra --draft=false --latest
    if ($LASTEXITCODE -ne 0) { throw 'Draft uploaded, but publishing failed. Review the release on GitHub.' }
} else {
    Write-Host 'Draft uploaded. Review it in GitHub Releases, then publish it to enable update notices.'
}
