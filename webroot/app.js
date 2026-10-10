/* ==========================================================================
 * KO卸载 · WebUI 逻辑 (液态玻璃控制台 · 中英双语)
 * 桥接: window.ksu.exec() 只返回 stdout 最后一行 -> 单行 k=v|k=v 协议
 * 命令: webstatus.sh 轮询 / detect.sh 内核检测 / checkupdate.sh 在线升级
 *       server.sh 本地服务 / action.sh 立即清除 / open_link.sh 打开链接
 * ========================================================================== */
(function () {
  'use strict';

  var POLL_MS = 2000;
  var MODDIR = '/data/adb/modules/ko_uninstall';
  var STATUS_CMD = 'sh ' + MODDIR + '/bin/webstatus.sh';
  var DETECT_CMD = 'sh ' + MODDIR + '/bin/detect.sh';
  var UP_CMD = 'sh ' + MODDIR + '/bin/checkupdate.sh';
  var LANG_KEY = 'ko_uninstall_lang';
  var LINK_RE = /^https?:\/\/[^\s|"'`;&<>]{1,300}$/;

  /* ---------- 文案 ---------- */
  var I18N = {
    zh: {
      LIVE: '在线', OFFLINE: '离线',
      init_st: '读取中', init_ms: '连接 root 执行接口…',
      mode_unload: '卸载模式', mode_monitor: '监控模式',
      hint_unload: '开机即卸载 inte，使其失效', hint_monitor: '只观察不卸载，用于诊断。切回卸载即恢复防护',
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
      d_title: '重新检测', up_check: '检查更新', up_checking: '检查中…',
      up_cur: '当前版本', up_hint: '点击「检查更新」从 update.json 获取最新版本',
      up_new: '发现新版本', up_ok: '已是最新版本', up_none: '无法获取更新信息',
      up_zip: '跳转下载 / Release', up_open: '已尝试打开更新页',
      srv_on: '本地服务 · 开', srv_off: '本地服务 · 关', srv_starting: '启动中…', srv_stopping: '停止中…',
      srv_hint_on: '本地服务运行中: 浏览器打开 http://127.0.0.1:8080',
      srv_hint_off: '本地服务: 开启后在浏览器打开 http://127.0.0.1:8080 即可管理（需 busybox）',
      t_mode_ok: '模式已切换: ', t_mode_fail: '切换失败: ',
      t_iv_ok: '复核间隔已设为 ', t_iv_ok2: ' 秒', t_iv_fail: '设置失败: ',
      t_act: '已执行清除', t_act_fail: '执行失败: ',
      t_ref: '已刷新', t_link_fail: '打开失败: ', t_link_ok: '已尝试打开',
      t_detect_ok: '检测完成', e_nobridge: '无法调用 root 执行接口', e_parse: '桥接返回内容异常', e_bridge: '桥接失败',
      log_empty: '（暂无日志）', diag_empty: '（无）', by: 'Kazaf',
      nodes_cap: '/proc/inte_*', daemon_cap: 'daemon', run_cap: '本次运行', inte_cap: 'inte 模块'
    },
    en: {
      LIVE: 'LIVE', OFFLINE: 'OFFLINE',
      init_st: 'LOADING', init_ms: 'connecting root exec…',
      mode_unload: 'UNLOAD', mode_monitor: 'MONITOR',
      hint_unload: 'remove inte at boot ｜ monitor: observe only', hint_monitor: 'observe only, for diagnostics. Switch back to restore',
      guard_off: 'guard idle', guard_found: 'guard watching·found', guard_watch: 'guard watching·',
      s_normal_ok: 'ACTIVE', s_normal_ok_ms: 'inte not loaded, integrity check disabled',
      s_normal_warn: 'PENDING', s_normal_warn_ms: 'inte still loaded, daemon handling',
      s_running: 'WORKING', s_running_ms: 'removing inte now',
      s_failed: 'FAILED', s_failed_ms: 'inte could not be removed (kernel protected?)',
      s_builtin: 'BUILTIN', s_builtin_ms: 'inte built into kernel, rmmod ineffective',
      s_notfound: 'N/A', s_notfound_ms: 'no inte subsystem on this device',
      s_stopped: 'STOPPED', s_stopped_ms: 'daemon exited (may return after reboot)',
      s_daemonstop: 'DAEMON DOWN', s_daemonstop_ms: 'inte cleared, but daemon not running',
      s_starting: 'STARTING', s_starting_ms: 'daemon initializing, inte currently loaded',
      v_unloaded: 'UNLOADED', v_loaded: 'LOADED',
      v_daemon_up: 'UP', v_daemon_down: 'DOWN',
      detect_run: '…', detect_hit: 'EXISTS', detect_clean: 'N/A',
      d_title: 're-detect', up_check: 'CHECK', up_checking: 'CHECKING…',
      up_cur: 'current', up_hint: 'check update.json for latest version',
      up_new: 'new version', up_ok: 'up to date', up_none: 'cannot fetch update info',
      up_zip: 'DOWNLOAD / RELEASE', up_open: 'trying to open release page',
      srv_on: 'Server · ON', srv_off: 'Server · OFF', srv_starting: 'Starting…', srv_stopping: 'Stopping…',
      srv_hint_on: 'server running: open http://127.0.0.1:8080 in browser',
      srv_hint_off: 'local server: enables management from any browser at http://127.0.0.1:8080 (needs busybox)',
      t_mode_ok: 'mode -> ', t_mode_fail: 'switch failed: ',
      t_iv_ok: 'interval -> ', t_iv_ok2: 's', t_iv_fail: 'set failed: ',
      t_act: 'executed', t_act_fail: 'exec failed: ',
      t_ref: 'refreshed', t_link_fail: 'open failed: ', t_link_ok: 'trying to open',
      t_detect_ok: 'detected', e_nobridge: 'cannot reach root exec bridge', e_parse: 'bridge returned bad data', e_bridge: 'bridge error',
      log_empty: '(no log)', diag_empty: '(none)', by: 'Kazaf',
      nodes_cap: '/proc/inte_*', daemon_cap: 'daemon', run_cap: 'runtime', inte_cap: 'inte module'
    }
  };

  var lang = 'zh';
  try { lang = localStorage.getItem(LANG_KEY) || 'zh'; } catch (e) {}
  if (lang !== 'en') lang = 'zh';
  function t(k) { return (I18N[lang] && I18N[lang][k] != null) ? I18N[lang][k] : k; }

  function $(id) { return document.getElementById(id); }
  var el = {
    hero: $('hero'), st: $('st'), ms: $('ms'), liveBox: $('liveBox'), led: $('led'), liveTxt: $('liveTxt'),
    chipVer: $('chipVer'), footVer: $('footVer'), ring: $('ring'), ringVal: $('ringVal'), ringCap: $('ringCap'),
    metaGuard: $('metaGuard'), metaMode: $('metaMode'),
    vInte: $('vInte'), sInte: $('sInte'), vNodes: $('vNodes'), sNodes: $('sNodes'),
    vProc: $('vProc'), sProc: $('sProc'), vRun: $('vRun'), sRun: $('sRun'),
    vModel: $('vModel'), vBrand: $('vBrand'), vKernel: $('vKernel'), vRoot: $('vRoot'),
    vTargets: $('vTargets'), vUp: $('vUp'),
    segMode: $('segMode'), segIv: $('segIv'), modeHint: $('modeHint'),
    btnAct: $('btnAct'), btnRef: $('btnRef'), btnServer: $('btnServer'), srvHint: $('srvHint'),
    btnLink: $('btnLink'), btnRelease: $('btnRelease'),
    btnDetect: $('btnDetect'), detect: $('detect'),
    d_ko: $('d_ko'), d_systbl: $('d_systbl'), d_status: $('d_status'), d_mod: $('d_mod'), d_sysmod: $('d_sysmod'), d_ksym: $('d_ksym'),
    btnUp: $('btnUp'), upCur: $('upCur'), upInfo: $('upInfo'), upNote: $('upNote'), upZipWrap: $('upZipWrap'), btnUpZip: $('btnUpZip'),
    log: $('log'), langToggle: $('langToggle'),
    diag: $('diag'), diagBody: $('diagBody'), toast: $('toast')
  };

  var bridge = null, bridgeHow = '', busy = false, lastData = null, srvState = 0;

  /* ---------- Toast ---------- */
  var toastTimer = null;
  function toast(m) {
    if (!el.toast) return;
    el.toast.textContent = m;
    el.toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.toast.classList.remove('show'); }, 2200);
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
    return import('kernelsu').then(function (m) {
      if (useBridge(m, "import('kernelsu')")) { tried.push('OK import'); return tried; }
      if (m && m.default && useBridge(m.default, "import().default")) { tried.push('OK import.default'); return tried; }
      return tried;
    }).catch(function (e) { tried.push('import failed: ' + (e && e.message ? e.message : e)); return tried; });
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

  /* ---------- 渲染 ---------- */
  var RING_COLOR = { ok: ['#34d399', '#22d3ee', 'rgba(52,211,153,0.4)'],
                     warn: ['#fbbf24', '#f97316', 'rgba(251,191,36,0.4)'],
                     err: ['#f87171', '#ef4444', 'rgba(248,113,113,0.4)'],
                     info: ['#38bdf8', '#22d3ee', 'rgba(56,189,248,0.4)'] };
  var RING_VAL = { ok: '安全', warn: '处理', err: '失效', info: '无需' };

  function guardText(d) {
    if (d.run !== '1') return t('guard_off');
    if (d.inte_loaded === '1') return t('guard_found');
    return t('guard_watch') + (d.interval || '5') + 's';
  }

  function render(d) {
    lastData = d;
    if (d.version) { el.chipVer.textContent = d.version; el.footVer.textContent = d.version; }
    if (d.author) { /* 已静态 */ }

    var s = statusText(d);
    el.hero.classList.remove('ok', 'warn', 'err', 'info');
    el.hero.classList.add(s.ok);
    el.st.textContent = s.head;
    el.st.style.animation = 'none';
    void el.st.offsetWidth;
    el.st.style.animation = '';
    el.ms.textContent = s.msg;
    el.metaMode.textContent = (d.mode === 'monitor') ? t('mode_monitor') : t('mode_unload');
    el.metaGuard.textContent = guardText(d);

    // 状态环
    var rc = RING_COLOR[s.ok] || RING_COLOR.info;
    el.ring.style.setProperty('--ring-a', rc[0]);
    el.ring.style.setProperty('--ring-b', rc[1]);
    el.ring.style.setProperty('--ring-glow', rc[2]);
    el.ringVal.textContent = RING_VAL[s.ok] || '--';
    el.ringCap.textContent = t('nodes_cap');

    // 指标
    setStat(el.vInte, (d.inte_loaded === '1') ? t('v_loaded') : t('v_unloaded'),
            d.inte_loaded === '1' ? 'err' : 'ok', d.targets || '-');
    setStat(el.vNodes, (d.nodes || '0') + '/3', d.nodes === '0' ? 'ok' : 'warn', t('nodes_cap'));
    setStat(el.vProc, (d.run === '1') ? (t('v_daemon_up') + ' pid=' + d.pid) : t('v_daemon_down'),
            d.run === '1' ? 'ok' : 'warn', t('daemon_cap'));
    setStat(el.vRun, fmtDur(d.runtime), '', t('run_cap'));

    el.vModel.textContent = d.model || '--';
    el.vBrand.textContent = d.brand || '--';
    el.vKernel.textContent = d.kernel || '--';
    el.vRoot.textContent = d.root || '--';
    el.vTargets.textContent = d.targets || '-';
    el.vUp.textContent = fmtDur(d.uptime);

    // 本地服务状态
    srvState = (d.server === '1') ? 1 : 0;
    el.btnServer.textContent = srvState ? t('srv_on') : t('srv_off');
    el.srvHint.textContent = srvState ? t('srv_hint_on') : t('srv_hint_off');

    // 日志
    if (d.log3) {
      el.log.textContent = d.log3.replace(/\|/g, '\n');
    } else if (d.log) {
      el.log.textContent = d.log;
    } else {
      el.log.textContent = t('log_empty');
    }

    // 模式 / 间隔高亮
    var mb = el.segMode.querySelectorAll('button');
    for (var i = 0; i < mb.length; i++) mb[i].classList.toggle('on', mb[i].getAttribute('data-mode') === (d.mode || 'unload'));
    el.modeHint.textContent = (d.mode === 'monitor') ? t('hint_monitor') : t('hint_unload');
    var ib = el.segIv.querySelectorAll('button');
    for (var j = 0; j < ib.length; j++) ib[j].classList.toggle('on', ib[j].getAttribute('data-iv') === (d.interval || '5'));

    // 检测面板 (若协议带 maple 字段)
    var m = { ko: d.m_ko, systbl: d.m_systbl, status: d.m_status, mod: d.m_mod, sysmod: d.m_sysmod, ksym: d.ksym };
    var has = (d.m_ko !== undefined && d.m_ko !== '');
    if (has) applyDetect(m);
  }

  function setStat(elv, txt, cls, sub) {
    elv.textContent = txt;
    elv.className = 'stat-v' + (cls ? ' ' + cls : '');
    var sib = elv.nextElementSibling;
    if (sib) sib.textContent = sub || '';
  }

  /* ---------- 检测面板渲染 ---------- */
  function applyDetect(m) {
    setD(el.d_ko, m.ko);
    setD(el.d_systbl, m.systbl);
    setD(el.d_status, m.status);
    setD(el.d_mod, m.mod);
    setD(el.d_sysmod, m.sysmod);
    setD(el.d_ksym, m.ksym);
  }
  function setD(node, v) {
    if (v === '1') { node.textContent = t('detect_hit'); node.className = 'dstat hit'; }
    else if (v === '0') { node.textContent = t('detect_clean'); node.className = 'dstat clean'; }
    else { node.textContent = t('detect_run'); node.className = 'dstat unknown'; }
  }

  /* ---------- 轮询 ---------- */
  function refresh() {
    if (!bridge) return;
    return sh(STATUS_CMD).then(function (out) {
      var d = parseLine(out);
      if (!Object.keys(d).length) {
        el.st.textContent = 'ERR'; el.ms.textContent = t('e_parse');
        el.diag.classList.remove('hide'); el.diagBody.textContent = out || t('diag_empty');
        return;
      }
      el.diag.classList.add('hide');
      render(d);
    }).catch(function (e) {
      el.st.textContent = 'ERR'; el.ms.textContent = t('e_bridge');
      el.diag.classList.remove('hide'); el.diagBody.textContent = e.message;
    });
  }

  function setLive(on, err) {
    el.liveBox.classList.remove('on', 'err');
    if (err) { el.liveBox.classList.add('err'); }
    else if (on) { el.liveBox.classList.add('on'); }
    el.liveTxt.textContent = on ? t('LIVE') : (err ? 'ERR' : t('OFFLINE'));
  }

  /* ---------- 操作 ---------- */
  function setMode(m) {
    if (busy) return; busy = true;
    sh("echo " + m + " > " + MODDIR + "/data/mode")
      .then(function () { toast(t('t_mode_ok') + (m === 'monitor' ? t('mode_monitor') : t('mode_unload'))); return refresh(); })
      .catch(function (e) { toast(t('t_mode_fail') + e.message); })
      .then(function () { busy = false; });
  }
  function setIv(iv) {
    if (busy) return; busy = true;
    sh("echo " + iv + " > " + MODDIR + "/data/interval")
      .then(function () { toast(t('t_iv_ok') + iv + t('t_iv_ok2')); return refresh(); })
      .catch(function (e) { toast(t('t_iv_fail') + e.message); })
      .then(function () { busy = false; });
  }
  function manualAct() {
    if (busy) return; busy = true;
    el.btnAct.disabled = true; el.btnAct.classList.add('loading');
    el.btnAct.innerHTML = '<span class="spin"></span>' + t('t_act');
    sh("sh " + MODDIR + "/action.sh")
      .then(function () { toast(t('t_act')); return refresh(); })
      .catch(function (e) { toast(t('t_act_fail') + e.message); })
      .then(function () {
        busy = false; el.btnAct.disabled = false; el.btnAct.classList.remove('loading');
        el.btnAct.textContent = t('s_running') === '处理中' ? '立即清除' : 'EXECUTE';
      });
  }
  function toggleServer() {
    if (busy) return; busy = true;
    var act = srvState ? 'stop' : 'start';
    el.btnServer.disabled = true;
    el.btnServer.textContent = act === 'start' ? t('srv_starting') : t('srv_stopping');
    sh("sh " + MODDIR + "/bin/server.sh " + act)
      .then(function () { return refresh(); })
      .catch(function (e) { toast(t('t_act_fail') + e.message); })
      .then(function () { busy = false; el.btnServer.disabled = false; });
  }
  function runDetect() {
    if (!bridge) return;
    var rows = el.detect.querySelectorAll('.dstat');
    for (var i = 0; i < rows.length; i++) { rows[i].textContent = t('detect_run'); rows[i].className = 'dstat unknown'; }
    sh(DETECT_CMD).then(function (out) {
      var d = parseLine(out);
      if (d.ko !== undefined) { applyDetect(d); toast(t('t_detect_ok')); }
      else { toast(t('e_parse')); }
    }).catch(function (e) { toast(t('e_bridge')); el.diag.classList.remove('hide'); el.diagBody.textContent = e.message; });
  }
  function checkUpdate() {
    if (busy) return; busy = true;
    el.btnUp.disabled = true; el.btnUp.classList.add('loading');
    el.btnUp.innerHTML = '<span class="spin"></span>' + t('up_checking');
    el.upNote.classList.add('hide'); el.upZipWrap.classList.add('hide');
    el.upInfo.classList.remove('new');
    el.upInfo.textContent = t('up_checking');
    sh(UP_CMD).then(function (out) {
      var d = parseLine(out);
      if (d.need === '1') {
        el.upInfo.textContent = t('up_new') + ': ' + d.latest + ' (code ' + d.code + ')';
        el.upInfo.classList.add('new');
        el.upNote.textContent = d.note ? ('更新日志: ' + d.note) : '';
        el.upNote.classList.remove('hide');
        el.upZipWrap.classList.remove('hide');
        el.upZipWrap.style.display = 'flex';
      } else if (d.need === '0') {
        el.upInfo.textContent = d.latest && d.latest !== d.current ? t('up_ok') + ' · 最新 ' + d.latest : t('up_ok');
        if (d.note) { el.upNote.textContent = d.note; el.upNote.classList.remove('hide'); }
      } else {
        el.upInfo.textContent = t('up_none');
      }
    }).catch(function (e) {
      el.upInfo.textContent = t('up_none');
      el.diag.classList.remove('hide'); el.diagBody.textContent = e.message;
    }).then(function () {
      busy = false; el.btnUp.disabled = false; el.btnUp.classList.remove('loading');
      el.btnUp.textContent = t('up_check');
    });
  }
  function openLink(url) {
    if (!bridge) return Promise.resolve();
    if (!LINK_RE.test(url || '')) return Promise.resolve();
    return sh("am start --user 0 -a android.intent.action.VIEW -d '" + url + "'");
  }
  function openProject() {
    sh('head -n 1 ' + MODDIR + '/data/link 2>/dev/null').then(function (r) {
      var u = String(r || '').replace(/\r/g, '').split('\n')[0].trim();
      if (!LINK_RE.test(u)) u = 'https://github.com';
      return openLink(u);
    }).then(function () { toast(t('t_link_ok')); })
      .catch(function (e) { toast(t('t_link_fail') + e.message); });
  }
  function openRelease() {
    sh('head -n 1 ' + MODDIR + '/data/link 2>/dev/null').then(function (r) {
      var u = String(r || '').replace(/\r/g, '').split('\n')[0].trim();
      if (!LINK_RE.test(u)) u = 'https://github.com';
      u = u.replace(/\/+$/, '') + '/releases/latest';
      return openLink(u);
    }).then(function () { toast(t('up_open')); })
      .catch(function (e) { toast(t('t_link_fail') + e.message); });
  }
  function openUpZip() {
    // 直接打开 release 页 (若协议里带 zip, 优先打开 zip 下载)
    var zip = lastData && lastData.zip ? lastData.zip : '';
    if (LINK_RE.test(zip)) {
      openLink(zip).then(function () { toast(t('up_open')); })
        .catch(function (e) { toast(t('t_link_fail') + e.message); });
    } else {
      openRelease();
    }
  }

  /* ---------- 语言 ---------- */
  function toggleLang() {
    lang = (lang === 'zh') ? 'en' : 'zh';
    try { localStorage.setItem(LANG_KEY, lang); } catch (e) {}
    document.documentElement.lang = (lang === 'zh') ? 'zh-CN' : 'en';
    el.langToggle.textContent = (lang === 'zh') ? 'EN' : '中';
    if (lastData) render(lastData);
    if (!bridge) { el.st.textContent = t('init_st'); el.ms.textContent = t('init_ms'); }
    el.srvHint.textContent = srvState ? t('srv_hint_on') : t('srv_hint_off');
  }

  /* ---------- 事件绑定 ---------- */
  el.langToggle.addEventListener('click', toggleLang);
  el.segMode.addEventListener('click', function (e) {
    var b = e.target.closest('button');
    if (!b) return;
    var m = b.getAttribute('data-mode');
    if (m) setMode(m);
  });
  el.segIv.addEventListener('click', function (e) {
    var b = e.target.closest('button');
    if (!b) return;
    var iv = b.getAttribute('data-iv');
    if (iv) setIv(iv);
  });
  el.btnAct.addEventListener('click', manualAct);
  el.btnRef.addEventListener('click', function () { refresh().then(function () { toast(t('t_ref')); }).catch(function () {}); });
  el.btnServer.addEventListener('click', toggleServer);
  el.btnDetect.addEventListener('click', runDetect);
  el.btnLink.addEventListener('click', openProject);
  el.btnRelease.addEventListener('click', openRelease);
  el.btnUpZip.addEventListener('click', openUpZip);
  el.btnUp.addEventListener('click', checkUpdate);

  /* ---------- 启动 ---------- */
  toggleLang(); // 初始化语言文案
  probe().then(function (tried) {
    if (!bridge) {
      el.st.textContent = 'NO BRIDGE'; el.ms.textContent = t('e_nobridge');
      setLive(false);
      el.diag.classList.remove('hide'); el.diagBody.textContent = tried.join('\n');
      return;
    }
    setLive(true);
    refresh();
    setInterval(refresh, POLL_MS);
  });
})();
