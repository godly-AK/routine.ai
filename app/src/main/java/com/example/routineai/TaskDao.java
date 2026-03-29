package com.example.routineai;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    // --- INSERT ---
    @Insert
    long insertRoutine(DailyRoutine routine);

    @Insert
    long insertSingleTask(Task task);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertCategory(Category category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTaskCategoryMapping(TaskCategoryMapping mapping);

    @Insert
    void insertAlarm(Alarm alarm);

    // --- UPDATE / DELETE (single record) ---
    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    // --- QUERIES ---
    @NonNull
    @Query("SELECT * FROM Tasks ORDER BY scheduled_time ASC")
    List<Task> getAllTasks();



    // Returns Long (boxed) so null is returned when category doesn't exist
    @Query("SELECT category_id FROM Categories WHERE category_name = :name")
    Long getCategoryIdByName(String name);
    // 1. Changes the task status to Completed


    // 2. Turns off the alarm flag so we know it was handled
    @Insert
    void insertRecurrence(TaskRecurrence recurrence);
    // 🎯 COMPLEX QUERY: 100% Normalized dynamic aggregation
    @Query("SELECT c.category_name AS categoryName,\n" +
            "  COUNT(el.log_id) AS totalTasks,\n" +
            "  SUM(CASE WHEN el.status='Completed' THEN 1 ELSE 0 END) AS tasksCompleted,\n" +
            "  SUM(CASE WHEN el.status='Missed'    THEN 1 ELSE 0 END) AS tasksMissed\n" +
            "FROM Categories c\n" +
            "LEFT JOIN Task_Category_Mapping tcm ON c.category_id = tcm.category_id\n" +
            "LEFT JOIN Tasks t ON tcm.task_id = t.id\n" +
            "LEFT JOIN Task_Recurrence tr ON tr.task_id = t.id\n" +  // ← join recurrence
            "LEFT JOIN Execution_Logs el ON el.task_id = t.id\n" +
            "WHERE tr.counts_towards_score = 1 OR tr.task_id IS NULL\n" + // ← filter here
            "GROUP BY c.category_id")
    List<CategoryScoreResult> getNormalizedProductivityScores();
    @Query("SELECT * FROM Tasks WHERE status NOT IN ('Completed', 'Missed') ORDER BY scheduled_time ASC")
    List<Task> getActiveTasksForPrompt();
    @Query("DELETE FROM Task_Category_Mapping WHERE task_id = :taskId")
    void deleteMappingsByTask(int taskId);

    @Query("DELETE FROM Alarms WHERE task_id = :taskId")
    void deleteAlarmByTask(int taskId);

    @Query("DELETE FROM Task_Recurrence WHERE task_id = :taskId")
    void deleteRecurrenceByTask(int taskId);

    @Query("SELECT * FROM Execution_Logs " +
            "WHERE date(executed_at) = date('now', 'localtime')")
    List<ExecutionLog> getLogsForToday();
}