package com.example.routineai;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TextView emptyStateView;

    // Shared executor — not recreated on every call
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Enable back button in the top action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Schedule");
        }

        recyclerView = findViewById(R.id.recyclerViewSchedule);
        emptyStateView = findViewById(R.id.emptyStateText); // Add this TextView in your XML

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set empty adapter first so RecyclerView isn't naked while data loads
        adapter = new TaskAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time user comes back to this screen
        // so newly added tasks are always visible
        loadTasks();
    }

    private void loadTasks() {
        executor.execute(() -> {
            List<Task> allTasks = AppDatabase.getDatabase(this).taskDao().getAllTasks();

            runOnUiThread(() -> {
                adapter.updateList(allTasks);

                // Show empty state message if no tasks exist
                if (emptyStateView != null) {
                    emptyStateView.setVisibility(allTasks.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    // Handles the back arrow in the action bar
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
        // Clean up the executor when the activity is destroyed
        executor.shutdown();
    }
}