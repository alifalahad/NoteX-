package com.example.notex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScanThumbnailAdapter extends RecyclerView.Adapter<ScanThumbnailAdapter.ThumbnailViewHolder> {
    
    private List<ScanActivity.ScannedPage> pages;
    private OnPageDeleteListener deleteListener;
    
    public interface OnPageDeleteListener {
        void onPageDelete(int position);
    }
    
    public ScanThumbnailAdapter(List<ScanActivity.ScannedPage> pages, OnPageDeleteListener deleteListener) {
        this.pages = pages;
        this.deleteListener = deleteListener;
    }
    
    @NonNull
    @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_scan_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        ScanActivity.ScannedPage page = pages.get(position);
        holder.ivThumbnail.setImageBitmap(page.getBitmap());
        holder.tvPageNumber.setText(String.valueOf(position + 1));
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onPageDelete(position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return pages.size();
    }
    
    static class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvPageNumber;
        ImageButton btnDelete;
        
        ThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvPageNumber = itemView.findViewById(R.id.tvPageNumber);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
