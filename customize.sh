#!/system/bin/sh
# ============================================================================
# KO卸载 · customize.sh — 安装 / 升级时初始化 (v2.0.0)
# 作者: Kazaf  |  开源: GitHub
# 兼容: Magisk / KernelSU / SukiSU / KernelSU-Next / RKSU / APatch
# ============================================================================

SKIPUNZIP=0

# ---- 项目主页（WebUI「项目主页」按钮使用）-----------------------------------
KO_LINK='https://github.com/kazaf080726/ko_uninstall'
KO_REPO='ko_uninstall'
KO_DATA="$MODPATH/data"

ui_print " "
ui_print "******************************************"
ui_print "   KO卸载  v2.0.0  ·  Kazaf"
ui_print "   史诗级重构 · 液态玻璃 WebUI + APP"
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
            *) grep -q '\.sh' "/proc/$_p/cmdline" 2>/dev/null && kill -9 "$_p" 2>/dev/null && ui_print "  · 已停止旧守护 (pid=$_p)" ;;
        esac
        _s=$(cat "$OLD/data/server.pid" 2>/dev/null)
        case "$_s" in
            '' | *[!0-9]*) ;;
            *) grep -q '\.sh' "/proc/$_s/cmdline" 2>/dev/null && kill -9 "$_s" 2>/dev/null ;;
        esac
    fi
done
rm -f /dev/.sh /data/local/.sh /data/adb/.ddc_inte_killer_link 2>/dev/null

# ---- 初始化数据目录 ---------------------------------------------------------
mkdir -p "$KO_DATA" "$MODPATH/bin" 2>/dev/null
echo unload >"$KO_DATA/mode"          # unload=自动卸载 | monitor=只监控
echo 5 >"$KO_DATA/interval"           # 复核间隔(秒) 1..3600
echo 5 >"$KO_DATA/interval_usr"       # 用户设定间隔 (自适应恢复用)
echo 1 >"$KO_DATA/strength"           # 强度: 1=激进(默认) 0=标准
printf 'unload=0,revive=0\n' >"$KO_DATA/stats"   # 防护统计
date +%s >"$KO_DATA/installed"        # 安装时间戳
: >"$KO_DATA/daemon.log"              # 守护日志
echo "unknown" >"$KO_DATA/state"      # normal/running/failed/builtin/notfound/stopped
echo "$KO_LINK" >"$KO_DATA/link"      # 项目主页链接
echo 1 >"$KO_DATA/server"             # 本地服务开关 0=关 1=开 (默认开, 保证 WebUI 可用)

# ---- 权限 ----------------------------------------------------------------
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$MODPATH/bin" 0 0 0755 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/data" 0 0 0755

# ---- 安装管理 APP (KO卸载 v2.0 · 与 WebUI 并存) ----------------------------
APP="$MODPATH/app/KOUninstall.apk"
if [ -f "$APP" ]; then
    ui_print "- 安装管理 APP …"
    if command -v pm >/dev/null 2>&1; then
        if pm install -r "$APP" >/dev/null 2>&1 || cmd package install -r "$APP" >/dev/null 2>&1; then
            ui_print "  · 管理 APP 安装成功 (KO卸载)"
        else
            ui_print "  · 管理 APP 安装失败, 可手动安装: $APP"
        fi
    else
        ui_print "  · 跳过 APP 安装 (未找到 pm)"
    fi
    mkdir -p "$KO_DATA/app" 2>/dev/null
    cp -f "$APP" "$KO_DATA/app/KOUninstall.apk" 2>/dev/null
fi

ui_print " "
ui_print "- 安装完成, 重启后生效"
ui_print "- 打开 KernelSU/Magisk 管理器 → 模块 → KO卸载 → WebUI"
ui_print "- 或直接打开「KO卸载」APP 管理"
ui_print " "
exit 0
