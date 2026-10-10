#!/system/bin/sh
# ============================================================================
# launch_daemon.sh — KO卸载 守护进程启动器 (进程隐藏版)
# 作者: Kazaf
# 原理: awk 提取 daemon.sh 正文到 /dev/.sh -> sh 启动 -> 立刻 rm
#       cmdline 变成 "sh /dev/.sh", fd 指向 "(deleted)", 无任何痕迹
# ============================================================================
BINDIR=$(CDPATH= cd -- "${0%/*}" 2>/dev/null && pwd)
[ -n "$BINDIR" ] || BINDIR=${0%/*}
MODDIR=$(CDPATH= cd -- "$BINDIR/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR="$BINDIR/.."

DAEMON="$BINDIR/daemon.sh"
[ -f "$DAEMON" ] || exit 0

MARK='@@__KO_DAEMON_BODY__@@'
KO_MODDIR="$MODDIR"
export KO_MODDIR

TMP=""
for d in /dev /data/local; do
    [ -d "$d" ] && [ -w "$d" ] && { TMP="$d/.sh"; break; }
done
[ -n "$TMP" ] || TMP="$MODDIR/data/.sh"

awk -v m="$MARK" 'BEGIN{f=0} $0==m{f=1;next} f{print}' "$DAEMON" > "$TMP" 2>/dev/null
[ -s "$TMP" ] || { rm -f "$TMP"; exit 0; }
chmod 700 "$TMP" 2>/dev/null

KO_MODDIR="$MODDIR" nohup setsid sh "$TMP" "$@" >/dev/null 2>&1 &
sleep 1
rm -f "$TMP" 2>/dev/null
exit 0
