package com.example.routineai;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "Execution_Logs",
        foreignKeys = @ForeignKey(entity = Task.class,
                parentColumns = "id",
                childColumns = "task_id",
                onDelete = ForeignKey.SET_NULL))
public class ExecutionLog {

    @PrimaryKey(autoGenerate = true)
    public int log_id;

    @ColumnInfo(index = true)
    public Integer task_id;

    // Room will map this to the database, and Java will use the exact name
    @ColumnInfo(name = "executed_at")
    public String executed_at;

    public String status;

    // --- CRITICAL: THE EMPTY CONSTRUCTOR ---
    // This fixes the "Expected 3 arguments but found 0" error
    public ExecutionLog() {}

    // You can keep your 3-argument constructor here if you want it!
    public ExecutionLog(int task_id, String executed_at, String status) {
        this.task_id = task_id;
        this.executed_at = executed_at;
        this.status = status;
    }
}