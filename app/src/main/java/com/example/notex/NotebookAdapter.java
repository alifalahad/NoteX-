package com.example.notex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.models.Notebook;

import java.util.List;

/**
 * NotebookAdapter - RecyclerView adapter for displaying notebooks
 */
public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.NotebookViewHolder> {

    private Context context;
    private List<Notebook> notebooks;
    private OnNotebookClickListener listener;

    public interface OnNotebookClickListener {
        void onNotebookClick(Notebook notebook);

        void onPinClick(Notebook notebook);

        void onDeleteClick(Notebook notebook);
    }

    public NotebookAdapter(Context context, List<Notebook> notebooks, OnNotebookClickListener listener) {
        this.context = context;
        this.notebooks = notebooks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notebook, parent, false);
        return new NotebookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
        Notebook notebook = notebooks.get(position);
        holder.bind(notebook);
    }

    @Override
    public int getItemCount() {
        return notebooks.size();
    }

    class NotebookViewHolder extends RecyclerView.ViewHolder {

        TextView tvNotebookTitle;
        TextView tvPageCount;
        TextView tvLastUpdated;
        View colorIndicator;
        ImageView ivPinned;
        ImageButton btnMoreOptions;

        public NotebookViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNotebookTitle = itemView.findViewById(R.id.tvNotebookTitle);
            tvPageCount = itemView.findViewById(R.id.tvPageCount);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            ivPinned = itemView.findViewById(R.id.ivPinned);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }

        public void bind(Notebook notebook) {
            tvNotebookTitle.setText(notebook.getTitle());
            tvPageCount.setText(notebook.getPageCount() + " pages");
            tvLastUpdated.setText("Updated recently");

            // Set color indicator
            try {
                int color = Color.parseColor(notebook.getColor());
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setColor(color);
                drawable.setCornerRadius(8);
                colorIndicator.setBackground(drawable);
            } catch (Exception e) {
                // Default color if parsing fails
                colorIndicator.setBackgroundColor(Color.parseColor("#2196F3"));
            }

            // Show/hide pin icon
            if (notebook.isPinned()) {
                ivPinned.setVisibility(View.VISIBLE);
            } else {
                ivPinned.setVisibility(View.GONE);
            }

            // Item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotebookClick(notebook);
                }
            });

            // More options menu
            btnMoreOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, btnMoreOptions);
                popup.inflate(R.menu.menu_notebook_options);

                // Set pin/unpin text
                popup.getMenu().findItem(R.id.action_pin).setTitle(
                        notebook.isPinned() ? "Unpin" : "Pin");

                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_pin) {
                        if (listener != null) {
                            listener.onPinClick(notebook);
                        }
                        return true;
                    } else if (id == R.id.action_delete) {
                        if (listener != null) {
                            listener.onDeleteClick(notebook);
                        }
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
