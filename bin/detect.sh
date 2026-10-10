#!/system/bin/sh
# ============================================================================
# detect.sh — KO卸载 内核检测面板 (融合 3.sh「MAPLE · 多于ko模块检测」)
# 输出: 可读多行 + 最后一行单行协议 (ksu.exec 只取最后一行)
# 协议行: ko=0|systbl=0|status=0|mod=0|sysmod=0|ksym=0|nodes=0|loaded=0
# ============================================================================
export PATH=/system/bin:/system/xbin:/vendor/bin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH
MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR=${0%/*}/..
. "$MODDIR/bin/common.sh" 2>/dev/null

echo ""
echo "=================================================="
echo "  KO卸载 · 内核检测 (MAPLE)"
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
else
    echo "[未确认] /sys/module/inte 不存在"
fi
if [ -r /proc/kallsyms ] && grep -q '\[inte\]' /proc/kallsyms 2>/dev/null; then
    echo "[确认] kallsyms 存在 inte 符号"
else
    echo "[未确认] kallsyms 无 inte 符号或读取受限"
fi

# 单行协议 (最后一行为 ksu.exec 返回值)
printf 'ko=%s|systbl=%s|status=%s|mod=%s|sysmod=%s|ksym=%s|nodes=%s|loaded=%s\n' \
    "$(maple_ko)" "$(maple_systbl)" "$(maple_status)" \
    "$(maple_mod)" "$(maple_sysmod)" "$(kallsyms_hit)" \
    "$(inte_nodes)" "$(detected_kos | tr -d ' ' | tr -c 'A-Za-z0-9' '.')"
exit 0
