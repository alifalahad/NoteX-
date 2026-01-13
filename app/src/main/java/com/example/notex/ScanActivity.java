package com.example.notex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.AspectRatio;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private PreviewView previewView;
    private FloatingActionButton btnCapture;
    private MaterialButton btnDone;
    private MaterialButton btnLandscape, btnPortrait, btnRatio;
    private RecyclerView recyclerThumbnails;
    private View btnClose, btnFlash, btnFlipCamera;
    
    private ImageCapture imageCapture;
    private Camera camera;
    private List<ScannedPage> scannedPages = new ArrayList<>();
    private ScanThumbnailAdapter thumbnailAdapter;
    private boolean isPortraitMode = false; // false = landscape, true = portrait
    private boolean isFlashOn = false;
    private boolean isFrontCamera = false;
    private int currentAspectRatio = AspectRatio.RATIO_4_3; // Default: 3:4
    private int currentRatioIndex = 0; // 0=3:4, 1=9:16, 2=1:1, 3=Full
    private final String[] ratioLabels = {"3:4", "9:16", "1:1", "Full"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        
        // Enable edge-to-edge display
        getWindow().setDecorFitsSystemWindows(false);
        
        initViews();
        
        // Restore state if available
        if (savedInstanceState != null) {
            isPortraitMode = savedInstanceState.getBoolean("isPortraitMode", false);
            ArrayList<String> savedPaths = savedInstanceState.getStringArrayList("scannedPaths");
            if (savedPaths != null) {
                for (String path : savedPaths) {
                    File file = new File(path);
                    if (file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(path);
                        if (bitmap != null) {
                            scannedPages.add(new ScannedPage(path, bitmap));
                        }
                    }
                }
                thumbnailAdapter.notifyDataSetChanged();
                updateUI();
            }
        }
        
        updateOrientationButtons();
        checkPermissionsAndStartCamera();
    }
    
    private void initViews() {
        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        btnDone = findViewById(R.id.btnDone);
        btnClose = findViewById(R.id.btnClose);
        btnFlash = findViewById(R.id.btnFlash);
        btnFlipCamera = findViewById(R.id.btnFlipCamera);
        btnLandscape = findViewById(R.id.btnLandscape);
        btnPortrait = findViewById(R.id.btnPortrait);
        btnRatio = findViewById(R.id.btnRatio);
        recyclerThumbnails = findViewById(R.id.recyclerThumbnails);
        
        btnClose.setOnClickListener(v -> finish());
        btnCapture.setOnClickListener(v -> captureImage());
        btnDone.setOnClickListener(v -> processScan());
        btnLandscape.setOnClickListener(v -> setOrientation(false));
        btnPortrait.setOnClickListener(v -> setOrientation(true));
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnFlipCamera.setOnClickListener(v -> flipCamera());
        btnRatio.setOnClickListener(v -> cycleAspectRatio());
        
        // Setup thumbnails RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerThumbnails.setLayoutManager(layoutManager);
        thumbnailAdapter = new ScanThumbnailAdapter(scannedPages, position -> {
            // Delete page
            scannedPages.remove(position);
            thumbnailAdapter.notifyItemRemoved(position);
            thumbnailAdapter.notifyItemRangeChanged(position, scannedPages.size());
            updateUI();
        });
        recyclerThumbnails.setAdapter(thumbnailAdapter);
    }
    
    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);
        updateFlashIcon();
        Toast.makeText(this, isFlashOn ? "Flash On" : "Flash Off", Toast.LENGTH_SHORT).show();
    }
    
    private void updateFlashIcon() {
        // Update flash button appearance based on state
        if (isFlashOn) {
            ((android.widget.ImageButton)btnFlash).setImageResource(android.R.drawable.btn_star_big_on);
            btnFlash.setAlpha(1.0f);
        } else {
            ((android.widget.ImageButton)btnFlash).setImageResource(android.R.drawable.btn_star_big_on);
            btnFlash.setAlpha(0.5f);
        }
    }
    
    private void flipCamera() {
        isFrontCamera = !isFrontCamera;
        Toast.makeText(this, isFrontCamera ? "Front Camera" : "Back Camera", Toast.LENGTH_SHORT).show();
        checkPermissionsAndStartCamera();
    }
    
    private void cycleAspectRatio() {
        // Cycle to next ratio
        currentRatioIndex = (currentRatioIndex + 1) % ratioLabels.length;
        
        // Update aspect ratio based on index
        switch (currentRatioIndex) {
            case 0: // 3:4
                currentAspectRatio = AspectRatio.RATIO_4_3;
                break;
            case 1: // 9:16
                currentAspectRatio = AspectRatio.RATIO_16_9;
                break;
            case 2: // 1:1
                currentAspectRatio = AspectRatio.RATIO_4_3; // Approximate
                break;
            case 3: // Full
                currentAspectRatio = AspectRatio.RATIO_16_9;
                break;
        }
        
        // Update button text
        btnRatio.setText(ratioLabels[currentRatioIndex]);
        
        // Restart camera with new ratio
        startCamera();
        
        Toast.makeText(this, "Ratio: " + ratioLabels[currentRatioIndex], Toast.LENGTH_SHORT).show();
    }
    
    private void checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview - match aspect ratio with capture
        Preview preview = new Preview.Builder()
            .setTargetAspectRatio(currentAspectRatio)
            .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // ImageCapture - Enhanced quality settings
        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(isPortraitMode ? android.view.Surface.ROTATION_90 : android.view.Surface.ROTATION_0)
            .setTargetAspectRatio(currentAspectRatio)
            .setJpegQuality(100) // Maximum JPEG quality
            .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
            .build();
        
        // Select camera (front or back)
        CameraSelector cameraSelector = isFrontCamera ? 
            CameraSelector.DEFAULT_FRONT_CAMERA : 
            CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera and store camera instance
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture);
            
            // Update flash button based on flash availability
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                btnFlash.setEnabled(true);
                updateFlashIcon();
            } else {
                btnFlash.setEnabled(false);
                isFlashOn = false;
            }
                
        } catch (Exception e) {
            Log.e(TAG, "Camera binding failed", e);
        }
    }
    
    private void captureImage() {
        if (imageCapture == null) return;
        
        // Create temp file
        File photoFile = new File(
            getExternalFilesDir(null),
            "scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg"
        );
        
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    processImage(photoFile);
                }
                
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    Toast.makeText(ScanActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    private void processImage(File imageFile) {
        try {
            // Load bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            
            // Apply orientation rotation if portrait mode is selected
            if (isPortraitMode && bitmap != null) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            
            // TODO: Edge detection and perspective correction will be added
            // For now, just use the image as-is
            Bitmap processedBitmap = bitmap;
            
            // Save processed image
            File processedFile = new File(
                getExternalFilesDir(null),
                "processed_" + imageFile.getName()
            );
            
            FileOutputStream fos = new FileOutputStream(processedFile);
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos); // Maximum quality
            fos.close();
            
            // Add to scanned pages
            ScannedPage page = new ScannedPage(processedFile.getAbsolutePath(), processedBitmap);
            scannedPages.add(page);
            
            runOnUiThread(() -> {
                thumbnailAdapter.notifyItemInserted(scannedPages.size() - 1);
                updateUI();
                Toast.makeText(this, "Page " + scannedPages.size() + " captured", Toast.LENGTH_SHORT).show();
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show());
        }
    }
    
    private void updateUI() {
        if (scannedPages.isEmpty()) {
            recyclerThumbnails.setVisibility(View.GONE);
            btnDone.setVisibility(View.GONE);
        } else {
            recyclerThumbnails.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.VISIBLE);
            btnDone.setText("Done (" + scannedPages.size() + ")");
        }
    }
    
    private void processScan() {
        if (scannedPages.isEmpty()) {
            Toast.makeText(this, "No pages to process", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if launched from library (should save to library)
        boolean saveToLibrary = getIntent().getBooleanExtra("saveToLibrary", false);
        
        if (saveToLibrary) {
            // Return image paths to library activity
            java.util.ArrayList<String> imagePaths = new java.util.ArrayList<>();
            for (ScannedPage page : scannedPages) {
                imagePaths.add(page.getImagePath());
            }
            
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("imagePaths", imagePaths);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }
        
        // Show options: Export PDF or Save to Notebook
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Scanned Pages");
        builder.setMessage(scannedPages.size() + " page(s) ready");
        
        builder.setPositiveButton("Export PDF", (dialog, which) -> exportToPDF());
        builder.setNegativeButton("Save to Notebook", (dialog, which) -> saveToNotebook());
        builder.setNeutralButton("Cancel", null);
        
        builder.show();
    }
    
    private void exportToPDF() {
        try {
            // Collect bitmaps
            java.util.ArrayList<Bitmap> bitmaps = new java.util.ArrayList<>();
            for (ScannedPage page : scannedPages) {
                if (page.getBitmap() != null && !page.getBitmap().isRecycled()) {
                    bitmaps.add(page.getBitmap());
                }
            }
            
            if (bitmaps.isEmpty()) {
                Toast.makeText(this, "No valid pages to export", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Generate PDF
            File pdfFile = new File(
                getExternalFilesDir(null),
                "Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf"
            );
            
            boolean success = ScanUtils.generatePDF(bitmaps, pdfFile);
            
            if (success && pdfFile.exists()) {
                Toast.makeText(this, "PDF saved: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                
                // Share PDF
                sharePDF(pdfFile);
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting PDF", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sharePDF(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                pdfFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "Failed to share PDF", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveToNotebook() {
        // Return file paths to calling activity
        Intent resultIntent = new Intent();
        java.util.ArrayList<String> filePaths = new java.util.ArrayList<>();
        for (ScannedPage page : scannedPages) {
            filePaths.add(page.getFilePath());
        }
        resultIntent.putStringArrayListExtra("scanned_pages", filePaths);
        resultIntent.putExtra("page_count", scannedPages.size());
        setResult(RESULT_OK, resultIntent);
        
        Toast.makeText(this, scannedPages.size() + " page(s) ready to add to notebook", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPortraitMode", isPortraitMode);
        
        // Save scanned page paths
        ArrayList<String> paths = new ArrayList<>();
        for (ScannedPage page : scannedPages) {
            paths.add(page.getFilePath());
        }
        outState.putStringArrayList("scannedPaths", paths);
    }
    
    private void setOrientation(boolean portrait) {
        if (isPortraitMode == portrait) return;
        
        isPortraitMode = portrait;
        updateOrientationButtons();
        
        // Restart camera with new orientation
        checkPermissionsAndStartCamera();
    }
    
    private void updateOrientationButtons() {
        if (isPortraitMode) {
            btnPortrait.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            btnLandscape.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF757575));
        } else {
            btnLandscape.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            btnPortrait.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF757575));
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        for (ScannedPage page : scannedPages) {
            if (page.getBitmap() != null && !page.getBitmap().isRecycled()) {
                page.getBitmap().recycle();
            }
        }
    }
    
    private void toggleOrientation() {
        isPortraitMode = !isPortraitMode;
        String mode = isPortraitMode ? "Portrait" : "Landscape";
        Toast.makeText(this, "Orientation: " + mode, Toast.LENGTH_SHORT).show();
        
        // Restart camera with new orientation
        checkPermissionsAndStartCamera();
    }
    
    // Inner class to represent a scanned page
    public static class ScannedPage {
        private String filePath;
        private Bitmap bitmap;
        
        public ScannedPage(String filePath, Bitmap bitmap) {
            this.filePath = filePath;
            this.bitmap = bitmap;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public String getImagePath() {
            return filePath;
        }
        
        public Bitmap getBitmap() {
            return bitmap;
        }
    }
}
