package com.kazaf.kouninstall;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
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
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
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
 * v3.2 — SukiSU Miuix 风格重构:
 *   - 悬浮胶囊底部导航 (Miuix 风格, 选中主题色渐变 + Q弹)
 *   - 15 色主题系统 (Monet 近似) + 明暗模式 (浅/深/跟随系统), SharedPreferences 持久化
 *   - 液态玻璃卡片 + 流动光斑背景 + 呼吸状态灯
 *   - root 缺失全屏提示: 请赋予应用 root 权限后重新打开
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
    /* 主题色板 (与 WebUI 一致, Monet 15 色) */
    private static final int[] PALETTE = {
            0xFF22D3EE, 0xFF38BDF8, 0xFF818CF8, 0xFFC084FC, 0xFFF472B6,
            0xFFFB7185, 0xFFF87171, 0xFFFB923C, 0xFFFBBF24, 0xFFFACC15,
            0xFFA3E635, 0xFF34D399, 0xFF2DD4BF, 0xFFF9A8D4, 0xFFFDA4AF
    };
    private static final String MOD = "/data/adb/modules/ko_uninstall";
    private static final String DATA = MOD + "/data";
    private static final long POLL_MS = 3000;
    private static final String LOGO_B64 = "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";
    private static final String PREFS = "ko_ui";
    private static final String K_SEED = "seed";
    private static final String K_MODE = "ui_mode";
    private static final String K_MONET = "monet";
    private static final String K_BLUR = "blur";
    private static final String K_FLOAT = "float_bar";
    private static final String K_LIQUID = "liquid";
    private static final String K_BADGE = "badge";
    private static final String K_SCALE = "scale";
    private static final String K_LOG_LINES = "log_lines";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService exe = Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;

    /* ================= 视图 ================= */
    private LinearLayout home, detect, settings, themeSc, col;
    private TextView vVersion, liveDot, headText, msgText, guardLine;
    private TextView vInte, vNodes, vProc, vRun, vUnload, vRevive, vMode, vIv, vStr;
    private TextView[] dimChips = new TextView[11];
    private TextView logText, bootText;
    private TextView vModel, vKernel, vRunInfo;
    private String kernelVer = null;
    private String modelName = null;
    private String lastUn = "0", lastRv = "0";
    private Button bClear, bServer, bUp;
    private FrameLayout root;
    private LinearLayout noRoot;
    private View navSel;
    private LinearLayout navBar;
    private LinearLayout heroCard;
    private final java.util.Map<Integer, TextView> navIcons = new java.util.HashMap<>();
    private final java.util.Map<Integer, TextView> navLabels = new java.util.HashMap<>();
    private final Map<String, String> lastD = new HashMap<>();
    private boolean busy = false, rootOk = false, modOk = false;
    private long lastIv = 5;
    private int serverOn = 0, curTab = 0;
    private Runnable poller;
    private BackgroundView bgView;
    private int themeSeed = 0xFF22D3EE;
    private String uiMode = "system";
    private boolean monetOn = true, blurOn = true, floatBar = true, liquidOn = true, badgeOn = false;
    private float scale = 1.0f;
    private int logLines = 4;
    private final java.util.List<View> themeViews = new java.util.ArrayList<>();

    /* ================= 环形动态组件 (Scene 风格) ================= */
    private class RingView extends View {
        private float progress = 0f;
        private int arcColor = C_OK;
        private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pArc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float sw = dp(6);
        private boolean spin = false;
        private float spinAngle = -90f;
        private ValueAnimator anim;
        public RingView(android.content.Context c) {
            super(c);
            pTrack.setStyle(Paint.Style.STROKE);
            pTrack.setStrokeWidth(sw);
            pTrack.setStrokeCap(Paint.Cap.ROUND);
            pArc.setStyle(Paint.Style.STROKE);
            pArc.setStrokeWidth(sw);
            pArc.setStrokeCap(Paint.Cap.ROUND);
        }
        public void setProgress(float p) {
            spin = false;
            progress = Math.max(0f, Math.min(1f, p));
            stopSpin();
            invalidate();
        }
        public void setColor(int c) { arcColor = c; pArc.setColor(c); invalidate(); }
        /** 旋转态: 检测中/处理中 */
        public void spin() {
            spin = true;
            if (anim != null) { anim.cancel(); }
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setDuration(1100);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(a -> { spinAngle = -90f + a.getAnimatedFraction() * 360f; invalidate(); });
            anim.start();
        }
        private void stopSpin() { if (anim != null) { anim.cancel(); anim = null; } }
        @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); stopSpin(); }
        @Override protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - sw / 2f - dp(1);
            pTrack.setColor(dark ? 0x26FFFFFF : 0x14000000);
            cv.drawCircle(cx, cy, r, pTrack);
            if (spin) {
                pArc.setColor(arcColor);
                RectF rect = new RectF(cx - r, cy - r, cx + r, cy + r);
                cv.drawArc(rect, spinAngle, 300f, false, pArc);
            } else if (progress > 0f) {
                pArc.setColor(arcColor);
                RectF rect = new RectF(cx - r, cy - r, cx + r, cy + r);
                cv.drawArc(rect, -90f, progress * 360f, false, pArc);
            }
        }
    }

    private RingView heroRing;
    private RingView detectRing;
    private android.widget.ProgressBar detectBar;

    /* ================= 深浅色模式 ================= */
    private boolean dark = true;
    private boolean isDarkNow() {
        int m = uiMode.equals("light") ? 1 : (uiMode.equals("dark") ? 0 : 2);
        if (m == 2) {
            int night = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return m == 0;
    }
    private int modeBg1() { return dark ? 0xFF0B1020 : 0xFFF2F5FB; }
    private int modeBg2() { return dark ? 0xFF122052 : 0xFFDDE7F7; }
    private int modeBg3() { return dark ? 0xFF0B1020 : 0xFFF7F9FD; }
    private int modeTxt() { return dark ? 0xFFF2F5FF : 0xFF1A1B2E; }
    private int modeTxt2() { return dark ? 0xFFB9C3E0 : 0xFF56627E; }
    private int modeTxt3() { return dark ? 0xFF7E8BB0 : 0xFF8B95AD; }
    private int modeCard() { return dark ? 0x2A1D4ED8 : 0xE6FFFFFF; }
    private int modeStroke() { return dark ? 0x40FFFFFF : 0x40A0B0CC; }
    private int modeGlass() { return dark ? 0x1E111A30 : 0x2680A0CC; }
    private int modeNav() { return dark ? 0xE61A2338 : 0xEEFFFFFF; }
    private int modeNavStroke() { return dark ? 0x59FFFFFF : 0x3390A0BC; }

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
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        themeSeed = prefs.getInt(K_SEED, 0xFF22D3EE);
        uiMode = prefs.getString(K_MODE, "system");
        monetOn = prefs.getBoolean(K_MONET, true);
        blurOn = prefs.getBoolean(K_BLUR, true);
        floatBar = prefs.getBoolean(K_FLOAT, true);
        liquidOn = prefs.getBoolean(K_LIQUID, true);
        badgeOn = prefs.getBoolean(K_BADGE, false);
        scale = prefs.getFloat(K_SCALE, 1.0f);
        logLines = prefs.getInt(K_LOG_LINES, 4);
        dark = isDarkNow();
        applyUiMode();

        root = new FrameLayout(this);
        root.setBackgroundColor(modeBg1());

        // 液态玻璃背景: 流动光斑
        bgView = new BackgroundView(this);
        root.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        // 内容四屏
        home = screen();
        detect = screen();
        settings = screen();
        themeSc = screen();
        root.addView(home, new FrameLayout.LayoutParams(-1, -1));
        root.addView(detect, new FrameLayout.LayoutParams(-1, -1));
        root.addView(settings, new FrameLayout.LayoutParams(-1, -1));
        root.addView(themeSc, new FrameLayout.LayoutParams(-1, -1));

        buildHome();
        buildDetect();
        buildSettings();
        buildTheme();

        // 底部悬浮胶囊导航 (Miuix 风格, Apple 悬浮底栏)
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        if (liquidOn) {
            GradientDrawable navBg = new GradientDrawable(Orientation.TL_BR,
                    dark
                            ? new int[]{blurOn ? 0xE61A2338 : 0xF01A2338, 0xC2101A30}
                            : new int[]{blurOn ? 0xE8FFFFFF : 0xF5FFFFFF, 0xD0E8F4FF});
            navBg.setCornerRadius(dp(30));
            navBg.setStroke(dp(1), modeNavStroke());
            navBar.setBackground(navBg);
        } else {
            GradientDrawable navBg = new GradientDrawable();
            navBg.setCornerRadius(dp(30));
            navBg.setColor(modeNav());
            navBar.setBackground(navBg);
        }
        navBar.setPadding(dp(6), dp(8), dp(6), dp(8));
        navBar.setElevation(dp(10));
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(70));
        navLp.gravity = Gravity.BOTTOM;
        navLp.leftMargin = floatBar ? dp(36) : 0;
        navLp.rightMargin = floatBar ? dp(36) : 0;
        navLp.bottomMargin = floatBar ? dp(14) : 0;
        root.addView(navBar, navLp);

        addNavTab(navBar, 0, "◉", "首页");
        addNavTab(navBar, 1, "◈", "检测");
        addNavTab(navBar, 2, "⚙", "设置");
        repaintNav();

        // root 缺失提示层 (全屏覆盖)
        noRoot = new LinearLayout(this);
        noRoot.setOrientation(LinearLayout.VERTICAL);
        noRoot.setGravity(Gravity.CENTER);
        noRoot.setBackground(glassBg(dp(0), 0xD9 | (modeBg1() & 0xFFFFFF), 0x00FFFFFF));
        TextView nrTitle = tv("需要 root 权限", 22, Typeface.BOLD, modeTxt());
        TextView nrMsg = tv("KO卸载 需要 root 才能读取与卸载 inte.ko。\n请先在 KernelSU / Magisk 中允许本应用获取 root，\n然后重新打开应用。", 14, Typeface.NORMAL, modeTxt2());
        nrMsg.setGravity(Gravity.CENTER);
        nrMsg.setLineSpacing(dp(4), 1.0f);
        TextView nrSub = tv("检测结果：未授予 root 或模块未安装", 12, Typeface.NORMAL, modeTxt3());
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
        // 初始仅显示首页, 其余屏隐藏 (防止叠加)
        curTab = 0;
        home.setVisibility(View.VISIBLE);
        detect.setVisibility(View.GONE);
        settings.setVisibility(View.GONE);
        themeSc.setVisibility(View.GONE);
        enterAnim(home);
    }

    /** 明暗模式切换后整体重建 (保持 poll 数据) */
    private void rebuildAll() {
        if (poller != null) ui.removeCallbacks(poller);
        Map<String, String> keep = new HashMap<>(lastD);
        buildUi();
        startPoll();
        if (!keep.isEmpty()) ui.post(() -> render(keep, "", 0, ""));
    }

    /* ---------- 主题 ---------- */
    private int themeDeep() {
        int r = Math.max(0, Math.min(255, (themeSeed >> 16 & 0xFF) - 24));
        int g = Math.max(0, Math.min(255, (themeSeed >> 8 & 0xFF) - 24));
        int b = Math.max(0, Math.min(255, (themeSeed & 0xFF) - 24));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private int themeA(float a) {
        int al = (int) (a * 255);
        return (al << 24) | (themeSeed & 0xFFFFFF);
    }
    private void applyUiMode() {
        int mode = uiMode.equals("light") ? 1 : (uiMode.equals("dark") ? 0 : 2);
        if (mode == 2) {
            int night = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            mode = night == android.content.res.Configuration.UI_MODE_NIGHT_YES ? 0 : 1;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                mode == 1 ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
    }
    /** 切换明暗模式 (带整体重建) */
    private void switchMode(String m) {
        uiMode = m;
        prefs.edit().putString(K_MODE, m).apply();
        applyUiMode();
        rebuildAll();
    }
    private void repaintTheme() {
        prefs.edit().putInt(K_SEED, themeSeed).apply();
        repaintNav();
        repaintHero();
        repaintSegs();
        repaintSwatches();
        if (bgView != null) bgView.invalidate();
    }
    private void repaintNav() {
        if (navBar == null) return;
        for (int i = 0; i < navBar.getChildCount(); i++) {
            View c = navBar.getChildAt(i);
            boolean on = (Integer) c.getTag() == curTab;
            TextView ic = navIcons.get(i);
            TextView lb = navLabels.get(i);
            if (ic != null) {
                ic.setTextColor(on ? themeSeed : modeTxt2());
                ic.setShadowLayer(on ? dp(8) : 0, 0, 0, themeA(0.6f));
            }
            if (lb != null) {
                lb.setTextColor(on ? modeTxt() : modeTxt2());
                lb.setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            }
            c.setBackground(on ? glassBg(dp(22), themeA(0.30f), 0x55FFFFFF) : null);
            c.setElevation(on ? dp(4) : 0);
        }
    }
    private void repaintHero() {
        if (heroCard == null) return;
        GradientDrawable hg = new GradientDrawable(Orientation.TL_BR,
                new int[]{themeA(0.40f), themeA(0.16f), modeBg1()});
        hg.setCornerRadius(dp(30));
        heroCard.setBackground(hg);
    }
    private void repaintSegs() {
        findAndSetTheme(col, null);
    }
    private void repaintSwatches() {
        applySwatchState();
    }
    private void applySwatchState() {
        if (swatches == null) return;
        for (int i = 0; i < swatches.length; i++) {
            if (swatches[i] == null) continue;
            boolean on = monetOn && PALETTE[i] == themeSeed;
            swatches[i].setSelected(on);
            swatches[i].setEnabled(monetOn);
            GradientDrawable g = (GradientDrawable) swatches[i].getBackground();
            if (!monetOn) {
                g.setStroke(dp(1), 0x22FFFFFF);
                swatches[i].setAlpha(0.35f);
                continue;
            }
            swatches[i].setAlpha(1f);
            if (on) g.setStroke(dp(2), 0xFFFFFFFF);
            else g.setStroke(dp(1), 0x44FFFFFF);
        }
    }
    private Button[] swatches;
    private void buildPaletteCard(LinearLayout col) {
        LinearLayout pc = glass();
        pc.addView(tv("外观主题", 16, Typeface.BOLD, C_TXT));
        pc.addView(tv("主题色 (Monet 15 色)", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(8)));
        swatches = new Button[15];
        for (int row = 0; row < 3; row++) {
            LinearLayout grid = hRow();
            for (int c = 0; c < 5; c++) {
                int i = row * 5 + c;
                swatches[i] = swatchBtn(PALETTE[i], i);
                grid.addView(swatches[i], lpwH(1f));
            }
            pc.addView(grid, lp(-1, -2, 0f, row == 0 ? dp(10) : dp(8)));
        }
        pc.addView(tv("明暗模式", 12, Typeface.NORMAL, C_TXT2), lp(-1, -2, 0f, dp(14)));
        LinearLayout tseg = new LinearLayout(this);
        tseg.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable tbg = new GradientDrawable();
        tbg.setCornerRadius(dp(16));
        tbg.setColor(0x1A111A30);
        tseg.setBackground(tbg);
        tseg.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[] modes = {"跟随系统", "浅色", "深色"};
        String[] keys = {"system", "light", "dark"};
        for (int i = 0; i < 3; i++) {
            Button b = new Button(this);
            b.setText(modes[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            final String mk = keys[i];
            final String ml = modes[i];
            b.setTag(mk);
            b.setOnClickListener(v -> {
                uiMode = mk;
                prefs.edit().putString(K_MODE, mk).apply();
                applyUiMode();
                repaintUiModeSeg(tseg);
                toast("明暗模式: " + ml);
            });
            tseg.addView(b, lpwH(1f));
        }
        repaintUiModeSeg(tseg);
        pc.addView(tseg, lp(-1, -2, 0f, dp(8)));
        col.addView(pc, lp(-1, -2, 0f, dp(8)));
    }
    private void repaintUiModeSeg(LinearLayout seg) {
        if (seg == null) return;
        for (int i = 0; i < seg.getChildCount(); i++) {
            Button b = (Button) seg.getChildAt(i);
            boolean on = uiMode.equals(b.getTag().toString());
            b.setTextColor(on ? 0xFF0B1020 : C_TXT2);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(13));
            g.setColor(on ? themeSeed : 0x00111A30);
            b.setBackground(g);
        }
    }
    private Button swatchBtn(int color, int idx) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setText("●");
        b.setTextColor(color);
        b.setPadding(dp(2), dp(2), dp(2), dp(2));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(color);
        g.setStroke(dp(1), 0x44FFFFFF);
        b.setBackground(g);
        b.setTag(idx);
        b.setOnClickListener(v -> {
            themeSeed = PALETTE[(Integer) v.getTag()];
            repaintTheme();
            toast("主题色已切换");
        });
        return b;
    }

    private void addNavTab(LinearLayout nav, int idx, String icon, String label) {
        FrameLayout item = new FrameLayout(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        TextView ic = tv(icon, 19, Typeface.NORMAL, modeTxt2());
        ic.setIncludeFontPadding(false);
        ic.setGravity(Gravity.CENTER);
        ic.setMinHeight(dp(28));
        TextView lb = tv(label, 12, Typeface.NORMAL, modeTxt2());
        lb.setIncludeFontPadding(false);
        lb.setGravity(Gravity.CENTER);
        lb.setMinHeight(dp(20));
        inner.addView(ic);
        inner.addView(lb, lp(-1, -2, 0f, dp(2)));
        inner.setPadding(dp(8), dp(4), dp(8), dp(4));
        item.addView(inner, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        // 导航栏角标 (导航栏角标开关)
        TextView badge = tv("", 9, Typeface.BOLD, 0xFFFFFFFF);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0xFFF87171);
        badge.setBackground(bg);
        badge.setGravity(Gravity.CENTER);
        badge.setMinWidth(dp(18));
        badge.setPadding(dp(5), dp(1), dp(5), dp(1));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-2, -2);
        blp.gravity = Gravity.TOP | Gravity.RIGHT;
        blp.topMargin = dp(1);
        blp.rightMargin = dp(8);
        item.addView(badge, blp);
        badge.setTag("badge");
        badge.setVisibility(badgeOn ? View.VISIBLE : View.GONE);
        badge.setText("0");
        navIcons.put(idx, ic);
        navLabels.put(idx, lb);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        nav.addView(item, p);
        item.setTag(idx);
        item.setOnClickListener(v -> {
            int i = (Integer) v.getTag();
            pressAnim(v);
            switchTab(i);
        });
    }

    private void switchTab(int idx) {
        if (curTab == idx) return;
        curTab = idx;
        home.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        detect.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        settings.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        themeSc.setVisibility(idx == 3 ? View.VISIBLE : View.GONE);
        View target = idx == 0 ? home : (idx == 1 ? detect : (idx == 2 ? settings : themeSc));
        enterAnim(target);
        repaintNav();
        if (idx == 1 && dimChips[0] != null && "检测中".equals(dimChips[0].getText().toString())) runDetect();
    }

    /** 从设置页进入主题设置子页 */
    private void openTheme() {
        switchTab(3);
    }

    private LinearLayout screen() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        col.setPadding(p, dp(10), p, dp(150));
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
        heroCard = hero;
        GradientDrawable hg = new GradientDrawable(Orientation.TL_BR,
                new int[]{themeA(0.40f), themeA(0.16f), modeBg1()});
        hg.setCornerRadius(dp(30));
        hero.setBackground(hg);
        hero.setPadding(dp(14), dp(12), dp(14), dp(11));

        LinearLayout hrow = new LinearLayout(this);
        hrow.setOrientation(LinearLayout.HORIZONTAL);
        hrow.setGravity(Gravity.CENTER_VERTICAL);
        // Inte logo (用户指定图标)
        ImageView logo = logoView();
        hrow.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView hTitle = tv("KO卸载", 22, Typeface.BOLD, modeTxt());
        hTitle.setShadowLayer(dp(10), 0, 0, themeA(0.5f));
        hrow.addView(hTitle, lp(0, -2, 1f, 0, dp(12)));
        liveDot = tv("●", 13, Typeface.BOLD, C_OK);
        liveDot.setGravity(Gravity.CENTER);
        GradientDrawable ldbg = new GradientDrawable();
        ldbg.setCornerRadius(dp(14));
        ldbg.setColor(modeGlass());
        liveDot.setBackground(ldbg);
        liveDot.setPadding(dp(10), dp(5), dp(10), dp(5));
        // 环形动态指示 (状态/处理中旋转)
        heroRing = new RingView(this);
        heroRing.setColor(C_OK);
        heroRing.setProgress(1f);
        hrow.addView(heroRing, lp(dp(46), dp(46), 0f, 0, dp(10)));
        hero.addView(hrow);

        headText = tv("读取中", 24, Typeface.BOLD, C_OK);
        headText.setShadowLayer(dp(16), 0, 0, themeA(0.35f));
        hero.addView(headText, lp(-1, -2, 0f, dp(8)));
        msgText = tv("正在读取守护进程状态…", 12, Typeface.NORMAL, modeTxt2());
        hero.addView(msgText, lp(-1, -2, 0f, dp(2)));
        LinearLayout glRow = hRow();
        guardLine = tv("主动防护 读取中", 11, Typeface.BOLD, C_INFO);
        GradientDrawable gg = new GradientDrawable();
        gg.setCornerRadius(dp(16));
        gg.setColor(modeGlass());
        guardLine.setBackground(gg);
        guardLine.setPadding(dp(12), dp(5), dp(12), dp(5));
        glRow.addView(guardLine, lpwH(1f));
        vVersion = tv("v3.2", 11, Typeface.NORMAL, modeTxt3());
        vVersion.setGravity(Gravity.CENTER);
        vVersion.setBackground(gg);
        vVersion.setPadding(dp(10), dp(5), dp(10), dp(5));
        glRow.addView(vVersion, lp(-1, -2, 0f, 0, dp(8)));
        hero.addView(glRow, lp(-1, -2, 0f, dp(8)));

        // hero 底部: 模式 / 间隔 / 强度 三 chip (对齐 WebUI hero 行)
        LinearLayout hchips = hRow();
        vMode = heroChip(hchips, "模式", "卸载", modeTxt());
        vIv = heroChip(hchips, "间隔", "5s", modeTxt());
        vStr = heroChip(hchips, "强度", "标准", modeTxt());
        hero.addView(hchips, lp(-1, -2, 0f, dp(8)));
        col.addView(hero, lp(-1, -2, 0f, dp(8)));

        // 2x2 指标玻璃卡
        LinearLayout m1 = hRow();
        vInte = cell(m1, "inte 模块", "--");
        vNodes = cell(m1, "/proc 节点", "--/3");
        col.addView(m1, lp(-1, -2, 0f, dp(8)));
        LinearLayout m2 = hRow();
        vProc = cell(m2, "守护进程", "--");
        vRun = cell(m2, "本次运行", "--");
        col.addView(m2, lp(-1, -2, 0f, dp(8)));

        // 统计: 卸载 / 复活 两大数 (不挤, 大字号)
        LinearLayout st = hRow();
        GradientDrawable stg = glassBg(dp(22), blurOn ? 0x221D4ED8 : (dark ? 0xE61A2338 : 0xF2FFFFFF), modeStroke());
        st.setBackground(stg);
        st.setPadding(dp(6), dp(8), dp(6), dp(8));
        vUnload = statCell(st, "累计卸载", "0", C_OK);
        vRevive = statCell(st, "拦截复活", "0", C_WARN);
        col.addView(st, lp(-1, -2, 0f, dp(8)));

        // 快捷操作 2x2 网格
        LinearLayout q = glass();
        q.addView(tv("快捷操作", 13, Typeface.BOLD, modeTxt()));
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        bClear = btn("立即清除", C_ERR);
        bClear.setOnClickListener(v -> clearNow());
        bServer = btn("本地服务", C_INFO);
        bServer.setOnClickListener(v -> toggleServer());
        row1.addView(bClear, lpwH(1f));
        row1.addView(bServer, lpwH(1f));
        q.addView(row1, lp(-1, -2, 0f, dp(8)));
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        bUp = btn("检查更新", C_OK);
        bUp.setOnClickListener(v -> checkUpdate());
        Button bLink = btn("打开项目", C_TXT2);
        bLink.setOnClickListener(v -> openProject());
        row2.addView(bUp, lpwH(1f));
        row2.addView(bLink, lpwH(1f));
        q.addView(row2, lp(-1, -2, 0f, dp(6)));
        col.addView(q, lp(-1, -2, 0f, dp(8)));

        // 设备信息列表 (KernelSU 主页风格: 图标 + 标签 + 值, 放底部滚动区)
        LinearLayout info = glass();
        info.addView(tv("设备信息", 13, Typeface.BOLD, modeTxt()));
        vModel = infoRow(info, "◆", "设备型号", modelName != null ? modelName : Build.MODEL);
        vKernel = infoRow(info, "❖", "内核版本", "读取中…");
        vRunInfo = infoRow(info, "✦", "复核策略", "卸载 · 5s · 标准");
        col.addView(info, lp(-1, -2, 0f, dp(8)));
    }

    /** Inte logo (用户指定图标, 圆形裁切) */
    private ImageView logoView() {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            byte[] b = android.util.Base64.decode(LOGO_B64, android.util.Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
            if (bmp != null) iv.setImageBitmap(bmp);
        } catch (Exception ignored) { }
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(24));
        g.setColor(dark ? 0xFF12223A : 0xFFE8EEF8);
        g.setStroke(dp(1), dark ? 0x66FFFFFF : 0x33A0B0CC);
        iv.setBackground(g);
        return iv;
    }

    /** KernelSU 信息列表行: 图标 + 标签 + 值 */
    private TextView infoRow(LinearLayout parent, String icon, String label, String initVal) {
        LinearLayout r = hRow();
        r.addView(tv(icon, 13, Typeface.NORMAL, themeSeed), lpwH(0f));
        r.addView(tv(label, 12, Typeface.NORMAL, modeTxt3()), lpwH(0f));
        TextView val = tv(initVal, 12, Typeface.BOLD, modeTxt());
        r.addView(val, lpwH(1f));
        r.addView(tv("›", 14, Typeface.NORMAL, modeTxt3()));
        parent.addView(r, lp(-1, -2, 0f, dp(5)));
        return val;
    }

    /** hero 内 模式/间隔/强度 chip (对齐 WebUI) */
    private TextView heroChip(LinearLayout parent, String label, String initVal, int valColor) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(dark ? 0x1E111A30 : 0x1E90A0BC);
        g.setStroke(dp(1), dark ? 0x33FFFFFF : 0x33A0B0CC);
        chip.setBackground(g);
        chip.setPadding(dp(8), dp(5), dp(8), dp(5));
        TextView lb = tv(label, 10, Typeface.NORMAL, modeTxt3());
        TextView vl = tv(initVal, 12, Typeface.BOLD, valColor);
        chip.addView(lb);
        chip.addView(vl, lp(-1, -2, 0f, 0, dp(5)));
        parent.addView(chip, lpwH(1f));
        return vl;
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
        TextView t = tv("内核检测 · 12 维", 17, Typeface.BOLD, modeTxt());
        t.setShadowLayer(dp(8), 0, 0, 0x5560A5FA);
        dhead.addView(t, lpwH(1f));
        detectRing = new RingView(this);
        detectRing.setColor(C_INFO);
        detectRing.setProgress(0f);
        dhead.addView(detectRing, lp(dp(40), dp(40), 0f, 0, dp(10)));
        Button bDetect = btn("重新检测", C_INFO);
        bDetect.setOnClickListener(v -> { enterAnim(bDetect); runDetect(); });
        dhead.addView(bDetect);
        col.addView(dhead, lp(-1, -2, 0f, dp(4)));

        // 动态进度条 (检测中流动)
        detectBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        detectBar.setIndeterminate(true);
        detectBar.setMax(1000);
        detectBar.setPadding(0, dp(2), 0, dp(2));
        col.addView(detectBar, lp(-1, -2, 0f, dp(4)));

        LinearLayout dc = glass();
        String[] labels = {
                "/proc/inte_ko", "/proc/inte_systbl", "/proc/inte_status",
                "/proc/modules·inte", "/sys/module/inte", "kallsyms·inte",
                "/dev·inte 节点", "inte.ko 文件驻留", "内核 cmdline 特征",
                "refcnt", "initstate"
        };
        for (int i = 0; i < labels.length; i++) {
            LinearLayout r = hRow();
            r.addView(tv(labels[i], 12, Typeface.NORMAL, modeTxt2()), lpwH(1f));
            TextView chip = tv("检测中", 12, Typeface.BOLD, modeTxt3());
            GradientDrawable cbg = new GradientDrawable();
            cbg.setCornerRadius(dp(11));
            cbg.setColor(dark ? 0x18111A30 : 0x1A90A0BC);
            chip.setBackground(cbg);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(4), dp(12), dp(4));
            r.addView(chip);
            dimChips[i] = chip;
            dc.addView(r, lp(-1, -2, 0f, dp(4)));
            enterDelayed(r, 80 + i * 45);
        }
        col.addView(dc, lp(-1, -2, 0f, dp(8)));

        TextView hint = tv("威胁维度命中即标红；inte.ko 文件驻留与 cmdline 特征为环境事实，中性展示。", 11, Typeface.NORMAL, modeTxt3());
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

        // 分组: 外观 (KernelSU 设置列表风格)
        LinearLayout ap = glass();
        ap.addView(tv("外观", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        settingRow(ap, "◐", "界面风格", "选择应用的界面风格", "Miuix", null);
        settingRow(ap, "◈", "主题设置", "自定义更多主题选项", "", this::openTheme);
        col.addView(ap, lp(-1, -2, 0f, dp(8)));

        // 分组: 运行策略
        LinearLayout cc = glass();
        cc.addView(tv("运行策略", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        cc.addView(tv("模式", 12, Typeface.NORMAL, modeTxt2()), lp(-1, -2, 0f, dp(6)));
        cc.addView(segment(new String[]{"卸载模式", "监控模式"}, new int[]{0, 1}, v -> setMode(v == 0 ? "unload" : "monitor")));
        cc.addView(tv("复核间隔", 12, Typeface.NORMAL, modeTxt2()), lp(-1, -2, 0f, dp(8)));
        cc.addView(segment(new String[]{"3s", "5s", "10s", "30s", "60s"}, new int[]{3, 5, 10, 30, 60}, v -> setIv(v)));
        cc.addView(tv("屏蔽强度", 12, Typeface.NORMAL, modeTxt2()), lp(-1, -2, 0f, dp(8)));
        cc.addView(segment(new String[]{"标准", "激进"}, new int[]{0, 1}, v -> setStrength(v)));
        col.addView(cc, lp(-1, -2, 0f, dp(8)));

        // 分组: 日志
        LinearLayout lc = glass();
        lc.addView(tv("守护日志", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        logText = tv("读取中…", 11, Typeface.NORMAL, modeTxt2());
        logText.setLineSpacing(dp(3), 1.0f);
        logText.setMaxLines(logLines);
        lc.addView(logText, lp(-1, -2, 0f, dp(6)));
        col.addView(lc, lp(-1, -2, 0f, dp(8)));

        LinearLayout bc = glass();
        bc.addView(tv("开机自保护 · 旧砖日志", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        bootText = tv("读取中…", 11, Typeface.NORMAL, modeTxt2());
        bootText.setLineSpacing(dp(3), 1.0f);
        bootText.setMaxLines(logLines);
        bc.addView(bootText, lp(-1, -2, 0f, dp(6)));
        col.addView(bc, lp(-1, -2, 0f, dp(8)));

        // 分组: 关于 (KernelSU 关于页风格)
        LinearLayout ac = glass();
        ac.addView(tv("关于", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        settingRow(ac, "✦", "作者 Kazaf", "KO卸载 v3.2 · 开源", "GitHub", this::openProject);
        col.addView(ac, lp(-1, -2, 0f, dp(8)));
    }

    /** KernelSU 设置条目: 图标 + 标题 + 副标题 + 右侧值/箭头 */
    private LinearLayout settingRow(LinearLayout parent, String icon, String title, String sub, String right, Runnable cb) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(2), dp(7), dp(2), dp(7));
        if (icon != null && !icon.isEmpty()) {
            TextView ic = tv(icon, 13, Typeface.NORMAL, themeSeed);
            ic.setGravity(Gravity.CENTER);
            GradientDrawable ig = new GradientDrawable();
            ig.setCornerRadius(dp(12));
            ig.setColor(themeA(0.14f));
            ic.setBackground(ig);
            ic.setPadding(dp(8), dp(6), dp(8), dp(6));
            r.addView(ic, lp(0, -2, 0f, 0, 0));
        }
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(tv(title, 13, Typeface.BOLD, modeTxt()));
        if (sub != null && !sub.isEmpty()) {
            tx.addView(tv(sub, 10, Typeface.NORMAL, modeTxt3()), lp(-1, -2, 0f, dp(1)));
        }
        r.addView(tx, lpwH(1f));
        if (right != null && !right.isEmpty()) {
            r.addView(tv(right, 12, Typeface.NORMAL, themeSeed));
        }
        r.addView(tv("›", 15, Typeface.NORMAL, modeTxt3()), lp(0, -2, 0f, 0, dp(6)));
        if (cb != null) {
            r.setOnClickListener(v -> { pressAnim(r); cb.run(); });
        }
        parent.addView(r, lp(-1, -2, 0f, dp(1)));
        return r;
    }

    /** KernelSU SwitchPreference 风格开关行 */
    private LinearLayout switchRow(LinearLayout parent, String icon, String title, String sub, boolean checked, java.util.function.Consumer<Boolean> cb) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(2), dp(7), dp(2), dp(7));
        if (icon != null && !icon.isEmpty()) {
            TextView ic = tv(icon, 13, Typeface.NORMAL, themeSeed);
            ic.setGravity(Gravity.CENTER);
            GradientDrawable ig = new GradientDrawable();
            ig.setCornerRadius(dp(12));
            ig.setColor(themeA(0.14f));
            ic.setBackground(ig);
            ic.setPadding(dp(8), dp(6), dp(8), dp(6));
            r.addView(ic, lp(0, -2, 0f, 0, 0));
        }
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(tv(title, 13, Typeface.BOLD, modeTxt()));
        if (sub != null && !sub.isEmpty()) {
            tx.addView(tv(sub, 10, Typeface.NORMAL, modeTxt3()), lp(-1, -2, 0f, dp(1)));
        }
        r.addView(tx, lpwH(1f));
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setShowText(false);
        sw.setOnCheckedChangeListener((b, v) -> { if (cb != null) cb.accept(v); });
        r.addView(sw);
        parent.addView(r, lp(-1, -2, 0f, dp(1)));
        return r;
    }

    /* ================= 主题设置页 (KernelSU 主题设置复刻) ================= */

    private void buildTheme() {
        LinearLayout col = themeCol();

        // 顶栏: 返回 + 标题
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = btn("‹ 返回", modeTxt3());
        back.setOnClickListener(v -> { pressAnim(back); switchTab(2); });
        TextView ttl = tv("主题设置", 18, Typeface.BOLD, modeTxt());
        ttl.setShadowLayer(dp(8), 0, 0, themeA(0.4f));
        top.addView(back);
        top.addView(ttl, lp(0, -2, 0f, 0, dp(14)));
        col.addView(top, lp(-1, -2, 0f, dp(4)));

        // 预览卡 (随主题实时变化)
        LinearLayout pv = glass();
        pv.addView(tv("预览", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.VERTICAL);
        mini.setGravity(Gravity.CENTER);
        GradientDrawable mg = new GradientDrawable(Orientation.TL_BR,
                new int[]{themeA(0.55f), themeA(0.2f), modeBg1()});
        mg.setCornerRadius(dp(20));
        mini.setBackground(mg);
        mini.setPadding(dp(10), dp(10), dp(10), dp(10));
        mini.addView(tv("KO卸载", 16, Typeface.BOLD, modeTxt()));
        mini.addView(tv("已生效 · inte 未加载 · 盯防中 5s", 10, Typeface.NORMAL, modeTxt2()), lp(-1, -2, 0f, dp(6)));
        pv.addView(mini, lp(-1, -2, 0f, dp(8)));
        // 迷你胶囊导航
        LinearLayout mininav = new LinearLayout(this);
        mininav.setOrientation(LinearLayout.HORIZONTAL);
        mininav.setGravity(Gravity.CENTER);
        GradientDrawable ng = new GradientDrawable();
        ng.setCornerRadius(dp(14));
        ng.setColor(modeNav());
        ng.setStroke(dp(1), modeNavStroke());
        mininav.setBackground(ng);
        mininav.setPadding(dp(6), dp(5), dp(6), dp(5));
        String[] navs = {"◉ 首页", "◈ 检测", "⚙ 设置"};
        for (int i = 0; i < navs.length; i++) {
            TextView t = tv(navs[i], 10, Typeface.NORMAL, i == 0 ? themeSeed : modeTxt3());
            t.setGravity(Gravity.CENTER);
            t.setBackground(glassBg(dp(12), i == 0 ? themeA(0.3f) : 0x00111A30, 0x44FFFFFF));
            t.setPadding(dp(8), dp(5), dp(8), dp(5));
            mininav.addView(t, lpwH(1f));
        }
        pv.addView(mininav, lp(-1, -2, 0f, dp(8)));
        col.addView(pv, lp(-1, -2, 0f, dp(8)));

        // 主题模式
        LinearLayout mc = glass();
        mc.addView(tv("主题模式", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        LinearLayout modeSeg = segment(new String[]{"跟随系统", "浅色", "深色"},
                new int[]{0, 1, 2},
                v -> switchMode(v == 0 ? "system" : (v == 1 ? "light" : "dark")));
        mc.addView(modeSeg, lp(-1, -2, 0f, dp(6)));
        col.addView(mc, lp(-1, -2, 0f, dp(8)));
        findAndSet(modeSeg, uiMode.equals("system") ? 0 : (uiMode.equals("light") ? 1 : 2));

        // 主题色 (Monet 15 色)
        LinearLayout pc = glass();
        pc.addView(tv("主题色 (Monet 15 色)", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        swatches = new Button[15];
        for (int row = 0; row < 3; row++) {
            LinearLayout grid = hRow();
            for (int c = 0; c < 5; c++) {
                int i = row * 5 + c;
                swatches[i] = swatchBtn(PALETTE[i], i);
                grid.addView(swatches[i], lpwH(1f));
            }
            pc.addView(grid, lp(-1, -2, 0f, row == 0 ? dp(8) : dp(6)));
        }
        col.addView(pc, lp(-1, -2, 0f, dp(8)));
        applySwatchState();

        // 效果开关 (完整复刻 KernelSU 主题设置)
        LinearLayout ec = glass();
        ec.addView(tv("效果", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        switchRow(ec, "◐", "启用 Monet 颜色", "使用主题色板着色", monetOn, v -> {
            monetOn = v;
            prefs.edit().putBoolean(K_MONET, v).apply();
            applySwatchState();
            toast(v ? "Monet 颜色已启用" : "Monet 颜色已关闭");
        });
        switchRow(ec, "◫", "模糊", "启用顶栏和底栏的模糊效果", blurOn, v -> {
            blurOn = v;
            prefs.edit().putBoolean(K_BLUR, v).apply();
            rebuildAll();
        });
        switchRow(ec, "◧", "悬浮底栏", "使用 Apple 风格的悬浮底栏", floatBar, v -> {
            floatBar = v;
            prefs.edit().putBoolean(K_FLOAT, v).apply();
            rebuildAll();
        });
        switchRow(ec, "◍", "液态玻璃", "启用悬浮底栏的液态玻璃效果", liquidOn, v -> {
            liquidOn = v;
            prefs.edit().putBoolean(K_LIQUID, v).apply();
            rebuildAll();
        });
        switchRow(ec, "◉", "导航栏角标", "在导航栏显示已启用维度数量", badgeOn, v -> {
            badgeOn = v;
            prefs.edit().putBoolean(K_BADGE, v).apply();
            rebuildAll();
        });
        col.addView(ec, lp(-1, -2, 0f, dp(8)));

        // 滑块: 界面缩放 / 日志行数
        LinearLayout slc = glass();
        slc.addView(tv("界面缩放", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(2)));
        slc.addView(tv("调整全局显示比例", 11, Typeface.NORMAL, modeTxt3()), lp(-1, -2, 0f, dp(4)));
        final TextView scaleVal = tv((int) (scale * 100) + "%", 13, Typeface.BOLD, themeSeed);
        SeekBar scaleBar = new SeekBar(this);
        scaleBar.setMax(40);
        scaleBar.setProgress((int) ((scale - 0.8f) * 100));
        scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean from) {
                scale = 0.8f + p / 100f;
                scaleVal.setText((int) (scale * 100) + "%");
                prefs.edit().putFloat(K_SCALE, scale).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) { rebuildAll(); }
        });
        LinearLayout srow = hRow();
        srow.addView(scaleBar, lpwH(1f));
        srow.addView(scaleVal, lp(0, -2, 0f, 0, dp(10)));
        slc.addView(srow, lp(-1, -2, 0f, dp(4)));
        slc.addView(tv("模块描述最大行数", 13, Typeface.BOLD, modeTxt3()), lp(-1, -2, 0f, dp(8)));
        slc.addView(tv("超过此行数的内容将自动折叠", 11, Typeface.NORMAL, modeTxt3()), lp(-1, -2, 0f, dp(4)));
        final TextView linesVal = tv(logLines + " 行", 13, Typeface.BOLD, themeSeed);
        SeekBar linesBar = new SeekBar(this);
        linesBar.setMax(6);
        linesBar.setProgress(logLines - 2);
        linesBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean from) {
                logLines = p + 2;
                linesVal.setText(logLines + " 行");
                prefs.edit().putInt(K_LOG_LINES, logLines).apply();
                if (logText != null) logText.setMaxLines(logLines);
                if (bootText != null) bootText.setMaxLines(logLines);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        LinearLayout lrow = hRow();
        lrow.addView(linesBar, lpwH(1f));
        lrow.addView(linesVal, lp(0, -2, 0f, 0, dp(10)));
        slc.addView(lrow, lp(-1, -2, 0f, dp(4)));
        col.addView(slc, lp(-1, -2, 0f, dp(8)));
    }

    private LinearLayout themeCol() {
        ScrollView sc = (ScrollView) ((LinearLayout) themeSc).getChildAt(0);
        return (LinearLayout) sc.getChildAt(0);
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
        l.setBackground(glassBg(dp(26), blurOn ? modeCard() : (dark ? 0xE61A2338 : 0xF2FFFFFF), modeStroke()));
        l.setPadding(dp(13), dp(9), dp(13), dp(9));
        l.setElevation(dp(2));
        return l;
    }

    private TextView tv(String t, float sp, int style, int color) {
        TextView x = new TextView(this);
        x.setText(t);
        x.setTextSize(sp * scale);
        x.setTypeface(Typeface.DEFAULT, style);
        x.setTextColor(color);
        return x;
    }

    private Button btn(String txt, int accent) {
        Button b = new Button(this);
        b.setText(txt);
        b.setTextSize(12 * scale);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(dark ? 0xFFF2F5FF : 0xFF1A1B2E);
        b.setGravity(Gravity.CENTER);
        int base = 0x66000000 | (accent & 0xFFFFFF);
        GradientDrawable g = new GradientDrawable(Orientation.TL_BR,
                new int[]{base, 0x2A000000 | (accent & 0xFFFFFF)});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), dark ? 0x55FFFFFF : 0x33A0B0CC);
        b.setBackground(g);
        b.setElevation(dp(3));
        b.setPadding(dp(8), dp(7), dp(8), dp(7));
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
        box.setBackground(glassBg(dp(18), themeA(0.14f), dark ? 0x33FFFFFF : 0x33A0B0CC));
        box.setPadding(dp(6), dp(7), dp(6), dp(7));
        box.addView(tv(label, 10, Typeface.NORMAL, modeTxt3()));
        TextView val = tv(initVal, 14, Typeface.BOLD, modeTxt());
        box.addView(val, lp(-1, -2, 0f, dp(2)));
        parent.addView(box, lpwH(1f));
        return val;
    }

    private TextView statCell(LinearLayout parent, String label, String initVal, int valColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(tv(label, 10, Typeface.NORMAL, modeTxt3()));
        TextView val = tv(initVal, 22, Typeface.BOLD, valColor);
        val.setGravity(Gravity.CENTER);
        box.addView(val, lp(-1, -2, 0f, dp(2)));
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
        l.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(dark ? 0x1A111A30 : 0x1A90A0BC);
        l.setBackground(bg);
        l.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < opts.length; i++) {
            Button b = new Button(this);
            b.setText(opts[i]);
            b.setTextSize(12 * scale);
            b.setAllCaps(false);
            b.setTextColor(modeTxt2());
            b.setGravity(Gravity.CENTER);
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

            // 背景渐变 (深浅色感知)
            Paint bg = new Paint();
            bg.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{modeBg1(), modeBg2(), modeBg3()}, null, Shader.TileMode.CLAMP));
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
                    new int[]{themeA(0.13f), 0x00FFFFFF & themeSeed}, null, Shader.TileMode.CLAMP));
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
        if (kernelVer == null) {
            exe.execute(() -> {
                String k = su("uname -r");
                if (k != null) {
                    k = k.trim();
                    int nl = k.indexOf('\n');
                    if (nl > 0) k = k.substring(0, nl);
                    if (k.length() > 30) k = k.substring(0, 30);
                    if (!k.isEmpty()) kernelVer = k.trim();
                }
                String mn = su("getprop ro.product.marketname");
                if (mn != null) {
                    mn = mn.trim();
                    if (mn.isEmpty()) mn = null;
                }
                modelName = (mn != null && !mn.isEmpty()) ? mn : (Build.MANUFACTURER + " " + Build.MODEL).trim();
                if (vModel != null) ui.post(() -> vModel.setText(modelName));
            });
        }
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

        if (heroRing != null) {
            switch (state) {
                case "running": heroRing.setColor(C_WARN); heroRing.spin(); break;
                case "normal": heroRing.setColor(lk ? C_WARN : C_OK); heroRing.setProgress(lk ? 0.75f : 1f); break;
                case "failed": case "builtin": heroRing.setColor(C_ERR); heroRing.setProgress(0.35f); break;
                case "notfound": heroRing.setColor(C_INFO); heroRing.setProgress(0.7f); break;
                case "stopped": heroRing.setColor(C_WARN); heroRing.setProgress(0.5f); break;
                default: heroRing.setColor(lk ? C_WARN : C_OK); heroRing.spin();
            }
        }

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
        setVal(vRun, rt >= 0 ? fmt(rt) : "--", modeTxt());

        parseStats(d.get("stats"), vUnload, vRevive);
        setVal(vMode, mode.equals("monitor") ? "监控" : "卸载", modeTxt());
        setVal(vIv, lastIv + "s", modeTxt());
        boolean aggr = "1".equals(d.get("strength"));
        setVal(vStr, aggr ? "激进" : "标准", aggr ? C_WARN : modeTxt());
        if (vRunInfo != null) {
            vRunInfo.setText(mode.equals("monitor") ? "监控 · " + lastIv + "s · " + (aggr ? "激进" : "标准")
                    : "卸载 · " + lastIv + "s · " + (aggr ? "激进" : "标准"));
        }
        if (vKernel != null && kernelVer != null) vKernel.setText(kernelVer);

        if (d.get("version") != null) vVersion.setText(d.get("version"));

        renderDims(d);

        serverOn = "1".equals(d.get("server")) ? 1 : 0;
        bServer.setText(serverOn == 1 ? "本地服务 · 开" : "本地服务 · 关");

        if (!log.isEmpty()) logText.setText(log);
        else if (d.get("log3") != null) logText.setText(d.get("log3").replace("|", "\n"));

        if (bootlog != null && !bootlog.isEmpty()) {
            bootText.setText(bootlog);
            bootText.setTextColor(bootlog.contains("DISABLE") ? C_ERR : (bootlog.contains("CLEAR") ? C_OK : modeTxt2()));
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
        lastUn = un;
        lastRv = rv;
        setVal(u, un, C_OK);
        setVal(r, rv, C_WARN);
        updateBadges();
    }

    /** 导航栏角标 (badgeOn 时显示统计) */
    private void updateBadges() {
        if (navBar == null) return;
        for (int i = 0; i < navBar.getChildCount(); i++) {
            FrameLayout it = (FrameLayout) navBar.getChildAt(i);
            for (int j = 0; j < it.getChildCount(); j++) {
                View bd = it.getChildAt(j);
                if ("badge".equals(bd.getTag())) {
                    TextView t = (TextView) bd;
                    if (!badgeOn) { t.setVisibility(View.GONE); continue; }
                    String s = i == 0 ? lastUn : (i == 1 ? lastRv : "0");
                    t.setText(s);
                    t.setVisibility("0".equals(s) ? View.GONE : View.VISIBLE);
                }
            }
        }
    }

    private void renderDims(Map<String, String> d) {
        String[] threat = {"m_ko", "m_systbl", "m_status", "m_mod", "m_sysmod", "ksym", "m_dev"};
        for (int i = 0; i < threat.length; i++) {
            String v = d.get(threat[i]);
            if (v == null || v.equals("")) setDim(i, "检测中", modeTxt3());
            else if (v.equals("0")) setDim(i, "未确认", C_OK);
            else setDim(i, "存在", C_ERR);
        }
        String f = d.get("m_file");
        if (f == null || f.equals("")) setDim(7, "检测中", modeTxt3());
        else if (f.equals("1")) setDim(7, "驻留（正常）", C_INFO);
        else setDim(7, "未驻留", C_OK);
        String c = d.get("m_cmd");
        if (c == null || c.equals("")) setDim(8, "检测中", modeTxt3());
        else if (c.equals("1")) setDim(8, "静态特征", C_INFO);
        else setDim(8, "无特征", C_OK);
        String rf = d.get("m_ref");
        if (rf == null || rf.equals("")) setDim(9, "检测中", modeTxt3());
        else if (rf.equals("0")) setDim(9, "未确认", C_OK);
        else setDim(9, "存在 · " + rf, C_ERR);
        String in = d.get("m_init");
        if (in == null || in.equals("")) setDim(10, "检测中", modeTxt3());
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
                        c.setSelected(on);
                        ((Button) c).setTextColor(on ? 0xFF0B1020 : modeTxt2());
                        GradientDrawable g = new GradientDrawable();
                        g.setCornerRadius(dp(13));
                        g.setColor(on ? themeSeed : 0x00111A30);
                        c.setBackground(g);
                    }
                }
            }
            for (int i = 0; i < l.getChildCount(); i++) findAndSet(l.getChildAt(i), value);
        }
    }

    /** 主题切换后重绘 segment 选中态 (基于最近一次状态) */
    private void findAndSetTheme(View v, Object skip) {
        if (v instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) v;
            Object tg = l.getTag();
            if (tg instanceof String[]) {
                int n = l.getChildCount();
                for (int i = 0; i < n; i++) {
                    View c = l.getChildAt(i);
                    if (c instanceof Button && c.getTag() != null && c.isSelected()) {
                        GradientDrawable g = new GradientDrawable();
                        g.setCornerRadius(dp(13));
                        g.setColor(themeSeed);
                        c.setBackground(g);
                    }
                }
            }
            for (int i = 0; i < l.getChildCount(); i++) findAndSetTheme(l.getChildAt(i), skip);
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
        if (detectRing != null) { detectRing.setColor(C_INFO); detectRing.spin(); }
        if (detectBar != null) detectBar.setVisibility(View.VISIBLE);
        for (int i = 0; i < dimChips.length; i++) {
            final int idx = i;
            ui.post(() -> setDim(idx, "检测中", modeTxt3()));
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
                int done = 0;
                for (int i = 0; i < dimChips.length; i++) {
                    String s = dimChips[i].getText().toString();
                    if (!"检测中".equals(s)) done++;
                }
                if (detectRing != null) {
                    detectRing.setColor(C_OK);
                    detectRing.setProgress(dimChips.length == 0 ? 0f : (float) done / dimChips.length);
                }
                if (detectBar != null) detectBar.setVisibility(View.GONE);
                toast("检测完成");
                busy = false;
            });
        });
    }

    private void renderDimsFromDetect(Map<String, String> d) {
        String[] keys = {"ko", "systbl", "status", "mod", "sysmod", "ksym", "dev"};
        for (int i = 0; i < keys.length; i++) {
            String v = d.get(keys[i]);
            if (v == null || v.equals("")) setDim(i, "检测中", modeTxt3());
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
