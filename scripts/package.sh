#!/bin/bash

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

set -eu

platform="${1:-linux}"
if [ "$platform" != "linux" ] && [ "$platform" != "mac" ]; then
  echo "Unsupported platform: $platform" >&2
  exit 1
fi

version="${APP_VERSION:-}"
if [ -z "$version" ]; then
  version=$(mvn -q -DforceStdout 'help:evaluate' -Dexpression=project.version 2>/dev/null)
  version=$(echo "$version" | tr -d '\r')
  if [ -z "$version" ]; then
    echo "Failed to extract version from Maven" >&2
    exit 1
  fi
fi

if [ -n "${GITHUB_ENV:-}" ]; then
  echo "APP_VERSION=$version" >> "$GITHUB_ENV"
fi

rm -rf staging
mkdir -p staging
cp target/javafx-tool-template.jar staging/
cp -r target/lib staging/
cp README.md LICENSE staging/
cp "scripts/${platform}"/* staging/

cd staging
if [ "$platform" = "linux" ]; then
  bash ../scripts/jre.sh
else
  bash ../scripts/jre_mac.sh
fi
