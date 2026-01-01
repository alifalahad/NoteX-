package com.example.notex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiPageCanvasView - A vertical scrollable view containing multiple canvas pages
 * Users can write/draw on any page and scroll between pages
 */
public class MultiPageCanvasView extends LinearLayout {

    private List<PageCanvasHolder> pages;
    private CanvasView.Mode currentMode = CanvasView.Mode.TEXT;
    private OnPageChangeListener onPageChangeListener;

    public interface OnPageChangeListener {
        void onPageCountChanged(int pageCount);
    }

    public MultiPageCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(0xFFF5F5F5); // Light gray background to show pages
        pages = new ArrayList<>();
        
        // Add first page by default
        addNewPage();
    }

    public void setMode(CanvasView.Mode mode) {
        this.currentMode = mode;
        // Update mode for all existing pages
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setMode(mode);
        }
    }

    public void addNewPage() {
        PageCanvasHolder holder = new PageCanvasHolder(getContext(), pages.size() + 1);
        holder.canvasView.setMode(currentMode);
        pages.add(holder);
        addView(holder.rootView);
        
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageCountChanged(pages.size());
        }
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.onPageChangeListener = listener;
    }

    public void finishAllTextInputs() {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.finishTextInput();
        }
    }

    /**
     * Serialize all pages to JSON array
     */
    public String toJson() {
        try {
            JSONArray pagesArray = new JSONArray();
            
            for (int i = 0; i < pages.size(); i++) {
                JSONObject pageObj = new JSONObject();
                pageObj.put("pageNumber", i + 1);
                pageObj.put("content", pages.get(i).canvasView.toJson());
                pagesArray.put(pageObj);
            }
            
            JSONObject result = new JSONObject();
            result.put("pages", pagesArray);
            result.put("totalPages", pages.size());
            
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    /**
     * Load pages from JSON array
     */
    public void fromJson(String jsonString) {
        try {
            if (jsonString == null || jsonString.isEmpty()) {
                return;
            }

            JSONObject json = new JSONObject(jsonString);
            
            if (!json.has("pages")) {
                return;
            }

            JSONArray pagesArray = json.getJSONArray("pages");
            
            // Clear existing pages
            removeAllViews();
            pages.clear();
            
            // Load all pages
            for (int i = 0; i < pagesArray.length(); i++) {
                JSONObject pageObj = pagesArray.getJSONObject(i);
                String content = pageObj.getString("content");
                
                PageCanvasHolder holder = new PageCanvasHolder(getContext(), i + 1);
                holder.canvasView.setMode(currentMode);
                holder.canvasView.fromJson(content);
                pages.add(holder);
                addView(holder.rootView);
            }
            
            // Ensure at least one page exists
            if (pages.isEmpty()) {
                addNewPage();
            }
            
            if (onPageChangeListener != null) {
                onPageChangeListener.onPageCountChanged(pages.size());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // If loading fails, ensure at least one page
            if (pages.isEmpty()) {
                addNewPage();
            }
        }
    }

    public int getPageCount() {
        return pages.size();
    }

    /**
     * Holder class for each page with its canvas
     */
    private class PageCanvasHolder {
        View rootView;
        CanvasView canvasView;
        int pageNumber;

        PageCanvasHolder(Context context, int pageNumber) {
            this.pageNumber = pageNumber;
            
            // Create container for visible page
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(VERTICAL);
            container.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            container.setPadding(16, 16, 16, 16); // Padding to separate pages
            
            // Canvas view with visible border
            canvasView = new CanvasView(context, null);
            canvasView.setBackgroundColor(Color.WHITE);
            
            // Set height for each page
            LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1800 // Page height
            );
            canvasParams.setMargins(0, 0, 0, 0);
            canvasView.setLayoutParams(canvasParams);
            
            // Add shadow/border to make page visible
            canvasView.setElevation(6f);
            
            container.addView(canvasView);
            
            this.rootView = container;
        }
    }
}
