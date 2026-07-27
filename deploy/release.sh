#!/usr/bin/env sh
set -eu

# 原子发布脚本。由 GitHub Actions 或管理员在服务器上执行。
# 参数 1：包含 backend/、frontend/、deploy/ 的发布包；参数 2：可选发布版本号。

PACKAGE=${1:?"用法: release.sh <release-package.tar.gz> [release-id]"}
RELEASE_ID=${2:-"$(date -u +%Y%m%d%H%M%S)"}
PMTOOL_ROOT=${PMTOOL_ROOT:-/opt/pmtool}
PMTOOL_USER=${PMTOOL_USER:-pmtool}
PMTOOL_SERVICE=${PMTOOL_SERVICE:-pmtool}
RELEASES_DIR="$PMTOOL_ROOT/releases"
CURRENT_LINK="$PMTOOL_ROOT/current"
PREVIOUS_LINK="$PMTOOL_ROOT/previous"
STAGE_DIR="$RELEASES_DIR/.staging-$RELEASE_ID-$$"
RELEASE_DIR="$RELEASES_DIR/$RELEASE_ID"

case "$RELEASE_ID" in
  *[!A-Za-z0-9._-]*|'') echo "发布版本号只能包含字母、数字、点、下划线和连字符" >&2; exit 2 ;;
esac

if [ ! -f "$PACKAGE" ]; then
  echo "找不到发布包：$PACKAGE" >&2
  exit 2
fi
if [ -e "$RELEASE_DIR" ]; then
  echo "发布版本已存在：$RELEASE_ID" >&2
  exit 2
fi

cleanup() { rm -rf "$STAGE_DIR"; }
trap cleanup EXIT INT TERM

install -d -m 0755 "$RELEASES_DIR" "$PMTOOL_ROOT/deploy"
mkdir "$STAGE_DIR"
tar -xzf "$PACKAGE" -C "$STAGE_DIR"

if [ ! -f "$STAGE_DIR/backend/pmtool.jar" ] || [ ! -f "$STAGE_DIR/frontend/index.html" ]; then
  echo "发布包缺少 backend/pmtool.jar 或 frontend/index.html" >&2
  exit 2
fi

# 将随发布包提供的运维脚本同步到固定位置，供后续人工回滚和验证使用。
for script in release.sh rollback.sh verify-deployment.sh; do
  if [ -f "$STAGE_DIR/deploy/$script" ]; then
    install -m 0755 "$STAGE_DIR/deploy/$script" "$PMTOOL_ROOT/deploy/$script"
  fi
done

mv "$STAGE_DIR" "$RELEASE_DIR"
trap - EXIT INT TERM

if id "$PMTOOL_USER" >/dev/null 2>&1; then
  chown -R "$PMTOOL_USER:$PMTOOL_USER" "$RELEASE_DIR"
fi

previous_target=""
if [ -L "$CURRENT_LINK" ]; then
  previous_target=$(readlink -f "$CURRENT_LINK" || true)
fi

switch_link() {
  target=$1
  ln -s "$target" "$PMTOOL_ROOT/.current-next"
  mv -Tf "$PMTOOL_ROOT/.current-next" "$CURRENT_LINK"
}

if [ -n "$previous_target" ] && [ -d "$previous_target" ]; then
  ln -s "$previous_target" "$PMTOOL_ROOT/.previous-next"
  mv -Tf "$PMTOOL_ROOT/.previous-next" "$PREVIOUS_LINK"
fi
switch_link "$RELEASE_DIR"

echo "正在重启 PMTool 服务：$RELEASE_ID"
systemctl restart "$PMTOOL_SERVICE"

if PMTOOL_SERVICE="$PMTOOL_SERVICE" "$PMTOOL_ROOT/deploy/verify-deployment.sh"; then
  echo "PMTool 发布成功：$RELEASE_ID"
else
  echo "新版本健康检查失败，开始恢复上一版本..." >&2
  if [ -n "$previous_target" ] && [ -d "$previous_target" ]; then
    switch_link "$previous_target"
    systemctl restart "$PMTOOL_SERVICE"
    PMTOOL_SERVICE="$PMTOOL_SERVICE" "$PMTOOL_ROOT/deploy/verify-deployment.sh" || true
    echo "已恢复到：$(basename "$previous_target")" >&2
  else
    echo "没有可恢复的上一版本；请检查日志：journalctl -u $PMTOOL_SERVICE -n 200" >&2
  fi
  exit 1
fi

# 保留当前及最近四个版本，便于回滚；不删除 current / previous 软链接指向的目录。
kept=0
for release in $(ls -1dt "$RELEASES_DIR"/* 2>/dev/null || true); do
  kept=$((kept + 1))
  if [ "$kept" -gt 5 ] && [ "$release" != "$previous_target" ]; then
    rm -rf "$release"
  fi
done
