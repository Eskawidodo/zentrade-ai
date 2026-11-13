package com.zentrade.ai.ui.trading;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.R;

import java.util.ArrayList;
import java.util.List;

/** Halaman Trading. Saat ini masih memakai data contoh dari desain. */
public class TradingFragment extends Fragment {

    private final List<ObjectAnimator> pulses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Log agent: nama agent berwarna emas
        TextView log = view.findViewById(R.id.tv_agent_log);
        if (log != null) {
            String label = "Technical & Supervisor Agent:";
            String rest = " Active trailing stop locked at breakeven +3 pips.";
            SpannableStringBuilder sb = new SpannableStringBuilder(label + rest);
            sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.primary)),
                    0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            log.setText(sb);
        }

        // Titik berdenyut (animate-ping pada desain)
        startPulse(view.findViewById(R.id.dot_tab_active));
    }

    private void startPulse(View v) {
        if (v == null) return;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.25f);
        a.setDuration(900);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.start();
        pulses.add(a);
    }

    @Override
    public void onDestroyView() {
        for (ObjectAnimator a : pulses) a.cancel();
        pulses.clear();
        super.onDestroyView();
    }
}
