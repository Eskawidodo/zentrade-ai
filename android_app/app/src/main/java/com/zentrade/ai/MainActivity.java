package com.zentrade.ai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zentrade.ai.ui.home.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ZentradeAI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
            bottomNav.setOnItemSelectedListener(item -> {
                try {
                    Fragment selectedFragment;
                    if (item.getItemId() == R.id.navigation_home) {
                        selectedFragment = new HomeFragment();
                    } else {
                        // Placeholder untuk tab lain
                        selectedFragment = new Fragment();
                    }
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, selectedFragment)
                            .commitNow();
                } catch (Throwable t) {
                    showError(t);
                }
                return true;
            });

            if (savedInstanceState == null) {
                bottomNav.setSelectedItemId(R.id.navigation_home);
            }
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