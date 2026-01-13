package com.example.notex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivityTextViewerBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class TextViewerActivity extends AppCompatActivity {
    
    private ActivityTextViewerBinding binding;
    private String filePath;
    private String originalContent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }
        
        filePath = getIntent().getStringExtra("filePath");
        if (filePath == null) {
            Toast.makeText(this, "Error: No file path provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        File file = new File(filePath);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(file.getName());
        }
        
        loadTextFile();
        setupListeners();
    }
    
    private void loadTextFile() {
        try {
            File file = new File(filePath);
            StringBuilder content = new StringBuilder();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            originalContent = content.toString();
            binding.etTextContent.setText(originalContent);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (hasChanges()) {
                showSaveDialog();
            } else {
                finish();
            }
        });
        
        binding.btnCancel.setOnClickListener(v -> {
            if (hasChanges()) {
                showDiscardDialog();
            } else {
                finish();
            }
        });
        
        binding.btnSave.setOnClickListener(v -> saveTextFile());
    }
    
    private boolean hasChanges() {
        String currentContent = binding.etTextContent.getText().toString();
        return !currentContent.equals(originalContent);
    }
    
    private void saveTextFile() {
        try {
            String content = binding.etTextContent.getText().toString();
            File file = new File(filePath);
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            
            originalContent = content;
            Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showSaveDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("Do you want to save your changes?")
            .setPositiveButton("Save", (dialog, which) -> saveTextFile())
            .setNegativeButton("Discard", (dialog, which) -> finish())
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    private void showDiscardDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard", (dialog, which) -> finish())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        if (hasChanges()) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }
}
