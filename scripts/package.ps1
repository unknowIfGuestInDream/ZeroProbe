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

$version = $env:APP_VERSION
if (-not $version) {
  $versionOutput = & mvn -q -DforceStdout 'help:evaluate' -Dexpression='project.version' 2>$null
  $version = $versionOutput.Trim()
  if (-not $version) { throw 'Failed to extract version from Maven' }
}
Add-Content -Path $env:GITHUB_ENV -Value "APP_VERSION=$version" -Encoding utf8

$stagingDir = 'staging'
if (Test-Path $stagingDir) { Remove-Item -Path $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir | Out-Null

Copy-Item -Path 'target\javafx-tool-template.jar' -Destination $stagingDir
Copy-Item -Path 'target\lib' -Destination $stagingDir -Recurse
Copy-Item -Path 'README.md' -Destination $stagingDir
Copy-Item -Path 'LICENSE' -Destination $stagingDir
Copy-Item -Path 'scripts\win\*' -Destination $stagingDir -Recurse

Push-Location staging
& ..\scripts\jre.ps1
Pop-Location
