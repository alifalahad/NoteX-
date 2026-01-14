package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.Notebook;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Base64;

public class PageEditorController implements Initializable {

    @FXML private Label pageTitleLabel;
    @FXML private Label pageCountLabel;
    @FXML private VBox pagesContainer;
    @FXML private ScrollPane pagesScrollPane;
    
    // Remove single canvas references - we'll have multiple canvases now
    // Each page will have its own canvas, overlay pane, and graphics context
    
    // Tool buttons
    @FXML private ToggleButton btnPaint;
    @FXML private ToggleButton btnErase;
    @FXML private ToggleButton btnText;
    @FXML private ToggleButton btnShape;
    @FXML private ToggleButton btnImage;
    @FXML private ToggleButton btnStickyNote;
    @FXML private ToggleButton btnLaser;
    @FXML private ToggleButton btnVoice;
    @FXML private Button btnUndo;
    @FXML private Button btnRedo;
    @FXML private Button btnSave;

    private DatabaseHelper databaseHelper;
    private Notebook currentNotebook;
    private GraphicsContext gc;
    private ToggleGroup toolsGroup;
    
    // Pressure-sensitive drawing
    private static final double PRESSURE_THRESHOLD = 0.3; // Adjust for force click sensitivity
    private boolean isForcePressed = false;
    private javafx.animation.Timeline forceClickTimer;
    private static final long FORCE_CLICK_DELAY_MS = 1; // 1ms for immediate response
    
    // Laser pointer
    private javafx.scene.shape.Path laserPath;
    private javafx.animation.Timeline laserFadeTimer;
    private List<javafx.scene.shape.LineTo> laserSegments = new ArrayList<>();
    
    // Undo/Redo stacks
    private Stack<WritableImage> undoStack = new Stack<>();
    private Stack<WritableImage> redoStack = new Stack<>();
    private static final int MAX_UNDO_STACK = 50;
    
    // macOS Trackpad gesture support
    private double initialZoomFactor = 1.0;
    private double currentZoomFactor = 1.0;
    private double initialRotation = 0.0;
    private boolean isPinching = false;
    private boolean isRotating = false;
    private boolean isTwoFingerScrolling = false;
    
    // Multi-canvas support
    private List<Canvas> canvases = new ArrayList<>();
    private List<Pane> overlayPanes = new ArrayList<>();
    private Canvas currentCanvas;
    private Pane currentOverlay;
    
    // Tool Enum
    private enum Tool {
        PAINT, ERASE, TEXT, SHAPE, IMAGE, STICKY_NOTE, LASER, VOICE
    }
    
    // Pen Style Enum
    private enum PenStyle {
        NORMAL, PENCIL, HIGHLIGHTER
    }
    
    // Shape Enum
    private enum ShapeType {
        RECTANGLE, CIRCLE, TRIANGLE, LINE, ARROW, PENTAGON, STAR, HEXAGON, DIAMOND, OVAL, RIGHT_TRIANGLE, PARALLELOGRAM, CROSS, HEART
    }
    
    // Eraser Type Enum
    private enum EraserType {
        FILL_LINE,    // Full line eraser
        PARTIAL       // Partial eraser (erases only where you click)
    }
    
    private Tool currentTool = Tool.PAINT;
    private PenStyle currentPenStyle = PenStyle.NORMAL;
    private boolean isDrawing = false;
    private double lastX, lastY;
    
    // Pen settings
    private Color currentColor = Color.BLACK;
    private double strokeWidth = 5.0;
    private boolean isDashedLine = false;
    
    // Shape settings
    private ShapeType currentShapeType = ShapeType.RECTANGLE;
    private boolean isShapeFilled = false;
    private boolean isShapeDashed = false;
    
    // Eraser settings
    private EraserType currentEraserType = EraserType.FILL_LINE;
    
    // Color palette
    private static final Color[] COLORS = {
        Color.BLACK, Color.web("#424242"), Color.BLUE, Color.web("#2196F3"),
        Color.RED, Color.web("#E91E63"), Color.GREEN, Color.web("#4CAF50"),
        Color.web("#FF9800"), Color.web("#FFEB3B"), Color.web("#9C27B0"), Color.web("#795548")
    };
    
    // Multi-page support
    private List<PageData> pages = new ArrayList<>();
    private int currentPageIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        databaseHelper = AuthManager.getInstance().getDatabaseHelper();
        currentNotebook = NoteXApp.getCurrentNotebook();

        if (currentNotebook == null) {
            handleBack();
            return;
        }

        setupCanvas();
        setupToolbar();
        loadPages();
    }

    private void setupCanvas() {
        // Pages container is set up in FXML
        if (pagesContainer != null) {
            pagesContainer.setSpacing(20);
            pagesContainer.setPadding(new Insets(20));
        }
    }
    
    private void createPageCanvas(int pageIndex) {
        // Create canvas for this page
        Canvas canvas = new Canvas(800, 1200);
        GraphicsContext context = canvas.getGraphicsContext2D();
        context.setFill(Color.WHITE);
        context.fillRect(0, 0, 800, 1200);
        
        // Create overlay pane for text fields, sticky notes, and eraser cursor
        Pane overlayPane = new Pane();
        overlayPane.setPrefSize(800, 1200);
        overlayPane.setMouseTransparent(true);
        
        // Create eraser cursor circle (transparent bubble)
        javafx.scene.shape.Circle eraserCursor = new javafx.scene.shape.Circle(0, 0, strokeWidth * 2);
        eraserCursor.setFill(Color.TRANSPARENT);
        eraserCursor.setStroke(Color.web("#FF5722"));
        eraserCursor.setStrokeWidth(2);
        eraserCursor.setVisible(false);
        eraserCursor.setMouseTransparent(true);
        overlayPane.getChildren().add(eraserCursor);
        
        // Stack canvas and overlay
        StackPane pageStack = new StackPane();
        pageStack.getChildren().addAll(canvas, overlayPane);
        pageStack.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;");
        
        // Add mouse handlers
        canvas.setOnMousePressed(e -> handleMousePressedOnCanvas(e, canvas, overlayPane, pageIndex));
        canvas.setOnMouseDragged(e -> handleMouseDraggedOnCanvas(e, canvas, overlayPane, pageIndex));
        canvas.setOnMouseReleased(e -> handleMouseReleasedOnCanvas(e, canvas, overlayPane, pageIndex));
        canvas.setOnMouseMoved(e -> handleMouseMovedOnCanvas(e, overlayPane));
        canvas.setOnMouseExited(e -> handleMouseExitedCanvas(overlayPane));
        
        // Add macOS trackpad gesture handlers
        canvas.setOnZoom(e -> handleZoomGesture(e, canvas));
        canvas.setOnZoomStarted(e -> handleZoomStarted(e, canvas));
        canvas.setOnZoomFinished(e -> handleZoomFinished(e, canvas));
        
        canvas.setOnRotate(e -> handleRotateGesture(e, canvas));
        canvas.setOnRotationStarted(e -> handleRotationStarted(e, canvas));
        canvas.setOnRotationFinished(e -> handleRotationFinished(e, canvas));
        
        canvas.setOnScroll(e -> handleScrollGesture(e, canvas));
        canvas.setOnScrollStarted(e -> handleScrollStarted(e));
        canvas.setOnScrollFinished(e -> handleScrollFinished(e));
        
        canvas.setOnSwipeLeft(e -> handleSwipeLeft());
        canvas.setOnSwipeRight(e -> handleSwipeRight());
        canvas.setOnSwipeUp(e -> handleSwipeUp());
        canvas.setOnSwipeDown(e -> handleSwipeDown());
        
        // Store references
        canvases.add(canvas);
        overlayPanes.add(overlayPane);
        
        // Add to container
        pagesContainer.getChildren().add(pageStack);
    }
    
    private void applyPenSettings() {
        gc.setStroke(currentColor);
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        
        // Apply pen style
        switch (currentPenStyle) {
            case PENCIL:
                gc.setGlobalAlpha(0.8);
                gc.setLineWidth(strokeWidth * 0.8);
                break;
            case HIGHLIGHTER:
                gc.setGlobalAlpha(0.4);
                gc.setLineWidth(strokeWidth * 3);
                break;
            default:
                gc.setGlobalAlpha(1.0);
                break;
        }
        
        // Apply dashed line
        if (isDashedLine) {
            gc.setLineDashes(10, 5);
        } else {
            gc.setLineDashes(null);
        }
    }

    private void setupToolbar() {
        toolsGroup = new ToggleGroup();
        btnPaint.setToggleGroup(toolsGroup);
        btnErase.setToggleGroup(toolsGroup);
        btnText.setToggleGroup(toolsGroup);
        btnShape.setToggleGroup(toolsGroup);
        btnImage.setToggleGroup(toolsGroup);
        btnStickyNote.setToggleGroup(toolsGroup);
        btnLaser.setToggleGroup(toolsGroup);
        btnVoice.setToggleGroup(toolsGroup);

        btnPaint.setSelected(true);
        currentTool = Tool.PAINT;
        
        // Add right-click handlers for all tools
        btnPaint.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showPenOptions();
            }
        });
        
        btnErase.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showEraserOptions();
            }
        });
        
        btnShape.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showShapeOptions();
            }
        });
        
        btnText.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showTextOptions();
            }
        });
        
        btnStickyNote.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showStickyNoteOptions();
            }
        });
    }

    private void loadPages() {
        // Try to load existing pages from database
        loadExistingPages();
        
        // If no pages exist, create initial page
        if (pages.isEmpty()) {
            pages.add(new PageData());
            currentPageIndex = 0;
        }
        
        // Create canvas for each page
        for (int i = 0; i < pages.size(); i++) {
            createPageCanvas(i);
            // Load page content if it exists
            if (pages.get(i).getCanvasData() != null && !pages.get(i).getCanvasData().isEmpty()) {
                loadPageDataOntoCanvas(i);
            }
        }
        
        // Set first page as current
        if (!canvases.isEmpty()) {
            currentCanvas = canvases.get(0);
            currentOverlay = overlayPanes.get(0);
            gc = currentCanvas.getGraphicsContext2D();
        }
        
        updatePageInfo();
        updatePagesView();
    }
    
    private void updatePagesView() {
        // Update horizontal pages view if container exists
        if (pagesContainer != null) {
            // This will show multiple page thumbnails side by side
            // For now, just update the page count label
        }
    }

    private void updatePageInfo() {
        pageTitleLabel.setText(currentNotebook.getTitle());
        pageCountLabel.setText((currentPageIndex + 1) + " / " + pages.size() + " pages");
    }

    private void handleMousePressedOnCanvas(MouseEvent e, Canvas canvas, Pane overlay, int pageIndex) {
        // Only handle left click
        if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
            return;
        }
        
        currentCanvas = canvas;
        currentOverlay = overlay;
        currentPageIndex = pageIndex;
        gc = canvas.getGraphicsContext2D();
        
        lastX = e.getX();
        lastY = e.getY();
        
        // Start force click timer (300ms hold = force click)
        isForcePressed = false;
        if (forceClickTimer != null) {
            forceClickTimer.stop();
        }
        
        forceClickTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(FORCE_CLICK_DELAY_MS),
            event -> {
                isForcePressed = true;
                
                // Activate tool-specific force click actions
                if (currentTool == Tool.TEXT) {
                    saveToUndoStack(); // Save state before adding text
                    // Create text field on force click
                    TextField textField = new TextField();
                    textField.setLayoutX(lastX);
                    textField.setLayoutY(lastY - 20);
                    textField.setPrefWidth(200);
                    textField.setStyle("-fx-background-color: white; -fx-border-color: " + toHexString(currentColor) + "; -fx-text-fill: " + toHexString(currentColor) + ";");
                    
                    textField.setOnAction(evt -> {
                        String text = textField.getText();
                        if (!text.isEmpty()) {
                            gc.setGlobalAlpha(1.0);
                            gc.setFill(currentColor);
                            gc.setFont(new Font(20));
                            gc.fillText(text, lastX, lastY);
                        }
                        overlay.getChildren().remove(textField);
                    });
                    
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused && overlay.getChildren().contains(textField)) {
                            String text = textField.getText();
                            if (!text.isEmpty()) {
                                gc.setGlobalAlpha(1.0);
                                gc.setFill(currentColor);
                                gc.setFont(new Font(20));
                                gc.fillText(text, lastX, lastY);
                            }
                            overlay.getChildren().remove(textField);
                        }
                    });
                    
                    overlay.setMouseTransparent(false);
                    overlay.getChildren().add(textField);
                    textField.requestFocus();
                    isDrawing = false;
                } else if (currentTool == Tool.SHAPE) {
                    saveToUndoStack(); // Save state before drawing shape
                    // Enable shape drawing
                    isDrawing = true;
                } else if (currentTool == Tool.ERASE && currentEraserType == EraserType.FILL_LINE) {
                    saveToUndoStack(); // Save state before erasing
                    // Enable full line eraser
                    isDrawing = true;
                } else if (currentTool == Tool.IMAGE) {
                    // Insert image on force click (saves in insertImage method)
                    insertImage(lastX, lastY);
                    isDrawing = false;
                } else if (currentTool == Tool.VOICE) {
                    // Start voice recording on force click (saves in startVoiceRecording method)
                    startVoiceRecording(lastX, lastY);
                    isDrawing = false;
                } else if (currentTool == Tool.LASER) {
                    // Start laser pointer drawing (no save needed - disappears after 2 seconds)
                    isDrawing = true;
                    startLaserPointer(overlay, lastX, lastY);
                } else if (currentTool == Tool.PAINT) {
                    // Already drawing for paint
                }
            }
        ));
        forceClickTimer.setCycleCount(1);
        forceClickTimer.play();
        
        // Immediate actions for certain tools
        if (currentTool == Tool.PAINT) {
            saveToUndoStack(); // Save state before drawing
            isDrawing = true;
            applyPenSettings();
            gc.beginPath();
            gc.moveTo(lastX, lastY);
        } else if (currentTool == Tool.ERASE && currentEraserType == EraserType.PARTIAL) {
            // Partial eraser works immediately
            saveToUndoStack(); // Save state before erasing
            isDrawing = true;
            double radius = strokeWidth * 2;
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.fillOval(lastX - radius, lastY - radius, radius * 2, radius * 2);
        }
    }
    
    private void handleMouseDraggedOnCanvas(MouseEvent e, Canvas canvas, Pane overlay, int pageIndex) {
        if (!isDrawing) return;

        double x = e.getX();
        double y = e.getY();
        
        // Update eraser cursor position
        updateEraserCursor(overlay, x, y);

        if (currentTool == Tool.PAINT) {
            gc.lineTo(x, y);
            gc.stroke();
            gc.beginPath();
            gc.moveTo(x, y);
        } else if (currentTool == Tool.ERASE) {
            if (currentEraserType == EraserType.PARTIAL) {
                // Partial eraser - erase centered on mouse pointer (matching the circle)
                double radius = strokeWidth * 2;
                gc.setGlobalAlpha(1.0);
                gc.setFill(Color.WHITE);
                // Draw circle centered at x,y with same radius as cursor
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            } else if (currentEraserType == EraserType.FILL_LINE && isForcePressed) {
                // Full line eraser - erases large area with force click and hold
                double radius = strokeWidth * 3;
                gc.setGlobalAlpha(1.0);
                gc.setFill(Color.WHITE);
                gc.fillRect(x - radius, y - radius, radius * 2, radius * 2);
            }
        } else if (currentTool == Tool.LASER) {
            // Update laser pointer path
            updateLaserPointer(x, y);
        }
        
        lastX = x;
        lastY = y;
    }
    
    private void handleMouseReleasedOnCanvas(MouseEvent e, Canvas canvas, Pane overlay, int pageIndex) {
        double x = e.getX();
        double y = e.getY();
        
        // Stop force click timer
        if (forceClickTimer != null) {
            forceClickTimer.stop();
        }
        
        // Draw shape when mouse released (only if force click was activated)
        if (currentTool == Tool.SHAPE && isDrawing && isForcePressed) {
            drawShape(lastX, lastY, x, y);
        }
        
        // Stop laser pointer and start fade timer
        if (currentTool == Tool.LASER && isDrawing) {
            stopLaserPointer();
        }
        
        isDrawing = false;
        isForcePressed = false;
        if (gc != null) {
            gc.closePath();
        }
    }
    
    // Eraser cursor visualization
    private void handleMouseMovedOnCanvas(MouseEvent e, Pane overlay) {
        updateEraserCursor(overlay, e.getX(), e.getY());
    }
    
    private void handleMouseExitedCanvas(Pane overlay) {
        hideEraserCursor(overlay);
    }
    
    private void updateEraserCursor(Pane overlay, double x, double y) {
        if (currentTool == Tool.ERASE && overlay != null) {
            // Find the eraser cursor circle (first child)
            if (overlay.getChildren().size() > 0 && 
                overlay.getChildren().get(0) instanceof javafx.scene.shape.Circle) {
                javafx.scene.shape.Circle cursor = (javafx.scene.shape.Circle) overlay.getChildren().get(0);
                // Position cursor centered at canvas coordinates (not scene coordinates)
                double radius = strokeWidth * 2;
                cursor.setLayoutX(x - radius);  // Offset by radius since circle draws from top-left of bounds
                cursor.setLayoutY(y - radius);
                cursor.setRadius(radius);
                cursor.setVisible(true);
            }
        } else {
            hideEraserCursor(overlay);
        }
    }
    
    private void hideEraserCursor(Pane overlay) {
        if (overlay != null && overlay.getChildren().size() > 0 && 
            overlay.getChildren().get(0) instanceof javafx.scene.shape.Circle) {
            javafx.scene.shape.Circle cursor = (javafx.scene.shape.Circle) overlay.getChildren().get(0);
            cursor.setVisible(false);
        }
    }
    
    private void drawShape(double startX, double startY, double endX, double endY) {
        gc.setStroke(currentColor);
        gc.setFill(currentColor);
        gc.setLineWidth(strokeWidth);
        gc.setGlobalAlpha(1.0);
        
        if (isShapeDashed) {
            gc.setLineDashes(10, 5);
        } else {
            gc.setLineDashes(null);
        }
        
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        double minX = Math.min(startX, endX);
        double minY = Math.min(startY, endY);
        
        switch (currentShapeType) {
            case RECTANGLE:
                if (isShapeFilled) {
                    gc.fillRect(minX, minY, width, height);
                } else {
                    gc.strokeRect(minX, minY, width, height);
                }
                break;
                
            case CIRCLE:
                double radius = Math.sqrt(width * width + height * height) / 2;
                if (isShapeFilled) {
                    gc.fillOval(startX - radius, startY - radius, radius * 2, radius * 2);
                } else {
                    gc.strokeOval(startX - radius, startY - radius, radius * 2, radius * 2);
                }
                break;
                
            case OVAL:
                if (isShapeFilled) {
                    gc.fillOval(minX, minY, width, height);
                } else {
                    gc.strokeOval(minX, minY, width, height);
                }
                break;
                
            case LINE:
                gc.strokeLine(startX, startY, endX, endY);
                break;
                
            case ARROW:
                gc.strokeLine(startX, startY, endX, endY);
                // Draw arrowhead
                double angle = Math.atan2(endY - startY, endX - startX);
                double arrowSize = 15;
                gc.strokeLine(endX, endY, 
                    endX - arrowSize * Math.cos(angle - Math.PI / 6), 
                    endY - arrowSize * Math.sin(angle - Math.PI / 6));
                gc.strokeLine(endX, endY, 
                    endX - arrowSize * Math.cos(angle + Math.PI / 6), 
                    endY - arrowSize * Math.sin(angle + Math.PI / 6));
                break;
                
            case TRIANGLE:
                double[] xPoints = {minX + width / 2, minX, minX + width};
                double[] yPoints = {minY, minY + height, minY + height};
                if (isShapeFilled) {
                    gc.fillPolygon(xPoints, yPoints, 3);
                } else {
                    gc.strokePolygon(xPoints, yPoints, 3);
                }
                break;
                
            case RIGHT_TRIANGLE:
                double[] rtXPoints = {minX, minX, minX + width};
                double[] rtYPoints = {minY, minY + height, minY + height};
                if (isShapeFilled) {
                    gc.fillPolygon(rtXPoints, rtYPoints, 3);
                } else {
                    gc.strokePolygon(rtXPoints, rtYPoints, 3);
                }
                break;
                
            case PENTAGON:
                double[] pentX = new double[5];
                double[] pentY = new double[5];
                double centerX = minX + width / 2;
                double centerY = minY + height / 2;
                double pentRadius = Math.min(width, height) / 2;
                for (int i = 0; i < 5; i++) {
                    pentX[i] = centerX + pentRadius * Math.cos(2 * Math.PI * i / 5 - Math.PI / 2);
                    pentY[i] = centerY + pentRadius * Math.sin(2 * Math.PI * i / 5 - Math.PI / 2);
                }
                if (isShapeFilled) {
                    gc.fillPolygon(pentX, pentY, 5);
                } else {
                    gc.strokePolygon(pentX, pentY, 5);
                }
                break;
                
            case HEXAGON:
                double[] hexX = new double[6];
                double[] hexY = new double[6];
                double hexCenterX = minX + width / 2;
                double hexCenterY = minY + height / 2;
                double hexRadius = Math.min(width, height) / 2;
                for (int i = 0; i < 6; i++) {
                    hexX[i] = hexCenterX + hexRadius * Math.cos(2 * Math.PI * i / 6);
                    hexY[i] = hexCenterY + hexRadius * Math.sin(2 * Math.PI * i / 6);
                }
                if (isShapeFilled) {
                    gc.fillPolygon(hexX, hexY, 6);
                } else {
                    gc.strokePolygon(hexX, hexY, 6);
                }
                break;
                
            case STAR:
                double[] starX = new double[10];
                double[] starY = new double[10];
                double starCenterX = minX + width / 2;
                double starCenterY = minY + height / 2;
                double outerRadius = Math.min(width, height) / 2;
                double innerRadius = outerRadius / 2.5;
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? outerRadius : innerRadius;
                    starX[i] = starCenterX + r * Math.cos(2 * Math.PI * i / 10 - Math.PI / 2);
                    starY[i] = starCenterY + r * Math.sin(2 * Math.PI * i / 10 - Math.PI / 2);
                }
                if (isShapeFilled) {
                    gc.fillPolygon(starX, starY, 10);
                } else {
                    gc.strokePolygon(starX, starY, 10);
                }
                break;
                
            case DIAMOND:
                double[] diamX = {minX + width / 2, minX + width, minX + width / 2, minX};
                double[] diamY = {minY, minY + height / 2, minY + height, minY + height / 2};
                if (isShapeFilled) {
                    gc.fillPolygon(diamX, diamY, 4);
                } else {
                    gc.strokePolygon(diamX, diamY, 4);
                }
                break;
                
            case PARALLELOGRAM:
                double offset = width * 0.2;
                double[] paraX = {minX + offset, minX + width, minX + width - offset, minX};
                double[] paraY = {minY, minY, minY + height, minY + height};
                if (isShapeFilled) {
                    gc.fillPolygon(paraX, paraY, 4);
                } else {
                    gc.strokePolygon(paraX, paraY, 4);
                }
                break;
                
            case CROSS:
                double crossWidth = width / 3;
                gc.strokeLine(minX + width / 2, minY, minX + width / 2, minY + height);
                gc.strokeLine(minX, minY + height / 2, minX + width, minY + height / 2);
                break;
                
            case HEART:
                double heartCenterX = minX + width / 2;
                double heartTop = minY + height * 0.3;
                gc.beginPath();
                gc.moveTo(heartCenterX, minY + height);
                gc.bezierCurveTo(heartCenterX, heartTop, minX, heartTop, minX, minY + height * 0.2);
                gc.bezierCurveTo(minX, minY, heartCenterX, minY, heartCenterX, heartTop);
                gc.bezierCurveTo(heartCenterX, minY, minX + width, minY, minX + width, minY + height * 0.2);
                gc.bezierCurveTo(minX + width, heartTop, heartCenterX, heartTop, heartCenterX, minY + height);
                if (isShapeFilled) {
                    gc.fill();
                } else {
                    gc.stroke();
                }
                gc.closePath();
                break;
        }
        
        gc.setLineDashes(null);
    }

    @FXML
    private void handlePaint() {
        currentTool = Tool.PAINT;
    }

    @FXML
    private void handleErase() {
        currentTool = Tool.ERASE;
    }

    @FXML
    private void handleText() {
        currentTool = Tool.TEXT;
    }

    @FXML
    private void handleShape() {
        currentTool = Tool.SHAPE;
    }

    @FXML
    private void handleImage() {
        currentTool = Tool.IMAGE;
    }

    @FXML
    private void handleStickyNote() {
        currentTool = Tool.STICKY_NOTE;
        createStickyNote(100, 100);
    }
    
    private void createStickyNote(double x, double y) {
        if (currentOverlay == null) return;
        
        // Create sticky note container
        VBox stickyNote = new VBox(5);
        stickyNote.setLayoutX(x);
        stickyNote.setLayoutY(y);
        stickyNote.setPrefSize(200, 150);
        stickyNote.setStyle("-fx-background-color: #FFEB3B; -fx-padding: 10; -fx-border-color: #FBC02D; -fx-border-width: 2;");
        
        // Add title bar for dragging
        HBox titleBar = new HBox();
        titleBar.setPrefHeight(20);
        titleBar.setStyle("-fx-background-color: #FBC02D; -fx-cursor: hand;");
        
        Label titleLabel = new Label("📌 Sticky Note");
        titleLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-font-size: 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> currentOverlay.getChildren().remove(stickyNote));
        
        titleBar.getChildren().addAll(titleLabel, spacer, closeBtn);
        
        // Add text area for note content
        TextArea textArea = new TextArea();
        textArea.setPromptText("Type your note here...");
        textArea.setWrapText(true);
        textArea.setStyle("-fx-background-color: #FFEB3B; -fx-control-inner-background: #FFEB3B; -fx-text-fill: #333333;");
        VBox.setVgrow(textArea, Priority.ALWAYS);
        
        stickyNote.getChildren().addAll(titleBar, textArea);
        
        // Make sticky note draggable
        final double[] dragDelta = new double[2];
        titleBar.setOnMousePressed(mouseEvent -> {
            dragDelta[0] = stickyNote.getLayoutX() - mouseEvent.getSceneX();
            dragDelta[1] = stickyNote.getLayoutY() - mouseEvent.getSceneY();
        });
        titleBar.setOnMouseDragged(mouseEvent -> {
            stickyNote.setLayoutX(mouseEvent.getSceneX() + dragDelta[0]);
            stickyNote.setLayoutY(mouseEvent.getSceneY() + dragDelta[1]);
        });
        
        currentOverlay.setMouseTransparent(false);
        currentOverlay.getChildren().add(stickyNote);
        textArea.requestFocus();
    }

    @FXML
    private void handleLaser() {
        currentTool = Tool.LASER;
    }

    @FXML
    private void handleVoice() {
        currentTool = Tool.VOICE;
    }
    
    private void insertImage(double x, double y) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        java.io.File selectedFile = fileChooser.showOpenDialog(btnSave.getScene().getWindow());
        if (selectedFile != null) {
            try {
                saveToUndoStack(); // Save state before adding image
                Image image = new Image(selectedFile.toURI().toString());
                // Resize image to fit nicely on canvas (max 300x300)
                double maxSize = 300;
                double width = image.getWidth();
                double height = image.getHeight();
                
                if (width > maxSize || height > maxSize) {
                    double scale = Math.min(maxSize / width, maxSize / height);
                    width *= scale;
                    height *= scale;
                }
                
                gc.drawImage(image, x, y, width, height);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Failed to load image: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    private void startVoiceRecording(double x, double y) {
        if (currentOverlay == null) return;
        
        // Save state for undo
        saveToUndoStack();
        
        // Create NoteX-style voice waveform widget directly on canvas
        HBox voiceWidget = new HBox(8);
        voiceWidget.setLayoutX(x);
        voiceWidget.setLayoutY(y);
        voiceWidget.setPrefSize(280, 50);
        voiceWidget.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-border-color: #FF9800; -fx-border-width: 1.5; -fx-background-radius: 25; -fx-border-radius: 25;");
        voiceWidget.setAlignment(Pos.CENTER_LEFT);
        
        // Play button (triangle)
        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(30, 30);
        playBtn.setStyle("-fx-background-color: #FF9800; -fx-background-radius: 15;");
        Label playIcon = new Label("\u25b6");
        playIcon.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        playBtn.getChildren().add(playIcon);
        
        // Animated waveform
        HBox waveform = new HBox(2);
        waveform.setAlignment(Pos.CENTER);
        waveform.setPrefHeight(30);
        HBox.setHgrow(waveform, Priority.ALWAYS);
        
        // Create waveform bars (like in NoteX screenshot)
        List<VBox> bars = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 40; i++) {
            VBox bar = new VBox();
            double height = 5 + random.nextDouble() * 20;
            bar.setPrefSize(2, height);
            bar.setStyle("-fx-background-color: #FF9800;");
            bars.add(bar);
            waveform.getChildren().add(bar);
        }
        
        // Timer label
        Label timerLabel = new Label("0:03");
        timerLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11; -fx-font-weight: bold;");
        
        voiceWidget.getChildren().addAll(playBtn, waveform, timerLabel);
        
        // Animate waveform bars
        final int[] currentBar = {0};
        javafx.animation.Timeline waveAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), e -> {
                // Reset previous bar
                if (currentBar[0] > 0 && currentBar[0] <= bars.size()) {
                    bars.get(currentBar[0] - 1).setStyle("-fx-background-color: #FF9800;");
                }
                // Highlight current bar
                if (currentBar[0] < bars.size()) {
                    bars.get(currentBar[0]).setStyle("-fx-background-color: #FFC107;");
                    currentBar[0]++;
                } else {
                    currentBar[0] = 0;
                }
            })
        );
        waveAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        waveAnimation.play();
        
        // Close button (small x on top right)
        Button closeBtn = new Button("\u00d7");
        closeBtn.setLayoutX(x + 260);
        closeBtn.setLayoutY(y - 10);
        closeBtn.setPrefSize(20, 20);
        closeBtn.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0;");
        
        closeBtn.setOnAction(e -> {
            waveAnimation.stop();
            currentOverlay.getChildren().removeAll(voiceWidget, closeBtn);
        });
        
        // Stop recording and save to canvas after 5 seconds
        javafx.animation.Timeline autoSaveTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> {
                waveAnimation.stop();
                currentOverlay.getChildren().removeAll(voiceWidget, closeBtn);
                
                // Draw permanent waveform widget on canvas
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(x, y, 280, 50, 25, 25);
                gc.setStroke(Color.web("#FF9800"));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(x, y, 280, 50, 25, 25);
                
                // Draw play button
                gc.setFill(Color.web("#FF9800"));
                gc.fillOval(x + 10, y + 10, 30, 30);
                gc.setFill(Color.WHITE);
                gc.setFont(new Font(12));
                gc.fillText("\u25b6", x + 20, y + 30);
                
                // Draw waveform bars
                for (int i = 0; i < 40; i++) {
                    double barHeight = 5 + random.nextDouble() * 20;
                    gc.setFill(Color.web("#FF9800"));
                    gc.fillRect(x + 50 + (i * 4), y + 25 - barHeight / 2, 2, barHeight);
                }
                
                // Draw timer
                gc.setFill(Color.web("#666666"));
                gc.setFont(Font.font("System", FontWeight.BOLD, 11));
                gc.fillText("0:03", x + 240, y + 32);
            })
        );
        autoSaveTimer.play();
        
        currentOverlay.setMouseTransparent(false);
        currentOverlay.getChildren().addAll(voiceWidget, closeBtn);
    }
    
    // ==================== LASER POINTER ====================
    
    private void startLaserPointer(Pane overlay, double x, double y) {
        if (overlay == null) return;
        
        // Create red laser path
        laserPath = new javafx.scene.shape.Path();
        laserPath.setStroke(Color.RED);
        laserPath.setStrokeWidth(3);
        laserPath.setStrokeLineCap(StrokeLineCap.ROUND);
        laserPath.setStrokeLineJoin(StrokeLineJoin.ROUND);
        laserPath.setOpacity(0.8);
        
        // Start path at initial position
        javafx.scene.shape.MoveTo moveTo = new javafx.scene.shape.MoveTo(x, y);
        laserPath.getElements().add(moveTo);
        laserSegments.clear();
        
        overlay.getChildren().add(laserPath);
    }
    
    private void updateLaserPointer(double x, double y) {
        if (laserPath == null) return;
        
        // Add line segment to path
        javafx.scene.shape.LineTo lineTo = new javafx.scene.shape.LineTo(x, y);
        laserPath.getElements().add(lineTo);
        laserSegments.add(lineTo);
    }
    
    private void stopLaserPointer() {
        if (laserPath == null || currentOverlay == null) return;
        
        // Stop any existing fade timer
        if (laserFadeTimer != null) {
            laserFadeTimer.stop();
        }
        
        // Start 2-second fade out and disappear
        final javafx.scene.shape.Path pathToFade = laserPath;
        laserFadeTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                // Remove laser path after 2 seconds
                currentOverlay.getChildren().remove(pathToFade);
            })
        );
        laserFadeTimer.play();
        
        // Fade animation
        javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(
            javafx.util.Duration.seconds(2), pathToFade
        );
        fadeTransition.setFromValue(0.8);
        fadeTransition.setToValue(0.0);
        fadeTransition.play();
        
        laserPath = null;
        laserSegments.clear();
    }
    
    // ==================== MACOS TRACKPAD GESTURES ====================
    
    // PINCH TO ZOOM
    private void handleZoomStarted(javafx.scene.input.ZoomEvent e, Canvas canvas) {
        isPinching = true;
        initialZoomFactor = currentZoomFactor;
        e.consume();
    }
    
    private void handleZoomGesture(javafx.scene.input.ZoomEvent e, Canvas canvas) {
        if (!isPinching) return;
        
        double zoomFactor = e.getZoomFactor();
        currentZoomFactor = initialZoomFactor * zoomFactor;
        
        // Limit zoom between 0.5x and 3x
        currentZoomFactor = Math.max(0.5, Math.min(3.0, currentZoomFactor));
        
        // Apply zoom to canvas
        canvas.setScaleX(currentZoomFactor);
        canvas.setScaleY(currentZoomFactor);
        
        // Update stroke width based on zoom (inverse relationship for consistent visual size)
        double adjustedStrokeWidth = strokeWidth / currentZoomFactor;
        gc.setLineWidth(adjustedStrokeWidth);
        
        e.consume();
    }
    
    private void handleZoomFinished(javafx.scene.input.ZoomEvent e, Canvas canvas) {
        isPinching = false;
        e.consume();
    }
    
    // ROTATION GESTURE
    private void handleRotationStarted(javafx.scene.input.RotateEvent e, Canvas canvas) {
        isRotating = true;
        initialRotation = canvas.getRotate();
        e.consume();
    }
    
    private void handleRotateGesture(javafx.scene.input.RotateEvent e, Canvas canvas) {
        if (!isRotating) return;
        
        double rotation = initialRotation + e.getAngle();
        canvas.setRotate(rotation);
        
        e.consume();
    }
    
    private void handleRotationFinished(javafx.scene.input.RotateEvent e, Canvas canvas) {
        isRotating = false;
        e.consume();
    }
    
    // TWO-FINGER SCROLL
    private void handleScrollStarted(javafx.scene.input.ScrollEvent e) {
        isTwoFingerScrolling = true;
        e.consume();
    }
    
    private void handleScrollGesture(javafx.scene.input.ScrollEvent e, Canvas canvas) {
        // Two-finger scroll for panning (handled by ScrollPane automatically)
        // We can add custom behavior here if needed
        
        // Detect if this is a trackpad scroll (has inertia and touch count)
        if (e.getTouchCount() == 0 && !e.isInertia()) {
            // This is a mouse wheel scroll, not trackpad
            // Could add different behavior for mouse vs trackpad
        }
        
        e.consume();
    }
    
    private void handleScrollFinished(javafx.scene.input.ScrollEvent e) {
        isTwoFingerScrolling = false;
        e.consume();
    }
    
    // SWIPE GESTURES (3-finger swipe)
    private void handleSwipeLeft() {
        // Navigate to next page
        if (currentPageIndex < pages.size() - 1) {
            currentPageIndex++;
            switchToPage(currentPageIndex);
            System.out.println("Swiped left - Next page: " + (currentPageIndex + 1));
        }
    }
    
    private void handleSwipeRight() {
        // Navigate to previous page
        if (currentPageIndex > 0) {
            currentPageIndex--;
            switchToPage(currentPageIndex);
            System.out.println("Swiped right - Previous page: " + (currentPageIndex + 1));
        }
    }
    
    private void handleSwipeUp() {
        // Could add page overview or zoom out
        System.out.println("Swiped up - Could show page overview");
        // For now, scroll to top
        if (pagesScrollPane != null) {
            pagesScrollPane.setVvalue(0.0);
        }
    }
    
    private void handleSwipeDown() {
        // Could add page overview or zoom out
        System.out.println("Swiped down - Could show page overview");
        // For now, scroll to bottom
        if (pagesScrollPane != null) {
            pagesScrollPane.setVvalue(1.0);
        }
    }
    
    private void switchToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < canvases.size()) {
            // Save current page
            if (currentPageIndex >= 0 && currentPageIndex < pages.size()) {
                savePageData(currentPageIndex);
            }
            
            // Switch to new page
            currentPageIndex = pageIndex;
            currentCanvas = canvases.get(pageIndex);
            currentOverlay = overlayPanes.get(pageIndex);
            gc = currentCanvas.getGraphicsContext2D();
            
            // Scroll to the page
            double pagePosition = (double) pageIndex / pages.size();
            if (pagesScrollPane != null) {
                pagesScrollPane.setVvalue(pagePosition);
            }
            
            updatePageInfo();
        }
    }

    @FXML
    private void handleUndo() {
        if (currentCanvas == null || undoStack.isEmpty()) {
            return;
        }
        
        // Save current state to redo stack
        WritableImage currentState = new WritableImage(
            (int) currentCanvas.getWidth(),
            (int) currentCanvas.getHeight()
        );
        currentCanvas.snapshot(null, currentState);
        redoStack.push(currentState);
        
        // Restore previous state
        WritableImage previousState = undoStack.pop();
        gc.drawImage(previousState, 0, 0);
        
        // Update buttons
        updateUndoRedoButtons();
    }

    @FXML
    private void handleRedo() {
        if (currentCanvas == null || redoStack.isEmpty()) {
            return;
        }
        
        // Save current state to undo stack
        WritableImage currentState = new WritableImage(
            (int) currentCanvas.getWidth(),
            (int) currentCanvas.getHeight()
        );
        currentCanvas.snapshot(null, currentState);
        undoStack.push(currentState);
        
        // Restore next state
        WritableImage nextState = redoStack.pop();
        gc.drawImage(nextState, 0, 0);
        
        // Update buttons
        updateUndoRedoButtons();
    }
    
    private void saveToUndoStack() {
        if (currentCanvas == null) {
            return;
        }
        
        WritableImage snapshot = new WritableImage(
            (int) currentCanvas.getWidth(),
            (int) currentCanvas.getHeight()
        );
        currentCanvas.snapshot(null, snapshot);
        undoStack.push(snapshot);
        
        // Limit stack size
        if (undoStack.size() > MAX_UNDO_STACK) {
            undoStack.remove(0);
        }
        
        // Clear redo stack when new action is performed
        redoStack.clear();
        
        updateUndoRedoButtons();
    }
    
    private void updateUndoRedoButtons() {
        if (btnUndo != null) {
            btnUndo.setDisable(undoStack.isEmpty());
        }
        if (btnRedo != null) {
            btnRedo.setDisable(redoStack.isEmpty());
        }
    }

    @FXML
    private void handleSave() {
        saveCurrentPage();
        saveAllPages();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Saved");
        alert.setContentText("All " + pages.size() + " page(s) saved successfully!");
        alert.showAndWait();
    }

    @FXML
    private void handleAddPage() {
        // Save current page if exists
        if (currentPageIndex >= 0 && currentPageIndex < pages.size()) {
            savePageData(currentPageIndex);
        }
        
        // Add new blank page
        pages.add(new PageData());
        int newPageIndex = pages.size() - 1;
        
        // Create canvas for new page
        createPageCanvas(newPageIndex);
        
        // Set as current page
        currentPageIndex = newPageIndex;
        currentCanvas = canvases.get(newPageIndex);
        currentOverlay = overlayPanes.get(newPageIndex);
        gc = currentCanvas.getGraphicsContext2D();
        
        // Scroll to the new page (bottom for vertical layout)
        if (pagesScrollPane != null) {
            pagesScrollPane.setVvalue(1.0); // Scroll to bottom (newest page)
        }
        
        updatePageInfo();
        updatePagesView();
    }

    @FXML
    private void handleBack() {
        NoteXApp.setRoot("views/my_notebooks");
    }
    
    // ==================== PEN OPTIONS DIALOG ====================
    
    private void showPenOptions() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Pen Options");
        
        VBox mainLayout = new VBox(16);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #424242;");
        
        // Title
        Label title = new Label("Pen Options");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        
        // === PEN STYLE ===
        Label styleLabel = new Label("Pen Style:");
        styleLabel.setTextFill(Color.web("#aaaaaa"));
        styleLabel.setFont(Font.font(14));
        
        HBox styleRow = new HBox(8);
        ToggleGroup penStyleGroup = new ToggleGroup();
        String[] styleNames = {"NORMAL", "PENCIL", "HIGHLIGHTER"};
        ToggleButton[] styleButtons = new ToggleButton[3];
        
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            ToggleButton btn = new ToggleButton(styleNames[i]);
            btn.setToggleGroup(penStyleGroup);
            btn.setPrefWidth(120);
            btn.setPrefHeight(40);
            
            PenStyle style = idx == 0 ? PenStyle.NORMAL : idx == 1 ? PenStyle.PENCIL : PenStyle.HIGHLIGHTER;
            if (currentPenStyle == style) {
                btn.setSelected(true);
                btn.setStyle("-fx-background-color: #00A3FF; -fx-text-fill: white; -fx-background-radius: 8;");
            } else {
                btn.setStyle("-fx-background-color: #e8e8e8; -fx-text-fill: #333333; -fx-background-radius: 8;");
            }
            
            btn.setOnAction(e -> {
                currentPenStyle = idx == 0 ? PenStyle.NORMAL : idx == 1 ? PenStyle.PENCIL : PenStyle.HIGHLIGHTER;
                for (ToggleButton b : styleButtons) {
                    if (b.isSelected()) {
                        b.setStyle("-fx-background-color: #00A3FF; -fx-text-fill: white; -fx-background-radius: 8;");
                    } else {
                        b.setStyle("-fx-background-color: #e8e8e8; -fx-text-fill: #333333; -fx-background-radius: 8;");
                    }
                }
                applyPenSettings();
            });
            
            styleButtons[i] = btn;
            styleRow.getChildren().add(btn);
        }
        
        // === LINE STYLE ===
        Label lineLabel = new Label("Line Style:");
        lineLabel.setTextFill(Color.web("#aaaaaa"));
        lineLabel.setFont(Font.font(14));
        
        HBox lineRow = new HBox(20);
        ToggleGroup lineStyleGroup = new ToggleGroup();
        
        RadioButton normalLine = new RadioButton("━━━  Normal");
        normalLine.setTextFill(Color.WHITE);
        normalLine.setToggleGroup(lineStyleGroup);
        normalLine.setSelected(!isDashedLine);
        normalLine.setOnAction(e -> {
            isDashedLine = false;
            applyPenSettings();
        });
        
        RadioButton dashedLine = new RadioButton("- - -  Dashed");
        dashedLine.setTextFill(Color.WHITE);
        dashedLine.setToggleGroup(lineStyleGroup);
        dashedLine.setSelected(isDashedLine);
        dashedLine.setOnAction(e -> {
            isDashedLine = true;
            applyPenSettings();
        });
        
        lineRow.getChildren().addAll(normalLine, dashedLine);
        
        // === COLOR PICKER ===
        Label colorLabel = new Label("Color:");
        colorLabel.setTextFill(Color.web("#aaaaaa"));
        colorLabel.setFont(Font.font(14));
        
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(10);
        colorGrid.setVgap(10);
        
        StackPane[] colorContainers = new StackPane[COLORS.length];
        
        for (int i = 0; i < COLORS.length; i++) {
            final int idx = i;
            final Color color = COLORS[i];
            
            StackPane container = new StackPane();
            container.setPrefSize(50, 50);
            container.setStyle(getColorContainerStyle(color.equals(currentColor)));
            
            Region colorBox = new Region();
            colorBox.setPrefSize(42, 42);
            colorBox.setStyle("-fx-background-color: " + toHexString(color) + "; -fx-background-radius: 4;");
            
            container.getChildren().add(colorBox);
            container.setOnMouseClicked(e -> {
                currentColor = color;
                for (int j = 0; j < colorContainers.length; j++) {
                    colorContainers[j].setStyle(getColorContainerStyle(COLORS[j].equals(currentColor)));
                }
                applyPenSettings();
            });
            
            colorContainers[idx] = container;
            colorGrid.add(container, i % 6, i / 6);
        }
        
        // === STROKE WIDTH ===
        Label widthLabel = new Label("Stroke Width:");
        widthLabel.setTextFill(Color.web("#aaaaaa"));
        widthLabel.setFont(Font.font(14));
        
        Slider widthSlider = new Slider(1, 20, strokeWidth);
        widthSlider.setShowTickLabels(false);
        widthSlider.setStyle("-fx-control-inner-background: #666666;");
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            strokeWidth = newVal.doubleValue();
            applyPenSettings();
        });
        
        // === DONE BUTTON ===
        Button doneBtn = new Button("DONE");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00A3FF; -fx-font-weight: bold;");
        doneBtn.setOnAction(e -> dialog.close());
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(doneBtn);
        
        mainLayout.getChildren().addAll(
            title,
            styleLabel, styleRow,
            lineLabel, lineRow,
            colorLabel, colorGrid,
            widthLabel, widthSlider,
            buttonRow
        );
        
        Scene scene = new Scene(mainLayout, 420, 480);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
    
    private String getColorContainerStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: #00A3FF; -fx-background-radius: 6; -fx-padding: 4;";
        } else {
            return "-fx-background-color: #666666; -fx-background-radius: 6; -fx-padding: 4;";
        }
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    // ==================== SHAPE OPTIONS DIALOG ====================
    
    private void showShapeOptions() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Shape Options");
        
        VBox mainLayout = new VBox(16);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #424242;");
        
        // === TITLE ===
        Label title = new Label("Shape Options");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System Bold", 20));
        
        // === SHAPE SELECTION ===
        Label shapeLabel = new Label("Select Shape:");
        shapeLabel.setTextFill(Color.web("#aaaaaa"));
        shapeLabel.setFont(Font.font(14));
        
        GridPane shapeGrid = new GridPane();
        shapeGrid.setHgap(10);
        shapeGrid.setVgap(10);
        
        ShapeType[] shapes = ShapeType.values();
        String[] shapeLabels = {"Rect", "Circle", "Triangle", "Line", "Arrow", "Pent", "Star", "Hex", "Diamond", "Oval", "Right △", "Para", "Cross", "Heart"};
        
        ToggleGroup shapeGroup = new ToggleGroup();
        
        for (int i = 0; i < shapes.length; i++) {
            final ShapeType shape = shapes[i];
            
            ToggleButton shapeBtn = new ToggleButton(shapeLabels[i]);
            shapeBtn.setToggleGroup(shapeGroup);
            shapeBtn.setPrefSize(90, 60);
            shapeBtn.setStyle("-fx-background-color: " + (shape == currentShapeType ? "#2196F3" : "#666666") + 
                            "; -fx-text-fill: white; -fx-font-size: 12;");
            shapeBtn.setSelected(shape == currentShapeType);
            shapeBtn.setOnAction(e -> {
                currentShapeType = shape;
                for (var node : shapeGrid.getChildren()) {
                    if (node instanceof ToggleButton) {
                        ((ToggleButton) node).setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-font-size: 12;");
                    }
                }
                shapeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12;");
            });
            
            shapeGrid.add(shapeBtn, i % 4, i / 4);
        }
        
        // === STYLE SELECTION ===
        Label styleLabel = new Label("Style:");
        styleLabel.setTextFill(Color.web("#aaaaaa"));
        styleLabel.setFont(Font.font(14));
        
        HBox styleRow = new HBox(16);
        styleRow.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup styleGroup = new ToggleGroup();
        
        RadioButton strokeBtn = new RadioButton("━  Stroke (Normal)");
        strokeBtn.setTextFill(Color.WHITE);
        strokeBtn.setToggleGroup(styleGroup);
        strokeBtn.setSelected(!isShapeFilled && !isShapeDashed);
        strokeBtn.setOnAction(e -> {
            isShapeFilled = false;
            isShapeDashed = false;
        });
        
        RadioButton dashedBtn = new RadioButton("- - -  Stroke (Dashed)");
        dashedBtn.setTextFill(Color.WHITE);
        dashedBtn.setToggleGroup(styleGroup);
        dashedBtn.setSelected(!isShapeFilled && isShapeDashed);
        dashedBtn.setOnAction(e -> {
            isShapeFilled = false;
            isShapeDashed = true;
        });
        
        RadioButton fillBtn = new RadioButton("▬  Fill");
        fillBtn.setTextFill(Color.WHITE);
        fillBtn.setToggleGroup(styleGroup);
        fillBtn.setSelected(isShapeFilled);
        fillBtn.setOnAction(e -> {
            isShapeFilled = true;
            isShapeDashed = false;
        });
        
        VBox styleColumn = new VBox(8);
        styleColumn.getChildren().addAll(strokeBtn, dashedBtn, fillBtn);
        styleRow.getChildren().add(styleColumn);
        
        // === COLOR PICKER ===
        Label colorLabel = new Label("Color:");
        colorLabel.setTextFill(Color.web("#aaaaaa"));
        colorLabel.setFont(Font.font(14));
        
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(10);
        colorGrid.setVgap(10);
        
        StackPane[] colorContainers = new StackPane[COLORS.length];
        
        for (int i = 0; i < COLORS.length; i++) {
            final int idx = i;
            final Color color = COLORS[i];
            
            StackPane container = new StackPane();
            container.setPrefSize(50, 50);
            container.setStyle(getColorContainerStyle(color.equals(currentColor)));
            
            Region colorBox = new Region();
            colorBox.setPrefSize(42, 42);
            colorBox.setStyle("-fx-background-color: " + toHexString(color) + "; -fx-background-radius: 4;");
            
            container.getChildren().add(colorBox);
            container.setOnMouseClicked(e -> {
                currentColor = color;
                for (int j = 0; j < colorContainers.length; j++) {
                    colorContainers[j].setStyle(getColorContainerStyle(COLORS[j].equals(currentColor)));
                }
            });
            
            colorContainers[idx] = container;
            colorGrid.add(container, i % 6, i / 6);
        }
        
        // === STROKE WIDTH ===
        Label widthLabel = new Label("Stroke Width: 5px");
        widthLabel.setTextFill(Color.web("#aaaaaa"));
        widthLabel.setFont(Font.font(14));
        
        Slider widthSlider = new Slider(1, 20, strokeWidth);
        widthSlider.setShowTickLabels(false);
        widthSlider.setStyle("-fx-control-inner-background: #666666;");
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            strokeWidth = newVal.doubleValue();
            widthLabel.setText("Stroke Width: " + Math.round(strokeWidth) + "px");
        });
        
        // === DONE BUTTON ===
        Button doneBtn = new Button("DONE");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-font-size: 14;");
        doneBtn.setOnAction(e -> dialog.close());
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(doneBtn);
        
        mainLayout.getChildren().addAll(
            title,
            shapeLabel, shapeGrid,
            styleLabel, styleRow,
            colorLabel, colorGrid,
            widthLabel, widthSlider,
            buttonRow
        );
        
        Scene scene = new Scene(mainLayout, 420, 650);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
    
    // ==================== ERASER OPTIONS DIALOG ====================
    
    private void showEraserOptions() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Eraser Options");
        
        VBox mainLayout = new VBox(16);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #424242;");
        
        // === TITLE ===
        Label title = new Label("Eraser Options");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System Bold", 20));
        
        // === ERASER TYPE ===
        Label typeLabel = new Label("Eraser Type:");
        typeLabel.setTextFill(Color.web("#aaaaaa"));
        typeLabel.setFont(Font.font(14));
        
        VBox typeRow = new VBox(12);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup eraserGroup = new ToggleGroup();
        
        RadioButton fillLineBtn = new RadioButton("Fill Line (Erase entire strokes)");
        fillLineBtn.setTextFill(Color.WHITE);
        fillLineBtn.setToggleGroup(eraserGroup);
        fillLineBtn.setSelected(currentEraserType == EraserType.FILL_LINE);
        fillLineBtn.setOnAction(e -> currentEraserType = EraserType.FILL_LINE);
        
        RadioButton partialBtn = new RadioButton("Partially (Erase only where you force click and hold)");
        partialBtn.setTextFill(Color.WHITE);
        partialBtn.setToggleGroup(eraserGroup);
        partialBtn.setSelected(currentEraserType == EraserType.PARTIAL);
        partialBtn.setOnAction(e -> currentEraserType = EraserType.PARTIAL);
        
        typeRow.getChildren().addAll(fillLineBtn, partialBtn);
        
        // === ERASER SIZE (for partial eraser) ===
        Label sizeLabel = new Label("Eraser Size: " + Math.round(strokeWidth) + "px");
        sizeLabel.setTextFill(Color.web("#aaaaaa"));
        sizeLabel.setFont(Font.font(14));
        
        Slider sizeSlider = new Slider(10, 50, Math.max(10, strokeWidth));
        sizeSlider.setShowTickLabels(false);
        sizeSlider.setStyle("-fx-control-inner-background: #666666;");
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            strokeWidth = newVal.doubleValue();
            sizeLabel.setText("Eraser Size: " + Math.round(strokeWidth) + "px");
        });
        
        // === DONE BUTTON ===
        Button doneBtn = new Button("DONE");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-font-size: 14;");
        doneBtn.setOnAction(e -> dialog.close());
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(doneBtn);
        
        mainLayout.getChildren().addAll(
            title,
            typeLabel, typeRow,
            sizeLabel, sizeSlider,
            buttonRow
        );
        
        Scene scene = new Scene(mainLayout, 420, 280);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
    
    // ==================== TEXT OPTIONS DIALOG ====================
    
    private void showTextOptions() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Text Options");
        
        VBox mainLayout = new VBox(16);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #424242;");
        
        // === TITLE ===
        Label title = new Label("Text Options");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System Bold", 20));
        
        // === COLOR PICKER ===
        Label colorLabel = new Label("Text Color:");
        colorLabel.setTextFill(Color.web("#aaaaaa"));
        colorLabel.setFont(Font.font(14));
        
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(10);
        colorGrid.setVgap(10);
        
        StackPane[] colorContainers = new StackPane[COLORS.length];
        
        for (int i = 0; i < COLORS.length; i++) {
            final int idx = i;
            final Color color = COLORS[i];
            
            StackPane container = new StackPane();
            container.setPrefSize(50, 50);
            container.setStyle(getColorContainerStyle(color.equals(currentColor)));
            
            Region colorBox = new Region();
            colorBox.setPrefSize(42, 42);
            colorBox.setStyle("-fx-background-color: " + toHexString(color) + "; -fx-background-radius: 4;");
            
            container.getChildren().add(colorBox);
            container.setOnMouseClicked(e -> {
                currentColor = color;
                for (int j = 0; j < colorContainers.length; j++) {
                    colorContainers[j].setStyle(getColorContainerStyle(COLORS[j].equals(currentColor)));
                }
            });
            
            colorContainers[idx] = container;
            colorGrid.add(container, i % 6, i / 6);
        }
        
        // === DONE BUTTON ===
        Button doneBtn = new Button("DONE");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-font-size: 14;");
        doneBtn.setOnAction(e -> dialog.close());
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(doneBtn);
        
        mainLayout.getChildren().addAll(
            title,
            colorLabel, colorGrid,
            buttonRow
        );
        
        Scene scene = new Scene(mainLayout, 420, 280);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
    
    // ==================== STICKY NOTE OPTIONS DIALOG ====================
    
    private void showStickyNoteOptions() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Sticky Note Options");
        
        VBox mainLayout = new VBox(16);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #424242;");
        
        // === TITLE ===
        Label title = new Label("Sticky Note Options");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System Bold", 20));
        
        Label infoLabel = new Label("Click anywhere on the canvas to place a sticky note.");
        infoLabel.setTextFill(Color.web("#aaaaaa"));
        infoLabel.setFont(Font.font(14));
        infoLabel.setWrapText(true);
        
        // === DONE BUTTON ===
        Button doneBtn = new Button("DONE");
        doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-font-size: 14;");
        doneBtn.setOnAction(e -> dialog.close());
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(doneBtn);
        
        mainLayout.getChildren().addAll(
            title,
            infoLabel,
            buttonRow
        );
        
        Scene scene = new Scene(mainLayout, 420, 200);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
    
    private void savePageData(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < canvases.size()) {
            Canvas canvas = canvases.get(pageIndex);
            WritableImage snapshot = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
            canvas.snapshot(null, snapshot);
            
            int width = (int)snapshot.getWidth();
            int height = (int)snapshot.getHeight();
            PixelReader pixelReader = snapshot.getPixelReader();
            
            ByteBuffer buffer = ByteBuffer.allocate(8 + width * height * 4);
            buffer.putInt(width);
            buffer.putInt(height);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = pixelReader.getArgb(x, y);
                    buffer.putInt(argb);
                }
            }
            
            String base64Data = Base64.getEncoder().encodeToString(buffer.array());
            pages.get(pageIndex).setCanvasData(base64Data);
        }
    }
    
    private void loadPageDataOntoCanvas(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < canvases.size()) {
            String canvasData = pages.get(pageIndex).getCanvasData();
            
            if (canvasData != null && !canvasData.isEmpty()) {
                try {
                    Canvas canvas = canvases.get(pageIndex);
                    GraphicsContext context = canvas.getGraphicsContext2D();
                    
                    byte[] data = Base64.getDecoder().decode(canvasData);
                    ByteBuffer buffer = ByteBuffer.wrap(data);
                    
                    int width = buffer.getInt();
                    int height = buffer.getInt();
                    
                    WritableImage image = new WritableImage(width, height);
                    PixelWriter pixelWriter = image.getPixelWriter();
                    
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb = buffer.getInt();
                            pixelWriter.setArgb(x, y, argb);
                        }
                    }
                    
                    context.drawImage(image, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ==================== SAVE/LOAD METHODS ====================
    
    private void saveCurrentPage() {
        if (currentPageIndex >= 0 && currentPageIndex < pages.size()) {
            savePageData(currentPageIndex);
        }
    }
    
    private void saveAllPages() {
        // Save all canvases to page data
        for (int i = 0; i < pages.size(); i++) {
            savePageData(i);
        }
        
        // Save each page to database
        for (int i = 0; i < pages.size(); i++) {
            String pageTitle = currentNotebook.getTitle() + " - Page " + (i + 1);
            String content = pages.get(i).getCanvasData();
            
            if (i == 0 && databaseHelper.getNotebookPages(currentNotebook.getId()).size() == 0) {
                // Add first page
                databaseHelper.addPage(currentNotebook.getId(), pageTitle, content);
            } else {
                // Update or add additional pages
                List<com.example.notex_desktop.models.Page> existingPages = 
                    databaseHelper.getNotebookPages(currentNotebook.getId());
                if (i < existingPages.size()) {
                    databaseHelper.updatePage(existingPages.get(i).getId(), pageTitle, content);
                } else {
                    databaseHelper.addPage(currentNotebook.getId(), pageTitle, content);
                }
            }
        }
    }
    
    private void loadExistingPages() {
        List<com.example.notex_desktop.models.Page> existingPages = 
            databaseHelper.getNotebookPages(currentNotebook.getId());
        
        if (!existingPages.isEmpty()) {
            pages.clear();
            for (com.example.notex_desktop.models.Page page : existingPages) {
                PageData pageData = new PageData();
                pageData.setCanvasData(page.getContent());
                pages.add(pageData);
            }
            currentPageIndex = 0;
            
            // Load pages onto canvases
            for (int i = 0; i < pages.size(); i++) {
                if (i < canvases.size()) {
                    loadPageDataOntoCanvas(i);
                }
            }
        }
    }
    


    // Inner class to store page data
    private static class PageData {
        private String canvasData;
        
        public PageData() {
            this.canvasData = "";
        }
        
        public String getCanvasData() {
            return canvasData;
        }
        
        public void setCanvasData(String canvasData) {
            this.canvasData = canvasData;
        }
    }
}
