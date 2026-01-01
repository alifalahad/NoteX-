package com.example.notex;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityMultiPageEditorBinding;
import com.example.notex.models.Page;

/**
 * MultiPageEditorActivity - Multi-page notebook editor with scrollable pages
 * Allows users to:
 * - Add multiple pages to a notebook
 * - Scroll vertically through pages
 * - Write/draw anywhere on any page
 * - Save all changes with a save button
 */
public class MultiPageEditorActivity extends AppCompatActivity {

    private ActivityMultiPageEditorBinding binding;
    private DatabaseHelper dbHelper;
    private String notebookId;
    private String pageId;
    private boolean isEditMode = false;
    private CanvasView.Mode currentMode = CanvasView.Mode.TEXT;
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMultiPageEditorBinding.inflate(getLayoutInflater());
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
        setupSaveButton();

        if (isEditMode) {
            loadPage();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Notebook" : "New Notebook");
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog();
            } else {
                finish();
            }
        });
    }

    private void setupCanvas() {
        binding.multiPageCanvas.setMode(currentMode);
        
        // Listen for page count changes
        binding.multiPageCanvas.setOnPageChangeListener(pageCount -> {
            hasUnsavedChanges = true;
            // Update toolbar subtitle to show page count
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(pageCount + " page" + (pageCount > 1 ? "s" : ""));
            }
        });
    }

    private void setupModeButtons() {
        // Text Mode
        binding.btnTextMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.TEXT);
            highlightButton(binding.btnTextMode);
        });

        // Draw Mode
        binding.btnDrawMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.DRAW);
            highlightButton(binding.btnDrawMode);
        });

        // Scroll Mode - No interaction, just scrolling
        binding.btnScrollMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.SCROLL);
            highlightButton(binding.btnScrollMode);
            Toast.makeText(this, "Scroll mode - No editing", Toast.LENGTH_SHORT).show();
        });

        // Photo Mode
        binding.btnPhotoMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.PHOTO);
            Toast.makeText(this, "Photo feature coming soon!", Toast.LENGTH_SHORT).show();
            highlightButton(binding.btnPhotoMode);
        });

        // Voice Mode
        binding.btnVoiceMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.VOICE);
            Toast.makeText(this, "Voice feature coming soon!", Toast.LENGTH_SHORT).show();
            highlightButton(binding.btnVoiceMode);
        });

        // Add Page Button
        binding.btnAddPage.setOnClickListener(v -> {
            binding.multiPageCanvas.addNewPage();
            Toast.makeText(this, "New page added", Toast.LENGTH_SHORT).show();
            hasUnsavedChanges = true;
            
            // Scroll to bottom to show new page
            binding.scrollView.post(() -> 
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            );
        });

        // Set initial mode highlight
        highlightButton(binding.btnTextMode);
    }

    private void setupSaveButton() {
        binding.fabSave.setOnClickListener(v -> {
            savePage();
        });
    }

    private void highlightButton(View button) {
        // Reset all buttons
        binding.btnTextMode.setAlpha(0.6f);
        binding.btnDrawMode.setAlpha(0.6f);
        binding.btnScrollMode.setAlpha(0.6f);
        binding.btnPhotoMode.setAlpha(0.6f);
        binding.btnVoiceMode.setAlpha(0.6f);
        
        // Highlight selected button
        button.setAlpha(1.0f);
    }

    private void setMode(CanvasView.Mode mode) {
        currentMode = mode;
        binding.multiPageCanvas.setMode(mode);
    }

    private void loadPage() {
        Page page = dbHelper.getPageById(pageId);
        if (page != null) {
            // Load multi-page canvas content from JSON
            String content = page.getContent();
            if (content != null && !content.isEmpty()) {
                binding.multiPageCanvas.fromJson(content);
            }
            Toast.makeText(this, "Loaded: " + page.getTitle(), Toast.LENGTH_SHORT).show();
            hasUnsavedChanges = false;
        }
    }

    private void savePage() {
        // Finish any active text input first
        binding.multiPageCanvas.finishAllTextInputs();

        // Serialize all pages to JSON
        String canvasData = binding.multiPageCanvas.toJson();

        boolean success;
        if (isEditMode) {
            success = dbHelper.updatePage(pageId, "Multi-Page Notebook", canvasData);
        } else {
            success = dbHelper.addPage(notebookId, "Multi-Page Notebook", canvasData);
            if (success) {
                // Switch to edit mode after first save
                isEditMode = true;
                // Get the newly created page ID
                java.util.List<Page> pages = dbHelper.getNotebookPages(notebookId);
                if (!pages.isEmpty()) {
                    pageId = pages.get(pages.size() - 1).getId();
                }
            }
        }

        if (success) {
            Toast.makeText(this, "✅ Saved! (" + 
                binding.multiPageCanvas.getPageCount() + " page" + 
                (binding.multiPageCanvas.getPageCount() > 1 ? "s" : "") + ")", 
                Toast.LENGTH_SHORT).show();
            hasUnsavedChanges = false;
        } else {
            Toast.makeText(this, "❌ Failed to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save before leaving?")
            .setPositiveButton("Save", (dialog, which) -> {
                savePage();
                finish();
            })
            .setNegativeButton("Discard", (dialog, which) -> finish())
            .setNeutralButton("Cancel", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mark text inputs as finished when app pauses
        binding.multiPageCanvas.finishAllTextInputs();
    }
}
