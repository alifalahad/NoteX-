package com.example.notex.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.R;
import com.example.notex.models.ScannedDocument;

import java.util.List;
import java.util.Set;

public class ScannedDocumentAdapter extends RecyclerView.Adapter<ScannedDocumentAdapter.DocumentViewHolder> {
    
    private Context context;
    private List<ScannedDocument> documents;
    private OnDocumentInteractionListener listener;
    private boolean selectionMode = false;
    private Set<ScannedDocument> selectedDocuments;
    private String searchQuery = "";
    
    public interface OnDocumentInteractionListener {
        void onDocumentClick(ScannedDocument document);
        void onDocumentLongClick(ScannedDocument document);
    }
    
    public ScannedDocumentAdapter(Context context, List<ScannedDocument> documents, OnDocumentInteractionListener listener) {
        this.context = context;
        this.documents = documents;
        this.listener = listener;
    }
    
    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }
    
    public void setSelectedDocuments(Set<ScannedDocument> selectedDocuments) {
        this.selectedDocuments = selectedDocuments;
    }
    
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
    }
    
    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        ScannedDocument document = documents.get(position);
        
        // Highlight matching text if searching
        if (!searchQuery.isEmpty() && document.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            SpannableString spannableString = new SpannableString(document.getName());
            int startPos = document.getName().toLowerCase().indexOf(searchQuery.toLowerCase());
            int endPos = startPos + searchQuery.length();
            
            // Yellow highlight
            spannableString.setSpan(new BackgroundColorSpan(0xFFFFEB3B), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Bold text
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            holder.tvDocumentName.setText(spannableString);
        } else {
            holder.tvDocumentName.setText(document.getName());
        }
        
        holder.tvDocumentInfo.setText(document.getDocumentInfo());
        holder.tvDocumentDate.setText(document.getFormattedDate());
        
        // Set document icon based on type
        if (document.getFileType().equals("PDF")) {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_pdf);
        } else if (document.getFileType().equals("TXT")) {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_text_file);
        } else {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_image);
        }
        
        // Handle selection mode
        if (selectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedDocuments != null && selectedDocuments.contains(document));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }
        
        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClick(document);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDocumentLongClick(document);
            }
            return true;
        });
    }
    
    @Override
    public int getItemCount() {
        return documents.size();
    }
    
    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        ImageView ivDocumentIcon;
        TextView tvDocumentName;
        TextView tvDocumentInfo;
        TextView tvDocumentDate;
        
        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxSelect);
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
            tvDocumentName = itemView.findViewById(R.id.tvDocumentName);
            tvDocumentInfo = itemView.findViewById(R.id.tvDocumentInfo);
            tvDocumentDate = itemView.findViewById(R.id.tvDocumentDate);
        }
    }
}
