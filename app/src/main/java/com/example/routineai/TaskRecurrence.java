package com.example.routineai;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "Task_Recurrence",
        foreignKeys = @ForeignKey(entity = Task.class,
                parentColumns = "id",
                childColumns = "task_id",
                onDelete = ForeignKey.CASCADE))
public class TaskRecurrence {

    @PrimaryKey(autoGenerate = true)
    public int recurrence_id;

    @ColumnInfo(index = true)
    public int task_id;

    public String frequency; // e.g., "Daily", "Weekly"

    // This perfectly solves your productivity score problem!
    public boolean counts_towards_score;

    public TaskRecurrence(int task_id, String frequency, boolean counts_towards_score) {
        this.task_id = task_id;
        this.frequency = frequency;
        this.counts_towards_score = counts_towards_score;
    }

    public TaskRecurrence() {}
}