#!/system/bin/sh
# ============================================================================
# webstatus.sh — KO卸载 WebUI 单行状态协议 (v1.1.0)
# KernelSU 3.3.0+ 的 window.ksu.exec() 只返回 stdout 最后一行
# 所有字段用 | 分隔, 一次性输出一行
# v1.1.0: 新增 dev/file/cmd/ref/init/strength/stats 字段
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

state=$(cat "$DATA/state" 2>/dev/null); [ -z "$state" ] && state=unknown
mode=$(cat "$DATA/mode" 2>/dev/null); case "$mode" in unload|monitor) ;; *) mode=unload ;; esac

kos=$(detected_kos); [ -n "$kos" ] && lk=1 || lk=0
nodes=$(inte_nodes)
ksym=$(kallsyms_hit)

iv=$(cat "$DATA/interval" 2>/dev/null)
case "$iv" in '' | *[!0-9]*) iv=5 ;; esac
[ "$iv" -lt 1 ] && iv=1
[ "$iv" -gt 3600 ] && iv=3600

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

# MAPLE 检测 (3.sh 集成) + v1.1.0 扩展
mko=$(maple_ko); msy=$(maple_systbl); mst=$(maple_status); mmd=$(maple_mod); msm=$(maple_sysmod)
mdev=$(inte_dev_nodes); mcmd=$(cmdline_hit); mref=$(inte_refcnt); minit=$(inte_initstate)
_f=$(inte_ko_file); mfile=${_f%%'|'*}
strength=$(ko_strength)
stats=$(ko_stats); [ -z "$stats" ] && stats="unload=0,revive=0"

# 本地服务
svc=$(cat "$DATA/server" 2>/dev/null); case "$svc" in 1) ;; *) svc=0 ;; esac
if [ "$svc" = "1" ] && [ -f "$DATA/server.pid" ]; then
    spp=$(cat "$DATA/server.pid" 2>/dev/null)
    case "$spp" in '' | *[!0-9]*) spp=- ;; esac
    kill -0 "$spp" 2>/dev/null || spp=-
else
    spp=-
fi

# 链接
link=$(cat "$DATA/link" 2>/dev/null); [ -z "$link" ] && link="-"

# 状态文案
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

_guard="未运行"
if [ "$run" = "1" ]; then
    if [ "$lk" = "1" ]; then _guard="盯防中·已发现"
    else _guard="盯防中·${iv}s"; fi
fi

err=""
case "$state" in
    failed) err="连续多次 rmmod 失败, 可能被内核保护" ;;
    builtin) err="模块为 builtin, rmmod 无法生效" ;;
esac
[ "$ok" = "err" ] && [ -z "$err" ] && err="$msg"

logtail=$(tail -1 "$DATA/daemon.log" 2>/dev/null)
log3=$(tail -n 3 "$DATA/daemon.log" 2>/dev/null | tr '\n' '|' | sed 's/|$//')

printf 'version=%s|state=%s|mode=%s|head=%s|msg=%s|ok=%s|inte_loaded=%s|targets=%s|nodes=%s|ksym=%s|run=%s|pid=%s|uptime=%s|runtime=%s|model=%s|brand=%s|kernel=%s|root=%s|err=%s|log=%s|log3=%s|guard=%s|interval=%s|author=%s|link=%s|server=%s|srv_pid=%s|m_ko=%s|m_systbl=%s|m_status=%s|m_mod=%s|m_sysmod=%s|m_dev=%s|m_file=%s|m_cmd=%s|m_ref=%s|m_init=%s|strength=%s|stats=%s' \
    "$(sv "$ver")" "$(sv "$state")" "$(sv "$mode")" "$(sv "$head")" "$(sv "$msg")" "$(sv "$ok")" \
    "$(sv "$lk")" "$(sv "$kos")" "$(sv "$nodes")" "$(sv "$ksym")" "$(sv "$run")" "$(sv "$pid")" \
    "$(sv "$uptime")" "$(sv "$rt")" "$(sv "$(dev_model)")" "$(sv "$(dev_brand)")" "$(sv "$(dev_kernel)")" \
    "$(sv "$(root_manager)")" "$(sv "$err")" "$(sv "$logtail")" "$(sv "$log3")" "$(sv "$_guard")" "$(sv "$iv")" \
    "Kazaf" "$(sv "$link")" "$svc" "$(sv "$spp")" "$mko" "$msy" "$mst" "$mmd" "$msm" \
    "$mdev" "$mfile" "$mcmd" "$mref" "$minit" "$strength" "$(sv "$stats")"
printf '\n'
exit 0
