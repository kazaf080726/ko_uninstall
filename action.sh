#!/system/bin/sh
# action.sh — Root 管理器「执行」按钮: 立即清除一次
# KO卸载 · Kazaf
MODDIR=${0%/*}
DATA="$MODDIR/data"
STATE="$DATA/state"
LOG="$DATA/daemon.log"
. "$MODDIR/bin/common.sh" 2>/dev/null
[ -z "$BB" ] && BB=busybox
mkdir -p "$DATA" 2>/dev/null

say() {
    echo "$1"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [action] $1" >>"$LOG" 2>/dev/null
}

say "===== KO卸载 · 手动执行清除 ====="
say "root 管理器: $(root_manager) | rmmod: $(find_rmmod)"

targets=$(detected_kos)
if [ -z "$targets" ]; then
    if [ "$(inte_nodes)" = "0" ]; then
        say "inte 未加载, 无需处理 (节点 0/3)"
        echo normal >"$STATE"
        say "当前状态: 已生效"
    else
        say "未发现已加载模块, 但存在 /proc 节点: $(inte_nodes)/3"
        say "可能为 builtin 形态, 建议重启后复查"
        echo builtin >"$STATE"
    fi
    exit 0
fi

say "检测到目标模块:$targets"

failed=0
for m in $targets; do
    out=$(do_rmmod "$m")
    say "rmmod $m => ${out:-（无输出, 成功）}"
    case "$out" in
        *builtin* | *"is built in"* | *"built into"*)
            echo builtin >"$STATE"
            say "$m 为 builtin, 无法卸载"
            failed=1 ;;
        protected)
            say "$m 在保护名单中, 已跳过" ;;
        no-rmmod)
            say "找不到可用的 rmmod 命令"
            failed=1 ;;
    esac
done

i=0
while [ $i -lt 10 ] && [ -n "$(detected_kos)" ]; do
    sleep 1
    i=$((i + 1))
done

if [ -z "$(detected_kos)" ]; then
    echo normal >"$STATE"
    say "清除成功 / /proc 节点剩余: $(inte_nodes)/3"
else
    [ "$failed" = "0" ] && echo failed >"$STATE"
    say "失败 / 仍存在:$(detected_kos)"
fi
exit 0
