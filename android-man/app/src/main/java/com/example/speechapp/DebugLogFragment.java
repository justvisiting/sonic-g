package com.example.speechapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DebugLogFragment extends Fragment {
    private RecyclerView debugRecyclerView;
    private DebugLogAdapter debugAdapter;
    private List<String> debugLogs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug_log, container, false);
        debugRecyclerView = view.findViewById(R.id.debugRecyclerView);
        debugLogs = new ArrayList<>();
        debugAdapter = new DebugLogAdapter(debugLogs);
        debugRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        debugRecyclerView.setAdapter(debugAdapter);
        return view;
    }

    public void addLog(String log) {
        if (debugLogs != null && debugAdapter != null) {
            debugLogs.add(log);
            debugAdapter.notifyItemInserted(debugLogs.size() - 1);
            debugRecyclerView.scrollToPosition(debugLogs.size() - 1);
        }
    }

    private static class DebugLogAdapter extends RecyclerView.Adapter<DebugLogAdapter.ViewHolder> {
        private final List<String> logs;

        DebugLogAdapter(List<String> logs) {
            this.logs = logs;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.debug_log_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.logText.setText(logs.get(position));
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView logText;

            ViewHolder(View view) {
                super(view);
                logText = view.findViewById(R.id.logText);
            }
        }
    }
}
