#!/system/bin/sh
# ============================================================================
# common.sh — KO卸载 公共函数库
# 作者: Kazaf  |  续承: 冬日渡川(ddc_inte_killer) / 无敌WX(Fuck-inte) / MapleAutumn(3.sh)
# 适配: Magisk / KernelSU / SukiSU / KernelSU-Next / RKSU / APatch
# ============================================================================

PATH="/system/bin:/system/xbin:/vendor/bin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH"
export PATH

BB=""
for _b in /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox \
          /data/adb/magisk/busybox /system/bin/busybox /system/xbin/busybox; do
    if [ -x "$_b" ]; then BB="$_b"; break; fi
done

# ---- 目标模块精确白名单 (仅触碰以下模块, 其他内核模块一律不碰) ----------------
# inte              : 一加/OPPO 实测模块名 (创建 /proc/inte_* 节点)
# oplus_integrity   : 部分 ColorOS 机型使用
# oplus_kohashpro   : 部分机型独立加载
# oplus_secure_guard / _new : vendor_boot 工具原目标 (兜底)
INTE_CANDIDATES="inte oplus_integrity oplus_kohashpro oplus_secure_guard oplus_secure_guard_new"

# ---- 保护名单 (绝不触碰, 即使名字相近也不动) ----------------------------------
INTE_PROTECT="cdt_integrity cdt_integrity.ko msm_core qmi_helpers"

is_protected() {
    for _p in $INTE_PROTECT; do
        [ "$1" = "$_p" ] && return 0
    done
    return 1
}

loaded_list() {
    if [ -r /proc/modules ]; then
        cat /proc/modules 2>/dev/null
    else
        lsmod 2>/dev/null
    fi
}

ko_loaded() {
    [ -n "$1" ] || return 1
    is_protected "$1" && return 1
    [ -d "/sys/module/$1" ] && return 0
    loaded_list | awk -v m="$1" '$1==m{found=1} END{exit !found}'
}

detected_kos() {
    _out=""
    for _c in $INTE_CANDIDATES; do
        is_protected "$_c" && continue
        if [ -d "/sys/module/$_c" ]; then
            _out="$_out $_c"
        fi
    done
    if [ -r /proc/modules ]; then
        _names=$(awk '{print $1}' /proc/modules 2>/dev/null)
        for _c in $INTE_CANDIDATES; do
            is_protected "$_c" && continue
            case " $_out " in *" $_c "*) continue ;; esac
            for _n in $_names; do
                [ "$_n" = "$_c" ] && _out="$_out $_c"
            done
        done
    fi
    printf '%s' "$_out" | sed 's/^ *//; s/ *$//'
}

# ---- /proc/inte_* 节点计数 ------------------------------------------------
inte_nodes() {
    _n=0
    for _p in /proc/inte_ko /proc/inte_systbl /proc/inte_status; do
        [ -e "$_p" ] && _n=$((_n + 1))
    done
    printf '%s' "$_n"
}

kallsyms_hit() {
    [ -r /proc/kallsyms ] || { printf '0'; return; }
    if grep -q '\[inte\]' /proc/kallsyms 2>/dev/null; then printf '1'; else printf '0'; fi
}

inte_active() {
    [ -n "$(detected_kos)" ] && return 0
    [ "$(inte_nodes)" -gt 0 ] && return 0
    [ "$(kallsyms_hit)" = "1" ] && return 0
    return 1
}

# ---- MAPLE 内核检测 (3.sh 集成) — 逐节点检测 ----------------------------------
maple_ko()     { [ -e /proc/inte_ko ] && printf '1' || printf '0'; }
maple_systbl() { [ -e /proc/inte_systbl ] && printf '1' || printf '0'; }
maple_status() { [ -e /proc/inte_status ] && printf '1' || printf '0'; }
# /proc/modules 中是否存在 inte 模块记录
maple_mod() {
    if [ -r /proc/modules ] && grep -q '^inte ' /proc/modules 2>/dev/null; then
        printf '1'
    else
        printf '0'
    fi
}
# /sys/module/inte 目录是否存在
maple_sysmod() { [ -d /sys/module/inte ] && printf '1' || printf '0'; }

# ---- 设备信息 --------------------------------------------------------------
dev_brand() { getprop ro.product.brand 2>/dev/null; }
dev_model() { getprop ro.product.model 2>/dev/null; }
dev_kernel() { uname -r 2>/dev/null; }

root_manager() {
    if [ -n "$KSU" ] || [ -d /data/adb/ksu ]; then
        _v=""
        [ -r /data/adb/ksu/version ] && _v=$(cat /data/adb/ksu/version 2>/dev/null)
        [ -z "$_v" ] && _v=$(getprop ro.kernelsu.version 2>/dev/null)
        case "$_v" in
            *SukiSU* | *sukisu* | *SUKISU*) printf 'SukiSU Ultra'; return ;;
        esac
        case "$_v" in
            *Next* | *next*) printf 'KernelSU-Next'; return ;;
        esac
        case "$_v" in
            *RKSU* | *rksu*) printf 'RKSU'; return ;;
        esac
        printf 'KernelSU%s' "${_v:+ $_v}"
        return
    fi
    if [ -d /data/adb/ap ]; then printf 'APatch'; return; fi
    if [ -d /data/adb/magisk ]; then printf 'Magisk'; return; fi
    printf '未知'
}

# ---- 守护进程识别与管理 ----------------------------------------------------
# 默认模块目录 (供外部脚本独立调用时兜底)
KO_DEFDIR="/data/adb/modules/ko_uninstall"

is_own_daemon() {
    [ -n "$1" ] || return 1
    [ -d "/proc/$1" ] || return 1
    _c=$(tr '\0' ' ' <"/proc/$1/cmdline" 2>/dev/null)
    case "$_c" in
        *ko_uninstall_daemon* | *launch_daemon* | *ko_daemon* | *inte_daemon*) return 0 ;;
    esac
    for _f in /proc/"$1"/fd/*; do
        _t=$(readlink "$_f" 2>/dev/null)
        case "$_t" in
            *".sh (deleted)"*) return 0 ;;
        esac
    done
    _rec=$(cat "${DATA:-$KO_DEFDIR/data}/daemon.pid" 2>/dev/null)
    case "$_rec" in '' | *[!0-9]*) ;; *) [ "$_rec" = "$1" ] && return 0 ;; esac
    return 1
}

daemon_pid() {
    _l=$(cat "${DATA:-$KO_DEFDIR/data}/daemon.lock" 2>/dev/null)
    case "$_l" in
        '' | *[!0-9]*) _l="" ;;
    esac
    if [ -n "$_l" ] && kill -0 "$_l" 2>/dev/null && is_own_daemon "$_l"; then
        printf '%s' "$_l"
        return 0
    fi
    printf ''
}

# ---- rmmod 定位与执行 ------------------------------------------------------
find_rmmod() {
    for _c in /system/bin/rmmod /system/xbin/rmmod /vendor/bin/rmmod; do
        [ -x "$_c" ] && { printf '%s' "$_c"; return; }
    done
    [ -n "$BB" ] && [ -x "$BB" ] && { printf '%s rmmod' "$BB"; return; }
    command -v rmmod >/dev/null 2>&1 && { printf 'rmmod'; return; }
    printf ''
}

do_rmmod() {
    _mod="$1"
    [ -n "$_mod" ] || return 1
    is_protected "$_mod" && { printf 'protected'; return 1; }
    _r="$(find_rmmod)"
    [ -n "$_r" ] || { printf 'no-rmmod'; return 1; }
    printf '%s' "$($_r "$_mod" 2>&1)"
    return $?
}

# 通用 httpd 定位 (本地服务模式用)
find_httpd() {
    [ -n "$BB" ] && [ -x "$BB" ] && { printf '%s httpd' "$BB"; return; }
    for _c in /system/bin/httpd /system/xbin/httpd /vendor/bin/httpd; do
        [ -x "$_c" ] && { printf '%s' "$_c"; return; }
    done
    command -v httpd >/dev/null 2>&1 && { printf 'httpd'; return; }
    printf ''
}
