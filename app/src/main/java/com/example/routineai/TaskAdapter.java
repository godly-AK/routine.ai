package com.example.routineai;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Set<Integer> completedToday;
    private Set<Integer> missedToday;

    public TaskAdapter(ArrayList<Task> taskList) {
        this.taskList       = taskList;
        this.completedToday = new HashSet<>();
        this.missedToday    = new HashSet<>();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = taskList.get(position);

        holder.titleText.setText(currentTask.title);
        holder.timeText.setText("Time: " + (currentTask.scheduledTime != null ? currentTask.scheduledTime : "--:--"));
        holder.priorityText.setText("Priority: " + (currentTask.priority != null ? currentTask.priority : "None"));
        int priorityColor;
        String p = currentTask.priority != null ? currentTask.priority : "";
        if (Task.PRIORITY_HIGH.equalsIgnoreCase(p)) {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_high);
        } else if (Task.PRIORITY_MEDIUM.equalsIgnoreCase(p)) {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_medium);
        } else if (Task.PRIORITY_LOW.equalsIgnoreCase(p)) {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_low);
        } else {
            priorityColor = Color.parseColor("#555555");
        }

        // ── Determine effective status ────────────────────────────────
        String effectiveStatus;
        boolean isRecurring = "Recurring".equalsIgnoreCase(currentTask.status);

        if (isRecurring) {
            if (completedToday.contains(currentTask.id))   effectiveStatus = "Completed";
            else if (missedToday.contains(currentTask.id)) effectiveStatus = "Missed";
            else                                           effectiveStatus = "Pending";
        } else {
            effectiveStatus = currentTask.status != null ? currentTask.status : "Pending";
        }

        // ── Apply rounded drawable background + text colours ──────────
        switch (effectiveStatus) {
            case "Completed":
                holder.itemView.setBackground(
                        holder.itemView.getContext().getDrawable(R.drawable.bg_task_completed));
                holder.titleText.setTextColor(android.graphics.Color.parseColor("#1B5E20"));
                holder.timeText.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                holder.priorityText.setTextColor(priorityColor);
                break;

            case "Missed":
                holder.itemView.setBackground(
                        holder.itemView.getContext().getDrawable(R.drawable.bg_task_missed));
                holder.titleText.setTextColor(android.graphics.Color.parseColor("#B71C1C"));
                holder.timeText.setTextColor(android.graphics.Color.parseColor("#C62828"));
                holder.priorityText.setTextColor(priorityColor);
                break;

            default:
                holder.itemView.setBackground(
                        holder.itemView.getContext().getDrawable(R.drawable.bg_task_pending));
                holder.titleText.setTextColor(android.graphics.Color.parseColor("#1A1A1A"));
                holder.timeText.setTextColor(android.graphics.Color.parseColor("#555555"));
                holder.priorityText.setTextColor(priorityColor);
                break;
        }
    }



    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateList(List<Task> newTasks, Set<Integer> completedToday, Set<Integer> missedToday) {
        this.taskList       = newTasks;
        this.completedToday = completedToday;
        this.missedToday    = missedToday;
        notifyDataSetChanged();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, timeText, priorityText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText    = itemView.findViewById(R.id.textTaskTitle);
            timeText     = itemView.findViewById(R.id.textTaskTime);
            priorityText = itemView.findViewById(R.id.textTaskPriority);
        }
    }
}