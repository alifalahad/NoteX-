package com.example.notex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Utility class for document scanning operations
 * Includes PDF generation, image enhancement, and basic edge detection
 */
public class ScanUtils {
    private static final String TAG = "ScanUtils";
    
    /**
     * Generate a multi-page PDF from scanned images
     * @param images List of bitmap images to include in PDF
     * @param outputFile Output PDF file
     * @return true if successful, false otherwise
     */
    public static boolean generatePDF(List<Bitmap> images, File outputFile) {
        if (images == null || images.isEmpty()) {
            Log.e(TAG, "No images to convert to PDF");
            return false;
        }
        
        PdfDocument document = new PdfDocument();
        
        try {
            for (int i = 0; i < images.size(); i++) {
                Bitmap bitmap = images.get(i);
                if (bitmap == null || bitmap.isRecycled()) {
                    continue;
                }
                
                // Create page info (A4 size scaled to fit image)
                int pageWidth = bitmap.getWidth();
                int pageHeight = bitmap.getHeight();
                
                // Scale to A4 ratio if needed (maintaining aspect ratio)
                float a4Ratio = 297f / 210f; // A4 height/width ratio
                float imageRatio = (float) pageHeight / pageWidth;
                
                if (imageRatio > a4Ratio) {
                    // Image is taller, constrain height
                    pageHeight = (int) (pageWidth * a4Ratio);
                } else {
                    // Image is wider, constrain width
                    pageWidth = (int) (pageHeight / a4Ratio);
                }
                
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, i + 1).create();
                    
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                
                // Draw bitmap on canvas
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                
                // Scale bitmap to fit page
                float scaleX = (float) pageWidth / bitmap.getWidth();
                float scaleY = (float) pageHeight / bitmap.getHeight();
                float scale = Math.min(scaleX, scaleY);
                
                int scaledWidth = (int) (bitmap.getWidth() * scale);
                int scaledHeight = (int) (bitmap.getHeight() * scale);
                int x = (pageWidth - scaledWidth) / 2;
                int y = (pageHeight - scaledHeight) / 2;
                
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
                canvas.drawBitmap(scaledBitmap, x, y, paint);
                
                if (scaledBitmap != bitmap && !scaledBitmap.isRecycled()) {
                    scaledBitmap.recycle();
                }
                
                document.finishPage(page);
            }
            
            // Write PDF to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                document.writeTo(fos);
                Log.i(TAG, "PDF generated successfully: " + outputFile.getAbsolutePath());
                return true;
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF", e);
            return false;
        } finally {
            document.close();
        }
    }
    
    /**
     * Auto-enhance image: adjust brightness, contrast, and sharpness
     * @param original Original bitmap
     * @return Enhanced bitmap
     */
    public static Bitmap autoEnhance(Bitmap original) {
        if (original == null || original.isRecycled()) {
            return original;
        }
        
        Bitmap enhanced = Bitmap.createBitmap(
            original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(enhanced);
        
        Paint paint = new Paint();
        
        // Increase contrast and brightness
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
            1.2f, 0, 0, 0, 10,      // Red
            0, 1.2f, 0, 0, 10,      // Green
            0, 0, 1.2f, 0, 10,      // Blue
            0, 0, 0, 1, 0           // Alpha
        });
        
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        
        return enhanced;
    }
    
    /**
     * Convert image to black and white (for document scanning)
     * @param original Original bitmap
     * @return Black and white bitmap
     */
    public static Bitmap convertToBlackAndWhite(Bitmap original) {
        if (original == null || original.isRecycled()) {
            return original;
        }
        
        Bitmap bw = Bitmap.createBitmap(
            original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bw);
        
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Remove color
        
        // Increase contrast for better readability
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
            1.5f, 0, 0, 0, -50,
            0, 1.5f, 0, 0, -50,
            0, 0, 1.5f, 0, -50,
            0, 0, 0, 1, 0
        });
        
        colorMatrix.postConcat(contrastMatrix);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        
        return bw;
    }
    
    /**
     * Basic edge detection using luminosity analysis
     * Returns detected corners or null if not found
     * @param bitmap Input image
     * @return Array of 4 points [topLeft, topRight, bottomRight, bottomLeft] or null
     */
    public static float[][] detectEdges(Bitmap bitmap) {
        // TODO: Implement basic edge detection
        // For v1, we'll return null and use the full image
        // v2 will add proper contour detection
        return null;
    }
    
    /**
     * Apply perspective correction to image
     * @param bitmap Original image
     * @param corners Four corner points
     * @return Corrected bitmap
     */
    public static Bitmap perspectiveCorrect(Bitmap bitmap, float[][] corners) {
        // TODO: Implement perspective transform
        // For v1, return original bitmap
        return bitmap;
    }
    
    /**
     * Rotate bitmap by specified degrees
     * @param source Original bitmap
     * @param degrees Rotation angle (90, 180, 270)
     * @return Rotated bitmap
     */
    public static Bitmap rotateBitmap(Bitmap source, float degrees) {
        if (source == null || source.isRecycled()) {
            return source;
        }
        
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degrees);
        
        Bitmap rotated = Bitmap.createBitmap(
            source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            
        return rotated;
    }
    
    /**
     * Crop bitmap to specified region
     * @param source Original bitmap
     * @param x Start X coordinate
     * @param y Start Y coordinate
     * @param width Width of crop region
     * @param height Height of crop region
     * @return Cropped bitmap
     */
    public static Bitmap cropBitmap(Bitmap source, int x, int y, int width, int height) {
        if (source == null || source.isRecycled()) {
            return source;
        }
        
        // Ensure crop region is within bounds
        x = Math.max(0, Math.min(x, source.getWidth()));
        y = Math.max(0, Math.min(y, source.getHeight()));
        width = Math.min(width, source.getWidth() - x);
        height = Math.min(height, source.getHeight() - y);
        
        if (width <= 0 || height <= 0) {
            return source;
        }
        
        return Bitmap.createBitmap(source, x, y, width, height);
    }
    
    /**
     * Generate PDF from list of image file paths
     * @param imagePaths List of image file paths
     * @param outputFile Output PDF file
     * @return true if successful
     */
    public static boolean generatePDF(ArrayList<String> imagePaths, File outputFile) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return false;
        }
        
        List<Bitmap> bitmaps = new ArrayList<>();
        try {
            for (String path : imagePaths) {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
            }
            
            return generatePDF(bitmaps, outputFile);
        } finally {
            for (Bitmap bitmap : bitmaps) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }
    
    /**
     * Get page count from PDF file
     * @param pdfFile PDF file
     * @return Number of pages, or 0 if error
     */
    public static int getPdfPageCount(File pdfFile) {
        try {
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            int pageCount = renderer.getPageCount();
            renderer.close();
            fileDescriptor.close();
            return pageCount;
        } catch (Exception e) {
            Log.e(TAG, "Error getting PDF page count", e);
            return 0;
        }
    }
    
    /**
     * Convert PDF to text using ML Kit text recognition with enhanced preprocessing
     * @param context Application context
     * @param pdfFile Input PDF file
     * @param txtFile Output text file
     * @return true if successful
     */
    public static boolean convertPdfToText(Context context, File pdfFile, File txtFile) {
        try {
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            
            StringBuilder allText = new StringBuilder();
            TextRecognizer recognizer = TextRecognition.getClient();
            
            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                
                // Render at optimal 2x resolution for balanced speed and quality
                Bitmap bitmap = Bitmap.createBitmap(
                    page.getWidth() * 2, // 2x for optimal speed/quality balance
                    page.getHeight() * 2,
                    Bitmap.Config.ARGB_8888
                );
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                
                // Fast preprocessing optimized for OCR
                bitmap = fastPreprocessForOCR(bitmap);
                
                // Extract text using ML Kit
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                CountDownLatch latch = new CountDownLatch(1);
                final StringBuilder pageText = new StringBuilder();
                
                recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // Preserve formatting by processing blocks and lines
                        for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                            for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                                pageText.append(line.getText()).append("\n");
                            }
                            pageText.append("\n"); // Extra line between blocks
                        }
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Text recognition failed", e);
                        latch.countDown();
                    });
                
                latch.await();
                
                if (pageText.length() > 0) {
                    allText.append(pageText);
                    allText.append("\n\n--- Page ").append(i + 1).append(" ---\n\n");
                }
                
                bitmap.recycle();
            }
            
            renderer.close();
            fileDescriptor.close();
            
            // Write text to file
            try (FileWriter writer = new FileWriter(txtFile)) {
                writer.write(allText.toString());
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error converting PDF to text", e);
            return false;
        }
    }
    
    /**
     * Fast preprocessing optimized for OCR - best balance of speed and accuracy
     * Uses optimized single-pass algorithm
     * @param original Original bitmap
     * @return Preprocessed bitmap optimized for text recognition
     */
    private static Bitmap fastPreprocessForOCR(Bitmap original) {
        if (original == null || original.isRecycled()) {
            return original;
        }
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Direct pixel manipulation for maximum speed
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Single-pass processing for speed
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            
            // Fast grayscale with optimal ITU-R BT.709 weights
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            int gray = (r * 77 + g * 150 + b * 29) >> 8; // Fast division by 256
            
            // Apply contrast enhancement inline
            gray = Math.min(255, Math.max(0, (gray - 128) * 3 / 2 + 128));
            
            // Simple adaptive thresholding - faster than full window
            int value = gray > 140 ? 255 : 0; // Optimized threshold
            
            pixels[i] = Color.rgb(value, value, value);
        }
        
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        
        return result;
    }
    
    /**
     * Preprocess bitmap for better OCR results
     * Applies contrast enhancement, noise reduction, and sharpening
     * @param original Original bitmap
     * @return Preprocessed bitmap optimized for text recognition
     */
    private static Bitmap preprocessForOCR(Bitmap original) {
        if (original == null || original.isRecycled()) {
            return original;
        }
        
        // Step 1: Increase contrast and brightness
        Bitmap enhanced = Bitmap.createBitmap(
            original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(enhanced);
        
        Paint paint = new Paint();
        
        // Enhanced color matrix for better text contrast
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
            1.5f, 0, 0, 0, 20,      // Red - increased contrast
            0, 1.5f, 0, 0, 20,      // Green - increased contrast
            0, 0, 1.5f, 0, 20,      // Blue - increased contrast
            0, 0, 0, 1, 0           // Alpha
        });
        
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        
        // Step 2: Apply grayscale conversion for better text detection
        Bitmap grayscale = convertToGrayscale(enhanced);
        enhanced.recycle();
        
        // Step 3: Apply adaptive thresholding for better text/background separation
        Bitmap thresholded = applyAdaptiveThreshold(grayscale);
        grayscale.recycle();
        
        return thresholded;
    }
    
    /**
     * Convert bitmap to grayscale
     * @param original Original bitmap
     * @return Grayscale bitmap
     */
    private static Bitmap convertToGrayscale(Bitmap original) {
        Bitmap grayscale = Bitmap.createBitmap(
            original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscale);
        
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        
        return grayscale;
    }
    
    /**
     * Apply adaptive thresholding to separate text from background
     * @param original Original grayscale bitmap
     * @return Thresholded bitmap
     */
    private static Bitmap applyAdaptiveThreshold(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Calculate local threshold for each pixel
        int windowSize = 15; // Size of local window
        int halfWindow = windowSize / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sum = 0;
                int count = 0;
                
                // Calculate average in local window
                for (int dy = -halfWindow; dy <= halfWindow; dy++) {
                    for (int dx = -halfWindow; dx <= halfWindow; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int pixel = pixels[ny * width + nx];
                            int gray = Color.red(pixel); // Already grayscale
                            sum += gray;
                            count++;
                        }
                    }
                }
                
                int average = sum / count;
                int currentPixel = pixels[y * width + x];
                int currentGray = Color.red(currentPixel);
                
                // Apply threshold with small bias to prefer white background
                int threshold = average - 10;
                int newColor = currentGray > threshold ? Color.WHITE : Color.BLACK;
                pixels[y * width + x] = newColor;
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
    
    /**
     * Convert text file to PDF
     * @param txtFile Input text file
     * @param pdfFile Output PDF file
     * @return true if successful
     */
    public static boolean convertTextToPdf(File txtFile, File pdfFile) {
        try {
            // Read text content
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            if (content.length() == 0) {
                return false;
            }
            
            // Create PDF
            PdfDocument document = new PdfDocument();
            
            // A4 size in points (1 point = 1/72 inch)
            int pageWidth = 595; // 8.27 inches * 72
            int pageHeight = 842; // 11.69 inches * 72
            int margin = 40;
            int textWidth = pageWidth - (2 * margin);
            
            TextPaint textPaint = new TextPaint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(12);
            textPaint.setAntiAlias(true);
            
            String text = content.toString();
            int pageNumber = 1;
            int startIndex = 0;
            
            while (startIndex < text.length()) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                
                // Calculate how much text fits on this page
                int availableHeight = pageHeight - (2 * margin);
                String remainingText = text.substring(startIndex);
                
                StaticLayout layout = StaticLayout.Builder.obtain(
                    remainingText, 0, remainingText.length(), textPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1.0f, 1.0f)
                    .setIncludePad(true)
                    .build();
                
                // Find how many lines fit on this page
                int lineCount = 0;
                int totalHeight = 0;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    totalHeight += layout.getLineBottom(i) - layout.getLineTop(i);
                    if (totalHeight > availableHeight) {
                        break;
                    }
                    lineCount = i + 1;
                }
                
                if (lineCount == 0) {
                    lineCount = 1; // At least one line
                }
                
                int endIndex = Math.min(startIndex + layout.getLineEnd(lineCount - 1), text.length());
                String pageText = text.substring(startIndex, endIndex);
                
                // Draw text on page
                canvas.save();
                canvas.translate(margin, margin);
                
                StaticLayout finalLayout = StaticLayout.Builder.obtain(
                    pageText, 0, pageText.length(), textPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1.0f, 1.0f)
                    .setIncludePad(true)
                    .build();
                
                finalLayout.draw(canvas);
                canvas.restore();
                
                document.finishPage(page);
                
                startIndex = endIndex;
                pageNumber++;
            }
            
            // Write PDF to file
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                document.writeTo(fos);
            }
            
            document.close();
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting text to PDF", e);
            return false;
        }
    }
}
