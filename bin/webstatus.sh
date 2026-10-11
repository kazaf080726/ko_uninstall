#!/system/bin/sh
# ============================================================================
# webstatus.sh — KO卸载 WebUI 单行状态协议 (v2.0.0)
# KernelSU 3.3.0+ 的 window.ksu.exec() 只返回 stdout 最后一行
# 所有字段用 | 分隔, 一次性输出一行
# v2.0.0: 守护运行且缓存新鲜时直接读 data/status (轮询提速, 消除 WebUI 卡顿)
#         守护未运行 -> 回退全量实时检测
# ============================================================================
export PATH=/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:/system/bin:/system/xbin:/vendor/bin:$PATH
MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR=${0%/*}/..
DATA="$MODDIR/data"
. "$MODDIR/bin/common.sh" 2>/dev/null

sv() { printf '%s' "$1" | tr '\n\r|' '   ' | cut -c1-200; }

pid=$(daemon_pid 2>/dev/null)
case "$pid" in '' | *[!0-9]*) pid="" ;; esac
if [ -n "$pid" ]; then run=1; else run=0; pid="-"; fi

# 预置变量 (缓存或回退都会填充)
state=""; mode=""; lk=0; kos=""; nodes=0; ksym=0; iv=5; strength=0; stats=""; guard=""
mko=0; msy=0; mst=0; mmd=0; msm=0; mdev=0; mfile=0; mcmd=0; mref=""; minit=""

# ---- 优先读守护缓存 (新鲜 <= 120s 且守护运行) -------------------------------
_cache=0
if [ "$run" = "1" ] && [ -f "$DATA/status" ]; then
    _up=$(awk '{print int($1)}' /proc/uptime 2>/dev/null); [ -z "$_up" ] && _up=0
    _ts=0
    while IFS= read -r _l; do
        case "$_l" in
            ts=*) _ts=${_l#ts=} ;;
            state=*) state=${_l#state=} ;;
            mode=*) mode=${_l#mode=} ;;
            inte_loaded=*) lk=${_l#inte_loaded=} ;;
            targets=*) kos=${_l#targets=} ;;
            nodes=*) nodes=${_l#nodes=} ;;
            ksym=*) ksym=${_l#ksym=} ;;
            interval=*) iv=${_l#interval=} ;;
            strength=*) strength=${_l#strength=} ;;
            stats=*) stats=${_l#stats=} ;;
            guard=*) guard=${_l#guard=} ;;
            m_ko=*) mko=${_l#m_ko=} ;;
            m_systbl=*) msy=${_l#m_systbl=} ;;
            m_status=*) mst=${_l#m_status=} ;;
            m_mod=*) mmd=${_l#m_mod=} ;;
            m_sysmod=*) msm=${_l#m_sysmod=} ;;
            m_dev=*) mdev=${_l#m_dev=} ;;
            m_file=*) mfile=${_l#m_file=} ;;
            m_cmd=*) mcmd=${_l#m_cmd=} ;;
            m_ref=*) mref=${_l#m_ref=} ;;
            m_init=*) minit=${_l#m_init=} ;;
        esac
    done < "$DATA/status"
    case "$_ts" in '' | *[!0-9]*) _ts=0 ;; esac
    if [ "$_ts" -gt 0 ] && [ "$_up" -ge "$_ts" ] && [ $((_up - _ts)) -lt 120 ]; then
        _cache=1
    fi
fi

# ---- 回退: 全量实时检测 (守护未运行 / 缓存过期) -----------------------------
if [ "$_cache" != "1" ]; then
    state=$(cat "$DATA/state" 2>/dev/null)
    mode=$(cat "$DATA/mode" 2>/dev/null)
    kos=$(detected_kos); [ -n "$kos" ] && lk=1 || lk=0
    nodes=$(inte_nodes)
    ksym=$(kallsyms_hit)
    mko=$(maple_ko); msy=$(maple_systbl); mst=$(maple_status); mmd=$(maple_mod); msm=$(maple_sysmod)
    mdev=$(inte_dev_nodes); mcmd=$(cmdline_hit); mref=$(inte_refcnt); minit=$(inte_initstate)
    _f=$(inte_ko_file); mfile=${_f%%'|'*}
    strength=$(ko_strength)
fi

# ---- 兜底与规整 ------------------------------------------------------------
[ -z "$state" ] && state=unknown
case "$mode" in unload | monitor) ;; *) mode=unload ;; esac
case "$iv" in '' | *[!0-9]*) iv=5 ;; esac
[ "$iv" -lt 1 ] && iv=1
[ "$iv" -gt 3600 ] && iv=3600
case "$strength" in 1) ;; *) strength=0 ;; esac
[ -z "$stats" ] && stats=$(ko_stats)
[ -z "$stats" ] && stats="unload=0,revive=0"

# ---- 实时补充 (轻量字段, 无论缓存与否都取实时值) ----------------------------
uptime=$(awk '{print int($1)}' /proc/uptime 2>/dev/null); [ -z "$uptime" ] && uptime=0
boot_uptime=$(cat "$DATA/boot_uptime" 2>/dev/null)
case "$boot_uptime" in '' | *[!0-9]*) boot_uptime=0 ;; esac
if [ "$boot_uptime" -gt 0 ] && [ "$uptime" -ge "$boot_uptime" ]; then
    rt=$((uptime - boot_uptime))
else
    rt=-1
fi
if [ "$rt" -ge 0 ] && [ "$rt" -gt "$uptime" ]; then rt=-1; fi

ver=$(sed -n 's/^version=//p' "$MODDIR/module.prop" 2>/dev/null | head -1)
[ -z "$ver" ] && ver="-"

svc=$(cat "$DATA/server" 2>/dev/null); case "$svc" in 1) ;; *) svc=0 ;; esac
if [ "$svc" = "1" ] && [ -f "$DATA/server.pid" ]; then
    spp=$(cat "$DATA/server.pid" 2>/dev/null)
    case "$spp" in '' | *[!0-9]*) spp=- ;; esac
    kill -0 "$spp" 2>/dev/null || spp=-
else
    spp=-
fi

link=$(cat "$DATA/link" 2>/dev/null); [ -z "$link" ] && link="-"

# ---- 状态文案 --------------------------------------------------------------
case "$state" in
    normal)
        if [ "$lk" = "1" ]; then ok=warn; head="待复核"; msg="inte 仍在加载, 守护正在处理"
        else ok=ok; head="已生效"; msg="inte 未加载, 内核完整性检测已失效"; fi ;;
    running)  ok=warn; head="处理中";   msg="正在卸载 inte" ;;
    failed)   ok=err;  head="未生效";   msg="inte 无法卸载 (可能被内核保护)" ;;
    builtin)  ok=err;  head="无法处理"; msg="inte 已编译进内核, rmmod 无效" ;;
    notfound) ok=info; head="无需处理"; msg="本机型未检测到 inte 子系统" ;;
    stopped)  ok=warn; head="已停止";   msg="守护进程已退出" ;;
    *)
        if [ "$lk" = "1" ]; then ok=warn; head="启动中"; msg="守护进程初始化中, inte 当前处于加载状态"
        else ok=ok; head="已生效"; msg="inte 未加载, 内核完整性检测已失效"; fi ;;
esac

if [ "$run" = "0" ] && [ "$state" = "normal" ]; then
    ok=warn; head="守护已停"; msg="inte 已清除, 但守护进程未运行 (重启后可能恢复)"
fi

if [ "$run" = "1" ]; then
    case "$state" in
        stopped | notfound | unknown)
            if [ "$lk" = "1" ]; then
                state=normal; ok=warn; head="处理中"; msg="检测到 inte 处于加载状态, 守护正在处理"
            else
                state=normal; ok=ok; head="已生效"; msg="inte 未加载, 内核完整性检测已失效"
            fi ;;
    esac
fi

# ---- 盯防文案 (缓存未提供时计算) --------------------------------------------
if [ -z "$guard" ]; then
    _guard="未运行"
    if [ "$run" = "1" ]; then
        case "$mode" in
            monitor) _guard="监控中·仅记录" ;;
            *) if [ "$lk" = "1" ]; then _guard="盯防中·已发现"; else _guard="盯防中·${iv}s"; fi ;;
        esac
    fi
    guard="$_guard"
fi

err=""
case "$state" in
    failed) err="连续多次 rmmod 失败, 可能被内核保护" ;;
    builtin) err="模块为 builtin, rmmod 无法生效" ;;
esac
[ "$ok" = "err" ] && [ -z "$err" ] && err="$msg"

logtail=$(tail -1 "$DATA/daemon.log" 2>/dev/null)
log3=$(tail -n 3 "$DATA/daemon.log" 2>/dev/null | tr '\n' '|' | sed 's/|$//')
bootlog=$(tail -n 3 "$DATA/boot.log" 2>/dev/null | tr '\n' '|' | sed 's/|$//')

printf 'version=%s|state=%s|mode=%s|head=%s|msg=%s|ok=%s|inte_loaded=%s|targets=%s|nodes=%s|ksym=%s|run=%s|pid=%s|uptime=%s|runtime=%s|model=%s|brand=%s|kernel=%s|root=%s|err=%s|log=%s|log3=%s|guard=%s|interval=%s|author=%s|link=%s|server=%s|srv_pid=%s|m_ko=%s|m_systbl=%s|m_status=%s|m_mod=%s|m_sysmod=%s|m_dev=%s|m_file=%s|m_cmd=%s|m_ref=%s|m_init=%s|strength=%s|stats=%s|bootlog=%s' \
    "$(sv "$ver")" "$(sv "$state")" "$(sv "$mode")" "$(sv "$head")" "$(sv "$msg")" "$(sv "$ok")" \
    "$(sv "$lk")" "$(sv "$kos")" "$(sv "$nodes")" "$(sv "$ksym")" "$(sv "$run")" "$(sv "$pid")" \
    "$(sv "$uptime")" "$(sv "$rt")" "$(sv "$(dev_model)")" "$(sv "$(dev_brand)")" "$(sv "$(dev_kernel)")" \
    "$(sv "$(root_manager)")" "$(sv "$err")" "$(sv "$logtail")" "$(sv "$log3")" "$(sv "$guard")" "$(sv "$iv")" \
    "Kazaf" "$(sv "$link")" "$svc" "$(sv "$spp")" "$mko" "$msy" "$mst" "$mmd" "$msm" \
    "$mdev" "$mfile" "$mcmd" "$mref" "$minit" "$strength" "$(sv "$stats")" "$(sv "$bootlog")"
printf '\n'
exit 0
