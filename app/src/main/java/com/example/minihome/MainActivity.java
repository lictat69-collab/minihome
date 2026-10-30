package com.example.minihome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final class App {
        final String pkg, label; final int c1, c2, glyph;
        App(String p, String l, int a, int b, int g) { pkg = p; label = l; c1 = a; c2 = b; glyph = g; }
    }
    // Док: порядок и приложения меняются здесь. "settings" и "all" - служебные.
    static final App[] DOCK = {
        new App("org.smarttube.stable", "SmartTube", 0xFFFF8A4C, 0xFFD6336C, 0),
        new App("ru.vk.store.tv", "VK Store", 0xFF4C8DFF, 0xFF2B4ACB, 1),
        new App("com.google.android.youtube.tv", "YouTube", 0xFFF25F5C, 0xFFA61E2D, 2),
        new App("com.yandex.browser.tv", "Браузер", 0xFF9B6BFF, 0xFF5A3EC8, 3),
        new App("com.android.vending", "Google Play", 0xFF2FD6A3, 0xFF0E8F7A, 4),
        new App("settings", "Настройки", 0xFF9AA7BD, 0xFF55627A, 5),
        new App("all", "Все", 0x33FFFFFF, 0x33FFFFFF, 6)
    };
    static final String[] NAMES = {"Горный рассвет", "Лесной день", "Закат в горах", "Звёздная ночь", "Утренний туман"};
    static final String[] DAYS = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    static final String[] MONTHS = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
    static final int ROTATE_MS = 45000;
    static final int[][] PAL = {
        {0xFF27346B, 0xFF7A6AA8, 0xFFF4A261, 0xFFF8D6A0, 0xFFFFF4D6, 0xFF7D6FA6, 0xFF4A5584, 0xFF1F2A44},
        {0xFF3B7FB8, 0xFF7FB6D5, 0xFFCFE8EE, 0xFFEAF4F0, 0xFFFFFFFF, 0xFF7FA6A0, 0xFF4F7D6B, 0xFF2A4D3E},
        {0xFF3D2C5E, 0xFF8A4F7D, 0xFFE76F51, 0xFFFFC48A, 0xFFFFE2A8, 0xFF8A4F7D, 0xFF5A3A6B, 0xFF2B1F3D},
        {0xFF0B132B, 0xFF1C2B45, 0xFF3A506B, 0xFF5B7490, 0xFFE8EEF4, 0xFF2F4565, 0xFF1C2B45, 0xFF0E1A2D},
        {0xFF8FB8C9, 0xFFB9D3D9, 0xFFE9F1F2, 0xFFF4F7F4, 0xFFFFF4D6, 0xFFA9C3C8, 0xFF7C9EA0, 0xFF4E6E6C}
    };

    final Handler h = new Handler();
    float u = 1f;
    ImageView bottom, top;
    TextView clock, date, caption;
    LinearLayout dock, dots;
    int sceneNo = 0;
    String curName = "";
    String[] custom = new String[0];

    int px(float ref) { return Math.round(ref * u); }

    final Runnable rotate = new Runnable() {
        public void run() { nextWallpaper(); h.postDelayed(this, ROTATE_MS); }
    };
    final Runnable tick = new Runnable() {
        public void run() {
            Calendar c = Calendar.getInstance();
            clock.setText(String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
            date.setText(DAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)]);
            h.postDelayed(this, 15000);
        }
    };

    TextView text(String s, float ref, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(ref));
        t.setShadowLayer(px(8), 0, px(2), 0x66081228);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        u = getResources().getDisplayMetrics().widthPixels / 1920f;
        try {
            String[] l = getAssets().list("wallpapers");
            ArrayList<String> ok = new ArrayList<>();
            if (l != null) for (String s : l) {
                String x = s.toLowerCase();
                if (x.endsWith(".jpg") || x.endsWith(".jpeg") || x.endsWith(".png")) ok.add(s);
            }
            custom = ok.toArray(new String[0]);
        } catch (Exception ignored) {}

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClipChildren(false);
        bottom = new ImageView(this);
        top = new ImageView(this);
        bottom.setScaleType(ImageView.ScaleType.CENTER_CROP);
        top.setScaleType(ImageView.ScaleType.CENTER_CROP);
        top.setAlpha(0f);
        root.addView(bottom, new FrameLayout.LayoutParams(-1, -1));
        root.addView(top, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout cd = new LinearLayout(this);
        cd.setOrientation(LinearLayout.VERTICAL);
        cd.setGravity(Gravity.END);
        clock = text("00:00", 76, Color.WHITE, true);
        date = text("", 26, Color.WHITE, false);
        date.setPadding(0, px(8), 0, 0);
        cd.addView(clock);
        cd.addView(date);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, px(64), px(96), 0);
        root.addView(cd, cp);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setPadding(px(64), px(44), px(64), px(34));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x38FFFFFF);
        g.setStroke(Math.max(1, px(1.5f)), 0x4DFFFFFF);
        g.setCornerRadius(px(72));
        dock.setBackground(g);
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp.bottomMargin = px(170);
        root.addView(dock, dp);

        LinearLayout cap = new LinearLayout(this);
        cap.setOrientation(LinearLayout.VERTICAL);
        cap.addView(text("ОБОИ", 16, 0xD9FFFFFF, true));
        caption = text("", 30, Color.WHITE, false);
        caption.setPadding(0, px(6), 0, 0);
        cap.addView(caption);
        FrameLayout.LayoutParams cap2 = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        cap2.setMargins(px(96), 0, 0, px(64));
        root.addView(cap, cap2);

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams dp2 = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        dp2.setMargins(0, 0, px(96), px(76));
        root.addView(dots, dp2);

        buildDock();
        setContentView(root);
        bottom.setImageBitmap(nextBitmap());
        updateMeta();
    }

    // ---------- плитка иконки ----------
    class Tile extends View {
        final App a;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean ring;
        Tile(App a) {
            super(MainActivity.this);
            this.a = a;
            setOutlineProvider(new ViewOutlineProvider() {
                public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getWidth() * 0.29f); }
            });
            setElevation(px(8));
        }
        void setRing(boolean r) { ring = r; setElevation(px(r ? 30 : 8)); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            float s = getWidth(), r = s * 0.29f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, s, s, a.c1, a.c2, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(0, 0, s, s), r, r, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.012f);
            p.setColor(a.glyph == 6 ? 0x59FFFFFF : 0x55FFFFFF);
            c.drawRoundRect(new RectF(s * 0.01f, s * 0.01f, s * 0.99f, s * 0.99f), r, r, p);
            glyph(c, s);
            if (ring) {
                p.setStrokeWidth(s * 0.03f);
                p.setColor(0xF2FFFFFF);
                c.drawRoundRect(new RectF(s * 0.015f, s * 0.015f, s * 0.985f, s * 0.985f), r, r, p);
            }
        }
        void tri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
            Path t = new Path();
            t.moveTo(x1, y1); t.lineTo(x2, y2); t.lineTo(x3, y3); t.close();
            p.setStyle(Paint.Style.FILL);
            c.drawPath(t, p);
            p.setStyle(Paint.Style.STROKE);
        }
        void knob(Canvas c, float x, float y) {
            p.setStyle(Paint.Style.FILL); p.setColor(0xFF6B7890);
            c.drawCircle(x, y, 5, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(Color.WHITE);
            c.drawCircle(x, y, 5, p);
        }
        void glyph(Canvas c, float s) {
            c.save();
            float k = s * 0.485f / 60f;
            c.translate(s * 0.2575f, s * 0.2575f);
            c.scale(k, k);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3.5f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(Color.WHITE);
            switch (a.glyph) {
                case 0: {
                    c.drawRoundRect(new RectF(8, 14, 52, 46), 10, 10, p);
                    tri(c, 26, 23, 39, 30, 26, 37);
                    break;
                }
                case 1: {
                    Path bag = new Path();
                    bag.moveTo(14, 22); bag.lineTo(46, 22); bag.lineTo(43, 48); bag.lineTo(17, 48); bag.close();
                    c.drawPath(bag, p);
                    Path hd = new Path();
                    hd.moveTo(22, 22); hd.lineTo(22, 18); hd.arcTo(new RectF(22, 10, 38, 26), 180, 180); hd.lineTo(38, 22);
                    c.drawPath(hd, p);
                    break;
                }
                case 2: {
                    c.drawCircle(30, 30, 21, p);
                    tri(c, 25, 21, 40, 30, 25, 39);
                    break;
                }
                case 3: {
                    c.drawCircle(30, 30, 21, p);
                    c.drawOval(new RectF(21, 9, 39, 51), p);
                    c.drawLine(9, 30, 51, 30, p);
                    break;
                }
                case 4: {
                    Path t = new Path();
                    t.moveTo(17, 12); t.lineTo(47, 30); t.lineTo(17, 48); t.close();
                    c.drawPath(t, p);
                    c.drawLine(17, 12, 36, 31, p);
                    break;
                }
                case 5: {
                    c.drawLine(10, 18, 50, 18, p);
                    c.drawLine(10, 30, 50, 30, p);
                    c.drawLine(10, 42, 50, 42, p);
                    knob(c, 22, 18); knob(c, 38, 30); knob(c, 26, 42);
                    break;
                }
                default: {
                    p.setStyle(Paint.Style.FILL);
                    float[] xs = {10, 24.5f, 39};
                    for (float x : xs) for (float y : xs) c.drawRoundRect(new RectF(x, y, x + 11, y + 11), 3.5f, 3.5f, p);
                }
            }
            c.restore();
        }
    }

    View item(final App a) {
        final LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setFocusable(true);
        l.setClickable(true);
        l.setClipChildren(false);
        l.setClipToPadding(false);
        if (Build.VERSION.SDK_INT >= 26) l.setDefaultFocusHighlightEnabled(false);
        final Tile t = new Tile(a);
        l.addView(t, new LinearLayout.LayoutParams(px(132), px(132)));
        final TextView tv = text(a.label, 22, Color.WHITE, false);
        tv.setSingleLine(true);
        tv.setPadding(0, px(22), 0, 0);
        l.addView(tv);
        l.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) {
                v.animate().scaleX(f ? 1.18f : 1f).scaleY(f ? 1.18f : 1f).translationY(f ? -px(10) : 0).setDuration(140).start();
                t.setRing(f);
                tv.setTypeface(Typeface.DEFAULT, f ? Typeface.BOLD : Typeface.NORMAL);
            }
        });
        l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (a.pkg.equals("all")) showAllApps(); else launch(a.pkg);
            }
        });
        return l;
    }

    void buildDock() {
        dock.removeAllViews();
        PackageManager pm = getPackageManager();
        for (App a : DOCK) {
            if (!a.pkg.equals("settings") && !a.pkg.equals("all")) {
                try { pm.getPackageInfo(a.pkg, 0); } catch (Exception e) { continue; }
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (dock.getChildCount() > 0) lp.leftMargin = px(44);
            dock.addView(item(a), lp);
        }
    }

    void launch(String pkg) {
        if (pkg.equals("settings")) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
            pkg = "com.android.tv.settings";
        }
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception ignored) {}
        }
    }

    void showAllApps() {
        final PackageManager pm = getPackageManager();
        LinkedHashMap<String, ResolveInfo> map = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String p = r.activityInfo.packageName;
                if (!p.equals(getPackageName()) && !map.containsKey(p)) map.put(p, r);
            }
        }
        final ArrayList<ResolveInfo> apps = new ArrayList<>(map.values());
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        final Dialog d = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(0xEE0B0F14));
        GridView gv = new GridView(this);
        gv.setNumColumns(6);
        gv.setPadding(px(120), px(100), px(120), px(60));
        gv.setVerticalSpacing(px(20));
        GradientDrawable sel = new GradientDrawable();
        sel.setColor(0x44FFFFFF);
        sel.setCornerRadius(px(28));
        gv.setSelector(sel);
        gv.setAdapter(new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int i) { return apps.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View cv, ViewGroup parent) {
                ResolveInfo r = apps.get(i);
                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setGravity(Gravity.CENTER_HORIZONTAL);
                l.setPadding(px(10), px(16), px(10), px(16));
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageDrawable(r.loadIcon(pm));
                l.addView(iv, new LinearLayout.LayoutParams(px(96), px(96)));
                TextView t = text(String.valueOf(r.loadLabel(pm)), 22, Color.WHITE, false);
                t.setSingleLine(true);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, px(10), 0, 0);
                l.addView(t);
                return l;
            }
        });
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                d.dismiss();
                launch(apps.get(pos).activityInfo.packageName);
            }
        });
        d.setContentView(gv);
        d.show();
    }

    // ---------- обои ----------
    Bitmap nextBitmap() {
        sceneNo++;
        if (custom.length > 0) {
            try {
                String name = custom[(sceneNo - 1) % custom.length];
                curName = name.substring(0, name.lastIndexOf('.'));
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap b = BitmapFactory.decodeStream(getAssets().open("wallpapers/" + name), null, o);
                if (b != null) return b;
            } catch (Exception ignored) {}
        }
        curName = NAMES[(sceneNo - 1) % NAMES.length];
        return scene(sceneNo);
    }

    void updateMeta() {
        caption.setText(curName);
        int n = custom.length > 0 ? Math.min(custom.length, 8) : PAL.length;
        int act = (sceneNo - 1) % n;
        dots.removeAllViews();
        for (int i = 0; i < n; i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(px(5));
            g.setColor(i == act ? 0xFFFFFFFF : 0x80FFFFFF);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(i == act ? 34 : 10), px(10));
            if (i > 0) lp.leftMargin = px(14);
            dots.addView(d, lp);
        }
    }

    void nextWallpaper() {
        final Bitmap nb = nextBitmap();
        updateMeta();
        top.setImageBitmap(nb);
        top.animate().alpha(1f).setDuration(1500).withEndAction(new Runnable() {
            public void run() {
                bottom.setImageBitmap(nb);
                top.setAlpha(0f);
            }
        }).start();
    }

    Bitmap scene(int seed) {
        int w = 960, hh = 540, pi = (seed - 1) % PAL.length;
        int[] p = PAL[pi];
        Random rnd = new Random(seed * 7919L);
        Bitmap b = Bitmap.createBitmap(w, hh, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(b);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setShader(new LinearGradient(0, 0, 0, hh, new int[]{p[0], p[1], p[2], p[3]}, new float[]{0f, 0.45f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, hh, pt);
        pt.setShader(null);
        if (pi == 0 || pi == 3) {
            pt.setColor(Color.WHITE);
            pt.setAlpha(170);
            for (int i = 0; i < 9; i++) c.drawCircle(rnd.nextInt(w), rnd.nextInt(hh / 4), 1.2f, pt);
        }
        float sx = 120 + rnd.nextInt(w - 240), sy = hh * 0.30f + rnd.nextInt(30);
        int sc = p[4] & 0x00FFFFFF;
        pt.setShader(new RadialGradient(sx, sy, 180, sc | 0xA0000000, sc, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, 180, pt);
        pt.setShader(null);
        pt.setColor(p[4]);
        c.drawCircle(sx, sy, 28, pt);
        float[] bases = {0.60f, 0.70f, 0.82f};
        float[] amps = {70, 55, 40};
        for (int i = 0; i < 3; i++) {
            pt.setColor(p[5 + i]);
            float base = hh * bases[i];
            float f1 = 0.008f + rnd.nextFloat() * 0.008f, f2 = 0.022f + rnd.nextFloat() * 0.012f, f3 = 0.06f + rnd.nextFloat() * 0.04f;
            float p1 = rnd.nextFloat() * 6.28f, p2 = rnd.nextFloat() * 6.28f, p3 = rnd.nextFloat() * 6.28f;
            Path path = new Path();
            path.moveTo(0, hh);
            for (int x = 0; x <= w; x += 4) {
                double y = base - amps[i] * (Math.sin(x * f1 + p1) * 0.6 + Math.sin(x * f2 + p2) * 0.3 + Math.sin(x * f3 + p3) * 0.1);
                path.lineTo(x, (float) y);
            }
            path.lineTo(w, hh);
            path.close();
            c.drawPath(path, pt);
            if (i < 2) {
                pt.setShader(new LinearGradient(0, base - 30, 0, base + 100, 0x00FFFFFF, 0x38FFFFFF, Shader.TileMode.CLAMP));
                c.drawRect(0, base - 30, w, base + 100, pt);
                pt.setShader(null);
            }
        }
        pt.setShader(new LinearGradient(0, hh * 0.55f, 0, hh, 0x00081020, 0x99081020, Shader.TileMode.CLAMP));
        c.drawRect(0, hh * 0.55f, w, hh, pt);
        return b;
    }

    // ---------- жизненный цикл ----------
    @Override protected void onResume() {
        super.onResume();
        h.removeCallbacks(rotate);
        h.removeCallbacks(tick);
        h.postDelayed(rotate, ROTATE_MS);
        h.post(tick);
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(rotate);
        h.removeCallbacks(tick);
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
        nextWallpaper();
    }

    @Override public void onBackPressed() { }
}
