package com.example.routineai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;

    // Constructor: We pass the list of tasks from the database into this adapter
    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    // 1. INFLATE: This method takes our item_task.xml file and turns it into a Java View
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    // 2. BIND: This method grabs the exact task for the current row, and puts the text into the TextViews
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = taskList.get(position);

        // 1. Title
        holder.titleText.setText(currentTask.title != null ? currentTask.title : "Untitled Task");


        // 2. Time (Using our new scheduledTime variable)
        String timeStr = (currentTask.scheduledTime != null) ? currentTask.scheduledTime : "--:--";
        holder.timeText.setText("Time: " + timeStr);

        // 3. Priority
        String priorityStr = (currentTask.priority != null) ? currentTask.priority : "None";
        holder.priorityText.setText("Priority: " + priorityStr);

        // 4. Set color based on priority (Optional but looks great!)
        if ("High".equalsIgnoreCase(currentTask.priority)) {
            holder.priorityText.setTextColor(android.graphics.Color.RED);
        } else {
            holder.priorityText.setTextColor(android.graphics.Color.GRAY);
        }
    }

    // 3. COUNT: Tells the RecyclerView exactly how many items we have
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateList(List<Task> newTasks) {
        this.taskList = newTasks;

        // 2. Tell the RecyclerView to refresh the UI on the screen
        notifyDataSetChanged();
    }

    // THE VIEWHOLDER: This class "holds" the UI elements so Android doesn't have to keep searching for them
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, timeText, priorityText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textTaskTitle);
            timeText = itemView.findViewById(R.id.textTaskTime);
            priorityText = itemView.findViewById(R.id.textTaskPriority);
        }
    }
}