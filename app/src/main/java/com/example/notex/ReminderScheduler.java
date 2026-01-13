package com.example.notex;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;

import java.util.Calendar;

/**
 * ReminderScheduler - Handles scheduling reminders using AlarmManager
 */
public class ReminderScheduler {

    /**
     * Schedule a reminder using AlarmManager or Geofencing
     */
    public static void scheduleReminder(Context context, Reminder reminder) {
        // Check if location-based reminder
        if (Reminder.TRIGGER_LOCATION.equals(reminder.getTriggerType())) {
            // Use geofencing for location-based reminders
            GeofencingManager geofencingManager = new GeofencingManager(context);
            geofencingManager.addGeofence(reminder);
            return;
        }
        
        // Time-based reminder - use AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminder_id", reminder.getId());
        intent.putExtra("reminder_title", reminder.getTitle());
        intent.putExtra("reminder_description", reminder.getDescription());
        intent.putExtra("reminder_type", reminder.getType());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.getId().hashCode(),
            intent,
            flags
        );

        long triggerTime = reminder.getScheduledAt();

        // Use exact alarm for precise timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        }

        // Handle recurring reminders
        if (reminder.isRecurring()) {
            scheduleRecurringReminder(context, reminder, alarmManager, pendingIntent);
        }
    }

    /**
     * Schedule recurring reminder
     */
    private static void scheduleRecurringReminder(Context context, Reminder reminder, 
                                                  AlarmManager alarmManager, PendingIntent pendingIntent) {
        long intervalMillis = getRepeatInterval(reminder.getRepeatType());
        
        if (intervalMillis > 0) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                reminder.getScheduledAt(),
                intervalMillis,
                pendingIntent
            );
        }
    }

    /**
     * Get repeat interval in milliseconds
     */
    private static long getRepeatInterval(String repeatType) {
        switch (repeatType) {
            case Reminder.REPEAT_DAILY:
                return AlarmManager.INTERVAL_DAY;
            case Reminder.REPEAT_WEEKLY:
                return AlarmManager.INTERVAL_DAY * 7;
            case Reminder.REPEAT_MONTHLY:
                return AlarmManager.INTERVAL_DAY * 30; // Approximate
            default:
                return 0;
        }
    }

    /**
     * Cancel a scheduled reminder
     */
    public static void cancelReminder(Context context, String reminderId) {
        // Cancel both alarm and geofence
        
        // Cancel alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (alarmManager != null) {
            Intent intent = new Intent(context, ReminderReceiver.class);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode(),
                intent,
                flags
            );

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        
        // Cancel geofence
        GeofencingManager geofencingManager = new GeofencingManager(context);
        geofencingManager.removeGeofence(reminderId);
    }

    /**
     * Reschedule all reminders (after device reboot)
     */
    public static void rescheduleAllReminders(Context context, String userId) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        java.util.List<Reminder> upcomingReminders = dbHelper.getUpcomingReminders(userId);
        
        for (Reminder reminder : upcomingReminders) {
            if (!reminder.isCompleted()) {
                if (Reminder.TRIGGER_LOCATION.equals(reminder.getTriggerType())) {
                    // Reschedule location-based reminder
                    GeofencingManager geofencingManager = new GeofencingManager(context);
                    geofencingManager.addGeofence(reminder);
                } else if (reminder.isTimeBased()) {
                    // Reschedule time-based reminder
                    scheduleReminder(context, reminder);
                }
            }
        }
    }
}
