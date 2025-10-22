# Alyf Observer

Alyf Observer is an Android application that demonstrates the use of an Accessibility Service to monitor the foreground application and display a floating overlay on top of a specific application (in this case, TikTok).

## Features

*   **Accessibility Service:** A service that runs in the background and gets notified when the user navigates between apps.
*   **TikTok Detection:** The accessibility service specifically checks if the foreground application is TikTok.
*   **Floating Overlay:** When TikTok is opened, a floating window is displayed on the screen. The overlay is hidden when the user navigates away from TikTok.
*   **Interactive Overlay:** The floating overlay is built with Jetpack Compose and contains buttons to:
    *   **Expand/Collapse:** Show or hide the menu of actions with smooth animations.
    *   **Dump UI Tree:** Log the entire view hierarchy of the current screen to logcat for debugging purposes.
    *   **Write Text:** Find an editable text field on the screen and write "hi" to it, followed by an automatic send/enter.
    *   **Close:** Hide the overlay.
    *   **Show Paste Layout:** Opens a layout with multiple pre-defined text options for quick pasting.
*   **Paste Layout:** A dedicated layout accessible from the main overlay, containing buttons for:
    *   **Promotional Text (⭐):** Pastes a pre-defined promotional message.
    *   **Payment Info (💵):** Pastes payment details.
    *   **Instruction (1️⃣):** Pastes instructions for the user.
    *   **Assurance (⚪):** Pastes a message assuring trustworthiness.
    *   **Delivery Time (🛑):** Pastes information about delivery time.
    *   **Close Paste Layout:** Closes the paste layout and returns to the main overlay.
    *   **Auto-Send on Paste:** All text pasted via these buttons (and the "Write Text" button) is automatically followed by an attempt to press a "Send" or "Enter" key.
*   **Keyboard-Aware Positioning:** The overlay now intelligently adjusts its vertical position to avoid being covered by the soft keyboard, and allows free vertical dragging when the keyboard is not active.
*   **Material You Theming:** The overlay's appearance now adapts to the system's Material You dynamic color theme.
*   **Idle State:** After a period of inactivity, the overlay collapses to a single button and then fades to a less intrusive state, moving to the edge of the screen.
*   **Draggable:** The overlay can be moved around the screen.
*   **Permission Handling:** The main activity guides the user to grant the necessary "draw over other apps" and "accessibility" permissions.
*   **Home Screen Widget:** A home screen widget that displays a website from the app's local assets.

## How it Works

1.  **Permissions:** The user launches the app and is prompted to grant the "draw over other apps" and "accessibility" permissions.
2.  **Accessibility Service:** `MyAccessibilityService` runs in the background, listening for window state changes.
3.  **App Detection:** When the service detects that TikTok has been opened, it sends a broadcast to `FloatingWindowService`.
4.  **Floating Window:** `FloatingWindowService` receives the broadcast and displays a floating overlay using `WindowManager`.
5.  **User Interaction:** The user can interact with the floating overlay, which in turn sends broadcasts back to `MyAccessibilityService` to perform actions like dumping the UI tree, writing to an editable field, or pasting text from the clipboard menu.
6.  **Hiding the Overlay:** When the user navigates away from TikTok, `MyAccessibilityService` sends another broadcast to hide the overlay.
7.  **Widget:** The user can add a widget to the home screen. The widget displays a website from the app's local assets.

## Components

*   **`MainActivity.kt`:** The main entry point of the application. Its primary responsibility is to request the necessary permissions from the user.
*   **`MyAccessibilityService.kt`:** The core of the application. It monitors the foreground app and controls the visibility of the floating overlay. It also performs actions based on commands received from the overlay.
*   **`FloatingWindowService.kt`:** This service is responsible for creating, displaying, and managing the floating overlay. The overlay's UI is built with Jetpack Compose. It also hosts the `WebAppInterface` for WebView communication.
*   **`data_layout.html`:** A local HTML file loaded into a WebView within the overlay. It provides a dynamic, Material You-inspired user interface for managing structured data (Type, Link URL, Status). This HTML uses JavaScript to interact with the Android app via a `WebAppInterface`.
    *   **JSON Data Handling:** `DataRow` objects (representing each row of data) are serialized to and from JSON. The `WebAppInterface` facilitates this by allowing JavaScript to call Android methods (`submitData` to save JSON data, `loadData` to retrieve JSON data). The Android app, in turn, uses `DataStore.kt` to persist this JSON data to internal storage, ensuring data consistency across sessions.
*   **`AndroidManifest.xml`:** Declares the necessary services, activities, and permissions for the application.
*   **`WebsiteWidgetProvider.kt`:** The widget provider for the home screen widget.
*   **`WidgetUpdateService.kt`:** A service that updates the widget with the website content.

## Widget

The home screen widget displays a website from the app's local assets. The website is stored in the `app/src/main/assets` directory. The widget is updated by the `WidgetUpdateService` which loads the website into a `WebView` and then renders it to the widget's `RemoteViews`.

## Recent Changes

*   **Dump UI Functionality:** Added a "Dump UI" button to the overlay that, when clicked, logs the entire UI hierarchy of the current screen to Logcat for debugging and analysis.
*   **Keyboard-Aware Overlay Positioning Fixes:** Resolved issues with the overlay's keyboard detection logic, ensuring correct vertical positioning and removing unnecessary debug log statements for a cleaner Logcat.
*   **WebView Enhancements & Custom Website Integration:**
    *   The WebView layout has been refined to be more compact and to better fit website content, including an adjustment to the initial scale for better viewing.
    *   Integrated a custom local HTML page (`data_layout.html`) that mimics the app's `DataLayout` composable, allowing for external development and interaction with the app's data storage. This HTML now features a Material You-inspired chocolate tonal palette for a modern aesthetic and starts with a single empty row for data entry, dynamically adding new rows as needed.
    *   Implemented a JavaScript interface (`WebAppInterface`) to enable seamless communication between the WebView and the Android application, allowing data to be sent from and loaded into the HTML page.
*   **Broadcast Mechanism Update:**
    *   Replaced deprecated `LocalBroadcastManager` with standard broadcast mechanisms in `MyAccessibilityService` to ensure reliable communication and proper functioning of features like text pasting.
*   **Enhanced Paste Functionality:**
*   **Enhanced Paste Functionality:**
    *   Added multiple dedicated buttons (⭐, 💵, 1️⃣, ⚪, 🛑) within a new "Paste Layout" for quick pasting of pre-defined messages (promotional text, payment info, instructions, assurance, delivery time).
    *   Implemented automatic "Send" or "Enter" key press after any text is pasted or written by the application.
*   **Improved Overlay UI/UX:**
    *   Updated button icons for clarity (e.g., "Show Paste Layout" icon changed to clipboard, "Close Paste Layout" icon changed to close).
    *   Integrated Material You dynamic theming for a modern, adaptive look.
    *   Added smooth expansion and collapse animations for the button list.
*   **Keyboard-Aware Overlay Positioning:** The overlay now intelligently adjusts its vertical position to avoid being covered by the soft keyboard and allows free vertical dragging when the keyboard is not active.
*   **Added Home Screen Widget:**
    *   Added a home screen widget that displays a website from the app's local assets.