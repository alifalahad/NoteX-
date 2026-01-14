package com.example.notex.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.example.notex.R;
import com.example.notex.models.Reminder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditReminderDialog {

    private Context context;
    private Reminder reminder;
    private OnReminderUpdatedListener listener;
    private AlertDialog dialog;

    // UI Components
    private TextInputEditText etTitle, etDescription, etLocation;
    private ChipGroup chipGroupType, chipGroupTrigger, chipGroupRepeat, chipGroupPriority;
    private MaterialButton btnDate, btnTime;
    private Slider sliderRadius;
    private CheckBox cbAllDay;
    private View layoutTimeSettings, layoutLocationSettings;

    private Calendar selectedDateTime;

    public interface OnReminderUpdatedListener {
        void onReminderUpdated(Reminder reminder);
    }

    public EditReminderDialog(Context context, Reminder reminder, OnReminderUpdatedListener listener) {
        this.context = context;
        this.reminder = reminder;
        this.listener = listener;
        this.selectedDateTime = Calendar.getInstance();
        this.selectedDateTime.setTimeInMillis(reminder.getScheduledAt());
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_create_reminder, null);
        
        initViews(view);
        loadReminderData();
        setupListeners();

        dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Reminder")
                .setView(view)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Override positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> updateReminder());
    }

    private void initViews(View view) {
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etLocation = view.findViewById(R.id.etLocation);
        chipGroupType = view.findViewById(R.id.chipGroupType);
        chipGroupTrigger = view.findViewById(R.id.chipGroupTrigger);
        chipGroupRepeat = view.findViewById(R.id.chipGroupRepeat);
        chipGroupPriority = view.findViewById(R.id.chipGroupPriority);
        btnDate = view.findViewById(R.id.btnSelectDate);
        btnTime = view.findViewById(R.id.btnSelectTime);
        sliderRadius = view.findViewById(R.id.sliderRadius);
        cbAllDay = view.findViewById(R.id.checkAllDay);
        layoutTimeSettings = view.findViewById(R.id.layoutTimeSettings);
        layoutLocationSettings = view.findViewById(R.id.layoutLocationSettings);
    }

    private void loadReminderData() {
        // Set title and description
        etTitle.setText(reminder.getTitle());
        etDescription.setText(reminder.getDescription());
        
        // Set type
        int typeId = R.id.chipReminder;
        if (reminder.getType() == Reminder.TYPE_EVENT) {
            typeId = R.id.chipEvent;
        } else if (reminder.getType() == Reminder.TYPE_TASK) {
            typeId = R.id.chipTask;
        }
        chipGroupType.check(typeId);
        
        // Set trigger type
        if (reminder.getTriggerType() == Reminder.TRIGGER_TIME) {
            chipGroupTrigger.check(R.id.chipTime);
            layoutTimeSettings.setVisibility(View.VISIBLE);
            layoutLocationSettings.setVisibility(View.GONE);
        } else {
            chipGroupTrigger.check(R.id.chipLocation);
            layoutTimeSettings.setVisibility(View.GONE);
            layoutLocationSettings.setVisibility(View.VISIBLE);
            etLocation.setText(reminder.getLocation());
            sliderRadius.setValue(reminder.getRadiusMeters());
        }
        
        // Set date and time
        updateDateTimeButtons();
        
        // Set repeat
        int repeatId = R.id.chipNone;
        if ("daily".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipDaily;
        } else if ("weekly".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipWeekly;
        } else if ("monthly".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipMonthly;
        }
        chipGroupRepeat.check(repeatId);
        
        // Set priority
        int priorityId = R.id.chipLow;
        if (reminder.getPriority() == Reminder.PRIORITY_MEDIUM) {
            priorityId = R.id.chipMedium;
        } else if (reminder.getPriority() == Reminder.PRIORITY_HIGH) {
            priorityId = R.id.chipHigh;
        }
        chipGroupPriority.check(priorityId);
        
        // Set all-day
        cbAllDay.setChecked(reminder.isAllDay());
    }

    private void setupListeners() {
        // Trigger type listener
        chipGroupTrigger.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTime) {
                layoutTimeSettings.setVisibility(View.VISIBLE);
                layoutLocationSettings.setVisibility(View.GONE);
            } else {
                layoutTimeSettings.setVisibility(View.GONE);
                layoutLocationSettings.setVisibility(View.VISIBLE);
            }
        });

        // Date picker
        btnDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        btnTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
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
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
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
        timePickerDialog.show();
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        btnDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void updateReminder() {
        String title = etTitle.getText().toString().trim();
        
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }

        // Update reminder object
        reminder.setTitle(title);
        reminder.setDescription(etDescription.getText().toString().trim());
        
        // Type
        int selectedTypeId = chipGroupType.getCheckedChipId();
        if (selectedTypeId == R.id.chipEvent) {
            reminder.setType(Reminder.TYPE_EVENT);
        } else if (selectedTypeId == R.id.chipTask) {
            reminder.setType(Reminder.TYPE_TASK);
        } else {
            reminder.setType(Reminder.TYPE_REMINDER);
        }
        
        // Trigger
        int selectedTriggerId = chipGroupTrigger.getCheckedChipId();
        if (selectedTriggerId == R.id.chipTime) {
            reminder.setTriggerType(Reminder.TRIGGER_TIME);
            reminder.setScheduledAt(selectedDateTime.getTimeInMillis());
        } else {
            reminder.setTriggerType(Reminder.TRIGGER_LOCATION);
            reminder.setLocation(etLocation.getText().toString().trim());
            reminder.setRadiusMeters((int) sliderRadius.getValue());
        }
        
        // Repeat
        int selectedRepeatId = chipGroupRepeat.getCheckedChipId();
        if (selectedRepeatId == R.id.chipDaily) {
            reminder.setRepeatType("daily");
        } else if (selectedRepeatId == R.id.chipWeekly) {
            reminder.setRepeatType("weekly");
        } else if (selectedRepeatId == R.id.chipMonthly) {
            reminder.setRepeatType("monthly");
        } else {
            reminder.setRepeatType("none");
        }
        
        // Priority
        int selectedPriorityId = chipGroupPriority.getCheckedChipId();
        if (selectedPriorityId == R.id.chipMedium) {
            reminder.setPriority(Reminder.PRIORITY_MEDIUM);
        } else if (selectedPriorityId == R.id.chipHigh) {
            reminder.setPriority(Reminder.PRIORITY_HIGH);
        } else {
            reminder.setPriority(Reminder.PRIORITY_LOW);
        }
        
        // All day
        reminder.setAllDay(cbAllDay.isChecked());
        
        // Update timestamp
        reminder.setUpdatedAt(System.currentTimeMillis());

        if (listener != null) {
            listener.onReminderUpdated(reminder);
        }

        dialog.dismiss();
    }
}
