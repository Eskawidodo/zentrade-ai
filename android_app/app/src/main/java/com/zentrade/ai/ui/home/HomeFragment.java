package com.zentrade.ai.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zentrade.ai.R;
import com.zentrade.ai.ui.office.OfficeView;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final OfficeView office = view.findViewById(R.id.office_view);
        if (office != null) {
            // Sementara: tap kantor untuk simulasi "ada setup trading" / "idle".
            // Nanti diganti dengan sinyal asli dari backend: office.setTradeSetupActive(true/false)
            office.setOnClickListener(v -> office.setTradeSetupActive(!office.isTradeSetupActive()));
        }
    }
}