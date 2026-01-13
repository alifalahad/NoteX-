package com.example.notex;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.notex.databinding.DialogCreateReminderBinding;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * CreateReminderDialog - Dialog for creating new reminders/events/tasks
 */
public class CreateReminderDialog {

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final AuthManager authManager;
    private final User currentUser;
    private AlertDialog dialog;
    private DialogCreateReminderBinding binding;
    
    private Calendar selectedDateTime;
    private String selectedType = Reminder.TYPE_REMINDER;
    private String selectedTriggerType = Reminder.TRIGGER_TIME;
    private String selectedRepeatType = Reminder.REPEAT_NONE;
    private int selectedPriority = Reminder.PRIORITY_MEDIUM;
    private OnReminderCreatedListener listener;

    public interface OnReminderCreatedListener {
        void onReminderCreated(Reminder reminder);
    }

    public CreateReminderDialog(Context context, OnReminderCreatedListener listener) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.authManager = AuthManager.getInstance(context);
        this.currentUser = authManager.getCurrentUser();
        this.listener = listener;
        this.selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.HOUR_OF_DAY, 1); // Default: 1 hour from now
    }

    public void show() {
        binding = DialogCreateReminderBinding.inflate(((Activity) context).getLayoutInflater());
        
        setupTypeChips();
        setupTriggerChips();
        setupRepeatChips();
        setupPriorityChips();
        setupDateTimePickers();
        setupQuickPresets();
        setupLocationSettings();
        setupButtons();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(binding.getRoot());
        dialog = builder.create();
        dialog.show();
    }

    private void setupTypeChips() {
        binding.chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.chipReminder.getId()) {
                selectedType = Reminder.TYPE_REMINDER;
            } else if (checkedId == binding.chipEvent.getId()) {
                selectedType = Reminder.TYPE_EVENT;
            } else if (checkedId == binding.chipTask.getId()) {
                selectedType = Reminder.TYPE_TASK;
            }
        });
    }

    private void setupTriggerChips() {
        binding.chipGroupTrigger.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.chipTime.getId()) {
                selectedTriggerType = Reminder.TRIGGER_TIME;
                binding.layoutTimeSettings.setVisibility(View.VISIBLE);
                binding.layoutLocationSettings.setVisibility(View.GONE);
            } else if (checkedId == binding.chipLocation.getId()) {
                selectedTriggerType = Reminder.TRIGGER_LOCATION;
                binding.layoutTimeSettings.setVisibility(View.GONE);
                binding.layoutLocationSettings.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRepeatChips() {
        binding.chipGroupRepeat.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.chipNone.getId()) {
                selectedRepeatType = Reminder.REPEAT_NONE;
            } else if (checkedId == binding.chipDaily.getId()) {
                selectedRepeatType = Reminder.REPEAT_DAILY;
            } else if (checkedId == binding.chipWeekly.getId()) {
                selectedRepeatType = Reminder.REPEAT_WEEKLY;
            } else if (checkedId == binding.chipMonthly.getId()) {
                selectedRepeatType = Reminder.REPEAT_MONTHLY;
            }
        });
    }

    private void setupPriorityChips() {
        binding.chipGroupPriority.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.chipLow.getId()) {
                selectedPriority = Reminder.PRIORITY_LOW;
            } else if (checkedId == binding.chipMedium.getId()) {
                selectedPriority = Reminder.PRIORITY_MEDIUM;
            } else if (checkedId == binding.chipHigh.getId()) {
                selectedPriority = Reminder.PRIORITY_HIGH;
            }
        });
    }

    private void setupDateTimePickers() {
        binding.btnSelectDate.setOnClickListener(v -> showDatePicker());
        binding.btnSelectTime.setOnClickListener(v -> showTimePicker());
        updateDateTimeButtons();
    }

    private void showDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(
            context,
            (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateTimeButtons();
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.show();
    }

    private void showTimePicker() {
        TimePickerDialog picker = new TimePickerDialog(
            context,
            (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateDateTimeButtons();
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        );
        picker.show();
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        binding.btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        binding.btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void setupQuickPresets() {
        binding.chipIn1Hour.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.add(Calendar.HOUR_OF_DAY, 1);
            updateDateTimeButtons();
        });

        binding.chipToday6PM.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.HOUR_OF_DAY, 18);
            selectedDateTime.set(Calendar.MINUTE, 0);
            updateDateTimeButtons();
        });

        binding.chipTomorrowMorning.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.add(Calendar.DAY_OF_MONTH, 1);
            selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
            selectedDateTime.set(Calendar.MINUTE, 0);
            updateDateTimeButtons();
        });
    }

    private void setupLocationSettings() {
        binding.sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvRadiusValue.setText((int)value + " meters");
        });
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        binding.btnSave.setOnClickListener(v -> saveReminder());
    }

    private void saveReminder() {
        String title = binding.etTitle.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = binding.etDescription.getText().toString().trim();
        
        // Create reminder object
        Reminder reminder = new Reminder(
            UUID.randomUUID().toString(),
            currentUser.getId(),
            title,
            selectedType
        );

        reminder.setDescription(description);
        reminder.setTriggerType(selectedTriggerType);
        reminder.setRepeatType(selectedRepeatType);
        reminder.setPriority(selectedPriority);

        if (selectedTriggerType.equals(Reminder.TRIGGER_TIME)) {
            reminder.setScheduledAt(selectedDateTime.getTimeInMillis());
            reminder.setAllDay(binding.checkAllDay.isChecked());
            reminder.setTimezone(Calendar.getInstance().getTimeZone().getID());
        } else {
            // Location-based
            String location = binding.etLocation.getText().toString().trim();
            if (location.isEmpty()) {
                Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show();
                return;
            }
            reminder.setLocation(location);
            reminder.setRadiusMeters((int) binding.sliderRadius.getValue());
            // Note: Actual lat/lng would be set via geocoding or map selection
        }

        // Save to database
        String reminderId = dbHelper.createReminder(reminder);
        
        if (reminderId != null) {
            // Schedule alarm for time-based reminders
            if (selectedTriggerType.equals(Reminder.TRIGGER_TIME)) {
                ReminderScheduler.scheduleReminder(context, reminder);
            }
            
            Toast.makeText(context, "Reminder created successfully", Toast.LENGTH_SHORT).show();
            
            if (listener != null) {
                listener.onReminderCreated(reminder);
            }
            
            dialog.dismiss();
        } else {
            Toast.makeText(context, "Failed to create reminder", Toast.LENGTH_SHORT).show();
        }
    }
}
