# Design System Strategy: Industrial Precision & The Command-Line Aesthetic

## 1. Overview & Creative North Star
The Creative North Star for this system is **"The Kinetic Console."** 

This is not a decorative dashboard; it is a high-performance instrument. We are moving away from the "consumer-web" aesthetic—characterized by soft pill-shaped buttons and airy whitespace—and moving toward a high-density, **Utilitarian Brutalism**. 

The design breaks the "template" look through a rigorous commitment to monospace typography and a "Data-First" hierarchy. By using a strict 0-6px radius and high-contrast zebra patterning, we evoke the feeling of a mission-critical terminal. The layout should feel intentional and "locked-in," utilizing a modular grid that mimics physical rack-mounted server hardware.

## 2. Colors & Surface Architecture
Our palette is rooted in the deep cold-blacks of a data center, utilizing the provided hex codes to create a layered, "recessive" depth.

### Surface Hierarchy & Nesting
To achieve an industrial feel, we utilize **Tonal Layering** instead of decorative shadows.
*   **Base Layer:** `background` (#0d1117) — The "floor" of the application.
*   **The Component Bed:** `surface` (#161b22) — Used for primary content areas and sidebar containers.
*   **The Elevated Tile:** `surface_container` (#1c2026) — Used for cards or active terminal blocks to provide a "lift" without shadows.
*   **The Interactive Layer:** `surface_container_highest` (#31353c) — Reserved for hover states or active selection indicators.

### The "No-Line" Rule & The "Ghost Border"
*   **Internal Separation:** Prohibit 1px solid borders for internal sectioning (e.g., separating a header from a body). Use a background shift from `surface` to `surface_container_low` instead.
*   **The Ghost Border:** For the outer perimeter of modules, use a "Ghost Border" using `outline_variant` at 40% opacity. This defines the edge without creating a heavy visual "box" that interrupts the eye's flow across data rows.
*   **The Zebra Principle:** For high-density data (Tables/Lists), use alternating rows of `surface` and `surface_container_low`. This provides horizontal guidance more effectively than vertical lines.

## 3. Typography: The Monospaced Authority
We are using **JetBrains Mono** or **IBM Plex Mono** exclusively. This choice is functional: monospaced fonts ensure that columns of numbers and code align perfectly, which is critical for DevOps monitoring.

*   **Display-LG (3.5rem):** Reserved for critical status counts (e.g., "99.9% UPTIME").
*   **Headline-SM (1.5rem):** Used for module titles (e.g., "ACTIVE INSTANCES").
*   **Body-MD (0.875rem):** The workhorse for logs, JSON blocks, and terminal output. Use `Text Muted` (#8b949e) for timestamps.
*   **Label-SM (0.6875rem):** Used for table headers and micro-metadata. Always uppercase with +0.05em letter spacing to ensure legibility at small scales.

## 4. Elevation & Depth
In an industrial UI, depth is about **Focus**, not "floating." 

*   **The Layering Principle:** Stack `surface_container_lowest` (#0a0e14) for "wells" (input fields or terminal windows) to make them feel carved into the UI. Use `surface_container_high` (#262a31) for "active" modules.
*   **Ambient Shadows:** We generally avoid shadows. However, if a modal must appear, use a sharp, 0-blur shadow: `4px 4px 0px rgba(0,0,0,0.5)`. This mimics the "hard shadow" of industrial lighting.
*   **Glassmorphism:** To maintain the "high-end" feel, use a subtle `backdrop-blur: 8px` on top-level navigation bars with a semi-transparent `surface` color (#161b22cc). This keeps the data visible beneath the chrome.

## 5. Components

### Buttons & Controls
*   **Primary Action:** Outlined. Border: 1px solid `primary` (#58a6ff). Text: `primary`. Hover state: Background `primary` at 10% opacity. No fill.
*   **Critical Action:** Outlined. Border: 1px solid `error` (#f85149). Text: `error`. 
*   **Radii:** Strictly 0px for a "sharp" look, or 4px for a "precision-milled" feel. Never exceed 6px.

### Status Badges & Dots
*   **The "Pulse" Dot:** Statuses are represented by a 6px circle. 
    *   `Success`: Solid #3fb950.
    *   `Warning`: Solid #d29922.
    *   `Error`: Solid #f85149.
*   **Labels:** Next to the dot, use `label-sm` in `Text Muted`.

### Terminal & Code Blocks
*   **Container:** `surface_container_lowest` (#0a0e14).
*   **Border:** `outline_variant` (#414752) top-border only to simulate a "tab" or window header.
*   **Syntax:** Use `primary` for keys and `text-primary` for values in JSON blocks.

### Tables (Industrial Grid)
*   **Header:** No background. 1px `border-bottom` using `outline_variant`.
*   **Rows:** Zebra striping using `surface` and `surface_container_low`. 
*   **Hover:** `surface_container_highest` to highlight the entire data row.

## 6. Do's and Don'ts

### Do:
*   **Do** align all text to the baseline of the monospace grid.
*   **Do** use `Text Muted` for secondary information to create a clear visual hierarchy against `Text Primary`.
*   **Do** use "Breadcrumbs" in the header to show system paths (e.g., `Cluster / Node-01 / Logs`).

### Don't:
*   **Don't** use gradients. All colors must be flat to maintain the "digital readout" aesthetic.
*   **Don't** use standard "Material" icons. Use thin-stroke, geometric icons or simple ASCII-style symbols (e.g., `[+]`, `[-]`, `>`).
*   **Don't** add padding to the edges of the screen; use the full real estate. DevOps users value information density over "breathing room."