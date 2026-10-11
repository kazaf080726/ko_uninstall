#!/system/bin/sh
# ============================================================================
# daemon.sh — KO卸载 守护进程 (v2.0.0)
# 作者: Kazaf
# 启动方式: 必须经 bin/launch_daemon.sh 启动 (进程隐藏)
# 正文标记 @@__KO_DAEMON_BODY__@@ 之上是标记行, 之下才是执行代码
# v2.0.0: 分段 sleep 即时响应配置变更 / 每 tick 写状态缓存 / monitor 运行时切换
#         12 维轻量检测缓存 / MAX_TRY 实时更新 / WebUI+APP 双端直读缓存提速
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
STF="$DATA/status"
mkdir -p "$DATA" 2>/dev/null

LOGMAX=262144
_UPT=$(awk '{print int($1)}' /proc/uptime 2>/dev/null)

log() {
    _ts=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null)
    case "$_ts" in
        19* | 20[01][0-9]-*)
            _ts="开机+${_UPT:-?}s" ;;
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
echo "$_UPT" >"$DATA/boot_uptime" 2>/dev/null
# 优雅退出: TERM/INT -> exit 0 -> 触发 EXIT trap 清理 (仅 trap 清理不 exit 会导致守护残留)
trap 'rm -f "$LOCK"; setstate stopped; log "exit" "守护进程退出"' EXIT
trap 'exit 0' INT TERM

# ---- 卸载全部目标 (逐模块, 记录结果) ---------------------------------------
unload_all() {
    _ok=0
    _tg=$(detected_kos)
    for _m in $_tg; do
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

# 轻量等待清除: 用 _loaded_fast 替代重量级 detected_kos
wait_clear() {
    _i=0
    while [ "$_i" -lt "$1" ]; do
        _loaded_fast || return 0
        sleep 1
        _i=$((_i + 1))
    done
    _loaded_fast && return 1
    return 0
}

# ---- 本地服务模式保活 (每 10 tick 检查一次) --------------------------------
ensure_server() {
    _want=$(cat "$DATA/server" 2>/dev/null)
    [ "$_want" = "1" ] || return 0
    [ -x "$MODDIR/bin/server.sh" ] || return 0
    sh "$MODDIR/bin/server.sh" status >/dev/null 2>&1 || sh "$MODDIR/bin/server.sh" start >/dev/null 2>&1
}

# ---- 读取用户配置 (实时读, 支持 WebUI 即时切换) ----------------------------
read_cfg() {
    mode=$(cat "$MODE_F" 2>/dev/null)
    case "$mode" in unload | monitor) ;; *) mode=unload ;; esac
    INTERVAL=$(cat "$DATA/interval" 2>/dev/null)
    case "$INTERVAL" in '' | *[!0-9]*) INTERVAL=5 ;; esac
    [ "$INTERVAL" -lt 1 ] && INTERVAL=1
    [ "$INTERVAL" -gt 3600 ] && INTERVAL=3600
    strength=$(ko_strength)
    MAX_TRY=5
    [ "$strength" = "1" ] && MAX_TRY=8
    if [ -f "$DATA/interval_usr" ]; then
        _iu=$(cat "$DATA/interval_usr" 2>/dev/null)
        case "$_iu" in '' | *[!0-9]*) _iu=5 ;; esac
        [ "$_iu" -lt 1 ] && _iu=1
        [ "$_iu" -gt 3600 ] && _iu=3600
        INTERVAL_USR="$_iu"
    else
        INTERVAL_USR="$INTERVAL"
    fi
}

# ---- 状态缓存 (每 tick 写, WebUI 轮询直接读, 大幅提速) ---------------------
write_cache() {
    _lk=0; _tg=""
    if _loaded_fast; then _lk=1; _tg=$(detected_kos); fi
    _st=$(cat "$STATE" 2>/dev/null); [ -z "$_st" ] && _st=unknown
    case "$mode" in
        monitor) _g="监控中·仅记录" ;;
        *) if [ "$_lk" = "1" ]; then _g="盯防中·已发现"; else _g="盯防中·${INTERVAL}s"; fi ;;
    esac
    _ns=$(inte_nodes)
    _up=$(awk '{print int($1)}' /proc/uptime 2>/dev/null)
    _f=$(inte_ko_file); _mf=${_f%%'|'*}
    if [ $((tick % 10)) -eq 0 ]; then KSYM=$(kallsyms_hit); fi
    {
        printf 'ts=%s\n' "$_up"
        printf 'pid=%s\n' "$$"
        printf 'state=%s\n' "$_st"
        printf 'mode=%s\n' "$mode"
        printf 'inte_loaded=%s\n' "$_lk"
        printf 'targets=%s\n' "$_tg"
        printf 'nodes=%s\n' "$_ns"
        printf 'ksym=%s\n' "$KSYM"
        printf 'interval=%s\n' "$INTERVAL"
        printf 'strength=%s\n' "$strength"
        printf 'server=%s\n' "$(cat "$DATA/server" 2>/dev/null)"
        printf 'stats=%s\n' "$(ko_stats 2>/dev/null)"
        printf 'guard=%s\n' "$_g"
        printf 'm_ko=%s\n' "$(maple_ko)"
        printf 'm_systbl=%s\n' "$(maple_systbl)"
        printf 'm_status=%s\n' "$(maple_status)"
        printf 'm_mod=%s\n' "$(maple_mod)"
        printf 'm_sysmod=%s\n' "$(maple_sysmod)"
        printf 'm_dev=%s\n' "$(inte_dev_nodes)"
        printf 'm_file=%s\n' "$_mf"
        printf 'm_cmd=%s\n' "$(cmdline_hit)"
        printf 'm_ref=%s\n' "$(inte_refcnt)"
        printf 'm_init=%s\n' "$(inte_initstate)"
    } >"$STF.tmp" 2>/dev/null && mv "$STF.tmp" "$STF" 2>/dev/null
}

# ---- 卸载/复活检测循环 (unload 模式) ---------------------------------------
unload_cycle() {
    if _loaded_fast; then
        stats_add revive 2>/dev/null
        battle=$((battle + 1))
        log "recheck" "检测到 inte 重新出现:$(detected_kos), 立即卸载 (复活 #$battle)"
        rm -f "$DATA/builtin_"* 2>/dev/null
        attempt=0
        while [ "$attempt" -lt "$MAX_TRY" ]; do
            attempt=$((attempt + 1))
            unload_all >/dev/null
            wait_clear 5 && break
        done
        if ! _loaded_fast; then
            setstate normal
            stats_add unload 2>/dev/null
            log "recheck" "已重新清除 (第 ${attempt} 次)"
            # 激进模式: 复活后自适应收紧间隔 (写回 interval, 供缓存/UI 展示)
            if [ "$strength" = "1" ]; then
                if [ "$battle" -ge 3 ]; then
                    INTERVAL=1
                    log "recheck" "复活频繁, 自适应间隔收紧为 1s"
                elif [ "$INTERVAL" -gt 2 ]; then
                    INTERVAL=2
                    log "recheck" "自适应间隔收紧为 2s"
                fi
                echo "$INTERVAL" >"$DATA/interval" 2>/dev/null
            fi
        else
            setstate failed
            log "recheck" "重新清除失败:$(detected_kos)"
        fi
    else
        # 未发现复活: 恢复用户设定
        if [ "$battle" -gt 0 ]; then
            battle=0
            INTERVAL="$INTERVAL_USR"
            echo "$INTERVAL" >"$DATA/interval" 2>/dev/null
            log "recheck" "inte 已稳定, 恢复用户设定间隔 ${INTERVAL}s"
        fi
        _cur=$(cat "$STATE" 2>/dev/null)
        case "$_cur" in
            normal | stopped | notfound | waiting) ;;
            *) setstate normal ;;
        esac
    fi
}

# ---- 首轮卸载 -------------------------------------------------------------
perform_unload() {
    setstate running
    _attempt=0
    while [ "$_attempt" -lt "$1" ]; do
        _attempt=$((_attempt + 1))
        _loaded_fast || break
        unload_all >/dev/null
        if wait_clear 10; then
            stats_add unload 2>/dev/null
            log "unload" "inte 已清除 (第 $_attempt 次尝试)"
            break
        fi
        log "unload" "第 $_attempt 次尝试后仍存在:$(detected_kos)"
        [ "$_attempt" -lt "$1" ] && sleep 1
    done
    if ! _loaded_fast; then
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
            log "check" "$1 次尝试后仍未清除:$(detected_kos)"
        fi
    fi
}

# ---- 启动 -------------------------------------------------------------
log "boot" "===== KO卸载 守护启动 ($1) ====="log "boot" "机型=$(dev_model) 品牌=$(dev_brand) 内核=$(dev_kernel)"
log "boot" "root=$(root_manager) rmmod=$(find_rmmod)"
read_cfg
log "boot" "模式=$mode 强度=$( [ "$strength" = "1" ] && echo 激进 || echo 标准 )"
log "boot" "复核间隔=${INTERVAL}s (用户设定=${INTERVAL_USR}s) 卸载尝试上限=${MAX_TRY}"

# 开机自保护: boot_completed=1 即确认开机完成, 清零启动计数 (防旧砖误禁用)
_bg_clear() {
    [ -x "$MODDIR/bin/boot_guard.sh" ] || return 0
    if [ "$(getprop sys.boot_completed 2>/dev/null)" = "1" ]; then
        "$MODDIR/bin/boot_guard.sh" clear >/dev/null 2>&1
    fi
}
_bg_clear

if ! inte_active; then
    setstate notfound
    log "check" "未检测到 inte 子系统, 守护进程仍运行以防后续加载"
fi

tick=0
battle=0
KSYM=$(kallsyms_hit)

if [ "$mode" = "monitor" ]; then
    setstate running
    log "monitor" "监控模式: 只记录状态, 不做卸载"
else
    perform_unload "$MAX_TRY"
fi

# ---- 主循环 (每 tick 重读配置, 分段 sleep 保证配置变更 1s 内生效) ----------
while true; do
    read_cfg
    tick=$((tick + 1))
    write_cache
    _bg_clear

    if [ "$mode" = "monitor" ]; then
        _k=$(detected_kos)
        [ -n "$_k" ] && log "monitor" "inte 处于加载状态:$_k (节点 $(inte_nodes)/3, maple=$(maple_ko)/$(maple_systbl)/$(maple_status))"
    else
        unload_cycle
    fi

    # 本地服务保活
    if [ $((tick % 10)) -eq 0 ]; then
        ensure_server
    fi

    _hb=$((300 / INTERVAL))
    [ "$_hb" -lt 1 ] && _hb=1
    if [ $((tick % _hb)) -eq 0 ]; then
        log "alive" "守护存活 (复核 ${tick} 次, 间隔 ${INTERVAL}s, 复活累计 $(tr ',' '\n' <"$DATA/stats" 2>/dev/null | sed -n 's/^revive=//p' | head -1))"
    fi

    # 分段 sleep: 每 1s 醒来重读 interval/mode/strength, 任一配置变更 1s 内生效
    _sl=0
    while [ "$_sl" -lt "$INTERVAL" ]; do
        sleep 1
        _sl=$((_sl + 1))
        _iv2=$(cat "$DATA/interval" 2>/dev/null)
        _md2=$(cat "$MODE_F" 2>/dev/null)
        _st2=$(ko_strength)
        case "$_iv2" in
            '' | *[!0-9]*) ;;
            *) [ "$_iv2" -ne "$INTERVAL" ] && break ;;
        esac
        case "$_md2" in
            unload | monitor) [ "$_md2" != "$mode" ] && break ;;
        esac
        [ "$_st2" != "$strength" ] && break
    done
done
