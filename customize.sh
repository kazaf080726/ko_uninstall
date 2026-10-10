#!/system/bin/sh
# ============================================================================
# KO卸载 · customize.sh — 安装 / 升级时初始化
# 作者: Kazaf  |  开源: GitHub
# 兼容: Magisk / KernelSU / SukiSU / KernelSU-Next / RKSU / APatch
# ============================================================================

SKIPUNZIP=0

# ---- 项目主页（WebUI「项目主页」按钮使用；发布 Release 后请改为真实地址）----
KO_LINK='https://github.com/kazaf080726/ko_uninstall'
KO_REPO='ko_uninstall'
KO_DATA="$MODPATH/data"

ui_print " "
ui_print "******************************************"
ui_print "   KO卸载  v1.00  ·  Kazaf"
ui_print "   史诗级重构 · 液态玻璃 WebUI"
ui_print "******************************************"
ui_print " "
ui_print "- 环境: $ARCH / API $API"
[ -n "$KSU_VER" ] && ui_print "- KernelSU: $KSU_VER"
[ -n "$MAGISK_VER" ] && ui_print "- Magisk: $MAGISK_VER"

# ---- 升级 / 覆盖安装前：清理旧守护进程（兼容上一代 ddc_inte_killer 与 Fuck-inte）--
for OLD in /data/adb/modules/ddc_inte_killer /data/adb/modules/Fuck-inte /data/adb/modules/ko_uninstall; do
    if [ -d "$OLD" ]; then
        ui_print "- 检测到旧模块 $OLD, 清理旧守护进程"
        _p=$(cat "$OLD/data/daemon.lock" 2>/dev/null)
        case "$_p" in
            '' | *[!0-9]*) ;;
            *) kill -9 "$_p" 2>/dev/null && ui_print "  · 已停止旧守护 (pid=$_p)" ;;
        esac
        _s=$(cat "$OLD/data/server.pid" 2>/dev/null)
        case "$_s" in
            '' | *[!0-9]*) ;;
            *) kill -9 "$_s" 2>/dev/null ;;
        esac
    fi
done
rm -f /dev/.sh /data/local/.sh /data/adb/.ddc_inte_killer_link 2>/dev/null

# ---- 初始化数据目录 ---------------------------------------------------------
mkdir -p "$KO_DATA" "$MODPATH/bin" 2>/dev/null
echo unload >"$KO_DATA/mode"          # unload=自动卸载 | monitor=只监控
echo 5 >"$KO_DATA/interval"           # 复核间隔(秒) 1..3600
date +%s >"$KO_DATA/installed"        # 安装时间戳
: >"$KO_DATA/daemon.log"              # 守护日志
echo "unknown" >"$KO_DATA/state"      # normal/running/failed/builtin/notfound/stopped
echo "$KO_LINK" >"$KO_DATA/link"      # 项目主页链接
echo 0 >"$KO_DATA/server"             # 本地服务开关 0=关 1=开

# ---- 权限 ----------------------------------------------------------------
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$MODPATH/bin" 0 0 0755 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/data" 0 0 0755

ui_print " "
ui_print "- 安装完成, 重启后生效"
ui_print "- 打开 KernelSU/Magisk 管理器 → 模块 → KO卸载 → WebUI"
ui_print " "
exit 0
