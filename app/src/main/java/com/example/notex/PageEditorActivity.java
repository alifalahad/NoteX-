package com.example.notex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityPageEditorBinding;
import com.example.notex.models.Page;

/**
 * PageEditorActivity - Canvas-style page editor with toolbar
 */
public class PageEditorActivity extends AppCompatActivity {

    private ActivityPageEditorBinding binding;
    private DatabaseHelper dbHelper;
    private String notebookId;
    private String pageId;
    private boolean isEditMode = false;
    private CanvasView.Mode currentMode = CanvasView.Mode.TEXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        // Get intent data
        notebookId = getIntent().getStringExtra("NOTEBOOK_ID");
        pageId = getIntent().getStringExtra("PAGE_ID");

        if (notebookId == null) {
            Toast.makeText(this, "Error: Notebook not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isEditMode = (pageId != null);

        setupToolbar();
        setupCanvas();
        setupModeButtons();

        if (isEditMode) {
            loadPage();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
            getSupportActionBar().setTitle(isEditMode ? "Edit Page" : "New Page");
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            savePage();
            finish();
        });
    }

    private void setupCanvas() {
        binding.canvasView.setMode(currentMode);
    }

    private void setupModeButtons() {
        // Text Mode
        binding.btnTextMode.setOnClickListener(v -> {
            binding.canvasView.finishTextInput();
            setMode(CanvasView.Mode.TEXT);
            binding.tvModeIndicator.setText("✏️ Text Mode - Tap anywhere to type");
        });

        // Draw Mode
        binding.btnDrawMode.setOnClickListener(v -> {
            binding.canvasView.finishTextInput();
            setMode(CanvasView.Mode.DRAW);
            binding.tvModeIndicator.setText("🖊️ Draw Mode - Draw with your finger");
        });

        // Photo Mode
        binding.btnPhotoMode.setOnClickListener(v -> {
            binding.canvasView.finishTextInput();
            setMode(CanvasView.Mode.PHOTO);
            binding.tvModeIndicator.setText("📷 Photo Mode - Coming soon!");
            Toast.makeText(this, "Photo feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Voice Mode
        binding.btnVoiceMode.setOnClickListener(v -> {
            binding.canvasView.finishTextInput();
            setMode(CanvasView.Mode.VOICE);
            binding.tvModeIndicator.setText("🎤 Voice Mode - Coming soon!");
            Toast.makeText(this, "Voice feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Set initial mode
        binding.tvModeIndicator.setText("✏️ Text Mode - Tap anywhere to type");
    }

    private void setMode(CanvasView.Mode mode) {
        currentMode = mode;
        binding.canvasView.setMode(mode);
    }

    private void loadPage() {
        Page page = dbHelper.getPageById(pageId);
        if (page != null) {
            // Load canvas content from JSON
            String content = page.getContent();
            if (content != null && !content.isEmpty()) {
                binding.canvasView.fromJson(content);
            }
            Toast.makeText(this, "Loaded: " + page.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePage() {
        // Finish any active text input first
        binding.canvasView.finishTextInput();

        // Serialize canvas content to JSON
        String canvasData = binding.canvasView.toJson();

        boolean success;
        if (isEditMode) {
            success = dbHelper.updatePage(pageId, "Canvas Page", canvasData);
        } else {
            success = dbHelper.addPage(notebookId, "Canvas Page", canvasData);
        }

        if (success) {
            Toast.makeText(this, "Page saved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save page", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        savePage();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Auto-save when app goes to background
        savePage();
    }
}
