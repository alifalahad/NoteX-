package com.example.notex;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;

/**
 * ReminderReceiver - BroadcastReceiver for handling reminder alarms
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Reminders";
    private static final String CHANNEL_DESC = "Reminder notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminder_id");
        String title = intent.getStringExtra("reminder_title");
        String description = intent.getStringExtra("reminder_description");
        String type = intent.getStringExtra("reminder_type");

        if (reminderId == null || title == null) {
            return;
        }

        // Mark as notified in database
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        Reminder reminder = dbHelper.getReminderById(reminderId);
        
        if (reminder != null && !reminder.isCompleted()) {
            showNotification(context, reminderId, title, description, type);
            
            // Update notified status
            reminder.setNotified(true);
            dbHelper.updateReminder(reminder);
        }
    }

    private void showNotification(Context context, String reminderId, String title, 
                                  String description, String type) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Create intent to open app when notification is tapped
        Intent appIntent = new Intent(context, UserHomeActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appIntent.putExtra("reminder_id", reminderId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            appIntent,
            flags
        );

        // Create snooze intent
        Intent snoozeIntent = new Intent(context, ReminderActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("reminder_id", reminderId);
        snoozeIntent.putExtra("reminder_title", title);
        snoozeIntent.putExtra("reminder_description", description);
        snoozeIntent.putExtra("reminder_type", type);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_snooze").hashCode(),
            snoozeIntent,
            flags
        );

        // Create mark done intent
        Intent doneIntent = new Intent(context, ReminderActionReceiver.class);
        doneIntent.setAction("MARK_DONE");
        doneIntent.putExtra("reminder_id", reminderId);

        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_done").hashCode(),
            doneIntent,
            flags
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(getTypeEmoji(type) + " " + title)
            .setContentText(description != null && !description.isEmpty() ? description : "Tap to view")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Mark Done", donePendingIntent)
            .setVibrate(new long[]{0, 500, 200, 500});

        // Show notification
        notificationManager.notify(reminderId.hashCode(), builder.build());
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "🔔";
        
        switch (type) {
            case Reminder.TYPE_EVENT:
                return "📅";
            case Reminder.TYPE_TASK:
                return "✓";
            default:
                return "🔔";
        }
    }
}
