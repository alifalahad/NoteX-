package com.example.notex;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;

import java.util.Calendar;

/**
 * ReminderActionReceiver - Handles snooze and mark done actions from notifications
 */
public class ReminderActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String reminderId = intent.getStringExtra("reminder_id");

        if (reminderId == null || action == null) {
            return;
        }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // Dismiss the notification
            notificationManager.cancel(reminderId.hashCode());
        }

        switch (action) {
            case "SNOOZE":
                handleSnooze(context, intent, dbHelper);
                break;
            case "MARK_DONE":
                handleMarkDone(context, reminderId, dbHelper);
                break;
        }
    }

    private void handleSnooze(Context context, Intent intent, DatabaseHelper dbHelper) {
        String reminderId = intent.getStringExtra("reminder_id");
        String title = intent.getStringExtra("reminder_title");
        String description = intent.getStringExtra("reminder_description");
        String type = intent.getStringExtra("reminder_type");

        Reminder reminder = dbHelper.getReminderById(reminderId);
        
        if (reminder != null) {
            // Snooze for 10 minutes
            Calendar snoozeTime = Calendar.getInstance();
            snoozeTime.add(Calendar.MINUTE, 10);
            
            reminder.setScheduledAt(snoozeTime.getTimeInMillis());
            reminder.setNotified(false);
            dbHelper.updateReminder(reminder);
            
            // Reschedule
            ReminderScheduler.scheduleReminder(context, reminder);
            
            Toast.makeText(context, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMarkDone(Context context, String reminderId, DatabaseHelper dbHelper) {
        dbHelper.markReminderCompleted(reminderId, true);
        Toast.makeText(context, "Reminder marked as done", Toast.LENGTH_SHORT).show();
    }
}
