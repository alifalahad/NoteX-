package com.example.notex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.models.User;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * UserAdapter - RecyclerView adapter for displaying users
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(OnUserClickListener listener) {
        this.users = new ArrayList<>();
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserIcon;
        private TextView tvUsername;
        private TextView tvEmail;
        private TextView tvUserId;
        private TextView tvPassword;
        private Chip chipRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserIcon = itemView.findViewById(R.id.ivUserIcon);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvPassword = itemView.findViewById(R.id.tvPassword);
            chipRole = itemView.findViewById(R.id.chipRole);
        }

        public void bind(User user) {
            tvUsername.setText(user.getUsername());
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
            tvUserId.setText("ID: " + user.getId().substring(0, Math.min(8, user.getId().length())));
            
            // Display password hash (truncated for security)
            String passwordHash = user.getPasswordHash();
            if (passwordHash != null && passwordHash.length() > 16) {
                tvPassword.setText("Pass: " + passwordHash.substring(0, 16) + "...");
            } else {
                tvPassword.setText("Pass: " + (passwordHash != null ? passwordHash : "N/A"));
            }

            // Set role chip
            chipRole.setText(user.getRole().name());
            if (user.isAdmin()) {
                chipRole.setChipBackgroundColorResource(R.color.secondary);
                ivUserIcon.setImageResource(R.drawable.ic_admin);
            } else {
                chipRole.setChipBackgroundColorResource(R.color.primary);
                ivUserIcon.setImageResource(R.drawable.ic_user);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
