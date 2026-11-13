package com.zentrade.ai.ui.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.zentrade.ai.R;
import com.zentrade.ai.ui.office.OfficeView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static class Chip {
        LinearLayout root;
        TextView status;
        GradientDrawable bg;
        int color;
    }

    private OfficeView office;
    private HorizontalScrollView chipScroll;
    private LinearLayout chipContainer;
    private final List<Chip> chips = new ArrayList<>();
    private int lastSpeaking = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshChips();
            handler.postDelayed(this, 500);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        office = view.findViewById(R.id.office_view);
        chipScroll = view.findViewById(R.id.chip_scroll);
        chipContainer = view.findViewById(R.id.chip_container);

        if (office != null) {
            // Sementara: tap kantor untuk simulasi "ada setup trading" / "idle".
            // Nanti diganti sinyal asli: office.setTradeSetupActive(true/false)
            office.setOnClickListener(v -> office.setTradeSetupActive(!office.isTradeSetupActive()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(tick);
        chips.clear();
        office = null;
        chipScroll = null;
        chipContainer = null;
        super.onDestroyView();
    }

    // ---------- Chip ----------

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void rebuildChips() {
        chipContainer.removeAllViews();
        chips.clear();
        int n = office.getAgentCount();
        for (int i = 0; i < n; i++) {
            final int index = i;
            OfficeView.AgentInfo info = office.getAgentInfo(i);
            Chip c = new Chip();
            c.color = info.color;

            LinearLayout root = new LinearLayout(requireContext());
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(10), dp(6), dp(12), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            root.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            root.setBackground(bg);

            View dot = new View(requireContext());
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(info.color);
            dot.setBackground(d);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dlp.setMarginEnd(dp(6));
            root.addView(dot, dlp);

            TextView name = new TextView(requireContext());
            name.setText(info.name);
            name.setTextColor(Color.parseColor("#E3E2E6"));
            name.setTextSize(11f);
            name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
            root.addView(name);

            TextView status = new TextView(requireContext());
            status.setTextColor(Color.parseColor("#D0C5AF"));
            status.setTextSize(10f);
            status.setPadding(dp(6), 0, 0, 0);
            root.addView(status);

            root.setOnClickListener(v -> showDetail(index));

            c.root = root;
            c.status = status;
            c.bg = bg;
            chips.add(c);
            chipContainer.addView(root);
        }
    }

    private void refreshChips() {
        if (office == null || chipContainer == null) return;
        int n = office.getAgentCount();
        if (n != chips.size()) rebuildChips();

        int speaking = -1;
        for (int i = 0; i < n && i < chips.size(); i++) {
            OfficeView.AgentInfo info = office.getAgentInfo(i);
            Chip c = chips.get(i);
            c.status.setText(info.status);
            if (info.speaking && speaking < 0) speaking = i;
            applyChipStyle(c, info.speaking);
        }

        // geser otomatis ke agent yang sedang bicara
        if (speaking >= 0 && speaking != lastSpeaking && chipScroll != null) {
            final View target = chips.get(speaking).root;
            chipScroll.post(() -> {
                if (chipScroll == null) return;
                int x = target.getLeft() - (chipScroll.getWidth() - target.getWidth()) / 2;
                chipScroll.smoothScrollTo(Math.max(0, x), 0);
            });
        }
        if (speaking >= 0) lastSpeaking = speaking;
    }

    private void applyChipStyle(Chip c, boolean active) {
        if (active) {
            c.bg.setColor(Color.argb(40, Color.red(c.color), Color.green(c.color), Color.blue(c.color)));
            c.bg.setStroke(dp(1.5f), c.color);
        } else {
            c.bg.setColor(Color.parseColor("#1F2024"));
            c.bg.setStroke(dp(1), Color.parseColor("#33FFFFFF"));
        }
    }

    // ---------- Panel detail ----------

    private void showDetail(int index) {
        if (office == null) return;
        OfficeView.AgentInfo info = office.getAgentInfo(index);
        if (info == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_agent_detail, null);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(info.color);
        v.findViewById(R.id.sheet_dot).setBackground(dot);

        ((TextView) v.findViewById(R.id.sheet_name)).setText(info.name);
        ((TextView) v.findViewById(R.id.sheet_role)).setText("Peran: " + info.role);

        TextView st = v.findViewById(R.id.sheet_status);
        st.setText(info.status.toUpperCase());
        st.setTextColor(info.color);
        GradientDrawable stBg = new GradientDrawable();
        stBg.setCornerRadius(dp(12));
        stBg.setColor(Color.argb(40, Color.red(info.color), Color.green(info.color), Color.blue(info.color)));
        st.setBackground(stBg);

        ((TextView) v.findViewById(R.id.sheet_task)).setText(info.task);
        ((TextView) v.findViewById(R.id.sheet_conf_label)).setText("CONFIDENCE  " + info.confidence + "%");
        ProgressBar bar = v.findViewById(R.id.sheet_conf_bar);
        bar.setProgress(info.confidence);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(info.color));
        ((TextView) v.findViewById(R.id.sheet_decision)).setText(info.lastDecision);

        dialog.setContentView(v);
        dialog.show();
    }
}