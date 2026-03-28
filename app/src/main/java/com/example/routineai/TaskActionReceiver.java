package com.example.routineai;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.Executors;

public class TaskActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int taskId = intent.getIntExtra("TASK_ID", -1);
        String action = intent.getAction();

        if (taskId != -1 && action != null) {

            // 1. Immediately dismiss the notification from the phone screen if it was a click
            if ("ACTION_MARK_DONE".equals(action)) {
                int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null && notificationId != -1) {
                    manager.cancel(notificationId);
                }
                Toast.makeText(context, "Crushed it! Task logged.", Toast.LENGTH_SHORT).show();
            }

            // 2. Jump onto a background thread to update the Database safely
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(context);
                androidx.sqlite.db.SupportSQLiteDatabase sqlDb = db.getOpenHelper().getWritableDatabase();

                try {
                    sqlDb.beginTransaction();

                    // Determine if the user clicked "Done" or swiped it away (Missed)
                    String logStatus = "ACTION_MARK_DONE".equals(action) ? "Completed" : "Missed";



                    // STEP 2: Check if this task exists in the Task_Recurrence table (Is it a daily habit?)
                    android.database.Cursor cursor = sqlDb.query("SELECT * FROM Task_Recurrence WHERE task_id = " + taskId);
                    boolean isRecurring = cursor.moveToFirst();
                    cursor.close();

                    // STEP 3: Apply the split logic
                    if (!isRecurring) {
                        // 🌟 IT IS A ONE-TIME EVENT 🌟
                        // Kill the task status and deactivate the alarm so it never bothers you again.
                        sqlDb.execSQL("UPDATE Tasks SET status = ? WHERE id = ?",
                                new Object[]{logStatus, taskId});
                        sqlDb.execSQL("UPDATE Alarms SET is_active = 0 WHERE task_id = ?",
                                new Object[]{taskId});
                        Log.d("ACTION_RECEIVER", "One-time task logged as " + logStatus + " and killed.");
                    } else {
                        // 🌟 IT IS A DAILY HABIT 🌟
                        // We DO NOT change the Tasks status or Alarms table.
                        // Android AlarmManager will automatically ring it again tomorrow!
                        sqlDb.execSQL("INSERT INTO Execution_Logs (task_id, executed_at, status) VALUES (?, datetime('now','localtime'), ?)",
                                new Object[]{taskId, logStatus});
                        Log.d("ACTION_RECEIVER", "Daily habit logged as " + logStatus + ". Alarm remains active for tomorrow.");
                    }

                    sqlDb.setTransactionSuccessful();

                } catch (Exception e) {
                    Log.e("ACTION_RECEIVER", "Failed to update DB", e);
                } finally {
                    sqlDb.endTransaction();
                }
            });
        }
    }
}