#!/usr/bin/env sh
set -eu

# 恢复 previous 软链接指向的上一版本；不回退数据库 Flyway 迁移。
PMTOOL_ROOT=${PMTOOL_ROOT:-/opt/pmtool}
CURRENT_LINK="$PMTOOL_ROOT/current"
PREVIOUS_LINK="$PMTOOL_ROOT/previous"

if [ ! -L "$CURRENT_LINK" ] || [ ! -L "$PREVIOUS_LINK" ]; then
  echo "没有可回滚的当前版本或上一版本" >&2
  exit 2
fi

current_target=$(readlink -f "$CURRENT_LINK")
previous_target=$(readlink -f "$PREVIOUS_LINK")
if [ ! -d "$current_target" ] || [ ! -d "$previous_target" ]; then
  echo "当前版本或上一版本目录不存在" >&2
  exit 2
fi

switch_link() {
  link=$1
  target=$2
  ln -s "$target" "$PMTOOL_ROOT/.link-next"
  mv -Tf "$PMTOOL_ROOT/.link-next" "$link"
}

switch_link "$CURRENT_LINK" "$previous_target"
switch_link "$PREVIOUS_LINK" "$current_target"
systemctl restart pmtool

if "$PMTOOL_ROOT/deploy/verify-deployment.sh"; then
  echo "已回滚到：$(basename "$previous_target")"
else
  echo "回滚后的健康检查失败，恢复原版本..." >&2
  switch_link "$CURRENT_LINK" "$current_target"
  switch_link "$PREVIOUS_LINK" "$previous_target"
  systemctl restart pmtool
  exit 1
fi
