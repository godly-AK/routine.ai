package com.example.routineai;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "Task_Category_Mapping",
        primaryKeys = {"task_id", "category_id"},
        foreignKeys = {
                @ForeignKey(entity = Task.class, parentColumns = "id", childColumns = "task_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "category_id", childColumns = "category_id", onDelete = ForeignKey.CASCADE)
        })
public class TaskCategoryMapping {
    @ColumnInfo(index = true)
    public int task_id;

    @ColumnInfo(index = true)
    public int category_id;

    public TaskCategoryMapping(int task_id, int category_id) {
        this.task_id = task_id;
        this.category_id = category_id;
    }
    public TaskCategoryMapping() {}
}