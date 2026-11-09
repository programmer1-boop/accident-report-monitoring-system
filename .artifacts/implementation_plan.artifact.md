# Implementation Plan — Complete UI Design System Alignment with ARMS Concept Board

Comprehensive implementation plan for aligning all native Android XML layouts and styling with the supplied ARMS UI design concept board across all roles (Welcome, Citizen, Guest Reporter, Responders/Enforcers, Dispatcher, Super Admin, Shared States).

---

## 1. Design Concept Board Specifications

### A. Color System
- **Primary Navy:** `#142B4A` (`@color/arms_navy`)
- **Secondary Navy:** `#203D63`
- **Emergency Red:** `#D92D35` / `#E53945` (`@color/arms_red`)
- **Background:** `#F4F6F9` / `#F8FAFC` (`@color/arms_background`)
- **Surface Card:** `#FFFFFF` (`@color/arms_card_bg`)
- **Secondary Text:** `#667085` (`@color/arms_secondary_text`)
- **Success Green:** `#16865c` / `#2E9D68` (`@color/arms_green`)
- **Warning Amber:** `#D97706` / `#F2A900` (`@color/arms_amber`)

### B. Screen Composition & Components
- **Cards & Containers:** `16dp` corner radius cards (`MaterialCardView`), soft elevation shadows (`4dp`), and subtle 1px dividers.
- **Buttons:** Primary buttons (`54dp` height, rounded corners, Navy or Emergency Red), secondary outlined buttons, and bottom navigation bars with prominent center red floating action button.
- **Status Badges:** Pill-shaped status badges with leading status indicator dots (`SUBMITTED`, `UNDER_REVIEW`, `VERIFIED`, `ASSIGNED`, `EN_ROUTE`, `ON_SCENE`, `RESOLVED`, `REJECTED`).

---

## 2. Proposed Layout Refinements

1. **Colors & Styles (`res/values/colors.xml`, `res/values/styles.xml`):** Ensure exact hex values match the concept board palette.
2. **Citizen & Guest Layouts:** Refine dashboard cards, recent reports list items, and bottom navigation.
3. **Staff Portals Layouts:** Refine Responder, Dispatcher, and Super Admin dashboard cards and metric grids to match the concept board's operational panels.

---

## 3. Verification Plan

### Automated Verification
- Execute Android build check: `gradle_build("app:assembleDebug")`.
- Execute Node.js server syntax check: `node -c backend/src/server.js`.
