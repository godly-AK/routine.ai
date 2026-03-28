package com.example.routineai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductivityAdapter extends RecyclerView.Adapter<ProductivityAdapter.ViewHolder> {

    private List<CategoryScoreResult> scores;

    public ProductivityAdapter(List<CategoryScoreResult> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_meter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryScoreResult score = scores.get(position);

        holder.categoryName.setText(score.categoryName);
        holder.taskStats.setText(score.tasksCompleted + " Completed • " + score.tasksMissed + " Missed");

        // Calculate the percentage dynamically so we stay 100% normalized!
        int percentage = 0;
        if (score.totalTasks > 0) {
            percentage = (int) (((double) score.tasksCompleted / score.totalTasks) * 100);
        }

        holder.percentageText.setText(percentage + "%");
        holder.progressBar.setProgress(percentage);
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName, percentageText, taskStats;
        ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.textCategoryName);
            percentageText = itemView.findViewById(R.id.textPercentage);
            taskStats = itemView.findViewById(R.id.textTaskStats);
            progressBar = itemView.findViewById(R.id.progressBarCategory);
        }
    }
}