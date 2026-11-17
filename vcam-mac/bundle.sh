#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Building vcam (Release)..."
swift build -c release

BIN_PATH="$(swift build -c release --show-bin-path)/vcam"
APP_DIR="build/vcam.app"
CONTENTS_DIR="$APP_DIR/Contents"

echo "==> Assembling standalone macOS application bundle at $APP_DIR..."
rm -rf "$APP_DIR"
mkdir -p "$CONTENTS_DIR"/{MacOS,Resources,Library/SystemExtensions}

cp "$BIN_PATH" "$CONTENTS_DIR/MacOS/vcam"
chmod +x "$CONTENTS_DIR/MacOS/vcam"

if [ -f "Resources/Info.plist" ]; then
    cp "Resources/Info.plist" "$CONTENTS_DIR/Info.plist"
fi

echo "==> Successfully created $APP_DIR!"
echo "You can launch it with: open $APP_DIR"
