package com.zentrade.ai.ui.nav;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.zentrade.ai.R;

/**
 * Bottom navigation kustom (sesuai desain): ikon + label, tab aktif berwarna emas dengan titik di bawah.
 */
public class BottomNavBar {

    public static final int HOME = 0;
    public static final int TRADING = 1;
    public static final int AGENTS = 2;
    public static final int JOURNAL = 3;
    public static final int SETTINGS = 4;
    private static final int COUNT = 5;

    public interface Listener {
        void onTabSelected(int tab);
    }

    private final View[] items = new View[COUNT];
    private final ImageView[] icons = new ImageView[COUNT];
    private final TextView[] labels = new TextView[COUNT];
    private final View[] dots = new View[COUNT];
    private final ColorStateList activeColor;
    private final ColorStateList inactiveColor;
    private final int activeInt;
    private final int inactiveInt;
    private final Listener listener;
    private int current = -1;

    public BottomNavBar(View root, Listener listener) {
        this.listener = listener;
        activeInt = ContextCompat.getColor(root.getContext(), R.color.primary);
        inactiveInt = ContextCompat.getColor(root.getContext(), R.color.on_surface_variant);
        activeColor = ColorStateList.valueOf(activeInt);
        inactiveColor = ColorStateList.valueOf(inactiveInt);

        items[HOME] = root.findViewById(R.id.nav_home);
        icons[HOME] = root.findViewById(R.id.nav_home_icon);
        labels[HOME] = root.findViewById(R.id.nav_home_label);
        dots[HOME] = root.findViewById(R.id.nav_home_dot);

        items[TRADING] = root.findViewById(R.id.nav_trading);
        icons[TRADING] = root.findViewById(R.id.nav_trading_icon);
        labels[TRADING] = root.findViewById(R.id.nav_trading_label);
        dots[TRADING] = root.findViewById(R.id.nav_trading_dot);

        items[AGENTS] = root.findViewById(R.id.nav_agents);
        icons[AGENTS] = root.findViewById(R.id.nav_agents_icon);
        labels[AGENTS] = root.findViewById(R.id.nav_agents_label);
        dots[AGENTS] = root.findViewById(R.id.nav_agents_dot);

        items[JOURNAL] = root.findViewById(R.id.nav_journal);
        icons[JOURNAL] = root.findViewById(R.id.nav_journal_icon);
        labels[JOURNAL] = root.findViewById(R.id.nav_journal_label);
        dots[JOURNAL] = root.findViewById(R.id.nav_journal_dot);

        items[SETTINGS] = root.findViewById(R.id.nav_settings);
        icons[SETTINGS] = root.findViewById(R.id.nav_settings_icon);
        labels[SETTINGS] = root.findViewById(R.id.nav_settings_label);
        dots[SETTINGS] = root.findViewById(R.id.nav_settings_dot);

        for (int i = 0; i < COUNT; i++) {
            final int tab = i;
            items[i].setOnClickListener(v -> select(tab));
        }
    }

    public int getCurrent() {
        return current;
    }

    /** Pilih tab dan beritahu listener (kalau tab berbeda dari yang aktif). */
    public void select(int tab) {
        if (tab == current) return;
        applyVisual(tab);
        if (listener != null) listener.onTabSelected(tab);
    }

    /** Ubah tampilan saja tanpa memanggil listener (mis. saat layar diputar). */
    public void setSelectedSilently(int tab) {
        applyVisual(tab);
    }

    private void applyVisual(int tab) {
        current = tab;
        for (int i = 0; i < COUNT; i++) {
            boolean on = i == tab;
            icons[i].setImageTintList(on ? activeColor : inactiveColor);
            labels[i].setTextColor(on ? activeInt : inactiveInt);
            dots[i].setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
