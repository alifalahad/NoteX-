package com.example.notex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notex.models.Page;

import java.util.List;

/**
 * PageAdapter - RecyclerView adapter for displaying pages
 */
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

    private Context context;
    private List<Page> pages;
    private OnPageClickListener listener;

    public interface OnPageClickListener {
        void onPageClick(Page page);
    }

    public PageAdapter(Context context, List<Page> pages, OnPageClickListener listener) {
        this.context = context;
        this.pages = pages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Page page = pages.get(position);
        holder.bind(page);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    class PageViewHolder extends RecyclerView.ViewHolder {

        TextView tvPageTitle;
        TextView tvPagePreview;
        TextView tvPageNumber;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPageTitle = itemView.findViewById(R.id.tvPageTitle);
            tvPagePreview = itemView.findViewById(R.id.tvPagePreview);
            tvPageNumber = itemView.findViewById(R.id.tvPageNumber);
        }

        public void bind(Page page) {
            tvPageTitle.setText(page.getTitle());
            tvPageNumber.setText("Page " + page.getPageNumber());

            // Show preview of content
            String content = page.getContent();
            if (content != null && !content.trim().isEmpty()) {
                String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                tvPagePreview.setText(preview);
                tvPagePreview.setVisibility(View.VISIBLE);
            } else {
                tvPagePreview.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPageClick(page);
                }
            });
        }
    }
}
