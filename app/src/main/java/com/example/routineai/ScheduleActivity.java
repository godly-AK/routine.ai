package com.example.routineai;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TextView emptyStateView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Schedule");
        }

        recyclerView   = findViewById(R.id.recyclerViewSchedule);
        emptyStateView = findViewById(R.id.emptyStateText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Empty adapter while data loads
        adapter = new TaskAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);

            List<Task> allTasks       = db.taskDao().getAllTasks();
            List<ExecutionLog> todayLogs = db.taskDao().getLogsForToday();

            // Split today's logs into two sets by status
            Set<Integer> completedToday = new HashSet<>();
            Set<Integer> missedToday    = new HashSet<>();

            for (ExecutionLog log : todayLogs) {
                if (log.task_id == null) continue;
                if ("Completed".equals(log.status)) completedToday.add(log.task_id);
                else if ("Missed".equals(log.status)) missedToday.add(log.task_id);
            }

            final Set<Integer> finalCompleted = completedToday;
            final Set<Integer> finalMissed    = missedToday;

            runOnUiThread(() -> {
                adapter.updateList(allTasks, finalCompleted, finalMissed);

                if (emptyStateView != null) {
                    emptyStateView.setVisibility(allTasks.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}