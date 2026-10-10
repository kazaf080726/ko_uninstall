#!/system/bin/sh
# ============================================================================
# daemon.sh — KO卸载 守护进程
# 作者: Kazaf
# 启动方式: 必须经 bin/launch_daemon.sh 启动 (进程隐藏)
# 正文标记 @@__KO_DAEMON_BODY__@@ 之上是标记行, 之下才是执行代码
# ============================================================================
@@__KO_DAEMON_BODY__@@
# ---- 正文开始 ------------------------------------------------------------
if [ -n "$KO_MODDIR" ] && [ -d "$KO_MODDIR" ]; then
    MODDIR="$KO_MODDIR"
else
    MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
    [ -n "$MODDIR" ] || MODDIR=${0%/*}/..
fi
DATA="$MODDIR/data"
. "$MODDIR/bin/common.sh" 2>/dev/null
[ -z "$BB" ] && BB=busybox

STATE="$DATA/state"
MODE_F="$DATA/mode"
LOG="$DATA/daemon.log"
LOCK="$DATA/daemon.lock"
PIDF="$DATA/daemon.pid"
mkdir -p "$DATA" 2>/dev/null

LOGMAX=262144

log() {
    _ts=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null)
    case "$_ts" in
        19* | 20[01][0-9]-*)
            _up=$(awk '{print int($1)}' /proc/uptime 2>/dev/null)
            _ts="开机+${_up:-?}s" ;;
    esac
    echo "$_ts [$1] $2" >>"$LOG" 2>/dev/null
    _sz=$(wc -c <"$LOG" 2>/dev/null || echo 0)
    if [ "$_sz" -gt "$LOGMAX" ]; then
        tail -c 65536 "$LOG" >"$LOG.tmp" 2>/dev/null && mv "$LOG.tmp" "$LOG"
    fi
}
setstate() { echo "$1" >"$STATE" 2>/dev/null; }

# ---- 单实例锁 ------------------------------------------------------------
if [ -f "$LOCK" ]; then
    _ol=$(cat "$LOCK" 2>/dev/null)
    if [ -n "$_ol" ] && kill -0 "$_ol" 2>/dev/null && is_own_daemon "$_ol"; then
        log "init" "已有守护进程运行 (pid=$_ol), 本次($1)退出"
        exit 0
    fi
fi
echo $$ >"$LOCK"
echo $$ >"$PIDF"
awk '{print int($1)}' /proc/uptime >"$DATA/boot_uptime" 2>/dev/null
trap 'rm -f "$LOCK"; setstate stopped; log "exit" "守护进程退出"' EXIT INT TERM

# ---- 卸载全部目标 (逐模块, 记录结果) ---------------------------------------
unload_all() {
    _ok=0
    for _m in $(detected_kos); do
        _out=$(do_rmmod "$_m")
        _rc=$?
        log "unload" "rmmod $_m => ${_out:-（无输出, 成功）}"
        case "$_out" in
            *builtin* | *"is built in"* | *"built into"*)
                log "unload" "$_m 为 builtin, 无法卸载"
                echo builtin >"$DATA/builtin_$_m" 2>/dev/null
                continue ;;
            protected | no-rmmod)
                log "unload" "$_m 被跳过 ($_out)"
                continue ;;
        esac
        if [ $_rc -eq 0 ] || ! ko_loaded "$_m"; then
            _ok=$((_ok + 1))
        fi
    done
    printf '%s' "$_ok"
}

wait_clear() {
    _i=0
    while [ "$_i" -lt "$1" ]; do
        [ -z "$(detected_kos)" ] && return 0
        sleep 1
        _i=$((_i + 1))
    done
    [ -z "$(detected_kos)" ]
}

_loaded_fast() {
    while IFS= read -r _l; do
        for _c in $INTE_CANDIDATES; do
            case " $_l " in
                *" $_c "*) return 0 ;;
            esac
        done
    done < /proc/modules 2>/dev/null
    return 1
}

# ---- 本地服务模式保活 (每 10 tick 检查一次) --------------------------------
ensure_server() {
    _want=$(cat "$DATA/server" 2>/dev/null)
    [ "$_want" = "1" ] || return 0
    [ -x "$MODDIR/bin/server.sh" ] || return 0
    sh "$MODDIR/bin/server.sh" status >/dev/null 2>&1 || sh "$MODDIR/bin/server.sh" start >/dev/null 2>&1
}

log "boot" "===== KO卸载 守护启动 ($1) ====="
log "boot" "机型=$(dev_model) 品牌=$(dev_brand) 内核=$(dev_kernel)"
log "boot" "root=$(root_manager) rmmod=$(find_rmmod)"

mode=$(cat "$MODE_F" 2>/dev/null)
case "$mode" in unload | monitor) ;; *) mode=unload ;; esac
log "boot" "模式=$mode"

setstate running

if ! inte_active; then
    setstate notfound
    log "check" "未检测到 inte 子系统, 守护进程仍运行以防后续加载"
fi

if [ "$mode" = "monitor" ]; then
    setstate running
    log "monitor" "监控模式: 只记录状态, 不做卸载"
    while true; do
        _k=$(detected_kos)
        [ -n "$_k" ] && log "monitor" "inte 处于加载状态:$_k (节点 $(inte_nodes)/3, maple=$(maple_ko)/$(maple_systbl)/$(maple_status))"
        sleep 60
    done
fi

perform_unload() {
    setstate running
    _attempt=0
    while [ "$_attempt" -lt 5 ]; do
        _attempt=$((_attempt + 1))
        [ -z "$(detected_kos)" ] && break
        unload_all >/dev/null
        if wait_clear 15; then
            log "unload" "inte 已清除 (第 $_attempt 次尝试)"
            break
        fi
        log "unload" "第 $_attempt 次尝试后仍存在:$(detected_kos)"
        [ "$_attempt" -lt 5 ] && sleep 2
    done
    if [ -z "$(detected_kos)" ]; then
        setstate normal
        log "check" "已清除. 剩余 /proc 节点: $(inte_nodes)/3 (maple=$(maple_ko)/$(maple_systbl)/$(maple_status), ksym=$(kallsyms_hit))"
    else
        _bi=0
        for _m in $(detected_kos); do
            [ -f "$DATA/builtin_$_m" ] && _bi=1
        done
        if [ "$_bi" = "1" ]; then
            setstate builtin
            log "check" "存在 builtin 模块, 无法卸载"
        else
            setstate failed
            log "check" "5 次尝试后仍未清除:$(detected_kos)"
        fi
    fi
}

INTERVAL=$(cat "$DATA/interval" 2>/dev/null)
case "$INTERVAL" in
    '' | *[!0-9]*) INTERVAL=5 ;;
esac
[ "$INTERVAL" -lt 1 ] && INTERVAL=1
[ "$INTERVAL" -gt 3600 ] && INTERVAL=3600
echo "$INTERVAL" > "$DATA/interval" 2>/dev/null
log "boot" "复核间隔=${INTERVAL}s"

# 首次卸载
perform_unload

# 复核循环
tick=0
while true; do
    sleep "$INTERVAL"
    tick=$((tick + 1))

    if _loaded_fast; then
        log "recheck" "检测到 inte 重新出现:$(detected_kos), 立即卸载"
        rm -f "$DATA/builtin_"* 2>/dev/null
        attempt=0
        while [ $attempt -lt 5 ]; do
            attempt=$((attempt + 1))
            unload_all >/dev/null
            wait_clear 5 && break
        done
        if [ -z "$(detected_kos)" ]; then
            setstate normal
            log "recheck" "已重新清除 (第 ${attempt} 次)"
        else
            setstate failed
            log "recheck" "重新清除失败:$(detected_kos)"
        fi
    else
        _cur=$(cat "$STATE" 2>/dev/null)
        case "$_cur" in
            normal | stopped | notfound | waiting) ;;
            *) setstate normal ;;
        esac
    fi

    # 本地服务保活
    if [ $((tick % 10)) -eq 0 ]; then
        ensure_server
    fi

    _hb=$((300 / INTERVAL))
    [ "$_hb" -lt 1 ] && _hb=1
    if [ $((tick % _hb)) -eq 0 ]; then
        log "alive" "守护存活 (复核 ${tick} 次, 间隔 ${INTERVAL}s)"
    fi
done
