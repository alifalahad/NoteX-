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
    private CanvasView.Mode currentMode = CanvasView.Mode.SCROLL; // Default to scroll
    private boolean hasUnsavedChanges = false;
    
    // Track current pen settings
    private CanvasView.PenStyle currentPenStyle = CanvasView.PenStyle.NORMAL;
    private boolean isDashedLine = false;
    private int currentPenColor = android.graphics.Color.BLACK;
    private float currentStrokeWidth = 5f;
    
    // Track current shape settings
    private CanvasView.ShapeType currentShapeType = CanvasView.ShapeType.RECTANGLE;
    private int currentShapeColor = android.graphics.Color.BLACK;
    private float currentShapeStrokeWidth = 5f;
    private boolean isDashedShape = false;
    private boolean isFilledShape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMultiPageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        // Get intent data
        notebookId = getIntent().getStringExtra("NOTEBOOK_ID");
        pageId = getIntent().getStringExtra("PAGE_ID");
        String notebookName = getIntent().getStringExtra("NOTEBOOK_NAME");

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
        } else {
            // Check if notebook has existing pages
            java.util.List<Page> existingPages = dbHelper.getNotebookPages(notebookId);
            if (existingPages != null && !existingPages.isEmpty()) {
                // Load the first page
                pageId = existingPages.get(0).getId();
                isEditMode = true;
                loadPage();
            } else {
                // Create first page automatically for new notebooks
                binding.multiPageCanvas.addNewPage();
                // Auto-save the initial page
                String canvasData = binding.multiPageCanvas.toJson();
                boolean success = dbHelper.addPage(notebookId, "Multi-Page Notebook", canvasData);
                if (success) {
                    isEditMode = true;
                    java.util.List<Page> pages = dbHelper.getNotebookPages(notebookId);
                    if (!pages.isEmpty()) {
                        pageId = pages.get(pages.size() - 1).getId();
                    }
                }
            }
            // Set toolbar title with notebook name
            if (getSupportActionBar() != null && notebookName != null) {
                getSupportActionBar().setTitle(notebookName);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
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
        
        // Listen for mode changes and text selection
        binding.multiPageCanvas.setOnModeChangeListener(new CanvasView.OnTextSelectedListener() {
            @Override
            public void onModeChanged(CanvasView.Mode newMode) {
                currentMode = newMode;
                // Auto-switched to TEXT mode for editing after text creation
                if (newMode == CanvasView.Mode.TEXT) {
                    highlightButton(binding.btnTextMode);
                    // Enable text edit mode
                    binding.multiPageCanvas.setTextEditMode(true);
                }
            }
            
            @Override
            public void onTextSelected(CanvasView.TextElement textElement) {
                // Show text formatting toolbar when text is selected
                showTextFormatting(textElement);
            }
        });
    }

    private void setupModeButtons() {
        // Pen/Draw Mode with long-press options
        binding.btnDrawMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.DRAW);
            highlightButton(binding.btnDrawMode);
        });
        
        binding.btnDrawMode.setOnLongClickListener(v -> {
            showPenOptions();
            return true;
        });

        // Load saved eraser preferences
        android.content.SharedPreferences prefs = getSharedPreferences("NoteXPrefs", MODE_PRIVATE);
        float savedEraserSize = prefs.getFloat("eraserSize", 40f);
        // Always start with Full Line mode
        binding.multiPageCanvas.setEraserMode(CanvasView.EraserMode.FULL_LINE);
        binding.multiPageCanvas.setEraserSize(savedEraserSize);
        
        // Eraser Mode
        binding.btnEraser.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            // Always default to Full Line mode
            binding.multiPageCanvas.setEraserMode(CanvasView.EraserMode.FULL_LINE);
            setMode(CanvasView.Mode.ERASER);
            highlightButton(binding.btnEraser);
            Toast.makeText(this, "Eraser mode - Full Line", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnEraser.setOnLongClickListener(v -> {
            showEraserOptions();
            return true;
        });

        // Text Mode - single click shows options dialog
        binding.btnTextMode.setOnClickListener(v -> {
            showTextModeOptions();
        });
        
        // Save Button in Toolbar
        binding.btnSave.setOnClickListener(v -> {
            savePage();
        });
        
        // Scroll Mode FAB (new floating button)
        binding.fabScrollMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.SCROLL);
            clearToolHighlights(); // Don't highlight any tool when in scroll mode
            Toast.makeText(this, "Scroll mode active", Toast.LENGTH_SHORT).show();
        });

        // Shapes Mode - click to show shape options
        binding.btnShapes.setOnClickListener(v -> {
            showShapeOptions();
        });

        // Photo Mode - show menu with upload/delete options
        binding.btnPhotoMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.PHOTO);
            highlightButton(binding.btnPhotoMode);
            showPhotoMenu();
        });

        // Sticky Note Mode - click to show color options
        binding.btnStickyNote.setOnClickListener(v -> {
            showStickyNoteColorOptions();
        });

        // Laser Pointer Mode
        binding.btnLaser.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.LASER_POINTER);
            highlightButton(binding.btnLaser);
            Toast.makeText(this, "Laser Pointer - Dot mode", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnLaser.setOnLongClickListener(v -> {
            showLaserOptions();
            return true;
        });

        // Voice Mode
        binding.btnVoiceMode.setOnClickListener(v -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.VOICE);
            highlightButton(binding.btnVoiceMode);
            showVoiceMenu();
        });

        // Undo Button
        binding.btnUndo.setOnClickListener(v -> {
            if (binding.multiPageCanvas.undo()) {
                Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show();
                hasUnsavedChanges = true;
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        });

        // Redo Button
        binding.btnRedo.setOnClickListener(v -> {
            if (binding.multiPageCanvas.redo()) {
                Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show();
                hasUnsavedChanges = true;
            } else {
                Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
            }
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

        // Set initial mode - no highlighting for scroll mode
        clearToolHighlights();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        // Handle Delete or Backspace key to delete selected image
        if ((keyCode == android.view.KeyEvent.KEYCODE_DEL || keyCode == android.view.KeyEvent.KEYCODE_FORWARD_DEL)) {
            CanvasView currentCanvas = binding.multiPageCanvas.getCurrentCanvas();
            if (currentCanvas != null && currentCanvas.hasSelectedImage()) {
                if (currentCanvas.deleteSelectedImage()) {
                    Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                    hasUnsavedChanges = true;
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void highlightButton(View button) {
        // Reset all mode buttons
        binding.btnDrawMode.setAlpha(0.4f);
        binding.btnEraser.setAlpha(0.4f);
        binding.btnTextMode.setAlpha(0.4f);
        binding.btnShapes.setAlpha(0.4f);
        binding.btnPhotoMode.setAlpha(0.4f);
        binding.btnStickyNote.setAlpha(0.4f);
        binding.btnLaser.setAlpha(0.4f);
        binding.btnVoiceMode.setAlpha(0.4f);
        
        // Highlight selected button
        button.setAlpha(1.0f);
    }
    
    private void clearToolHighlights() {
        // Reset all mode buttons to default
        binding.btnDrawMode.setAlpha(0.4f);
        binding.btnEraser.setAlpha(0.4f);
        binding.btnTextMode.setAlpha(0.4f);
        binding.btnShapes.setAlpha(0.4f);
        binding.btnPhotoMode.setAlpha(0.4f);
        binding.btnStickyNote.setAlpha(0.4f);
        binding.btnLaser.setAlpha(0.4f);
        binding.btnVoiceMode.setAlpha(0.4f);
    }
    
    private void showPenOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Create custom dialog layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        // Title
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Pen Options");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // === PEN STYLE with visual selection ===
        android.widget.TextView styleLabel = new android.widget.TextView(this);
        styleLabel.setText("Pen Style:");
        styleLabel.setTextSize(16);
        styleLabel.setPadding(0, 10, 0, 10);
        layout.addView(styleLabel);
        
        String[] penStyles = {"Normal", "Pencil", "Highlighter"};
        android.widget.LinearLayout styleRow = new android.widget.LinearLayout(this);
        styleRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.Button[] styleButtons = new android.widget.Button[3];
        
        for (int i = 0; i < penStyles.length; i++) {
            final int index = i;
            android.widget.Button btn = new android.widget.Button(this);
            btn.setText(penStyles[i]);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                0, 100, 1.0f);
            params.setMargins(5, 0, 5, 0);
            btn.setLayoutParams(params);
            
            // Initialize with current state
            CanvasView.PenStyle checkStyle = index == 0 ? CanvasView.PenStyle.NORMAL :
                                             index == 1 ? CanvasView.PenStyle.PENCIL :
                                             CanvasView.PenStyle.HIGHLIGHTER;
            boolean isSelected = (currentPenStyle == checkStyle);
            
            if (isSelected) {
                btn.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                btn.setTextColor(android.graphics.Color.WHITE);
            } else {
                btn.setBackgroundColor(android.graphics.Color.parseColor("#E8E8E8"));
                btn.setTextColor(android.graphics.Color.DKGRAY);
            }
            
            btn.setOnClickListener(v -> {
                // Update state
                currentPenStyle = index == 0 ? CanvasView.PenStyle.NORMAL :
                                  index == 1 ? CanvasView.PenStyle.PENCIL :
                                  CanvasView.PenStyle.HIGHLIGHTER;
                
                // Update all buttons' visual state
                for (int j = 0; j < styleButtons.length; j++) {
                    if (j == index) {
                        styleButtons[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                        styleButtons[j].setTextColor(android.graphics.Color.WHITE);
                    } else {
                        styleButtons[j].setBackgroundColor(android.graphics.Color.parseColor("#E8E8E8"));
                        styleButtons[j].setTextColor(android.graphics.Color.DKGRAY);
                    }
                }
                
                binding.multiPageCanvas.setPenStyle(currentPenStyle);
            });
            styleButtons[index] = btn;
            styleRow.addView(btn);
        }
        layout.addView(styleRow);
        
        // === LINE STYLE with RadioGroup ===
        android.widget.TextView lineLabel = new android.widget.TextView(this);
        lineLabel.setText("Line Style:");
        lineLabel.setTextSize(16);
        lineLabel.setPadding(0, 20, 0, 10);
        layout.addView(lineLabel);
        
        android.widget.RadioGroup lineStyleGroup = new android.widget.RadioGroup(this);
        lineStyleGroup.setOrientation(android.widget.RadioGroup.HORIZONTAL);
        
        android.widget.RadioButton normalLineBtn = new android.widget.RadioButton(this);
        normalLineBtn.setId(View.generateViewId()); // Generate unique ID
        normalLineBtn.setText("━━━  Normal");
        normalLineBtn.setTextSize(14);
        normalLineBtn.setChecked(!isDashedLine); // Set based on current state
        android.widget.RadioGroup.LayoutParams radioParams1 = new android.widget.RadioGroup.LayoutParams(
            0, android.widget.RadioGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        radioParams1.setMargins(0, 0, 10, 0);
        normalLineBtn.setLayoutParams(radioParams1);
        
        android.widget.RadioButton dashedLineBtn = new android.widget.RadioButton(this);
        dashedLineBtn.setId(View.generateViewId()); // Generate unique ID
        dashedLineBtn.setText("- - -  Dashed");
        dashedLineBtn.setTextSize(14);
        dashedLineBtn.setChecked(isDashedLine); // Set based on current state
        android.widget.RadioGroup.LayoutParams radioParams2 = new android.widget.RadioGroup.LayoutParams(
            0, android.widget.RadioGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        dashedLineBtn.setLayoutParams(radioParams2);
        
        lineStyleGroup.addView(normalLineBtn);
        lineStyleGroup.addView(dashedLineBtn);
        
        // Use OnCheckedChangeListener instead of individual onClick
        lineStyleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == normalLineBtn.getId()) {
                isDashedLine = false;
                binding.multiPageCanvas.setDashedLine(false);
                Toast.makeText(this, "Normal line selected", Toast.LENGTH_SHORT).show();
            } else if (checkedId == dashedLineBtn.getId()) {
                isDashedLine = true;
                binding.multiPageCanvas.setDashedLine(true);
                Toast.makeText(this, "Dashed line selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        layout.addView(lineStyleGroup);
        
        // === COLOR PICKER with selection border ===
        android.widget.TextView colorLabel = new android.widget.TextView(this);
        colorLabel.setText("Color:");
        colorLabel.setTextSize(16);
        colorLabel.setPadding(0, 20, 0, 10);
        layout.addView(colorLabel);
        
        // Expanded color palette with two rows
        int[] colors = {
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#424242"), // Dark Gray
            android.graphics.Color.BLUE,
            android.graphics.Color.parseColor("#2196F3"), // Light Blue
            android.graphics.Color.RED,
            android.graphics.Color.parseColor("#E91E63"), // Pink
            android.graphics.Color.GREEN,
            android.graphics.Color.parseColor("#4CAF50"), // Light Green
            android.graphics.Color.parseColor("#FF9800"), // Orange
            android.graphics.Color.parseColor("#FFEB3B"), // Yellow
            android.graphics.Color.parseColor("#9C27B0"), // Purple
            android.graphics.Color.parseColor("#795548")  // Brown
        };
        
        // Create two rows for colors
        android.widget.LinearLayout colorsContainer = new android.widget.LinearLayout(this);
        colorsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        android.widget.LinearLayout colorRow1 = new android.widget.LinearLayout(this);
        colorRow1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        
        android.widget.LinearLayout colorRow2 = new android.widget.LinearLayout(this);
        colorRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow2.setPadding(0, 8, 0, 0);
        
        View[] colorContainers = new View[colors.length];
        
        for (int i = 0; i < colors.length; i++) {
            final int index = i;
            final int color = colors[i];
            
            // Container with selection border
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                new android.widget.LinearLayout.LayoutParams(85, 85);
            containerParams.setMargins(8, 0, 8, 0);
            container.setLayoutParams(containerParams);
            container.setPadding(5, 5, 5, 5);
            
            // Set selection based on current color
            if (color == currentPenColor) {
                container.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
            } else {
                container.setBackgroundColor(android.graphics.Color.LTGRAY);
            }
            
            // Color circle
            View colorCircle = new View(this);
            colorCircle.setBackgroundColor(color);
            container.addView(colorCircle);
            
            colorContainers[index] = container;
            
            container.setOnClickListener(v -> {
                // Update state
                currentPenColor = color;
                
                // Update all containers' border
                for (int j = 0; j < colorContainers.length; j++) {
                    if (j == index) {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                    } else {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.LTGRAY);
                    }
                }
                
                binding.multiPageCanvas.setPenColor(color);
            });
            
            // Add to first or second row (6 colors per row)
            if (i < 6) {
                colorRow1.addView(container);
            } else {
                colorRow2.addView(container);
            }
        }
        
        colorsContainer.addView(colorRow1);
        colorsContainer.addView(colorRow2);
        layout.addView(colorsContainer);
        
        // === STROKE WIDTH ===
        android.widget.TextView widthLabel = new android.widget.TextView(this);
        widthLabel.setText("Stroke Width:");
        widthLabel.setTextSize(16);
        widthLabel.setPadding(0, 20, 0, 10);
        layout.addView(widthLabel);
        
        android.widget.SeekBar widthSeekBar = new android.widget.SeekBar(this);
        widthSeekBar.setMax(20);
        widthSeekBar.setProgress((int) currentStrokeWidth); // Set to current value
        widthSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float width = Math.max(1, progress);
                currentStrokeWidth = width; // Update state
                binding.multiPageCanvas.setStrokeWidth(width);
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        layout.addView(widthSeekBar);
        
        builder.setView(layout);
        builder.setPositiveButton("Done", null);
        builder.show();
    }
    
    private void showShapeOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Create custom dialog layout with ScrollView for many shapes
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        scrollView.addView(layout);
        
        // Title
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Shape Options");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // === SHAPE SELECTION with visual highlighting (2xN grid) ===
        android.widget.TextView shapeLabel = new android.widget.TextView(this);
        shapeLabel.setText("Select Shape:");
        shapeLabel.setTextSize(16);
        shapeLabel.setPadding(0, 10, 0, 10);
        layout.addView(shapeLabel);
        
        CanvasView.ShapeType[] shapes = {
            CanvasView.ShapeType.RECTANGLE,
            CanvasView.ShapeType.CIRCLE,
            CanvasView.ShapeType.TRIANGLE,
            CanvasView.ShapeType.LINE,
            CanvasView.ShapeType.ARROW,
            CanvasView.ShapeType.PENTAGON,
            CanvasView.ShapeType.STAR,
            CanvasView.ShapeType.HEXAGON,
            CanvasView.ShapeType.DIAMOND,
            CanvasView.ShapeType.OVAL,
            CanvasView.ShapeType.RIGHT_TRIANGLE,
            CanvasView.ShapeType.PARALLELOGRAM,
            CanvasView.ShapeType.CROSS,
            CanvasView.ShapeType.HEART
        };
        
        String[] shapeNames = {"Rect", "Circle", "Tri", "Line", "Arrow", "Pent", "Star", "Hex",
                               "Diamond", "Oval", "RightTri", "Para", "Cross", "Heart"};
        
        // Create rows dynamically (2 shapes per row)
        android.widget.LinearLayout currentRow = null;
        android.widget.LinearLayout[] shapeContainers = new android.widget.LinearLayout[shapes.length];
        
        for (int i = 0; i < shapes.length; i++) {
            if (i % 2 == 0) {
                currentRow = new android.widget.LinearLayout(this);
                currentRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                if (i > 0) currentRow.setPadding(0, 8, 0, 0);
                layout.addView(currentRow);
            }
            
            final int index = i;
            final CanvasView.ShapeType shape = shapes[i];
            
            android.widget.LinearLayout shapeContainer = new android.widget.LinearLayout(this);
            shapeContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.LinearLayout.LayoutParams containerParams = new android.widget.LinearLayout.LayoutParams(0, 110, 1.0f);
            containerParams.setMargins(4, 4, 4, 4);
            shapeContainer.setLayoutParams(containerParams);
            shapeContainer.setPadding(6, 6, 6, 6);
            
            // Set selection based on current shape
            if (shape == currentShapeType) {
                shapeContainer.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
            } else {
                shapeContainer.setBackgroundColor(0xFFEEEEEE);
            }
            
            // Shape preview
            android.view.View shapeView = new android.view.View(this) {
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    super.onDraw(canvas);
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.BLACK);
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setAntiAlias(true);
                    
                    float w = getWidth();
                    float h = getHeight();
                    float centerX = w / 2;
                    float centerY = h / 2;
                    float size = Math.min(w, h) * 0.6f;
                    
                    android.graphics.Path path = new android.graphics.Path();
                    switch (shape) {
                        case RECTANGLE:
                            canvas.drawRect(centerX - size/2, centerY - size/2, centerX + size/2, centerY + size/2, paint);
                            break;
                        case CIRCLE:
                            canvas.drawCircle(centerX, centerY, size/2, paint);
                            break;
                        case TRIANGLE:
                            path.moveTo(centerX, centerY - size/2);
                            path.lineTo(centerX - size/2, centerY + size/2);
                            path.lineTo(centerX + size/2, centerY + size/2);
                            path.close();
                            canvas.drawPath(path, paint);
                            break;
                        case LINE:
                            canvas.drawLine(centerX - size/2, centerY, centerX + size/2, centerY, paint);
                            break;
                        case ARROW:
                            canvas.drawLine(centerX - size/2, centerY, centerX + size/2, centerY, paint);
                            path.moveTo(centerX + size/2, centerY);
                            path.lineTo(centerX + size/2 - 10, centerY - 7);
                            path.moveTo(centerX + size/2, centerY);
                            path.lineTo(centerX + size/2 - 10, centerY + 7);
                            canvas.drawPath(path, paint);
                            break;
                        case PENTAGON:
                            for (int j = 0; j <= 5; j++) {
                                float angle = (float)(Math.toRadians(-90 + j * 72));
                                float x = centerX + size/2 * (float)Math.cos(angle);
                                float y = centerY + size/2 * (float)Math.sin(angle);
                                if (j == 0) path.moveTo(x, y);
                                else path.lineTo(x, y);
                            }
                            canvas.drawPath(path, paint);
                            break;
                        case STAR:
                            for (int j = 0; j <= 10; j++) {
                                float r = (j % 2 == 0) ? size/2 : size/2 * 0.4f;
                                float angle = (float)(Math.toRadians(-90 + j * 36));
                                float x = centerX + r * (float)Math.cos(angle);
                                float y = centerY + r * (float)Math.sin(angle);
                                if (j == 0) path.moveTo(x, y);
                                else path.lineTo(x, y);
                            }
                            canvas.drawPath(path, paint);
                            break;
                        case HEXAGON:
                            for (int j = 0; j <= 6; j++) {
                                float angle = (float)(Math.toRadians(-90 + j * 60));
                                float x = centerX + size/2 * (float)Math.cos(angle);
                                float y = centerY + size/2 * (float)Math.sin(angle);
                                if (j == 0) path.moveTo(x, y);
                                else path.lineTo(x, y);
                            }
                            canvas.drawPath(path, paint);
                            break;
                        case DIAMOND:
                            path.moveTo(centerX, centerY - size/2);
                            path.lineTo(centerX + size/2, centerY);
                            path.lineTo(centerX, centerY + size/2);
                            path.lineTo(centerX - size/2, centerY);
                            path.close();
                            canvas.drawPath(path, paint);
                            break;
                        case OVAL:
                            android.graphics.RectF oval = new android.graphics.RectF(centerX - size/2, centerY - size/3, centerX + size/2, centerY + size/3);
                            canvas.drawOval(oval, paint);
                            break;
                        case RIGHT_TRIANGLE:
                            path.moveTo(centerX - size/2, centerY - size/2);
                            path.lineTo(centerX + size/2, centerY + size/2);
                            path.lineTo(centerX - size/2, centerY + size/2);
                            path.close();
                            canvas.drawPath(path, paint);
                            break;
                        case PARALLELOGRAM:
                            float offset = size * 0.3f;
                            path.moveTo(centerX - size/2 + offset, centerY - size/2);
                            path.lineTo(centerX + size/2, centerY - size/2);
                            path.lineTo(centerX + size/2 - offset, centerY + size/2);
                            path.lineTo(centerX - size/2, centerY + size/2);
                            path.close();
                            canvas.drawPath(path, paint);
                            break;
                        case CROSS:
                            canvas.drawLine(centerX, centerY - size/2, centerX, centerY + size/2, paint);
                            canvas.drawLine(centerX - size/2, centerY, centerX + size/2, centerY, paint);
                            break;
                        case HEART:
                            path.moveTo(centerX, centerY + size/3);
                            path.cubicTo(centerX - size/2, centerY - size/4, centerX - size/2, centerY + size/4, centerX, centerY + size/2);
                            path.cubicTo(centerX + size/2, centerY + size/4, centerX + size/2, centerY - size/4, centerX, centerY + size/3);
                            canvas.drawPath(path, paint);
                            break;
                    }
                }
            };
            android.widget.LinearLayout.LayoutParams shapeParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0);
            shapeParams.weight = 1;
            shapeView.setLayoutParams(shapeParams);
            
            // Shape name
            android.widget.TextView nameText = new android.widget.TextView(this);
            nameText.setText(shapeNames[i]);
            nameText.setTextSize(9);
            nameText.setGravity(android.view.Gravity.CENTER);
            nameText.setPadding(0, 2, 0, 0);
            
            shapeContainer.addView(shapeView);
            shapeContainer.addView(nameText);
            shapeContainers[index] = shapeContainer;
            
            shapeContainer.setOnClickListener(v -> {
                // Update state
                currentShapeType = shape;
                
                // Update all containers' background
                for (int j = 0; j < shapeContainers.length; j++) {
                    if (j == index) {
                        shapeContainers[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                    } else {
                        shapeContainers[j].setBackgroundColor(0xFFEEEEEE);
                    }
                }
                
                binding.multiPageCanvas.setShapeType(shape);
                setMode(CanvasView.Mode.SHAPE);
                highlightButton(binding.btnShapes);
            });
            
            currentRow.addView(shapeContainer);
        }
        
        // === LINE STYLE with RadioGroup (Normal, Dashed, Fill) ===
        android.widget.TextView lineLabel = new android.widget.TextView(this);
        lineLabel.setText("Style:");
        lineLabel.setTextSize(16);
        lineLabel.setPadding(0, 20, 0, 10);
        layout.addView(lineLabel);
        
        android.widget.RadioGroup lineStyleGroup = new android.widget.RadioGroup(this);
        lineStyleGroup.setOrientation(android.widget.RadioGroup.VERTICAL);
        
        android.widget.RadioButton normalLineBtn = new android.widget.RadioButton(this);
        normalLineBtn.setId(View.generateViewId());
        normalLineBtn.setText("━━━  Stroke (Normal)");
        normalLineBtn.setTextSize(14);
        normalLineBtn.setChecked(!isDashedShape && !isFilledShape);
        
        android.widget.RadioButton dashedLineBtn = new android.widget.RadioButton(this);
        dashedLineBtn.setId(View.generateViewId());
        dashedLineBtn.setText("- - -  Stroke (Dashed)");
        dashedLineBtn.setTextSize(14);
        dashedLineBtn.setChecked(isDashedShape && !isFilledShape);
        
        android.widget.RadioButton fillBtn = new android.widget.RadioButton(this);
        fillBtn.setId(View.generateViewId());
        fillBtn.setText("█████  Fill");
        fillBtn.setTextSize(14);
        fillBtn.setChecked(isFilledShape);
        
        lineStyleGroup.addView(normalLineBtn);
        lineStyleGroup.addView(dashedLineBtn);
        lineStyleGroup.addView(fillBtn);
        
        lineStyleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == normalLineBtn.getId()) {
                isDashedShape = false;
                isFilledShape = false;
                binding.multiPageCanvas.setDashedShape(false);
                binding.multiPageCanvas.setFilledShape(false);
            } else if (checkedId == dashedLineBtn.getId()) {
                isDashedShape = true;
                isFilledShape = false;
                binding.multiPageCanvas.setDashedShape(true);
                binding.multiPageCanvas.setFilledShape(false);
            } else if (checkedId == fillBtn.getId()) {
                isDashedShape = false;
                isFilledShape = true;
                binding.multiPageCanvas.setDashedShape(false);
                binding.multiPageCanvas.setFilledShape(true);
            }
        });
        
        layout.addView(lineStyleGroup);
        
        // === COLOR PICKER with selection border ===
        android.widget.TextView colorLabel = new android.widget.TextView(this);
        colorLabel.setText("Color:");
        colorLabel.setTextSize(16);
        colorLabel.setPadding(0, 20, 0, 10);
        layout.addView(colorLabel);
        
        int[] colors = {
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#424242"), // Dark Gray
            android.graphics.Color.BLUE,
            android.graphics.Color.parseColor("#2196F3"), // Light Blue
            android.graphics.Color.RED,
            android.graphics.Color.parseColor("#E91E63"), // Pink
            android.graphics.Color.GREEN,
            android.graphics.Color.parseColor("#4CAF50"), // Light Green
            android.graphics.Color.parseColor("#FF9800"), // Orange
            android.graphics.Color.parseColor("#FFEB3B"), // Yellow
            android.graphics.Color.parseColor("#9C27B0"), // Purple
            android.graphics.Color.parseColor("#795548")  // Brown
        };
        
        android.widget.LinearLayout colorsContainer = new android.widget.LinearLayout(this);
        colorsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        android.widget.LinearLayout colorRow1 = new android.widget.LinearLayout(this);
        colorRow1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        
        android.widget.LinearLayout colorRow2 = new android.widget.LinearLayout(this);
        colorRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow2.setPadding(0, 8, 0, 0);
        
        View[] colorContainers = new View[colors.length];
        
        for (int i = 0; i < colors.length; i++) {
            final int index = i;
            final int color = colors[i];
            
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                new android.widget.LinearLayout.LayoutParams(85, 85);
            containerParams.setMargins(8, 0, 8, 0);
            container.setLayoutParams(containerParams);
            container.setPadding(5, 5, 5, 5);
            
            // Set selection based on current color
            if (color == currentShapeColor) {
                container.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
            } else {
                container.setBackgroundColor(android.graphics.Color.LTGRAY);
            }
            
            View colorCircle = new View(this);
            colorCircle.setBackgroundColor(color);
            container.addView(colorCircle);
            
            colorContainers[index] = container;
            
            container.setOnClickListener(v -> {
                // Update state
                currentShapeColor = color;
                
                // Update all containers' border
                for (int j = 0; j < colorContainers.length; j++) {
                    if (j == index) {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                    } else {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.LTGRAY);
                    }
                }
                
                binding.multiPageCanvas.setShapeColor(color);
            });
            
            if (i < 6) {
                colorRow1.addView(container);
            } else {
                colorRow2.addView(container);
            }
        }
        
        colorsContainer.addView(colorRow1);
        colorsContainer.addView(colorRow2);
        layout.addView(colorsContainer);
        
        // === STROKE WIDTH ===
        android.widget.TextView widthLabel = new android.widget.TextView(this);
        widthLabel.setText("Stroke Width: " + (int)currentShapeStrokeWidth + "px");
        widthLabel.setTextSize(16);
        widthLabel.setPadding(0, 20, 0, 10);
        layout.addView(widthLabel);
        
        android.widget.SeekBar widthSeekBar = new android.widget.SeekBar(this);
        widthSeekBar.setMax(30);
        widthSeekBar.setProgress((int)currentShapeStrokeWidth - 1);
        widthSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float width = Math.max(1, progress + 1);
                currentShapeStrokeWidth = width;
                widthLabel.setText("Stroke Width: " + (int)width + "px");
                binding.multiPageCanvas.setShapeStrokeWidth(width);
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        layout.addView(widthSeekBar);
        
        builder.setView(scrollView);
        builder.setPositiveButton("Done", null);
        builder.show();
    }
    
    private void showEraserOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Create custom dialog layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        // Title
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Shape Options");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // === SHAPE SELECTION with visual highlighting ===
        android.widget.TextView shapeLabel = new android.widget.TextView(this);
        shapeLabel.setText("Select Shape:");
        shapeLabel.setTextSize(16);
        shapeLabel.setPadding(0, 10, 0, 10);
        layout.addView(shapeLabel);
        
        CanvasView.ShapeType[] shapes = {
            CanvasView.ShapeType.RECTANGLE,
            CanvasView.ShapeType.CIRCLE,
            CanvasView.ShapeType.TRIANGLE,
            CanvasView.ShapeType.LINE,
            CanvasView.ShapeType.ARROW,
            CanvasView.ShapeType.PENTAGON,
            CanvasView.ShapeType.STAR,
            CanvasView.ShapeType.HEXAGON
        };
        
        String[] shapeNames = {"Rect", "Circle", "Tri", "Line", "Arrow", "Pent", "Star", "Hex"};
        
        // Create 2 rows of shape buttons (4 per row)
        android.widget.LinearLayout row1 = new android.widget.LinearLayout(this);
        row1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        
        android.widget.LinearLayout row2 = new android.widget.LinearLayout(this);
        row2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row2.setPadding(0, 8, 0, 0);
        
        android.widget.LinearLayout[] shapeContainers = new android.widget.LinearLayout[shapes.length];
        
        for (int i = 0; i < shapes.length; i++) {
            final int index = i;
            final CanvasView.ShapeType shape = shapes[i];
            
            android.widget.LinearLayout shapeContainer = new android.widget.LinearLayout(this);
            shapeContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.LinearLayout.LayoutParams containerParams = new android.widget.LinearLayout.LayoutParams(0, 130, 1.0f);
            containerParams.setMargins(4, 4, 4, 4);
            shapeContainer.setLayoutParams(containerParams);
            shapeContainer.setPadding(6, 6, 6, 6);
            
            // Set selection based on current shape
            if (shape == currentShapeType) {
                shapeContainer.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
            } else {
                shapeContainer.setBackgroundColor(0xFFEEEEEE);
            }
            
            // Shape preview
            android.view.View shapeView = new android.view.View(this) {
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    super.onDraw(canvas);
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.BLACK);
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(3);
                    paint.setAntiAlias(true);
                    
                    float w = getWidth();
                    float h = getHeight();
                    float centerX = w / 2;
                    float centerY = h / 2;
                    float size = Math.min(w, h) * 0.65f;
                    
                    switch (shape) {
                        case RECTANGLE:
                            canvas.drawRect(centerX - size/2, centerY - size/2, 
                                          centerX + size/2, centerY + size/2, paint);
                            break;
                        case CIRCLE:
                            canvas.drawCircle(centerX, centerY, size/2, paint);
                            break;
                        case TRIANGLE:
                            android.graphics.Path triPath = new android.graphics.Path();
                            triPath.moveTo(centerX, centerY - size/2);
                            triPath.lineTo(centerX - size/2, centerY + size/2);
                            triPath.lineTo(centerX + size/2, centerY + size/2);
                            triPath.close();
                            canvas.drawPath(triPath, paint);
                            break;
                        case LINE:
                            canvas.drawLine(centerX - size/2, centerY, centerX + size/2, centerY, paint);
                            break;
                        case ARROW:
                            canvas.drawLine(centerX - size/2, centerY, centerX + size/2, centerY, paint);
                            android.graphics.Path arrowPath = new android.graphics.Path();
                            arrowPath.moveTo(centerX + size/2, centerY);
                            arrowPath.lineTo(centerX + size/2 - 12, centerY - 8);
                            arrowPath.moveTo(centerX + size/2, centerY);
                            arrowPath.lineTo(centerX + size/2 - 12, centerY + 8);
                            canvas.drawPath(arrowPath, paint);
                            break;
                        case PENTAGON:
                            android.graphics.Path pentPath = new android.graphics.Path();
                            float pentRadius = size / 2;
                            pentPath.moveTo(centerX + pentRadius * (float)Math.cos(Math.toRadians(-90)),
                                          centerY + pentRadius * (float)Math.sin(Math.toRadians(-90)));
                            for (int j = 1; j <= 5; j++) {
                                pentPath.lineTo(centerX + pentRadius * (float)Math.cos(Math.toRadians(-90 + j * 72)),
                                              centerY + pentRadius * (float)Math.sin(Math.toRadians(-90 + j * 72)));
                            }
                            pentPath.close();
                            canvas.drawPath(pentPath, paint);
                            break;
                        case STAR:
                            android.graphics.Path starPath = new android.graphics.Path();
                            float outerR = size / 2;
                            float innerR = outerR * 0.4f;
                            starPath.moveTo(centerX + outerR * (float)Math.cos(Math.toRadians(-90)),
                                          centerY + outerR * (float)Math.sin(Math.toRadians(-90)));
                            for (int j = 1; j <= 10; j++) {
                                float r = (j % 2 == 0) ? outerR : innerR;
                                starPath.lineTo(centerX + r * (float)Math.cos(Math.toRadians(-90 + j * 36)),
                                              centerY + r * (float)Math.sin(Math.toRadians(-90 + j * 36)));
                            }
                            starPath.close();
                            canvas.drawPath(starPath, paint);
                            break;
                        case HEXAGON:
                            android.graphics.Path hexPath = new android.graphics.Path();
                            float hexRadius = size / 2;
                            hexPath.moveTo(centerX + hexRadius * (float)Math.cos(Math.toRadians(-90)),
                                         centerY + hexRadius * (float)Math.sin(Math.toRadians(-90)));
                            for (int j = 1; j <= 6; j++) {
                                hexPath.lineTo(centerX + hexRadius * (float)Math.cos(Math.toRadians(-90 + j * 60)),
                                             centerY + hexRadius * (float)Math.sin(Math.toRadians(-90 + j * 60)));
                            }
                            hexPath.close();
                            canvas.drawPath(hexPath, paint);
                            break;
                    }
                }
            };
            android.widget.LinearLayout.LayoutParams shapeParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0);
            shapeParams.weight = 1;
            shapeView.setLayoutParams(shapeParams);
            
            // Shape name
            android.widget.TextView nameText = new android.widget.TextView(this);
            nameText.setText(shapeNames[i]);
            nameText.setTextSize(10);
            nameText.setGravity(android.view.Gravity.CENTER);
            nameText.setPadding(0, 3, 0, 0);
            
            shapeContainer.addView(shapeView);
            shapeContainer.addView(nameText);
            shapeContainers[index] = shapeContainer;
            
            shapeContainer.setOnClickListener(v -> {
                // Update state
                currentShapeType = shape;
                
                // Update all containers' background
                for (int j = 0; j < shapeContainers.length; j++) {
                    if (j == index) {
                        shapeContainers[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                    } else {
                        shapeContainers[j].setBackgroundColor(0xFFEEEEEE);
                    }
                }
                
                binding.multiPageCanvas.setShapeType(shape);
                setMode(CanvasView.Mode.SHAPE);
                highlightButton(binding.btnShapes);
            });
            
            if (i < 4) {
                row1.addView(shapeContainer);
            } else {
                row2.addView(shapeContainer);
            }
        }
        
        layout.addView(row1);
        layout.addView(row2);
        
        // === LINE STYLE with RadioGroup ===
        android.widget.TextView lineLabel = new android.widget.TextView(this);
        lineLabel.setText("Line Style:");
        lineLabel.setTextSize(16);
        lineLabel.setPadding(0, 20, 0, 10);
        layout.addView(lineLabel);
        
        android.widget.RadioGroup lineStyleGroup = new android.widget.RadioGroup(this);
        lineStyleGroup.setOrientation(android.widget.RadioGroup.HORIZONTAL);
        
        android.widget.RadioButton normalLineBtn = new android.widget.RadioButton(this);
        normalLineBtn.setId(View.generateViewId());
        normalLineBtn.setText("━━━  Normal");
        normalLineBtn.setTextSize(14);
        normalLineBtn.setChecked(!isDashedShape);
        android.widget.RadioGroup.LayoutParams radioParams1 = new android.widget.RadioGroup.LayoutParams(
            0, android.widget.RadioGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        radioParams1.setMargins(0, 0, 10, 0);
        normalLineBtn.setLayoutParams(radioParams1);
        
        android.widget.RadioButton dashedLineBtn = new android.widget.RadioButton(this);
        dashedLineBtn.setId(View.generateViewId());
        dashedLineBtn.setText("- - -  Dashed");
        dashedLineBtn.setTextSize(14);
        dashedLineBtn.setChecked(isDashedShape);
        android.widget.RadioGroup.LayoutParams radioParams2 = new android.widget.RadioGroup.LayoutParams(
            0, android.widget.RadioGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        dashedLineBtn.setLayoutParams(radioParams2);
        
        lineStyleGroup.addView(normalLineBtn);
        lineStyleGroup.addView(dashedLineBtn);
        
        lineStyleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == normalLineBtn.getId()) {
                isDashedShape = false;
                binding.multiPageCanvas.setDashedShape(false);
            } else if (checkedId == dashedLineBtn.getId()) {
                isDashedShape = true;
                binding.multiPageCanvas.setDashedShape(true);
            }
        });
        
        layout.addView(lineStyleGroup);
        
        // === COLOR PICKER with selection border ===
        android.widget.TextView colorLabel = new android.widget.TextView(this);
        colorLabel.setText("Color:");
        colorLabel.setTextSize(16);
        colorLabel.setPadding(0, 20, 0, 10);
        layout.addView(colorLabel);
        
        int[] colors = {
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#424242"), // Dark Gray
            android.graphics.Color.BLUE,
            android.graphics.Color.parseColor("#2196F3"), // Light Blue
            android.graphics.Color.RED,
            android.graphics.Color.parseColor("#E91E63"), // Pink
            android.graphics.Color.GREEN,
            android.graphics.Color.parseColor("#4CAF50"), // Light Green
            android.graphics.Color.parseColor("#FF9800"), // Orange
            android.graphics.Color.parseColor("#FFEB3B"), // Yellow
            android.graphics.Color.parseColor("#9C27B0"), // Purple
            android.graphics.Color.parseColor("#795548")  // Brown
        };
        
        android.widget.LinearLayout colorsContainer = new android.widget.LinearLayout(this);
        colorsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        android.widget.LinearLayout colorRow1 = new android.widget.LinearLayout(this);
        colorRow1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        
        android.widget.LinearLayout colorRow2 = new android.widget.LinearLayout(this);
        colorRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow2.setPadding(0, 8, 0, 0);
        
        View[] colorContainers = new View[colors.length];
        
        for (int i = 0; i < colors.length; i++) {
            final int index = i;
            final int color = colors[i];
            
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                new android.widget.LinearLayout.LayoutParams(85, 85);
            containerParams.setMargins(8, 0, 8, 0);
            container.setLayoutParams(containerParams);
            container.setPadding(5, 5, 5, 5);
            
            // Set selection based on current color
            if (color == currentShapeColor) {
                container.setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
            } else {
                container.setBackgroundColor(android.graphics.Color.LTGRAY);
            }
            
            View colorCircle = new View(this);
            colorCircle.setBackgroundColor(color);
            container.addView(colorCircle);
            
            colorContainers[index] = container;
            
            container.setOnClickListener(v -> {
                // Update state
                currentShapeColor = color;
                
                // Update all containers' border
                for (int j = 0; j < colorContainers.length; j++) {
                    if (j == index) {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.parseColor("#00A3FF"));
                    } else {
                        colorContainers[j].setBackgroundColor(android.graphics.Color.LTGRAY);
                    }
                }
                
                binding.multiPageCanvas.setShapeColor(color);
            });
            
            if (i < 6) {
                colorRow1.addView(container);
            } else {
                colorRow2.addView(container);
            }
        }
        
        colorsContainer.addView(colorRow1);
        colorsContainer.addView(colorRow2);
        layout.addView(colorsContainer);
        
        // === STROKE WIDTH ===
        android.widget.TextView widthLabel = new android.widget.TextView(this);
        widthLabel.setText("Stroke Width: " + (int)currentShapeStrokeWidth + "px");
        widthLabel.setTextSize(16);
        widthLabel.setPadding(0, 20, 0, 10);
        layout.addView(widthLabel);
        
        android.widget.SeekBar widthSeekBar = new android.widget.SeekBar(this);
        widthSeekBar.setMax(30);
        widthSeekBar.setProgress((int)currentShapeStrokeWidth - 1);
        widthSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float width = Math.max(1, progress + 1);
                currentShapeStrokeWidth = width;
                widthLabel.setText("Stroke Width: " + (int)width + "px");
                binding.multiPageCanvas.setShapeStrokeWidth(width);
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        layout.addView(widthSeekBar);
        
        builder.setView(layout);
        builder.setPositiveButton("Done", null);
        builder.show();
    }
    
    private void showLaserOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Laser Pointer");
        
        String[] laserModes = {"Dot", "Line"};
        builder.setItems(laserModes, (dialog, which) -> {
            binding.multiPageCanvas.setLaserMode(which == 0 ? CanvasView.LaserMode.DOT : CanvasView.LaserMode.LINE);
            setMode(CanvasView.Mode.LASER_POINTER);
            highlightButton(binding.btnLaser);
            Toast.makeText(this, "Laser: " + laserModes[which], Toast.LENGTH_SHORT).show();
        });
        
        builder.show();
    }
    
    private void showPhotoMenu() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Photo Options");
        
        // Get current canvas to check if image is selected
        CanvasView currentCanvas = binding.multiPageCanvas.getCurrentCanvas();
        boolean hasSelectedImage = currentCanvas != null && currentCanvas.hasSelectedImage();
        
        String[] options;
        if (hasSelectedImage) {
            options = new String[]{
                "📤 Upload Photo", 
                "🔄 Rotate...",
                "↔️ Flip Horizontal",
                "↕️ Flip Vertical",
                "🗑️ Delete Selected Photo"
            };
        } else {
            options = new String[]{"📤 Upload Photo"};
        }
        
        builder.setItems(options, (dialog, which) -> {
            if (!hasSelectedImage) {
                // Only "Upload Photo" is available
                if (which == 0) {
                    openImagePicker();
                }
            } else {
                // All options available
                switch (which) {
                    case 0: // Upload Photo
                        openImagePicker();
                        break;
                    case 1: // Rotate with slider
                        showPhotoRotationDialog(currentCanvas);
                        break;
                    case 2: // Flip Horizontal
                        if (currentCanvas.flipSelectedImageHorizontal()) {
                            hasUnsavedChanges = true;
                        }
                        break;
                    case 3: // Flip Vertical
                        if (currentCanvas.flipSelectedImageVertical()) {
                            hasUnsavedChanges = true;
                        }
                        break;
                    case 4: // Delete
                        if (currentCanvas.deleteSelectedImage()) {
                            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                            hasUnsavedChanges = true;
                        }
                        break;
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showVoiceMenu() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Voice Recording Options");
        
        // Get current canvas to check if voice recording is selected
        CanvasView currentCanvas = binding.multiPageCanvas.getCurrentCanvas();
        boolean hasSelectedVoice = currentCanvas != null && currentCanvas.hasSelectedVoice();
        
        String[] options;
        if (hasSelectedVoice) {
            options = new String[]{
                "🎙️ Record New Audio",
                "🗑️ Delete Selected Recording"
            };
        } else {
            options = new String[]{"🎙️ Record New Audio"};
        }
        
        builder.setItems(options, (dialog, which) -> {
            if (!hasSelectedVoice) {
                // Only "Record New Audio" is available
                if (which == 0) {
                    startVoiceRecording();
                }
            } else {
                // All options available
                switch (which) {
                    case 0: // Record New Audio
                        startVoiceRecording();
                        break;
                    case 1: // Delete
                        if (currentCanvas.deleteSelectedVoice()) {
                            Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
                            hasUnsavedChanges = true;
                        }
                        break;
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showPhotoRotationDialog(CanvasView canvas) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Create custom dialog layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        // Title
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Rotate Photo");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // Current rotation value display
        android.widget.TextView valueText = new android.widget.TextView(this);
        float currentRotation = canvas.getSelectedImageRotation();
        valueText.setText(String.format("Rotation: %.0f°", currentRotation));
        valueText.setTextSize(16);
        valueText.setPadding(0, 10, 0, 10);
        valueText.setGravity(android.view.Gravity.CENTER);
        layout.addView(valueText);
        
        // SeekBar for rotation (0-360)
        android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
        seekBar.setMax(360);
        seekBar.setProgress((int) currentRotation);
        seekBar.setPadding(0, 10, 0, 20);
        layout.addView(seekBar);
        
        // Update value text and apply rotation as user drags
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                valueText.setText(String.format("Rotation: %d°", progress));
                if (fromUser) {
                    canvas.setSelectedImageRotation(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                hasUnsavedChanges = true;
            }
        });
        
        builder.setView(layout);
        builder.setPositiveButton("Done", (dialog, which) -> {
            hasUnsavedChanges = true;
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Reset to original rotation
            canvas.setSelectedImageRotation(currentRotation);
        });
        builder.show();
    }
    
    private void showStickyNoteColorOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Sticky Note Color");
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        // Color grid (similar to pen color selection)
        int[] colors = {
            0xFFFFEB3B, // Yellow
            0xFFE91E63, // Pink
            0xFF2196F3, // Light Blue
            0xFF4CAF50, // Light Green
            0xFFFF9800, // Orange
            0xFF9C27B0  // Purple
        };
        
        android.widget.LinearLayout colorRow1 = new android.widget.LinearLayout(this);
        colorRow1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow1.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout colorRow2 = new android.widget.LinearLayout(this);
        colorRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow2.setGravity(android.view.Gravity.CENTER);
        
        for (int i = 0; i < colors.length; i++) {
            final int color = colors[i];
            android.widget.FrameLayout colorContainer = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = new android.widget.LinearLayout.LayoutParams(120, 120);
            containerParams.setMargins(10, 10, 10, 10);
            colorContainer.setLayoutParams(containerParams);
            
            // Set border
            colorContainer.setBackgroundColor(android.graphics.Color.LTGRAY);
            colorContainer.setPadding(4, 4, 4, 4);
            
            android.view.View colorView = new android.view.View(this);
            colorView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            colorView.setBackgroundColor(color);
            
            colorContainer.addView(colorView);
            colorContainer.setOnClickListener(v -> {
                binding.multiPageCanvas.setStickyNoteColor(color);
                setMode(CanvasView.Mode.STICKY_NOTE);
                highlightButton(binding.btnStickyNote);
                Toast.makeText(this, "Tap canvas to add sticky note", Toast.LENGTH_SHORT).show();
                ((android.app.AlertDialog)v.getTag()).dismiss();
            });
            
            colorContainer.setTag(builder.create());
            
            if (i < 3) {
                colorRow1.addView(colorContainer);
            } else {
                colorRow2.addView(colorContainer);
            }
        }
        
        layout.addView(colorRow1);
        layout.addView(colorRow2);
        
        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        android.app.AlertDialog dialog = builder.create();
        
        // Set the dialog reference for all color containers
        for (int i = 0; i < colorRow1.getChildCount(); i++) {
            colorRow1.getChildAt(i).setTag(dialog);
        }
        for (int i = 0; i < colorRow2.getChildCount(); i++) {
            colorRow2.getChildAt(i).setTag(dialog);
        }
        
        dialog.show();
    }
    
    private void showTextModeOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Text Options");
        
        String[] textOptions = {"Add Text Box", "Edit Text"};
        builder.setItems(textOptions, (dialog, which) -> {
            binding.multiPageCanvas.finishAllTextInputs();
            setMode(CanvasView.Mode.TEXT);
            highlightButton(binding.btnTextMode);
            
            if (which == 0) {
                // Add Text Box - create new text without edit mode
                binding.multiPageCanvas.setTextEditMode(false);
                Toast.makeText(this, "Tap on canvas to add text", Toast.LENGTH_SHORT).show();
            } else {
                // Edit Text - enable edit mode for selecting/moving/formatting
                binding.multiPageCanvas.setTextEditMode(true);
                Toast.makeText(this, "Tap existing text to edit/move/format", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.show();
    }
    
    private void showTextFormatting(CanvasView.TextElement textElement) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Text Options");
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        // Edit Text button
        com.google.android.material.button.MaterialButton btnEditText = new com.google.android.material.button.MaterialButton(this);
        btnEditText.setText("Edit Text");
        btnEditText.setTextSize(16);
        android.widget.LinearLayout.LayoutParams editTextParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        editTextParams.setMargins(0, 0, 0, 20);
        btnEditText.setLayoutParams(editTextParams);
        btnEditText.setBackgroundColor(0xFF00A3FF);
        btnEditText.setTextColor(android.graphics.Color.WHITE);
        btnEditText.setOnClickListener(v -> {
            // Close the dialog
            ((android.app.AlertDialog) v.getTag()).dismiss();
            // Open text editor with keyboard
            binding.multiPageCanvas.editSelectedText();
        });
        layout.addView(btnEditText);
        
        // Bold, Italic, Underline buttons
        android.widget.TextView styleLabel = new android.widget.TextView(this);
        styleLabel.setText("Text Style:");
        styleLabel.setPadding(0, 10, 0, 10);
        styleLabel.setTextSize(16);
        styleLabel.setTextColor(android.graphics.Color.DKGRAY);
        layout.addView(styleLabel);
        
        android.widget.LinearLayout styleButtonsRow = new android.widget.LinearLayout(this);
        styleButtonsRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        styleButtonsRow.setGravity(android.view.Gravity.CENTER);
        
        // Bold button
        com.google.android.material.button.MaterialButton btnBold = new com.google.android.material.button.MaterialButton(this);
        btnBold.setText("B");
        btnBold.setTextSize(18);
        android.widget.LinearLayout.LayoutParams boldParams = new android.widget.LinearLayout.LayoutParams(0, 120);
        boldParams.weight = 1;
        boldParams.setMargins(5, 5, 5, 5);
        btnBold.setLayoutParams(boldParams);
        btnBold.setBackgroundColor(textElement.isBold ? 0xFF00A3FF : 0xFFE8E8E8);
        btnBold.setTextColor(textElement.isBold ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        btnBold.setOnClickListener(v -> {
            textElement.isBold = !textElement.isBold;
            binding.multiPageCanvas.setTextBold(textElement.isBold);
            btnBold.setBackgroundColor(textElement.isBold ? 0xFF00A3FF : 0xFFE8E8E8);
            btnBold.setTextColor(textElement.isBold ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        });
        styleButtonsRow.addView(btnBold);
        
        // Italic button
        com.google.android.material.button.MaterialButton btnItalic = new com.google.android.material.button.MaterialButton(this);
        btnItalic.setText("I");
        btnItalic.setTextSize(18);
        android.widget.LinearLayout.LayoutParams italicParams = new android.widget.LinearLayout.LayoutParams(0, 120);
        italicParams.weight = 1;
        italicParams.setMargins(5, 5, 5, 5);
        btnItalic.setLayoutParams(italicParams);
        btnItalic.setBackgroundColor(textElement.isItalic ? 0xFF00A3FF : 0xFFE8E8E8);
        btnItalic.setTextColor(textElement.isItalic ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        btnItalic.setOnClickListener(v -> {
            textElement.isItalic = !textElement.isItalic;
            binding.multiPageCanvas.setTextItalic(textElement.isItalic);
            btnItalic.setBackgroundColor(textElement.isItalic ? 0xFF00A3FF : 0xFFE8E8E8);
            btnItalic.setTextColor(textElement.isItalic ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        });
        styleButtonsRow.addView(btnItalic);
        
        // Underline button
        com.google.android.material.button.MaterialButton btnUnderline = new com.google.android.material.button.MaterialButton(this);
        btnUnderline.setText("U");
        btnUnderline.setTextSize(18);
        android.widget.LinearLayout.LayoutParams underlineParams = new android.widget.LinearLayout.LayoutParams(0, 120);
        underlineParams.weight = 1;
        underlineParams.setMargins(5, 5, 5, 5);
        btnUnderline.setLayoutParams(underlineParams);
        btnUnderline.setBackgroundColor(textElement.isUnderline ? 0xFF00A3FF : 0xFFE8E8E8);
        btnUnderline.setTextColor(textElement.isUnderline ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        btnUnderline.setOnClickListener(v -> {
            textElement.isUnderline = !textElement.isUnderline;
            binding.multiPageCanvas.setTextUnderline(textElement.isUnderline);
            btnUnderline.setBackgroundColor(textElement.isUnderline ? 0xFF00A3FF : 0xFFE8E8E8);
            btnUnderline.setTextColor(textElement.isUnderline ? android.graphics.Color.WHITE : android.graphics.Color.DKGRAY);
        });
        styleButtonsRow.addView(btnUnderline);
        
        layout.addView(styleButtonsRow);
        
        // Text Size
        android.widget.TextView sizeLabel = new android.widget.TextView(this);
        sizeLabel.setText("Text Size: " + (int)textElement.textSize + "sp");
        sizeLabel.setPadding(0, 20, 0, 10);
        sizeLabel.setTextSize(16);
        sizeLabel.setTextColor(android.graphics.Color.DKGRAY);
        layout.addView(sizeLabel);
        
        android.widget.SeekBar sizeSeekBar = new android.widget.SeekBar(this);
        sizeSeekBar.setMax(100);
        sizeSeekBar.setProgress((int)(textElement.textSize));
        sizeSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float size = Math.max(10, progress); // Min size 10
                sizeLabel.setText("Text Size: " + (int)size + "sp");
                binding.multiPageCanvas.setTextSize(size);
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        layout.addView(sizeSeekBar);
        
        // Color picker
        android.widget.TextView colorLabel = new android.widget.TextView(this);
        colorLabel.setText("Text Color:");
        colorLabel.setPadding(0, 20, 0, 10);
        colorLabel.setTextSize(16);
        colorLabel.setTextColor(android.graphics.Color.DKGRAY);
        layout.addView(colorLabel);
        
        // Color buttons (using same colors as pen options)
        int[] colors = {
            android.graphics.Color.BLACK,
            0xFF424242, // DARK_GRAY
            android.graphics.Color.BLUE,
            0xFF2196F3, // LIGHT_BLUE
            android.graphics.Color.RED,
            0xFFE91E63, // PINK
            android.graphics.Color.GREEN,
            0xFF4CAF50, // LIGHT_GREEN
            0xFFFF9800, // ORANGE
            0xFFFFEB3B, // YELLOW
            0xFF9C27B0, // PURPLE
            0xFF795548  // BROWN
        };
        
        android.widget.LinearLayout colorRow1 = new android.widget.LinearLayout(this);
        colorRow1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow1.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout colorRow2 = new android.widget.LinearLayout(this);
        colorRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorRow2.setGravity(android.view.Gravity.CENTER);
        
        for (int i = 0; i < colors.length; i++) {
            final int color = colors[i];
            android.widget.FrameLayout colorContainer = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = new android.widget.LinearLayout.LayoutParams(80, 80);
            containerParams.setMargins(5, 5, 5, 5);
            colorContainer.setLayoutParams(containerParams);
            
            // Set border - blue if selected, gray otherwise
            if (color == textElement.textColor) {
                colorContainer.setBackgroundColor(0xFF00A3FF);
            } else {
                colorContainer.setBackgroundColor(android.graphics.Color.LTGRAY);
            }
            colorContainer.setPadding(4, 4, 4, 4);
            
            android.view.View colorView = new android.view.View(this);
            colorView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            colorView.setBackgroundColor(color);
            
            colorContainer.addView(colorView);
            colorContainer.setOnClickListener(v -> {
                textElement.textColor = color;
                binding.multiPageCanvas.setTextColor(color);
                // Update all borders
                for (int j = 0; j < colorRow1.getChildCount(); j++) {
                    android.widget.FrameLayout container = (android.widget.FrameLayout) colorRow1.getChildAt(j);
                    container.setBackgroundColor(android.graphics.Color.LTGRAY);
                }
                for (int j = 0; j < colorRow2.getChildCount(); j++) {
                    android.widget.FrameLayout container = (android.widget.FrameLayout) colorRow2.getChildAt(j);
                    container.setBackgroundColor(android.graphics.Color.LTGRAY);
                }
                colorContainer.setBackgroundColor(0xFF00A3FF);
            });
            
            if (i < 6) {
                colorRow1.addView(colorContainer);
            } else {
                colorRow2.addView(colorContainer);
            }
        }
        
        layout.addView(colorRow1);
        layout.addView(colorRow2);
        
        builder.setView(layout);
        builder.setPositiveButton("Done", null);
        android.app.AlertDialog dialog = builder.show();
        
        // Store dialog reference in Edit Text button tag
        btnEditText.setTag(dialog);
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
        int actualPageCount = binding.multiPageCanvas.getPageCount();

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
                actualPageCount + " page" + 
                (actualPageCount > 1 ? "s" : "") + ")", 
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
    
    // Image Picker Implementation
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_READ_STORAGE = 1002;
    
    private void openImagePicker() {
        // Check for permission on Android 13+ or legacy
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_READ_STORAGE);
                return;
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_STORAGE);
                return;
            }
        }
        
        // Launch image picker
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    // Voice Recording Implementation
    private static final int PERMISSION_RECORD_AUDIO = 1003;
    private android.media.MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private android.app.AlertDialog voiceRecorderDialog;
    private android.widget.TextView tvTimer;
    private android.widget.TextView tvRecordingStatus;
    private com.google.android.material.button.MaterialButton btnStart, btnPause, btnStop, btnDelete;
    private long recordingStartTime = 0;
    private long pausedDuration = 0;
    private long totalRecordedTime = 0; // Track cumulative recording time
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && !isPaused) {
                // Calculate time: total previous segments + current segment
                long currentSegment = System.currentTimeMillis() - recordingStartTime;
                long totalTime = totalRecordedTime + currentSegment;
                int seconds = (int) (totalTime / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 100);
            }
        }
    };
    
    private void startVoiceRecording() {
        // Check for permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
            return;
        }
        
        showVoiceRecorderDialog();
    }
    
    private void showVoiceRecorderDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_voice_recorder, null);
        
        // Initialize views
        tvTimer = dialogView.findViewById(R.id.tvTimer);
        tvRecordingStatus = dialogView.findViewById(R.id.tvRecordingStatus);
        btnStart = dialogView.findViewById(R.id.btnStart);
        btnPause = dialogView.findViewById(R.id.btnPause);
        btnStop = dialogView.findViewById(R.id.btnStop);
        btnDelete = dialogView.findViewById(R.id.btnDelete);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Start button
        btnStart.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else if (isPaused) {
                resumeRecording();
            }
        });
        
        // Pause button
        btnPause.setOnClickListener(v -> pauseRecording());
        
        // Stop button
        btnStop.setOnClickListener(v -> {
            stopRecording();
            if (voiceRecorderDialog != null) {
                voiceRecorderDialog.dismiss();
            }
        });
        
        // Delete button
        btnDelete.setOnClickListener(v -> {
            deleteRecording();
            if (voiceRecorderDialog != null) {
                voiceRecorderDialog.dismiss();
            }
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> {
            // Just close the dialog without recording
            if (isRecording) {
                deleteRecording();
            }
            if (voiceRecorderDialog != null) {
                voiceRecorderDialog.dismiss();
            }
        });
        
        builder.setView(dialogView);
        builder.setCancelable(false);
        voiceRecorderDialog = builder.create();
        voiceRecorderDialog.show();
    }
    
    private void startRecording() {
        try {
            // Create file for recording
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            audioFilePath = getExternalFilesDir(null) + "/voice_" + timeStamp + ".3gp";
            
            mediaRecorder = new android.media.MediaRecorder();
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            pausedDuration = 0;
            totalRecordedTime = 0;
            
            // Update UI
            tvRecordingStatus.setText("🎤 Recording...");
            btnStart.setVisibility(android.view.View.GONE);
            btnPause.setVisibility(android.view.View.VISIBLE);
            btnStop.setVisibility(android.view.View.VISIBLE);
            btnDelete.setVisibility(android.view.View.VISIBLE);
            
            // Start timer
            timerHandler.postDelayed(timerRunnable, 0);
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void pauseRecording() {
        if (isRecording && !isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                mediaRecorder.pause();
                isPaused = true;
                
                // Calculate time recorded in this segment
                long currentSegment = System.currentTimeMillis() - recordingStartTime;
                totalRecordedTime += currentSegment;
                
                // Update UI
                tvRecordingStatus.setText("⏸ Paused");
                btnPause.setVisibility(android.view.View.GONE);
                btnStart.setVisibility(android.view.View.VISIBLE);
                btnStart.setText("RESUME");
                
                timerHandler.removeCallbacks(timerRunnable);
            } catch (Exception e) {
                Toast.makeText(this, "❌ Failed to pause", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void resumeRecording() {
        if (isRecording && isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                mediaRecorder.resume();
                isPaused = false;
                recordingStartTime = System.currentTimeMillis(); // Reset start time for this segment
                
                // Update UI
                tvRecordingStatus.setText("🎤 Recording...");
                btnStart.setVisibility(android.view.View.GONE);
                btnPause.setVisibility(android.view.View.VISIBLE);
                
                // Resume timer
                timerHandler.postDelayed(timerRunnable, 0);
            } catch (Exception e) {
                Toast.makeText(this, "❌ Failed to resume", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                if (isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    mediaRecorder.resume(); // Resume before stopping
                } else if (!isPaused) {
                    // Add the final recording segment
                    long currentSegment = System.currentTimeMillis() - recordingStartTime;
                    totalRecordedTime += currentSegment;
                }
                
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                // Use total recorded time as duration
                long duration = totalRecordedTime;
                
                isRecording = false;
                isPaused = false;
                
                timerHandler.removeCallbacks(timerRunnable);
                
                // Add voice recording to current canvas page
                CanvasView currentCanvas = binding.multiPageCanvas.getCurrentCanvas();
                if (currentCanvas != null && audioFilePath != null) {
                    currentCanvas.addVoiceRecording(audioFilePath, duration);
                    hasUnsavedChanges = true;
                    Toast.makeText(this, "✅ Recording added to page", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "✅ Recording saved: " + audioFilePath, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "❌ Failed to stop recording", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void deleteRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                if (isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    mediaRecorder.resume();
                }
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception ignored) {}
        }
        
        // Delete file if exists
        if (audioFilePath != null) {
            java.io.File file = new java.io.File(audioFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
        
        isRecording = false;
        isPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        Toast.makeText(this, "🗑️ Recording deleted", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_READ_STORAGE) {
                openImagePicker();
            } else if (requestCode == PERMISSION_RECORD_AUDIO) {
                startVoiceRecording();
            }
        } else {
            String message = requestCode == PERMISSION_READ_STORAGE ? 
                "Storage permission denied. Cannot access images." :
                "Microphone permission denied. Cannot record audio.";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            android.net.Uri imageUri = data.getData();
            if (imageUri != null) {
                // Add image to canvas
                binding.multiPageCanvas.addImage(imageUri);
                Toast.makeText(this, "✅ Image added to canvas", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
