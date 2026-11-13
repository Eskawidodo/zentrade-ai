package com.zentrade.ai.ui.journal;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.R;

import android.content.res.ColorStateList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Halaman Journal: Reasoning (log agent), Closed Trades, dan Memory Bank. Data masih contoh dari desain. */
public class JournalFragment extends Fragment {

    // Warna teks pada log (sama dengan palet desain)
    private static final String C_PRIMARY = "#F2CA50";
    private static final String C_PRIMARY_FIXED = "#FFE088";
    private static final String C_TERTIARY = "#58E7AA";
    private static final String C_SECONDARY = "#E9C176";
    private static final String C_ERROR = "#FFB4AB";
    private static final String C_MUTED = "#D0C5AF";

    private static final int TAB_LOGS = 0;
    private static final int TAB_HISTORY = 1;
    private static final int TAB_MEMORY = 2;

    private View[] tabs = new View[3];
    private ImageView[] tabIcons = new ImageView[3];
    private TextView[] tabLabels = new TextView[3];
    private View[] views = new View[3];
    private LinearLayout feed;
    private final List<ObjectAnimator> pulses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tabs[TAB_LOGS] = v.findViewById(R.id.tab_logs);
        tabs[TAB_HISTORY] = v.findViewById(R.id.tab_history);
        tabs[TAB_MEMORY] = v.findViewById(R.id.tab_memory);
        tabIcons[TAB_LOGS] = v.findViewById(R.id.tab_logs_icon);
        tabIcons[TAB_HISTORY] = v.findViewById(R.id.tab_history_icon);
        tabIcons[TAB_MEMORY] = v.findViewById(R.id.tab_memory_icon);
        tabLabels[TAB_LOGS] = v.findViewById(R.id.tab_logs_label);
        tabLabels[TAB_HISTORY] = v.findViewById(R.id.tab_history_label);
        tabLabels[TAB_MEMORY] = v.findViewById(R.id.tab_memory_label);
        views[TAB_LOGS] = v.findViewById(R.id.view_logs);
        views[TAB_HISTORY] = v.findViewById(R.id.view_history);
        views[TAB_MEMORY] = v.findViewById(R.id.view_memory);
        feed = v.findViewById(R.id.terminal_feed);

        for (int i = 0; i < 3; i++) {
            final int tab = i;
            tabs[i].setOnClickListener(x -> selectTab(tab));
        }
        selectTab(TAB_LOGS);

        fillLogs();
        v.findViewById(R.id.btn_wipe).setOnClickListener(x -> wipeLogs());

        // Teks pelajaran (Memory Bank) dengan sorotan warna
        ((TextView) v.findViewById(R.id.tv_lesson_1)).setText(html(
                "False breakout detected on Friday NY Close during low liquidity. Adjusting News Agent buffer to avoid entries past "
                        + hl(C_PRIMARY, "20:00 UTC", false) + "."));
        ((TextView) v.findViewById(R.id.tv_lesson_2)).setText(html(
                "EURUSD spread widened to 3.2 pips during European session open. Added spread cap filter ("
                        + hl(C_PRIMARY, "&lt; 1.8 pips", false) + ") before order dispatch."));

        // Denyut (animate-ping / animate-pulse pada desain)
        pulse(v.findViewById(R.id.dot_stream), 0.25f, 900);
        pulse(v.findViewById(R.id.dot_live), 0.3f, 1000);
        pulse(v.findViewById(R.id.tv_awaiting), 0.4f, 1000);
    }

    // ---------- Tab ----------

    private void selectTab(int selected) {
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        int muted = ContextCompat.getColor(requireContext(), R.color.on_surface_variant);
        for (int i = 0; i < 3; i++) {
            boolean on = i == selected;
            views[i].setVisibility(on ? View.VISIBLE : View.GONE);
            tabs[i].setBackgroundResource(on ? R.drawable.bg_r4_surface_high : 0);
            ImageViewCompat.setImageTintList(tabIcons[i], ColorStateList.valueOf(on ? primary : muted));
            tabLabels[i].setTextColor(on ? primary : muted);
        }
    }

    // ---------- Log terminal ----------

    private void fillLogs() {
        feed.removeAllViews();
        addLog("14:32:01", "TECHNICAL_AGENT", C_PRIMARY, false, "SCAN", C_TERTIARY, false,
                "Scanning " + hl(C_PRIMARY, "EURUSD H1", false) + ". Price " + hl(C_PRIMARY_FIXED, "1.08250", false)
                        + " entered major Support Zone " + hl(C_TERTIARY, "[1.08230 - 1.08270]", false)
                        + ". Shifting granularity to M5 frame.", false);
        addLog("14:34:45", "TECHNICAL_AGENT", C_PRIMARY, false, "PATTERN_CONFIRMED", C_TERTIARY, false,
                "M5 Candle closed as " + hl(C_SECONDARY, "Bullish Engulfing", false) + " at " + hl(C_PRIMARY_FIXED, "1.08290", false)
                        + ". RSI(14) = " + hl(C_TERTIARY, "38.2", false) + ". Signal: " + hl(C_TERTIARY, "VALID BUY SETUP", true) + ".", false);
        addLog("14:34:50", "NEWS_AGENT", C_SECONDARY, false, "CALENDAR_AUDIT", C_TERTIARY, false,
                "Economic Calendar verified: No High-Impact USD/EUR events until " + hl(C_MUTED, "US Initial Jobless Claims", false)
                        + " in 3h 15m. Sentiment: " + hl(C_TERTIARY, "GREEN PASS", true) + ".", false);
        addLog("14:35:02", "RISK_AGENT", "#E9C349", false, "CAPITAL_CALCULATOR", C_PRIMARY, false,
                "Equity: " + hl(C_PRIMARY, "$48,250", false) + ". Max risk: 1.0% ($482.50). SL set at " + hl(C_ERROR, "1.08050", false)
                        + " (24 pips). Lot size computed: " + hl(C_PRIMARY, "0.85 lots", true) + ". TP set at "
                        + hl(C_TERTIARY, "1.08690", false) + " (40 pips, R:R 1:2.0).", false);
        addLog("14:35:10", "SUPERVISOR", null, true, "DISPATCH", C_TERTIARY, true,
                "Consensus verified (" + hl(C_TERTIARY, "3/3 approvals", true) + "). Sending " + hl(C_PRIMARY, "POST /api/v1/order", false)
                        + " to VPS MetaTrader 5 Bridge... Order " + hl(C_PRIMARY, "#8921 Filled @ 1.08290", true) + ".", true);
    }

    private void wipeLogs() {
        feed.removeAllViews();
        String now = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        addLog(now, "SYSTEM", null, true, "BUFFER_RESET", C_TERTIARY, false,
                "Telemetry stream buffer wiped by operator. Standing by for incoming packets.", false);
    }

    /**
     * @param tagColor  warna teks tag agent; null + tagFilled=true berarti tag emas penuh (SUPERVISOR/SYSTEM)
     * @param highlight true untuk item terakhir (latar lebih terang)
     */
    private void addLog(String time, String tag, @Nullable String tagColor, boolean tagFilled,
                        String action, String actionColor, boolean actionPulse, String htmlMsg, boolean highlight) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(8), dp(8), dp(8), dp(8));
        item.setBackgroundResource(highlight ? R.drawable.bg_r2_high : R.drawable.bg_r2_low);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (feed.getChildCount() > 0) ilp.topMargin = dp(10);
        item.setLayoutParams(ilp);

        LinearLayout head = new LinearLayout(requireContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = numSm("[" + time + "]", C_MUTED);
        head.addView(t);

        TextView tg = numSm(tag, tagFilled ? "#3C2F00" : tagColor);
        tg.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tg.setPadding(dp(6), dp(1), dp(6), dp(1));
        tg.setBackgroundResource(tagFilled ? R.drawable.bg_r2_primary : R.drawable.bg_r2_highest);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMarginStart(dp(8));
        head.addView(tg, tlp);

        TextView ac = numSm(action, actionColor);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.setMarginStart(dp(8));
        head.addView(ac, alp);
        if (actionPulse) pulse(ac, 0.4f, 1000);

        item.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView msg = numSm("", "#E3E2E6");
        msg.setText(html(htmlMsg));
        msg.setLineSpacing(dp(4), 1f);
        msg.setPadding(dp(4), 0, 0, 0);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = dp(2);
        item.addView(msg, mlp);

        feed.addView(item);
    }

    // ---------- Util ----------

    private TextView numSm(String text, String color) {
        TextView tv = new TextView(requireContext());
        TextViewCompat.setTextAppearance(tv, R.style.Zt_NumSm);
        tv.setIncludeFontPadding(false);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        return tv;
    }

    private static String hl(String color, String text, boolean bold) {
        String s = "<font color='" + color + "'><tt>" + text + "</tt></font>";
        return bold ? "<b>" + s + "</b>" : s;
    }

    private static CharSequence html(String s) {
        return Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void pulse(View v, float minAlpha, long duration) {
        if (v == null) return;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "alpha", 1f, minAlpha);
        a.setDuration(duration);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.start();
        pulses.add(a);
    }

    @Override
    public void onDestroyView() {
        for (ObjectAnimator a : pulses) a.cancel();
        pulses.clear();
        feed = null;
        super.onDestroyView();
    }
}
