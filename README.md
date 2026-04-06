# 🚀 PocketPilot – Smart Personal Finance Tracker

> 📌 This project was developed as part of the screening assessment for the **Mobile App Developer Intern** position at **Zorvyn**.

> Track. Analyze. Optimize. 💰  
PocketPilot is a modern Android application designed to help users manage their finances efficiently with real-time insights, budget tracking, and beautiful analytics.

---

## 📱 App Preview

### 🏠 Home Screen
- Overview of balance, income, expenses
- Weekly trend visualization
- Smart insights & savings goal tracking

### 📊 Analytics Screen
- Expense breakdown (Donut Chart)
- Income vs Expense comparison
- Weekly comparison insights

### 💳 Transactions Screen
- Add / Edit / Delete transactions
- Category-based tracking
- Budget monitoring

---

## 🖼️ Screenshots

| Home | Analytics | Transactions |
|------|----------|-------------|
| ![Home](assets/Home.jpeg) | ![Analytics](assets/Analytics.jpeg) | ![Transactions](assets/Transaction.jpeg) |

| Edit Transaction | Budget Dialog |
|-----------------|--------------|
| ![Edit](assets/Edit.jpeg) | ![Budget](assets/Budget.jpeg) |

---

## ✨ Features

### 💰 Financial Tracking
- Add, update, delete transactions
- Categorize expenses (Food, Travel, Health, etc.)
- Income & Expense tracking

### 📊 Smart Analytics
- Income vs Expense bar graph
- Expense breakdown using donut chart
- Weekly comparison (This Week vs Last Week)

### 🎯 Budget Management
- Set category-wise monthly budgets
- Real-time budget alerts
- Shows exceeded or remaining budget

### 🧠 Smart Insights
- Highlights highest spending category
- Suggests improvements for better savings

### 📈 Goal Tracking
- Set savings goals
- Progress tracking with percentage completion

---

## 🏗️ Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture for clean separation of concerns.

###  Project Structure

```text
com.example.pocketpilot
│
├── data
│   ├── local (Room Database)
│   │   ├── AppDatabase
│   │   ├── TransactionDao
│   │   └── TransactionEntity
│   ├── repository
│   │   └── FinanceRepository
│
├── ui
│   ├── home
│   ├── analytics
│   ├── transactions
│   └── viewmodel
│
└── MainActivity
```

---

## 🧱 Tech Stack

- **Language:** Kotlin  
- **Architecture:** MVVM  
- **Database:** Room Database (SQLite)  
- **UI:** XML (Material Design)  
- **State Management:** ViewModel + LiveData  
- **Navigation:** Navigation Component  
- **Storage:** SharedPreferences (for goals & budgets)  

---

## ⚙️ How It Works

### 📌 Data Flow

UI → ViewModel → Repository → Room Database

### 📌 Example

- User adds transaction  
- Stored in Room DB  
- ViewModel updates LiveData  
- UI auto-refreshes  

---

## 📦 Download APK

You can directly download and install the app on your Android device:

👉 Navigate to the **APK folder** in this repository and download the `.apk` file.

### 📲 How to Install APK

1. Download the APK file from the repository  
2. Transfer it to your Android device (if downloaded on PC)  
3. Open the APK file on your device  
4. Allow **“Install from unknown sources”** if prompted  
5. Tap **Install**  
6. Open the app and start using 🎉  

---

## 🚀 Installation (For Developers)

Follow these steps to run **PocketPilot** locally:

1. Clone the repository:

```bash
git clone https://github.com/RatnaSrivastava16/PocketPilot.git
```
2. Move into the project folder

```bash
cd PocketPilot
```

3. Open in Android Studio
   - Launch Android Studio
   - Click Open
   - Select the project folder
   - Wait for Gradle sync   

4. Run the app
   - Connect device / start emulator
   - Build & Run

---

## 📌 Key Highlights
- 🔥 Clean and scalable architecture
- 🎨 Modern UI with dark theme
- ⚡ Real-time updates using LiveData
- 📊 Data-driven insights
- 📱 Fully responsive layouts

---

## 🧪 Future Improvements
- 🔐 User Authentication (Firebase)
- ☁️ Cloud Sync
- 📉 Advanced charts (MPAndroidChart)
- 🤖 AI-based spending predictions
- 📤 Export reports (PDF/CSV)
