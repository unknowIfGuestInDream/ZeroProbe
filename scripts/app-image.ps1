#
# Copyright (c) 2026 unknowIfGuestInDream.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#     * Neither the name of unknowIfGuestInDream, any associated website, nor the
# names of its contributors may be used to endorse or promote products
# derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL UNKNOWIFGUESTINDREAM BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

$ErrorActionPreference = 'Stop'

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
  throw 'jpackage not found on PATH. Ensure JDK 21 is installed and JAVA_HOME/bin is on PATH.'
}

$version = $env:APP_VERSION
if (-not $version) {
  $versionOutput = & mvn -q -DforceStdout 'help:evaluate' -Dexpression='project.version' 2>$null
  $version = $versionOutput.Trim()
  if (-not $version) { throw 'APP_VERSION not set and failed to extract version from Maven' }
}
if ($env:GITHUB_ENV) {
  Add-Content -Path $env:GITHUB_ENV -Value "APP_VERSION=$version" -Encoding utf8
}

$jarPath = 'target\javafx-tool-template.jar'
if (-not (Test-Path $jarPath)) { throw 'JAR not found at target\javafx-tool-template.jar' }

$jpackageInput = 'jpackage-input'
if (Test-Path $jpackageInput) { Remove-Item -Path $jpackageInput -Recurse -Force }
New-Item -ItemType Directory -Path $jpackageInput | Out-Null
Copy-Item -Path $jarPath -Destination $jpackageInput
Copy-Item -Path 'target\lib' -Destination $jpackageInput -Recurse

if (-not (Test-Path 'dist')) { New-Item -ItemType Directory -Path 'dist' | Out-Null }

$appImageDir = 'app-image-out'
if (Test-Path $appImageDir) { Remove-Item -Path $appImageDir -Recurse -Force }

$jpackageArgs = @(
  '--input', $jpackageInput,
  '--name', 'javafx-tool-template',
  '--main-jar', 'javafx-tool-template.jar',
  '--main-class', 'com.tlcsdm.fxtemplate.Launcher',
  '--type', 'app-image',
  '--java-options', '-Xms64m',
  '--java-options', '-Xmx256m',
  '--java-options', '-Dfile.encoding=UTF-8',
  '--add-modules', 'java.se,jdk.unsupported,jdk.zipfs,jdk.management,jdk.crypto.ec,jdk.localedata,jdk.charsets',
  '--jlink-options', '--strip-debug --no-man-pages --no-header-files --compress zip-6',
  '--app-version', $version,
  '--vendor', 'Tlcsdm',
  '--description', 'JavaFX tool template application',
  '--dest', $appImageDir
)

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

$appDir = Get-ChildItem -Path $appImageDir -Directory | Select-Object -First 1
if ($null -eq $appDir) { throw 'No output directory found after jpackage' }

$zipName = "javafx-tool-template-app-image-windows-$version.zip"
$zipPath = Join-Path 'dist' $zipName
if (Test-Path $zipPath) { Remove-Item -Path $zipPath -Force }

Compress-Archive -Path "$($appDir.FullName)\*" -DestinationPath $zipPath -Force

Remove-Item -Path $jpackageInput -Recurse -Force
Remove-Item -Path $appImageDir -Recurse -Force
