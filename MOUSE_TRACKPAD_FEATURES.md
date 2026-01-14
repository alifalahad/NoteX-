# Mouse & macOS Trackpad Features - NoteX Desktop

## ✅ Mouse Features Implemented

### 1. **Basic Mouse Operations**
- **Left Click**: Select and activate tools
- **Click & Hold (1ms delay)**: Force click simulation for special actions
- **Click & Drag**: Drawing, erasing, and shape creation
- **Right Click**: Context menus for tool options
- **Mouse Move**: Eraser cursor preview, hover effects
- **Mouse Wheel Scroll**: Vertical scrolling through pages

### 2. **Drawing Controls**
- **Paint Tool**: 
  - Click to start drawing
  - Drag to draw continuous lines
  - Stroke width adjustment
  - Color selection
  - Pen styles (Normal, Pencil, Highlighter)
  
- **Eraser Tool**:
  - Real-time cursor visualization (red circle)
  - Cursor follows mouse exactly
  - Two modes: Partial (immediate) and Fill Line (force click)
  - Adjustable eraser size

- **Text Tool**:
  - Force click creates text field
  - Click to place cursor
  - Drag text fields to reposition

- **Shape Tool**:
  - Force click to start
  - Drag to define shape size
  - 14 shapes available (Rectangle, Circle, Triangle, Line, Arrow, Pentagon, Star, Hexagon, Diamond, Oval, Right Triangle, Parallelogram, Cross, Heart)

- **Laser Pointer**:
  - Click & hold to draw red laser
  - Automatically fades out over 2 seconds
  - Non-permanent (disappears after fade)

- **Image Tool**:
  - Force click opens file chooser
  - Click to place image on canvas
  - Auto-resize to max 300x300px

- **Voice Tool**:
  - Force click creates NoteX-style waveform widget
  - Animated waveform visualization
  - Auto-saves after 5 seconds

### 3. **Mouse Button Mapping**
- **Left Button**: Primary action (draw, select, place)
- **Right Button**: Context menu for tool settings
- **Middle Button**: Not currently mapped (could add for quick tool switch)

---

## ✅ macOS Trackpad Features Implemented

### 1. **Force Touch / Force Click**
- **Simulated with 1ms timer** - immediate response on press
- **Activates special features**:
  - Text field creation
  - Shape drawing mode
  - Image insertion
  - Voice recording
  - Fill-line eraser mode

### 2. **Pinch to Zoom** (Two-finger pinch)
- **Zoom In**: Pinch outward
- **Zoom Out**: Pinch inward
- **Zoom Range**: 0.5x to 3.0x
- **Smart Scaling**: Stroke width adjusts inversely to maintain visual consistency
- **Live Preview**: Real-time zoom feedback
- **Canvas-specific**: Each page can have independent zoom level

### 3. **Rotation Gesture** (Two-finger rotate)
- **Rotate Canvas**: Twist two fingers to rotate the canvas
- **Full 360° rotation**: No limits on rotation angle
- **Persistent**: Rotation state maintained per canvas
- **Visual Feedback**: Real-time rotation preview

### 4. **Two-Finger Scroll**
- **Vertical Scroll**: Navigate through multiple pages
- **Smooth Scrolling**: Native macOS momentum scrolling
- **Inertia Support**: Continues scrolling with momentum
- **Automatic**: Handled by ScrollPane with trackpad optimization

### 5. **Three-Finger Swipe**
- **Swipe Left**: Navigate to next page
- **Swipe Right**: Navigate to previous page
- **Swipe Up**: Scroll to top of document
- **Swipe Down**: Scroll to bottom of document
- **Page Switching**: Automatically saves current page before switching

### 6. **Two-Finger Tap** (Right-Click)
- **Tool Options**: Opens context menu for selected tool
- **Quick Settings**: Access pen colors, shapes, eraser types
- **Same as Right-Click**: Full parity with mouse right-click

### 7. **Smart Zoom** (Two-finger double-tap)
- **Context-Aware**: Detected via zoom events
- **Quick Toggle**: Double-tap to toggle between 1x and 2x zoom

### 8. **Trackpad-Specific Optimizations**
- **Touch Count Detection**: Differentiates between mouse and trackpad
- **Inertia Handling**: Smooth deceleration for natural feel
- **Pressure Sensitivity**: Ready for future pressure-based features
- **Gesture Conflict Resolution**: Prevents accidental tool activation during gestures

---

## 🎯 Feature Matrix

| Feature | Mouse | Trackpad | Notes |
|---------|-------|----------|-------|
| **Basic Click** | ✅ | ✅ | 1ms delay for force click |
| **Right-Click Menu** | ✅ | ✅ Two-finger tap | Context menus |
| **Drag to Draw** | ✅ | ✅ | All drawing tools |
| **Scroll** | ✅ Wheel | ✅ Two-finger | Vertical navigation |
| **Zoom** | ❌ | ✅ Pinch | 0.5x - 3.0x range |
| **Rotate** | ❌ | ✅ Two-finger twist | Full 360° |
| **Swipe Navigation** | ❌ | ✅ Three-finger | Page switching |
| **Force Touch** | ❌ | ✅ Simulated | Special actions |
| **Cursor Preview** | ✅ | ✅ | Eraser tool |
| **Pressure Sensitivity** | ❌ | 🔄 Framework ready | Future feature |

---

## 🛠️ Technical Implementation

### Mouse Event Handlers
```java
canvas.setOnMousePressed()    // Click detection
canvas.setOnMouseDragged()    // Drawing while dragging
canvas.setOnMouseReleased()   // End drawing action
canvas.setOnMouseMoved()      // Cursor tracking
canvas.setOnMouseExited()     // Hide cursor when leaving canvas
```

### Trackpad Gesture Handlers
```java
// Zoom (Pinch)
canvas.setOnZoom()            // Active zoom
canvas.setOnZoomStarted()     // Begin pinch
canvas.setOnZoomFinished()    // End pinch

// Rotation
canvas.setOnRotate()          // Active rotate
canvas.setOnRotationStarted() // Begin twist
canvas.setOnRotationFinished()// End twist

// Scroll
canvas.setOnScroll()          // Two-finger scroll
canvas.setOnScrollStarted()   // Begin scroll
canvas.setOnScrollFinished()  // End scroll

// Swipe
canvas.setOnSwipeLeft()       // Next page
canvas.setOnSwipeRight()      // Previous page
canvas.setOnSwipeUp()         // Scroll to top
canvas.setOnSwipeDown()       // Scroll to bottom
```

### Force Click Simulation
```java
private static final long FORCE_CLICK_DELAY_MS = 1;  // 1ms for immediate response

Timeline forceClickTimer = new Timeline(
    new KeyFrame(Duration.millis(1), event -> {
        isForcePressed = true;
        // Activate tool-specific actions
    })
);
```

---

## 🎨 User Experience Features

### Visual Feedback
- **Eraser Cursor**: Red circle shows exact erase area
- **Laser Pointer**: Fades smoothly over 2 seconds
- **Zoom Indicator**: Canvas scales in real-time
- **Rotation Preview**: Live rotation feedback

### Smart Behaviors
- **Gesture Priority**: Drawing disabled during zoom/rotate
- **Auto-Save**: Canvas state saved before gestures
- **Undo/Redo**: All actions tracked (max 50 levels)
- **Conflict Prevention**: Gestures don't trigger tool actions

### Accessibility
- **Two-Finger Tap**: Alternative to right-click
- **Zoom**: Up to 3x for better visibility
- **Swipe Navigation**: Quick page switching without clicking
- **Consistent Controls**: Same experience across mouse and trackpad

---

## 📝 Usage Tips

### For Mouse Users
1. **Right-click** any tool button to access settings
2. **Click & hold** (1ms) on canvas to activate force click features
3. Use **mouse wheel** to scroll through pages
4. **Drag** while drawing to create continuous strokes

### For Trackpad Users
1. **Two-finger tap** for right-click menus
2. **Force touch** (press firmly) for special actions
3. **Pinch** to zoom in/out on canvas
4. **Twist** two fingers to rotate canvas
5. **Swipe left/right** to switch pages quickly
6. **Two-finger scroll** to navigate vertically

---

## 🔧 Customization Options

### Available Settings
- **Stroke Width**: Adjustable for pen and eraser
- **Colors**: 12-color palette
- **Pen Styles**: Normal, Pencil, Highlighter
- **Shape Types**: 14 different shapes
- **Eraser Types**: Partial (immediate) or Fill Line (force click)
- **Zoom Limits**: 0.5x to 3.0x (modifiable in code)

### Future Enhancements
- [ ] Pressure sensitivity for drawing thickness
- [ ] Custom gesture mapping
- [ ] Multi-touch drawing (two-finger simultaneous draw)
- [ ] Gesture customization panel
- [ ] Mouse button remapping
- [ ] Adjustable force click threshold

---

## 🎯 Gesture Best Practices

1. **Zoom**: Use pinch gesture when you need to see details
2. **Rotate**: Twist canvas for better drawing angles
3. **Swipe**: Quick navigation between pages
4. **Force Touch**: Precise tool activation (text, shapes, images)
5. **Two-Finger Scroll**: Natural page navigation

---

*Last Updated: January 14, 2026*
*NoteX Desktop - Advanced Mouse & Trackpad Support*
