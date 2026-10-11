#!/system/bin/sh
# post-fs-data.sh — 早期启动守护进程 (后台运行, 不阻塞开机)
# KO卸载 · Kazaf
MODDIR=${0%/*}
[ -f "$MODDIR/disable" ] && exit 0

L="$MODDIR/bin/launch_daemon.sh"
[ -x "$L" ] || exit 0

# 不用 nohup: setsid 已脱离会话; nohup 会把 SIGTERM 置为忽略导致守护无法优雅停止
( "$L" post-fs-data ) >/dev/null 2>&1 &
exit 0
