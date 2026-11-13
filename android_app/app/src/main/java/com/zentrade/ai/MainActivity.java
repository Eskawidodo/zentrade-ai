package com.zentrade.ai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.ui.agents.AgentsFragment;
import com.zentrade.ai.ui.home.HomeFragment;
import com.zentrade.ai.ui.journal.JournalFragment;
import com.zentrade.ai.ui.nav.BottomNavBar;
import com.zentrade.ai.ui.trading.TradingFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ZentradeAI";
    private static final String KEY_TAB = "selected_tab";

    private BottomNavBar bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            bottomNav = new BottomNavBar(findViewById(R.id.bottom_nav_bar), this::showTab);

            if (savedInstanceState == null) {
                bottomNav.select(BottomNavBar.HOME);
            } else {
                // Fragment dipulihkan otomatis; cukup samakan tampilan nav.
                bottomNav.setSelectedSilently(savedInstanceState.getInt(KEY_TAB, BottomNavBar.HOME));
            }
        } catch (Throwable t) {
            showError(t);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) outState.putInt(KEY_TAB, Math.max(0, bottomNav.getCurrent()));
    }

    private void showTab(int tab) {
        try {
            Fragment fragment;
            switch (tab) {
                case BottomNavBar.HOME:
                    fragment = new HomeFragment();
                    break;
                case BottomNavBar.TRADING:
                    fragment = new TradingFragment();
                    break;
                case BottomNavBar.AGENTS:
                    fragment = new AgentsFragment();
                    break;
                case BottomNavBar.JOURNAL:
                    fragment = new JournalFragment();
                    break;
                default:
                    // Placeholder untuk tab lain
                    fragment = new Fragment();
                    break;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commitNow();
        } catch (Throwable t) {
            showError(t);
        }
    }

    // Tampilkan error di layar (supaya bisa difoto) alih-alih langsung menutup aplikasi.
    private void showError(Throwable t) {
        Log.e(TAG, "Startup error", t);
        StringBuilder sb = new StringBuilder("ERROR (kirim screenshot ini):\n\n");
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getName()).append(": ").append(c.getMessage()).append("\n");
            StackTraceElement[] st = c.getStackTrace();
            for (int i = 0; i < Math.min(st.length, 8); i++) {
                sb.append("   at ").append(st[i]).append("\n");
            }
            sb.append("\nCaused by:\n");
        }
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.BLACK);
        tv.setTextSize(11f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(24, 48, 24, 24);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);
    }
}
