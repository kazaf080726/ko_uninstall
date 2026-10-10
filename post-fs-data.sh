#!/system/bin/sh
# post-fs-data.sh — 早期启动守护进程 (后台运行, 不阻塞开机)
# KO卸载 · Kazaf
MODDIR=${0%/*}
[ -f "$MODDIR/disable" ] && exit 0

L="$MODDIR/bin/launch_daemon.sh"
[ -x "$L" ] || exit 0

if command -v nohup >/dev/null 2>&1; then
    nohup "$L" post-fs-data >/dev/null 2>&1 &
else
    ( "$L" post-fs-data ) >/dev/null 2>&1 &
fi
exit 0
