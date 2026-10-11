#!/system/bin/sh
# ============================================================================
# detect.sh — KO卸载 内核检测面板 (v1.1.0 增强 · 融合 3.sh「MAPLE · 多于ko模块检测」)
# 输出: 可读多行 + 最后一行单行协议 (ksu.exec 只取最后一行)
# 协议行: ko=0|systbl=0|status=0|mod=0|sysmod=0|ksym=0|nodes=0|loaded=0|dev=0|file=0|cmd=0|ref=0|init=0
# v1.1.0: 新增 /dev 节点 / inte.ko 文件 / cmdline / refcnt / initstate 检测
# ============================================================================
export PATH=/system/bin:/system/xbin:/vendor/bin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH
MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR=${0%/*}/..
. "$MODDIR/bin/common.sh" 2>/dev/null

echo ""
echo "=================================================="
echo "  KO卸载 · 内核检测 (MAPLE+ v1.1.0)"
echo "=================================================="

echo ""
echo "── 内核检测状态 ──"
for p in /proc/inte_ko /proc/inte_systbl /proc/inte_status; do
    if [ -e "$p" ]; then
        echo "[存在] $p"
    else
        echo "[未确认] $p (不存在或访问受限)"
    fi
done
echo ""
echo "── 模块状态 ──"
if [ -r /proc/modules ] && grep -q '^inte ' /proc/modules 2>/dev/null; then
    echo "[确认] inte 模块已加载 (欧加多于ko已内置)"
else
    echo "[未确认] 未找到模块记录或读取受限"
fi
if [ -d /sys/module/inte ]; then
    echo "[确认] /sys/module/inte 目录存在"
    _rc=$(inte_refcnt); [ -n "$_rc" ] && echo "       · refcnt=${_rc}"
    _is=$(inte_initstate); [ -n "$_is" ] && echo "       · initstate=${_is}"
else
    echo "[未确认] /sys/module/inte 不存在"
fi
if [ -r /proc/kallsyms ] && grep -q '\[inte\]' /proc/kallsyms 2>/dev/null; then
    echo "[确认] kallsyms 存在 inte 符号"
else
    echo "[未确认] kallsyms 无 inte 符号或读取受限"
fi

echo ""
echo "── 扩展检测 (v1.1.0) ──"
_n=$(inte_dev_nodes)
if [ "$_n" -gt 0 ]; then
    echo "[确认] /dev 下 inte 相关节点: ${_n} 个"
else
    echo "[未确认] /dev 下无 inte 相关节点"
fi
_f=$(inte_ko_file)
if [ "${_f%%'|'*}" = "1" ]; then
    echo "[确认] inte.ko 文件存在: ${_f#*'|'}"
else
    echo "[未确认] 未找到 inte.ko 文件"
fi
if [ "$(cmdline_hit)" = "1" ]; then
    echo "[确认] 内核 cmdline 含 inte/integrity 特征"
else
    echo "[未确认] 内核 cmdline 无 inte 特征"
fi
_c=$(inte_proc_content)
if [ -n "$_c" ]; then
    echo "[确认] /proc/inte_ko 内容: $_c"
fi

# 单行协议 (最后一行为 ksu.exec 返回值)
printf 'ko=%s|systbl=%s|status=%s|mod=%s|sysmod=%s|ksym=%s|nodes=%s|loaded=%s|dev=%s|file=%s|cmd=%s|ref=%s|init=%s\n' \
    "$(maple_ko)" "$(maple_systbl)" "$(maple_status)" \
    "$(maple_mod)" "$(maple_sysmod)" "$(kallsyms_hit)" \
    "$(inte_nodes)" "$(detected_kos | tr -d ' ' | tr -c 'A-Za-z0-9' '.')" \
    "$(inte_dev_nodes)" "${_f%%'|'*}" "$(cmdline_hit)" \
    "$(inte_refcnt)" "$(inte_initstate)"
exit 0
