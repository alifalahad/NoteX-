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
    private CanvasView.OnModeChangeListener modeChangeListener;
    private int activePageIndex = 0; // last interacted page

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
    
    public void setShapeType(CanvasView.ShapeType shape) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setShapeType(shape);
        }
    }
    
    public void setPenColor(int color) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setPenColor(color);
        }
    }
    
    public void setPenStyle(CanvasView.PenStyle style) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setPenStyle(style);
        }
    }
    
    public void setStrokeWidth(float width) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setStrokeWidth(width);
        }
    }
    
    public void setDashedLine(boolean dashed) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setDashedLine(dashed);
        }
    }
    
    public void setEraserSize(float size) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setEraserSize(size);
        }
    }
    
    public void setEraserMode(CanvasView.EraserMode mode) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setEraserMode(mode);
        }
    }
    
    public void setLaserMode(CanvasView.LaserMode mode) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setLaserMode(mode);
        }
    }
    
    public void setStickyNoteColor(int color) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setStickyNoteColor(color);
        }
    }
    
    public void setShapeColor(int color) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setShapeColor(color);
        }
    }
    
    public void setShapeStrokeWidth(float width) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setShapeStrokeWidth(width);
        }
    }
    
    public void setDashedShape(boolean dashed) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setDashedShape(dashed);
        }
    }
    
    public void setFilledShape(boolean filled) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setFilledShape(filled);
        }
    }
    
    // Text formatting methods
    public void setTextBold(boolean bold) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextBold(bold);
        }
    }
    
    public void setTextItalic(boolean italic) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextItalic(italic);
        }
    }
    
    public void setTextUnderline(boolean underline) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextUnderline(underline);
        }
    }
    
    public void setTextColor(int color) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextColor(color);
        }
    }
    
    public void setTextSize(float size) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextSize(size);
        }
    }
    
    public void editSelectedText() {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.editSelectedText();
        }
    }
    
    public void setTextEditMode(boolean enabled) {
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setTextEditMode(enabled);
        }
    }
    
    /**
     * Add an image to the currently active page
     */
    public void addImage(android.net.Uri imageUri) {
        if (!pages.isEmpty()) {
            int index = activePageIndex;
            if (index < 0 || index >= pages.size()) {
                index = 0;
            }
            pages.get(index).canvasView.addImage(imageUri);
        }
    }

    public void addNewPage() {
        final int newIndex = pages.size();
        PageCanvasHolder holder = new PageCanvasHolder(getContext(), newIndex + 1);
        holder.canvasView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activePageIndex = newIndex;
            }
            return false;
        });
        holder.canvasView.setMode(currentMode);
        
        // Set mode change listener for auto-switching
        if (modeChangeListener != null) {
            holder.canvasView.setOnModeChangeListener(modeChangeListener);
        }
        
        pages.add(holder);
        addView(holder.rootView);
        
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageCountChanged(pages.size());
        }
    }
    
    public void setOnModeChangeListener(CanvasView.OnModeChangeListener listener) {
        this.modeChangeListener = listener;
        // Also set for existing pages
        for (PageCanvasHolder holder : pages) {
            holder.canvasView.setOnModeChangeListener(listener);
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
     * Get the current active canvas (last interacted page)
     */
    public CanvasView getCurrentCanvas() {
        if (activePageIndex >= 0 && activePageIndex < pages.size()) {
            return pages.get(activePageIndex).canvasView;
        }
        return pages.isEmpty() ? null : pages.get(0).canvasView;
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
            activePageIndex = 0;
            
            // Load all pages
            for (int i = 0; i < pagesArray.length(); i++) {
                JSONObject pageObj = pagesArray.getJSONObject(i);
                String content = pageObj.getString("content");
                
                PageCanvasHolder holder = new PageCanvasHolder(getContext(), i + 1);
                final int pageIndex = i;
                holder.canvasView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        activePageIndex = pageIndex;
                    }
                    return false;
                });
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
     * Undo the last action on the currently visible/active page
     * For now, we'll undo on the first page
     */
    public boolean undo() {
        if (!pages.isEmpty()) {
            // Find the most recently interacted page or use the first one
            return pages.get(0).canvasView.undo();
        }
        return false;
    }

    /**
     * Redo the last undone action on the currently visible/active page
     */
    public boolean redo() {
        if (!pages.isEmpty()) {
            // Find the most recently interacted page or use the first one
            return pages.get(0).canvasView.redo();
        }
        return false;
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
