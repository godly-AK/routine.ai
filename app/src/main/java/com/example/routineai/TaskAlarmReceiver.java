package com.example.routineai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class TaskAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "routine_tasks_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle    = intent.getStringExtra("TASK_TITLE");
        String taskPriority = intent.getStringExtra("TASK_PRIORITY");
        int    taskId       = intent.getIntExtra("TASK_ID", -1);
        boolean isRecurring = intent.getBooleanExtra("IS_RECURRING", false);

        if (taskTitle == null) taskTitle = "Scheduled Task";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Routine Tasks", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for scheduled tasks");
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int notificationId = (int) System.currentTimeMillis();

        Intent doneIntent = new Intent(context, TaskActionReceiver.class);
        doneIntent.setAction("ACTION_MARK_DONE");
        doneIntent.putExtra("TASK_ID", taskId);
        doneIntent.putExtra("NOTIFICATION_ID", notificationId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, notificationId, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismissIntent = new Intent(context, TaskActionReceiver.class);
        dismissIntent.setAction("ACTION_DISMISSED");
        dismissIntent.putExtra("TASK_ID", taskId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context, notificationId + 1, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time for: " + taskTitle)
                .setContentText("Priority: " + taskPriority + " - Get to work!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDeleteIntent(dismissPendingIntent)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_edit, "Mark Done", donePendingIntent);

        android.app.Notification notification = builder.build();
        notification.flags |= android.app.Notification.FLAG_INSISTENT;
        notificationManager.notify(notificationId, notification);

        // ── Reschedule for tomorrow if this is a daily recurring task ──
        if (isRecurring && taskId != -1) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.add(java.util.Calendar.DATE, 1); // same time, next day

            android.app.AlarmManager alarmManager =
                    (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            PendingIntent recurringIntent = PendingIntent.getBroadcast(
                    context, taskId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(), recurringIntent);
                    }
                } else {
                    alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(), recurringIntent);
                }
            }
        }
    }
}