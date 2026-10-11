/* ==========================================================================
 * KO卸载 · WebUI v3.2 (SukiSU Miuix 风格重构)
 * 桥接: window.ksu.exec() 只返回 stdout 最后一行 -> 单行 k=v|k=v 协议
 * 命令: webstatus.sh 轮询 / detect.sh 检测 / checkupdate.sh 升级
 *       server.sh 本地服务 / open_link.sh 打开链接 / action.sh 立即清除
 * v3.2: 三 tab 视图(总览/检测/设置) + 悬浮胶囊导航 + 15色主题 + 明暗模式
 *       修复 v3.1 布局重叠/遮挡, 检测项加入口图标, 渲染全面重排
 * ========================================================================== */
(function () {
  'use strict';

  var POLL_MS = 2000;
  var MODDIR = '/data/adb/modules/ko_uninstall';
  var STATUS_CMD = 'sh ' + MODDIR + '/bin/webstatus.sh';
  var DETECT_CMD = 'sh ' + MODDIR + '/bin/detect.sh';
  var UP_CMD = 'sh ' + MODDIR + '/bin/checkupdate.sh';
  var SET_CMD = 'sh ' + MODDIR + '/bin/action.sh';
  var SRV_CMD = 'sh ' + MODDIR + '/bin/server.sh';
  var LINK_CMD = 'sh ' + MODDIR + '/bin/open_link.sh';
  var LANG_KEY = 'ko_uninstall_lang';
  var THEME_KEY = 'ko_uninstall_seed';
  var MODE_KEY = 'ko_uninstall_ui_mode';
  var LINK_RE = /^https?:\/\/[^\s|"'`;&<>]{1,300}$/;

  /* ---------- 文案 ---------- */
  var I18N = {
    zh: {
      LIVE: '在线', OFFLINE: '离线', CONN: '连接中',
      init_st: '读取中', init_ms: '连接 root 执行接口…',
      mode_unload: '卸载模式', mode_monitor: '监控模式',
      hint_unload: '开机即卸载 inte，使其失效', hint_monitor: '只观察不卸载，用于诊断。切回卸载即恢复防护',
      str_aggr: '激进 · 自适应', str_std: '标准',
      hint_aggr: '激进: 复活频繁时自动收紧复核间隔, 最多 8 轮卸载', hint_std: '标准: 固定复核间隔, 最多 5 轮卸载',
      guard_off: '主动防护 未运行', guard_found: '主动防护 盯防中·已发现', guard_watch: '主动防护 盯防中·',
      s_normal_ok: '已生效', s_normal_ok_ms: 'inte 未加载，内核完整性检测已失效',
      s_normal_warn: '待复核', s_normal_warn_ms: 'inte 仍在加载，守护正在处理',
      s_running: '处理中', s_running_ms: '正在卸载 inte',
      s_failed: '未生效', s_failed_ms: 'inte 无法卸载（可能被内核保护）',
      s_builtin: '无法处理', s_builtin_ms: 'inte 已编译进内核，rmmod 无效',
      s_notfound: '无需处理', s_notfound_ms: '本机型未检测到 inte 子系统',
      s_stopped: '已停止', s_stopped_ms: '守护进程已退出（重启后可能恢复）',
      s_daemonstop: '守护已停', s_daemonstop_ms: 'inte 已清除，但守护进程未运行（重启后可能恢复）',
      s_starting: '启动中', s_starting_ms: '守护进程初始化中，inte 当前处于加载状态',
      v_unloaded: '未加载', v_loaded: '加载中',
      v_daemon_up: '运行中', v_daemon_down: '已停止',
      detect_run: '检测中', detect_hit: '存在', detect_clean: '未确认',
      detect_resident: '驻留（正常）', detect_none: '未驻留', detect_feat: '有特征（静态）', detect_feat_none: '无特征',
      scan_idle: '实时扫描', scan_go: '正在扫描',
      prot_next: '下次复核', prot_off: '防护未运行', prot_work: '处理中',
      d_title: '重新检测', up_check: '检查更新', up_checking: '检查中…',
      up_cur: '当前版本', up_hint: '点击「检查更新」从 update.json 获取最新版本',
      up_new: '发现新版本', up_ok: '已是最新版本', up_none: '无法获取更新信息',
      up_zip: '跳转下载 / Release', up_open: '已尝试打开更新页',
      srv_on: '本地服务 · 开', srv_off: '本地服务 · 关', srv_starting: '启动中…', srv_stopping: '停止中…',
      srv_hint_on: '本地服务运行中: 浏览器打开 http://127.0.0.1:8080',
      srv_hint_off: '本地服务: 开启后在浏览器打开 http://127.0.0.1:8080 即可管理（需 busybox）',
      t_mode_ok: '模式已切换: ', t_mode_fail: '切换失败: ',
      t_iv_ok: '复核间隔已设为 ', t_iv_ok2: ' 秒', t_iv_fail: '设置失败: ',
      t_str_ok: '强度已切换: ', t_str_fail: '切换失败: ',
      t_act: '立即清除', t_act_do: '正在清除…', t_act_fail: '执行失败: ',
      t_ref: '已刷新', t_link_fail: '打开失败: ', t_link_ok: '已尝试打开',
      t_detect_ok: '检测完成', e_nobridge: '无法调用 root 执行接口', e_parse: '桥接返回内容异常', e_bridge: '桥接失败',
      t_theme_ok: '主题已切换', t_theme_fail: '主题切换失败',
      t_ui_ok: '明暗模式已切换', t_ui_fail: '明暗模式切换失败'
    },
    en: {
      LIVE: 'ONLINE', OFFLINE: 'OFFLINE', CONN: 'CONNECTING',
      init_st: 'Loading', init_ms: 'Connecting root exec bridge…',
      mode_unload: 'Unload', mode_monitor: 'Monitor',
      hint_unload: 'Unload inte on boot', hint_monitor: 'Watch only. Switch back to unload to resume',
      str_aggr: 'Aggressive · adaptive', str_std: 'Standard',
      hint_aggr: 'Aggressive: tighten interval on revival, up to 8 rounds', hint_std: 'Standard: fixed interval, up to 5 rounds',
      guard_off: 'Guard offline', guard_found: 'Guard · found', guard_watch: 'Guard · ',
      s_normal_ok: 'ACTIVE', s_normal_ok_ms: 'inte not loaded, kernel integrity check disabled',
      s_normal_warn: 'CHECKING', s_normal_warn_ms: 'inte still loaded, daemon working',
      s_running: 'WORKING', s_running_ms: 'unloading inte',
      s_failed: 'FAILED', s_failed_ms: 'inte protected, rmmod failed',
      s_builtin: 'BUILTIN', s_builtin_ms: 'inte built into kernel, rmmod ineffective',
      s_notfound: 'CLEAR', s_notfound_ms: 'inte subsystem not found on this device',
      s_stopped: 'STOPPED', s_stopped_ms: 'daemon exited (may recover on reboot)',
      s_daemonstop: 'DAEMON OFF', s_daemonstop_ms: 'inte cleared but daemon not running',
      s_starting: 'STARTING', s_starting_ms: 'daemon initializing, inte currently loaded',
      v_unloaded: 'unloaded', v_loaded: 'loaded',
      v_daemon_up: 'running', v_daemon_down: 'stopped',
      detect_run: 'checking', detect_hit: 'found', detect_clean: 'clean',
      detect_resident: 'resident (normal)', detect_none: 'not resident', detect_feat: 'feature (static)', detect_feat_none: 'no feature',
      scan_idle: 'live scan', scan_go: 'scanning',
      prot_next: 'next check', prot_off: 'guard off', prot_work: 'working',
      d_title: 'Re-detect', up_check: 'Check', up_checking: 'Checking…',
      up_cur: 'Current', up_hint: 'Tap to fetch latest from update.json',
      up_new: 'New version', up_ok: 'Up to date', up_none: 'Cannot fetch update info',
      up_zip: 'Open download / Release', up_open: 'Update page opened',
      srv_on: 'Local · ON', srv_off: 'Local · OFF', srv_starting: 'Starting…', srv_stopping: 'Stopping…',
      srv_hint_on: 'Local server: open http://127.0.0.1:8080 in browser',
      srv_hint_off: 'Local server: enable then open http://127.0.0.1:8080 (busybox required)',
      t_mode_ok: 'Mode: ', t_mode_fail: 'Mode failed: ',
      t_iv_ok: 'Interval set to ', t_iv_ok2: 's', t_iv_fail: 'Set failed: ',
      t_str_ok: 'Strength: ', t_str_fail: 'Switch failed: ',
      t_act: 'Purge now', t_act_do: 'Purging…', t_act_fail: 'Exec failed: ',
      t_ref: 'Refreshed', t_link_fail: 'Open failed: ', t_link_ok: 'Opened',
      t_detect_ok: 'Detected', e_nobridge: 'cannot reach root exec bridge', e_parse: 'bridge returned bad data', e_bridge: 'bridge error',
      t_theme_ok: 'Theme applied', t_theme_fail: 'Theme failed',
      t_ui_ok: 'UI mode set', t_ui_fail: 'UI mode failed'
    }
  };

  var lang = (function () {
    try { return localStorage.getItem(LANG_KEY) === 'en' ? 'en' : 'zh'; } catch (e) { return 'zh'; }
  })();
  function t(k) { return (I18N[lang] && I18N[lang][k]) || I18N.zh[k] || k; }

  function $(id) { return document.getElementById(id); }
  var el = {
    liveBox: $('liveBox'), led: $('led'), liveTxt: $('liveTxt'), chipVer: $('chipVer'),
    st: $('st'), ms: $('ms'), ringVal: $('ringVal'), metaGuard: $('metaGuard'),
    metaMode: $('metaMode'), metaStr: $('metaStr'),
    protK: $('protK'), protV: $('protV'), protFill: $('protFill'),
    vInte: $('vInte'), sInte: $('sInte'), vNodes: $('vNodes'), sNodes: $('sNodes'),
    vProc: $('vProc'), sProc: $('sProc'), vRun: $('vRun'), sRun: $('sRun'),
    vUnload: $('vUnload'), vRevive: $('vRevive'), vIv: $('vIv'), vStr: $('vStr'),
    btnAct: $('btnAct'), btnRef: $('btnRef'), btnServer: $('btnServer'),
    btnLink: $('btnLink'), btnRelease: $('btnRelease'),
    btnDetect: $('btnDetect'), detect: $('detect'), scanLine: $('scanLine'), scanFill: $('scanFill'),
    d_ko: $('d_ko'), d_systbl: $('d_systbl'), d_status: $('d_status'), d_mod: $('d_mod'),
    d_sysmod: $('d_sysmod'), d_ksym: $('d_ksym'), d_dev: $('d_dev'), d_file: $('d_file'),
    d_cmd: $('d_cmd'), d_ref: $('d_ref'), d_init: $('d_init'),
    segMode: $('segMode'), modeHint: $('modeHint'), segStr: $('segStr'), strHint: $('strHint'),
    segIv: $('segIv'), srvHint: $('srvHint'),
    palette: $('palette'), segTheme: $('segTheme'),
    btnUp: $('btnUp'), upCur: $('upCur'), upInfo: $('upInfo'), upNote: $('upNote'),
    upZipWrap: $('upZipWrap'), btnUpZip: $('btnUpZip'),
    log: $('log'), bootlog: $('bootlog'),
    vModel: $('vModel'), vBrand: $('vBrand'), vKernel: $('vKernel'), vRoot: $('vRoot'),
    vTargets: $('vTargets'), vUp: $('vUp'),
    diag: $('diag'), diagBody: $('diagBody'), footVer: $('footVer')
  };

  var bridge = null, bridgeHow = '', busy = false, lastData = null, srvState = 0;
  var lastIv = 5, lastDetect = {};
  var protAnim = null;

  /* ---------- Toast ---------- */
  var toastTimer = null;
  function toast(m, kind) {
    var old = document.querySelector('.toast');
    if (old) old.remove();
    var t0 = document.createElement('div');
    t0.className = 'toast';
    if (kind) t0.classList.add(kind);
    t0.textContent = m;
    document.body.appendChild(t0);
    requestAnimationFrame(function () { t0.classList.add('show'); });
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      t0.classList.remove('show');
      setTimeout(function () { t0.remove(); }, 350);
    }, 2000);
  }

  /* ---------- 桥接探测 ---------- */
  function useBridge(obj, how) {
    if (!obj || typeof obj.exec !== 'function') return false;
    bridge = {
      exec: function (cmd) { return Promise.resolve(obj.exec(cmd)); },
      toast: (typeof obj.toast === 'function') ? function (m) { try { obj.toast(m); } catch (e) {} } : null
    };
    bridgeHow = how;
    return true;
  }

  function probe() {
    var tried = [];
    try {
      if (window.ksu) {
        if (useBridge(window.ksu, 'window.ksu')) { tried.push('OK window.ksu'); return Promise.resolve(tried); }
        tried.push('window.ksu no exec');
      } else tried.push('no window.ksu');
    } catch (e) { tried.push('window.ksu error'); }
    try {
      if (typeof ksu !== 'undefined' && ksu) {
        if (useBridge(ksu, 'global ksu')) { tried.push('OK global ksu'); return Promise.resolve(tried); }
      } else tried.push('no global ksu');
    } catch (e) { tried.push('global ksu error'); }
    var alt = ['KsuInterface', 'ksuInterface', 'KSU', 'kernelsu', 'jsBridge', 'magisk', 'apatch'];
    for (var i = 0; i < alt.length; i++) {
      try {
        if (window[alt[i]] && typeof window[alt[i]].exec === 'function') {
          if (useBridge(window[alt[i]], 'window.' + alt[i])) { tried.push('OK ' + alt[i]); return Promise.resolve(tried); }
        }
      } catch (e) {}
    }
    return Promise.reject(new Error('no bridge found: ' + tried.join(', ')));
  }

  function sh(cmd) {
    if (!bridge) return Promise.reject(new Error('bridge unavailable'));
    return bridge.exec(cmd).then(function (r) {
      if (typeof r === 'string') return r;
      if (r && typeof r === 'object') return r.stdout || r.out || '';
      return String(r == null ? '' : r);
    }).catch(function (e) { throw new Error(String(e && e.message ? e.message : e)); });
  }

  /* ---------- 单行协议解析 ---------- */
  function parseLine(text) {
    var o = {};
    if (!text) return o;
    var lines = String(text).replace(/\r/g, '').split('\n').filter(function (s) { return s.trim(); });
    if (!lines.length) return o;
    lines[lines.length - 1].split('|').forEach(function (seg) {
      var p = seg.indexOf('=');
      if (p > 0) o[seg.slice(0, p).trim()] = seg.slice(p + 1);
    });
    return o;
  }

  function fmtDur(sec) {
    var n = parseInt(sec, 10);
    if (isNaN(n) || n < 0) return '--';
    var d = Math.floor(n / 86400), h = Math.floor((n % 86400) / 3600),
        m = Math.floor((n % 3600) / 60), s = n % 60;
    if (lang === 'zh') {
      if (d > 0) return d + ' 天 ' + h + ' 时';
      if (h > 0) return h + ' 时 ' + m + ' 分';
      if (m > 0) return m + ' 分 ' + s + ' 秒';
      return s + ' 秒';
    }
    if (d > 0) return d + 'd ' + h + 'h';
    if (h > 0) return h + 'h ' + m + 'm';
    if (m > 0) return m + 'm ' + s + 's';
    return s + 's';
  }

  /* ---------- 状态映射 ---------- */
  function statusText(d) {
    var lk = d.inte_loaded === '1';
    var run = d.run === '1';
    var state = d.state || 'unknown';
    if (run && (state === 'stopped' || state === 'notfound' || state === 'unknown')) {
      state = lk ? 'running' : 'normal';
    }
    if (state === 'normal') {
      return lk ? { ok: 'warn', head: t('s_normal_warn'), msg: t('s_normal_warn_ms') }
                : { ok: 'ok',   head: t('s_normal_ok'),   msg: t('s_normal_ok_ms') };
    }
    if (state === 'running')  return { ok: 'warn', head: t('s_running'), msg: t('s_running_ms') };
    if (state === 'failed')   return { ok: 'err',  head: t('s_failed'), msg: t('s_failed_ms') };
    if (state === 'builtin')  return { ok: 'err',  head: t('s_builtin'), msg: t('s_builtin_ms') };
    if (state === 'notfound') return { ok: 'info', head: t('s_notfound'), msg: t('s_notfound_ms') };
    if (state === 'stopped')  return { ok: 'warn', head: t('s_stopped'), msg: t('s_stopped_ms') };
    if (run && !lk) return { ok: 'warn', head: t('s_daemonstop'), msg: t('s_daemonstop_ms') };
    return lk ? { ok: 'warn', head: t('s_starting'), msg: t('s_starting_ms') }
              : { ok: 'ok',   head: t('s_normal_ok'), msg: t('s_normal_ok_ms') };
  }

  /* ---------- 主题系统 ---------- */
  var PALETTE = ['#22d3ee', '#38bdf8', '#818cf8', '#c084fc', '#f472b6',
                 '#fb7185', '#f87171', '#fb923c', '#fbbf24', '#facc15',
                 '#a3e635', '#34d399', '#2dd4bf', '#f9a8d4', '#fda4af'];
  function applySeed(seed) {
    try { localStorage.setItem(THEME_KEY, seed); } catch (e) {}
    document.documentElement.style.setProperty('--seed', seed);
    document.documentElement.style.setProperty('--primary', seed);
    document.documentElement.style.setProperty('--primary-deep', shade(seed, -24));
    document.documentElement.style.setProperty('--primary-soft', alpha(seed, .16));
    document.documentElement.style.setProperty('--primary-glow', alpha(seed, .45));
    var sw = el.palette.querySelectorAll('.swatch');
    sw.forEach(function (s) {
      s.classList.toggle('on', s.getAttribute('data-seed') === seed);
    });
  }
  function shade(hex, amt) {
    var n = parseInt(hex.slice(1), 16), r = Math.max(0, Math.min(255, (n >> 16) + amt)),
        g = Math.max(0, Math.min(255, ((n >> 8) & 255) + amt)),
        b = Math.max(0, Math.min(255, (n & 255) + amt));
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
  }
  function alpha(hex, a) {
    var n = parseInt(hex.slice(1), 16);
    return 'rgba(' + (n >> 16) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')';
  }
  function applyUiMode(mode) {
    try { localStorage.setItem(MODE_KEY, mode); } catch (e) {}
    var root = document.documentElement;
    if (mode === 'system') {
      var dark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.removeAttribute('data-theme');
      if (!dark) root.setAttribute('data-theme', 'light');
    } else {
      root.setAttribute('data-theme', mode === 'light' ? 'light' : 'dark');
      if (mode === 'dark') root.removeAttribute('data-theme');
    }
    var btns = el.segTheme.querySelectorAll('.seg-btn');
    btns.forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-theme') === mode); });
  }
  function buildPalette() {
    if (!el.palette) return;
    var cur = null;
    try { cur = localStorage.getItem(THEME_KEY); } catch (e) {}
    PALETTE.forEach(function (seed) {
      var s = document.createElement('button');
      s.className = 'swatch' + (seed === cur ? ' on' : '');
      s.setAttribute('data-seed', seed);
      s.style.background = 'linear-gradient(135deg, ' + seed + ', ' + shade(seed, -20) + ')';
      s.title = seed;
      s.addEventListener('click', function () {
        applySeed(seed);
        toast(t('t_theme_ok'), 'ok');
        try { window.ksu && window.ksu.toast && window.ksu.toast(t('t_theme_ok')); } catch (e) {}
      });
      el.palette.appendChild(s);
    });
    applySeed(cur || PALETTE[0]);
  }

  /* ---------- 渲染 ---------- */
  var RING_COLOR = { ok: ['#34d399', '#22d3ee', 'rgba(52,211,153,.4)'],
                     warn: ['#fbbf24', '#f97316', 'rgba(251,191,36,.4)'],
                     err: ['#f87171', '#ef4444', 'rgba(248,113,113,.4)'],
                     info: ['#60a5fa', '#3b82f6', 'rgba(96,165,250,.4)'],
                     neutral: ['#94a3b8', '#64748b', 'rgba(148,163,184,.3)'] };

  function render(d) {
    if (!d) return;
    lastData = d;
    var st = statusText(d);

    /* 顶栏 */
    var run = d.run === '1';
    el.led.className = 'led' + (run ? ' on' : (d.state === 'stopped' || d.state === 'notfound' ? ' off' : ' warn'));
    el.liveTxt.textContent = run ? t('LIVE') : t('OFFLINE');
    el.liveBox.classList.toggle('off', !run);

    /* hero */
    el.st.textContent = st.head;
    el.st.className = 'hero-pill ' + st.ok;
    el.ms.textContent = st.msg;
    var rc = RING_COLOR[st.ok] || RING_COLOR.neutral;
    el.ringVal.textContent = st.ok === 'ok' ? 'ON' : (st.ok === 'err' ? '!!' : '…');
    el.ringVal.style.color = rc[0];

    /* meta */
    var guard = '--', mode = '--', str = '--';
    if (d.guard === '1' || d.guard === '2') guard = t('guard_watch') + (d.next || '--') + 's';
    else if (d.run === '1') guard = t('guard_found');
    else guard = t('guard_off');
    if (d.mode === 'monitor') mode = t('mode_monitor'); else if (d.mode) mode = t('mode_unload');
    if (d.strength === '1') str = t('str_aggr'); else if (d.strength === '0') str = t('str_std');
    el.metaGuard.textContent = guard;
    el.metaMode.textContent = mode;
    el.metaStr.textContent = str;

    /* 进度条 */
    var next = parseInt(d.next, 10), iv = parseInt(d.interval, 10);
    if (d.guard === '1' || d.guard === '2') {
      el.protK.textContent = t('prot_next');
      if (!isNaN(next) && !isNaN(iv) && iv > 0) {
        el.protV.textContent = next + 's';
        el.protFill.style.width = Math.max(2, Math.min(100, (100 * next / iv))) + '%';
      } else { el.protV.textContent = '--'; el.protFill.style.width = '4%'; }
    } else if (d.run === '1' && d.inte_loaded === '1') {
      el.protK.textContent = t('prot_work'); el.protV.textContent = '…';
      el.protFill.style.width = '70%';
    } else {
      el.protK.textContent = t('prot_off'); el.protV.textContent = '--'; el.protFill.style.width = '0';
    }

    /* 指标卡 */
    setStat(el.vInte, el.sInte, d.inte_loaded === '1' ? t('v_loaded') : t('v_unloaded'),
            d.inte_loaded === '1' ? 'err' : 'ok', 'inte.ko');
    var nodes = parseInt(d.nodes, 10);
    setStat(el.vNodes, el.sNodes, isNaN(nodes) ? '--' : (nodes > 0 ? String(nodes) : '0/3'),
            nodes > 0 ? 'warn' : 'ok', '/proc/inte_*');
    setStat(el.vProc, el.sProc, d.run === '1' ? t('v_daemon_up') : t('v_daemon_down'),
            d.run === '1' ? 'ok' : 'warn', 'pid=' + (d.pid || '--'));
    var up = parseInt(d.uptime, 10);
    setStat(el.vRun, el.sRun, fmtDur(up), 'neutral', '本次运行');

    /* 统计 */
    el.vUnload.textContent = d.unload || '0';
    el.vRevive.textContent = d.revive || '0';
    el.vIv.textContent = (d.interval || '--') + 's';
    el.vStr.textContent = d.strength === '1' ? t('str_aggr') : t('str_std');

    /* 日志 */
    if (d.logs) renderLog(el.log, d.logs, 3);
    if (d.bootlogs) renderLog(el.bootlog, d.bootlogs, 6);
    if (d.bootlogs === '') el.bootlog.innerHTML = '<div class="empty">（暂无日志）</div>';

    /* 环境 */
    if (d.model) el.vModel.textContent = d.model;
    if (d.brand) el.vBrand.textContent = d.brand;
    if (d.kernel) el.vKernel.textContent = d.kernel;
    if (d.root) el.vRoot.textContent = d.root;
    if (d.targets) el.vTargets.textContent = d.targets;
    if (d.uptime) el.vUp.textContent = fmtDur(up);

    /* 控件态 */
    syncSeg(el.segMode, 'mode', d.mode === 'monitor' ? 'monitor' : 'unload');
    syncSeg(el.segStr, 'strength', d.strength === '1' ? '1' : '0');
    syncSeg(el.segIv, 'iv', String(d.interval || 5));
    el.modeHint.textContent = d.mode === 'monitor' ? t('hint_monitor') : t('hint_unload');
    el.strHint.textContent = d.strength === '1' ? t('hint_aggr') : t('hint_std');
  }
  function setStat(vEl, sEl, val, kind, sub) {
    vEl.textContent = val;
    vEl.className = 'stat-v ' + kind;
    sEl.textContent = sub || '';
  }
  function syncSeg(seg, attr, val) {
    if (!seg) return;
    var btns = seg.querySelectorAll('.seg-btn');
    btns.forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-' + attr) === val); });
  }
  function renderLog(container, logs, max) {
    if (!container || !logs) return;
    if (typeof logs === 'string') logs = logs.split('\n').filter(function (s) { return s.trim(); });
    var arr = logs.slice(-max);
    if (!arr.length) { container.innerHTML = '<div class="empty">（暂无日志）</div>'; return; }
    container.innerHTML = '';
    arr.forEach(function (line) {
      var d = document.createElement('div');
      var kind = '';
      if (/\[check\]|\[unload\]|\[alive\]/.test(line)) kind = 'ok';
      else if (/warn|失败|异常/.test(line)) kind = 'warn';
      else if (/error|fatal|err/.test(line)) kind = 'err';
      if (kind) d.className = kind;
      d.textContent = line;
      container.appendChild(d);
    });
  }

  /* ---------- 检测渲染 ---------- */
  function dstat(id, cls, txt) { var e = $(id); if (e) { e.className = 'dstat ' + cls; e.textContent = txt; } }
  function renderDetect(d) {
    var hit = function (v) { return v === '1'; };
    dstat('d_ko', hit(d.ko) ? 'err' : 'ok', hit(d.ko) ? t('detect_hit') : t('detect_clean'));
    dstat('d_systbl', hit(d.systbl) ? 'err' : 'ok', hit(d.systbl) ? t('detect_hit') : t('detect_clean'));
    dstat('d_status', hit(d.status) ? 'err' : 'ok', hit(d.status) ? t('detect_hit') : t('detect_clean'));
    dstat('d_mod', hit(d.mod) ? 'err' : 'ok', hit(d.mod) ? t('detect_hit') : t('detect_clean'));
    dstat('d_sysmod', hit(d.sysmod) ? 'err' : 'ok', hit(d.sysmod) ? t('detect_hit') : t('detect_clean'));
    dstat('d_ksym', hit(d.ksym) ? 'err' : 'ok', hit(d.ksym) ? t('detect_hit') : t('detect_clean'));
    dstat('d_dev', hit(d.dev) ? 'err' : 'ok', hit(d.dev) ? t('detect_hit') : t('detect_clean'));
    dstat('d_file', d.file === '1' ? 'info' : 'ok', d.file === '1' ? t('detect_resident') : t('detect_none'));
    dstat('d_cmd', d.cmd === '1' ? 'info' : 'ok', d.cmd === '1' ? t('detect_feat') : t('detect_feat_none'));
    dstat('d_ref', hit(d.ref) ? 'err' : 'ok', hit(d.ref) ? t('detect_hit') : t('detect_clean'));
    dstat('d_init', hit(d.init) ? 'err' : 'ok', hit(d.init) ? t('detect_hit') : t('detect_clean'));
    if (el.scanLine) el.scanLine.classList.remove('go');
    if (el.scanFill) el.scanFill.style.width = '0';
    if (el.btnDetect) el.btnDetect.textContent = t('d_title');
    toast(t('t_detect_ok'), 'ok');
  }
  function detectStart() {
    if (el.scanLine) el.scanLine.classList.add('go');
    if (el.btnDetect) el.btnDetect.textContent = '…';
  }

  /* ---------- 数据轮询 ---------- */
  function pollStatus() {
    if (!bridge) return;
    sh(STATUS_CMD).then(function (out) {
      var d = parseLine(out);
      if (Object.keys(d).length > 0) render(d);
      else { el.st.textContent = t('init_st'); el.ms.textContent = t('e_parse'); }
    }).catch(function (e) {
      el.st.textContent = t('OFFLINE');
      el.ms.textContent = t('e_bridge');
    });
  }

  /* ---------- 检测 ---------- */
  function runDetect() {
    if (busy) return;
    busy = true;
    detectStart();
    sh(DETECT_CMD).then(function (out) {
      var d = parseLine(out);
      renderDetect(d);
      lastDetect = d;
      busy = false;
    }).catch(function (e) {
      if (el.scanLine) el.scanLine.classList.remove('go');
      if (el.btnDetect) el.btnDetect.textContent = t('d_title');
      toast(t('e_bridge'), 'err');
      busy = false;
    });
  }

  /* ---------- 立即清除 ---------- */
  function actNow() {
    if (busy) return;
    busy = true;
    var old = el.btnAct.textContent;
    el.btnAct.textContent = t('t_act_do');
    sh(SET_CMD + ' clear').then(function () {
      busy = false;
      el.btnAct.textContent = old;
      toast(t('t_act'), 'ok');
      setTimeout(pollStatus, 600);
    }).catch(function (e) {
      busy = false;
      el.btnAct.textContent = old;
      toast(t('t_act_fail') + e.message, 'err');
    });
  }

  /* ---------- 本地服务 ---------- */
  function toggleServer() {
    if (busy) return;
    busy = true;
    var want = srvState === 1 ? 'stop' : 'start';
    el.btnServer.textContent = want === 'start' ? t('srv_starting') : t('srv_stopping');
    sh(SRV_CMD + ' ' + want + ' 8080').then(function (out) {
      var o = parseLine(out);
      busy = false;
      var running = /started/.test(out) || (o.server === '1');
      srvState = running ? 1 : 0;
      el.btnServer.textContent = srvState === 1 ? t('srv_on') : t('srv_off');
      el.srvHint.textContent = srvState === 1 ? t('srv_hint_on') : t('srv_hint_off');
    }).catch(function (e) {
      busy = false;
      el.btnServer.textContent = srvState === 1 ? t('srv_on') : t('srv_off');
      toast(t('e_bridge'), 'err');
    });
  }

  /* ---------- 设置 seg ---------- */
  function bindSeg(seg, key, cmdArg, okMsg, failMsg) {
    if (!seg) return;
    seg.addEventListener('click', function (ev) {
      var b = ev.target.closest('.seg-btn');
      if (!b || !b.getAttribute('data-' + key)) return;
      var val = b.getAttribute('data-' + key);
      if (b.classList.contains('on')) return;
      if (busy) return;
      busy = true;
      syncSeg(seg, key, val);
      sh(SET_CMD + ' ' + cmdArg + ' ' + val).then(function () {
        busy = false;
        toast(okMsg + val, 'ok');
        setTimeout(pollStatus, 400);
      }).catch(function (e) {
        busy = false;
        syncSeg(seg, key, lastData ? (key === 'mode' ? (lastData.mode === 'monitor' ? 'monitor' : 'unload')
                                : key === 'strength' ? (lastData.strength === '1' ? '1' : '0')
                                : String(lastData.interval || 5)) : null);
        toast(failMsg + e.message, 'err');
      });
    });
  }

  /* ---------- 在线升级 ---------- */
  function checkUpdate() {
    if (busy) return;
    busy = true;
    var old = el.btnUp.textContent;
    el.btnUp.textContent = t('up_checking');
    sh(UP_CMD).then(function (out) {
      var d = parseLine(out);
      busy = false;
      el.btnUp.textContent = old;
      el.upCur.textContent = d.latest || el.upCur.textContent;
      if (d.need === '1') {
        el.upInfo.textContent = t('up_new') + ': ' + d.latest + ' (code ' + (d.code || '') + ')';
        if (d.note) { el.upNote.textContent = d.note; el.upNote.classList.remove('hide'); }
        if (d.zip && LINK_RE.test(d.zip)) {
          el.upZipWrap.classList.remove('hide');
          el.btnUpZip.textContent = t('up_zip');
          el.btnUpZip.onclick = function () { openLink(d.zip); };
        }
        toast(t('up_new') + ' ' + d.latest, 'ok');
      } else if (d.need === '0') {
        el.upInfo.textContent = t('up_ok') + ' · ' + d.latest;
        el.upNote.classList.add('hide');
        el.upZipWrap.classList.add('hide');
      } else {
        el.upInfo.textContent = t('up_none');
      }
    }).catch(function (e) {
      busy = false;
      el.btnUp.textContent = old;
      el.upInfo.textContent = t('up_none');
    });
  }

  /* ---------- 打开链接 ---------- */
  function openLink(url) {
    if (!LINK_RE.test(url)) { toast(t('t_link_fail') + 'invalid', 'err'); return; }
    sh(LINK_CMD + ' "' + url + '"').then(function () {
      toast(t('t_link_ok'), 'ok');
    }).catch(function () { toast(t('t_link_fail'), 'err'); });
  }

  /* ---------- 页面切换 ---------- */
  var pages = { overview: 'page-overview', detect: 'page-detect', settings: 'page-settings' };
  function switchPage(name) {
    var navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-page') === name); });
    Object.keys(pages).forEach(function (k) {
      var p = $(pages[k]);
      if (!p) return;
      if (k === name) {
        p.hidden = false;
        p.classList.remove('enter');
        void p.offsetWidth;
        p.classList.add('enter');
      } else {
        p.hidden = true;
      }
    });
    if (name === 'detect' && Object.keys(lastDetect).length) renderDetect(lastDetect);
  }

  /* ---------- 初始化 ---------- */
  function init() {
    el.chipVer.textContent = 'v3.2';
    el.footVer.textContent = 'v3.2';
    el.btnServer.textContent = t('srv_off');
    el.srvHint.textContent = t('srv_hint_off');

    buildPalette();
    var uiMode = null;
    try { uiMode = localStorage.getItem(MODE_KEY); } catch (e) {}
    applyUiMode(uiMode || 'system');

    bindSeg(el.segMode, 'mode', 'setmode', t('t_mode_ok'), t('t_mode_fail'));
    bindSeg(el.segStr, 'strength', 'setstrength', t('t_str_ok'), t('t_str_fail'));
    bindSeg(el.segIv, 'iv', 'setinterval', t('t_iv_ok') + '', t('t_iv_fail'));

    var navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(function (b) {
      b.addEventListener('click', function () { switchPage(b.getAttribute('data-page')); });
    });
    if (el.btnDetect) el.btnDetect.addEventListener('click', runDetect);
    if (el.btnAct) el.btnAct.addEventListener('click', actNow);
    if (el.btnServer) el.btnServer.addEventListener('click', toggleServer);
    if (el.btnRef) el.btnRef.addEventListener('click', function () { pollStatus(); toast(t('t_ref'), 'ok'); });
    if (el.btnUp) el.btnUp.addEventListener('click', checkUpdate);
    if (el.btnLink) el.btnLink.addEventListener('click', function () { openLink('https://github.com/kazaf080726/ko_uninstall'); });
    if (el.btnRelease) el.btnRelease.addEventListener('click', function () { openLink('https://github.com/kazaf080726/ko_uninstall/releases'); });
    if (el.langToggle) { /* keep optional */ }

    /* 明暗模式 */
    if (el.segTheme) {
      el.segTheme.addEventListener('click', function (ev) {
        var b = ev.target.closest('.seg-btn');
        if (!b) return;
        var m = b.getAttribute('data-theme');
        applyUiMode(m);
        toast(t('t_ui_ok'), 'ok');
      });
    }

    probe().then(function () {
      el.liveTxt.textContent = t('CONN');
      pollStatus();
      setInterval(pollStatus, POLL_MS);
    }).catch(function (e) {
      el.liveTxt.textContent = t('OFFLINE');
      el.led.className = 'led off';
      el.st.textContent = t('OFFLINE');
      el.ms.textContent = t('e_nobridge');
      if (el.diag) {
        el.diag.classList.remove('hide');
        el.diagBody.textContent = String(e && e.message ? e.message : e);
      }
      try { window.ksu && window.ksu.toast && window.ksu.toast(t('e_nobridge')); } catch (e2) {}
    });

    /* 系统明暗变化跟随 */
    if (window.matchMedia) {
      try {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
          var m = null;
          try { m = localStorage.getItem(MODE_KEY); } catch (e) {}
          if (!m || m === 'system') applyUiMode('system');
        });
      } catch (e) {}
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
