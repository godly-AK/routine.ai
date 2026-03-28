package com.example.routineai;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "Alarms",
        foreignKeys = @ForeignKey(entity = Task.class, parentColumns = "id", childColumns = "task_id", onDelete = ForeignKey.CASCADE))
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    public int alarm_id;

    @ColumnInfo(index = true)
    public int task_id;

    public String trigger_time;
    public boolean is_active;

    public Alarm(int task_id, String trigger_time, boolean is_active) {
        this.task_id = task_id;
        this.trigger_time = trigger_time;
        this.is_active = is_active;
    }
    public Alarm() {}
}