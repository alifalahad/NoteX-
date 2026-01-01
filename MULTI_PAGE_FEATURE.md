# Multi-Page Notebook Feature

## Overview
The multi-page notebook feature allows users to create notebooks with multiple scrollable pages where they can write text, draw, add photos, and record voice notes.

## Features

### ✨ Key Capabilities
1. **Multiple Pages in One Notebook**
   - Add unlimited pages to a single notebook
   - Each page has its own canvas for content
   - Pages are numbered automatically

2. **Vertical Scrolling**
   - Scroll seamlessly through all pages
   - Write/draw anywhere on any page
   - No need to navigate between separate screens

3. **Unified Save Function**
   - Single "Save" button in the toolbar
   - Saves all pages at once
   - Prompts before discarding unsaved changes

4. **All Drawing Modes Supported**
   - ✏️ Text Mode - Tap anywhere to type
   - 🖊️ Draw Mode - Freehand drawing
   - 📷 Photo Mode - Add images (coming soon)
   - 🎤 Voice Mode - Record audio (coming soon)

## How to Use

### Creating a Multi-Page Notebook

1. **Open a Notebook**
   - Navigate to your notebooks list
   - Select a notebook

2. **Create New Note**
   - Tap the "+" (FAB) button
   - Choose "📄 Multi-Page Notebook"

3. **Add Content**
   - The first page is created automatically
   - Select a mode (Text/Draw/Photo/Voice)
   - Tap/write anywhere on the page

4. **Add More Pages**
   - Tap "➕ Add Page" in the toolbar
   - New page appears at the bottom
   - Scroll down to access it

5. **Save Your Work**
   - Tap "💾 Save" in the toolbar
   - All pages are saved together
   - You'll see a confirmation message

### Editing Existing Multi-Page Notebooks

1. Open the notebook from your list
2. Tap on the multi-page note
3. Scroll through pages to edit
4. Tap "💾 Save" when done

## UI Components

### Toolbar
- **Close Button** (←) - Exit with save prompt if changes exist
- **Save Button** (💾) - Save all pages immediately

### Mode Toolbar (Horizontal Scroll)
- **✏️ Text** - Text input mode
- **🖊️ Draw** - Drawing mode
- **📷 Photo** - Photo insertion
- **🎤 Voice** - Voice recording
- **|** - Separator
- **➕ Add Page** - Create new page

### Status Indicator (Bottom)
- Current mode indicator
- Total page count

## Technical Details

### Storage Format
- All pages stored as a single JSON object
- Each page contains its own canvas data
- Format:
```json
{
  "pages": [
    {
      "pageNumber": 1,
      "content": "{...canvas data...}"
    },
    {
      "pageNumber": 2,
      "content": "{...canvas data...}"
    }
  ],
  "totalPages": 2
}
```

### Page Layout
- Each page has a fixed height (1400px)
- A4-like aspect ratio
- Page number header
- Light gray separator between pages

### Auto-Save Behavior
- No automatic saving during editing
- Manual save via "💾 Save" button
- Prompts on exit if unsaved changes exist
- Text inputs finalized on app pause

## Comparison: Single Page vs Multi-Page

| Feature | Single Page | Multi-Page |
|---------|-------------|------------|
| Pages per note | 1 | Unlimited |
| Navigation | N/A | Vertical scroll |
| Save action | Auto on exit | Manual save button |
| Use case | Quick notes | Long documents, lectures |

## Tips

1. **Performance**: Each page is a separate canvas, so very large numbers of pages (50+) may affect performance
2. **Content Organization**: Use page numbers to organize content logically
3. **Saving**: Remember to save frequently when working on complex notebooks
4. **Mode Switching**: Finish text input before switching modes to preserve content

## Future Enhancements

- Page reordering (drag & drop)
- Page deletion
- Page duplication
- Custom page sizes
- Page templates
- Export individual pages
- Print preview with page breaks
