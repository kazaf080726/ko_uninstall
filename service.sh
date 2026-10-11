#!/system/bin/sh
# service.sh — 系统启动完成后兜底: 守护进程若未起则再启一次
# KO卸载 · Kazaf
MODDIR=${0%/*}
[ -f "$MODDIR/disable" ] && exit 0

# service.sh 的 PATH 在不同管理器下不一致, getprop/am 都在 /system/bin
export PATH=/system/bin:/system/xbin:$PATH

L="$MODDIR/bin/launch_daemon.sh"
[ -x "$L" ] || exit 0

# 不用 nohup: setsid 已脱离会话; nohup 会把 SIGTERM 置为忽略导致守护无法优雅停止
( "$L" service ) >/dev/null 2>&1 &
exit 0
