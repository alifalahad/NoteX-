package com.example.notex;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private String searchQuery = "";

    public interface OnNotebookClickListener {
        void onNotebookClick(Notebook notebook);

        void onPinClick(Notebook notebook);

        void onDeleteClick(Notebook notebook);

        void onExportPdfClick(Notebook notebook);
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

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
        notifyDataSetChanged();
    }

    class NotebookViewHolder extends RecyclerView.ViewHolder {

        TextView tvNotebookTitle;
        TextView tvPageCount;
        TextView tvLastUpdated;
        ImageButton btnMoreOptions;
        com.google.android.material.card.MaterialCardView notebookCard;

        public NotebookViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNotebookTitle = itemView.findViewById(R.id.tvNotebookTitle);
            tvPageCount = itemView.findViewById(R.id.tvPageCount);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
            notebookCard = itemView.findViewById(R.id.notebookCard);
        }

        public void bind(Notebook notebook) {
            // Highlight matching text if search query exists
            String title = notebook.getTitle();
            if (!searchQuery.isEmpty() && title.toLowerCase().contains(searchQuery.toLowerCase())) {
                SpannableString spannableTitle = new SpannableString(title);
                int startIndex = title.toLowerCase().indexOf(searchQuery.toLowerCase());
                if (startIndex >= 0) {
                    int endIndex = startIndex + searchQuery.length();
                    spannableTitle.setSpan(
                        new BackgroundColorSpan(Color.parseColor("#FFEB3B")),
                        startIndex,
                        endIndex,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
                tvNotebookTitle.setText(spannableTitle);
            } else {
                tvNotebookTitle.setText(title);
            }
            
            tvPageCount.setText(notebook.getPageCount() + " pages");
            tvLastUpdated.setText("Updated recently");

            // Set plain solid card color
            try {
                int color = Color.parseColor(notebook.getColor());
                notebookCard.setCardBackgroundColor(color);
            } catch (Exception e) {
                // Default color if parsing fails
                int defaultColor = Color.parseColor("#5DADE2");
                notebookCard.setCardBackgroundColor(defaultColor);
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
                    if (id == R.id.action_export_pdf) {
                        if (listener != null) {
                            listener.onExportPdfClick(notebook);
                        }
                        return true;
                    } else if (id == R.id.action_pin) {
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
