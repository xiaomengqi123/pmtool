#!/usr/bin/env sh
set -eu

# 打包已构建的后端 JAR 与前端 dist，供 release.sh 或 GitHub Actions 使用。
OUTPUT=${1:-pmtool-release.tar.gz}
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BACKEND_JAR="$ROOT_DIR/backend/target/pmtool-1.0.0.jar"
FRONTEND_DIST="$ROOT_DIR/frontend/dist"
STAGE_DIR=$(mktemp -d "${TMPDIR:-/tmp}/pmtool-package.XXXXXX")

cleanup() { rm -rf "$STAGE_DIR"; }
trap cleanup EXIT INT TERM

if [ ! -f "$BACKEND_JAR" ] || [ ! -f "$FRONTEND_DIST/index.html" ]; then
  echo "请先完成后端 mvn package 和前端 npm run build" >&2
  exit 2
fi

mkdir -p "$STAGE_DIR/backend" "$STAGE_DIR/frontend" "$STAGE_DIR/deploy"
cp "$BACKEND_JAR" "$STAGE_DIR/backend/pmtool.jar"
cp -R "$FRONTEND_DIST/." "$STAGE_DIR/frontend/"
cp "$ROOT_DIR/deploy/release.sh" "$ROOT_DIR/deploy/rollback.sh" "$ROOT_DIR/deploy/verify-deployment.sh" "$STAGE_DIR/deploy/"
tar -czf "$OUTPUT" -C "$STAGE_DIR" backend frontend deploy
echo "发布包已生成：$OUTPUT"
