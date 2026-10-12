# CashCount Pro - Cash Denomination Management Application

A complete, production-grade Android application built with Kotlin and Jetpack Compose for fast cash counting, denomination tallying, register reconciliation, reporting, and printable statement generation.

---

## 1. Product Overview

**CashCount Pro** is engineered for retail stores, supermarkets, restaurants, hotels, petrol pumps, cashiers, accounts departments, and daily cash collection teams. It streamlines register tallying from minutes to seconds, providing real-time calculations, differences tracking (Excess/Shortage/Balanced), historical audit records, and exportable statements.

---

## 2. Key Features

- **Denomination Calculator**:
  - Preloaded with Indian Rupee (INR ₹) standard notes (₹500, ₹200, ₹100, ₹50, ₹20, ₹10) and coins (₹20, ₹10, ₹5, ₹2, ₹1).
  - Multi-currency architecture supporting USD ($), EUR (€), GBP (£), and AED (د.إ).
  - Real-time calculation engine: `Amount = denomination × quantity`.
  - Automatic tallying of Total Notes, Total Coins, Total Quantity, and Total Actual Cash.
  - Reconciliation against Expected Cash: calculates exact Excess / Shortage / Balanced status.

- **Main Dashboard**:
  - Metric cards for Today's Total Cash, Total Cash Counts, Last Cash Count time & amount, and Net Difference.
  - Active Cashier / User pill card with quick user switcher.
  - Quick launch buttons: "New Count", "Quick Count", "View History", "Reports".
  - Recent transactions list with one-tap receipt view, editing, duplication, and deletion.

- **Quick Count Mode (Mobile Optimized)**:
  - Designed for high-speed, one-handed mobile phone tallying.
  - Prominent running total banner at the top.
  - Large denomination cards with clear Note/Coin badges.
  - Large decrement (`-`) and increment (`+`) buttons.
  - Rapid bundle step multipliers (`+5`, `+10`, `+50`, `+100`) and quick reset (`C`).
  - Undo support for mistake-free counting.

- **Cash History & Management**:
  - Full transaction ledger showing ID, date/time, cashier, register till, actual cash, expected cash, difference, and status.
  - Instant live search across transaction number, cashier name, till location, and notes.
  - Filter by date (Today, Yesterday, Last 7 Days, This Month, All Time).
  - Filter by status (All, Balanced, Shortage, Excess).
  - Sorting options (Newest, Oldest, Highest Cash, Lowest Cash, Largest Difference).
  - Full transaction actions: View Statement, Edit, Duplicate into new count, Delete with confirmation.
  - Real CSV file export.

- **Receipt / Statement Generation & Printing**:
  - Printable statement with customizable business branding (Store Name, Address, Tel, GSTIN).
  - Complete denomination breakdown table (`Denomination × Qty = Amount`).
  - Totals block with Opening Cash, Expected Cash, Actual Cash, Difference, and status.
  - Cashier signature & manager verification lines.
  - Direct Print / PDF generation using Android `PrintManager`.
  - Native CSV export sharing via `FileProvider`.
  - Formatted text receipt sharing (for WhatsApp, SMS, Email) and clipboard copy.

- **Reports & Analytics**:
  - Daily, Weekly (7 Days), Monthly, and All-Time financial reports.
  - Summary metrics: Total Cash, Transaction Count, Average Cash per Count, Total Surplus (Excess), Total Deficit (Shortage).
  - Cashier performance breakdown with visual distribution bars.
  - Register / Till performance breakdown.
  - Direct CSV report export.

- **Settings & Administration**:
  - Business profile configuration (Name, Address, Phone, Email, GSTIN, Receipt Footer).
  - Multi-currency switcher with currency symbol synchronization.
  - Denomination customizer: enable/disable any note or coin, add custom denominations.
  - Staff & User management: Add users with roles (`ADMIN`, `MANAGER`, `CASHIER`) and quick PINs.
  - Register & Till management: Add new tills/locations.
  - Audit logging: Tracks user sessions, creations, edits, deletions, and configuration changes.
  - Full JSON backup export & restore with data validation.
  - Sample demo data loader and one-tap reset.

---

## 3. Architecture & Tech Stack

- **Platform**: Android (minSdk 24, targetSdk 36)
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Persistence**: Room Database with SQLite, reactive Kotlin Coroutines `Flow`
- **Dependency Injection**: Constructor Injection
- **Printing & Export**: Android `PrintManager` (HTML print adapter) & `FileProvider` (CSV generation)

---

## 4. Room Database Schema

- `users`: Staff accounts, roles (`ADMIN`, `MANAGER`, `CASHIER`), quick PINs, created timestamps.
- `locations`: Physical till registers and checkout counters.
- `denominations`: Active and custom currency notes & coins with sorting orders.
- `transactions`: Full cash count master records with timestamps, opening cash, expected cash, actual cash, difference, and status.
- `transaction_items`: Itemized denomination breakdown (`denominationValue`, `denominationType`, `quantity`, `amount`).
- `settings`: Business profile, currency code, receipt footer, and theme preferences.
- `audit_logs`: Timestamped system audit trail.

---

## 5. Verification & Testing

Automated JVM tests cover:
- Core calculation engine (`DenominationEngineTest.kt`):
  - Note & Coin formula verification (`₹500 × 10 = ₹5,000`, `₹200 × 5 = ₹1,000`, `₹100 × 10 = ₹1,000`).
  - Total aggregation, difference calculation, and status determination (Balanced, Shortage, Excess).
  - Zero, negative, and decimal coin inputs.
  - Exclusion of disabled denominations.
  - Currency symbol formatting.
- Robolectric context test (`ExampleRobolectricTest.kt`).
