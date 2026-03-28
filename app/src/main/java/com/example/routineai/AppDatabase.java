package com.example.routineai;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Tell Room which entities (tables) belong to this database.
// If you update the schema later, you increase the version number.
@Database(entities = {
        DailyRoutine.class,
        Task.class,
        Category.class,
        TaskCategoryMapping.class,
        ExecutionLog.class,
        Alarm.class,
        TaskRecurrence.class

}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Link the DAO we just made
    public abstract TaskDao taskDao();

    // Singleton pattern to prevent multiple instances of database opened at the same time
    private static volatile AppDatabase INSTANCE;

    // 🎯 TRIGGER DEFINITION: Automatically logs completed/missed tasks natively in SQL
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            createTrigger(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            createTrigger(db); // ← recreate every open, covers migrations
        }

        private void createTrigger(@NonNull SupportSQLiteDatabase db) {
            // Drop first so it doesn't fail if it already exists
            db.execSQL("DROP TRIGGER IF EXISTS log_task_execution");
            db.execSQL("CREATE TRIGGER log_task_execution " +
                    "AFTER UPDATE OF status ON Tasks " +
                    "WHEN NEW.status != OLD.status AND NEW.status IN ('Completed', 'Missed') " +
                    "BEGIN " +
                    "  INSERT INTO Execution_Logs (task_id, executed_at, status) " +
                    "  VALUES (NEW.id, datetime('now', 'localtime'), NEW.status); " +
                    "END;");
        }
    };
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "routine_database")
                            .addCallback(roomCallback) // <-- Attach the Trigger here
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}