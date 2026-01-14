package com.example.notex;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.notex.adapters.ScannedDocumentAdapter;
import com.example.notex.databinding.ActivityScanDocumentsBinding;
import com.example.notex.models.ScannedDocument;
import com.example.notex.ScanUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanDocumentsActivity extends AppCompatActivity implements ScannedDocumentAdapter.OnDocumentInteractionListener {
    
    private ActivityScanDocumentsBinding binding;
    private ScannedDocumentAdapter adapter;
    private List<ScannedDocument> documents;
    private List<ScannedDocument> allDocuments; // Store all documents for search
    private Set<ScannedDocument> selectedDocuments;
    private boolean selectionMode = false;
    private boolean searchMode = false;
    
    private ActivityResultLauncher<Intent> scanLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make status bar transparent and extend content behind it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.toolbarPrimary));
        }
        
        binding = ActivityScanDocumentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }
        
        documents = new ArrayList<>();
        allDocuments = new ArrayList<>();
        selectedDocuments = new HashSet<>();
        
        setupRecyclerView();
        setupListeners();
        setupScanLauncher();
        loadDocuments();
    }
    
    private void setupRecyclerView() {
        adapter = new ScannedDocumentAdapter(this, documents, this);
        binding.recyclerDocuments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        binding.recyclerDocuments.setAdapter(adapter);
    }
    
    private void setupListeners() {
        binding.fabCamera.setOnClickListener(v -> launchScanner());
        
        binding.btnRename.setOnClickListener(v -> renameSelectedDocuments());
        binding.btnShare.setOnClickListener(v -> shareSelectedDocuments());
        binding.btnDelete.setOnClickListener(v -> deleteSelectedDocuments());
        
        // Search toolbar listeners
        binding.btnBackSearch.setOnClickListener(v -> exitSearchMode());
        binding.btnCloseSearch.setOnClickListener(v -> exitSearchMode());
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocuments(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }
    
    private void setupScanLauncher() {
        scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> imagePaths = result.getData().getStringArrayListExtra("imagePaths");
                    if (imagePaths != null && !imagePaths.isEmpty()) {
                        saveScannedDocument(imagePaths);
                    }
                }
            }
        );
    }
    
    private void launchScanner() {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("saveToLibrary", true);
        scanLauncher.launch(intent);
    }
    
    private void loadDocuments() {
        documents.clear();
        allDocuments.clear();
        
        File scansDir = new File(getExternalFilesDir(null), "scanned_documents");
        if (!scansDir.exists()) {
            scansDir.mkdirs();
        }
        
        File[] files = scansDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    ScannedDocument doc = createDocumentFromFile(file);
                    if (doc != null) {
                        documents.add(doc);
                    }
                }
            }
        }
        
        // Sort by creation date (newest first)
        documents.sort((d1, d2) -> Long.compare(d2.getCreatedAt(), d1.getCreatedAt()));
        
        // Keep a copy for search
        allDocuments.addAll(documents);
        
        updateUI();
    }
    
    private ScannedDocument createDocumentFromFile(File file) {
        String name = file.getName();
        String extension = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
        
        if (!extension.equals("PDF") && !extension.equals("TXT")) {
            return null;
        }
        
        ScannedDocument doc = new ScannedDocument();
        doc.setName(name);
        doc.setFilePath(file.getAbsolutePath());
        doc.setFileType(extension);
        doc.setFileSize(file.length());
        doc.setCreatedAt(file.lastModified());
        doc.setModifiedAt(file.lastModified());
        
        // For PDF, try to get page count
        if (extension.equals("PDF")) {
            int pageCount = ScanUtils.getPdfPageCount(file);
            doc.setPageCount(pageCount);
        } else {
            doc.setPageCount(1);
        }
        
        return doc;
    }
    
    private void saveScannedDocument(ArrayList<String> imagePaths) {
        new AlertDialog.Builder(this)
            .setTitle("Save Document")
            .setMessage("Enter document name")
            .setView(createEditTextView())
            .setPositiveButton("Save", (dialog, which) -> {
                android.widget.EditText editText = (android.widget.EditText) ((AlertDialog) dialog).findViewById(android.R.id.text1);
                String docName = editText.getText().toString().trim();
                if (docName.isEmpty()) {
                    docName = "Scan_" + System.currentTimeMillis();
                }
                
                savePdfDocument(docName, imagePaths);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private View createEditTextView() {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setId(android.R.id.text1);
        editText.setHint("Document name");
        editText.setSingleLine();
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = params.rightMargin = (int) (24 * getResources().getDisplayMetrics().density);
        editText.setLayoutParams(params);
        container.addView(editText);
        
        return container;
    }
    
    private void savePdfDocument(String docName, ArrayList<String> imagePaths) {
        new Thread(() -> {
            try {
                File scansDir = new File(getExternalFilesDir(null), "scanned_documents");
                if (!scansDir.exists()) {
                    scansDir.mkdirs();
                }
                
                File pdfFile = new File(scansDir, docName + ".pdf");
                
                // Generate PDF from images
                boolean success = ScanUtils.generatePDF(imagePaths, pdfFile);
                
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Document saved successfully", Toast.LENGTH_SHORT).show();
                        loadDocuments();
                    } else {
                        Toast.makeText(this, "Failed to save document", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void updateUI() {
        if (documents.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.recyclerDocuments.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.recyclerDocuments.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onDocumentClick(ScannedDocument document) {
        if (selectionMode) {
            toggleSelection(document);
        } else {
            openDocument(document);
        }
    }
    
    @Override
    public void onDocumentLongClick(ScannedDocument document) {
        if (!selectionMode) {
            enterSelectionMode();
            toggleSelection(document);
        }
    }
    
    private void toggleSelection(ScannedDocument document) {
        if (selectedDocuments.contains(document)) {
            selectedDocuments.remove(document);
        } else {
            selectedDocuments.add(document);
        }
        
        if (selectedDocuments.isEmpty()) {
            exitSelectionMode();
        } else {
            updateSelectionToolbar();
        }
        
        adapter.setSelectedDocuments(selectedDocuments);
        adapter.notifyDataSetChanged();
    }
    
    private void enterSelectionMode() {
        selectionMode = true;
        binding.selectionToolbar.setVisibility(View.VISIBLE);
        adapter.setSelectionMode(true);
        updateSelectionToolbar();
    }
    
    private void exitSelectionMode() {
        selectionMode = false;
        selectedDocuments.clear();
        binding.selectionToolbar.setVisibility(View.GONE);
        adapter.setSelectionMode(false);
        adapter.setSelectedDocuments(selectedDocuments);
        adapter.notifyDataSetChanged();
    }
    
    private void updateSelectionToolbar() {
        int count = selectedDocuments.size();
        binding.tvSelectionCount.setText(count + " selected");
    }
    
    private void openDocument(ScannedDocument document) {
        File file = new File(document.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Open TXT files in TextViewerActivity
        if (document.getFileType().equals("TXT")) {
            Intent intent = new Intent(this, TextViewerActivity.class);
            intent.putExtra("filePath", file.getAbsolutePath());
            startActivity(intent);
            return;
        }
        
        // Open PDF files with external viewer
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        
        if (document.getFileType().equals("PDF")) {
            intent.setDataAndType(uri, "application/pdf");
        }
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDocumentOptions(ScannedDocument document) {
        CharSequence[] options = {"Rename", "Share", "Delete"};
        
        new AlertDialog.Builder(this)
            .setTitle(document.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Rename
                        renameDocument(document);
                        break;
                    case 1: // Share
                        shareDocument(document);
                        break;
                    case 2: // Delete
                        deleteDocument(document);
                        break;
                }
            })
            .show();
    }
    
    private void renameDocument(ScannedDocument document) {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setText(document.getName().replace(".pdf", "").replace(".txt", ""));
        editText.setSingleLine();
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = params.rightMargin = (int) (24 * getResources().getDisplayMetrics().density);
        editText.setLayoutParams(params);
        container.addView(editText);
        
        new AlertDialog.Builder(this)
            .setTitle("Rename Document")
            .setView(container)
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    File oldFile = new File(document.getFilePath());
                    File newFile = new File(oldFile.getParent(), newName + "." + document.getFileType().toLowerCase());
                    
                    if (oldFile.renameTo(newFile)) {
                        document.setName(newFile.getName());
                        document.setFilePath(newFile.getAbsolutePath());
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to rename", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void shareDocument(ScannedDocument document) {
        File file = new File(document.getFilePath());
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (document.getFileType().equals("PDF")) {
            shareIntent.setType("application/pdf");
        } else {
            shareIntent.setType("text/plain");
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "Share document"));
    }
    
    private void deleteDocument(ScannedDocument document) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete " + document.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                File file = new File(document.getFilePath());
                if (file.delete()) {
                    documents.remove(document);
                    updateUI();
                    Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void renameSelectedDocuments() {
        if (selectedDocuments.isEmpty()) {
            Toast.makeText(this, "No documents selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDocuments.size() > 1) {
            Toast.makeText(this, "Please select only one document to rename", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ScannedDocument document = selectedDocuments.iterator().next();
        renameDocument(document);
    }
    
    private void shareSelectedDocuments() {
        if (selectedDocuments.isEmpty()) {
            Toast.makeText(this, "No documents selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ArrayList<Uri> uris = new ArrayList<>();
        for (ScannedDocument doc : selectedDocuments) {
            File file = new File(doc.getFilePath());
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            uris.add(uri);
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("*/*");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "Share documents"));
    }
    
    private void deleteSelectedDocuments() {
        if (selectedDocuments.isEmpty()) {
            Toast.makeText(this, "No documents selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Delete Documents")
            .setMessage("Are you sure you want to delete " + selectedDocuments.size() + " document(s)?")
            .setPositiveButton("Delete", (dialog, which) -> {
                int deletedCount = 0;
                for (ScannedDocument doc : new ArrayList<>(selectedDocuments)) {
                    File file = new File(doc.getFilePath());
                    if (file.delete()) {
                        documents.remove(doc);
                        deletedCount++;
                    }
                }
                
                Toast.makeText(this, "Deleted " + deletedCount + " document(s)", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
                updateUI();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_documents, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (selectionMode) {
                exitSelectionMode();
            } else {
                finish();
            }
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            if (!selectionMode) {
                enterSearchMode();
            } else {
                Toast.makeText(this, "Exit selection mode first", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == R.id.action_select) {
            if (!documents.isEmpty()) {
                enterSelectionMode();
            } else {
                Toast.makeText(this, "No documents to select", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == R.id.action_convert) {
            performOCR();
            return true;
        } else if (item.getItemId() == R.id.action_delete_all) {
            deleteAllDocuments();
            return true;
        } else if (item.getItemId() == R.id.action_export) {
            if (selectionMode && !selectedDocuments.isEmpty()) {
                shareSelectedDocuments();
            } else {
                Toast.makeText(this, "Please select documents to export", Toast.LENGTH_SHORT).show();
                if (!documents.isEmpty() && !selectionMode) {
                    enterSelectionMode();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void enterSearchMode() {
        if (allDocuments.isEmpty()) {
            Toast.makeText(this, "No documents to search", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Exit selection mode if active
        if (selectionMode) {
            exitSelectionMode();
        }
        
        searchMode = true;
        
        // Hide main toolbar and FAB
        binding.toolbar.setVisibility(View.GONE);
        binding.selectionToolbar.setVisibility(View.GONE);
        binding.fabCamera.setVisibility(View.GONE);
        
        // Show search toolbar
        binding.searchToolbar.setVisibility(View.VISIBLE);
        
        // Focus on search and show keyboard
        binding.etSearch.requestFocus();
        binding.etSearch.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }
    
    private void exitSearchMode() {
        searchMode = false;
        binding.searchToolbar.setVisibility(View.GONE);
        binding.toolbar.setVisibility(View.VISIBLE);
        binding.fabCamera.setVisibility(View.VISIBLE);
        binding.etSearch.setText("");
        
        // Clear search highlighting
        adapter.setSearchQuery("");
        
        // Reset to all documents
        documents.clear();
        documents.addAll(allDocuments);
        updateUI();
        
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
    }
    
    private void filterDocuments(String query) {
        documents.clear();
        
        if (query.trim().isEmpty()) {
            documents.addAll(allDocuments);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ScannedDocument doc : allDocuments) {
                if (doc.getName().toLowerCase().contains(lowerQuery)) {
                    documents.add(doc);
                }
            }
        }
        
        // Pass search query to adapter for highlighting
        adapter.setSearchQuery(query);
        updateUI();
    }
    
    private void deleteAllDocuments() {
        if (documents.isEmpty()) {
            Toast.makeText(this, "No documents to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Delete All Documents")
            .setMessage("Are you sure you want to delete all " + documents.size() + " document(s)?")
            .setPositiveButton("Delete All", (dialog, which) -> {
                int deletedCount = 0;
                for (ScannedDocument doc : new ArrayList<>(documents)) {
                    File file = new File(doc.getFilePath());
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
                documents.clear();
                Toast.makeText(this, "Deleted " + deletedCount + " document(s)", Toast.LENGTH_SHORT).show();
                updateUI();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void performOCR() {
        if (documents.isEmpty()) {
            Toast.makeText(this, "No documents available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If selection mode is active and documents are selected, convert those
        if (selectionMode && !selectedDocuments.isEmpty()) {
            showConversionOptions(new ArrayList<>(selectedDocuments));
        } else {
            // Otherwise show all documents
            showConversionOptions(documents);
        }
    }
    
    private void showConversionOptions(List<ScannedDocument> docsToConvert) {
        // Count PDF and TXT documents
        int pdfCount = 0;
        int txtCount = 0;
        for (ScannedDocument doc : docsToConvert) {
            if (doc.getFileType().equals("PDF")) {
                pdfCount++;
            } else if (doc.getFileType().equals("TXT")) {
                txtCount++;
            }
        }
        
        // Build options based on available document types
        List<String> optionsList = new ArrayList<>();
        if (pdfCount > 0) {
            optionsList.add("PDF to Text (" + pdfCount + " document" + (pdfCount > 1 ? "s" : "") + ")");
        }
        if (txtCount > 0) {
            optionsList.add("Text to PDF (" + txtCount + " document" + (txtCount > 1 ? "s" : "") + ")");
        }
        
        if (optionsList.isEmpty()) {
            Toast.makeText(this, "No convertible documents found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final int finalPdfCount = pdfCount;
        final int finalTxtCount = txtCount;
        
        CharSequence[] options = optionsList.toArray(new CharSequence[0]);
        
        new AlertDialog.Builder(this)
            .setTitle("Convert Documents")
            .setItems(options, (dialog, which) -> {
                if (finalPdfCount > 0 && which == 0) {
                    // PDF to Text
                    convertPdfToText(docsToConvert);
                } else {
                    // Text to PDF
                    convertTextToPdf(docsToConvert);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void convertPdfToText(List<ScannedDocument> allDocs) {
        List<ScannedDocument> pdfDocs = new ArrayList<>();
        for (ScannedDocument doc : allDocs) {
            if (doc.getFileType().equals("PDF")) {
                pdfDocs.add(doc);
            }
        }
        
        if (pdfDocs.isEmpty()) {
            Toast.makeText(this, "No PDF documents to convert", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create lightweight progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Converting PDFs to Text...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(pdfDocs.size());
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            int successCount = 0;
            for (int i = 0; i < pdfDocs.size(); i++) {
                ScannedDocument doc = pdfDocs.get(i);
                final int current = i + 1;
                final int total = pdfDocs.size();
                
                runOnUiThread(() -> {
                    progressDialog.setProgress(current);
                    int percentage = (current * 100) / total;
                    progressDialog.setMessage("Converting " + current + "/" + total + " (" + percentage + "%)");
                });
                
                File pdfFile = new File(doc.getFilePath());
                File txtFile = new File(pdfFile.getParent(), 
                    pdfFile.getName().replace(".pdf", "") + "_converted.txt");
                
                if (ScanUtils.convertPdfToText(this, pdfFile, txtFile)) {
                    successCount++;
                }
            }
            
            final int finalCount = successCount;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalCount > 0) {
                    Toast.makeText(this, "Converted " + finalCount + " PDF(s) to Text", Toast.LENGTH_LONG).show();
                    exitSelectionMode();
                    loadDocuments();
                } else {
                    Toast.makeText(this, "Conversion failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void convertTextToPdf(List<ScannedDocument> allDocs) {
        List<ScannedDocument> txtDocs = new ArrayList<>();
        for (ScannedDocument doc : allDocs) {
            if (doc.getFileType().equals("TXT")) {
                txtDocs.add(doc);
            }
        }
        
        if (txtDocs.isEmpty()) {
            Toast.makeText(this, "No Text documents to convert", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create lightweight progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Converting Text to PDFs...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(txtDocs.size());
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            int successCount = 0;
            for (int i = 0; i < txtDocs.size(); i++) {
                ScannedDocument doc = txtDocs.get(i);
                final int current = i + 1;
                final int total = txtDocs.size();
                
                runOnUiThread(() -> {
                    progressDialog.setProgress(current);
                    int percentage = (current * 100) / total;
                    progressDialog.setMessage("Converting " + current + "/" + total + " (" + percentage + "%)");
                });
                
                File txtFile = new File(doc.getFilePath());
                File pdfFile = new File(txtFile.getParent(), 
                    txtFile.getName().replace(".txt", "") + "_converted.pdf");
                
                if (ScanUtils.convertTextToPdf(txtFile, pdfFile)) {
                    successCount++;
                }
            }
            
            final int finalCount = successCount;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalCount > 0) {
                    Toast.makeText(this, "Converted " + finalCount + " Text file(s) to PDF", Toast.LENGTH_LONG).show();
                    exitSelectionMode();
                    loadDocuments();
                } else {
                    Toast.makeText(this, "Conversion failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    @Override
    public void onBackPressed() {
        if (searchMode) {
            exitSearchMode();
        } else if (selectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}
