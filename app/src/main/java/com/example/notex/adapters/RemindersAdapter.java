package com.example.notex.adapters;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.R;
import com.example.notex.models.Reminder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder> {

    private Context context;
    private List<Reminder> reminders;
    private OnReminderActionListener listener;
    private Handler handler;
    private Runnable updateTimerRunnable;
    private Set<String> playedAlarms; // Track which reminders have played sound
    private Set<String> completedReminders; // Track completed reminders

    public interface OnReminderActionListener {
        void onEdit(Reminder reminder);
        void onDelete(Reminder reminder);
        void onComplete(Reminder reminder);
    }

    public RemindersAdapter(Context context, OnReminderActionListener listener) {
        this.context = context;
        this.reminders = new ArrayList<>();
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.playedAlarms = new HashSet<>();
        this.completedReminders = new HashSet<>();
        startTimerUpdates();
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    public void markAsCompleted(String reminderId) {
        completedReminders.add(reminderId);
    }

    private void startTimerUpdates() {
        updateTimerRunnable = new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                // Check if any reminder needs second-by-second updates
                boolean needsFastUpdate = false;
                for (Reminder reminder : reminders) {
                    long diff = reminder.getScheduledAt() - System.currentTimeMillis();
                    if (diff > 0 && diff < 60000) { // Less than 1 minute
                        needsFastUpdate = true;
                        break;
                    }
                }
                // Update every second if any reminder is < 1 min, otherwise every 10 seconds
                handler.postDelayed(this, needsFastUpdate ? 1000 : 10000);
            }
        };
        handler.post(updateTimerRunnable);
    }

    public void stopTimerUpdates() {
        if (handler != null && updateTimerRunnable != null) {
            handler.removeCallbacks(updateTimerRunnable);
        }
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        boolean isCompleted = completedReminders.contains(reminder.getId()) || reminder.isCompleted();
        
        // Set priority color bar
        int priorityColor;
        if (reminder.getPriority() == Reminder.PRIORITY_HIGH) {
            priorityColor = 0xFFEF4444; // Red
        } else if (reminder.getPriority() == Reminder.PRIORITY_MEDIUM) {
            priorityColor = 0xFFF59E0B; // Orange
        } else {
            priorityColor = 0xFF3B82F6; // Blue
        }
        
        // Set card background based on status
        long now = System.currentTimeMillis();
        long diff = reminder.getScheduledAt() - now;
        
        if (isCompleted) {
            // Completed - Green/Success vibe
            holder.priorityBar.setBackgroundColor(0xFF10B981);
            holder.cardReminder.setCardBackgroundColor(0xFFECFDF5);
        } else if (diff <= 0) {
            // Overdue/Missed - Gray/Ash vibe
            holder.priorityBar.setBackgroundColor(0xFF6B7280);
            holder.cardReminder.setCardBackgroundColor(0xFFF3F4F6);
            
            // Play sound if not already played
            if (!playedAlarms.contains(reminder.getId())) {
                playAlarmSound();
                playedAlarms.add(reminder.getId());
            }
        } else {
            // Active - Priority color
            holder.priorityBar.setBackgroundColor(priorityColor);
            int cardTint;
            if (reminder.getPriority() == Reminder.PRIORITY_HIGH) {
                cardTint = 0xFFFEF2F2; // Light red
            } else if (reminder.getPriority() == Reminder.PRIORITY_MEDIUM) {
                cardTint = 0xFFFFFBEB; // Light orange
            } else {
                cardTint = 0xFFEFF6FF; // Light blue
            }
            holder.cardReminder.setCardBackgroundColor(cardTint);
        }
        
        // Set title
        holder.tvTitle.setText(reminder.getTitle());
        
        // Set type icon
        String emoji = "🔔";
        if (reminder.getType().equals(Reminder.TYPE_EVENT)) {
            emoji = "📅";
        } else if (reminder.getType().equals(Reminder.TYPE_TASK)) {
            emoji = "✓";
        }
        holder.tvTypeIcon.setText(emoji);
        
        // Set description
        if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(reminder.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }
        
        // Set date time
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault());
        holder.tvDateTime.setText(sdf.format(new Date(reminder.getScheduledAt())));
        
        // Set countdown timer
        updateCountdown(holder, reminder, isCompleted);
        
        // Show repeat indicator
        if (reminder.isRecurring()) {
            holder.ivRepeat.setVisibility(View.VISIBLE);
        } else {
            holder.ivRepeat.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(reminder);
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(reminder);
        });
        
        holder.btnComplete.setOnClickListener(v -> {
            completedReminders.add(reminder.getId());
            if (listener != null) listener.onComplete(reminder);
        });
    }

    private void playAlarmSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = MediaPlayer.create(context, notification);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCountdown(ReminderViewHolder holder, Reminder reminder, boolean isCompleted) {
        long now = System.currentTimeMillis();
        long scheduledTime = reminder.getScheduledAt();
        long diff = scheduledTime - now;
        
        if (isCompleted) {
            holder.tvCountdown.setText("✓ Completed");
            holder.tvCountdown.setTextColor(0xFF10B981);
        } else if (diff <= 0) {
            holder.tvCountdown.setText("Time's up!");
            holder.tvCountdown.setTextColor(0xFFEF4444);
        } else {
            String timeLeft = formatTimeLeft(diff);
            holder.tvCountdown.setText(timeLeft);
            
            // Color based on urgency
            if (diff < 60000) { // Less than 1 minute - red
                holder.tvCountdown.setTextColor(0xFFEF4444);
            } else if (diff < 3600000) { // Less than 1 hour - orange
                holder.tvCountdown.setTextColor(0xFFF59E0B);
            } else {
                holder.tvCountdown.setTextColor(0xFF10B981);
            }
        }
    }

    private String formatTimeLeft(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        
        if (days > 0) {
            return String.format(Locale.getDefault(), "%dd %dh left", days, hours);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm left", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds left", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%ds left", seconds);
        }
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        View priorityBar;
        MaterialCardView cardReminder;
        TextView tvTypeIcon, tvTitle, tvDateTime, tvDescription, tvCountdown;
        ImageView ivRepeat;
        MaterialButton btnEdit, btnDelete, btnComplete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardReminder = (MaterialCardView) itemView;
            priorityBar = itemView.findViewById(R.id.priorityBar);
            tvTypeIcon = itemView.findViewById(R.id.tvTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCountdown = itemView.findViewById(R.id.tvCountdown);
            ivRepeat = itemView.findViewById(R.id.ivRepeat);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
}
