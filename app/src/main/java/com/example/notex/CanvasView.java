package com.example.notex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * CanvasView - Custom view for drawing and placing content
 */
public class CanvasView extends FrameLayout {

    public enum Mode {
        TEXT, DRAW, PHOTO, VOICE, SCROLL
    }

    private Mode currentMode = Mode.TEXT;
    private Paint drawPaint;
    private Paint textPaint;
    private Path currentPath;
    private List<PathData> paths;
    private List<TextElement> textElements;
    private DrawView drawView;
    private EditText activeTextEdit;
    private TextElement editingTextElement = null; // Track which text is being edited
    private TextElement selectedTextElement = null; // Track selected text for drag/resize
    private boolean isDraggingText = false;
    private float dragStartX, dragStartY;
    private android.widget.LinearLayout textActionButtons;
    private GestureDetector gestureDetector;
    
    // Text manipulation states
    private enum TextEditState {
        NONE,           // No text selected
        SELECTED,       // Text selected, showing options
        MOVING,         // Actively moving text
        RESIZING,       // Actively resizing text
        ROTATING        // Actively rotating text
    }
    private TextEditState textEditState = TextEditState.NONE;
    private float initialTouchX, initialTouchY;
    private float initialTextX, initialTextY;
    private float initialTextSize = 40f;
    private float initialRotation = 0f;
    private int resizeCorner = -1; // -1: none, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Drawing paint
        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5f);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        // Text paint
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);

        paths = new ArrayList<>();
        textElements = new ArrayList<>();

        // Add draw view
        drawView = new DrawView(getContext());
        addView(drawView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Setup gesture detector for long press
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (currentMode == Mode.TEXT && selectedTextElement != null) {
                    // Long press on selected text opens edit mode
                    editTextElement(selectedTextElement);
                }
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentMode == Mode.TEXT) {
                    TextElement tappedText = findTextAtPosition(e.getX(), e.getY());
                    if (tappedText != null) {
                        // Double tap to edit
                        editTextElement(tappedText);
                        return true;
                    }
                }
                return false;
            }
        });
        
        // Create action buttons (Edit and Delete)
        createActionButtons();
    }
    
    private void createActionButtons() {
        textActionButtons = new android.widget.LinearLayout(getContext());
        textActionButtons.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        textActionButtons.setBackgroundColor(0xEEFFFFFF);
        textActionButtons.setElevation(8f);
        textActionButtons.setPadding(12, 12, 12, 12);
        textActionButtons.setVisibility(View.GONE);
        
        // Move button - enters continuous move mode
        android.widget.Button moveBtn = new android.widget.Button(getContext());
        moveBtn.setText("✋ Move");
        moveBtn.setTextSize(12);
        moveBtn.setPadding(16, 8, 16, 8);
        moveBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && textEditState == TextEditState.SELECTED) {
                textEditState = TextEditState.MOVING;
                hideActionButtons();
                drawView.invalidate();
                // Show toast to inform user
                android.widget.Toast.makeText(getContext(), "Move mode: Drag to move. Tap Done when finished.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Resize button - enters continuous resize mode
        android.widget.Button resizeBtn = new android.widget.Button(getContext());
        resizeBtn.setText("↔️ Resize");
        resizeBtn.setTextSize(12);
        resizeBtn.setPadding(16, 8, 16, 8);
        resizeBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && textEditState == TextEditState.SELECTED) {
                textEditState = TextEditState.RESIZING;
                hideActionButtons();
                drawView.invalidate();
                android.widget.Toast.makeText(getContext(), "Resize mode: Drag up/down. Tap Done when finished.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Rotate button - enters continuous rotate mode
        android.widget.Button rotateBtn = new android.widget.Button(getContext());
        rotateBtn.setText("🔄 Rotate");
        rotateBtn.setTextSize(12);
        rotateBtn.setPadding(16, 8, 16, 8);
        rotateBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && textEditState == TextEditState.SELECTED) {
                textEditState = TextEditState.ROTATING;
                hideActionButtons();
                drawView.invalidate();
                android.widget.Toast.makeText(getContext(), "Rotate mode: Drag to rotate. Tap Done when finished.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Edit button
        android.widget.Button editBtn = new android.widget.Button(getContext());
        editBtn.setText("✏️ Edit");
        editBtn.setTextSize(12);
        editBtn.setPadding(16, 8, 16, 8);
        editBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && textEditState == TextEditState.SELECTED) {
                editTextElement(selectedTextElement);
                selectedTextElement = null;
                textEditState = TextEditState.NONE;
                hideActionButtons();
            }
        });
        
        // Delete button
        android.widget.Button deleteBtn = new android.widget.Button(getContext());
        deleteBtn.setText("🗑️ Delete");
        deleteBtn.setTextSize(12);
        deleteBtn.setPadding(16, 8, 16, 8);
        deleteBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && textEditState == TextEditState.SELECTED) {
                textElements.remove(selectedTextElement);
                selectedTextElement = null;
                textEditState = TextEditState.NONE;
                hideActionButtons();
                drawView.invalidate();
            }
        });
        
        // Done button - exits move/resize/rotate mode
        android.widget.Button doneBtn = new android.widget.Button(getContext());
        doneBtn.setText("✓ Done");
        doneBtn.setTextSize(12);
        doneBtn.setPadding(16, 8, 16, 8);
        doneBtn.setOnClickListener(v -> {
            if (selectedTextElement != null && 
                (textEditState == TextEditState.MOVING || 
                 textEditState == TextEditState.RESIZING || 
                 textEditState == TextEditState.ROTATING)) {
                textEditState = TextEditState.SELECTED;
                showActionButtons();
                drawView.invalidate();
            }
        });
        
        textActionButtons.addView(moveBtn);
        textActionButtons.addView(resizeBtn);
        textActionButtons.addView(rotateBtn);
        textActionButtons.addView(editBtn);
        textActionButtons.addView(deleteBtn);
        textActionButtons.addView(doneBtn);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(textActionButtons, params);
    }
    
    private void showActionButtons() {
        if (selectedTextElement != null && textActionButtons != null) {
            // Position buttons near selected text
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textActionButtons.getLayoutParams();
            params.leftMargin = (int) selectedTextElement.x;
            params.topMargin = (int) (selectedTextElement.y + 40); // Below text
            textActionButtons.setLayoutParams(params);
            
            // Show buttons only in SELECTED state or active manipulation modes
            if (textEditState == TextEditState.SELECTED) {
                textActionButtons.setVisibility(View.VISIBLE);
            } else if (textEditState == TextEditState.MOVING || 
                       textEditState == TextEditState.RESIZING || 
                       textEditState == TextEditState.ROTATING) {
                // Show only Done button during active manipulation
                textActionButtons.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void hideActionButtons() {
        if (textActionButtons != null) {
            textActionButtons.setVisibility(View.GONE);
        }
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
    }

    public void addTextAtPosition(float x, float y) {
        // Finish any existing text input first (without duplication)
        finishTextInput();

        // Create inline EditText at tapped position
        activeTextEdit = new EditText(getContext());
        activeTextEdit.setBackgroundColor(Color.TRANSPARENT);
        activeTextEdit.setTextColor(Color.BLACK);
        activeTextEdit.setTextSize(16);
        activeTextEdit.setGravity(Gravity.TOP | Gravity.START);
        activeTextEdit.setHint("Type here...");
        activeTextEdit.setSingleLine(false);
        activeTextEdit.setMinWidth(200);
        activeTextEdit.setPadding(8, 8, 8, 8);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) x;
        params.topMargin = (int) y;

        addView(activeTextEdit, params);
        activeTextEdit.requestFocus();

        // Show keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(activeTextEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void finishTextInput() {
        if (activeTextEdit != null) {
            String text = activeTextEdit.getText().toString().trim();
            if (!text.isEmpty()) {
                // Get exact position where text was placed
                float x = ((FrameLayout.LayoutParams) activeTextEdit.getLayoutParams()).leftMargin;
                float y = ((FrameLayout.LayoutParams) activeTextEdit.getLayoutParams()).topMargin;
                
                // Add baseline offset for proper text rendering position
                y += activeTextEdit.getBaseline();
                
                TextElement newElement = new TextElement(text, x, y);
                // Preserve text size if editing existing element
                if (editingTextElement != null) {
                    newElement.textSize = editingTextElement.textSize;
                    newElement.rotation = editingTextElement.rotation;
                }
                textElements.add(newElement);
            }
            removeView(activeTextEdit);
            activeTextEdit = null;
            editingTextElement = null; // Clear editing reference
            drawView.invalidate();

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    public void clearCanvas() {
        paths.clear();
        textElements.clear();
        if (activeTextEdit != null) {
            removeView(activeTextEdit);
            activeTextEdit = null;
        }
        drawView.invalidate();
    }

    // Serialize canvas content to JSON
    public String toJson() {
        try {
            JSONObject json = new JSONObject();

            // Save paths
            JSONArray pathsArray = new JSONArray();
            for (PathData pathData : paths) {
                JSONObject pathObj = new JSONObject();
                pathObj.put("points", new JSONArray(pathData.points));
                pathsArray.put(pathObj);
            }
            json.put("paths", pathsArray);

            // Save text elements
            JSONArray textsArray = new JSONArray();
            for (TextElement element : textElements) {
                JSONObject textObj = new JSONObject();
                textObj.put("text", element.text);
                textObj.put("x", element.x);
                textObj.put("y", element.y);                textObj.put("textSize", element.textSize);
                textObj.put("rotation", element.rotation);                textsArray.put(textObj);
            }
            json.put("texts", textsArray);

            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // Load canvas content from JSON
    public void fromJson(String jsonString) {
        try {
            if (jsonString == null || jsonString.isEmpty() || jsonString.equals("Canvas content")) {
                return;
            }

            JSONObject json = new JSONObject(jsonString);

            // Load paths
            if (json.has("paths")) {
                JSONArray pathsArray = json.getJSONArray("paths");
                for (int i = 0; i < pathsArray.length(); i++) {
                    JSONObject pathObj = pathsArray.getJSONObject(i);
                    JSONArray pointsArray = pathObj.getJSONArray("points");

                    List<Float> points = new ArrayList<>();
                    for (int j = 0; j < pointsArray.length(); j++) {
                        points.add((float) pointsArray.getDouble(j));
                    }

                    if (points.size() >= 2) {
                        PathData pathData = new PathData();
                        pathData.points = points;
                        paths.add(pathData);
                    }
                }
            }

            // Load text elements
            if (json.has("texts")) {
                JSONArray textsArray = json.getJSONArray("texts");
                for (int i = 0; i < textsArray.length(); i++) {
                    JSONObject textObj = textsArray.getJSONObject(i);
                    String text = textObj.getString("text");
                    float x = (float) textObj.getDouble("x");
                    float y = (float) textObj.getDouble("y");
                    TextElement element = new TextElement(text, x, y);
                    
                    // Load optional properties
                    if (textObj.has("textSize")) {
                        element.textSize = (float) textObj.getDouble("textSize");
                    }
                    if (textObj.has("rotation")) {
                        element.rotation = (float) textObj.getDouble("rotation");
                    }
                    
                    textElements.add(element);
                }
            }

            drawView.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inner DrawView class for rendering
    private class DrawView extends View {
        public DrawView(Context context) {
            super(context);
            setBackgroundColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Draw all paths (drawings)
            for (PathData pathData : paths) {
                Path path = new Path();
                List<Float> points = pathData.points;
                if (points.size() >= 2) {
                    path.moveTo(points.get(0), points.get(1));
                    for (int i = 2; i < points.size(); i += 2) {
                        if (i + 1 < points.size()) {
                            path.lineTo(points.get(i), points.get(i + 1));
                        }
                    }
                    canvas.drawPath(path, drawPaint);
                }
            }

            // Draw current path
            if (currentPath != null) {
                canvas.drawPath(currentPath, drawPaint);
            }

            // Draw all text elements with multi-line support
            for (TextElement element : textElements) {
                canvas.save();
                
                // Apply rotation and custom text size
                canvas.rotate(element.rotation, element.x, element.y);
                
                // Set custom text size
                Paint customTextPaint = new Paint(textPaint);
                customTextPaint.setTextSize(element.textSize);
                
                // Handle multi-line text
                String[] lines = element.text.split("\n");
                float currentY = element.y;
                float lineHeight = customTextPaint.descent() - customTextPaint.ascent();
                
                for (String line : lines) {
                    canvas.drawText(line, element.x, currentY, customTextPaint);
                    currentY += lineHeight;
                }
                
                canvas.restore();
                
                // Draw selection box if this text is selected
                if (element == selectedTextElement) {
                    Paint selectionPaint = new Paint();
                    selectionPaint.setColor(0xFF2196F3); // Blue
                    selectionPaint.setStyle(Paint.Style.STROKE);
                    selectionPaint.setStrokeWidth(3f);
                    
                    // Calculate bounds with custom text size
                    TextBounds bounds = calculateTextBounds(element);
                    
                    float padding = 8;
                    float left = bounds.left - padding;
                    float top = bounds.top - padding;
                    float right = bounds.right + padding;
                    float bottom = bounds.bottom + padding;
                    
                    // Draw selection rectangle
                    canvas.drawRect(left, top, right, bottom, selectionPaint);
                    
                    // Draw corner handles based on current state
                    if (textEditState == TextEditState.SELECTED || textEditState == TextEditState.RESIZING) {
                        Paint handlePaint = new Paint();
                        handlePaint.setColor(textEditState == TextEditState.RESIZING ? 0xFFFF9800 : 0xFF2196F3);
                        handlePaint.setStyle(Paint.Style.FILL);
                        float handleSize = 15;
                        
                        // Top-left handle
                        canvas.drawCircle(left, top, handleSize, handlePaint);
                        // Top-right handle
                        canvas.drawCircle(right, top, handleSize, handlePaint);
                        // Bottom-left handle
                        canvas.drawCircle(left, bottom, handleSize, handlePaint);
                        // Bottom-right handle
                        canvas.drawCircle(right, bottom, handleSize, handlePaint);
                    }
                    
                    // Draw rotation indicator
                    if (textEditState == TextEditState.SELECTED || textEditState == TextEditState.ROTATING) {
                        Paint rotatePaint = new Paint();
                        rotatePaint.setColor(textEditState == TextEditState.ROTATING ? 0xFF4CAF50 : 0xFF2196F3);
                        rotatePaint.setStyle(Paint.Style.FILL);
                        float rotateHandleSize = 12;
                        
                        // Draw rotation handle at top center
                        float centerX = (left + right) / 2;
                        canvas.drawCircle(centerX, top - 30, rotateHandleSize, rotatePaint);
                        // Draw line to rotation handle
                        rotatePaint.setStyle(Paint.Style.STROKE);
                        rotatePaint.setStrokeWidth(2f);
                        canvas.drawLine(centerX, top, centerX, top - 30, rotatePaint);
                    }
                    
                    // Draw state indicator text
                    if (textEditState == TextEditState.MOVING) {
                        Paint statePaint = new Paint();
                        statePaint.setColor(0xFF4CAF50);
                        statePaint.setTextSize(14);
                        statePaint.setAntiAlias(true);
                        canvas.drawText("MOVING - Drag to move, tap Done to finish", left, top - 10, statePaint);
                    } else if (textEditState == TextEditState.RESIZING) {
                        Paint statePaint = new Paint();
                        statePaint.setColor(0xFFFF9800);
                        statePaint.setTextSize(14);
                        statePaint.setAntiAlias(true);
                        canvas.drawText("RESIZING - Drag up/down, tap Done to finish", left, top - 10, statePaint);
                    } else if (textEditState == TextEditState.ROTATING) {
                        Paint statePaint = new Paint();
                        statePaint.setColor(0xFF9C27B0);
                        statePaint.setTextSize(14);
                        statePaint.setAntiAlias(true);
                        canvas.drawText("ROTATING - Drag to rotate, tap Done to finish", left, top - 10, statePaint);
                    }
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (currentMode != Mode.DRAW) {
                return false;
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Request parent to not intercept touch events while drawing
                    getParent().requestDisallowInterceptTouchEvent(true);
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    // Start new path data
                    PathData pathData = new PathData();
                    pathData.addPoint(x, y);
                    paths.add(pathData);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (currentPath != null && paths.size() > 0) {
                        currentPath.lineTo(x, y);
                        // Add point to current path data
                        paths.get(paths.size() - 1).addPoint(x, y);
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Allow parent to intercept touch events again
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (currentPath != null && paths.size() > 0) {
                        // Add final point
                        paths.get(paths.size() - 1).addPoint(x, y);
                        currentPath = null;
                        invalidate();
                    }
                    return true;
            }

            return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass to gesture detector first
        if (currentMode == Mode.TEXT) {
            gestureDetector.onTouchEvent(event);
        }
        
        if (currentMode == Mode.TEXT) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = x;
                    initialTouchY = y;
                    
                    if (textEditState == TextEditState.MOVING) {
                        // Continue moving in move mode
                        if (selectedTextElement != null) {
                            initialTextX = selectedTextElement.x;
                            initialTextY = selectedTextElement.y;
                            return true;
                        }
                    } else if (textEditState == TextEditState.RESIZING) {
                        // Continue resizing in resize mode
                        if (selectedTextElement != null) {
                            initialTextSize = selectedTextElement.textSize;
                            // Any touch in resize mode resizes
                            resizeCorner = 3; // Use bottom-right for simplicity
                            return true;
                        }
                    } else if (textEditState == TextEditState.ROTATING) {
                        // Continue rotating in rotate mode
                        if (selectedTextElement != null) {
                            initialRotation = selectedTextElement.rotation;
                            return true;
                        }
                    } else {
                        // Normal tap - select or deselect
                        TextElement tappedText = findTextAtPosition(x, y);
                        if (tappedText != null) {
                            // Deselect all other text boxes
                            selectedTextElement = tappedText;
                            textEditState = TextEditState.SELECTED;
                            showActionButtons();
                            drawView.invalidate();
                            return true;
                        } else {
                            // Deselect if tapping empty area
                            if (selectedTextElement != null) {
                                selectedTextElement = null;
                                textEditState = TextEditState.NONE;
                                hideActionButtons();
                                drawView.invalidate();
                                return true;
                            } else {
                                // Add new text only if nothing is selected
                                addTextAtPosition(x, y);
                                return true;
                            }
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (textEditState == TextEditState.MOVING && selectedTextElement != null) {
                        // Move the text
                        float dx = x - initialTouchX;
                        float dy = y - initialTouchY;
                        selectedTextElement.x = initialTextX + dx;
                        selectedTextElement.y = initialTextY + dy;
                        drawView.invalidate();
                        return true;
                    } else if (textEditState == TextEditState.RESIZING && selectedTextElement != null && resizeCorner >= 0) {
                        // Resize the text
                        float distance = (float) Math.sqrt(
                            Math.pow(x - initialTouchX, 2) + Math.pow(y - initialTouchY, 2)
                        );
                        float dy = y - initialTouchY;
                        float scaleFactor = 1.0f + (dy / 200.0f);
                        selectedTextElement.textSize = Math.max(8f, Math.min(72f, initialTextSize * scaleFactor));
                        drawView.invalidate();
                        return true;
                    } else if (textEditState == TextEditState.ROTATING && selectedTextElement != null) {
                        // Rotate the text
                        float centerX = selectedTextElement.x;
                        float centerY = selectedTextElement.y;
                        
                        // Calculate angle from center
                        double angleInitial = Math.atan2(initialTouchY - centerY, initialTouchX - centerX);
                        double angleCurrent = Math.atan2(y - centerY, x - centerX);
                        float rotationChange = (float) Math.toDegrees(angleCurrent - angleInitial);
                        
                        selectedTextElement.rotation = initialRotation + rotationChange;
                        drawView.invalidate();
                        return true;
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    if (textEditState == TextEditState.MOVING || 
                        textEditState == TextEditState.RESIZING || 
                        textEditState == TextEditState.ROTATING) {
                        // Stay in current mode, just update display
                        // User can continue moving/resizing/rotating with next touch
                        drawView.invalidate();
                        return true;
                    }
                    break;
            }
            return true;
        } else if (currentMode == Mode.DRAW) {
            selectedTextElement = null;
            textEditState = TextEditState.NONE;
            hideActionButtons();
            return drawView.onTouchEvent(event);
        } else if (currentMode == Mode.SCROLL) {
            selectedTextElement = null;
            textEditState = TextEditState.NONE;
            hideActionButtons();
            return false;
        }
        selectedTextElement = null;
        textEditState = TextEditState.NONE;
        hideActionButtons();
        return false;
    }

    /**
     * Helper class for text bounds
     */
    private static class TextBounds {
        float left, top, right, bottom;
    }
    
    /**
     * Calculate text bounds with custom size
     */
    private TextBounds calculateTextBounds(TextElement element) {
        Paint customPaint = new Paint(textPaint);
        customPaint.setTextSize(element.textSize);
        
        android.graphics.Rect bounds = new android.graphics.Rect();
        customPaint.getTextBounds(element.text, 0, element.text.length(), bounds);
        
        TextBounds tb = new TextBounds();
        tb.left = element.x;
        tb.top = element.y - bounds.height();
        tb.right = element.x + bounds.width();
        tb.bottom = element.y;
        return tb;
    }
    
    /**
     * Find text element at given position
     */
    private TextElement findTextAtPosition(float x, float y) {
        // Check in reverse order (top elements first)
        for (int i = textElements.size() - 1; i >= 0; i--) {
            TextElement element = textElements.get(i);
            
            TextBounds bounds = calculateTextBounds(element);
            
            // Calculate text area with some padding for easier selection
            float padding = 20;
            float left = bounds.left - padding;
            float top = bounds.top - padding;
            float right = bounds.right + padding;
            float bottom = bounds.bottom + padding;
            
            // Check if tap is within text bounds
            if (x >= left && x <= right && y >= top && y <= bottom) {
                return element;
            }
        }
        return null;
    }

    /**
     * Edit an existing text element
     */
    private void editTextElement(TextElement textElement) {
        // Finish any current editing first
        finishTextInput();
        
        // Remove the text element from the list (will be re-added when done)
        textElements.remove(textElement);
        editingTextElement = textElement;
        drawView.invalidate();
        
        // Create EditText with existing text
        activeTextEdit = new EditText(getContext());
        activeTextEdit.setBackgroundColor(Color.TRANSPARENT);
        activeTextEdit.setTextColor(Color.BLACK);
        activeTextEdit.setTextSize(16);
        activeTextEdit.setGravity(Gravity.TOP | Gravity.START);
        activeTextEdit.setSingleLine(false);
        activeTextEdit.setMinWidth(200);
        activeTextEdit.setPadding(8, 8, 8, 8);
        
        // Set existing text
        activeTextEdit.setText(textElement.text);
        activeTextEdit.setSelection(textElement.text.length()); // Cursor at end
        
        // Calculate position (reverse the baseline offset)
        float y = textElement.y - activeTextEdit.getBaseline();
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) textElement.x;
        params.topMargin = (int) y;

        addView(activeTextEdit, params);
        activeTextEdit.requestFocus();

        // Show keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(activeTextEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Only intercept touches when in TEXT or DRAW mode
        if (currentMode == Mode.TEXT || currentMode == Mode.DRAW) {
            return true;
        }
        // Allow parent scrollview to handle scrolling for SCROLL mode and others
        return false;
    }

    // Helper classes
    private static class TextElement {
        String text;
        float x;
        float y;
        float textSize = 40f;
        float rotation = 0f;

        TextElement(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private static class PathData {
        List<Float> points = new ArrayList<>();

        void addPoint(float x, float y) {
            points.add(x);
            points.add(y);
        }
    }
}
