package com.zentrade.ai.ui.settings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.R;

import java.util.Locale;

/**
 * Halaman Settings. Saat ini masih statis (data contoh dari desain).
 * Aplikasi hanya dashboard + kontrol: kunci MetaApi, broker, dan LLM disimpan di server, bukan di HP.
 * Token akses aplikasi nanti disimpan terenkripsi (Android Keystore).
 */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Token akses aplikasi: tombol mata untuk tampil / sembunyikan (kunci MetaApi/broker/LLM hanya di server)
        setupSecretToggle(v, R.id.et_vps_token, R.id.et_vps_token_toggle);

        // Protokol: HTTPS / WSS
        final View[] protocol = {v.findViewById(R.id.seg_https), v.findViewById(R.id.seg_wss)};
        for (int i = 0; i < protocol.length; i++) {
            final int idx = i;
            protocol[i].setOnClickListener(x -> selectProtocol(protocol, idx));
        }

        // Slider max daily drawdown: 1.0% .. 10.0% (langkah 0.5)
        SeekBar seek = v.findViewById(R.id.seek_dd);
        final TextView ddValue = v.findViewById(R.id.tv_dd_value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                ddValue.setText(String.format(Locale.US, "%.1f %%", 1.0f + progress * 0.5f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
    }

    private void setupSecretToggle(View root, int editId, int toggleId) {
        final EditText edit = root.findViewById(editId);
        final ImageView toggle = root.findViewById(toggleId);
        if (edit == null || toggle == null) return;
        edit.setTypeface(Typeface.MONOSPACE);
        final boolean[] visible = {false};
        toggle.setOnClickListener(x -> {
            visible[0] = !visible[0];
            edit.setTransformationMethod(visible[0]
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            toggle.setImageResource(visible[0] ? R.drawable.ic_s_visibility_off : R.drawable.ic_s_visibility);
            edit.setSelection(edit.getText().length());
        });
    }

    private void selectProtocol(View[] items, int selected) {
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        int muted = ContextCompat.getColor(requireContext(), R.color.on_surface_variant);
        for (int i = 0; i < items.length; i++) {
            TextView t = (TextView) items[i];
            boolean on = i == selected;
            t.setBackgroundResource(on ? R.drawable.bg_r2_high : R.drawable.bg_r2_lowest);
            t.setTextColor(on ? primary : muted);
            t.setTypeface(t.getTypeface(), on ? Typeface.BOLD : Typeface.NORMAL);
        }
    }
}
