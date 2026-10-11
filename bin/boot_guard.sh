#!/system/bin/sh
# ============================================================
# boot_guard.sh — 开机自保护 (旧砖模式)
# 连续 3 次启动未确认开机完成 -> 自动禁用本模块, 防止无限重启
# 用法: boot_guard.sh check | clear | show
# 环境变量可覆盖 (用于隔离测试):
#   KO_DATA   = 数据目录  (默认 /data/adb/modules/ko_uninstall/data)
#   KO_MODDIR = 模块目录  (默认 /data/adb/modules/ko_uninstall)
# 作者: Kazaf
# ============================================================
KO_MODDIR=${KO_MODDIR:-/data/adb/modules/ko_uninstall}
KO_DATA=${KO_DATA:-$KO_MODDIR/data}
MAX_BOOT=3
BOOT_LOG=$KO_DATA/boot.log

log_boot() {
    _ts=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null)
    echo "$_ts [$1] $2" >>"$BOOT_LOG" 2>/dev/null
}

case "$1" in
    check)
        # post-fs-data 阶段调用: 启动计数 +1, 未达上限则放行, 达上限则禁用
        N=$(cat "$KO_DATA/boot_count" 2>/dev/null)
        case "$N" in
            '' | *[!0-9]*) N=0 ;;
        esac
        N=$((N + 1))
        echo "$N" >"$KO_DATA/boot_count" 2>/dev/null
        U=$(awk '{print int($1)}' /proc/uptime 2>/dev/null)
        if [ "$N" -ge "$MAX_BOOT" ]; then
            touch "$KO_MODDIR/disable" 2>/dev/null
            log_boot "DISABLE" "连续 ${N} 次启动未完成开机, 已自动禁用本模块 (uptime=${U}s)"
            echo "disable|$N"
        else
            log_boot "boot" "第 ${N} 次启动 (uptime=${U}s), 等待开机完成确认"
            echo "ok|$N"
        fi
        ;;
    clear)
        # 守护在 boot_completed=1 时调用: 开机成功确认, 清零启动计数 (已为 0 则静默)
        N=$(cat "$KO_DATA/boot_count" 2>/dev/null)
        case "$N" in
            '' | *[!0-9]*) N=0 ;;
        esac
        if [ "$N" -ne 0 ]; then
            echo 0 >"$KO_DATA/boot_count" 2>/dev/null
            log_boot "CLEAR" "开机完成确认 (boot_completed=1), 启动计数已清零"
            echo "cleared"
        else
            echo "noop"
        fi
        ;;
    show)
        cat "$KO_DATA/boot_count" 2>/dev/null
        echo
        ;;
    *) echo "usage: boot_guard.sh check|clear|show"; exit 1 ;;
esac
exit 0
