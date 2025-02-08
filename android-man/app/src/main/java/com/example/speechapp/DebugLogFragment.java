package com.example.speechapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DebugLogFragment extends Fragment {
    private TextView debugLogView;
    private ScrollView scrollView;
    private StringBuilder logBuilder = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);
        debugLogView = view.findViewById(R.id.debugLogView);
        scrollView = view.findViewById(R.id.scrollView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateLogDisplay();
    }

    public void addLog(String log) {
        if (log == null) return;
        
        logBuilder.append(log).append("\n\n");
        updateLogDisplay();
    }

    public void clearLog() {
        logBuilder.setLength(0);
        updateLogDisplay();
    }

    private void updateLogDisplay() {
        if (debugLogView != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                debugLogView.setText(logBuilder.toString());
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        }
    }
}
