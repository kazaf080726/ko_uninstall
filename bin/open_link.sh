#!/system/bin/sh
# ============================================================================
# open_link.sh — 用系统浏览器/对应 App 打开一条链接 (KO卸载)
# 用法: sh bin/open_link.sh <url> [最多次数] [间隔秒]
# 返回: 0=已拉起  1=参数缺失  2=找不到 am  3=被系统拒绝/无接收者
# ============================================================================

URL="$1"
MAXN="$2"
GAP="$3"
[ -z "$URL" ] && exit 1
case "$MAXN" in '' | *[!0-9]*) MAXN=1 ;; esac
case "$GAP" in '' | *[!0-9]*) GAP=3 ;; esac
[ "$MAXN" -lt 1 ] && MAXN=1

export PATH=/system/bin:/system/xbin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH

command -v am >/dev/null 2>&1 || exit 2

OUT=""
for _d in /data/local/tmp /dev; do
    [ -d "$_d" ] && [ -w "$_d" ] && { OUT="$_d/.am_out"; break; }
done
[ -n "$OUT" ] || exit 3

_try() {
    rm -f "$OUT" 2>/dev/null
    am start --user 0 -a android.intent.action.VIEW -d "$URL" >"$OUT" 2>&1 &
    _ap=$!
    _n=0
    while [ "$_n" -lt 8 ]; do
        kill -0 "$_ap" 2>/dev/null || break
        sleep 1
        _n=$((_n + 1))
    done
    if kill -0 "$_ap" 2>/dev/null; then
        kill -9 "$_ap" 2>/dev/null
        rm -f "$OUT" 2>/dev/null
        return 3
    fi
    wait "$_ap" 2>/dev/null
    _rc=$?
    _o=$(cat "$OUT" 2>/dev/null)
    rm -f "$OUT" 2>/dev/null
    [ $_rc -ne 0 ] && return 3
    case "$_o" in
        *rror* | *xception* | *nable* | *Idling* | *"does not exist" | *"Can't find service"*)
            return 3 ;;
    esac
    case "$_o" in
        *"Starting: Intent"*) return 0 ;;
    esac
    return 3
}

_i=0
while [ "$_i" -lt "$MAXN" ]; do
    _i=$((_i + 1))
    _try && exit 0
    [ "$_i" -lt "$MAXN" ] && sleep "$GAP"
done
exit 3
