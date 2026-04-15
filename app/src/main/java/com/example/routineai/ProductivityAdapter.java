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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_meter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryScoreResult score = scores.get(position);

        // Capitalise category name nicely
        String name = score.categoryName != null
                ? score.categoryName.substring(0, 1).toUpperCase() + score.categoryName.substring(1)
                : "Unknown";
        holder.categoryName.setText(name);

        // Completed / missed in separate TextViews
        holder.taskStats.setText(score.tasksCompleted + " Completed");
        holder.taskMissed.setText(score.tasksMissed + " Missed");

        // Calculate percentage
        int percentage = 0;
        if (score.totalTasks > 0) {
            percentage = (int) (((double) score.tasksCompleted / score.totalTasks) * 100);
        }
        holder.percentageText.setText(percentage + "%");
        holder.progressBar.setProgress(percentage);

        // Colour percentage + bar based on score
        int color;
        int barColor;
        if (percentage >= 75) {
            color    = android.graphics.Color.parseColor("#2E7D32"); // green
            barColor = android.graphics.Color.parseColor("#4CAF50");
        } else if (percentage >= 40) {
            color    = android.graphics.Color.parseColor("#E65100"); // orange
            barColor = android.graphics.Color.parseColor("#FF9800");
        } else {
            color    = android.graphics.Color.parseColor("#B71C1C"); // red
            barColor = android.graphics.Color.parseColor("#EF5350");
        }
        holder.percentageText.setTextColor(color);
        holder.progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(barColor));
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName, percentageText, taskStats, taskMissed;
        ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryName   = itemView.findViewById(R.id.textCategoryName);
            percentageText = itemView.findViewById(R.id.textPercentage);
            taskStats      = itemView.findViewById(R.id.textTaskStats);
            taskMissed     = itemView.findViewById(R.id.textTaskMissed);
            progressBar    = itemView.findViewById(R.id.progressBarCategory);
        }
    }
}