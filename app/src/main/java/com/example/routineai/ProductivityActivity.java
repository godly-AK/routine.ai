package com.example.routineai;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.Executors;

public class ProductivityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productivity);

        // Show title and back arrow in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Productivity Score");
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerViewProductivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch the data on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);

            // Runs the 3NF-compliant Complex Query we built earlier
            List<CategoryScoreResult> scores = db.taskDao().getNormalizedProductivityScores();

            // Push the UI update back to the main thread
            runOnUiThread(() -> {
                ProductivityAdapter adapter = new ProductivityAdapter(scores);
                recyclerView.setAdapter(adapter);
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
}