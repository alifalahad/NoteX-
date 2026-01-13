package com.example.notex.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.notex.R;
import com.example.notex.ReminderScheduler;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class CreateReminderActivity extends AppCompatActivity {

    private static final int RINGTONE_PICKER_REQUEST = 999;

    private TextInputEditText etTitle, etDescription, etLocation;
    private ChipGroup chipGroupType, chipGroupRepeat, chipGroupPriority, chipGroupPresets, chipGroupTrigger;
    private MaterialButton btnDate, btnTime, btnSelectRingtone;
    private CheckBox cbAllDay;
    private ExtendedFloatingActionButton fabSave;
    private LinearLayout layoutTimeSettings, layoutLocationSettings;
    private com.google.android.material.slider.Slider sliderRadius;
    private TextView tvRadiusValue;

    private DatabaseHelper dbHelper;
    private Calendar selectedDateTime;
    private String userId;
    private Uri selectedRingtoneUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_reminder);

        // Get user
        User currentUser = AuthManager.getInstance(this).getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = String.valueOf(currentUser.getId());

        dbHelper = DatabaseHelper.getInstance(this);
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.HOUR_OF_DAY, 1); // Default to 1 hour from now
        selectedRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        setupListeners();
        updateDateTimeButtons();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipGroupRepeat = findViewById(R.id.chipGroupRepeat);
        chipGroupPriority = findViewById(R.id.chipGroupPriority);
        chipGroupPresets = findViewById(R.id.chipGroupPresets);
        chipGroupTrigger = findViewById(R.id.chipGroupTrigger);
        btnDate = findViewById(R.id.btnSelectDate);
        btnTime = findViewById(R.id.btnSelectTime);
        btnSelectRingtone = findViewById(R.id.btnSelectRingtone);
        cbAllDay = findViewById(R.id.checkAllDay);
        fabSave = findViewById(R.id.fabSave);
        layoutTimeSettings = findViewById(R.id.layoutTimeSettings);
        layoutLocationSettings = findViewById(R.id.layoutLocationSettings);
        sliderRadius = findViewById(R.id.sliderRadius);
        tvRadiusValue = findViewById(R.id.tvRadiusValue);
    }

    private void setupListeners() {
        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());
        btnSelectRingtone.setOnClickListener(v -> showRingtonePicker());
        fabSave.setOnClickListener(v -> saveReminder());

        // Trigger type switching
        chipGroupTrigger.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTime) {
                layoutTimeSettings.setVisibility(View.VISIBLE);
                layoutLocationSettings.setVisibility(View.GONE);
            } else if (checkedId == R.id.chipLocation) {
                layoutTimeSettings.setVisibility(View.GONE);
                layoutLocationSettings.setVisibility(View.VISIBLE);
            }
        });

        // Radius slider
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            tvRadiusValue.setText((int)value + " meters");
        });

        // Quick presets
        Chip chipIn1Hour = findViewById(R.id.chipIn1Hour);
        Chip chipToday6PM = findViewById(R.id.chipToday6PM);
        Chip chipTomorrowMorning = findViewById(R.id.chipTomorrowMorning);

        chipIn1Hour.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.add(Calendar.HOUR_OF_DAY, 1);
            updateDateTimeButtons();
        });

        chipToday6PM.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.HOUR_OF_DAY, 18);
            selectedDateTime.set(Calendar.MINUTE, 0);
            updateDateTimeButtons();
        });

        chipTomorrowMorning.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.add(Calendar.DAY_OF_MONTH, 1);
            selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
            selectedDateTime.set(Calendar.MINUTE, 0);
            updateDateTimeButtons();
        });
    }

    private void showRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri);
        startActivityForResult(intent, RINGTONE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedRingtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (selectedRingtoneUri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(this, selectedRingtoneUri);
                btnSelectRingtone.setText(ringtone.getTitle(this));
            } else {
                btnSelectRingtone.setText("Silent");
            }
        }
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        btnDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }

        // Check trigger type
        int selectedTriggerId = chipGroupTrigger.getCheckedChipId();
        boolean isTimeTrigger = selectedTriggerId == R.id.chipTime;

        // Validate based on trigger type
        if (!isTimeTrigger) {
            String location = etLocation.getText().toString().trim();
            if (location.isEmpty()) {
                etLocation.setError("Location is required");
                return;
            }
        }

        // Get type first
        int selectedTypeId = chipGroupType.getCheckedChipId();
        String type = Reminder.TYPE_REMINDER;
        if (selectedTypeId == R.id.chipEvent) {
            type = Reminder.TYPE_EVENT;
        } else if (selectedTypeId == R.id.chipTask) {
            type = Reminder.TYPE_TASK;
        }

        String id = UUID.randomUUID().toString();
        Reminder reminder = new Reminder(id, String.valueOf(userId), title, type);
        reminder.setDescription(etDescription.getText().toString().trim());
        
        // Set trigger type and related data
        if (isTimeTrigger) {
            reminder.setTriggerType(Reminder.TRIGGER_TIME);
            reminder.setScheduledAt(selectedDateTime.getTimeInMillis());
        } else {
            reminder.setTriggerType(Reminder.TRIGGER_LOCATION);
            reminder.setLocation(etLocation.getText().toString().trim());
            reminder.setRadiusMeters((int) sliderRadius.getValue());
            // You can add latitude/longitude here if you implement location picker
            reminder.setLatitude(0.0);
            reminder.setLongitude(0.0);
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
        
        reminder.setAllDay(cbAllDay.isChecked());

        // Save to database
        dbHelper.createReminder(reminder);
        
        // Schedule alarm (only for time-based reminders)
        if (isTimeTrigger) {
            ReminderScheduler.scheduleReminder(this, reminder);
        }
        
        Toast.makeText(this, "Reminder created!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
