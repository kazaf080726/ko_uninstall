package com.kazaf.kouninstall;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * KO卸载 · 管理 APP
 * v3.1 — 液态玻璃 UI 全面重构:
 *   - 三屏结构(首页/检测/设置) 复刻 KernelSU / SukiSU 管理器界面
 *   - 流动光斑玻璃背景 + 半透明玻璃卡片 + 高光描边
 *   - Q弹弹性入场动画 / 呼吸状态灯 / 按压缩放
 *   - root 缺失全屏玻璃提示: 请赋予应用 root 权限后重新打开
 * 作者: Kazaf
 */
public class MainActivity extends Activity {

    /* ================= 常量 ================= */
    private static final int C_OK = 0xFF4ADE80;
    private static final int C_WARN = 0xFFFBBF24;
    private static final int C_ERR = 0xFFF87171;
    private static final int C_INFO = 0xFF60A5FA;
    private static final int C_TXT = 0xFFF2F5FF;
    private static final int C_TXT2 = 0xFFB9C3E0;
    private static final int C_TXT3 = 0xFF7E8BB0;
    private static final int C_BG1 = 0xFF0B1020;
    private static final int C_BG2 = 0xFF101B3A;
    private static final String MOD = "/data/adb/modules/ko_uninstall";
    private static final String DATA = MOD + "/data";
    private static final long POLL_MS = 3000;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService exe = Executors.newSingleThreadExecutor();

    /* ================= 视图 ================= */
    private LinearLayout home, detect, settings, col;
    private TextView vVersion, liveDot, headText, msgText, guardLine;
    private TextView vInte, vNodes, vProc, vRun, vUnload, vRevive, vMode, vIv, vStr;
    private TextView[] dimChips = new TextView[11];
    private TextView logText, bootText;
    private Button bClear, bServer, bUp;
    private FrameLayout root;
    private LinearLayout noRoot;
    private View navSel;
    private final Map<String, String> lastD = new HashMap<>();
    private boolean busy = false, rootOk = false, modOk = false;
    private long lastIv = 5;
    private int serverOn = 0, curTab = 0;
    private Runnable poller;
    private BackgroundView bgView;

    private interface SegCb { void on(int v); }

    /* ================= 生命周期 ================= */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        startPoll();
        // 启动后延迟 1.5s 自动跑一次 12 维检测
        ui.postDelayed(this::runDetect, 1500);
    }

    /* ================= 核心 UI 构建 ================= */

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(C_BG1);

        // 液态玻璃背景: 流动光斑
        bgView = new BackgroundView(this);
        root.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        // 内容三屏
        home = screen();
        detect = screen();
        settings = screen();
        root.addView(home, new FrameLayout.LayoutParams(-1, -1));
        root.addView(detect, new FrameLayout.LayoutParams(-1, -1));
        root.addView(settings, new FrameLayout.LayoutParams(-1, -1));

        buildHome();
        buildDetect();
        buildSettings();

        // 底部导航 (玻璃条)
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        GradientDrawable navBg = glassBg(dp(30), 0x26111A30, 0x4DFFFFFF);
        nav.setBackground(navBg);
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(64));
        navLp.gravity = Gravity.BOTTOM;
        navLp.leftMargin = dp(20);
        navLp.rightMargin = dp(20);
        navLp.bottomMargin = dp(14);
        root.addView(nav, navLp);

        navSel = new View(this);
        navSel.setBackground(glassBg(dp(22), 0x3A34D399, 0x99FFFFFF));
        navSel.setElevation(dp(6));
        addNavTab(nav, 0, "首页");
        addNavTab(nav, 1, "检测");
        addNavTab(nav, 2, "设置");

        // root 缺失提示层 (全屏覆盖)
        noRoot = new LinearLayout(this);
        noRoot.setOrientation(LinearLayout.VERTICAL);
        noRoot.setGravity(Gravity.CENTER);
        noRoot.setBackground(glassBg(dp(0), 0xD90B1020, 0x00FFFFFF));
        TextView nrTitle = tv("需要 root 权限", 22, Typeface.BOLD, C_TXT);
        TextView nrMsg = tv("KO卸载 需要 root 才能读取与卸载 inte.ko。\n请先在 KernelSU / Magisk 中允许本应用获取 root，\n然后重新打开应用。", 14, Typeface.NORMAL, C_TXT2);
        nrMsg.setGravity(Gravity.CENTER);
        nrMsg.setLineSpacing(dp(4), 1.0f);
        TextView nrSub = tv("检测结果：未授予 root 或模块未安装", 12, Typeface.NORMAL, C_TXT3);
        Button nrBtn = btn("重新检测", C_OK);
        nrBtn.setOnClickListener(v -> { enterAnim(nrBtn); retryEnv(); });
        Button nrBtn2 = btn("退出应用", C_TXT2);
        nrBtn2.setOnClickListener(v -> finish());
        noRoot.addView(nrTitle);
        noRoot.addView(nrMsg, lp(-1, -2, 0f, dp(18)));
        noRoot.addView(nrSub, lp(-1, -2, 0f, dp(28)));
        noRoot.addView(nrBtn, lp(-1, -2, 0f, dp(16)));
        noRoot.addView(nrBtn2, lp(-1, -2, 0f, dp(10)));
        root.addView(noRoot, new FrameLayout.LayoutParams(-1, -1));
        noRoot.setVisibility(View.GONE);

        setContentView(root);
        enterAnim(home);
    }

    private void addNavTab(LinearLayout nav, int idx, String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTextColor(idx == 0 ? 0xFF34D399 : C_TXT2);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(glassBg(dp(22), idx == 0 ? 0x3A34D399 : 0x00111A30, 0x44FFFFFF));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1f);
        p.leftMargin = dp(6);
        p.rightMargin = dp(6);
        nav.addView(b, p);
        b.setTag(idx);
        b.setOnClickListener(v -> {
            int i = (Integer) v.getTag();
            switchTab(i);
            for (int j = 0; j < nav.getChildCount(); j++) {
                Button tb = (Button) nav.getChildAt(j);
                boolean on = (Integer) tb.getTag() == i;
                tb.setTextColor(on ? 0xFF34D399 : C_TXT2);
                tb.setBackground(glassBg(dp(22), on ? 0x3A34D399 : 0x00111A30, 0x44FFFFFF));
            }
        });
    }

    private void switchTab(int idx) {
        if (curTab == idx) return;
        curTab = idx;
        home.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        detect.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        settings.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        View target = idx == 0 ? home : (idx == 1 ? detect : settings);
        enterAnim(target);
        if (idx == 1 && dimChips[0] != null && "检测中".equals(dimChips[0].getText().toString())) runDetect();
    }

    private LinearLayout screen() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        col.setPadding(p, dp(14), p, dp(96));
        sc.addView(col, new ScrollView.LayoutParams(-1, -2));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        return wrap;
    }

    /* ================= 首页 ================= */

    private void buildHome() {
        LinearLayout col = homeCol();
        this.col = col;

        // hero 玻璃卡
        LinearLayout hero = glass();
        GradientDrawable hg = new GradientDrawable(Orientation.TL_BR,
                new int[]{0x6610B981, 0x2A1D4ED8, 0x1A111A30});
        hg.setCornerRadius(dp(30));
        hero.setBackground(hg);
        hero.setPadding(dp(22), dp(22), dp(22), dp(20));

        LinearLayout hrow = new LinearLayout(this);
        hrow.setOrientation(LinearLayout.HORIZONTAL);
        hrow.setGravity(Gravity.CENTER_VERTICAL);
        liveDot = tv("●", 13, Typeface.BOLD, C_OK);
        liveDot.setGravity(Gravity.CENTER);
        GradientDrawable ldbg = new GradientDrawable();
        ldbg.setCornerRadius(dp(14));
        ldbg.setColor(0x24111A30);
        liveDot.setBackground(ldbg);
        liveDot.setPadding(dp(10), dp(5), dp(10), dp(5));
        vVersion = tv("v3.1", 12, Typeface.NORMAL, C_TXT3);
        vVersion.setGravity(Gravity.CENTER);
        vVersion.setBackground(ldbg);
        vVersion.setPadding(dp(10), dp(5), dp(10), dp(5));
        TextView hTitle = tv("KO卸载", 24, Typeface.BOLD, C_TXT);
        hTitle.setShadowLayer(dp(10), 0, 0, 0x6634D399);
        hrow.addView(hTitle, lpwH(1f));
        hrow.addView(liveDot);
        hrow.addView(vVersion, lp(-1, -2, 0f, 0, dp(10)));
        hero.addView(hrow);

        headText = tv("读取中", 30, Typeface.BOLD, C_OK);
        headText.setShadowLayer(dp(16), 0, 0, 0x5534D399);
        hero.addView(headText, lp(-1, -2, 0f, dp(16)));
        msgText = tv("正在读取守护进程状态…", 14, Typeface.NORMAL, C_TXT2);
        hero.addView(msgText, lp(-1, -2, 0f, dp(6)));
        guardLine = tv("主动防护 读取中", 13, Typeface.BOLD, C_INFO);
        GradientDrawable gg = new GradientDrawable();
        gg.setCornerRadius(dp(16));
        gg.setColor(0x24111A30);
        guardLine.setBackground(gg);
        guardLine.setPadding(dp(14), dp(7), dp(14), dp(7));
        hero.addView(guardLine, lp(-1, -2, 0f, dp(14)));
        col.addView(hero, lp(-1, -2, 0f, dp(14)));

        // 2x2 指标玻璃卡
        LinearLayout m1 = hRow();
        vInte = cell(m1, "inte 模块", "--");
        vNodes = cell(m1, "/proc 节点", "--/3");
        col.addView(m1, lp(-1, -2, 0f, dp(12)));
        LinearLayout m2 = hRow();
        vProc = cell(m2, "守护进程", "--");
        vRun = cell(m2, "本次运行", "--");
        col.addView(m2, lp(-1, -2, 0f, dp(12)));

        // 统计玻璃条
        LinearLayout st = hRow();
        GradientDrawable stg = glassBg(dp(22), 0x221D4ED8, 0x3DFFFFFF);
        st.setBackground(stg);
        st.setPadding(dp(6), dp(10), dp(6), dp(10));
        vUnload = statCell(st, "卸载", "0");
        vRevive = statCell(st, "复活", "0");
        vMode = statCell(st, "模式", "--");
        vIv = statCell(st, "间隔", "--");
        vStr = statCell(st, "强度", "--");
        col.addView(st, lp(-1, -2, 0f, dp(12)));

        // 快捷操作
        LinearLayout q = glass();
        q.addView(tv("快捷操作", 15, Typeface.BOLD, C_TXT));
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
        q.addView(btns, lp(-1, -2, 0f, dp(14)));
        Button bLink = btn("打开项目 · GitHub", C_TXT2);
        bLink.setOnClickListener(v -> openProject());
        q.addView(bLink, lp(-1, -2, 0f, dp(10)));
        col.addView(q, lp(-1, -2, 0f, dp(12)));
    }

    private LinearLayout homeCol() {
        ScrollView sc = (ScrollView) ((LinearLayout) home).getChildAt(0);
        LinearLayout c = (LinearLayout) sc.getChildAt(0);
        return c;
    }

    /* ================= 检测页 ================= */

    private void buildDetect() {
        LinearLayout col = detectCol();

        LinearLayout dhead = new LinearLayout(this);
        dhead.setOrientation(LinearLayout.HORIZONTAL);
        dhead.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = tv("内核检测 · 12 维", 18, Typeface.BOLD, C_TXT);
        t.setShadowLayer(dp(8), 0, 0, 0x5560A5FA);
        dhead.addView(t, lpwH(1f));
        Button bDetect = btn("重新检测", C_INFO);
        bDetect.setOnClickListener(v -> { enterAnim(bDetect); runDetect(); });
        dhead.addView(bDetect);
        col.addView(dhead, lp(-1, -2, 0f, dp(6)));

        LinearLayout dc = glass();
        String[] labels = {
                "/proc/inte_ko", "/proc/inte_systbl", "/proc/inte_status",
                "/proc/modules·inte", "/sys/module/inte", "kallsyms·inte",
                "/dev·inte 节点", "inte.ko 文件驻留", "内核 cmdline 特征",
                "refcnt", "initstate"
        };
        for (int i = 0; i < labels.length; i++) {
            LinearLayout r = hRow();
            r.addView(tv(labels[i], 12, Typeface.NORMAL, C_TXT2), lpwH(1f));
            TextView chip = tv("检测中", 12, Typeface.BOLD, C_TXT3);
            GradientDrawable cbg = new GradientDrawable();
            cbg.setCornerRadius(dp(11));
            cbg.setColor(0x18111A30);
            chip.setBackground(cbg);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(4), dp(12), dp(4));
            r.addView(chip);
            dimChips[i] = chip;
            dc.addView(r, lp(-1, -2, 0f, dp(6)));
            enterDelayed(r, 80 + i * 45);
        }
        col.addView(dc, lp(-1, -2, 0f, dp(12)));

        TextView hint = tv("威胁维度命中即标红；inte.ko 文件驻留与 cmdline 特征为环境事实，中性展示。", 11, Typeface.NORMAL, C_TXT3);
        hint.setLineSpacing(dp(3), 1.0f);
        col.addView(hint, lp(-1, -2, 0f, dp(4)));
    }

    private LinearLayout detectCol() {
        ScrollView sc = (ScrollView) ((LinearLayout) detect).getChildAt(0);
        return (LinearLayout) sc.getChildAt(0);
    }

    /* ================= 设置页 ================= */

    private void buildSettings() {
        LinearLayout col = settingsCol();

        LinearLayout cc = glass();
        cc.addView(tv("运行策略", 16, Typeface.BOLD, C_TXT));
        cc.addView(tv("模式", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(8)));
        cc.addView(segment(new String[]{"卸载模式", "监控模式"}, new int[]{0, 1}, v -> setMode(v == 0 ? "unload" : "monitor")));
        cc.addView(tv("复核间隔", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(12)));
        cc.addView(segment(new String[]{"3s", "5s", "10s", "30s", "60s"}, new int[]{3, 5, 10, 30, 60}, v -> setIv(v)));
        cc.addView(tv("屏蔽强度", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(12)));
        cc.addView(segment(new String[]{"标准", "激进"}, new int[]{0, 1}, v -> setStrength(v)));
        col.addView(cc, lp(-1, -2, 0f, dp(12)));

        LinearLayout lc = glass();
        lc.addView(tv("守护日志", 16, Typeface.BOLD, C_TXT));
        logText = tv("读取中…", 11, Typeface.NORMAL, C_TXT2);
        logText.setLineSpacing(dp(3), 1.0f);
        lc.addView(logText, lp(-1, -2, 0f, dp(8)));
        col.addView(lc, lp(-1, -2, 0f, dp(12)));

        LinearLayout bc = glass();
        bc.addView(tv("开机自保护 · 旧砖日志", 16, Typeface.BOLD, C_TXT));
        bootText = tv("读取中…", 11, Typeface.NORMAL, C_TXT2);
        bootText.setLineSpacing(dp(3), 1.0f);
        bc.addView(bootText, lp(-1, -2, 0f, dp(8)));
        col.addView(bc, lp(-1, -2, 0f, dp(12)));

        LinearLayout ac = glass();
        ac.addView(tv("关于", 16, Typeface.BOLD, C_TXT));
        ac.addView(tv("作者 Kazaf · KO卸载 v3.1\n开源: github.com/kazaf080726/ko_uninstall\n续承 ddc_inte_killer / Fuck-inte / MAPLE 三位前辈", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(8)));
        col.addView(ac, lp(-1, -2, 0f, dp(12)));
    }

    private LinearLayout settingsCol() {
        ScrollView sc = (ScrollView) ((LinearLayout) settings).getChildAt(0);
        return (LinearLayout) sc.getChildAt(0);
    }

    /* ================= 液态玻璃组件 ================= */

    /** 半透明玻璃渐变 + 白高光描边 */
    private GradientDrawable glassBg(int radius, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable(Orientation.TL_BR,
                new int[]{fill, 0x0D1A2033});
        g.setCornerRadius(radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout glass() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(glassBg(dp(26), 0x261D4ED8, 0x40FFFFFF));
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        l.setElevation(dp(2));
        return l;
    }

    private TextView tv(String t, float sp, int style, int color) {
        TextView x = new TextView(this);
        x.setText(t);
        x.setTextSize(sp);
        x.setTypeface(Typeface.DEFAULT, style);
        x.setTextColor(color);
        return x;
    }

    private Button btn(String txt, int accent) {
        Button b = new Button(this);
        b.setText(txt);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(0xFFF2F5FF);
        int base = 0x66000000 | (accent & 0xFFFFFF);
        GradientDrawable g = new GradientDrawable(Orientation.TL_BR,
                new int[]{base, 0x2A000000 | (accent & 0xFFFFFF)});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), 0x55FFFFFF);
        b.setBackground(g);
        b.setElevation(dp(3));
        b.setPadding(dp(10), dp(9), dp(10), dp(9));
        b.setOnTouchListener((v, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setStartDelay(70).setDuration(220)
                        .setInterpolator(new OvershootInterpolator(2.2f)).start();
            }
            return false;
        });
        return b;
    }

    private LinearLayout hRow() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView cell(LinearLayout parent, String label, String initVal) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(glassBg(dp(18), 0x1E34D399, 0x33FFFFFF));
        box.setPadding(dp(10), dp(12), dp(10), dp(12));
        box.addView(tv(label, 11, Typeface.NORMAL, C_TXT3));
        TextView val = tv(initVal, 15, Typeface.BOLD, C_TXT);
        box.addView(val, lp(-1, -2, 0f, dp(2)));
        parent.addView(box, lpwH(1f));
        return val;
    }

    private TextView statCell(LinearLayout parent, String label, String initVal) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(tv(label, 10, Typeface.NORMAL, C_TXT3));
        TextView val = tv(initVal, 15, Typeface.BOLD, C_TXT2);
        val.setGravity(Gravity.CENTER);
        box.addView(val);
        parent.addView(box, lpwH(1f));
        return val;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float wgt, int mt) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, wgt);
        p.topMargin = mt;
        return p;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float wgt, int mt, int ml) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, wgt);
        p.topMargin = mt;
        p.leftMargin = ml;
        return p;
    }

    private LinearLayout.LayoutParams lpw(float wgt) {
        return new LinearLayout.LayoutParams(-1, 0, wgt);
    }

    private LinearLayout.LayoutParams lpwH(float wgt) {
        return new LinearLayout.LayoutParams(0, -2, wgt);
    }

    /** 分段选择器 */
    private LinearLayout segment(String[] opts, int[] vals, SegCb cb) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0x1A111A30);
        l.setBackground(bg);
        l.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < opts.length; i++) {
            Button b = new Button(this);
            b.setText(opts[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setTextColor(C_TXT2);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(13));
            g.setColor(0x00111A30);
            b.setBackground(g);
            b.setPadding(dp(6), dp(7), dp(6), dp(7));
            final int vv = vals[i];
            b.setOnClickListener(v -> { pressAnim(b); cb.on(vv); });
            b.setTag(vv);
            l.addView(b, lpwH(1f));
        }
        l.setTag(opts);
        return l;
    }

    /* ================= 动画 ================= */

    private void enterAnim(View v) {
        v.setAlpha(0f);
        v.setScaleX(0.92f);
        v.setScaleY(0.92f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(420).setInterpolator(new OvershootInterpolator(1.6f)).start();
    }

    private void enterDelayed(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(10));
        v.animate().alpha(1f).translationY(0)
                .setStartDelay(delay).setDuration(380)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void pressAnim(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start();
        v.animate().scaleX(1f).scaleY(1f).setStartDelay(90).setDuration(180)
                .setInterpolator(new OvershootInterpolator(2f)).start();
    }

    private void pulse(View v, long period) {
        v.animate().cancel();
        v.animate().alpha(0.35f).scaleX(1.22f).scaleY(1.22f).setDuration(period / 2).start();
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(period / 2).setDuration(period / 2).start();
    }

    /** 液态玻璃背景: 流动光斑 */
    private class BackgroundView extends View {
        private final Paint p1 = new Paint(), p2 = new Paint(), p3 = new Paint();
        private long t0 = System.currentTimeMillis();

        BackgroundView(Activity a) { super(a); }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (w == 0) return;
            long ms = System.currentTimeMillis() - t0;
            float k = ms / 10000f;

            // 深色底渐变
            Paint bg = new Paint();
            bg.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{C_BG1, 0xFF122052, 0xFF0B1020}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, bg);

            // 三个光斑缓慢流动 (液态感)
            float cx1 = w * (0.5f + 0.35f * (float) Math.sin(k * 0.9f));
            float cy1 = h * (0.25f + 0.12f * (float) Math.cos(k * 1.1f));
            float cx2 = w * (0.7f + 0.25f * (float) Math.cos(k * 0.7f + 2f));
            float cy2 = h * (0.7f + 0.15f * (float) Math.sin(k * 0.8f + 1f));
            float cx3 = w * (0.25f + 0.2f * (float) Math.cos(k * 1.3f + 4f));
            float cy3 = h * (0.85f + 0.1f * (float) Math.sin(k * 0.6f + 3f));

            float r1 = w * 0.5f, r2 = w * 0.42f, r3 = w * 0.38f;
            p1.setShader(new RadialGradient(cx1, cy1, r1,
                    new int[]{0x2210B981, 0x0010B981}, null, Shader.TileMode.CLAMP));
            p2.setShader(new RadialGradient(cx2, cy2, r2,
                    new int[]{0x2A1D4ED8, 0x001D4ED8}, null, Shader.TileMode.CLAMP));
            p3.setShader(new RadialGradient(cx3, cy3, r3,
                    new int[]{0x2E8B5CF6, 0x008B5CF6}, null, Shader.TileMode.CLAMP));
            c.drawCircle(cx1, cy1, r1, p1);
            c.drawCircle(cx2, cy2, r2, p2);
            c.drawCircle(cx3, cy3, r3, p3);

            if (!isShown() || getParent() == null) return;
            // 60fps 流动
            postInvalidateOnAnimation();
        }
    }

    /* ================= root 环境 ================= */

    private boolean checkEnv() {
        String id = su("id");
        rootOk = id.contains("uid=0");
        modOk = su("test -d " + MOD + " && echo ok").trim().equals("ok");
        return rootOk && modOk;
    }

    private void retryEnv() {
        exe.execute(() -> {
            boolean ok = checkEnv();
            ui.post(() -> {
                if (ok) {
                    noRoot.setVisibility(View.GONE);
                    toast("root 已就绪");
                    poll();
                } else {
                    toast(rootOk ? "模块未安装: " + MOD : "仍未获取到 root");
                }
            });
        });
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
    }

    private void poll() {
        exe.execute(() -> {
            if (!checkEnv()) {
                ui.post(() -> {
                    liveDot.setText(rootOk ? "● 模块缺失" : "● 无 root");
                    liveDot.setTextColor(rootOk ? C_WARN : C_ERR);
                    headText.setText("无法管理");
                    msgText.setText(rootOk ? "模块未安装: " + MOD : "请赋予应用 root 权限，再重新打开应用");
                    guardLine.setText("—");
                    noRoot.setVisibility(View.VISIBLE);
                    enterAnim(noRoot);
                });
                return;
            }
            noRoot.setVisibility(View.GONE);
            String raw = su("cat " + DATA + "/status 2>/dev/null; echo __B__; cat " + DATA + "/boot_uptime 2>/dev/null; echo __L__; tail -n 3 " + DATA + "/daemon.log 2>/dev/null; echo __BL__; tail -n 3 " + DATA + "/boot.log 2>/dev/null");
            Map<String, String> d = new HashMap<>();
            String log = "", bootlog = "";
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
        lastD.clear();
        lastD.putAll(d);
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
        pulse(guardLine, 2400);

        setVal(vInte, lk ? "加载中" : "未加载", lk ? C_ERR : C_OK);
        setVal(vNodes, d.get("nodes") + "/3", "0".equals(d.get("nodes")) ? C_OK : C_WARN);
        setVal(vProc, run ? "运行中 pid=" + d.get("pid") : "已停止", run ? C_OK : C_WARN);
        long ts = 0, upt = 0;
        try { ts = Long.parseLong(d.get("ts")); } catch (Exception ignored) {}
        try { upt = Long.parseLong(d.get("uptime")); } catch (Exception ignored) {}
        if (upt == 0 && ts > 0 && boot > 0) upt = ts;
        long rt = (upt > 0 && boot > 0 && upt >= boot) ? (upt - boot) : -1;
        setVal(vRun, rt >= 0 ? fmt(rt) : "--", C_TXT);

        parseStats(d.get("stats"), vUnload, vRevive);
        setVal(vMode, mode.equals("monitor") ? "监控" : "卸载", C_TXT);
        setVal(vIv, lastIv + "s", C_TXT);
        boolean aggr = "1".equals(d.get("strength"));
        setVal(vStr, aggr ? "激进" : "标准", aggr ? C_WARN : C_TXT);

        if (d.get("version") != null) vVersion.setText(d.get("version"));

        renderDims(d);

        serverOn = "1".equals(d.get("server")) ? 1 : 0;
        bServer.setText(serverOn == 1 ? "本地服务 · 开" : "本地服务 · 关");

        if (!log.isEmpty()) logText.setText(log);
        else if (d.get("log3") != null) logText.setText(d.get("log3").replace("|", "\n"));

        if (bootlog != null && !bootlog.isEmpty()) {
            bootText.setText(bootlog);
            bootText.setTextColor(bootlog.contains("DISABLE") ? C_ERR : (bootlog.contains("CLEAR") ? C_OK : C_TXT2));
        } else if (d.get("bootlog") != null) {
            bootText.setText(d.get("bootlog").replace("|", "\n"));
        }

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
        g.setColor(Color.argb(30, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF));
        t.setBackground(g);
    }

    private void setSeg(LinearLayout root, String title, int value) {
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
                        g.setCornerRadius(dp(13));
                        g.setColor(on ? 0xFF34D399 : 0x00111A30);
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

    /* ================= root 执行 ================= */

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

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private void toast2(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
