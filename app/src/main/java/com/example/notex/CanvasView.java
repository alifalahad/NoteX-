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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * CanvasView - Custom view for drawing and placing content
 */
public class CanvasView extends FrameLayout {

    public enum Mode {
        TEXT, DRAW, PHOTO, VOICE, SCROLL, SHAPE, ERASER, LASER_POINTER, STICKY_NOTE, LASSO_SELECT
    }
    
    public enum ShapeType {
        RECTANGLE, CIRCLE, TRIANGLE, LINE, ARROW, PENTAGON, STAR, HEXAGON, 
        DIAMOND, OVAL, RIGHT_TRIANGLE, PARALLELOGRAM, CROSS, HEART
    }
    
    public enum PenStyle {
        NORMAL, PENCIL, HIGHLIGHTER
    }
    
    public enum LaserMode {
        DOT, LINE
    }
    
    public enum EraserMode {
        FULL_LINE, PARTIAL
    }
    
    // Listener interface for mode changes
    public interface OnModeChangeListener {
        void onModeChanged(Mode newMode);
    }
    
    // Listener interface for text selection
    public interface OnTextSelectedListener extends OnModeChangeListener {
        void onTextSelected(TextElement textElement);
    }

    private Mode currentMode = Mode.TEXT;
    private ShapeType currentShape = ShapeType.RECTANGLE;
    private PenStyle currentPenStyle = PenStyle.NORMAL;
    private LaserMode currentLaserMode = LaserMode.DOT;
    private EraserMode currentEraserMode = EraserMode.FULL_LINE;
    private OnModeChangeListener modeChangeListener;
    private int currentPenColor = Color.BLACK;
    private float currentStrokeWidth = 5f;
    private boolean isDashedLine = false;
    private float eraserSize = 40f;
    private List<TextElement> stickyNotes = new ArrayList<>();
    private int stickyNoteColor = 0xFFFFEB3B; // Default yellow
    private float eraserX = -1; // Track eraser position for preview
    private float eraserY = -1;
    private long lastEraseTime = 0; // Throttle eraser calls
    private static final long ERASE_THROTTLE_MS = 50; // Slower throttle for better performance
    private float laserStrokeWidth = 4f;
    private long laserSustainTime = 2000; // 2 seconds
    private long laserLastDrawTime = 0;
    private int shapeColor = Color.BLACK;
    private float shapeStrokeWidth = 5f;
    private boolean isDashedShape = false;
    private boolean isFilledShape = false;
    
    private Paint drawPaint;
    private Paint textPaint;
    private Paint imagePaint;
    private Path currentPath;
    private List<PathData> paths;
    private List<TextElement> textElements;
    private List<ImageElement> imageElements;
    private List<VoiceElement> voiceElements;
    private DrawView drawView;
    private EditText activeTextEdit;
    private TextElement editingTextElement = null; // Track which text is being edited
    private TextElement selectedTextElement = null; // Track selected text for drag/resize
    private boolean isDraggingText = false;
    private float dragStartX, dragStartY;
    private float textOffsetX, textOffsetY; // Offset from touch to text position
    private boolean textWasDragged = false; // Track if text was actually dragged
    private android.widget.LinearLayout textActionButtons;
    private GestureDetector gestureDetector;
    private boolean justCreatedText = false; // Track if we just created a text box
    private boolean isFinishingText = false; // Guard flag to prevent recursive calls
    private boolean isTextEditMode = false; // Enable text editing in lasso mode
    
    // Image interaction state
    private ImageElement selectedImageElement = null;
    private boolean isDraggingImage = false;
    private boolean isResizingImage = false;
    private int resizeHandle = -1; // -1=none, 0-7=corner/edge handles
    private float imageOffsetX, imageOffsetY;
    private float imageStartWidth, imageStartHeight;
    private float imageStartX, imageStartY;
    
    // Voice playback state
    private android.media.MediaPlayer mediaPlayer;
    private VoiceElement playingVoiceElement = null;
    private android.os.Handler playbackHandler = new android.os.Handler();
    private Runnable playbackProgressRunnable;
    
    // Voice interaction state
    private VoiceElement selectedVoiceElement = null;
    private boolean isDraggingVoice = false;
    private boolean isResizingVoice = false;
    private int voiceResizeHandle = -1;
    private float voiceOffsetX, voiceOffsetY;
    private float voiceStartWidth, voiceStartHeight;
    private float voiceStartX, voiceStartY;
    
    // Long-press detection using ViewConfiguration (Android standard)
    private android.os.Handler longPressHandler;
    private Runnable longPressRunnable;
    private boolean isLongPressDetected = false;
    private long touchDownTime = 0;
    private static final int LONG_PRESS_TIMEOUT = android.view.ViewConfiguration.getLongPressTimeout(); // Standard Android timeout (~500ms)
    
    // Undo/Redo stacks
    private java.util.Stack<CanvasState> undoStack = new java.util.Stack<>();
    private java.util.Stack<CanvasState> redoStack = new java.util.Stack<>();
    private static final int MAX_UNDO_STACK = 50;
    
    // Simplified text state - just track if editing
    private boolean isEditingText = false;
    private float initialTouchX, initialTouchY;

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Drawing paint
        drawPaint = new Paint();
        updateDrawPaint();
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        // Text paint
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);
        
        // Image paint
        imagePaint = new Paint();
        imagePaint.setAntiAlias(true);
        imagePaint.setFilterBitmap(true);

        paths = new ArrayList<>();
        textElements = new ArrayList<>();
        imageElements = new ArrayList<>();
        voiceElements = new ArrayList<>();
        
        // Initialize long-press handler
        longPressHandler = new android.os.Handler();

        // Add draw view
        drawView = new DrawView(getContext());
        addView(drawView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Setup gesture detector for double-tap to edit and long-press for dragging
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                // Long-press enables dragging for any text element (including sticky notes)
                TextElement element = findTextAtPosition(e.getX(), e.getY());
                if (element != null) {
                    // Enable dragging mode (works for both text and sticky notes)
                    selectedTextElement = element;
                    isDraggingText = true;
                    textWasDragged = false;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    textOffsetX = e.getX() - element.x;
                    textOffsetY = e.getY() - element.y;
                    
                    // Switch to TEXT edit mode if not already in it
                    if (currentMode != Mode.TEXT || !isTextEditMode) {
                        currentMode = Mode.TEXT;
                        isTextEditMode = true;
                        if (modeChangeListener != null) {
                            modeChangeListener.onModeChanged(Mode.TEXT);
                        }
                    }
                    
                    getParent().requestDisallowInterceptTouchEvent(true);
                    drawView.invalidate();
                }
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Double tap to edit text content in TEXT mode with edit enabled
                if (currentMode == Mode.TEXT && isTextEditMode && selectedTextElement != null) {
                    TextElement tappedText = findTextAtPosition(e.getX(), e.getY());
                    if (tappedText == selectedTextElement) {
                        editTextElement(selectedTextElement);
                        return true;
                    }
                }
                return false;
            }
        });

    }
    

    


    public void setMode(Mode mode) {
        // Finish any active text input when switching modes
        if (this.currentMode == Mode.TEXT && mode != Mode.TEXT) {
            finishTextInput();
            justCreatedText = false;
        }
        // Reset text edit mode when switching away from lasso
        if (mode != Mode.LASSO_SELECT) {
            isTextEditMode = false;
        }
        this.currentMode = mode;
    }
    
    public void setTextEditMode(boolean enabled) {
        this.isTextEditMode = enabled;
    }
    
    public void setOnModeChangeListener(OnModeChangeListener listener) {
        this.modeChangeListener = listener;
    }
    
    public void setShapeType(ShapeType shape) {
        this.currentShape = shape;
    }
    
    public void setPenColor(int color) {
        this.currentPenColor = color;
        updateDrawPaint();
    }
    
    public void setPenStyle(PenStyle style) {
        this.currentPenStyle = style;
        updateDrawPaint();
    }
    
    public void setStrokeWidth(float width) {
        this.currentStrokeWidth = width;
        updateDrawPaint();
    }
    
    public void setDashedLine(boolean dashed) {
        this.isDashedLine = dashed;
        updateDrawPaint();
    }
    
    public void setEraserSize(float size) {
        this.eraserSize = size;
    }
    
    public void setEraserMode(EraserMode mode) {
        this.currentEraserMode = mode;
    }
    
    public void setStickyNoteColor(int color) {
        this.stickyNoteColor = color;
    }
    
    public void setLaserMode(LaserMode mode) {
        this.currentLaserMode = mode;
    }
    
    public void setLaserStrokeWidth(float width) {
        this.laserStrokeWidth = width;
    }
    
    public void setShapeColor(int color) {
        this.shapeColor = color;
    }
    
    public void setShapeStrokeWidth(float width) {
        this.shapeStrokeWidth = width;
    }
    
    public void setDashedShape(boolean dashed) {
        this.isDashedShape = dashed;
    }
    
    public void setFilledShape(boolean filled) {
        this.isFilledShape = filled;
    }
    
    // Text selection and formatting methods
    public TextElement getSelectedText() {
        return selectedTextElement;
    }
    
    public void setSelectedText(TextElement element) {
        this.selectedTextElement = element;
        drawView.invalidate();
    }
    
    public void setTextBold(boolean bold) {
        if (selectedTextElement != null) {
            selectedTextElement.isBold = bold;
            drawView.invalidate();
        }
    }
    
    public void setTextItalic(boolean italic) {
        if (selectedTextElement != null) {
            selectedTextElement.isItalic = italic;
            drawView.invalidate();
        }
    }
    
    public void setTextUnderline(boolean underline) {
        if (selectedTextElement != null) {
            selectedTextElement.isUnderline = underline;
            drawView.invalidate();
        }
    }
    
    public void setTextColor(int color) {
        if (selectedTextElement != null) {
            selectedTextElement.textColor = color;
            drawView.invalidate();
        }
    }
    
    public void setTextSize(float size) {
        if (selectedTextElement != null) {
            selectedTextElement.textSize = size;
            drawView.invalidate();
        }
    }
    
    public void editSelectedText() {
        if (selectedTextElement != null) {
            editTextElement(selectedTextElement);
        }
    }
    
    private void updateDrawPaint() {
        drawPaint.setColor(currentPenColor);
        drawPaint.setStrokeWidth(currentStrokeWidth);
        
        // Apply pen style
        if (currentPenStyle == PenStyle.HIGHLIGHTER) {
            drawPaint.setAlpha(100); // Semi-transparent for highlighter
            drawPaint.setStrokeWidth(currentStrokeWidth * 3); // Wider
        } else if (currentPenStyle == PenStyle.PENCIL) {
            drawPaint.setAlpha(180); // Slightly transparent for pencil
        } else {
            drawPaint.setAlpha(255); // Fully opaque for normal pen
        }
        
        // Apply dashed line effect
        if (isDashedLine) {
            float dashLength = currentStrokeWidth * 4; // Scale with stroke width
            float gapLength = currentStrokeWidth * 2;
            drawPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dashLength, gapLength}, 0));
        } else {
            drawPaint.setPathEffect(null);
        }
    }

    public void addTextAtPosition(float x, float y) {
        // Finish any existing text input first (without duplication)
        finishTextInput();
        
        // Set flag that we're creating new text
        justCreatedText = true;

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
        
        // Add focus change listener to finish text when clicking outside
        activeTextEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && activeTextEdit != null) {
                finishTextInput();
            }
        });

        // Show keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(activeTextEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    public void finishTextInput() {
        if (activeTextEdit != null && !isFinishingText) {
            isFinishingText = true; // Set guard flag
            String text = activeTextEdit.getText().toString().trim();
            
            if (editingTextElement != null) {
                // Update existing text element
                editingTextElement.text = text;
                
                // Handle sticky notes vs regular text
                if (editingTextElement.isSticky) {
                    // Sticky note already in list, just update text
                    // Remove if empty
                    if (text.isEmpty()) {
                        textElements.remove(editingTextElement);
                    }
                } else {
                    // Regular text: re-add if not empty
                    if (!text.isEmpty()) {
                        textElements.add(editingTextElement);
                    }
                }
                
                // Select the edited element if not empty
                if (!text.isEmpty()) {
                    selectedTextElement = editingTextElement;
                }
                
                editingTextElement = null;
            } else if (!text.isEmpty()) {
                // New text (not editing existing)
                // Save state before adding text
                saveState();
                
                // Get exact position where text was placed
                float x = ((FrameLayout.LayoutParams) activeTextEdit.getLayoutParams()).leftMargin;
                float y = ((FrameLayout.LayoutParams) activeTextEdit.getLayoutParams()).topMargin;
                
                // Add baseline offset for proper text rendering position
                y += activeTextEdit.getBaseline();
                
                TextElement newElement = new TextElement(text, x, y);
                textElements.add(newElement);
                
                // Select the newly created text
                selectedTextElement = newElement;
                
                // Auto-switch to TEXT mode with edit enabled
                currentMode = Mode.TEXT;
                isTextEditMode = true;
                if (modeChangeListener != null) {
                    modeChangeListener.onModeChanged(Mode.TEXT);
                }
            }
            
            removeView(activeTextEdit);
            activeTextEdit = null;
            justCreatedText = false; // Reset flag
            drawView.invalidate();

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
            
            isFinishingText = false; // Reset guard flag
        }
    }

    public void clearCanvas() {
        paths.clear();
        textElements.clear();
        imageElements.clear();
        voiceElements.clear();
        if (activeTextEdit != null) {
            removeView(activeTextEdit);
            activeTextEdit = null;
        }
        drawView.invalidate();
    }
    
    /**
     * Delete the currently selected image
     */
    public boolean deleteSelectedImage() {
        if (selectedImageElement != null) {
            imageElements.remove(selectedImageElement);
            selectedImageElement = null;
            saveState(); // Save state for undo/redo
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Rotate the currently selected image by 90 degrees clockwise
     */
    public boolean rotateSelectedImage() {
        if (selectedImageElement != null) {
            selectedImageElement.rotation += 90f;
            if (selectedImageElement.rotation >= 360f) {
                selectedImageElement.rotation -= 360f;
            }
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Set the rotation angle of the currently selected image (0-360 degrees)
     */
    public boolean setSelectedImageRotation(float rotation) {
        if (selectedImageElement != null) {
            selectedImageElement.rotation = rotation % 360f;
            if (selectedImageElement.rotation < 0) {
                selectedImageElement.rotation += 360f;
            }
            saveState(); // Save state for undo/redo
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Get the current rotation of the selected image
     */
    public float getSelectedImageRotation() {
        if (selectedImageElement != null) {
            return selectedImageElement.rotation;
        }
        return 0f;
    }
    
    /**
     * Flip the currently selected image horizontally
     */
    public boolean flipSelectedImageHorizontal() {
        if (selectedImageElement != null) {
            selectedImageElement.flipHorizontal = !selectedImageElement.flipHorizontal;
            saveState(); // Save state for undo/redo
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Flip the currently selected image vertically
     */
    public boolean flipSelectedImageVertical() {
        if (selectedImageElement != null) {
            selectedImageElement.flipVertical = !selectedImageElement.flipVertical;
            saveState(); // Save state for undo/redo
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Check if an image is currently selected
     */
    public boolean hasSelectedImage() {
        return selectedImageElement != null;
    }
    
    /**
     * Check if a voice recording is currently selected
     */
    public boolean hasSelectedVoice() {
        return selectedVoiceElement != null;
    }
    
    /**
     * Delete the currently selected voice recording
     */
    public boolean deleteSelectedVoice() {
        if (selectedVoiceElement != null) {
            // Stop playback if playing
            if (mediaPlayer != null && playingVoiceElement == selectedVoiceElement) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                playingVoiceElement = null;
                if (playbackProgressRunnable != null) {
                    playbackHandler.removeCallbacks(playbackProgressRunnable);
                    playbackProgressRunnable = null;
                }
            }
            
            // Delete the audio file
            if (selectedVoiceElement.audioPath != null) {
                try {
                    java.io.File file = new java.io.File(selectedVoiceElement.audioPath);
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    // Ignore file deletion errors
                }
            }
            
            voiceElements.remove(selectedVoiceElement);
            selectedVoiceElement = null;
            saveState();
            drawView.invalidate();
            return true;
        }
        return false;
    }
    
    /**
     * Add a voice recording to the canvas
     */
    public void addVoiceRecording(String audioPath, long duration) {
        // Place at center or top-left if size unknown
        float x = getWidth() > 0 ? getWidth() / 2f - 50 : 100;
        float y = getHeight() > 0 ? 100 : 100;
        
        VoiceElement voiceElement = new VoiceElement(audioPath, x, y, duration);
        voiceElements.add(voiceElement);
        drawView.invalidate();
    }
    public void addImage(android.net.Uri imageUri) {
        try {
            // Load bitmap from URI
            android.graphics.Bitmap bitmap;
            try (java.io.InputStream input = getContext().getContentResolver().openInputStream(imageUri)) {
                if (input == null) {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    return;
                }
                bitmap = android.graphics.BitmapFactory.decodeStream(input);
            }

            if (bitmap == null) {
                Toast.makeText(getContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Scale down large images to fit canvas
            int maxWidth = getWidth() > 0 ? getWidth() / 2 : 800;
            int maxHeight = getHeight() > 0 ? getHeight() / 2 : 800;
            
            float scale = Math.min(
                (float) maxWidth / bitmap.getWidth(),
                (float) maxHeight / bitmap.getHeight()
            );
            
            if (scale < 1.0f) {
                int newWidth = (int) (bitmap.getWidth() * scale);
                int newHeight = (int) (bitmap.getHeight() * scale);
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            
            // Place image at center of canvas
            float x = (getWidth() - bitmap.getWidth()) / 2f;
            float y = (getHeight() - bitmap.getHeight()) / 2f;
            
            // Ensure image is visible (minimum coordinates)
            if (x < 0) x = 50;
            if (y < 0) y = 50;
            
            // Persist the bitmap to app storage so it can be reloaded after saving
            java.io.File imagesDir = new java.io.File(getContext().getFilesDir(), "canvas_images");
            if (!imagesDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imagesDir.mkdirs();
            }
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            java.io.File outFile = new java.io.File(imagesDir, "img_" + timeStamp + "_" + java.util.UUID.randomUUID() + ".png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            }

            ImageElement imageElement = new ImageElement(
                bitmap, outFile.getAbsolutePath(), x, y, bitmap.getWidth(), bitmap.getHeight());
            imageElements.add(imageElement);
            
            saveState(); // Save state for undo/redo
            drawView.invalidate();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    // Serialize canvas content to JSON
    public String toJson() {
        try {
            JSONObject json = new JSONObject();

            // Save paths with all properties
            JSONArray pathsArray = new JSONArray();
            for (PathData pathData : paths) {
                JSONObject pathObj = new JSONObject();
                pathObj.put("points", new JSONArray(pathData.points));
                pathObj.put("color", pathData.color);
                pathObj.put("strokeWidth", pathData.strokeWidth);
                pathObj.put("penStyle", pathData.penStyle.name());
                pathObj.put("isDashed", pathData.isDashed);
                pathObj.put("isFilled", pathData.isFilled);
                pathsArray.put(pathObj);
            }
            json.put("paths", pathsArray);

            // Save text elements with all properties
            JSONArray textsArray = new JSONArray();
            for (TextElement element : textElements) {
                JSONObject textObj = new JSONObject();
                textObj.put("text", element.text);
                textObj.put("x", element.x);
                textObj.put("y", element.y);
                textObj.put("textSize", element.textSize);
                textObj.put("rotation", element.rotation);
                textObj.put("textColor", element.textColor);
                textObj.put("backgroundColor", element.backgroundColor);
                textObj.put("isBold", element.isBold);
                textObj.put("isItalic", element.isItalic);
                textObj.put("isUnderline", element.isUnderline);
                textObj.put("isSticky", element.isSticky);
                textsArray.put(textObj);
            }
            json.put("texts", textsArray);
            
            // Save images (store file path + geometry)
            JSONArray imagesArray = new JSONArray();
            for (ImageElement element : imageElements) {
                if (element.path != null && !element.path.isEmpty()) {
                    JSONObject imageObj = new JSONObject();

                    imageObj.put("path", element.path);
                    imageObj.put("x", element.x);
                    imageObj.put("y", element.y);
                    imageObj.put("width", element.width);
                    imageObj.put("height", element.height);
                    imageObj.put("rotation", element.rotation);
                    imageObj.put("flipHorizontal", element.flipHorizontal);
                    imageObj.put("flipVertical", element.flipVertical);
                    imagesArray.put(imageObj);
                }
            }
            json.put("images", imagesArray);
            
            // Save voice recordings
            JSONArray voicesArray = new JSONArray();
            for (VoiceElement element : voiceElements) {
                if (element.audioPath != null && !element.audioPath.isEmpty()) {
                    JSONObject voiceObj = new JSONObject();
                    voiceObj.put("audioPath", element.audioPath);
                    voiceObj.put("x", element.x);
                    voiceObj.put("y", element.y);
                    voiceObj.put("width", element.width);
                    voiceObj.put("height", element.height);
                    voiceObj.put("duration", element.duration);
                    voiceObj.put("timestamp", element.timestamp);
                    voicesArray.put(voiceObj);
                }
            }
            json.put("voices", voicesArray);

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

            // Reset current content before loading
            paths.clear();
            textElements.clear();
            imageElements.clear();

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
                        // Get path properties or use defaults
                        int color = pathObj.optInt("color", Color.BLACK);
                        float strokeWidth = (float) pathObj.optDouble("strokeWidth", 5.0);
                        String penStyleStr = pathObj.optString("penStyle", "NORMAL");
                        boolean isDashed = pathObj.optBoolean("isDashed", false);
                        boolean isFilled = pathObj.optBoolean("isFilled", false);
                        
                        PenStyle penStyle = PenStyle.NORMAL;
                        try {
                            penStyle = PenStyle.valueOf(penStyleStr);
                        } catch (Exception e) {
                            penStyle = PenStyle.NORMAL;
                        }
                        
                        PathData pathData = new PathData(color, strokeWidth, penStyle, isDashed, isFilled);
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
                    
                    // Load all properties
                    if (textObj.has("textSize")) {
                        element.textSize = (float) textObj.getDouble("textSize");
                    }
                    if (textObj.has("rotation")) {
                        element.rotation = (float) textObj.getDouble("rotation");
                    }
                    if (textObj.has("textColor")) {
                        element.textColor = textObj.getInt("textColor");
                    }
                    if (textObj.has("backgroundColor")) {
                        element.backgroundColor = textObj.getInt("backgroundColor");
                    }
                    if (textObj.has("isBold")) {
                        element.isBold = textObj.getBoolean("isBold");
                    }
                    if (textObj.has("isItalic")) {
                        element.isItalic = textObj.getBoolean("isItalic");
                    }
                    if (textObj.has("isUnderline")) {
                        element.isUnderline = textObj.getBoolean("isUnderline");
                    }
                    if (textObj.has("isSticky")) {
                        element.isSticky = textObj.getBoolean("isSticky");
                    }
                    
                    textElements.add(element);
                }
            }
            
            // Load images
            if (json.has("images")) {
                JSONArray imagesArray = json.getJSONArray("images");
                for (int i = 0; i < imagesArray.length(); i++) {
                    try {
                        JSONObject imageObj = imagesArray.getJSONObject(i);
                        float x = (float) imageObj.optDouble("x", 50);
                        float y = (float) imageObj.optDouble("y", 50);
                        float width = (float) imageObj.optDouble("width", 200);
                        float height = (float) imageObj.optDouble("height", 200);
                        float rotation = (float) imageObj.optDouble("rotation", 0);
                        boolean flipH = imageObj.optBoolean("flipHorizontal", false);
                        boolean flipV = imageObj.optBoolean("flipVertical", false);

                        // New format: load from file path
                        if (imageObj.has("path")) {
                            String path = imageObj.optString("path", null);
                            if (path != null && !path.isEmpty()) {
                                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
                                if (bitmap != null) {
                                    ImageElement element = new ImageElement(bitmap, path, x, y, width, height);
                                    element.rotation = rotation;
                                    element.flipHorizontal = flipH;
                                    element.flipVertical = flipV;
                                    imageElements.add(element);
                                }
                            }
                            continue;
                        }

                        // Backward compatibility: old format with base64 bitmap
                        if (imageObj.has("bitmap")) {
                            String base64Image = imageObj.getString("bitmap");
                            byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            if (bitmap != null) {
                                // Persist it to file so subsequent saves use the new format
                                java.io.File imagesDir = new java.io.File(getContext().getFilesDir(), "canvas_images");
                                if (!imagesDir.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    imagesDir.mkdirs();
                                }
                                String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                                        .format(new java.util.Date());
                                java.io.File outFile = new java.io.File(imagesDir, "img_" + timeStamp + "_" + java.util.UUID.randomUUID() + ".png");
                                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                                }

                                ImageElement element = new ImageElement(bitmap, outFile.getAbsolutePath(), x, y, width, height);
                                element.rotation = rotation;
                                element.flipHorizontal = flipH;
                                element.flipVertical = flipV;
                                imageElements.add(element);
                            }
                        }
                    } catch (Exception imageError) {
                        // Skip corrupted images
                        imageError.printStackTrace();
                    }
                }
            }
            
            // Load voice recordings
            if (json.has("voices")) {
                JSONArray voicesArray = json.getJSONArray("voices");
                for (int i = 0; i < voicesArray.length(); i++) {
                    try {
                        JSONObject voiceObj = voicesArray.getJSONObject(i);
                        String audioPath = voiceObj.optString("audioPath", null);
                        if (audioPath != null && !audioPath.isEmpty()) {
                            float x = (float) voiceObj.optDouble("x", 100);
                            float y = (float) voiceObj.optDouble("y", 100);
                            long duration = voiceObj.optLong("duration", 0);
                            
                            VoiceElement element = new VoiceElement(audioPath, x, y, duration);
                            element.width = (float) voiceObj.optDouble("width", 300);
                            element.height = (float) voiceObj.optDouble("height", 80);
                            if (voiceObj.has("timestamp")) {
                                element.timestamp = voiceObj.optString("timestamp", "");
                            }
                            voiceElements.add(element);
                        }
                    } catch (Exception voiceError) {
                        voiceError.printStackTrace();
                    }
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
                    
                    // Create paint with path-specific settings
                    Paint pathPaint = new Paint(drawPaint);
                    pathPaint.setColor(pathData.color);
                    pathPaint.setStrokeWidth(pathData.strokeWidth);
                    
                    // Check if this is a filled shape
                    if (pathData.isFilled) {
                        pathPaint.setStyle(Paint.Style.FILL);
                    } else {
                        pathPaint.setStyle(Paint.Style.STROKE);
                    }
                    
                    // Apply pen style
                    if (pathData.penStyle == PenStyle.HIGHLIGHTER) {
                        pathPaint.setAlpha(100);
                        pathPaint.setStrokeWidth(pathData.strokeWidth * 3);
                    } else if (pathData.penStyle == PenStyle.PENCIL) {
                        pathPaint.setAlpha(180);
                        pathPaint.setStrokeCap(Paint.Cap.ROUND);
                        pathPaint.setStrokeJoin(Paint.Join.ROUND);
                    }
                    
                    // Apply dashed line (only for stroked shapes)
                    if (pathData.isDashed && !pathData.isFilled) {
                        float dashLength = pathData.strokeWidth * 4;
                        float gapLength = pathData.strokeWidth * 2;
                        pathPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dashLength, gapLength}, 0));
                    } else {
                        pathPaint.setPathEffect(null);
                    }
                    
                    canvas.drawPath(path, pathPaint);
                }
            }

            // Draw current path
            if (currentPath != null && currentMode != Mode.LASER_POINTER) {
                // For shapes, use shape-specific settings
                if (currentMode == Mode.SHAPE) {
                    Paint shapePaint = new Paint(drawPaint);
                    shapePaint.setColor(shapeColor);
                    shapePaint.setStrokeWidth(shapeStrokeWidth);
                    shapePaint.setAntiAlias(true);
                    
                    // Set fill or stroke based on isFilledShape
                    if (isFilledShape) {
                        shapePaint.setStyle(Paint.Style.FILL);
                    } else {
                        shapePaint.setStyle(Paint.Style.STROKE);
                        
                        // Apply dashed line for stroke mode
                        if (isDashedShape) {
                            float dashLength = shapeStrokeWidth * 4;
                            float gapLength = shapeStrokeWidth * 2;
                            shapePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dashLength, gapLength}, 0));
                        }
                    }
                    
                    canvas.drawPath(currentPath, shapePaint);
                } else {
                    canvas.drawPath(currentPath, drawPaint);
                }
            }
            
            // Draw laser pointer
            if (currentMode == Mode.LASER_POINTER && currentPath != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - laserLastDrawTime < laserSustainTime) {
                    Paint laserPaint = new Paint();
                    laserPaint.setColor(Color.RED);
                    laserPaint.setAntiAlias(true);
                    
                    if (currentLaserMode == LaserMode.DOT) {
                        // Draw red dot
                        canvas.drawCircle(initialTouchX, initialTouchY, laserStrokeWidth * 2, laserPaint);
                    } else {
                        // Draw red line
                        laserPaint.setStyle(Paint.Style.STROKE);
                        laserPaint.setStrokeWidth(laserStrokeWidth);
                        canvas.drawPath(currentPath, laserPaint);
                    }
                    // Trigger redraw to clear after sustain time
                    postInvalidateDelayed(50);
                }
            }

            // Draw all text elements with multi-line support
            for (TextElement element : textElements) {
                canvas.save();
                
                // Calculate text bounds first (needed for both selection and sticky notes)
                Paint customTextPaint = new Paint(textPaint);
                customTextPaint.setTextSize(element.textSize);
                
                // Apply bold/italic for accurate measurement
                if (element.isBold && element.isItalic) {
                    customTextPaint.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC));
                } else if (element.isBold) {
                    customTextPaint.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                } else if (element.isItalic) {
                    customTextPaint.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC));
                }
                
                // Handle multi-line text for accurate bounds
                String[] lines = element.text.isEmpty() ? new String[]{" "} : element.text.split("\n");
                float lineHeight = customTextPaint.descent() - customTextPaint.ascent();
                
                // Calculate max width across all lines
                float maxWidth = 0;
                for (String line : lines) {
                    android.graphics.Rect lineBounds = new android.graphics.Rect();
                    customTextPaint.getTextBounds(line.isEmpty() ? " " : line, 0, Math.max(1, line.length()), lineBounds);
                    maxWidth = Math.max(maxWidth, lineBounds.width());
                }
                
                // Calculate total height for all lines
                float totalHeight = lineHeight * lines.length;
                
                float padding = element.isSticky ? 20 : 10;
                
                // For sticky notes, ensure minimum visible size
                float minWidth = element.isSticky ? 200 : 0;
                float minHeight = element.isSticky ? 100 : 0;
                
                // Calculate rectangle bounds that encompass all text
                float rectLeft = element.x;
                float rectTop = element.y - lineHeight;
                float rectRight = element.x + Math.max(maxWidth, minWidth) + (padding * 2);
                float rectBottom = element.y + totalHeight - lineHeight + (padding * 2);
                
                // Draw sticky note background if this is a sticky note
                if (element.isSticky) {
                    Paint stickyPaint = new Paint();
                    stickyPaint.setColor(element.backgroundColor);
                    stickyPaint.setStyle(Paint.Style.FILL);
                    stickyPaint.setShadowLayer(8, 0, 4, 0x40000000); // Add shadow
                    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, stickyPaint);
                }
                
                // Draw selection background if this is the selected text
                if (element == selectedTextElement) {
                    Paint selectionPaint = new Paint();
                    selectionPaint.setColor(0x4000A3FF); // Light blue with transparency
                    selectionPaint.setStyle(Paint.Style.FILL);
                    
                    // Draw selection rectangle with padding
                    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, selectionPaint);
                    
                    // Draw selection border
                    Paint borderPaint = new Paint();
                    borderPaint.setColor(0xFF00A3FF); // Blue border
                    borderPaint.setStyle(Paint.Style.STROKE);
                    borderPaint.setStrokeWidth(3);
                    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, borderPaint);
                }
                
                // Apply rotation
                canvas.rotate(element.rotation, element.x, element.y);
                
                // Update text color
                customTextPaint.setColor(element.textColor);
                
                // Apply underline
                customTextPaint.setUnderlineText(element.isUnderline);
                
                // Draw all text lines inside the sticky note
                float currentY = element.y + padding;
                for (String line : lines) {
                    canvas.drawText(line, element.x + padding, currentY, customTextPaint);
                    currentY += lineHeight;
                }
                
                canvas.restore();
            }
            
            // Draw all images
            for (ImageElement imageElement : imageElements) {
                if (imageElement.bitmap != null && !imageElement.bitmap.isRecycled()) {
                    canvas.save();
                    
                    // Calculate center for transformations
                    float centerX = imageElement.x + imageElement.width / 2;
                    float centerY = imageElement.y + imageElement.height / 2;
                    
                    // Move to center for rotation/flip
                    canvas.translate(centerX, centerY);
                    
                    // Apply rotation
                    canvas.rotate(imageElement.rotation);
                    
                    // Apply flips
                    float scaleX = imageElement.flipHorizontal ? -1f : 1f;
                    float scaleY = imageElement.flipVertical ? -1f : 1f;
                    canvas.scale(scaleX, scaleY);
                    
                    // Move back from center
                    canvas.translate(-centerX, -centerY);
                    
                    // Draw image
                    android.graphics.RectF destRect = new android.graphics.RectF(
                        imageElement.x, 
                        imageElement.y,
                        imageElement.x + imageElement.width,
                        imageElement.y + imageElement.height
                    );
                    canvas.drawBitmap(imageElement.bitmap, null, destRect, imagePaint);
                    
                    // Draw selection border and resize handles if selected
                    if (imageElement == selectedImageElement) {
                        // Blue border
                        Paint borderPaint = new Paint();
                        borderPaint.setColor(0xFF2196F3); // Blue
                        borderPaint.setStyle(Paint.Style.STROKE);
                        borderPaint.setStrokeWidth(4);
                        canvas.drawRect(destRect, borderPaint);
                        
                        // Draw 8 resize handles (corners and edges)
                        Paint handlePaint = new Paint();
                        handlePaint.setColor(0xFF2196F3);
                        handlePaint.setStyle(Paint.Style.FILL);
                        
                        float handleSize = 20;
                        float left = imageElement.x;
                        float top = imageElement.y;
                        float right = imageElement.x + imageElement.width;
                        float bottom = imageElement.y + imageElement.height;
                        float handleCenterX = (left + right) / 2;
                        float handleCenterY = (top + bottom) / 2;
                        
                        // Draw 8 handles: TL, T, TR, R, BR, B, BL, L
                        canvas.drawCircle(left, top, handleSize / 2, handlePaint); // 0: Top-left
                        canvas.drawCircle(handleCenterX, top, handleSize / 2, handlePaint); // 1: Top
                        canvas.drawCircle(right, top, handleSize / 2, handlePaint); // 2: Top-right
                        canvas.drawCircle(right, handleCenterY, handleSize / 2, handlePaint); // 3: Right
                        canvas.drawCircle(right, bottom, handleSize / 2, handlePaint); // 4: Bottom-right
                        canvas.drawCircle(handleCenterX, bottom, handleSize / 2, handlePaint); // 5: Bottom
                        canvas.drawCircle(left, bottom, handleSize / 2, handlePaint); // 6: Bottom-left
                        canvas.drawCircle(left, handleCenterY, handleSize / 2, handlePaint); // 7: Left
                    }
                    
                    canvas.restore();
                }
            }
            
            // Draw all voice recordings as horizontal lines with waveform
            for (VoiceElement voiceElement : voiceElements) {
                boolean isPlaying = (voiceElement == playingVoiceElement);
                boolean isSelected = (voiceElement == selectedVoiceElement);
                
                // Draw background rectangle
                Paint bgPaint = new Paint();
                bgPaint.setColor(isPlaying ? 0xFFE8F5E9 : 0xFFFFF3E0); // Light green if playing, light orange otherwise
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setAntiAlias(true);
                android.graphics.RectF bgRect = new android.graphics.RectF(
                    voiceElement.x, voiceElement.y,
                    voiceElement.x + voiceElement.width, voiceElement.y + voiceElement.height
                );
                canvas.drawRoundRect(bgRect, 12, 12, bgPaint);
                
                // Draw waveform lines
                Paint waveformPaint = new Paint();
                waveformPaint.setColor(isPlaying ? 0xFF4CAF50 : 0xFFFF9800); // Green if playing, orange otherwise
                waveformPaint.setStrokeWidth(3);
                waveformPaint.setStrokeCap(Paint.Cap.ROUND);
                waveformPaint.setAntiAlias(true);
                
                float waveformStartX = voiceElement.x + 50;
                float waveformEndX = voiceElement.x + voiceElement.width - 10;
                float centerY = voiceElement.y + voiceElement.height / 2;
                int numBars = 40;
                float barSpacing = (waveformEndX - waveformStartX) / numBars;
                
                // Calculate playback progress position
                float progressX = waveformStartX;
                if (isPlaying && mediaPlayer != null) {
                    try {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();
                        if (duration > 0) {
                            float progress = (float) currentPosition / duration;
                            progressX = waveformStartX + progress * (waveformEndX - waveformStartX);
                        }
                    } catch (Exception e) {
                        // Ignore if media player is not in valid state
                    }
                }
                
                for (int i = 0; i < numBars; i++) {
                    float x = waveformStartX + i * barSpacing;
                    // Create varying heights for waveform effect
                    float barHeight = (float) (Math.sin(i * 0.5) * 15 + 20);
                    
                    // Change color for bars after playback position
                    if (isPlaying && x > progressX) {
                        Paint unplayedPaint = new Paint();
                        unplayedPaint.setColor(0xFFBDBDBD); // Gray for unplayed
                        unplayedPaint.setStrokeWidth(3);
                        unplayedPaint.setStrokeCap(Paint.Cap.ROUND);
                        unplayedPaint.setAntiAlias(true);
                        canvas.drawLine(x, centerY - barHeight / 2, x, centerY + barHeight / 2, unplayedPaint);
                    } else {
                        canvas.drawLine(x, centerY - barHeight / 2, x, centerY + barHeight / 2, waveformPaint);
                    }
                }
                
                // Draw playback progress indicator line
                if (isPlaying && progressX > waveformStartX) {
                    Paint progressLinePaint = new Paint();
                    progressLinePaint.setColor(0xFFFF5722); // Red/orange progress line
                    progressLinePaint.setStrokeWidth(4);
                    progressLinePaint.setAntiAlias(true);
                    canvas.drawLine(progressX, voiceElement.y + 10, progressX, voiceElement.y + voiceElement.height - 30, progressLinePaint);
                    
                    // Draw circle at top of progress line
                    canvas.drawCircle(progressX, voiceElement.y + 10, 6, progressLinePaint);
                }
                
                // Draw play/pause icon on the left
                Paint iconPaint = new Paint();
                iconPaint.setColor(isPlaying ? 0xFF4CAF50 : 0xFFFF9800);
                iconPaint.setStyle(Paint.Style.FILL);
                iconPaint.setAntiAlias(true);
                
                float iconCenterX = voiceElement.x + 25;
                float iconCenterY = centerY;
                
                if (isPlaying) {
                    // Pause icon (two bars)
                    canvas.drawRect(iconCenterX - 6, iconCenterY - 10, iconCenterX - 2, iconCenterY + 10, iconPaint);
                    canvas.drawRect(iconCenterX + 2, iconCenterY - 10, iconCenterX + 6, iconCenterY + 10, iconPaint);
                } else {
                    // Play icon (triangle)
                    android.graphics.Path playPath = new android.graphics.Path();
                    playPath.moveTo(iconCenterX - 5, iconCenterY - 8);
                    playPath.lineTo(iconCenterX - 5, iconCenterY + 8);
                    playPath.lineTo(iconCenterX + 8, iconCenterY);
                    playPath.close();
                    canvas.drawPath(playPath, iconPaint);
                }
                
                // Draw duration text at bottom
                Paint textPaint = new Paint();
                textPaint.setColor(0xFF666666);
                textPaint.setTextSize(20);
                textPaint.setAntiAlias(true);
                
                int seconds = (int) (voiceElement.duration / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                String durationText = String.format("%d:%02d", minutes, seconds);
                canvas.drawText(durationText, voiceElement.x + 10, voiceElement.y + voiceElement.height - 8, textPaint);
                
                // Draw selection border and resize handles if selected
                if (isSelected) {
                    Paint borderPaint = new Paint();
                    borderPaint.setColor(0xFF2196F3); // Blue
                    borderPaint.setStyle(Paint.Style.STROKE);
                    borderPaint.setStrokeWidth(4);
                    canvas.drawRoundRect(bgRect, 12, 12, borderPaint);
                    
                    // Draw resize handles (left and right only for horizontal resize)
                    Paint handlePaint = new Paint();
                    handlePaint.setColor(0xFF2196F3);
                    handlePaint.setStyle(Paint.Style.FILL);
                    
                    float handleSize = 20;
                    float left = voiceElement.x;
                    float right = voiceElement.x + voiceElement.width;
                    float midY = voiceElement.y + voiceElement.height / 2;
                    
                    // Left and right handles for width adjustment
                    canvas.drawCircle(left, midY, handleSize / 2, handlePaint);
                    canvas.drawCircle(right, midY, handleSize / 2, handlePaint);
                }
            }
            
            // Draw eraser preview circle when in eraser mode
            if (currentMode == Mode.ERASER && eraserX >= 0 && eraserY >= 0) {
                Paint eraserPreviewPaint = new Paint();
                eraserPreviewPaint.setColor(0x40FF0000); // Semi-transparent red
                eraserPreviewPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(eraserX, eraserY, eraserSize, eraserPreviewPaint);
                
                // Draw border
                Paint borderPaint = new Paint();
                borderPaint.setColor(0xFFFF0000); // Red border
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(2);
                canvas.drawCircle(eraserX, eraserY, eraserSize, borderPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (currentMode != Mode.DRAW && currentMode != Mode.SHAPE && currentMode != Mode.ERASER && currentMode != Mode.LASER_POINTER) {
                return false;
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Request parent to not intercept touch events while drawing
                    getParent().requestDisallowInterceptTouchEvent(true);
                    
                    if (currentMode == Mode.ERASER) {
                        // Eraser mode - remove paths near touch point
                        eraserX = x;
                        eraserY = y;
                        erasePaths(x, y);
                        return true;
                    } else if (currentMode == Mode.LASER_POINTER) {
                        // Laser pointer - just show red dot/line, don't save
                        initialTouchX = x;
                        initialTouchY = y;
                        currentPath = new Path();
                        laserLastDrawTime = System.currentTimeMillis();
                        if (currentLaserMode == LaserMode.LINE) {
                            currentPath.moveTo(x, y);
                        }
                        invalidate();
                        return true;
                    }
                    
                    // Save state before drawing
                    saveState();
                    
                    if (currentMode == Mode.DRAW) {
                        currentPath = new Path();
                        currentPath.moveTo(x, y);
                        // Start new path data with current settings
                        PathData pathData = new PathData(currentPenColor, currentStrokeWidth, currentPenStyle, isDashedLine);
                        pathData.addPoint(x, y);
                        paths.add(pathData);
                    } else if (currentMode == Mode.SHAPE) {
                        // Store start point for shape
                        initialTouchX = x;
                        initialTouchY = y;
                        currentPath = new Path();
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (currentMode == Mode.ERASER) {
                        eraserX = x;
                        eraserY = y;
                        
                        // Throttle eraser calls to prevent lag (max 60fps)
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastEraseTime >= ERASE_THROTTLE_MS) {
                            erasePaths(x, y);
                            lastEraseTime = currentTime;
                        } else {
                            // Just update preview without erasing
                            invalidate();
                        }
                        return true;
                    } else if (currentMode == Mode.LASER_POINTER) {
                        if (currentLaserMode == LaserMode.LINE && currentPath != null) {
                            currentPath.lineTo(x, y);
                        }
                        initialTouchX = x;
                        initialTouchY = y;
                        laserLastDrawTime = System.currentTimeMillis();
                        invalidate();
                        return true;
                    } else if (currentMode == Mode.DRAW && currentPath != null && paths.size() > 0) {
                        currentPath.lineTo(x, y);
                        // Add point to current path data
                        paths.get(paths.size() - 1).addPoint(x, y);
                        invalidate();
                    } else if (currentMode == Mode.SHAPE && currentPath != null) {
                        // Update shape path based on current shape type
                        currentPath.reset();
                        drawShapePath(currentPath, initialTouchX, initialTouchY, x, y);
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    // Allow parent to intercept touch events again
                    getParent().requestDisallowInterceptTouchEvent(false);
                    
                    if (currentMode == Mode.ERASER || currentMode == Mode.LASER_POINTER) {
                        // Clear eraser preview and laser pointer
                        eraserX = -1;
                        eraserY = -1;
                        currentPath = null;
                        invalidate();
                        return true;
                    } else if (currentMode == Mode.DRAW) {
                        currentPath = null;
                    } else if (currentMode == Mode.SHAPE && currentPath != null) {
                        // Save the shape path to paths with shape settings (use isDashedShape and isFilledShape)
                        PathData shapeData = new PathData(shapeColor, shapeStrokeWidth, PenStyle.NORMAL, isDashedShape, isFilledShape);
                        
                        // Convert path to points (approximation)
                        // For shapes with multiple contours (like arrows), we need to measure each contour
                        android.graphics.PathMeasure measure = new android.graphics.PathMeasure(currentPath, false);
                        do {
                            float length = measure.getLength();
                            float distance = 0;
                            float[] coords = new float[2];
                            
                            while (distance < length) {
                                measure.getPosTan(distance, coords, null);
                                shapeData.addPoint(coords[0], coords[1]);
                                distance += 5; // Sample every 5 pixels
                            }
                            // Add the end point to ensure shape closes properly
                            if (length > 0) {
                                measure.getPosTan(length, coords, null);
                                shapeData.addPoint(coords[0], coords[1]);
                            }
                        } while (measure.nextContour());
                        
                        paths.add(shapeData);
                        currentPath = null;
                        saveState(); // Save state for undo/redo
                    }
                    invalidate();
                    return true;

                default:
                    return false;
            }
        }
        
        private void drawShapePath(Path path, float startX, float startY, float endX, float endY) {
            switch (currentShape) {
                case RECTANGLE:
                    path.moveTo(startX, startY);
                    path.lineTo(endX, startY);
                    path.lineTo(endX, endY);
                    path.lineTo(startX, endY);
                    path.close();
                    break;
                    
                case CIRCLE:
                    float centerX = (startX + endX) / 2;
                    float centerY = (startY + endY) / 2;
                    float radiusX = Math.abs(endX - startX) / 2;
                    float radiusY = Math.abs(endY - startY) / 2;
                    android.graphics.RectF oval = new android.graphics.RectF(
                        centerX - radiusX, centerY - radiusY,
                        centerX + radiusX, centerY + radiusY
                    );
                    path.addOval(oval, Path.Direction.CW);
                    break;
                    
                case TRIANGLE:
                    float midX = (startX + endX) / 2;
                    path.moveTo(midX, startY); // Top point
                    path.lineTo(endX, endY);   // Bottom right
                    path.lineTo(startX, endY); // Bottom left
                    path.close();
                    break;
                    
                case LINE:
                    path.moveTo(startX, startY);
                    path.lineTo(endX, endY);
                    break;
                    
                case ARROW:
                    // Draw main line
                    path.moveTo(startX, startY);
                    path.lineTo(endX, endY);
                    
                    // Draw arrowhead with proper triangle (fixed)
                    double angle = Math.atan2(endY - startY, endX - startX);
                    float arrowLength = 40;
                    
                    // Calculate arrowhead points with correct angle
                    float x1 = endX - arrowLength * (float)Math.cos(angle - Math.toRadians(30));
                    float y1 = endY - arrowLength * (float)Math.sin(angle - Math.toRadians(30));
                    float x2 = endX - arrowLength * (float)Math.cos(angle + Math.toRadians(30));
                    float y2 = endY - arrowLength * (float)Math.sin(angle + Math.toRadians(30));
                    
                    // Draw arrowhead lines (not filled for cleaner look)
                    path.moveTo(endX, endY);
                    path.lineTo(x1, y1);
                    path.moveTo(endX, endY);
                    path.lineTo(x2, y2);
                    break;
                    
                case PENTAGON:
                    float pentagonCenterX = (startX + endX) / 2;
                    float pentagonCenterY = (startY + endY) / 2;
                    float pentagonRadius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                    
                    path.moveTo(
                        pentagonCenterX + pentagonRadius * (float) Math.cos(Math.toRadians(-90)),
                        pentagonCenterY + pentagonRadius * (float) Math.sin(Math.toRadians(-90))
                    );
                    for (int i = 1; i <= 5; i++) {
                        path.lineTo(
                            pentagonCenterX + pentagonRadius * (float) Math.cos(Math.toRadians(-90 + i * 72)),
                            pentagonCenterY + pentagonRadius * (float) Math.sin(Math.toRadians(-90 + i * 72))
                        );
                    }
                    path.close();
                    break;
                    
                case STAR:
                    float starCenterX = (startX + endX) / 2;
                    float starCenterY = (startY + endY) / 2;
                    float starOuterRadius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                    float starInnerRadius = starOuterRadius * 0.4f;
                    
                    path.moveTo(
                        starCenterX + starOuterRadius * (float) Math.cos(Math.toRadians(-90)),
                        starCenterY + starOuterRadius * (float) Math.sin(Math.toRadians(-90))
                    );
                    for (int i = 1; i <= 10; i++) {
                        float radius = (i % 2 == 0) ? starOuterRadius : starInnerRadius;
                        path.lineTo(
                            starCenterX + radius * (float) Math.cos(Math.toRadians(-90 + i * 36)),
                            starCenterY + radius * (float) Math.sin(Math.toRadians(-90 + i * 36))
                        );
                    }
                    path.close();
                    break;
                    
                case HEXAGON:
                    float hexCenterX = (startX + endX) / 2;
                    float hexCenterY = (startY + endY) / 2;
                    float hexRadius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                    
                    path.moveTo(
                        hexCenterX + hexRadius * (float) Math.cos(Math.toRadians(-90)),
                        hexCenterY + hexRadius * (float) Math.sin(Math.toRadians(-90))
                    );
                    for (int i = 1; i <= 6; i++) {
                        path.lineTo(
                            hexCenterX + hexRadius * (float) Math.cos(Math.toRadians(-90 + i * 60)),
                            hexCenterY + hexRadius * (float) Math.sin(Math.toRadians(-90 + i * 60))
                        );
                    }
                    path.close();
                    break;
                    
                case DIAMOND:
                    float diamondCenterX = (startX + endX) / 2;
                    float diamondCenterY = (startY + endY) / 2;
                    float diamondWidth = Math.abs(endX - startX) / 2;
                    float diamondHeight = Math.abs(endY - startY) / 2;
                    
                    path.moveTo(diamondCenterX, diamondCenterY - diamondHeight); // Top
                    path.lineTo(diamondCenterX + diamondWidth, diamondCenterY); // Right
                    path.lineTo(diamondCenterX, diamondCenterY + diamondHeight); // Bottom
                    path.lineTo(diamondCenterX - diamondWidth, diamondCenterY); // Left
                    path.close();
                    break;
                    
                case OVAL:
                    android.graphics.RectF ovalRect = new android.graphics.RectF(
                        Math.min(startX, endX), 
                        Math.min(startY, endY),
                        Math.max(startX, endX), 
                        Math.max(startY, endY)
                    );
                    path.addOval(ovalRect, Path.Direction.CW);
                    break;
                    
                case RIGHT_TRIANGLE:
                    path.moveTo(startX, startY);
                    path.lineTo(endX, endY);
                    path.lineTo(startX, endY);
                    path.close();
                    break;
                    
                case PARALLELOGRAM:
                    float paraOffset = Math.abs(endX - startX) * 0.3f;
                    path.moveTo(startX + paraOffset, startY);
                    path.lineTo(endX, startY);
                    path.lineTo(endX - paraOffset, endY);
                    path.lineTo(startX, endY);
                    path.close();
                    break;
                    
                case CROSS:
                    float crossCenterX = (startX + endX) / 2;
                    float crossCenterY = (startY + endY) / 2;
                    float crossWidth = Math.abs(endX - startX);
                    float crossHeight = Math.abs(endY - startY);
                    
                    // Vertical line
                    path.moveTo(crossCenterX, startY);
                    path.lineTo(crossCenterX, endY);
                    // Horizontal line
                    path.moveTo(startX, crossCenterY);
                    path.lineTo(endX, crossCenterY);
                    break;
                    
                case HEART:
                    float heartCenterX = (startX + endX) / 2;
                    float heartWidth = Math.abs(endX - startX);
                    float heartHeight = Math.abs(endY - startY);
                    
                    // Heart shape using cubic bezier curves
                    path.moveTo(heartCenterX, startY + heartHeight * 0.3f);
                    
                    // Left top curve
                    path.cubicTo(
                        heartCenterX - heartWidth * 0.5f, startY - heartHeight * 0.1f,
                        heartCenterX - heartWidth * 0.5f, startY + heartHeight * 0.3f,
                        heartCenterX, startY + heartHeight * 0.6f
                    );
                    
                    // Right top curve
                    path.cubicTo(
                        heartCenterX + heartWidth * 0.5f, startY + heartHeight * 0.3f,
                        heartCenterX + heartWidth * 0.5f, startY - heartHeight * 0.1f,
                        heartCenterX, startY + heartHeight * 0.3f
                    );
                    
                    path.close();
                    break;
            }
        }
        
        private void erasePaths(float x, float y) {
            try {
                boolean erased = false;
                
                if (currentEraserMode == EraserMode.FULL_LINE) {
                    // Full line mode - remove entire path if any point is touched
                    List<PathData> pathsToRemove = new ArrayList<>();
                    
                    for (PathData pathData : paths) {
                        if (pathData == null || pathData.points == null) continue;
                        
                        List<Float> points = pathData.points;
                        boolean touchedPath = false;
                        
                        // Check if any point in the path is within eraser radius
                        for (int i = 0; i < points.size() && i < 1000; i += 2) { // Safety limit
                            if (i + 1 < points.size()) {
                                float px = points.get(i);
                                float py = points.get(i + 1);
                                float distance = (float) Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2));
                                
                                if (distance < eraserSize) {
                                    touchedPath = true;
                                    break;
                                }
                            }
                        }
                        
                        if (touchedPath) {
                            pathsToRemove.add(pathData);
                            erased = true;
                        }
                    }
                    
                    // Remove all touched paths
                    paths.removeAll(pathsToRemove);
                    
                } else {
                    // Partial mode - properly split paths at eraser points
                    List<PathData> newPaths = new ArrayList<>();
                    List<PathData> pathsToRemove = new ArrayList<>();
                    
                    synchronized(paths) {
                        for (PathData pathData : paths) {
                            if (pathData == null || pathData.points == null || pathData.points.isEmpty()) {
                                continue;
                            }
                            
                            try {
                                List<Float> points = pathData.points;
                                List<Float> currentSegment = new ArrayList<>();
                                boolean pathHasErasure = false;
                                
                                // Check each point and build segments
                                for (int i = 0; i < points.size() && i < 2000; i += 2) {
                                    if (i + 1 >= points.size()) break;
                                    
                                    float px = points.get(i);
                                    float py = points.get(i + 1);
                                    float distance = (float) Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2));
                                    
                                    if (distance < eraserSize) {
                                        // Point is being erased
                                        pathHasErasure = true;
                                        
                                        // Save current segment if it has enough points
                                        if (currentSegment.size() >= 4) {
                                            PathData newSegment = new PathData(
                                                pathData.color, 
                                                pathData.strokeWidth, 
                                                pathData.penStyle, 
                                                pathData.isDashed
                                            );
                                            newSegment.points = new ArrayList<>(currentSegment);
                                            newPaths.add(newSegment);
                                        }
                                        
                                        // Start fresh segment
                                        currentSegment.clear();
                                        erased = true;
                                    } else {
                                        // Point survives - add to current segment
                                        currentSegment.add(px);
                                        currentSegment.add(py);
                                    }
                                }
                                
                                // Add final segment if exists
                                if (currentSegment.size() >= 4) {
                                    PathData newSegment = new PathData(
                                        pathData.color, 
                                        pathData.strokeWidth, 
                                        pathData.penStyle, 
                                        pathData.isDashed
                                    );
                                    newSegment.points = new ArrayList<>(currentSegment);
                                    newPaths.add(newSegment);
                                }
                                
                                // Mark original path for removal if it was affected
                                if (pathHasErasure) {
                                    pathsToRemove.add(pathData);
                                }
                                
                            } catch (Exception e) {
                                android.util.Log.e("CanvasView", "Error in partial erase", e);
                            }
                        }
                        
                        // Update paths - remove old, add new segments
                        paths.removeAll(pathsToRemove);
                        paths.addAll(newPaths);
                    }
                }
                
                if (erased) {
                    invalidate();
                }
            } catch (Exception e) {
                android.util.Log.e("CanvasView", "Error in erasePaths", e);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Always pass to gesture detector for long-press and double-tap detection
        gestureDetector.onTouchEvent(event);
        
        // Check for voice recording play button click in any mode (except when actively editing)
        if (event.getAction() == MotionEvent.ACTION_DOWN && currentMode != Mode.DRAW && currentMode != Mode.SHAPE) {
            float x = event.getX();
            float y = event.getY();
            VoiceElement touchedVoice = findVoiceAtPosition(x, y);
            if (touchedVoice != null) {
                // Check if clicking the play button area (left 50px)
                if (x < touchedVoice.x + 50) {
                    toggleVoicePlayback(touchedVoice);
                    return true;
                }
            }
        }
        
        // Handle image interaction in PHOTO mode
        if (currentMode == Mode.PHOTO) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Check if touching an existing image
                    ImageElement touchedImage = findImageAtPosition(x, y);
                    if (touchedImage != null) {
                        // Single tap selects image
                        selectedImageElement = touchedImage;
                        
                        // Check if touching a resize handle
                        resizeHandle = getResizeHandle(touchedImage, x, y);
                        if (resizeHandle >= 0) {
                            isResizingImage = true;
                            imageStartWidth = touchedImage.width;
                            imageStartHeight = touchedImage.height;
                            imageStartX = touchedImage.x;
                            imageStartY = touchedImage.y;
                            dragStartX = x;
                            dragStartY = y;
                        } else {
                            // Not on a handle - start dragging immediately
                            isDraggingImage = true;
                        }
                        
                        imageOffsetX = x - touchedImage.x;
                        imageOffsetY = y - touchedImage.y;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        drawView.invalidate();
                    } else {
                        // Clicked empty space - deselect
                        selectedImageElement = null;
                        drawView.invalidate();
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (selectedImageElement != null) {
                        if (isResizingImage && resizeHandle >= 0) {
                            // Resize image based on handle
                            float deltaX = x - dragStartX;
                            float deltaY = y - dragStartY;
                            resizeImage(selectedImageElement, resizeHandle, deltaX, deltaY);
                            drawView.invalidate();
                        } else if (isDraggingImage) {
                            // Drag image
                            selectedImageElement.x = x - imageOffsetX;
                            selectedImageElement.y = y - imageOffsetY;
                            drawView.invalidate();
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    isDraggingImage = false;
                    isResizingImage = false;
                    resizeHandle = -1;
                    if (event.getAction() == MotionEvent.ACTION_UP && selectedImageElement != null) {
                        saveState(); // Save state after image drag/resize
                    }
                    break;
            }
            return true;
        }
        
        if (currentMode == Mode.TEXT) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = x;
                    initialTouchY = y;
                    touchDownTime = System.currentTimeMillis();
                    isLongPressDetected = false;
                    
                    // Check if tapping text or sticky note
                    final TextElement tappedElement = findTextAtPosition(x, y);
                    
                    if (tappedElement != null) {
                        // Start long-press timer for dragging
                        longPressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                // Long-press detected - enable dragging
                                isLongPressDetected = true;
                                selectedTextElement = tappedElement;
                                isDraggingText = true;
                                textWasDragged = false;
                                dragStartX = initialTouchX;
                                dragStartY = initialTouchY;
                                textOffsetX = initialTouchX - tappedElement.x;
                                textOffsetY = initialTouchY - tappedElement.y;
                                
                                // Switch to TEXT edit mode
                                if (currentMode != Mode.TEXT || !isTextEditMode) {
                                    currentMode = Mode.TEXT;
                                    isTextEditMode = true;
                                    if (modeChangeListener != null) {
                                        modeChangeListener.onModeChanged(Mode.TEXT);
                                    }
                                }
                                
                                getParent().requestDisallowInterceptTouchEvent(true);
                                drawView.invalidate();
                                
                                // Haptic feedback
                                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                            }
                        };
                        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    float deltaX = Math.abs(x - initialTouchX);
                    float deltaY = Math.abs(y - initialTouchY);
                    
                    // Cancel long-press if moved before timeout
                    if ((deltaX > 10 || deltaY > 10) && !isLongPressDetected) {
                        if (longPressRunnable != null) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                    }
                    
                    // Handle dragging
                    if (isDraggingText && selectedTextElement != null && isTextEditMode) {
                        if (deltaX > 10 || deltaY > 10) {
                            textWasDragged = true;
                        }
                        
                        selectedTextElement.x = x - textOffsetX;
                        selectedTextElement.y = y - textOffsetY;
                        drawView.postInvalidateOnAnimation();
                        return true;
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Cancel long-press timer
                    if (longPressRunnable != null) {
                        longPressHandler.removeCallbacks(longPressRunnable);
                    }
                    
                    // Check if we were dragging
                    if (isDraggingText && isTextEditMode) {
                        if (textWasDragged && selectedTextElement != null) {
                            saveState(); // Save state after text drag
                        }
                        isDraggingText = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                        
                        // If didn't drag, show formatting. If dragged, just finish drag.
                        if (!textWasDragged && selectedTextElement != null) {
                            // Tapped but not dragged - show formatting
                            if (modeChangeListener != null && modeChangeListener instanceof OnTextSelectedListener) {
                                ((OnTextSelectedListener) modeChangeListener).onTextSelected(selectedTextElement);
                            }
                        }
                        textWasDragged = false;
                        isLongPressDetected = false;
                        return true;
                    }
                    
                    // Handle single tap (clicked before long-press timeout)
                    if (!isLongPressDetected) {
                        TextElement tappedText = findTextAtPosition(initialTouchX, initialTouchY);
                        if (tappedText != null) {
                            selectedTextElement = tappedText;
                            drawView.invalidate();
                            
                            // ALWAYS show formatting options on single tap when in edit mode
                            if (isTextEditMode) {
                                // Show formatting dialog immediately on single click
                                if (modeChangeListener != null && modeChangeListener instanceof OnTextSelectedListener) {
                                    ((OnTextSelectedListener) modeChangeListener).onTextSelected(tappedText);
                                }
                            } else {
                                // Single tap opens editor for typing
                                editTextElement(tappedText);
                            }
                            return true;
                        } else if (!justCreatedText && !isTextEditMode) {
                            // Create new text
                            addTextAtPosition(initialTouchX, initialTouchY);
                            return true;
                        } else if (isTextEditMode) {
                            // Deselect
                            selectedTextElement = null;
                            drawView.invalidate();
                        }
                    }
                    
                    isLongPressDetected = false;
                    break;
            }
            return true;
        } else if (currentMode == Mode.DRAW || currentMode == Mode.SHAPE || currentMode == Mode.ERASER || currentMode == Mode.LASER_POINTER) {
            selectedTextElement = null;
            isEditingText = false;
            return drawView.onTouchEvent(event);
        } else if (currentMode == Mode.STICKY_NOTE) {
            // Handle sticky note creation
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                addStickyNote(event.getX(), event.getY());
                saveState(); // Save state for undo/redo
                return true;
            }
            return false;
        } else if (currentMode == Mode.VOICE) {
            // Handle voice recording interaction (drag, resize, play)
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    VoiceElement touchedVoice = findVoiceAtPosition(x, y);
                    if (touchedVoice != null) {
                        selectedVoiceElement = touchedVoice;
                        
                        // Check if touching a resize handle
                        voiceResizeHandle = getVoiceResizeHandle(touchedVoice, x, y);
                        if (voiceResizeHandle >= 0) {
                            isResizingVoice = true;
                            voiceStartWidth = touchedVoice.width;
                            voiceStartX = touchedVoice.x;
                            dragStartX = x;
                            dragStartY = y;
                        } else {
                            // Not on handle - check if clicking play button area (left side)
                            if (x < touchedVoice.x + 50) {
                                // Clicked play button
                                toggleVoicePlayback(touchedVoice);
                            } else {
                                // Start dragging
                                isDraggingVoice = true;
                            }
                        }
                        
                        voiceOffsetX = x - touchedVoice.x;
                        voiceOffsetY = y - touchedVoice.y;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        drawView.invalidate();
                    } else {
                        selectedVoiceElement = null;
                        drawView.invalidate();
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (selectedVoiceElement != null) {
                        if (isResizingVoice && voiceResizeHandle >= 0) {
                            float deltaX = x - dragStartX;
                            if (voiceResizeHandle == 0) {
                                // Left handle - adjust x and width
                                selectedVoiceElement.x = voiceStartX + deltaX;
                                selectedVoiceElement.width = Math.max(100, voiceStartWidth - deltaX);
                            } else {
                                // Right handle - adjust width only
                                selectedVoiceElement.width = Math.max(100, voiceStartWidth + deltaX);
                            }
                            drawView.invalidate();
                        } else if (isDraggingVoice) {
                            selectedVoiceElement.x = x - voiceOffsetX;
                            selectedVoiceElement.y = y - voiceOffsetY;
                            drawView.invalidate();
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (event.getAction() == MotionEvent.ACTION_UP && (isDraggingVoice || isResizingVoice)) {
                        saveState(); // Save state after voice drag/resize
                    }
                    isDraggingVoice = false;
                    isResizingVoice = false;
                    voiceResizeHandle = -1;
                    break;
            }
            return true;
        } else if (currentMode == Mode.SCROLL) {
            selectedTextElement = null;
            isEditingText = false;
            return false;
        } else if (currentMode == Mode.LASSO_SELECT) {
            // Handle text selection and dragging in lasso mode
            float x = event.getX();
            float y = event.getY();
            
            // Pass to gesture detector for double-tap detection
            gestureDetector.onTouchEvent(event);
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    TextElement tappedText = findTextAtPosition(x, y);
                    if (tappedText != null) {
                        selectedTextElement = tappedText;
                        isDraggingText = true;
                        textWasDragged = false; // Reset drag flag
                        dragStartX = x;
                        dragStartY = y;
                        textOffsetX = x - tappedText.x;
                        textOffsetY = y - tappedText.y;
                        drawView.invalidate();
                        return true;
                    } else {
                        // Tapped on empty space - deselect
                        selectedTextElement = null;
                        isDraggingText = false;
                        drawView.invalidate();
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (isDraggingText && selectedTextElement != null) {
                        // Move the text
                        float deltaX = x - dragStartX;
                        float deltaY = y - dragStartY;
                        
                        // Only move if drag distance is significant (avoid accidental moves)
                        if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                            textWasDragged = true; // Mark that we dragged
                            selectedTextElement.x = x - textOffsetX;
                            selectedTextElement.y = y - textOffsetY;
                            drawView.invalidate();
                        }
                        return true;
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    if (isDraggingText) {
                        isDraggingText = false;
                        // Only show formatting dialog if:
                        // 1. Text was NOT dragged (just tapped)
                        // 2. We're NOT in text edit mode
                        if (!textWasDragged && selectedTextElement != null && !isTextEditMode) {
                            // Notify listener that text was selected (tapped, not dragged)
                            if (modeChangeListener != null && modeChangeListener instanceof OnTextSelectedListener) {
                                ((OnTextSelectedListener) modeChangeListener).onTextSelected(selectedTextElement);
                            }
                        }
                        return true;
                    }
                    break;
            }
            return false;
        }
        selectedTextElement = null;
        isEditingText = false;
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
        
        // Apply bold/italic for accurate measurement
        if (element.isBold && element.isItalic) {
            customPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC));
        } else if (element.isBold) {
            customPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        } else if (element.isItalic) {
            customPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC));
        }
        
        // Handle multi-line text
        String[] lines = element.text.isEmpty() ? new String[]{" "} : element.text.split("\n");
        float lineHeight = customPaint.descent() - customPaint.ascent();
        
        // Calculate max width across all lines
        float maxWidth = 0;
        for (String line : lines) {
            android.graphics.Rect lineBounds = new android.graphics.Rect();
            customPaint.getTextBounds(line.isEmpty() ? " " : line, 0, Math.max(1, line.length()), lineBounds);
            maxWidth = Math.max(maxWidth, lineBounds.width());
        }
        
        // Calculate total height for all lines
        float totalHeight = lineHeight * lines.length;
        
        float padding = element.isSticky ? 20 : 0;
        float minWidth = element.isSticky ? 200 : 0;
        float minHeight = element.isSticky ? 100 : 0;
        
        TextBounds tb = new TextBounds();
        tb.left = element.x;
        tb.top = element.y - lineHeight;
        tb.right = element.x + Math.max(maxWidth, minWidth) + (padding * 2);
        tb.bottom = element.y + Math.max(totalHeight - lineHeight, minHeight) + (padding * 2);
        return tb;
    }
    
    /**
     * Find text element at given position
     */
    private TextElement findTextAtPosition(float x, float y) {
        // Search in reverse order (top elements first)
        for (int i = textElements.size() - 1; i >= 0; i--) {
            TextElement element = textElements.get(i);
            TextBounds bounds = calculateTextBounds(element);
            if (x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Find voice element at given position
     */
    private VoiceElement findVoiceAtPosition(float x, float y) {
        for (int i = voiceElements.size() - 1; i >= 0; i--) {
            VoiceElement voice = voiceElements.get(i);
            if (x >= voice.x && x <= voice.x + voice.width &&
                y >= voice.y && y <= voice.y + voice.height) {
                return voice;
            }
        }
        return null;
    }
    
    /**
     * Get resize handle for voice element (0=left, 1=right, -1=none)
     */
    private int getVoiceResizeHandle(VoiceElement voice, float x, float y) {
        float handleSize = 40;
        float midY = voice.y + voice.height / 2;
        
        if (Math.abs(x - voice.x) < handleSize && Math.abs(y - midY) < handleSize) return 0; // Left
        if (Math.abs(x - (voice.x + voice.width)) < handleSize && Math.abs(y - midY) < handleSize) return 1; // Right
        
        return -1;
    }
    
    /**
     * Play or stop voice recording
     */
    private void toggleVoicePlayback(VoiceElement voice) {
        try {
            if (mediaPlayer != null && playingVoiceElement == voice) {
                // Stop current playback
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                playingVoiceElement = null;
                
                // Stop progress updates
                if (playbackProgressRunnable != null) {
                    playbackHandler.removeCallbacks(playbackProgressRunnable);
                    playbackProgressRunnable = null;
                }
                
                drawView.invalidate();
            } else {
                // Stop any existing playback
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    if (playbackProgressRunnable != null) {
                        playbackHandler.removeCallbacks(playbackProgressRunnable);
                        playbackProgressRunnable = null;
                    }
                }
                
                // Start new playback
                mediaPlayer = new android.media.MediaPlayer();
                mediaPlayer.setDataSource(voice.audioPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playingVoiceElement = voice;
                
                // Start progress tracking
                playbackProgressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            drawView.invalidate(); // Redraw to show progress
                            playbackHandler.postDelayed(this, 50); // Update every 50ms
                        }
                    }
                };
                playbackHandler.postDelayed(playbackProgressRunnable, 0);
                
                // Auto-stop when done
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                    playingVoiceElement = null;
                    
                    if (playbackProgressRunnable != null) {
                        playbackHandler.removeCallbacks(playbackProgressRunnable);
                        playbackProgressRunnable = null;
                    }
                    
                    drawView.invalidate();
                });
                
                drawView.invalidate();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to play recording", Toast.LENGTH_SHORT).show();
        }
    }
    private ImageElement findImageAtPosition(float x, float y) {
        // Search in reverse order (top images first)
        for (int i = imageElements.size() - 1; i >= 0; i--) {
            ImageElement image = imageElements.get(i);
            if (x >= image.x && x <= image.x + image.width &&
                y >= image.y && y <= image.y + image.height) {
                return image;
            }
        }
        return null;
    }
    
    /**
     * Get resize handle at position (returns 0-7 for handles, -1 for none)
     * Handles: 0=TL, 1=T, 2=TR, 3=R, 4=BR, 5=B, 6=BL, 7=L
     */
    private int getResizeHandle(ImageElement image, float x, float y) {
        float handleSize = 40; // Touch target size
        float left = image.x;
        float top = image.y;
        float right = image.x + image.width;
        float bottom = image.y + image.height;
        float centerX = (left + right) / 2;
        float centerY = (top + bottom) / 2;
        
        // Check each handle position
        if (Math.abs(x - left) < handleSize && Math.abs(y - top) < handleSize) return 0; // TL
        if (Math.abs(x - centerX) < handleSize && Math.abs(y - top) < handleSize) return 1; // T
        if (Math.abs(x - right) < handleSize && Math.abs(y - top) < handleSize) return 2; // TR
        if (Math.abs(x - right) < handleSize && Math.abs(y - centerY) < handleSize) return 3; // R
        if (Math.abs(x - right) < handleSize && Math.abs(y - bottom) < handleSize) return 4; // BR
        if (Math.abs(x - centerX) < handleSize && Math.abs(y - bottom) < handleSize) return 5; // B
        if (Math.abs(x - left) < handleSize && Math.abs(y - bottom) < handleSize) return 6; // BL
        if (Math.abs(x - left) < handleSize && Math.abs(y - centerY) < handleSize) return 7; // L
        
        return -1; // No handle touched
    }
    
    /**
     * Resize image based on handle and drag delta
     */
    private void resizeImage(ImageElement image, int handle, float deltaX, float deltaY) {
        float minSize = 50; // Minimum width/height
        
        switch (handle) {
            case 0: // Top-left
                image.x = imageStartX + deltaX;
                image.y = imageStartY + deltaY;
                image.width = Math.max(minSize, imageStartWidth - deltaX);
                image.height = Math.max(minSize, imageStartHeight - deltaY);
                break;
            case 1: // Top
                image.y = imageStartY + deltaY;
                image.height = Math.max(minSize, imageStartHeight - deltaY);
                break;
            case 2: // Top-right
                image.y = imageStartY + deltaY;
                image.width = Math.max(minSize, imageStartWidth + deltaX);
                image.height = Math.max(minSize, imageStartHeight - deltaY);
                break;
            case 3: // Right
                image.width = Math.max(minSize, imageStartWidth + deltaX);
                break;
            case 4: // Bottom-right
                image.width = Math.max(minSize, imageStartWidth + deltaX);
                image.height = Math.max(minSize, imageStartHeight + deltaY);
                break;
            case 5: // Bottom
                image.height = Math.max(minSize, imageStartHeight + deltaY);
                break;
            case 6: // Bottom-left
                image.x = imageStartX + deltaX;
                image.width = Math.max(minSize, imageStartWidth - deltaX);
                image.height = Math.max(minSize, imageStartHeight + deltaY);
                break;
            case 7: // Left
                image.x = imageStartX + deltaX;
                image.width = Math.max(minSize, imageStartWidth - deltaX);
                break;
        }
    }

    /**
     * Edit an existing text element
     */
    private void editTextElement(TextElement textElement) {
        // Finish any current editing first
        finishTextInput();
        
        // For sticky notes, keep them in the list so background stays visible
        // For regular text, remove from list during editing
        if (!textElement.isSticky) {
            textElements.remove(textElement);
        }
        editingTextElement = textElement;
        drawView.invalidate();
        
        // Create EditText with existing text
        activeTextEdit = new EditText(getContext());
        
        // For sticky notes, set background color to match
        if (textElement.isSticky) {
            activeTextEdit.setBackgroundColor(textElement.backgroundColor);
            activeTextEdit.setPadding(20, 20, 20, 20);
            activeTextEdit.setMinWidth(250);
            activeTextEdit.setMinHeight(150);
        } else {
            activeTextEdit.setBackgroundColor(Color.TRANSPARENT);
            activeTextEdit.setPadding(8, 8, 8, 8);
            activeTextEdit.setMinWidth(200);
        }
        
        activeTextEdit.setTextColor(textElement.textColor);
        activeTextEdit.setTextSize(textElement.textSize);
        activeTextEdit.setGravity(Gravity.TOP | Gravity.START);
        activeTextEdit.setSingleLine(false);
        
        // Set existing text
        activeTextEdit.setText(textElement.text);
        activeTextEdit.setSelection(textElement.text.length()); // Cursor at end
        
        // Calculate position - align with sticky note rendering
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        
        if (textElement.isSticky) {
            // Position to match the sticky note background
            Paint measurePaint = new Paint();
            measurePaint.setTextSize(textElement.textSize);
            float lineHeight = measurePaint.descent() - measurePaint.ascent();
            
            params.leftMargin = (int) textElement.x;
            params.topMargin = (int) (textElement.y - lineHeight);
        } else {
            params.leftMargin = (int) textElement.x;
            params.topMargin = (int) (textElement.y - 40);
        }

        addView(activeTextEdit, params);
        activeTextEdit.requestFocus();
        
        // Add focus change listener to finish text when clicking outside
        activeTextEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && activeTextEdit != null) {
                finishTextInput();
            }
        });

        // Show keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(activeTextEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void addStickyNote(float x, float y) {
        // Save state before adding sticky note
        saveState();
        
        // Create sticky note as TextElement with colored background
        // This allows it to be edited using Text Edit mode
        String initialText = ""; // Empty text
        
        // Create text element at the position
        TextElement stickyElement = new TextElement(initialText, x, y);
        stickyElement.textSize = 24f;
        stickyElement.textColor = Color.BLACK;
        stickyElement.backgroundColor = stickyNoteColor; // Store sticky note color
        stickyElement.isSticky = true; // Mark as sticky note
        textElements.add(stickyElement);
        
        // Immediately open editor for the sticky note
        editTextElement(stickyElement);
        
        // Switch to text mode so user can select and edit
        currentMode = Mode.TEXT;
        isTextEditMode = false; // Set to false so sticky note opens in edit mode
        if (modeChangeListener != null) {
            modeChangeListener.onModeChanged(Mode.TEXT);
        }
        
        drawView.invalidate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Intercept when dragging text to prevent scroll interference
        if (isDraggingText && isTextEditMode) {
            return true;
        }
        // Only intercept touches when in TEXT or DRAW mode
        if (currentMode == Mode.TEXT || currentMode == Mode.DRAW) {
            return true;
        }
        // Allow parent scrollview to handle scrolling for SCROLL mode and others
        return false;
    }

    // Helper classes
    public static class ImageElement {
        android.graphics.Bitmap bitmap;
        String path;
        float x;
        float y;
        float width;
        float height;
        float rotation = 0f;
        boolean flipHorizontal = false;
        boolean flipVertical = false;

        ImageElement(android.graphics.Bitmap bitmap, String path, float x, float y, float width, float height) {
            this.bitmap = bitmap;
            this.path = path;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    public static class VoiceElement {
        String audioPath;
        float x;
        float y;
        float width;
        float height;
        long duration; // in milliseconds
        String timestamp;
        
        VoiceElement(String audioPath, float x, float y, long duration) {
            this.audioPath = audioPath;
            this.x = x;
            this.y = y;
            this.width = 300; // Default width
            this.height = 80; // Default height
            this.duration = duration;
            this.timestamp = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date());
        }
    }
    
    public static class TextElement {
        String text;
        float x;
        float y;
        float textSize = 40f;
        float rotation = 0f;
        int textColor = Color.BLACK;
        int backgroundColor = Color.TRANSPARENT; // For sticky notes
        boolean isBold = false;
        boolean isItalic = false;
        boolean isUnderline = false;
        boolean isSticky = false; // Mark if this is a sticky note

        TextElement(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private static class PathData {
        List<Float> points = new ArrayList<>();
        int color;
        float strokeWidth;
        PenStyle penStyle;
        boolean isDashed;
        boolean isFilled = false;
        
        PathData(int color, float strokeWidth, PenStyle penStyle, boolean isDashed) {
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.penStyle = penStyle;
            this.isDashed = isDashed;
        }
        
        PathData(int color, float strokeWidth, PenStyle penStyle, boolean isDashed, boolean isFilled) {
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.penStyle = penStyle;
            this.isDashed = isDashed;
            this.isFilled = isFilled;
        }

        void addPoint(float x, float y) {
            points.add(x);
            points.add(y);
        }
    }
    
    // Canvas state for undo/redo
    private static class CanvasState {
        List<PathData> paths;
        List<TextElement> textElements;
        
        CanvasState(List<PathData> paths, List<TextElement> texts) {
            // Deep copy paths
            this.paths = new ArrayList<>();
            for (PathData path : paths) {
                PathData newPath = new PathData(path.color, path.strokeWidth, path.penStyle, path.isDashed);
                newPath.points = new ArrayList<>(path.points);
                this.paths.add(newPath);
            }
            
            // Deep copy text elements
            this.textElements = new ArrayList<>();
            for (TextElement text : texts) {
                TextElement newText = new TextElement(text.text, text.x, text.y);
                newText.textSize = text.textSize;
                newText.rotation = text.rotation;
                newText.textColor = text.textColor;
                newText.isBold = text.isBold;
                newText.isItalic = text.isItalic;
                newText.isUnderline = text.isUnderline;
                this.textElements.add(newText);
            }
        }
    }
    
    // Save current state to undo stack
    public void saveState() {
        undoStack.push(new CanvasState(paths, textElements));
        redoStack.clear(); // Clear redo stack when new action is performed
        
        // Limit undo stack size
        if (undoStack.size() > MAX_UNDO_STACK) {
            undoStack.remove(0);
        }
    }
    
    // Undo last action
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        // Save current state to redo stack
        redoStack.push(new CanvasState(paths, textElements));
        
        // Restore previous state
        CanvasState state = undoStack.pop();
        paths = new ArrayList<>();
        for (PathData path : state.paths) {
            PathData newPath = new PathData(path.color, path.strokeWidth, path.penStyle, path.isDashed);
            newPath.points = new ArrayList<>(path.points);
            paths.add(newPath);
        }
        
        textElements = new ArrayList<>();
        for (TextElement text : state.textElements) {
            TextElement newText = new TextElement(text.text, text.x, text.y);
            newText.textSize = text.textSize;
            newText.rotation = text.rotation;
            newText.textColor = text.textColor;
            newText.isBold = text.isBold;
            newText.isItalic = text.isItalic;
            newText.isUnderline = text.isUnderline;
            textElements.add(newText);
        }
        
        drawView.invalidate();
        return true;
    }
    
    // Redo last undone action
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        // Save current state to undo stack
        undoStack.push(new CanvasState(paths, textElements));
        
        // Restore redo state
        CanvasState state = redoStack.pop();
        paths = new ArrayList<>();
        for (PathData path : state.paths) {
            PathData newPath = new PathData(path.color, path.strokeWidth, path.penStyle, path.isDashed);
            newPath.points = new ArrayList<>(path.points);
            paths.add(newPath);
        }
        
        textElements = new ArrayList<>();
        for (TextElement text : state.textElements) {
            TextElement newText = new TextElement(text.text, text.x, text.y);
            newText.textSize = text.textSize;
            newText.rotation = text.rotation;
            newText.textColor = text.textColor;
            newText.isBold = text.isBold;
            newText.isItalic = text.isItalic;
            newText.isUnderline = text.isUnderline;
            textElements.add(newText);
        }
        
        drawView.invalidate();
        return true;
    }
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
