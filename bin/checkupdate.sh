#!/system/bin/sh
# ============================================================================
# checkupdate.sh — KO卸载 在线升级检测 (update.json)
# 读取 module.prop 的 updateJson -> 拉取远端 JSON -> 解析最新版本
# 协议行: need=1|current=v1.00|latest=v1.01|code=101|zip=URL|note=xxx|ts=...
#         need=0 表示已是最新 / 无法联网 / 未配置 updateJson
# ============================================================================
export PATH=/system/bin:/system/xbin:/vendor/bin:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk:$PATH
MODDIR=$(CDPATH= cd -- "${0%/*}/.." 2>/dev/null && pwd)
[ -n "$MODDIR" ] || MODDIR=${0%/*}/..
. "$MODDIR/bin/common.sh" 2>/dev/null

# 当前版本
CUR_VER=$(sed -n 's/^version=//p' "$MODDIR/module.prop" 2>/dev/null | head -1)
CUR_CODE=$(sed -n 's/^versionCode=//p' "$MODDIR/module.prop" 2>/dev/null | head -1)
[ -z "$CUR_VER" ] && CUR_VER="?"
[ -z "$CUR_CODE" ] && CUR_CODE=0

# 更新源
UJSON=$(sed -n 's/^updateJson=//p' "$MODDIR/module.prop" 2>/dev/null | head -1)
case "$UJSON" in
    '' | *REPLACE_USER*) printf 'need=0|current=%s|latest=%s|code=%s|zip=|note=未配置更新源 (发布后生效)|ts=\n' "$CUR_VER" "$CUR_VER" "$CUR_CODE"; exit 0 ;;
esac

TMP=/data/local/tmp/.ko_update.json
: >"$TMP"

# 抓取: curl -> wget -> busybox wget
_fetch=0
if command -v curl >/dev/null 2>&1; then
    curl -fsSL --max-time 8 "$UJSON" >"$TMP" 2>/dev/null && _fetch=1
fi
if [ "$_fetch" = "0" ] && command -v wget >/dev/null 2>&1; then
    wget -q -T 8 -O "$TMP" "$UJSON" 2>/dev/null && _fetch=1
fi
if [ "$_fetch" = "0" ] && [ -n "$BB" ] && [ -x "$BB" ]; then
    "$BB" wget -q -T 8 -O "$TMP" "$UJSON" 2>/dev/null && _fetch=1
fi

if [ "$_fetch" = "0" ] || [ ! -s "$TMP" ]; then
    printf 'need=0|current=%s|latest=%s|code=%s|zip=|note=无法联网获取更新信息|ts=\n' "$CUR_VER" "$CUR_VER" "$CUR_CODE"
    rm -f "$TMP" 2>/dev/null
    exit 0
fi

_jget() { # 简易 JSON 取值: _jget "version"
    sed -n "s/.*\"$1\"[ ]*:[ ]*\"\([^\"}]*\)\".*/\1/p" "$TMP" 2>/dev/null | head -1
}
LAT_VER=$(_jget "version")
LAT_CODE=$(_jget "versionCode")
ZIP_URL=$(_jget "zipUrl")
CHANGE=$(_jget "changelog")
TS=$(_jget "lastUpdate")

case "$LAT_CODE" in '' | *[!0-9]*) LAT_CODE=0 ;; esac
case "$CUR_CODE" in '' | *[!0-9]*) CUR_CODE=0 ;; esac

if [ "$LAT_CODE" -gt "$CUR_CODE" ] && [ -n "$LAT_VER" ]; then
    printf 'need=1|current=%s|latest=%s|code=%s|zip=%s|note=%s|ts=%s\n' \
        "$CUR_VER" "$LAT_VER" "$LAT_CODE" "$ZIP_URL" "$CHANGE" "$TS"
else
    printf 'need=0|current=%s|latest=%s|code=%s|zip=%s|note=%s|ts=%s\n' \
        "$CUR_VER" "$LAT_VER" "$LAT_CODE" "$ZIP_URL" "$CHANGE" "$TS"
fi
rm -f "$TMP" 2>/dev/null
exit 0
