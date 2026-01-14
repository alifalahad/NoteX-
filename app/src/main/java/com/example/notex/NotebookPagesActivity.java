package com.example.notex;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.notex.databinding.ActivityNotebookPagesBinding;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Notebook;
import com.example.notex.models.Page;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NotebookPagesActivity - Displays pages within a notebook
 */
public class NotebookPagesActivity extends AppCompatActivity {

    private ActivityNotebookPagesBinding binding;
    private DatabaseHelper dbHelper;
    private Notebook notebook;
    private String notebookId;
    private PageAdapter pageAdapter;
    private List<Page> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotebookPagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        // Get notebook ID from intent
        notebookId = getIntent().getStringExtra("NOTEBOOK_ID");
        if (notebookId == null) {
            android.widget.Toast.makeText(this, "Notebook not found", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load notebook details
        notebook = dbHelper.getNotebookById(notebookId);
        if (notebook == null) {
            android.widget.Toast.makeText(this, "Notebook not found", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadPages();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
            getSupportActionBar().setTitle(notebook.getTitle());
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        pages = new ArrayList<>();
        pageAdapter = new PageAdapter(this, pages, page -> {
            // Open notebook in multi-page editor
            Intent intent = new Intent(NotebookPagesActivity.this, MultiPageEditorActivity.class);
            intent.putExtra("NOTEBOOK_ID", notebookId);
            intent.putExtra("PAGE_ID", page.getId());
            startActivity(intent);
        });

        binding.recyclerViewPages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewPages.setAdapter(pageAdapter);
    }

    private void setupClickListeners() {
        // Open notebook editor
        binding.fabNewPage.setOnClickListener(v -> {
            Intent intent = new Intent(NotebookPagesActivity.this, MultiPageEditorActivity.class);
            intent.putExtra("NOTEBOOK_ID", notebookId);
            startActivity(intent);
        });
    }

    private void loadPages() {
        pages.clear();
        pages.addAll(dbHelper.getNotebookPages(notebookId));
        pageAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (pages.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewPages.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewPages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPages();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notebook_pages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export_pdf) {
            exportNotebookToPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportNotebookToPdf() {
        if (pages.isEmpty()) {
            Toast.makeText(this, "No pages to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ask for PDF name
        android.app.AlertDialog.Builder nameDialog = new android.app.AlertDialog.Builder(this);
        nameDialog.setTitle("Export as PDF");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(notebook.getTitle());
        input.setHint("PDF name");
        input.setSelection(input.getText().length());
        nameDialog.setView(input);
        
        nameDialog.setPositiveButton("Export", (dialog, which) -> {
            String pdfName = input.getText().toString().trim();
            if (pdfName.isEmpty()) {
                pdfName = notebook.getTitle();
            }
            performPdfExport(pdfName);
        });
        
        nameDialog.setNegativeButton("Cancel", null);
        nameDialog.show();
    }
    
    private void performPdfExport(String pdfName) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Exporting notebook to PDF...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(pages.size());
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                // Create PDF document
                PdfDocument pdfDocument = new PdfDocument();

                for (int i = 0; i < pages.size(); i++) {
                    Page page = pages.get(i);
                    final int currentPage = i + 1;

                    runOnUiThread(() -> {
                        progressDialog.setProgress(currentPage);
                        progressDialog.setMessage("Rendering page " + currentPage + " of " + pages.size() + "...");
                    });

                    // Create a page in the PDF (A4 size: 595 x 842 points)
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, i + 1).create();
                    PdfDocument.Page pdfPage = pdfDocument.startPage(pageInfo);

                    Canvas canvas = pdfPage.getCanvas();
                    canvas.drawColor(Color.WHITE);

                    // Render the page content
                    renderPageToPdfCanvas(page, canvas);

                    pdfDocument.finishPage(pdfPage);
                }

                // Save PDF to scanned_documents folder
                File docsDir = new File(getExternalFilesDir(null), "scanned_documents");
                if (!docsDir.exists()) {
                    docsDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String fileName = pdfName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";
                File pdfFile = new File(docsDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                pdfDocument.writeTo(fos);
                fos.close();
                pdfDocument.close();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "PDF saved to My Documents: " + fileName, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void renderPageToPdfCanvas(Page page, Canvas canvas) {
        try {
            String content = page.getContent();
            if (content == null || content.isEmpty()) return;

            JSONObject json = new JSONObject(content);

            // Render paths (drawings)
            if (json.has("paths")) {
                JSONArray paths = json.getJSONArray("paths");
                for (int i = 0; i < paths.length(); i++) {
                    JSONObject pathObj = paths.getJSONObject(i);
                    Path path = new Path();
                    JSONArray points = pathObj.getJSONArray("points");
                    
                    if (points.length() >= 2) {
                        path.moveTo((float) points.getDouble(0), (float) points.getDouble(1));
                        for (int j = 2; j < points.length(); j += 2) {
                            if (j + 1 < points.length()) {
                                path.lineTo((float) points.getDouble(j), (float) points.getDouble(j + 1));
                            }
                        }
                    }

                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(pathObj.optInt("color", Color.BLACK));
                    paint.setStrokeWidth((float) pathObj.optDouble("strokeWidth", 5.0));
                    
                    boolean isFilled = pathObj.optBoolean("isFilled", false);
                    paint.setStyle(isFilled ? android.graphics.Paint.Style.FILL : android.graphics.Paint.Style.STROKE);
                    
                    if (pathObj.optBoolean("isDashed", false)) {
                        paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{20, 10}, 0));
                    }

                    canvas.drawPath(path, paint);
                }
            }

            // Render text elements
            if (json.has("texts")) {
                JSONArray texts = json.getJSONArray("texts");
                for (int i = 0; i < texts.length(); i++) {
                    JSONObject textObj = texts.getJSONObject(i);
                    String text = textObj.optString("text", "");
                    float x = (float) textObj.optDouble("x", 0);
                    float y = (float) textObj.optDouble("y", 0);
                    float textSize = (float) textObj.optDouble("textSize", 40);
                    float rotation = (float) textObj.optDouble("rotation", 0);
                    int textColor = textObj.optInt("textColor", Color.BLACK);
                    int bgColor = textObj.optInt("backgroundColor", Color.TRANSPARENT);
                    boolean isBold = textObj.optBoolean("isBold", false);
                    boolean isItalic = textObj.optBoolean("isItalic", false);
                    boolean isSticky = textObj.optBoolean("isSticky", false);

                    android.graphics.Paint textPaint = new android.graphics.Paint();
                    textPaint.setAntiAlias(true);
                    textPaint.setColor(textColor);
                    textPaint.setTextSize(textSize);

                    // Set typeface based on bold and italic
                    int style = android.graphics.Typeface.NORMAL;
                    if (isBold && isItalic) {
                        style = android.graphics.Typeface.BOLD_ITALIC;
                    } else if (isBold) {
                        style = android.graphics.Typeface.BOLD;
                    } else if (isItalic) {
                        style = android.graphics.Typeface.ITALIC;
                    }
                    textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, style));

                    canvas.save();
                    canvas.rotate(rotation, x, y);

                    // Draw sticky note background if applicable
                    if (isSticky && bgColor != Color.TRANSPARENT) {
                        android.graphics.Paint bgPaint = new android.graphics.Paint();
                        bgPaint.setColor(bgColor);
                        bgPaint.setStyle(android.graphics.Paint.Style.FILL);
                        
                        android.graphics.Rect bounds = new android.graphics.Rect();
                        textPaint.getTextBounds(text, 0, text.length(), bounds);
                        float padding = 20;
                        canvas.drawRect(x - padding, y - bounds.height() - padding, 
                                      x + bounds.width() + padding, y + padding, bgPaint);
                    }

                    canvas.drawText(text, x, y, textPaint);
                    canvas.restore();
                }
            }

            // Render images
            if (json.has("images")) {
                JSONArray images = json.getJSONArray("images");
                for (int i = 0; i < images.length(); i++) {
                    JSONObject imageObj = images.getJSONObject(i);
                    String imagePath = imageObj.optString("path", "");
                    float x = (float) imageObj.optDouble("x", 0);
                    float y = (float) imageObj.optDouble("y", 0);
                    float width = (float) imageObj.optDouble("width", 100);
                    float height = (float) imageObj.optDouble("height", 100);
                    float rotation = (float) imageObj.optDouble("rotation", 0);
                    boolean flipH = imageObj.optBoolean("flipHorizontal", false);
                    boolean flipV = imageObj.optBoolean("flipVertical", false);

                    if (!imagePath.isEmpty()) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                            if (bitmap != null) {
                                canvas.save();
                                canvas.translate(x, y);
                                canvas.rotate(rotation);
                                
                                float scaleX = flipH ? -1 : 1;
                                float scaleY = flipV ? -1 : 1;
                                canvas.scale(scaleX, scaleY, width / 2, height / 2);
                                
                                android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                                android.graphics.RectF dstRect = new android.graphics.RectF(0, 0, width, height);
                                canvas.drawBitmap(bitmap, srcRect, dstRect, null);
                                canvas.restore();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
