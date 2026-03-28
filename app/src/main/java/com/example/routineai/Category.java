package com.example.routineai;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Categories", indices = {@Index(value = {"category_name"}, unique = true)})
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int category_id;
    public String category_name;

    public Category(String category_name) {
        this.category_name = category_name;
    }
    public Category() {}
}