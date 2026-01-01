package com.example.notex;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityCreateNotebookBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

/**
 * CreateNotebookActivity - Create new notebook with title and color
 */
public class CreateNotebookActivity extends AppCompatActivity {

    private ActivityCreateNotebookBinding binding;
    private AuthManager authManager;
    private DatabaseHelper dbHelper;
    private String selectedColor = "#2196F3"; // Default blue

    private final String[] notebookColors = {
            "#2196F3", // Blue
            "#4CAF50", // Green
            "#FFC107", // Amber
            "#FF5722", // Deep Orange
            "#9C27B0", // Purple
            "#00BCD4", // Cyan
            "#FF9800", // Orange
            "#E91E63", // Pink
            "#795548", // Brown
            "#607D8B" // Blue Grey
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNotebookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);
        dbHelper = DatabaseHelper.getInstance(this);

        setupToolbar();
        setupColorPicker();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupColorPicker() {
        GridLayout colorPicker = binding.colorPicker;

        int size = (int) (50 * getResources().getDisplayMetrics().density); // 50dp
        int margin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp

        for (String colorHex : notebookColors) {
            View colorView = new View(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);

            // Create circular background
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(colorHex));

            // Add border for selected color
            if (colorHex.equals(selectedColor)) {
                drawable.setStroke(6, Color.BLACK);
            }

            colorView.setBackground(drawable);
            colorView.setTag(colorHex);

            colorView.setOnClickListener(v -> {
                selectedColor = (String) v.getTag();
                updateColorSelection();
            });

            colorPicker.addView(colorView);
        }
    }

    private void updateColorSelection() {
        GridLayout colorPicker = binding.colorPicker;

        for (int i = 0; i < colorPicker.getChildCount(); i++) {
            View child = colorPicker.getChildAt(i);
            String colorHex = (String) child.getTag();

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(colorHex));

            if (colorHex.equals(selectedColor)) {
                drawable.setStroke(6, Color.BLACK);
            }

            child.setBackground(drawable);
        }
    }

    private void setupClickListeners() {
        binding.btnCreateNotebook.setOnClickListener(v -> createNotebook());
    }

    private void createNotebook() {
        String title = binding.etNotebookTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a notebook title", Toast.LENGTH_SHORT).show();
            return;
        }

        User currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean success = dbHelper.addNotebook(currentUser.getId(), title, selectedColor);

        if (success) {
            Toast.makeText(this, "Notebook created successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to create notebook", Toast.LENGTH_SHORT).show();
        }
    }
}
