package com.fahim.alyfobserver.ui;

import android.content.ClipData;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Import for ContextCompat

import com.fahim.alyfobserver.R;

import java.util.ArrayList;
import java.util.List;

public class ButtonCustomizationActivity extends AppCompatActivity {

    private GridLayout buttonGrid;
    private EditText editTextRows, editTextColumns;
    private Button buttonApplyGrid;
    private List<OverlayButtonConfig> availableOverlayButtons; // List of available overlay buttons
    private List<Button> currentDraggableButtons; // Buttons currently in the grid

    // Data class to hold button configuration
    public static class OverlayButtonConfig {
        public String text;
        public String iconName; // Using String for icon name for now (e.g., "Info", "Edit")

        public OverlayButtonConfig(String text, String iconName) {
            this.text = text;
            this.iconName = iconName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customization_layout);

        buttonGrid = findViewById(R.id.buttonGrid);
        editTextRows = findViewById(R.id.editTextRows);
        editTextColumns = findViewById(R.id.editTextColumns);
        buttonApplyGrid = findViewById(R.id.buttonApplyGrid);

        // Initialize available overlay buttons
        availableOverlayButtons = new ArrayList<>();
        availableOverlayButtons.add(new OverlayButtonConfig("Dump UI", "Info"));
        availableOverlayButtons.add(new OverlayButtonConfig("Write Text", "Edit"));
        availableOverlayButtons.add(new OverlayButtonConfig("Close", "Close"));
        availableOverlayButtons.add(new OverlayButtonConfig("Show Paste Layout", "ContentPaste"));
        availableOverlayButtons.add(new OverlayButtonConfig("Data", "Build"));
        availableOverlayButtons.add(new OverlayButtonConfig("Show WebView Layout", "Web"));

        currentDraggableButtons = new ArrayList<>();

        buttonApplyGrid.setOnClickListener(v -> applyGridSettings());

        // Initial grid setup
        applyGridSettings();
    }

    private void applyGridSettings() {
        int rows = 0;
        int cols = 0;
        try {
            rows = Integer.parseInt(editTextRows.getText().toString());
            cols = Integer.parseInt(editTextColumns.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for rows and columns", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rows <= 0 || cols <= 0) {
            Toast.makeText(this, "Rows and columns must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIX: Clear all views BEFORE setting row and column counts
        buttonGrid.removeAllViews();
        currentDraggableButtons.clear(); // Also clear this list here

        buttonGrid.setRowCount(rows);
        buttonGrid.setColumnCount(cols);

        setupGrid();
    }

    private void setupGrid() {
        // Add empty cells as drop targets
        for (int i = 0; i < buttonGrid.getRowCount() * buttonGrid.getColumnCount(); i++) {
            View emptyCell = new View(this);
            emptyCell.setBackgroundColor(Color.parseColor("#CCCCCC")); // Light gray for empty cells
            emptyCell.setTag("empty_cell_" + i); // Tag to identify empty cells
            emptyCell.setOnDragListener(new MyDragListener());

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(4, 4, 4, 4);
            emptyCell.setLayoutParams(lp);
            buttonGrid.addView(emptyCell);
        }

        // Dynamically add available overlay buttons to the grid (initially to the first available slots)
        int buttonIndex = 0;
        for (int i = 0; i < buttonGrid.getChildCount(); i++) {
            View child = buttonGrid.getChildAt(i);
            if (child.getTag() != null && child.getTag().toString().startsWith("empty_cell")) {
                if (buttonIndex < availableOverlayButtons.size()) {
                    OverlayButtonConfig config = availableOverlayButtons.get(buttonIndex);
                    Button buttonToPlace = createButtonFromConfig(config);
                    buttonToPlace.setOnTouchListener(new MyTouchListener());
                    buttonToPlace.setVisibility(View.VISIBLE);

                    currentDraggableButtons.add(buttonToPlace);

                    buttonGrid.removeViewAt(i); // Remove the empty cell
                    buttonGrid.addView(buttonToPlace, i); // Add the button in its place
                    buttonIndex++;
                }
            }
        }
    }

    private Button createButtonFromConfig(OverlayButtonConfig config) {
        Button button = new Button(this); // Use default Button constructor
        button.setText(config.text);
        button.setTag(config); // Store the config in the button's tag

        // Apply FAB-like styling
        int fabSize = (int) (56 * getResources().getDisplayMetrics().density); // 56dp to pixels
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = fabSize;
        layoutParams.height = fabSize;
        button.setLayoutParams(layoutParams);

        // Set background tint (using purple_200 as an example)
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_200)));

        // Make it circular (requires a shape drawable or setting corner radius programmatically)
        // For simplicity, we'll rely on the default button shape and tint for now.
        // A more robust solution would involve creating a custom drawable with a corner radius.

        button.setPadding(0, 0, 0, 0); // Remove default padding
        button.setGravity(Gravity.CENTER); // Center text

        return button;
    }

    private static class MyTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                // Store the OverlayButtonConfig in the ClipData's local state
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDragAndDrop(data, shadowBuilder, view, 0); // Pass the view itself as local state
                view.setVisibility(View.INVISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }

    private class MyDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            // v is the View that the drag event is dispatched to (the target empty cell)
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // Highlight the empty cell when a draggable item enters it
                    if (v.getTag() != null && v.getTag().toString().startsWith("empty_cell")) {
                        v.setBackgroundColor(Color.parseColor("#99CCFF")); // Highlight empty cell
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // Revert the empty cell's color when a draggable item exits it
                    if (v.getTag() != null && v.getTag().toString().startsWith("empty_cell")) {
                        v.setBackgroundColor(Color.parseColor("#CCCCCC")); // Revert color
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    // A draggable item has been dropped on this empty cell (v)
                    View draggedView = (View) event.getLocalState(); // This is the original dragged view
                    // OverlayButtonConfig draggedConfig = (OverlayButtonConfig) draggedView.getTag(); // Retrieve config from the draggedView's tag
                    ViewGroup owner = (ViewGroup) draggedView.getParent(); // Original parent of the dragged view

                    // Ensure the drop target is an empty cell
                    if (v.getTag() != null && v.getTag().toString().startsWith("empty_cell")) {
                        int targetIndex = buttonGrid.indexOfChild(v); // Get the index of the target empty cell

                        // Capture original position BEFORE removing draggedView from its owner
                        int originalPosition = -1;
                        if (owner != null) {
                            originalPosition = owner.indexOfChild(draggedView);
                        }

                        // Remove the empty cell from the grid
                        buttonGrid.removeView(v);

                        // Remove the dragged view from its original parent
                        if (owner != null) {
                            owner.removeView(draggedView);
                        }

                        // Add the draggedView to the grid at the target index
                        // We need to ensure its LayoutParams are correct for the new position
                        int targetRow = targetIndex / buttonGrid.getColumnCount();
                        int targetColumn = targetIndex % buttonGrid.getColumnCount();

                        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                        lp.width = (int) (56 * getResources().getDisplayMetrics().density); // Use fixed size for button
                        lp.height = (int) (56 * getResources().getDisplayMetrics().density); // Use fixed size for button
                        lp.columnSpec = GridLayout.spec(targetColumn, 1f);
                        lp.rowSpec = GridLayout.spec(targetRow, 1f);
                        lp.setMargins(4, 4, 4, 4);
                        draggedView.setLayoutParams(lp);

                        buttonGrid.addView(draggedView, targetIndex);
                        draggedView.setVisibility(View.VISIBLE); // Make it visible at the new position

                        // Create a new empty cell to replace the original position of the dragged button
                        if (originalPosition != -1) { // Only if we successfully captured the original position
                            View newEmptyCell = new View(ButtonCustomizationActivity.this);
                            newEmptyCell.setBackgroundColor(Color.parseColor("#CCCCCC"));
                            newEmptyCell.setTag("empty_cell_recreated_" + System.currentTimeMillis()); // Unique tag
                            newEmptyCell.setOnDragListener(new MyDragListener());

                            GridLayout.LayoutParams emptyLp = new GridLayout.LayoutParams();
                            emptyLp.width = 0;
                            emptyLp.height = 0;
                            emptyLp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                            emptyLp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                            emptyLp.setMargins(4, 4, 4, 4);
                            newEmptyCell.setLayoutParams(emptyLp);

                            // Add new empty cell at original position
                            buttonGrid.addView(newEmptyCell, originalPosition);
                        }
                        return true; // Indicate that the drop was handled
                    } else {
                        // If dropped on a non-empty cell or outside, revert visibility
                        draggedView.setVisibility(View.VISIBLE);
                        return false; // Indicate that the drop was not handled
                    }
                case DragEvent.ACTION_DRAG_ENDED:
                    View view = (View) event.getLocalState();
                    if (!event.getResult()) { // If the drop was not handled by ACTION_DROP
                        view.setVisibility(View.VISIBLE); // Make the dragged view visible again at its original spot
                    }
                    // Revert background color of any highlighted empty cells
                    for (int i = 0; i < buttonGrid.getChildCount(); i++) {
                        View child = buttonGrid.getChildAt(i);
                        if (child.getTag() != null && child.getTag().toString().startsWith("empty_cell")) {
                            child.setBackgroundColor(Color.parseColor("#CCCCCC"));
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}