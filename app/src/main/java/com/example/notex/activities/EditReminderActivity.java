package com.example.notex.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.notex.R;
import com.example.notex.ReminderScheduler;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditReminderActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etLocation;
    private ChipGroup chipGroupType, chipGroupTrigger, chipGroupRepeat, chipGroupPriority;
    private MaterialButton btnDate, btnTime;
    private Slider sliderRadius;
    private CheckBox cbAllDay;
    private View layoutTimeSettings, layoutLocationSettings;
    private ExtendedFloatingActionButton fabSave;

    private DatabaseHelper dbHelper;
    private Reminder reminder;
    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reminder);

        // Get reminder ID from intent
        String reminderId = getIntent().getStringExtra("reminder_id");
        if (reminderId == null) {
            Toast.makeText(this, "Error: Reminder not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = DatabaseHelper.getInstance(this);
        reminder = dbHelper.getReminderById(reminderId);
        
        if (reminder == null) {
            Toast.makeText(this, "Error: Reminder not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(reminder.getScheduledAt());

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        loadReminderData();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipGroupTrigger = findViewById(R.id.chipGroupTrigger);
        chipGroupRepeat = findViewById(R.id.chipGroupRepeat);
        chipGroupPriority = findViewById(R.id.chipGroupPriority);
        btnDate = findViewById(R.id.btnSelectDate);
        btnTime = findViewById(R.id.btnSelectTime);
        sliderRadius = findViewById(R.id.sliderRadius);
        cbAllDay = findViewById(R.id.checkAllDay);
        layoutTimeSettings = findViewById(R.id.layoutTimeSettings);
        layoutLocationSettings = findViewById(R.id.layoutLocationSettings);
        fabSave = findViewById(R.id.fabSave);
    }

    private void loadReminderData() {
        etTitle.setText(reminder.getTitle());
        etDescription.setText(reminder.getDescription());
        
        int typeId = R.id.chipReminder;
        if (reminder.getType() == Reminder.TYPE_EVENT) {
            typeId = R.id.chipEvent;
        } else if (reminder.getType() == Reminder.TYPE_TASK) {
            typeId = R.id.chipTask;
        }
        chipGroupType.check(typeId);
        
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
        
        updateDateTimeButtons();
        
        int repeatId = R.id.chipNone;
        if ("daily".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipDaily;
        } else if ("weekly".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipWeekly;
        } else if ("monthly".equals(reminder.getRepeatType())) {
            repeatId = R.id.chipMonthly;
        }
        chipGroupRepeat.check(repeatId);
        
        int priorityId = R.id.chipLow;
        if (reminder.getPriority() == Reminder.PRIORITY_MEDIUM) {
            priorityId = R.id.chipMedium;
        } else if (reminder.getPriority() == Reminder.PRIORITY_HIGH) {
            priorityId = R.id.chipHigh;
        }
        chipGroupPriority.check(priorityId);
        
        cbAllDay.setChecked(reminder.isAllDay());
    }

    private void setupListeners() {
        chipGroupTrigger.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTime) {
                layoutTimeSettings.setVisibility(View.VISIBLE);
                layoutLocationSettings.setVisibility(View.GONE);
            } else {
                layoutTimeSettings.setVisibility(View.GONE);
                layoutLocationSettings.setVisibility(View.VISIBLE);
            }
        });

        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());
        fabSave.setOnClickListener(v -> updateReminder());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
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
                this,
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

        reminder.setTitle(title);
        reminder.setDescription(etDescription.getText().toString().trim());
        
        int selectedTypeId = chipGroupType.getCheckedChipId();
        if (selectedTypeId == R.id.chipEvent) {
            reminder.setType(Reminder.TYPE_EVENT);
        } else if (selectedTypeId == R.id.chipTask) {
            reminder.setType(Reminder.TYPE_TASK);
        } else {
            reminder.setType(Reminder.TYPE_REMINDER);
        }
        
        int selectedTriggerId = chipGroupTrigger.getCheckedChipId();
        if (selectedTriggerId == R.id.chipTime) {
            reminder.setTriggerType(Reminder.TRIGGER_TIME);
            reminder.setScheduledAt(selectedDateTime.getTimeInMillis());
        } else {
            reminder.setTriggerType(Reminder.TRIGGER_LOCATION);
            reminder.setLocation(etLocation.getText().toString().trim());
            reminder.setRadiusMeters((int) sliderRadius.getValue());
        }
        
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
        
        int selectedPriorityId = chipGroupPriority.getCheckedChipId();
        if (selectedPriorityId == R.id.chipMedium) {
            reminder.setPriority(Reminder.PRIORITY_MEDIUM);
        } else if (selectedPriorityId == R.id.chipHigh) {
            reminder.setPriority(Reminder.PRIORITY_HIGH);
        } else {
            reminder.setPriority(Reminder.PRIORITY_LOW);
        }
        
        reminder.setAllDay(cbAllDay.isChecked());
        reminder.setUpdatedAt(System.currentTimeMillis());

        dbHelper.updateReminder(reminder);
        
        ReminderScheduler.cancelReminder(this, reminder.getId());
        ReminderScheduler.scheduleReminder(this, reminder);
        
        Toast.makeText(this, "Reminder updated", Toast.LENGTH_SHORT).show();
        finish();
    }
}
