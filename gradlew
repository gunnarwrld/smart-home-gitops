#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Smart Home GitOps — Gradle Wrapper Script
# Delegates to the cached Gradle 9.4.1 installation.
#
# If gradle-wrapper.jar is not present, this script falls back to the
# locally cached Gradle distribution.

set -e

GRADLE_DIST="$HOME/.gradle/wrapper/dists/gradle-9.4.1-bin/b5e198eb3220290bedd3cfe5f/gradle-9.4.1"

if [ -d "$GRADLE_DIST" ]; then
    exec "$GRADLE_DIST/bin/gradle" "$@"
else
    echo "ERROR: Gradle 9.4.1 not found at $GRADLE_DIST"
    echo "Please run: curl -L https://services.gradle.org/distributions/gradle-9.4.1-bin.zip -o /tmp/gradle.zip && unzip /tmp/gradle.zip -d $HOME/.gradle/wrapper/dists/gradle-9.4.1-bin/b5e198eb3220290bedd3cfe5f/"
    exit 1
fi
