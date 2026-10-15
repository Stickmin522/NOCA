#!/usr/bin/env bash
set -euo pipefail

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
sdk_root=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [[ -z "$sdk_root" ]]; then
    echo "Set ANDROID_HOME to your Android SDK directory." >&2
    exit 1
fi
case "$(uname -s)" in
    Linux) ndk_host=linux-x86_64 ;;
    Darwin) ndk_host=darwin-x86_64 ;;
    *) echo "Use Linux or macOS to build the Android native library." >&2; exit 1 ;;
esac
export ANDROID_HOME="$sdk_root"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER:-$sdk_root/ndk/28.2.13676358/toolchains/llvm/prebuilt/$ndk_host/bin/aarch64-linux-android29-clang}"
if [[ ! -x "$CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER" ]]; then
    echo "Install Android NDK 28.2.13676358." >&2
    exit 1
fi
cd "$project_root"
if [[ $# -eq 0 ]]; then set -- :app:assembleRelease; fi
exec ./gradlew "$@" --no-daemon --max-workers=2 --console=plain
