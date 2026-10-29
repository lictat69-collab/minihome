package com.example.minihome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    // Любимые приложения в доке (в этом порядке). Отсутствующие пропускаются.
    static final String[] DOCK = {
        "org.smarttube.stable", "ru.vk.store.tv", "com.google.android.youtube.tv",
        "com.yandex.browser.tv", "com.android.vending", "com.android.tv.settings"
    };
    static final int ROTATE_MS = 45000;

    final Handler h = new Handler();
    ImageView bottom, top;
    LinearLayout dock;
    int sceneNo = 0;
    String[] custom = new String[0];

    final Runnable rotate = new Runnable() {
        public void run() { nextWallpaper(); h.postDelayed(this, ROTATE_MS); }
    };

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        bottom = new ImageView(this);
        top = new ImageView(this);
        bottom.setScaleType(ImageView.ScaleType.CENTER_CROP);
        top.setScaleType(ImageView.ScaleType.CENTER_CROP);
        top.setAlpha(0f);
        root.addView(bottom, new FrameLayout.LayoutParams(-1, -1));
        root.addView(top, new FrameLayout.LayoutParams(-1, -1));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(34);
        clock.setShadowLayer(8, 0, 2, 0xAA000000);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(28), dp(48), 0);
        root.addView(clock, cp);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable dbg = new GradientDrawable();
        dbg.setColor(0x66101418);
        dbg.setCornerRadius(dp(30));
        dock.setBackground(dbg);
        FrameLayout.LayoutParams dp_ = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp_.setMargins(0, 0, 0, dp(40));
        root.addView(dock, dp_);

        buildDock();
        setContentView(root);
        showFirst();
    }

    void buildDock() {
        dock.removeAllViews();
        PackageManager pm = getPackageManager();
        for (final String p : DOCK) {
            try {
                pm.getPackageInfo(p, 0);
                CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(p, 0));
                Drawable icon = pm.getApplicationIcon(p);
                dock.addView(dockItem(label.toString(), icon, new Runnable() {
                    public void run() { launch(p); }
                }));
            } catch (Exception ignored) {}
        }
        Drawable g = new ColorDrawable(0x00000000);
        dock.addView(dockItem("Все", gridIcon(), new Runnable() {
            public void run() { showAllApps(); }
        }));
    }

    Drawable gridIcon() {
        Bitmap bm = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(0xFFFFFFFF);
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++)
            c.drawRoundRect(new RectF(12 + i * 40, 12 + j * 40, 44 + i * 40, 44 + j * 40), 8, 8, pt);
        return new android.graphics.drawable.BitmapDrawable(getResources(), bm);
    }

    GradientDrawable focusBg(boolean f) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(20));
        g.setColor(f ? 0x55FFFFFF : 0x00000000);
        return g;
    }

    View dockItem(String label, Drawable icon, final Runnable action) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(dp(16), dp(12), dp(16), dp(8));
        l.setFocusable(true);
        l.setClickable(true);
        l.setBackground(focusBg(false));
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        l.addView(iv, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setSingleLine(true);
        t.setMaxWidth(dp(100));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(4), 0, 0);
        l.addView(t);
        l.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) {
                v.animate().scaleX(f ? 1.2f : 1f).scaleY(f ? 1.2f : 1f).setDuration(120).start();
                v.setBackground(focusBg(f));
            }
        });
        l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        return l;
    }

    void launch(String pkg) {
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
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String c : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(c);
            for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
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
        gv.setPadding(dp(60), dp(50), dp(60), dp(30));
        gv.setVerticalSpacing(dp(10));
        gv.setSelector(focusBg(true));
        gv.setAdapter(new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int i) { return apps.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View cv, ViewGroup parent) {
                ResolveInfo r = apps.get(i);
                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setGravity(Gravity.CENTER_HORIZONTAL);
                l.setPadding(dp(8), dp(10), dp(8), dp(10));
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageDrawable(r.loadIcon(pm));
                l.addView(iv, new LinearLayout.LayoutParams(dp(56), dp(56)));
                TextView t = new TextView(MainActivity.this);
                t.setText(r.loadLabel(pm));
                t.setTextColor(Color.WHITE);
                t.setTextSize(13);
                t.setSingleLine(true);
                t.setGravity(Gravity.CENTER);
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
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeStream(getAssets().open("wallpapers/" + name), null, o);
            } catch (Exception ignored) {}
        }
        return scene(sceneNo);
    }

    void showFirst() {
        bottom.setImageBitmap(nextBitmap());
    }

    void nextWallpaper() {
        final Bitmap nb = nextBitmap();
        if (nb == null) return;
        top.setImageBitmap(nb);
        top.animate().alpha(1f).setDuration(1500).withEndAction(new Runnable() {
            public void run() {
                bottom.setImageBitmap(nb);
                top.setAlpha(0f);
            }
        }).start();
    }

    static final int[][] PAL = {
        {0xFF2B3A67, 0xFFF4A261, 0xFFFFE8B0, 0xFF6D6A9C, 0xFF40507A, 0xFF1F2A44}, // рассвет
        {0xFF4A90C2, 0xFFBFE3F0, 0xFFFFFFFF, 0xFF7FA6A0, 0xFF4F7D6B, 0xFF2A4D3E}, // лес днём
        {0xFF3D2C5E, 0xFFE76F51, 0xFFFFD58A, 0xFF8A4F7D, 0xFF5A3A6B, 0xFF2B1F3D}, // закат
        {0xFF0B132B, 0xFF3A506B, 0xFFE8EEF4, 0xFF2F4565, 0xFF1C2B45, 0xFF0E1A2D}, // ночь
        {0xFF8FB8C9, 0xFFE9F1F2, 0xFFFFF4D6, 0xFFA9C3C8, 0xFF7C9EA0, 0xFF4E6E6C}  // туман
    };

    Bitmap scene(int seed) {
        int w = 960, hgt = 540;
        int[] p = PAL[(seed - 1) % PAL.length];
        Random rnd = new Random(seed * 7919L);
        Bitmap b = Bitmap.createBitmap(w, hgt, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(b);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setShader(new LinearGradient(0, 0, 0, hgt * 0.8f, p[0], p[1], Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, hgt, pt);
        pt.setShader(null);
        pt.setColor(p[2]);
        pt.setAlpha(235);
        c.drawCircle(120 + rnd.nextInt(w - 240), hgt * 0.30f + rnd.nextInt(40), 38, pt);
        pt.setAlpha(255);
        for (int i = 0; i < 3; i++) {
            pt.setColor(p[3 + i]);
            float base = hgt * (0.56f + 0.13f * i);
            float amp = 70 - i * 12;
            float f1 = 0.004f + rnd.nextFloat() * 0.004f, f2 = 0.011f + rnd.nextFloat() * 0.006f, f3 = 0.03f + rnd.nextFloat() * 0.02f;
            float p1 = rnd.nextFloat() * 6.28f, p2 = rnd.nextFloat() * 6.28f, p3 = rnd.nextFloat() * 6.28f;
            Path path = new Path();
            path.moveTo(0, hgt);
            for (int x = 0; x <= w; x += 6) {
                double y = base - amp * (Math.sin(x * f1 + p1) * 0.6 + Math.sin(x * f2 + p2) * 0.3 + Math.sin(x * f3 + p3) * 0.1);
                path.lineTo(x, (float) y);
            }
            path.lineTo(w, hgt);
            path.close();
            c.drawPath(path, pt);
        }
        // лёгкое затемнение снизу, чтобы док читался
        pt.setShader(new LinearGradient(0, hgt * 0.65f, 0, hgt, 0x00000000, 0x99000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, hgt, pt);
        return b;
    }

    // ---------- жизненный цикл ----------
    @Override protected void onResume() {
        super.onResume();
        h.removeCallbacks(rotate);
        h.postDelayed(rotate, ROTATE_MS);
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(rotate);
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i); // кнопка Home, когда лаунчер уже открыт
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
        nextWallpaper();
    }

    @Override public void onBackPressed() { /* на домашнем экране Назад ничего не делает */ }
}
