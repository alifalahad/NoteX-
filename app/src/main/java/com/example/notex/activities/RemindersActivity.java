package com.example.notex.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.R;
import com.example.notex.adapters.RemindersAdapter;
import com.example.notex.utils.AuthManager;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Reminder;
import com.example.notex.ReminderScheduler;
import com.example.notex.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity implements RemindersAdapter.OnReminderActionListener {

    private RecyclerView recyclerReminders;
    private RemindersAdapter adapter;
    private LinearLayout emptyState;
    private LinearLayout calendarStrip;
    private LinearLayout daysContainer;
    private TextView tvTodayLabel;
    private TabLayout tabLayout;
    private DatabaseHelper dbHelper;
    private String userId;
    private String currentFilter = null;
    private int selectedDayOffset = 0; // 0 = today

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        User currentUser = AuthManager.getInstance(this).getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = String.valueOf(currentUser.getId());

        dbHelper = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerReminders = findViewById(R.id.recyclerReminders);
        emptyState = findViewById(R.id.emptyState);
        tabLayout = findViewById(R.id.tabLayout);
        calendarStrip = findViewById(R.id.calendarStrip);
        daysContainer = findViewById(R.id.daysContainer);
        tvTodayLabel = findViewById(R.id.tvTodayLabel);
        FloatingActionButton fabNewReminder = findViewById(R.id.fabNewReminder);

        setupTabs();
        setupCalendarStrip();

        recyclerReminders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RemindersAdapter(this, this);
        recyclerReminders.setAdapter(adapter);

        fabNewReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateReminderActivity.class);
            startActivity(intent);
        });

        loadReminders();
    }

    private void setupCalendarStrip() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        tvTodayLabel.setText("Today, " + monthFormat.format(Calendar.getInstance().getTime()));
        
        buildCalendarDays();
    }

    private void buildCalendarDays() {
        daysContainer.removeAllViews();
        
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -3); // Start 3 days before today
        
        for (int i = 0; i < 7; i++) {
            final int dayOffset = i - 3;
            
            LinearLayout dayItem = new LinearLayout(this);
            dayItem.setOrientation(LinearLayout.VERTICAL);
            dayItem.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(4, 0, 4, 0);
            dayItem.setLayoutParams(params);
            dayItem.setPadding(8, 12, 8, 12);
            
            // Day name
            TextView tvDayName = new TextView(this);
            tvDayName.setText(dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]);
            tvDayName.setTextSize(12);
            tvDayName.setGravity(Gravity.CENTER);
            
            // Day number
            TextView tvDayNum = new TextView(this);
            tvDayNum.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            tvDayNum.setTextSize(16);
            tvDayNum.setTypeface(null, Typeface.BOLD);
            tvDayNum.setGravity(Gravity.CENTER);
            tvDayNum.setPadding(0, 4, 0, 0);
            
            // Style based on selection
            if (dayOffset == selectedDayOffset) {
                // Selected day - circular background
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(0xFF6366F1);
                tvDayNum.setBackground(circle);
                tvDayNum.setTextColor(Color.WHITE);
                tvDayNum.setPadding(20, 8, 20, 8);
                tvDayName.setTextColor(0xFF6366F1);
            } else if (dayOffset == 0) {
                // Today but not selected
                tvDayNum.setTextColor(0xFF6366F1);
                tvDayName.setTextColor(0xFF6366F1);
            } else {
                tvDayNum.setTextColor(0xFF374151);
                tvDayName.setTextColor(0xFF9CA3AF);
            }
            
            dayItem.addView(tvDayName);
            dayItem.addView(tvDayNum);
            
            dayItem.setOnClickListener(v -> {
                selectedDayOffset = dayOffset;
                buildCalendarDays();
                loadReminders();
            });
            
            daysContainer.addView(dayItem);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Reminders"));
        tabLayout.addTab(tabLayout.newTab().setText("Events"));
        tabLayout.addTab(tabLayout.newTab().setText("Tasks"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = null;
                        calendarStrip.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        currentFilter = Reminder.TYPE_REMINDER;
                        calendarStrip.setVisibility(View.GONE);
                        break;
                    case 2:
                        currentFilter = Reminder.TYPE_EVENT;
                        calendarStrip.setVisibility(View.GONE);
                        break;
                    case 3:
                        currentFilter = Reminder.TYPE_TASK;
                        calendarStrip.setVisibility(View.GONE);
                        break;
                }
                loadReminders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadReminders() {
        List<Reminder> allReminders = dbHelper.getAllReminders(userId);
        List<Reminder> filteredReminders = new ArrayList<>();
        
        // Get selected day bounds
        Calendar dayStart = Calendar.getInstance();
        dayStart.add(Calendar.DAY_OF_MONTH, selectedDayOffset);
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);
        
        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.add(Calendar.DAY_OF_MONTH, 1);
        
        for (Reminder reminder : allReminders) {
            // Type filter
            if (currentFilter != null && !reminder.getType().equals(currentFilter)) {
                continue;
            }
            
            // Date filter (only for "All" tab)
            if (currentFilter == null) {
                long scheduledAt = reminder.getScheduledAt();
                if (scheduledAt < dayStart.getTimeInMillis() || scheduledAt >= dayEnd.getTimeInMillis()) {
                    continue;
                }
            }
            
            filteredReminders.add(reminder);
        }
        
        if (filteredReminders.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerReminders.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerReminders.setVisibility(View.VISIBLE);
            adapter.setReminders(filteredReminders);
        }
    }

    @Override
    public void onEdit(Reminder reminder) {
        Intent intent = new Intent(this, EditReminderActivity.class);
        intent.putExtra("reminder_id", reminder.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete \"" + reminder.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ReminderScheduler.cancelReminder(this, reminder.getId());
                    dbHelper.deleteReminder(reminder.getId());
                    Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                    loadReminders();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onComplete(Reminder reminder) {
        dbHelper.markReminderCompleted(reminder.getId(), true);
        ReminderScheduler.cancelReminder(this, reminder.getId());
        adapter.markAsCompleted(reminder.getId());
        Toast.makeText(this, "Reminder completed!", Toast.LENGTH_SHORT).show();
        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopTimerUpdates();
        }
    }
}
