#!/system/bin/sh
# ============================================================================
# server.sh — KO卸载 本地服务模式
# 用 busybox httpd 把 webroot 暴露到 127.0.0.1:<port> (默认 8080),
# 让用户在任意浏览器打开即可管理 (无需进 Root 管理器 WebUI)
# 用法: sh bin/server.sh start|stop|status [port]
# ============================================================================
export PATH=/system/bin:/system/xbin:/vendor/bin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH
MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR=${0%/*}/..
DATA="$MODDIR/data"
. "$MODDIR/bin/common.sh" 2>/dev/null

ACTION="$1"
PORT="$2"
case "$PORT" in '' | *[!0-9]*) PORT=8080 ;; esac
[ "$PORT" -lt 1024 ] && PORT=8080
[ "$PORT" -gt 65535 ] && PORT=8080

PIDF="$DATA/server.pid"
LOGF="$DATA/server.log"
H="$(find_httpd)"
[ -n "$H" ] || { echo "no-httpd"; exit 1; }

_pid_alive() {
    [ -f "$PIDF" ] || return 1
    _p=$(cat "$PIDF" 2>/dev/null)
    case "$_p" in '' | *[!0-9]*) return 1 ;; esac
    kill -0 "$_p" 2>/dev/null || return 1
    # 校验是 httpd 进程, 防止误杀
    _c=$(tr '\0' ' ' <"/proc/$_p/cmdline" 2>/dev/null)
    case "$_c" in
        *httpd*) return 0 ;;
    esac
    return 1
}

case "$ACTION" in
    start)
        _pid_alive && { echo "running"; exit 0; }
        rm -f "$PIDF" 2>/dev/null
        nohup setsid $H -f -p "127.0.0.1:$PORT" -h "$MODDIR/webroot" >"$LOGF" 2>&1 &
        echo $! >"$PIDF"
        sleep 1
        _pid_alive && { echo "started:$PORT"; echo 1 >"$DATA/server"; exit 0; }
        rm -f "$PIDF" 2>/dev/null
        echo "failed"
        exit 1 ;;
    stop)
        if _pid_alive; then
            kill -9 "$(cat "$PIDF" 2>/dev/null)" 2>/dev/null
        fi
        rm -f "$PIDF" 2>/dev/null
        echo 0 >"$DATA/server"
        echo "stopped"
        exit 0 ;;
    status)
        if _pid_alive; then
            echo "running:$PORT"
            exit 0
        fi
        echo "stopped"
        exit 1 ;;
    *)
        echo "usage: server.sh start|stop|status [port]"
        exit 2 ;;
esac
