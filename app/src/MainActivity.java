package com.kazaf.kouninstall;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * KO卸载 v2.0 管理 APP
 * 直读守护进程状态缓存 (data/status), WebUI 之外的独立原生管理入口
 * 作者: Kazaf
 */
public class MainActivity extends Activity {

    private static final String MOD = "/data/adb/modules/ko_uninstall";
    private static final String DATA = MOD + "/data";
    private static final long POLL_MS = 2000L;

    // 配色
    private static final int C_BG1 = 0xFF0B1020, C_BG2 = 0xFF111A30;
    private static final int C_CARD = 0xFF18213D, C_CARD_LINE = 0x22FFFFFF;
    private static final int C_TXT = 0xFFE7ECFF, C_TXT2 = 0xFF93A0C4, C_TXT3 = 0xFF5E6888;
    private static final int C_OK = 0xFF34D399, C_WARN = 0xFFFBBF24, C_ERR = 0xFFF87171, C_INFO = 0xFF38BDF8;
    private static final int C_CHIP_OK = 0x2434D399, C_CHIP_WARN = 0x24FBBF24, C_CHIP_ERR = 0x24F87171, C_CHIP_NEU = 0x2438BDF8;

    private Handler ui = new Handler(Looper.getMainLooper());
    private ExecutorService exe = Executors.newSingleThreadExecutor();
    private Runnable poller;
    private boolean busy = false, rootOk = false, modOk = false;
    private long lastIv = 5L;

    // 视图
    private TextView liveDot, vVersion, headText, msgText, guardLine;
    private TextView vInte, vNodes, vProc, vRun, vUnload, vRevive, vMode, vIv, vStr;
    private TextView[] dimChips = new TextView[11];
    private TextView logText;
    private TextView bootText;
    private Button bClear, bServer, bUp;
    private int serverOn = 0;
    private LinearLayout col;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        // 首帧强制绘制背景, 防止窗口空绘制黑屏 (OPLUS Empty window 检测)
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(C_BG1));
        w.setStatusBarColor(C_BG1);
        w.setNavigationBarColor(C_BG1);
        buildUi();
        startPoll();
    }

    /* ================= UI ================= */

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{C_BG1, C_BG2}));

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        col.setPadding(p, dp(8), p, dp(24));

        // ---- header ----
        LinearLayout hd = new LinearLayout(this);
        hd.setOrientation(LinearLayout.HORIZONTAL);
        hd.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = tv("KO卸载", 22, Typeface.BOLD, C_TXT);
        liveDot = tv("● 离线", 13, Typeface.BOLD, C_TXT3);
        liveDot.setGravity(Gravity.CENTER);
        GradientDrawable ldbg = new GradientDrawable();
        ldbg.setCornerRadius(dp(14));
        ldbg.setColor(0x24111A30);
        liveDot.setBackground(ldbg);
        liveDot.setPadding(dp(10), dp(5), dp(10), dp(5));
        vVersion = tv("v3.0-beta", 12, Typeface.NORMAL, C_TXT3);
        vVersion.setGravity(Gravity.CENTER);
        vVersion.setBackground(ldbg);
        vVersion.setPadding(dp(10), dp(5), dp(10), dp(5));
        hd.addView(title, lp(0, dp(40), 1f));
        hd.addView(vVersion);
        hd.addView(liveDot, lp(dp(4), dp(40), 0f));
        col.addView(hd);

        // ---- hero ----
        LinearLayout hero = card();
        headText = tv("读取中…", 26, Typeface.BOLD, C_TXT);
        msgText = tv("连接 root 执行接口…", 13, Typeface.NORMAL, C_TXT2);
        guardLine = tv("主动防护 读取中", 12, Typeface.BOLD, C_TXT2);
        hero.addView(headText);
        hero.addView(msgText, lp(-1, -2, 0f));
        hero.addView(guardLine, lp(-1, -2, 0f, dp(6)));
        col.addView(hero, lp(-1, -2, 0f, dp(12)));

        // ---- metrics 2x2 ----
        LinearLayout m1 = row();
        vInte = cell(m1, "inte 模块", "--");
        vNodes = cell(m1, "/proc 节点", "--/3");
        col.addView(m1, lp(-1, -2, 0f, dp(10)));
        LinearLayout m2 = row();
        vProc = cell(m2, "守护进程", "--");
        vRun = cell(m2, "本次运行", "--");
        col.addView(m2, lp(-1, -2, 0f, dp(10)));

        // ---- stats ----
        LinearLayout st = card();
        st.setOrientation(LinearLayout.HORIZONTAL);
        st.setGravity(Gravity.CENTER_VERTICAL);
        vUnload = statCell(st, "累计卸载", "0");
        vRevive = statCell(st, "复活次数", "0");
        vMode = statCell(st, "模式", "--");
        vIv = statCell(st, "间隔", "--");
        vStr = statCell(st, "强度", "--");
        col.addView(st, lp(-1, -2, 0f, dp(12)));

        // ---- 检测 12 维 ----
        LinearLayout dc = card();
        LinearLayout dhead = new LinearLayout(this);
        dhead.setOrientation(LinearLayout.HORIZONTAL);
        dhead.setGravity(Gravity.CENTER_VERTICAL);
        dhead.addView(tv("内核检测 · 12 维", 15, Typeface.BOLD, C_TXT), lp(0, dp(40), 1f));
        Button bDetect = btn("重新检测", C_INFO);
        bDetect.setOnClickListener(v -> runDetect());
        dhead.addView(bDetect);
        dc.addView(dhead);
        String[] labels = {
                "/proc/inte_ko", "/proc/inte_systbl", "/proc/inte_status",
                "/proc/modules·inte", "/sys/module/inte", "kallsyms·inte",
                "/dev·inte 节点", "inte.ko 文件驻留", "内核 cmdline 特征",
                "refcnt", "initstate"
        };
        for (int i = 0; i < labels.length; i++) {
            LinearLayout r = row();
            r.addView(tv(labels[i], 12, Typeface.NORMAL, C_TXT2), lp(0, dp(30), 1f));
            TextView chip = tv("检测中", 12, Typeface.BOLD, C_TXT3);
            GradientDrawable cbg = new GradientDrawable();
            cbg.setCornerRadius(dp(11));
            cbg.setColor(0x18111A30);
            chip.setBackground(cbg);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(4), dp(12), dp(4));
            r.addView(chip);
            dimChips[i] = chip;
            dc.addView(r, lp(-1, -2, 0f, dp(4)));
        }
        col.addView(dc, lp(-1, -2, 0f, dp(12)));

        // ---- 控制 ----
        LinearLayout cc = card();
        cc.addView(tv("控制", 15, Typeface.BOLD, C_TXT));
        cc.addView(tv("模式", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(4)));
        cc.addView(segment(new String[]{"卸载模式", "监控模式"}, new int[]{0, 1}, v -> setMode(v == 0 ? "unload" : "monitor")));
        cc.addView(tv("复核间隔", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(10)));
        cc.addView(segment(new String[]{"3s", "5s", "10s", "30s", "60s"}, new int[]{3, 5, 10, 30, 60}, v -> setIv(v)));
        cc.addView(tv("屏蔽强度", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(10)));
        cc.addView(segment(new String[]{"标准", "激进"}, new int[]{0, 1}, v -> setStrength(v)));
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        bClear = btn("立即清除", C_ERR);
        bClear.setOnClickListener(v -> clearNow());
        bServer = btn("本地服务", C_INFO);
        bServer.setOnClickListener(v -> toggleServer());
        bUp = btn("检查更新", C_OK);
        bUp.setOnClickListener(v -> checkUpdate());
        btns.addView(bClear, lpwH(1f));
        btns.addView(bServer, lpwH(1f));
        btns.addView(bUp, lpwH(1f));
        cc.addView(btns, lp(-1, -2, 0f, dp(16)));
        Button bLink = btn("打开项目 · GitHub", C_INFO);
        bLink.setOnClickListener(v -> openProject());
        cc.addView(bLink, lp(-1, -2, 0f, dp(8)));
        col.addView(cc, lp(-1, -2, 0f, dp(12)));

        // ---- 日志 ----
        LinearLayout lc = card();
        lc.addView(tv("守护日志", 15, Typeface.BOLD, C_TXT));
        logText = tv("（暂无日志）", 11, Typeface.NORMAL, C_TXT2);
        logText.setTypeface(Typeface.MONOSPACE);
        lc.addView(logText, lpw(1f));
        lc.addView(tv("开机自保护 · 旧砖日志", 15, Typeface.BOLD, C_TXT), lp(-1, -2, 0f, dp(10)));
        bootText = tv("（暂无日志）", 11, Typeface.NORMAL, C_TXT2);
        bootText.setTypeface(Typeface.MONOSPACE);
        lc.addView(bootText, lpw(1f));
        col.addView(lc, lp(-1, -2, 0f, dp(12)));

        TextView foot = tv("作者 Kazaf · KO卸载 v3.0-beta · 开源 GitHub", 11, Typeface.NORMAL, C_TXT3);
        foot.setGravity(Gravity.CENTER);
        col.addView(foot, lp(-1, -2, 0f, dp(18)));

        sc.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(sc, lpw(1f));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                col.setPadding(p, dp(8) + top, p, dp(24) + bottom);
                return insets;
            });
        }
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(22));
        g.setColor(C_CARD);
        l.setBackground(g);
        l.setPadding(dp(16), dp(14), dp(16), dp(14));
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView cell(LinearLayout parent, String label, String initVal) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(0x14111A30);
        box.setBackground(g);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.addView(tv(label, 11, Typeface.NORMAL, C_TXT3));
        TextView val = tv(initVal, 16, Typeface.BOLD, C_TXT);
        box.addView(val);
        parent.addView(box, lpwH(1f));
        return val;
    }

    private TextView statCell(LinearLayout parent, String label, String initVal) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(tv(label, 10, Typeface.NORMAL, C_TXT3));
        TextView val = tv(initVal, 16, Typeface.BOLD, C_TXT2);
        val.setGravity(Gravity.CENTER);
        box.addView(val);
        parent.addView(box, lpwH(1f));
        return val;
    }

    private Button btn(String txt, int accent) {
        Button b = new Button(this);
        b.setText(txt);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(0xFFE7ECFF);
        b.setAllCaps(false);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(Color.argb(30, (accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF));
        g.setStroke(dp(1), Color.argb(60, (accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF));
        b.setBackground(g);
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        return b;
    }

    /** 分段选择器: opts 显示文本, vals 对应值, cb 回调 */
    private LinearLayout segment(String[] opts, int[] vals, SegCb cb) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < opts.length; i++) {
            Button b = new Button(this);
            b.setText(opts[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setTextColor(C_TXT2);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(12));
            g.setColor(0x14111A30);
            b.setBackground(g);
            b.setPadding(dp(6), dp(7), dp(6), dp(7));
            final int vv = vals[i];
            b.setOnClickListener(v -> cb.on(vv));
            b.setTag(vv);
            l.addView(b, lpwH(1f));
        }
        l.setTag(opts);
        return l;
    }

    private interface SegCb { void on(int v); }

    private TextView tv(String t, float sp, int style, int color) {
        TextView x = new TextView(this);
        x.setText(t);
        x.setTextSize(sp);
        x.setTypeface(Typeface.DEFAULT, style);
        x.setTextColor(color);
        x.setIncludeFontPadding(false);
        return x;
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout.LayoutParams lp(int w, int h, float wgt) {
        return new LinearLayout.LayoutParams(w, h, wgt);
    }
    private LinearLayout.LayoutParams lp(int w, int h, float wgt, int mt) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, wgt);
        p.topMargin = mt;
        return p;
    }
    private LinearLayout.LayoutParams lpw(float wgt) {
        // VERTICAL 父容器: 宽度撑满 + weight 均分剩余高度
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, wgt);
    }
    private LinearLayout.LayoutParams lpwH(float wgt) {
        // HORIZONTAL 父容器: width=0 + weight 均分宽度
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, wgt);
    }

    /* ================= root 执行 ================= */

    /**
     * root 执行: 4s 硬超时 + 强杀, 防止 KernelSU 授权弹窗未响应时永久阻塞;
     * 进程退出后才读取输出, read 必然 EOF 返回, 不会阻塞执行线程。
     */
    private String su(String cmd) {
        Process p = null;
        try {
            p = new ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start();
            if (!p.waitFor(4000, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                p.waitFor(1500, TimeUnit.MILLISECONDS);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            try { in.close(); } catch (IOException ignored) {}
            return bos.toString("UTF-8");
        } catch (Exception e) {
            if (p != null) { try { p.destroyForcibly(); } catch (Exception ignored) {} }
            return "";
        }
    }

    private boolean checkEnv() {
        String id = su("id");
        rootOk = id.contains("uid=0");
        modOk = su("test -d " + MOD + " && echo ok").trim().equals("ok");
        return rootOk && modOk;
    }

    /* ================= 轮询 ================= */

    private void startPoll() {
        poller = new Runnable() {
            @Override
            public void run() {
                if (!busy) poll();
                ui.postDelayed(this, POLL_MS);
            }
        };
        ui.post(poller);
        // 启动后延迟 2s 自动跑一次 12 维检测, 立即呈现实时结果
        ui.postDelayed(() -> {
            if (!busy && dimChips[0] != null && "检测中".equals(dimChips[0].getText().toString())) runDetect();
        }, 2000);
    }

    private void poll() {
        exe.execute(() -> {
            if (!checkEnv()) {
                ui.post(() -> {
                    liveDot.setText(rootOk ? "● 模块缺失" : "● 无 root");
                    liveDot.setTextColor(rootOk ? C_WARN : C_ERR);
                    headText.setText("无法管理");
                    msgText.setText(rootOk ? "模块未安装: " + MOD : "未授予 root 权限（请在 KernelSU 允许本应用）");
                    guardLine.setText("—");
                });
                return;
            }
            String raw = su("cat " + DATA + "/status 2>/dev/null; echo __B__; cat " + DATA + "/boot_uptime 2>/dev/null; echo __L__; tail -n 3 " + DATA + "/daemon.log 2>/dev/null; echo __BL__; tail -n 3 " + DATA + "/boot.log 2>/dev/null");
            Map<String, String> d = new HashMap<>();
            String log = "";
            String bootlog = "";
            long boot = 0;
            int mode = raw.indexOf("__B__");
            int lpos = raw.indexOf("__L__");
            int blpos = raw.indexOf("__BL__");
            String sPart = mode > 0 ? raw.substring(0, mode) : raw;
            if (mode > 0 && lpos > mode) {
                String bPart = raw.substring(mode + 5, lpos).trim();
                log = raw.substring(lpos + 5, blpos > lpos ? blpos : raw.length()).trim();
                if (blpos > lpos) bootlog = raw.substring(blpos + 5).trim();
                try { boot = Long.parseLong(bPart.trim()); } catch (Exception ignored) {}
            }
            for (String l : sPart.split("\n")) {
                int p = l.indexOf('=');
                if (p > 0) d.put(l.substring(0, p).trim(), l.substring(p + 1).trim());
            }
            final String flog = log;
            final String fbootlog = bootlog;
            final long fboot = boot;
            ui.post(() -> {
                liveDot.setText("● 在线");
                liveDot.setTextColor(C_OK);
                if (d.isEmpty()) {
                    // 守护未写缓存 -> 回退 webstatus 单行协议
                    fallbackStatus();
                    return;
                }
                render(d, flog, fboot, fbootlog);
            });
        });
    }

    private void fallbackStatus() {
        exe.execute(() -> {
            String out = su("sh " + MOD + "/bin/webstatus.sh 2>/dev/null").trim();
            Map<String, String> d = new HashMap<>();
            String last = out.isEmpty() ? "" : out.split("\n")[out.split("\n").length - 1];
            for (String seg : last.split("\\|")) {
                int p = seg.indexOf('=');
                if (p > 0) d.put(seg.substring(0, p).trim(), seg.substring(p + 1).trim());
            }
            ui.post(() -> render(d, "", 0, ""));
        });
    }

    /* ================= 渲染 ================= */

    private void render(Map<String, String> d, String log, long boot, String bootlog) {
        String state = d.get("state") == null ? "unknown" : d.get("state");
        String loaded = d.get("inte_loaded");
        boolean lk = "1".equals(loaded);
        boolean run = "1".equals(d.get("run")) || d.containsKey("ts");
        String mode = d.get("mode") == null ? "unload" : d.get("mode");

        if (run && (state.equals("stopped") || state.equals("notfound") || state.equals("unknown"))) {
            state = lk ? "running" : "normal";
        }

        String head, msg;
        int color;
        switch (state) {
            case "normal":
                if (lk) { head = "待复核"; msg = "inte 仍在加载, 守护正在处理"; color = C_WARN; }
                else { head = "已生效"; msg = "inte 未加载, 内核完整性检测已失效"; color = C_OK; }
                break;
            case "running": head = "处理中"; msg = "正在卸载 inte"; color = C_WARN; break;
            case "failed": head = "未生效"; msg = "inte 无法卸载 (可能被内核保护)"; color = C_ERR; break;
            case "builtin": head = "无法处理"; msg = "inte 已编译进内核, rmmod 无效"; color = C_ERR; break;
            case "notfound": head = "无需处理"; msg = "本机型未检测到 inte 子系统"; color = C_INFO; break;
            case "stopped": head = "已停止"; msg = "守护进程已退出"; color = C_WARN; break;
            default:
                head = lk ? "启动中" : "已生效";
                msg = lk ? "守护进程初始化中, inte 当前处于加载状态" : "inte 未加载, 内核完整性检测已失效";
                color = lk ? C_WARN : C_OK;
        }
        if (!run && state.equals("normal")) { head = "守护已停"; msg = "inte 已清除, 但守护进程未运行"; color = C_WARN; }

        headText.setText(head);
        headText.setTextColor(color);
        msgText.setText(msg);

        String iv = d.get("interval");
        try { lastIv = iv == null ? 5 : Long.parseLong(iv); } catch (Exception ignored) {}
        if (lastIv < 1) lastIv = 1;
        String guard;
        if (!run) guard = "主动防护 未运行";
        else if (mode.equals("monitor")) guard = "监控模式 · 仅记录";
        else if (lk) guard = "主动防护 盯防中 · 已发现";
        else guard = "主动防护 盯防中 · " + lastIv + "s";
        guardLine.setText(guard);

        // metrics
        setVal(vInte, lk ? "加载中" : "未加载", lk ? C_ERR : C_OK);
        setVal(vNodes, d.get("nodes") + "/3", "0".equals(d.get("nodes")) ? C_OK : C_WARN);
        setVal(vProc, run ? "运行中 pid=" + d.get("pid") : "已停止", run ? C_OK : C_WARN);
        long ts = 0, upt = 0;
        try { ts = Long.parseLong(d.get("ts")); } catch (Exception ignored) {}
        try { upt = Long.parseLong(d.get("uptime")); } catch (Exception ignored) {}
        if (upt == 0 && ts > 0 && boot > 0) upt = ts;
        long rt = (upt > 0 && boot > 0 && upt >= boot) ? (upt - boot) : -1;
        setVal(vRun, rt >= 0 ? fmt(rt) : "--", C_TXT);

        // stats
        parseStats(d.get("stats"), vUnload, vRevive);
        setVal(vMode, mode.equals("monitor") ? "监控" : "卸载", C_TXT);
        setVal(vIv, lastIv + "s", C_TXT);
        boolean aggr = "1".equals(d.get("strength"));
        setVal(vStr, aggr ? "激进" : "标准", aggr ? C_WARN : C_TXT);

        // version
        if (d.get("version") != null) vVersion.setText(d.get("version"));

        // 12 维
        renderDims(d);

        // 服务状态
        serverOn = "1".equals(d.get("server")) ? 1 : 0;
        bServer.setText(serverOn == 1 ? "本地服务 · 开" : "本地服务 · 关");

        // 日志
        if (!log.isEmpty()) logText.setText(log);
        else if (d.get("log3") != null) logText.setText(d.get("log3").replace("|", "\n"));

        // 开机自保护日志 (旧砖模式)
        if (bootlog != null && !bootlog.isEmpty()) {
            bootText.setText(bootlog);
            bootText.setTextColor(bootlog.contains("DISABLE") ? C_ERR : (bootlog.contains("CLEAR") ? C_OK : C_TXT2));
        } else if (d.get("bootlog") != null) {
            bootText.setText(d.get("bootlog").replace("|", "\n"));
        }

        // 模式/间隔/强度段高亮
        setSeg(col, "模式", mode.equals("monitor") ? 1 : 0);
        setSeg(col, "间隔", (int) lastIv);
        setSeg(col, "强度", aggr ? 1 : 0);
    }

    private void setVal(TextView t, String s, int c) {
        t.setText(s);
        t.setTextColor(c);
    }

    private void parseStats(String stats, TextView u, TextView r) {
        String un = "0", rv = "0";
        if (stats != null) {
            for (String seg : stats.split(",")) {
                int p = seg.indexOf('=');
                if (p > 0) {
                    String k = seg.substring(0, p).trim(), v = seg.substring(p + 1).trim();
                    if (k.equals("unload")) un = v;
                    else if (k.equals("revive")) rv = v;
                }
            }
        }
        setVal(u, un, C_TXT);
        setVal(r, rv, C_TXT);
    }

    /** 12 维: 威胁维度 1=红, 环境事实维度(file/cmd) 中性展示 */
    private void renderDims(Map<String, String> d) {
        String[] threat = {"m_ko", "m_systbl", "m_status", "m_mod", "m_sysmod", "ksym", "m_dev"};
        for (int i = 0; i < threat.length; i++) {
            String v = d.get(threat[i]);
            if (v == null || v.equals("")) setDim(i, "检测中", C_TXT3);
            else if (v.equals("0")) setDim(i, "未确认", C_OK);
            else setDim(i, "存在", C_ERR);
        }
        String f = d.get("m_file");
        if (f == null || f.equals("")) setDim(7, "检测中", C_TXT3);
        else if (f.equals("1")) setDim(7, "驻留（正常）", C_INFO);
        else setDim(7, "未驻留", C_OK);
        String c = d.get("m_cmd");
        if (c == null || c.equals("")) setDim(8, "检测中", C_TXT3);
        else if (c.equals("1")) setDim(8, "静态特征", C_INFO);
        else setDim(8, "无特征", C_OK);
        String rf = d.get("m_ref");
        if (rf == null || rf.equals("")) setDim(9, "检测中", C_TXT3);
        else if (rf.equals("0")) setDim(9, "未确认", C_OK);
        else setDim(9, "存在 · " + rf, C_ERR);
        String in = d.get("m_init");
        if (in == null || in.equals("")) setDim(10, "检测中", C_TXT3);
        else if (in.equals("0")) setDim(10, "未确认", C_OK);
        else setDim(10, "存在 · " + in, C_ERR);
    }

    private void setDim(int idx, String txt, int color) {
        TextView t = dimChips[idx];
        if (t == null) return;
        t.setText(txt);
        t.setTextColor(color);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(11));
        g.setColor(Color.argb(28, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF));
        t.setBackground(g);
    }

    private void setSeg(LinearLayout root, String title, int value) {
        // 遍历根下所有带 Tag 的段控件
        findAndSet(root, value);
    }

    private void findAndSet(View v, int value) {
        if (v instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) v;
            Object tg = l.getTag();
            if (tg instanceof String[]) {
                int n = l.getChildCount();
                for (int i = 0; i < n; i++) {
                    View c = l.getChildAt(i);
                    if (c instanceof Button && c.getTag() != null) {
                        boolean on = Integer.toString(value).equals(c.getTag().toString());
                        ((Button) c).setTextColor(on ? 0xFF0B1020 : C_TXT2);
                        GradientDrawable g = new GradientDrawable();
                        g.setCornerRadius(dp(12));
                        g.setColor(on ? 0xFF34D399 : 0x14111A30);
                        c.setBackground(g);
                    }
                }
            }
            for (int i = 0; i < l.getChildCount(); i++) findAndSet(l.getChildAt(i), value);
        }
    }

    private String fmt(long s) {
        if (s < 0) return "--";
        if (s < 60) return s + " 秒";
        if (s < 3600) return (s / 60) + " 分 " + (s % 60) + " 秒";
        if (s < 86400) return (s / 3600) + " 时 " + ((s % 3600) / 60) + " 分";
        return (s / 86400) + " 天 " + ((s % 86400) / 3600) + " 时";
    }

    /* ================= 操作 ================= */

    private void setMode(String m) {
        if (busy) return;
        busy = true;
        exe.execute(() -> {
            su("echo " + m + " > " + DATA + "/mode");
            ui.post(() -> { toast("模式已切换: " + (m.equals("monitor") ? "监控" : "卸载")); busy = false; });
        });
    }

    private void setIv(int iv) {
        if (busy) return;
        busy = true;
        exe.execute(() -> {
            su("echo " + iv + " > " + DATA + "/interval; echo " + iv + " > " + DATA + "/interval_usr");
            ui.post(() -> { toast("复核间隔已设为 " + iv + " 秒"); busy = false; });
        });
    }

    private void setStrength(int st) {
        if (busy) return;
        busy = true;
        exe.execute(() -> {
            su("echo " + st + " > " + DATA + "/strength");
            ui.post(() -> { toast("强度已切换: " + (st == 1 ? "激进" : "标准")); busy = false; });
        });
    }

    private void clearNow() {
        if (busy) return;
        busy = true;
        bClear.setEnabled(false);
        bClear.setText("清除中…");
        exe.execute(() -> {
            su("sh " + MOD + "/action.sh >/dev/null 2>&1");
            ui.post(() -> { bClear.setEnabled(true); bClear.setText("立即清除"); toast("已执行立即清除"); busy = false; });
        });
    }

    private void toggleServer() {
        if (busy) return;
        busy = true;
        String act = serverOn == 1 ? "stop" : "start";
        bServer.setEnabled(false);
        exe.execute(() -> {
            String out = su("sh " + MOD + "/bin/server.sh " + act + " 2>&1").trim();
            boolean ok = out.contains("已启动") || out.contains("已停止") || out.contains("已在运行") || out.contains("未在运行");
            ui.post(() -> {
                bServer.setEnabled(true);
                toast(ok ? (act.equals("start") ? "本地服务已启动" : "本地服务已停止") : ("执行结果: " + out));
                busy = false;
                poll();
            });
        });
    }

    private void checkUpdate() {
        if (busy) return;
        busy = true;
        bUp.setEnabled(false);
        bUp.setText("检查中…");
        exe.execute(() -> {
            String out = su("sh " + MOD + "/bin/checkupdate.sh 2>&1").trim();
            String last = out.isEmpty() ? "" : out.split("\n")[out.split("\n").length - 1];
            Map<String, String> d = new HashMap<>();
            for (String seg : last.split("\\|")) {
                int p = seg.indexOf('=');
                if (p > 0) d.put(seg.substring(0, p).trim(), seg.substring(p + 1).trim());
            }
            ui.post(() -> {
                bUp.setEnabled(true);
                bUp.setText("检查更新");
                busy = false;
                if ("1".equals(d.get("need"))) {
                    toast("发现新版本: " + d.get("latest") + " (code " + d.get("code") + ")");
                    if (d.get("note") != null) toast2("更新日志: " + d.get("note"));
                } else if ("0".equals(d.get("need"))) {
                    toast("已是最新版本: " + d.get("latest"));
                } else {
                    toast("无法获取更新信息");
                }
            });
        });
    }

    private void runDetect() {
        if (busy) return;
        busy = true;
        for (int i = 0; i < dimChips.length; i++) {
            final int idx = i;
            ui.post(() -> setDim(idx, "检测中", C_TXT3));
        }
        exe.execute(() -> {
            String out = su("sh " + MOD + "/bin/detect.sh 2>/dev/null").trim();
            String last = out.isEmpty() ? "" : out.split("\n")[out.split("\n").length - 1];
            Map<String, String> d = new HashMap<>();
            for (String seg : last.split("\\|")) {
                int p = seg.indexOf('=');
                if (p > 0) d.put(seg.substring(0, p).trim(), seg.substring(p + 1).trim());
            }
            ui.post(() -> {
                renderDimsFromDetect(d);
                toast("检测完成");
                busy = false;
            });
        });
    }

    private void renderDimsFromDetect(Map<String, String> d) {
        String[] keys = {"ko", "systbl", "status", "mod", "sysmod", "ksym", "dev"};
        for (int i = 0; i < keys.length; i++) {
            String v = d.get(keys[i]);
            if (v == null || v.equals("")) setDim(i, "检测中", C_TXT3);
            else if (v.equals("0")) setDim(i, "未确认", C_OK);
            else setDim(i, "存在", C_ERR);
        }
        String f = d.get("file");
        setDim(7, "1".equals(f) ? "驻留（正常）" : "未驻留", "1".equals(f) ? C_INFO : C_OK);
        String c = d.get("cmd");
        setDim(8, "1".equals(c) ? "静态特征" : "无特征", "1".equals(c) ? C_INFO : C_OK);
        String rf = d.get("ref");
        if (rf == null || rf.equals("")) setDim(9, "未确认", C_OK);
        else setDim(9, "存在 · " + rf, C_ERR);
        String in = d.get("init");
        if (in == null || in.equals("")) setDim(10, "未确认", C_OK);
        else setDim(10, "存在 · " + in, C_ERR);
    }

    private void openProject() {
        if (busy) return;
        busy = true;
        exe.execute(() -> {
            String link = su("head -n 1 " + DATA + "/link 2>/dev/null").trim();
            if (!link.matches("https?://[^\\s]+")) link = "https://github.com/kazaf080726/ko_uninstall";
            su("am start --user 0 -a android.intent.action.VIEW -d '" + link + "' >/dev/null 2>&1");
            ui.post(() -> { toast("已尝试打开项目主页"); busy = false; });
        });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private void toast2(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
