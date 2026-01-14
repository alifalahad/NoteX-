package com.example.notex;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityNotebooksBinding;
import com.example.notex.models.Notebook;
import com.example.notex.models.Page;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

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
 * NotebooksActivity - Displays user's notebooks
 */
public class NotebooksActivity extends AppCompatActivity {

    private ActivityNotebooksBinding binding;
    private AuthManager authManager;
    private DatabaseHelper dbHelper;
    private NotebookAdapter notebookAdapter;
    private List<Notebook> notebooks;
    private List<Notebook> allNotebooks;
    private User currentUser;
    private boolean searchMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotebooksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);
        dbHelper = DatabaseHelper.getInstance(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        loadNotebooks();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotebooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnBackSearch.setOnClickListener(v -> exitSearchMode());
        binding.btnCloseSearch.setOnClickListener(v -> exitSearchMode());
    }

    private void enterSearchMode() {
        searchMode = true;
        binding.toolbar.setVisibility(View.GONE);
        binding.searchToolbar.setVisibility(View.VISIBLE);
        binding.fabNewNotebook.setVisibility(View.GONE);
        
        binding.etSearch.setText("");
        binding.etSearch.requestFocus();
        
        // Show keyboard with delay
        binding.etSearch.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void exitSearchMode() {
        searchMode = false;
        binding.searchToolbar.setVisibility(View.GONE);
        binding.toolbar.setVisibility(View.VISIBLE);
        binding.fabNewNotebook.setVisibility(View.VISIBLE);
        
        binding.etSearch.setText("");
        
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        }
        
        // Reset to all notebooks and clear highlighting
        notebooks.clear();
        notebooks.addAll(allNotebooks);
        notebookAdapter.setSearchQuery("");
    }

    private void filterNotebooks(String query) {
        notebooks.clear();
        
        if (query.isEmpty()) {
            notebooks.addAll(allNotebooks);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Notebook notebook : allNotebooks) {
                if (notebook.getTitle().toLowerCase().contains(lowerQuery)) {
                    notebooks.add(notebook);
                }
            }
        }
        
        // Pass search query to adapter for highlighting
        notebookAdapter.setSearchQuery(query);
    }

    private void setupRecyclerView() {
        notebooks = new ArrayList<>();
        notebookAdapter = new NotebookAdapter(this, notebooks, new NotebookAdapter.OnNotebookClickListener() {
            @Override
            public void onNotebookClick(Notebook notebook) {
                // Open notebook editor directly
                Intent intent = new Intent(NotebooksActivity.this, MultiPageEditorActivity.class);
                intent.putExtra("NOTEBOOK_ID", notebook.getId());
                intent.putExtra("NOTEBOOK_NAME", notebook.getTitle());
                startActivity(intent);
            }

            @Override
            public void onPinClick(Notebook notebook) {
                dbHelper.toggleNotebookPin(notebook.getId());
                loadNotebooks();
            }

            @Override
            public void onDeleteClick(Notebook notebook) {
                dbHelper.deleteNotebook(notebook.getId());
                loadNotebooks();
                android.widget.Toast.makeText(NotebooksActivity.this,
                        "Notebook deleted",
                        android.widget.Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onExportPdfClick(Notebook notebook) {
                exportNotebookToPdf(notebook);
            }
        });

        binding.recyclerViewNotebooks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewNotebooks.setAdapter(notebookAdapter);
    }

    private void setupClickListeners() {
        binding.fabNewNotebook.setOnClickListener(v -> {
            Intent intent = new Intent(NotebooksActivity.this, CreateNotebookActivity.class);
            startActivity(intent);
        });
    }

    private void loadNotebooks() {
        allNotebooks = dbHelper.getUserNotebooks(currentUser.getId());
        notebooks.clear();
        notebooks.addAll(allNotebooks);
        notebookAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (notebooks.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewNotebooks.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewNotebooks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notebooks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            enterSearchMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (searchMode) {
            exitSearchMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotebooks();
    }

    private void exportNotebookToPdf(Notebook notebook) {
        // Get all pages for this notebook
        List<Page> pages = dbHelper.getNotebookPages(notebook.getId());
        
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
            performPdfExport(notebook, pages, pdfName);
        });
        
        nameDialog.setNegativeButton("Cancel", null);
        nameDialog.show();
    }
    
    private void performPdfExport(Notebook notebook, List<Page> pages, String pdfName) {
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

                // Save PDF to scanned_documents folder (My Documents)
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
        // Render page content to PDF by parsing JSON
        String content = page.getContent();
        if (content == null || content.isEmpty()) {
            // Draw empty page message
            Paint paint = new Paint();
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(18f);
            paint.setAntiAlias(true);
            canvas.drawText("Empty page", 50, 100, paint);
            return;
        }

        try {
            JSONObject json = new JSONObject(content);
            
            // Render paths (drawings)
            if (json.has("paths")) {
                JSONArray pathsArray = json.getJSONArray("paths");
                for (int i = 0; i < pathsArray.length(); i++) {
                    JSONObject pathObj = pathsArray.getJSONObject(i);
                    JSONArray pointsArray = pathObj.getJSONArray("points");
                    
                    int color = pathObj.optInt("color", Color.BLACK);
                    float strokeWidth = (float) pathObj.optDouble("strokeWidth", 5.0);
                    boolean isDashed = pathObj.optBoolean("isDashed", false);
                    boolean isFilled = pathObj.optBoolean("isFilled", false);
                    
                    Paint pathPaint = new Paint();
                    pathPaint.setColor(color);
                    pathPaint.setStrokeWidth(strokeWidth);
                    pathPaint.setAntiAlias(true);
                    
                    if (isFilled) {
                        pathPaint.setStyle(Paint.Style.FILL);
                    } else {
                        pathPaint.setStyle(Paint.Style.STROKE);
                        pathPaint.setStrokeJoin(Paint.Join.ROUND);
                        pathPaint.setStrokeCap(Paint.Cap.ROUND);
                    }
                    
                    Path path = new Path();
                    if (pointsArray.length() >= 2) {
                        path.moveTo((float)pointsArray.getDouble(0), (float)pointsArray.getDouble(1));
                        for (int j = 2; j < pointsArray.length(); j += 2) {
                            if (j + 1 < pointsArray.length()) {
                                path.lineTo((float)pointsArray.getDouble(j), (float)pointsArray.getDouble(j + 1));
                            }
                        }
                    }
                    canvas.drawPath(path, pathPaint);
                }
            }
            
            // Render text elements
            if (json.has("texts")) {
                JSONArray textsArray = json.getJSONArray("texts");
                for (int i = 0; i < textsArray.length(); i++) {
                    JSONObject textObj = textsArray.getJSONObject(i);
                    String text = textObj.getString("text");
                    float x = (float) textObj.getDouble("x");
                    float y = (float) textObj.getDouble("y");
                    float textSize = (float) textObj.optDouble("textSize", 40.0);
                    int textColor = textObj.optInt("textColor", Color.BLACK);
                    int bgColor = textObj.optInt("backgroundColor", Color.TRANSPARENT);
                    float rotation = (float) textObj.optDouble("rotation", 0);
                    boolean isBold = textObj.optBoolean("isBold", false);
                    boolean isItalic = textObj.optBoolean("isItalic", false);
                    boolean isSticky = textObj.optBoolean("isSticky", false);
                    
                    Paint textPaint = new Paint();
                    textPaint.setColor(textColor);
                    textPaint.setTextSize(textSize);
                    textPaint.setAntiAlias(true);
                    
                    int style = Typeface.NORMAL;
                    if (isBold && isItalic) {
                        style = Typeface.BOLD_ITALIC;
                    } else if (isBold) {
                        style = Typeface.BOLD;
                    } else if (isItalic) {
                        style = Typeface.ITALIC;
                    }
                    textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
                    
                    canvas.save();
                    
                    // Draw sticky note background if needed
                    if (isSticky && bgColor != Color.TRANSPARENT) {
                        Paint bgPaint = new Paint();
                        bgPaint.setColor(bgColor);
                        bgPaint.setStyle(Paint.Style.FILL);
                        
                        Paint.FontMetrics fm = textPaint.getFontMetrics();
                        float textHeight = fm.descent - fm.ascent;
                        float textWidth = textPaint.measureText(text);
                        
                        canvas.drawRect(x - 10, y + fm.ascent - 10, x + textWidth + 10, y + fm.descent + 10, bgPaint);
                    }
                    
                    // Apply rotation
                    if (rotation != 0) {
                        canvas.rotate(rotation, x, y);
                    }
                    
                    canvas.drawText(text, x, y, textPaint);
                    canvas.restore();
                }
            }
            
            // Render images
            if (json.has("images")) {
                JSONArray imagesArray = json.getJSONArray("images");
                for (int i = 0; i < imagesArray.length(); i++) {
                    JSONObject imageObj = imagesArray.getJSONObject(i);
                    String path = imageObj.optString("path", null);
                    
                    if (path != null && !path.isEmpty()) {
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
                        if (bitmap != null) {
                            float x = (float) imageObj.optDouble("x", 50);
                            float y = (float) imageObj.optDouble("y", 50);
                            float width = (float) imageObj.optDouble("width", 200);
                            float height = (float) imageObj.optDouble("height", 200);
                            float rotation = (float) imageObj.optDouble("rotation", 0);
                            boolean flipH = imageObj.optBoolean("flipHorizontal", false);
                            boolean flipV = imageObj.optBoolean("flipVertical", false);
                            
                            canvas.save();
                            float centerX = x + width / 2;
                            float centerY = y + height / 2;
                            canvas.translate(centerX, centerY);
                            canvas.rotate(rotation);
                            canvas.scale(flipH ? -1f : 1f, flipV ? -1f : 1f);
                            canvas.translate(-centerX, -centerY);
                            
                            android.graphics.RectF destRect = new android.graphics.RectF(x, y, x + width, y + height);
                            canvas.drawBitmap(bitmap, null, destRect, null);
                            canvas.restore();
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Draw error message
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTextSize(14f);
            paint.setAntiAlias(true);
            canvas.drawText("Error rendering page: " + e.getMessage(), 50, 100, paint);
        }
    }
}
