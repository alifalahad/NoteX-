package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.Reminder;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RemindersController implements Initializable {

    @FXML private HBox tabBar;
    @FXML private Button tabAll;
    @FXML private Button tabReminders;
    @FXML private Button tabEvents;
    @FXML private Button tabTasks;
    @FXML private VBox calendarSection;
    @FXML private Label todayLabel;
    @FXML private HBox calendarStrip;
    @FXML private VBox remindersList;
    @FXML private VBox contentContainer;
    @FXML private Button fabAdd;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    private String currentTab = "ALL";
    private LocalDate selectedDate = LocalDate.now();
    private Timeline refreshTimer;

    private static final String TAB_ACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: white; -fx-border-width: 0 0 3 0; -fx-padding: 12 0;";
    private static final String TAB_INACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-padding: 12 0;";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        databaseHelper = authManager.getDatabaseHelper();
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            handleBack();
            return;
        }

        setupUI();
        loadReminders();
        startRefreshTimer();
    }

    private void setupUI() {
        // Set today's date label
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
        todayLabel.setText("Today, " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        
        // Build calendar strip
        buildCalendarStrip();
        
        // Set initial tab styles
        updateTabStyles();
    }

    private void buildCalendarStrip() {
        calendarStrip.getChildren().clear();
        LocalDate today = LocalDate.now();
        
        for (int i = -1; i < 6; i++) {
            LocalDate date = today.plusDays(i);
            VBox dayBox = createDayBox(date, date.equals(selectedDate));
            calendarStrip.getChildren().add(dayBox);
        }
    }

    private VBox createDayBox(LocalDate date, boolean isSelected) {
        VBox dayBox = new VBox(4);
        dayBox.setAlignment(Pos.CENTER);
        dayBox.setPadding(new Insets(8, 12, 8, 12));
        dayBox.setMinWidth(50);
        dayBox.setCursor(javafx.scene.Cursor.HAND);

        String dayName = date.format(DateTimeFormatter.ofPattern("EEE"));
        String dayNum = String.valueOf(date.getDayOfMonth());

        Label dayLabel = new Label(dayName);
        dayLabel.setFont(Font.font("System", 12));
        dayLabel.setStyle("-fx-text-fill: black;");

        Label numLabel = new Label(dayNum);
        numLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        numLabel.setStyle("-fx-text-fill: black;");

        if (isSelected) {
            dayBox.setStyle("-fx-background-color: #5C6BC0; -fx-background-radius: 25;");
        } else if (date.equals(LocalDate.now())) {
            dayBox.setStyle("-fx-background-color: #E8EAF6; -fx-background-radius: 25;");
        } else {
            dayBox.setStyle("-fx-background-color: transparent;");
        }

        dayBox.getChildren().addAll(dayLabel, numLabel);

        dayBox.setOnMouseClicked(e -> {
            selectedDate = date;
            buildCalendarStrip();
            loadReminders();
        });

        return dayBox;
    }

    private void updateTabStyles() {
        tabAll.setStyle(currentTab.equals("ALL") ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE);
        tabReminders.setStyle(currentTab.equals("REMINDERS") ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE);
        tabEvents.setStyle(currentTab.equals("EVENTS") ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE);
        tabTasks.setStyle(currentTab.equals("TASKS") ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE);
        
        // Show/hide calendar section based on tab
        calendarSection.setVisible(currentTab.equals("ALL"));
        calendarSection.setManaged(currentTab.equals("ALL"));
    }

    private void loadReminders() {
        remindersList.getChildren().clear();
        List<Reminder> reminders;

        switch (currentTab) {
            case "REMINDERS":
                reminders = databaseHelper.getUserRemindersByType(currentUser.getId(), Reminder.ReminderType.REMINDER);
                break;
            case "EVENTS":
                reminders = databaseHelper.getUserRemindersByType(currentUser.getId(), Reminder.ReminderType.EVENT);
                break;
            case "TASKS":
                reminders = databaseHelper.getUserRemindersByType(currentUser.getId(), Reminder.ReminderType.TASK);
                break;
            default: // ALL
                String dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                reminders = databaseHelper.getUserRemindersByDate(currentUser.getId(), dateStr);
                break;
        }

        if (reminders.isEmpty()) {
            showEmptyState();
        } else {
            for (Reminder reminder : reminders) {
                remindersList.getChildren().add(createReminderCard(reminder));
            }
        }
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40, 20, 40, 20));

        Label emptyIcon = new Label("📅");
        emptyIcon.setStyle("-fx-font-size: 48;");

        Label emptyTitle = new Label("No items yet");
        emptyTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        emptyTitle.setStyle("-fx-text-fill: #666666;");

        Label emptySubtitle = new Label("Tap + to add a new reminder, event, or task");
        emptySubtitle.setFont(Font.font("System", 14));
        emptySubtitle.setStyle("-fx-text-fill: #999999;");

        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle);
        remindersList.getChildren().add(emptyState);
    }

    private VBox createReminderCard(Reminder reminder) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + getCardBackgroundColor(reminder) + "; " +
                     "-fx-background-radius: 12; " +
                     "-fx-border-color: " + reminder.getStatusColor() + "; " +
                     "-fx-border-width: 0 0 0 5; " +
                     "-fx-border-radius: 12;");

        // Header with icon and title
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(getTypeIcon(reminder.getType()));
        icon.setStyle("-fx-font-size: 24;");

        VBox titleSection = new VBox(2);
        HBox.setHgrow(titleSection, Priority.ALWAYS);

        Label titleLabel = new Label(reminder.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #212121;");

        Label dateLabel = new Label(formatDisplayDate(reminder.getTriggerDate()));
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setStyle("-fx-text-fill: #666666;");

        titleSection.getChildren().addAll(titleLabel, dateLabel);
        header.getChildren().addAll(icon, titleSection);

        // Description (if exists)
        if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
            Label descLabel = new Label(reminder.getDescription());
            descLabel.setFont(Font.font("System", 13));
            descLabel.setStyle("-fx-text-fill: #555555;");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        // Time left label
        Label timeLeftLabel = new Label(getTimeLeftString(reminder));
        timeLeftLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        timeLeftLabel.setStyle("-fx-text-fill: #4CAF50;");

        if (reminder.getStatus() == Reminder.Status.COMPLETED) {
            timeLeftLabel.setText("✓ Completed");
            timeLeftLabel.setStyle("-fx-text-fill: #4CAF50;");
        } else if (reminder.getStatus() == Reminder.Status.OVERDUE) {
            timeLeftLabel.setStyle("-fx-text-fill: #F44336;");
        }

        // Action buttons
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏ Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5C6BC0; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showReminderDialog(reminder));

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteReminder(reminder));

        Button doneBtn = new Button("✓ Done");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4CAF50; -fx-cursor: hand;");
        doneBtn.setOnAction(e -> markAsDone(reminder));

        if (reminder.getStatus() != Reminder.Status.COMPLETED) {
            actions.getChildren().addAll(editBtn, deleteBtn, doneBtn);
        } else {
            actions.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(header, timeLeftLabel, actions);
        return card;
    }

    private String getCardBackgroundColor(Reminder reminder) {
        if (reminder.getStatus() == Reminder.Status.COMPLETED) {
            return "#E8F5E9"; // Light green
        }
        switch (reminder.getPriority()) {
            case HIGH: return "#FFEBEE"; // Light red
            case MEDIUM: return "#FFF8E1"; // Light yellow
            default: return "#E3F2FD"; // Light blue
        }
    }

    private String getTypeIcon(Reminder.ReminderType type) {
        switch (type) {
            case EVENT: return "📅";
            case TASK: return "✓";
            default: return "🔔";
        }
    }

    private String formatDisplayDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM dd 'at' hh:mm a");
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String getTimeLeftString(Reminder reminder) {
        if (reminder.getTriggerDate() == null) return "";
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime triggerTime = LocalDateTime.parse(reminder.getTriggerDate(), formatter);
            LocalDateTime now = LocalDateTime.now();

            if (triggerTime.isBefore(now)) {
                long minutes = ChronoUnit.MINUTES.between(triggerTime, now);
                if (minutes < 60) return minutes + "m overdue";
                long hours = ChronoUnit.HOURS.between(triggerTime, now);
                if (hours < 24) return hours + "h overdue";
                long days = ChronoUnit.DAYS.between(triggerTime, now);
                return days + "d overdue";
            }

            long minutes = ChronoUnit.MINUTES.between(now, triggerTime);
            long seconds = ChronoUnit.SECONDS.between(now, triggerTime) % 60;
            
            if (minutes < 60) {
                return minutes + "m " + seconds + "s left";
            }
            
            long hours = ChronoUnit.HOURS.between(now, triggerTime);
            long remainingMinutes = minutes % 60;
            
            if (hours < 24) {
                return hours + "h " + remainingMinutes + "m left";
            }
            
            long days = ChronoUnit.DAYS.between(now, triggerTime);
            long remainingHours = hours % 24;
            return days + "d " + remainingHours + "h left";
            
        } catch (Exception e) {
            return "";
        }
    }

    private void startRefreshTimer() {
        refreshTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Refresh time left labels
            loadReminders();
        }));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void deleteReminder(Reminder reminder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Reminder");
        confirm.setHeaderText("Delete \"" + reminder.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            databaseHelper.deleteReminder(reminder.getId());
            loadReminders();
        }
    }

    private void markAsDone(Reminder reminder) {
        databaseHelper.markReminderCompleted(reminder.getId());
        loadReminders();
    }

    @FXML
    private void handleAddReminder() {
        showReminderDialog(null);
    }

    private void showReminderDialog(Reminder existingReminder) {
        if (refreshTimer != null) {
            refreshTimer.pause();
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingReminder == null ? "Create Reminder" : "Edit Reminder");
        dialog.setWidth(500);
        dialog.setHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #5C6BC0;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            if (refreshTimer != null) refreshTimer.play();
            dialog.close();
        });

        Label titleLabel = new Label(existingReminder == null ? "Create Reminder" : "Edit Reminder");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        header.getChildren().addAll(closeBtn, titleLabel);

        // Content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Type selection
        Label typeLabel = new Label("Type");
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox typeBox = new HBox(10);
        ToggleGroup typeGroup = new ToggleGroup();

        ToggleButton reminderType = createTypeButton("🔔 Reminder", typeGroup);
        ToggleButton eventType = createTypeButton("📅 Event", typeGroup);
        ToggleButton taskType = createTypeButton("✓ Task", typeGroup);

        if (existingReminder != null) {
            switch (existingReminder.getType()) {
                case EVENT: eventType.setSelected(true); break;
                case TASK: taskType.setSelected(true); break;
                default: reminderType.setSelected(true);
            }
        } else {
            reminderType.setSelected(true);
        }

        typeBox.getChildren().addAll(reminderType, eventType, taskType);

        // Title field
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        titleField.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-padding: 12;");
        if (existingReminder != null) titleField.setText(existingReminder.getTitle());

        // Description field
        TextArea descField = new TextArea();
        descField.setPromptText("Description (Optional)");
        descField.setPrefRowCount(3);
        descField.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8;");
        if (existingReminder != null && existingReminder.getDescription() != null) {
            descField.setText(existingReminder.getDescription());
        }

        // Trigger selection
        Label triggerLabel = new Label("Trigger");
        triggerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox triggerBox = new HBox(10);
        ToggleGroup triggerGroup = new ToggleGroup();

        ToggleButton timeTrigger = createTriggerButton("⏰ Time", triggerGroup);
        ToggleButton locationTrigger = createTriggerButton("📍 Location", triggerGroup);
        timeTrigger.setSelected(true);

        triggerBox.getChildren().addAll(timeTrigger, locationTrigger);

        // Time/Location specific fields
        VBox timeFields = new VBox(15);
        VBox locationFields = new VBox(15);

        // When section (Date and Time)
        Label whenLabel = new Label("When");
        whenLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox whenBox = new HBox(10);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 20;");

        ComboBox<String> timePicker = new ComboBox<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                String time = String.format("%02d:%02d", h, m);
                String displayTime = LocalTime.of(h, m).format(DateTimeFormatter.ofPattern("hh:mm a"));
                timePicker.getItems().add(displayTime);
            }
        }
        timePicker.setValue(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        timePicker.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 20;");
        timePicker.setEditable(true);

        if (existingReminder != null && existingReminder.getTriggerDate() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime dt = LocalDateTime.parse(existingReminder.getTriggerDate(), formatter);
                datePicker.setValue(dt.toLocalDate());
                timePicker.setValue(dt.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
            } catch (Exception e) {}
        }

        whenBox.getChildren().addAll(datePicker, timePicker);

        // Quick Presets
        Label presetsLabel = new Label("Quick Presets");
        presetsLabel.setStyle("-fx-text-fill: #666666;");

        HBox presetsBox = new HBox(10);
        Button preset1h = createPresetButton("In 1 hour");
        Button presetToday6pm = createPresetButton("Today 6 PM");
        Button presetTomorrow9am = createPresetButton("Tomorrow 9 AM");

        preset1h.setOnAction(e -> {
            LocalDateTime newTime = LocalDateTime.now().plusHours(1);
            datePicker.setValue(newTime.toLocalDate());
            timePicker.setValue(newTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
        });

        presetToday6pm.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            timePicker.setValue("06:00 PM");
        });

        presetTomorrow9am.setOnAction(e -> {
            datePicker.setValue(LocalDate.now().plusDays(1));
            timePicker.setValue("09:00 AM");
        });

        presetsBox.getChildren().addAll(preset1h, presetToday6pm, presetTomorrow9am);

        timeFields.getChildren().addAll(whenLabel, whenBox, presetsLabel, presetsBox);

        // Location fields
        TextField locationNameField = new TextField();
        locationNameField.setPromptText("Location Name");
        locationNameField.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-padding: 12;");

        Label radiusLabel = new Label("Reminder Radius");
        radiusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Slider radiusSlider = new Slider(50, 500, 100);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.setMajorTickUnit(100);

        Label radiusValueLabel = new Label("100 meters");
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            radiusValueLabel.setText(newVal.intValue() + " meters");
        });

        if (existingReminder != null && existingReminder.getTriggerType() == Reminder.TriggerType.LOCATION) {
            locationTrigger.setSelected(true);
            locationNameField.setText(existingReminder.getLocationName());
            radiusSlider.setValue(existingReminder.getLocationRadius());
        }

        locationFields.getChildren().addAll(locationNameField, radiusLabel, radiusSlider, radiusValueLabel);
        locationFields.setVisible(false);
        locationFields.setManaged(false);

        // Toggle between time and location
        timeTrigger.setOnAction(e -> {
            timeFields.setVisible(true);
            timeFields.setManaged(true);
            locationFields.setVisible(false);
            locationFields.setManaged(false);
        });

        locationTrigger.setOnAction(e -> {
            timeFields.setVisible(false);
            timeFields.setManaged(false);
            locationFields.setVisible(true);
            locationFields.setManaged(true);
        });

        // Repeat section
        Label repeatLabel = new Label("Repeat");
        repeatLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox repeatBox = new HBox(10);
        ToggleGroup repeatGroup = new ToggleGroup();

        ToggleButton repeatNone = createOptionButton("None", repeatGroup);
        ToggleButton repeatDaily = createOptionButton("Daily", repeatGroup);
        ToggleButton repeatWeekly = createOptionButton("Weekly", repeatGroup);
        ToggleButton repeatMonthly = createOptionButton("Monthly", repeatGroup);

        if (existingReminder != null) {
            switch (existingReminder.getRepeatType()) {
                case DAILY: repeatDaily.setSelected(true); break;
                case WEEKLY: repeatWeekly.setSelected(true); break;
                case MONTHLY: repeatMonthly.setSelected(true); break;
                default: repeatNone.setSelected(true);
            }
        } else {
            repeatNone.setSelected(true);
        }

        repeatBox.getChildren().addAll(repeatNone, repeatDaily, repeatWeekly, repeatMonthly);

        // Priority section
        Label priorityLabel = new Label("Priority");
        priorityLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox priorityBox = new HBox(10);
        ToggleGroup priorityGroup = new ToggleGroup();

        ToggleButton priorityLow = createPriorityButton("Low", "#2196F3", priorityGroup);
        ToggleButton priorityMedium = createPriorityButton("Medium", "#FF9800", priorityGroup);
        ToggleButton priorityHigh = createPriorityButton("High", "#F44336", priorityGroup);

        if (existingReminder != null) {
            switch (existingReminder.getPriority()) {
                case MEDIUM: priorityMedium.setSelected(true); break;
                case HIGH: priorityHigh.setSelected(true); break;
                default: priorityLow.setSelected(true);
            }
        } else {
            priorityLow.setSelected(true);
        }

        priorityBox.getChildren().addAll(priorityLow, priorityMedium, priorityHigh);

        // Ringtone section
        Label ringtoneLabel = new Label("Ringtone");
        ringtoneLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        ComboBox<String> ringtoneCombo = new ComboBox<>();
        ringtoneCombo.getItems().addAll("Default Notification Sound", "Alarm", "Bell", "Chime", "Silent");
        ringtoneCombo.setValue("Default Notification Sound");
        ringtoneCombo.setMaxWidth(Double.MAX_VALUE);
        ringtoneCombo.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-padding: 8;");

        if (existingReminder != null && existingReminder.getRingtone() != null) {
            ringtoneCombo.setValue(existingReminder.getRingtone());
        }

        // All Day checkbox (for location)
        CheckBox allDayCheck = new CheckBox("All Day Event");
        if (existingReminder != null) {
            allDayCheck.setSelected(existingReminder.isAllDay());
        }

        content.getChildren().addAll(
            typeLabel, typeBox,
            titleField, descField,
            triggerLabel, triggerBox,
            timeFields, locationFields,
            repeatLabel, repeatBox,
            priorityLabel, priorityBox,
            ringtoneLabel, ringtoneCombo,
            allDayCheck
        );

        scrollPane.setContent(content);

        // Save button
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(15, 20, 15, 20));
        bottomBar.setStyle("-fx-background-color: #F5F5F5;");

        Button saveBtn = new Button("💾 Save");
        saveBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-font-size: 14; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            // Validate
            if (titleField.getText().trim().isEmpty()) {
                showError("Please enter a title");
                return;
            }

            // Create or update reminder
            Reminder reminder = existingReminder != null ? existingReminder : new Reminder();
            reminder.setUserId(currentUser.getId());
            reminder.setTitle(titleField.getText().trim());
            reminder.setDescription(descField.getText().trim());

            // Set type
            if (eventType.isSelected()) {
                reminder.setType(Reminder.ReminderType.EVENT);
            } else if (taskType.isSelected()) {
                reminder.setType(Reminder.ReminderType.TASK);
            } else {
                reminder.setType(Reminder.ReminderType.REMINDER);
            }

            // Set trigger type and data
            if (locationTrigger.isSelected()) {
                reminder.setTriggerType(Reminder.TriggerType.LOCATION);
                reminder.setLocationName(locationNameField.getText().trim());
                reminder.setLocationRadius((int) radiusSlider.getValue());
            } else {
                reminder.setTriggerType(Reminder.TriggerType.TIME);
                
                // Parse date and time
                LocalDate date = datePicker.getValue();
                String timeStr = timePicker.getValue();
                try {
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    LocalTime time = LocalTime.parse(timeStr, timeFormatter);
                    LocalDateTime dateTime = LocalDateTime.of(date, time);
                    reminder.setTriggerDate(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                } catch (Exception ex) {
                    // Try alternative format
                    try {
                        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                        LocalTime time = LocalTime.parse(timeStr, timeFormatter);
                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        reminder.setTriggerDate(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    } catch (Exception ex2) {
                        showError("Invalid time format");
                        return;
                    }
                }
            }

            // Set repeat
            if (repeatDaily.isSelected()) {
                reminder.setRepeatType(Reminder.RepeatType.DAILY);
            } else if (repeatWeekly.isSelected()) {
                reminder.setRepeatType(Reminder.RepeatType.WEEKLY);
            } else if (repeatMonthly.isSelected()) {
                reminder.setRepeatType(Reminder.RepeatType.MONTHLY);
            } else {
                reminder.setRepeatType(Reminder.RepeatType.NONE);
            }

            // Set priority
            if (priorityMedium.isSelected()) {
                reminder.setPriority(Reminder.Priority.MEDIUM);
            } else if (priorityHigh.isSelected()) {
                reminder.setPriority(Reminder.Priority.HIGH);
            } else {
                reminder.setPriority(Reminder.Priority.LOW);
            }

            reminder.setRingtone(ringtoneCombo.getValue());
            reminder.setAllDay(allDayCheck.isSelected());

            // Save to database
            boolean success;
            if (existingReminder != null) {
                success = databaseHelper.updateReminder(reminder);
            } else {
                success = databaseHelper.addReminder(reminder);
            }

            if (success) {
                if (refreshTimer != null) refreshTimer.play();
                dialog.close();
                loadReminders();
            } else {
                showError("Failed to save reminder");
            }
        });

        bottomBar.getChildren().add(saveBtn);

        root.setTop(header);
        root.setCenter(scrollPane);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private ToggleButton createTypeButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            } else {
                btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            }
        });
        return btn;
    }

    private ToggleButton createTriggerButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #333333; -fx-background-radius: 20; -fx-padding: 8 16;");
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            } else {
                btn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #333333; -fx-background-radius: 20; -fx-padding: 8 16;");
            }
        });
        return btn;
    }

    private ToggleButton createOptionButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            } else {
                btn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #333333; -fx-background-radius: 20; -fx-padding: 8 16;");
            }
        });
        return btn;
    }

    private ToggleButton createPriorityButton(String text, String color, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20;");
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-border-color: #333333; -fx-border-width: 2; -fx-border-radius: 20;");
            } else {
                btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20;");
            }
        });
        return btn;
    }

    private Button createPresetButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
        return btn;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleTabAll() {
        currentTab = "ALL";
        updateTabStyles();
        loadReminders();
    }

    @FXML
    private void handleTabReminders() {
        currentTab = "REMINDERS";
        updateTabStyles();
        loadReminders();
    }

    @FXML
    private void handleTabEvents() {
        currentTab = "EVENTS";
        updateTabStyles();
        loadReminders();
    }

    @FXML
    private void handleTabTasks() {
        currentTab = "TASKS";
        updateTabStyles();
        loadReminders();
    }

    @FXML
    private void handleBack() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        NoteXApp.setRoot("views/user_home");
    }
}
