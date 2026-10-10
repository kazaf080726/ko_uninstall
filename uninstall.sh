#!/system/bin/sh
# uninstall.sh — 卸载前停掉守护进程与本地服务, 清理痕迹
# KO卸载 · Kazaf
MODDIR=${0%/*}
DATA="$MODDIR/data"

_p=$(cat "$DATA/daemon.lock" 2>/dev/null)
case "$_p" in
    '' | *[!0-9]*) ;;
    *) kill -9 "$_p" 2>/dev/null ;;
esac

_s=$(cat "$DATA/server.pid" 2>/dev/null)
case "$_s" in
    '' | *[!0-9]*) ;;
    *) kill -9 "$_s" 2>/dev/null ;;
esac

# 清理隐藏脚本与历史遗留标记
rm -f /dev/.sh /data/local/.sh 2>/dev/null
rm -f /data/adb/.ddc_inte_killer_link /data/adb/.ddc_inte_killer_has_inte 2>/dev/null
exit 0
