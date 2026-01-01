package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.notex.databinding.ActivityNotebookPagesBinding;
import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.Notebook;
import com.example.notex.models.Page;

import java.util.ArrayList;
import java.util.List;

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
}
