package com.example.routineai;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "Tasks")

public class Task {

    // --- Status Constants ---
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_DONE = "Done";

    // --- Priority Constants ---
    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";

    @PrimaryKey(autoGenerate = true)
    public int id;


    @ColumnInfo(name = "routine_id", index = true)
    public Integer routine_id;

    @NonNull
    public String title = "";

    @SerializedName("scheduled_time")
    @ColumnInfo(name = "scheduled_time")
    public String scheduledTime;

    @ColumnInfo(name = "duration_minutes")
    @SerializedName("duration_minutes")
    public int durationMinutes;

    public String priority;
    public String status;

    @androidx.room.Ignore
    public java.util.List<String> categories;

    // Required by Room
    public Task() {}

    // Manual creation constructor
    public Task(@NonNull String title, String scheduledTime,
                String priority, String status, int durationMinutes) {
        this.title = title;
        this.scheduledTime = scheduledTime;
        this.priority = priority;
        this.status = status;
        this.durationMinutes = durationMinutes;  // ← add this line
    }
}