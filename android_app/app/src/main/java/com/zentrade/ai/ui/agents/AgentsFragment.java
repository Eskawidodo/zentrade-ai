package com.zentrade.ai.ui.agents;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.R;

import java.util.ArrayList;
import java.util.List;

/** Halaman Agents. Saat ini masih memakai data contoh dari desain. */
public class AgentsFragment extends Fragment {

    private static final String[] MODELS = {"Claude 3.5 Sonnet", "GPT-4o", "Gemini 1.5 Pro"};

    private final List<ObjectAnimator> pulses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agents, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pilihan model Technical Agent (dropdown seperti <select> pada desain)
        final View selector = view.findViewById(R.id.model_tech_container);
        final TextView modelText = view.findViewById(R.id.tv_model_tech);
        if (selector != null && modelText != null) {
            selector.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(requireContext(), selector);
                for (int i = 0; i < MODELS.length; i++) menu.getMenu().add(0, i, i, MODELS[i]);
                menu.setOnMenuItemClickListener(item -> {
                    modelText.setText(MODELS[item.getItemId()]);
                    return true;
                });
                menu.show();
            });
        }

        // Denyut ikon hub dan titik "Current Deployed" (animate-pulse / animate-ping pada desain)
        startPulse(view.findViewById(R.id.icon_hub), 0.4f, 1000);
        startPulse(view.findViewById(R.id.dot_deployed), 0.25f, 900);
    }

    private void startPulse(View v, float minAlpha, long duration) {
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
        super.onDestroyView();
    }
}
