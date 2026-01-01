package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityNotebooksBinding;
import com.example.notex.models.Notebook;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

/**
 * NotebooksActivity - Displays user's notebooks
 */
public class NotebooksActivity extends AppCompatActivity {

    private ActivityNotebooksBinding binding;
    private AuthManager authManager;
    private DatabaseHelper dbHelper;
    private NotebookAdapter notebookAdapter;
    private List<Notebook> notebooks;
    private User currentUser;

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
        loadNotebooks();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        notebooks = new ArrayList<>();
        notebookAdapter = new NotebookAdapter(this, notebooks, new NotebookAdapter.OnNotebookClickListener() {
            @Override
            public void onNotebookClick(Notebook notebook) {
                // Open notebook pages
                Intent intent = new Intent(NotebooksActivity.this, NotebookPagesActivity.class);
                intent.putExtra("NOTEBOOK_ID", notebook.getId());
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
        notebooks.clear();
        notebooks.addAll(dbHelper.getUserNotebooks(currentUser.getId()));
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
    protected void onResume() {
        super.onResume();
        loadNotebooks();
    }
}
