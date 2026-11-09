# ARMS — Modern Citizen Dashboard UI/UX Design Specification
## Accident Reports and Monitoring System
> **Tagline:** Report. Monitor. Respond.
> **Platform:** Android Mobile Application (UI/UX Design Mockup Specification)
> **Location:** Valencia City, Bukidnon, Philippines

---

## 1. Design System & Color Palette

| Token Name | Hex Code | Usage |
| :--- | :--- | :--- |
| **Deep Navy Blue** | `#14243A` | Primary brand color, headers, text primary, navigation bar normal state |
| **Emergency Red** | `#E53945` | Primary action button, emergency alerts, active indicators, camera nav accent |
| **White** | `#FFFFFF` | Card backgrounds, top navigation, bottom bar background |
| **Light Background** | `#F5F7FA` | Screen root background |
| **Secondary Text** | `#778397` | Subtitles, labels, timestamps, metadata |
| **Success Green** | `#2E9D68` | Resolved status badges, positive indicators |

---

## 2. Screen Layout & Component Breakdown

### A. Compact Header
- **Layout:** Horizontal row with left branding & greeting, right notification bell and circular profile avatar.
- **Left Content:**
  - App Name: **ARMS** (Bold, Navy Blue, 20sp)
  - Greeting: *“Good morning, Alex!”* (Navy Blue, 16sp, Semi-bold)
  - Subtitle: *“Your safety matters.”* (Secondary Text, 12sp)
- **Right Content:**
  - Notification Bell (Outline icon with small Red notification dot badge).
  - Circular Profile Avatar (Polished photo/initials placeholder with subtle border).

### B. Emergency Reporting Card
- **Container:** Material card with rounded corners (`16dp`), white background, and subtle shadow (`elevation 4dp`).
- **Content:**
  - Emergency Alert Icon (Emergency Red accent).
  - Heading: **“Need Emergency Assistance?”** (Navy Blue, 18sp, Bold)
  - Subtitle: *“Report an incident and help responders locate it.”* (Secondary Text, 13sp)
  - Primary Action: Large Emergency Red button labeled **“Report an Emergency”** (Rounded `12dp`, white bold text).
  - Secondary Action: Text link labeled **“Track My Reports”** (Navy Blue / Emergency Red accent).

### C. My Reports Summary (2x2 Grid)
- **Grid Layout:** 2 columns × 2 rows of compact metric cards.
  1. **Total Reports — 08** (Neutral gray icon & card)
  2. **Under Review — 02** (Orange accent icon & card)
  3. **Responding — 01** (Emergency Red accent icon & card)
  4. **Resolved — 05** (Success Green accent icon & card)
- **Card Styling:** Rounded corners (`12dp`), clean white background, minimalist stat numbering (20sp, Bold).

### D. Recent Reports Section
- **Header:** Row with “Recent Reports” title (Left) and “View All” text link (Right, Emergency Red).
- **Card 1:**
  - **ID:** #ARMS-2026-081
  - **Type:** Road Accident — Valencia City
  - **Date/Time:** Oct 24, 2026 • 02:30 PM
  - **Status Badge:** **En Route** (Emergency Red badge)
  - **Action:** Small chevron/arrow icon.
- **Card 2:**
  - **ID:** #ARMS-2026-079
  - **Type:** Medical Emergency — Valencia City
  - **Date/Time:** Oct 23, 2026 • 09:15 AM
  - **Status Badge:** **Under Review** (Orange badge)
  - **Action:** Small chevron/arrow icon.

### E. Fixed Bottom Navigation Bar (5 Items)
- **Container:** White background, top shadow/border, fixed anchor at bottom of portrait screen.
- **Items (Evenly Spaced):**
  1. **Home** (House Icon — Navy Blue, Active / Selected State with bold indicator)
  2. **My Reports** (Clipboard List Icon — Secondary Text)
  3. **Camera** (Camera Icon — Emergency Red accent for quick emergency evidence capture)
  4. **Notifications** (Bell Icon — Secondary Text)
  5. **Profile** (User-Round Icon — Secondary Text)

---

## 3. Visual Mockup Wireframe

```text
┌─────────────────────────────────────────┐
│ ARMS          Good morning, Alex!   [🔔]⚪│
│ Report.Monitor. Your safety matters.    │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ 🚨 Need Emergency Assistance?       │ │
│ │ Report an incident & help responders│ │
│ │                                     │ │
│ │ [   REPORT AN EMERGENCY   ]         │ │
│ │               Track My Reports      │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ My Reports Summary                      │
│ ┌──────────────────┐ ┌────────────────┐ │
│ │ 📋 Total: 08     │ │ ⏳ Review: 02  │ │
│ └──────────────────┘ └────────────────┘ │
│ ┌──────────────────┐ ┌────────────────┐ │
│ │ 🚑 Responding: 01│ │ ✅ Resolved: 05│ │
│ └──────────────────┘ └────────────────┘ │
│                                         │
│ Recent Reports              [ View All ]│
│ ┌─────────────────────────────────────┐ │
│ │ #ARMS-2026-081    [ En Route ]      │ │
│ │ Road Accident - Valencia City       │ │
│ │ Oct 24, 2026 • 02:30 PM         >   │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ #ARMS-2026-079    [ Under Review ]  │ │
│ │ Medical Emergency - Valencia City   │ │
│ │ Oct 23, 2026 • 09:15 AM         >   │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│   🏠      📋      📷(🔴)    🔔      👤    │
│  Home  Reports  Camera Notif Profile    │
└─────────────────────────────────────────┘
```
