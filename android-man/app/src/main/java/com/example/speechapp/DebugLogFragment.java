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
    private TextView logTextView;
    private ScrollView scrollView;
    private StringBuilder logBuilder = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);
        logTextView = view.findViewById(R.id.logTextView);
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

    public void appendLog(String text) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            logBuilder.append(text).append("\n");
            if (logTextView != null) {
                logTextView.setText(logBuilder.toString());
                // Scroll to bottom
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    public void clearLog() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            logBuilder.setLength(0);
            if (logTextView != null) {
                logTextView.setText("");
            }
        });
    }

    private void updateLogDisplay() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            if (logTextView != null) {
                logTextView.setText(logBuilder.toString());
                // Scroll to bottom
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }
}
