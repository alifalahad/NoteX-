package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DocumentScannerController implements Initializable {

    @FXML private VBox documentsContainer;
    @FXML private Button btnCamera;

    private Path documentsPath;
    private List<File> capturedImages = new ArrayList<>();
    private Process cameraProcess;
    private volatile boolean isCameraRunning = false;
    private ImageView cameraPreview;
    private AnimationTimer cameraTimer;
    
    private static final String[] SUPPORTED_IMAGES = {"*.png", "*.jpg", "*.jpeg"};
    private static final String[] RATIOS = {"3:4", "1:1", "16:9", "Full"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDocumentsDirectory();
        loadDocuments();
    }

    private void setupDocumentsDirectory() {
        String userHome = System.getProperty("user.home");
        documentsPath = Paths.get(userHome, ".notex_desktop", "documents");
        
        try {
            Files.createDirectories(documentsPath);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to create documents directory");
        }
    }

    private void loadDocuments() {
        documentsContainer.getChildren().clear();
        
        try {
            List<Path> documents = Files.walk(documentsPath, 1)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".pdf") || fileName.endsWith(".txt") || 
                           fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
                })
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

            if (documents.isEmpty()) {
                showEmptyState();
            } else {
                for (Path docPath : documents) {
                    documentsContainer.getChildren().add(createDocumentCard(docPath));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load documents");
        }
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60, 20, 20, 20));
        
        Label emptyIcon = new Label("📄");
        emptyIcon.setStyle("-fx-font-size: 64;");
        
        Label emptyTitle = new Label("No Documents Yet");
        emptyTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        emptyTitle.setStyle("-fx-text-fill: #666666;");
        
        Label emptySubtitle = new Label("Tap the camera button to capture your first document");
        emptySubtitle.setFont(Font.font("System", 14));
        emptySubtitle.setStyle("-fx-text-fill: #999999;");
        emptySubtitle.setWrapText(true);
        emptySubtitle.setMaxWidth(300);
        emptySubtitle.setAlignment(Pos.CENTER);
        
        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle);
        documentsContainer.getChildren().add(emptyState);
    }

    private HBox createDocumentCard(Path docPath) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
        
        String fileName = docPath.getFileName().toString().toLowerCase();
        boolean isPdf = fileName.endsWith(".pdf");
        boolean isTxt = fileName.endsWith(".txt");
        
        String iconEmoji = isPdf ? "📕" : (isTxt ? "📘" : "🖼️");
        Label icon = new Label(iconEmoji);
        icon.setStyle("-fx-font-size: 40;");
        
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label nameLabel = new Label(docPath.getFileName().toString());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #212121;");
        
        try {
            long fileSize = Files.size(docPath);
            String sizeStr = formatFileSize(fileSize);
            
            String fileType = isPdf ? "PDF" : (isTxt ? "TXT" : "IMAGE");
            String lastModified = new SimpleDateFormat("MMM dd, yyyy")
                .format(new Date(Files.getLastModifiedTime(docPath).toMillis()));
            
            Label detailsLabel = new Label(fileType + " • 1 page • " + sizeStr);
            detailsLabel.setFont(Font.font("System", 12));
            detailsLabel.setStyle("-fx-text-fill: #666666;");
            
            Label dateLabel = new Label(lastModified);
            dateLabel.setFont(Font.font("System", 11));
            dateLabel.setStyle("-fx-text-fill: #999999;");
            
            info.getChildren().addAll(nameLabel, detailsLabel, dateLabel);
        } catch (IOException e) {
            info.getChildren().add(nameLabel);
        }
        
        card.getChildren().addAll(icon, info);
        card.setOnMouseClicked(e -> openDocument(docPath));
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #2196F3; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;"));
        
        return card;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void openDocument(Path docPath) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(docPath.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open document");
        }
    }

    @FXML
    private void handleCamera() {
        capturedImages.clear();
        showCameraDialog();
    }

    private void showCameraDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Document Scanner");
        dialog.setWidth(800);
        dialog.setHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2C2C2C;");

        // Top bar with filters
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1E1E1E;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            stopCamera();
            dialog.close();
        });

        Label titleLabel = new Label("Document Scanner");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);

        Button filterL = new Button("L");
        Button filterP = new Button("P");
        Button ratioBtn = new Button("3:4");

        AtomicReference<String> selectedOrientation = new AtomicReference<>("L");
        AtomicInteger ratioIndex = new AtomicInteger(0);
        
        // L is selected by default (green), P is gray
        filterL.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
        filterP.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
        // Ratio button is ALWAYS orange
        ratioBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 60; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");

        filterL.setOnAction(e -> {
            selectedOrientation.set("L");
            filterL.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
            filterP.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
        });

        filterP.setOnAction(e -> {
            selectedOrientation.set("P");
            filterP.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
            filterL.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-min-width: 50; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
        });

        // Ratio button cycles through ratios (always orange)
        ratioBtn.setOnAction(e -> {
            int nextIndex = (ratioIndex.get() + 1) % RATIOS.length;
            ratioIndex.set(nextIndex);
            ratioBtn.setText(RATIOS[nextIndex]);
            // Keep orange color
            ratioBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 60; -fx-min-height: 32; -fx-background-radius: 5; -fx-font-weight: bold;");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button starBtn = new Button("⭐");
        starBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18; -fx-cursor: hand;");

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18; -fx-cursor: hand;");

        topBar.getChildren().addAll(closeBtn, titleLabel, filterL, filterP, ratioBtn, spacer, starBtn, refreshBtn);

        // Center - Camera view
        StackPane centerPane = new StackPane();
        centerPane.setStyle("-fx-background-color: #000000;");

        cameraPreview = new ImageView();
        cameraPreview.setPreserveRatio(true);
        cameraPreview.setFitWidth(640);
        cameraPreview.setFitHeight(480);

        Label cameraStatus = new Label("📷 Starting camera...");
        cameraStatus.setFont(Font.font("System", 18));
        cameraStatus.setTextFill(Color.web("#999999"));
        cameraStatus.setAlignment(Pos.CENTER);

        centerPane.getChildren().addAll(cameraStatus, cameraPreview);

        // Bottom bar with thumbnails and controls
        VBox bottomSection = new VBox(10);
        bottomSection.setStyle("-fx-background-color: #1E1E1E;");
        bottomSection.setPadding(new Insets(10));

        HBox thumbnailContainer = new HBox(10);
        thumbnailContainer.setAlignment(Pos.CENTER_LEFT);
        thumbnailContainer.setPadding(new Insets(5));
        thumbnailContainer.setMinHeight(100);

        ScrollPane thumbnailScroll = new ScrollPane(thumbnailContainer);
        thumbnailScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        thumbnailScroll.setFitToHeight(true);
        thumbnailScroll.setPrefHeight(110);

        // Bottom controls
        HBox bottomControls = new HBox(20);
        bottomControls.setAlignment(Pos.CENTER);
        bottomControls.setPadding(new Insets(10));

        // Capture button (green)
        Button captureBtn = new Button("📷");
        captureBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 70; -fx-min-height: 70; -fx-background-radius: 50%; -fx-font-size: 28; -fx-cursor: hand;");
        
        // Done button (orange) - for creating PDF
        Button doneBtn = new Button("💾 Done");
        doneBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 120; -fx-min-height: 45; -fx-background-radius: 22; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand;");
        doneBtn.setVisible(false);
        doneBtn.setOnAction(e -> {
            stopCamera();
            createPdfFromImagesWithDialog();
            dialog.close();
        });

        captureBtn.setOnAction(e -> {
            captureFromCamera(thumbnailContainer, doneBtn, cameraStatus);
        });

        // Right side controls
        VBox rightControls = new VBox(8);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        
        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-min-width: 35; -fx-min-height: 35; -fx-background-radius: 5; -fx-font-size: 16;");
        
        Button minusBtn = new Button("-");
        minusBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-min-width: 35; -fx-min-height: 35; -fx-background-radius: 5; -fx-font-size: 16;");
        
        Label oneToOneLabel = new Label("1:1");
        oneToOneLabel.setStyle("-fx-background-color: white; -fx-text-fill: #673AB7; -fx-padding: 5 8; -fx-background-radius: 3; -fx-font-weight: bold;");
        
        Button cropBtn = new Button("⬜");
        cropBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-min-width: 35; -fx-min-height: 35; -fx-background-radius: 5; -fx-font-size: 14;");

        rightControls.getChildren().addAll(plusBtn, minusBtn, oneToOneLabel, cropBtn);

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox captureRow = new HBox(15);
        captureRow.setAlignment(Pos.CENTER);
        captureRow.getChildren().addAll(leftSpacer, captureBtn, doneBtn, rightSpacer, rightControls);

        bottomSection.getChildren().addAll(thumbnailScroll, captureRow);

        root.setTop(topBar);
        root.setCenter(centerPane);
        root.setBottom(bottomSection);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        
        // Start camera
        startCamera(cameraStatus);
        
        dialog.setOnCloseRequest(e -> stopCamera());
        dialog.showAndWait();
    }

    private void startCamera(Label statusLabel) {
        new Thread(() -> {
            try {
                // Check if imagesnap is available (macOS webcam tool)
                ProcessBuilder checkBuilder = new ProcessBuilder("which", "imagesnap");
                Process checkProcess = checkBuilder.start();
                int checkResult = checkProcess.waitFor();
                
                if (checkResult == 0) {
                    // imagesnap is available, use it for live preview
                    isCameraRunning = true;
                    Platform.runLater(() -> statusLabel.setText("📷 Camera ready - Click capture"));
                    
                    // Create temp directory for camera frames
                    Path tempDir = Files.createTempDirectory("notex_camera");
                    Path framePath = tempDir.resolve("frame.jpg");
                    
                    cameraTimer = new AnimationTimer() {
                        private long lastUpdate = 0;
                        
                        @Override
                        public void handle(long now) {
                            if (now - lastUpdate >= 100_000_000L && isCameraRunning) { // ~10 FPS
                                lastUpdate = now;
                                try {
                                    // Capture frame
                                    ProcessBuilder pb = new ProcessBuilder("imagesnap", "-q", "-w", "0.1", framePath.toString());
                                    Process p = pb.start();
                                    p.waitFor();
                                    
                                    if (Files.exists(framePath)) {
                                        javafx.scene.image.Image img = new javafx.scene.image.Image(framePath.toUri().toString() + "?" + System.currentTimeMillis());
                                        cameraPreview.setImage(img);
                                    }
                                } catch (Exception ex) {
                                    // Ignore frame errors
                                }
                            }
                        }
                    };
                    Platform.runLater(() -> cameraTimer.start());
                } else {
                    // No imagesnap, show message
                    Platform.runLater(() -> {
                        statusLabel.setText("📷 Click capture to select image\n(Install 'imagesnap' for live camera)");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("📷 Click capture to select image");
                });
            }
        }).start();
    }

    private void stopCamera() {
        isCameraRunning = false;
        if (cameraTimer != null) {
            cameraTimer.stop();
        }
        if (cameraProcess != null && cameraProcess.isAlive()) {
            cameraProcess.destroy();
        }
    }

    private void captureFromCamera(HBox thumbnailContainer, Button doneBtn, Label statusLabel) {
        try {
            // Try to capture using imagesnap first
            ProcessBuilder checkBuilder = new ProcessBuilder("which", "imagesnap");
            Process checkProcess = checkBuilder.start();
            int checkResult = checkProcess.waitFor();
            
            if (checkResult == 0) {
                // Use imagesnap to capture
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
                Path tempImage = Files.createTempFile("capture_" + timestamp, ".jpg");
                
                ProcessBuilder pb = new ProcessBuilder("imagesnap", "-q", tempImage.toString());
                Process p = pb.start();
                p.waitFor();
                
                if (Files.exists(tempImage) && Files.size(tempImage) > 0) {
                    File capturedFile = tempImage.toFile();
                    capturedImages.add(capturedFile);
                    addThumbnail(thumbnailContainer, capturedFile, capturedImages.size(), doneBtn);
                    
                    // Show Done button
                    doneBtn.setVisible(true);
                    doneBtn.setText("💾 Done (" + capturedImages.size() + ")");
                    return;
                }
            }
            
            // Fallback to file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", SUPPORTED_IMAGES)
            );

            File selectedFile = fileChooser.showOpenDialog(btnCamera.getScene().getWindow());
            if (selectedFile != null) {
                capturedImages.add(selectedFile);
                addThumbnail(thumbnailContainer, selectedFile, capturedImages.size(), doneBtn);
                
                // Show Done button
                doneBtn.setVisible(true);
                doneBtn.setText("💾 Done (" + capturedImages.size() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", SUPPORTED_IMAGES)
            );

            File selectedFile = fileChooser.showOpenDialog(btnCamera.getScene().getWindow());
            if (selectedFile != null) {
                capturedImages.add(selectedFile);
                addThumbnail(thumbnailContainer, selectedFile, capturedImages.size(), doneBtn);
                
                doneBtn.setVisible(true);
                doneBtn.setText("💾 Done (" + capturedImages.size() + ")");
            }
        }
    }

    private void addThumbnail(HBox container, File imageFile, int index, Button doneBtn) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage != null) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile.toURI().toString(), 80, 80, true, true);

                StackPane thumbStack = new StackPane();
                thumbStack.setPrefSize(90, 90);
                thumbStack.setStyle("-fx-background-color: #3C3C3C; -fx-background-radius: 5;");

                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(80);
                imageView.setFitHeight(80);
                imageView.setPreserveRatio(true);

                Button deleteBtn = new Button("✕");
                deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-min-width: 22; -fx-min-height: 22; -fx-background-radius: 11; -fx-font-size: 10; -fx-cursor: hand;");
                StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
                StackPane.setMargin(deleteBtn, new Insets(3));

                deleteBtn.setOnAction(e -> {
                    capturedImages.remove(imageFile);
                    container.getChildren().remove(thumbStack);
                    // Update done button
                    if (capturedImages.isEmpty()) {
                        doneBtn.setVisible(false);
                    } else {
                        doneBtn.setText("💾 Done (" + capturedImages.size() + ")");
                    }
                });

                Label indexLabel = new Label(String.valueOf(index));
                indexLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 3 6; -fx-background-radius: 3; -fx-font-weight: bold; -fx-font-size: 11;");
                StackPane.setAlignment(indexLabel, Pos.BOTTOM_LEFT);
                StackPane.setMargin(indexLabel, new Insets(3));

                thumbStack.getChildren().addAll(imageView, deleteBtn, indexLabel);
                container.getChildren().add(thumbStack);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void createPdfFromImagesWithDialog() {
        if (capturedImages.isEmpty()) {
            showError("No images to create PDF");
            return;
        }

        // Ask for PDF name
        TextInputDialog nameDialog = new TextInputDialog("Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        nameDialog.setTitle("Save Document");
        nameDialog.setHeaderText("Enter document name");
        nameDialog.setContentText("Name:");
        
        Optional<String> result = nameDialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            return; // User cancelled or entered empty name
        }
        
        String baseName = result.get().trim();
        // Remove .pdf extension if user added it
        if (baseName.toLowerCase().endsWith(".pdf")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        createPdfFromImages(baseName);
    }
    
    private void createPdfFromImages(String baseName) {
        if (capturedImages.isEmpty()) {
            showError("No images to create PDF");
            return;
        }

        try {
            String pdfFileName = baseName + ".pdf";
            String txtFileName = baseName + ".txt";
            Path pdfPath = documentsPath.resolve(pdfFileName);
            Path txtPath = documentsPath.resolve(txtFileName);
            
            // Use macOS native 'sips' and 'cupsfilter' or Python to create PDF
            // Try using Python with Pillow which is commonly available on macOS
            boolean success = createPdfWithMacOS(pdfPath.toFile(), capturedImages);
            
            if (!success) {
                // Fallback: Save as image collection
                String imgFolder = baseName;
                Path folderPath = documentsPath.resolve(imgFolder);
                Files.createDirectories(folderPath);
                
                for (int i = 0; i < capturedImages.size(); i++) {
                    Path destPath = folderPath.resolve("page_" + (i + 1) + ".jpg");
                    Files.copy(capturedImages.get(i).toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                showSuccess("Images saved to folder:\n" + imgFolder + "\n(" + capturedImages.size() + " images)");
                loadDocuments();
                return;
            }
            
            // Perform OCR on images and create text file
            performOCR(capturedImages, txtPath.toFile());
            
            // Reload documents
            loadDocuments();
            
            String message = "Documents created successfully!\n\nPDF: " + pdfFileName + "\nText: " + txtFileName + "\n(" + capturedImages.size() + " pages)";
            showSuccess(message);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to create PDF: " + e.getMessage());
        }
    }
    
    private boolean createPdfWithMacOS(File outputFile, List<File> images) {
        try {
            // Method 1: Try using Python with img2pdf (commonly available)
            StringBuilder pythonScript = new StringBuilder();
            pythonScript.append("import subprocess, sys\n");
            pythonScript.append("try:\n");
            pythonScript.append("    from PIL import Image\n");
            pythonScript.append("except ImportError:\n");
            pythonScript.append("    sys.exit(1)\n");
            pythonScript.append("images = []\n");
            
            for (File img : images) {
                pythonScript.append("images.append(Image.open('").append(img.getAbsolutePath().replace("'", "\\'")).append("').convert('RGB'))\n");
            }
            
            pythonScript.append("if images:\n");
            pythonScript.append("    images[0].save('").append(outputFile.getAbsolutePath().replace("'", "\\'")).append("', save_all=True, append_images=images[1:] if len(images) > 1 else [])\n");
            pythonScript.append("    print('SUCCESS')\n");
            
            // Write script to temp file
            Path scriptPath = Files.createTempFile("create_pdf_", ".py");
            Files.writeString(scriptPath, pythonScript.toString());
            
            // Execute Python script
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            int exitCode = p.waitFor();
            Files.deleteIfExists(scriptPath);
            
            if (exitCode == 0 && output.toString().contains("SUCCESS")) {
                return true;
            }
            
            // Method 2: Try using sips to create individual PDFs and combine
            return createPdfWithSips(outputFile, images);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean createPdfWithSips(File outputFile, List<File> images) {
        try {
            // Create temp directory for individual PDFs
            Path tempDir = Files.createTempDirectory("notex_pdf_");
            List<Path> pdfPages = new ArrayList<>();
            
            for (int i = 0; i < images.size(); i++) {
                Path pdfPage = tempDir.resolve("page_" + i + ".pdf");
                
                // Use sips to convert image to PDF
                ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "pdf", 
                    images.get(i).getAbsolutePath(), "--out", pdfPage.toString());
                Process p = pb.start();
                int exitCode = p.waitFor();
                
                if (exitCode == 0 && Files.exists(pdfPage)) {
                    pdfPages.add(pdfPage);
                }
            }
            
            if (pdfPages.isEmpty()) {
                return false;
            }
            
            if (pdfPages.size() == 1) {
                // Just copy the single PDF
                Files.copy(pdfPages.get(0), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Use Python to merge PDFs (PyPDF2 or built-in methods)
                StringBuilder mergeScript = new StringBuilder();
                mergeScript.append("import subprocess\n");
                mergeScript.append("pdf_files = [");
                for (int i = 0; i < pdfPages.size(); i++) {
                    if (i > 0) mergeScript.append(", ");
                    mergeScript.append("'").append(pdfPages.get(i).toString().replace("'", "\\'")).append("'");
                }
                mergeScript.append("]\n");
                mergeScript.append("output = '").append(outputFile.getAbsolutePath().replace("'", "\\'")).append("'\n");
                mergeScript.append("subprocess.run(['/System/Library/Automator/Combine PDF Pages.action/Contents/MacOS/join', '-o', output] + pdf_files)\n");
                
                Path mergeScriptPath = Files.createTempFile("merge_pdf_", ".py");
                Files.writeString(mergeScriptPath, mergeScript.toString());
                
                ProcessBuilder pb = new ProcessBuilder("python3", mergeScriptPath.toString());
                Process p = pb.start();
                p.waitFor();
                
                Files.deleteIfExists(mergeScriptPath);
            }
            
            // Cleanup temp files
            for (Path pdfPage : pdfPages) {
                Files.deleteIfExists(pdfPage);
            }
            Files.deleteIfExists(tempDir);
            
            return Files.exists(outputFile.toPath()) && Files.size(outputFile.toPath()) > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void performOCR(List<File> images, File outputFile) {
        try {
            StringBuilder allText = new StringBuilder();
            
            // Method 1: Try using tesseract (brew install tesseract)
            ProcessBuilder checkBuilder = new ProcessBuilder("which", "tesseract");
            Process checkProcess = checkBuilder.start();
            int checkResult = checkProcess.waitFor();
            
            if (checkResult == 0) {
                // Tesseract is available
                for (int i = 0; i < images.size(); i++) {
                    File imageFile = images.get(i);
                    
                    // Create temp output file for tesseract
                    Path tempTxt = Files.createTempFile("ocr_page_" + i, "");
                    
                    // Run tesseract
                    ProcessBuilder pb = new ProcessBuilder("tesseract", 
                        imageFile.getAbsolutePath(), 
                        tempTxt.toString().replace(".txt", ""));
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    int exitCode = p.waitFor();
                    
                    // Read the output
                    Path txtFile = Paths.get(tempTxt.toString() + ".txt");
                    if (Files.exists(txtFile)) {
                        String pageText = Files.readString(txtFile);
                        if (i > 0) {
                            allText.append("\\n\\n--- Page ").append(i + 1).append(" ---\\n\\n");
                        }
                        allText.append(pageText);
                        Files.deleteIfExists(txtFile);
                    }
                    Files.deleteIfExists(tempTxt);
                }
            } else {
                // Fallback: Try using macOS Vision framework via Python
                boolean ocrSuccess = performOCRWithPython(images, outputFile);
                if (ocrSuccess) {
                    return;
                }
                
                // If all else fails, create a placeholder text file
                allText.append("OCR not available.\\n\\n");
                allText.append("To enable OCR, install tesseract:\\n");
                allText.append("  brew install tesseract\\n\\n");
                allText.append("Document contains ").append(images.size()).append(" scanned page(s).\\n");
            }
            
            // Write combined text to file
            Files.writeString(outputFile.toPath(), allText.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            // Create error message file
            try {
                Files.writeString(outputFile.toPath(), 
                    "OCR failed: " + e.getMessage() + "\\n\\nDocument contains " + 
                    images.size() + " scanned page(s).");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private boolean performOCRWithPython(List<File> images, File outputFile) {
        try {
            // Try using Python with pytesseract or Vision framework
            StringBuilder pythonScript = new StringBuilder();
            pythonScript.append("import sys\\n");
            pythonScript.append("try:\\n");
            pythonScript.append("    from PIL import Image\\n");
            pythonScript.append("    import pytesseract\\n");
            pythonScript.append("except ImportError:\\n");
            pythonScript.append("    # Try using macOS Vision framework\\n");
            pythonScript.append("    try:\\n");
            pythonScript.append("        import Vision\\n");
            pythonScript.append("        import Quartz\\n");
            pythonScript.append("        from Foundation import NSURL\\n");
            pythonScript.append("    except ImportError:\\n");
            pythonScript.append("        sys.exit(1)\\n");
            pythonScript.append("\\n");
            pythonScript.append("all_text = []\\n");
            
            for (int i = 0; i < images.size(); i++) {
                pythonScript.append("try:\\n");
                pythonScript.append("    img = Image.open('").append(images.get(i).getAbsolutePath().replace("'", "\\\\'")).append("')\\n");
                pythonScript.append("    text = pytesseract.image_to_string(img)\\n");
                pythonScript.append("    all_text.append(text)\\n");
                pythonScript.append("except:\\n");
                pythonScript.append("    pass\\n");
            }
            
            pythonScript.append("\\n");
            pythonScript.append("with open('").append(outputFile.getAbsolutePath().replace("'", "\\\\'")).append("', 'w') as f:\\n");
            pythonScript.append("    for i, text in enumerate(all_text):\\n");
            pythonScript.append("        if i > 0:\\n");
            pythonScript.append("            f.write('\\\\n\\\\n--- Page ' + str(i + 1) + ' ---\\\\n\\\\n')\\n");
            pythonScript.append("        f.write(text)\\n");
            pythonScript.append("print('SUCCESS')\\n");
            
            // Write and execute script
            Path scriptPath = Files.createTempFile("ocr_script_", ".py");
            Files.writeString(scriptPath, pythonScript.toString());
            
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            int exitCode = p.waitFor();
            Files.deleteIfExists(scriptPath);
            
            return exitCode == 0 && output.toString().contains("SUCCESS");
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleBack() {
        NoteXApp.setRoot("views/user_home");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
