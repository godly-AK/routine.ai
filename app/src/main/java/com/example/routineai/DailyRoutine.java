package com.example.routineai;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Daily_Routines")
public class DailyRoutine {
    @PrimaryKey(autoGenerate = true)
    public int routine_id;
    public String raw_prompt;
    public String date_logged;

    public DailyRoutine(String raw_prompt, String date_logged) {
        this.raw_prompt = raw_prompt;
        this.date_logged = date_logged;
    }
    public DailyRoutine() {}
}