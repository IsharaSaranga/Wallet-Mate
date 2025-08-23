# 💰 Wallet Mate

Wallet Mate is a mobile application for personal finance management, developed using Kotlin, Android Studio, and Room (SQLite).  

It helps users track income, expenses, and budgets while providing clear visual insights into spending patterns.



📱 Features

- Transaction Management  
  Add, edit, and delete income and expense records with title, amount, category, and date.
  
- Category-Wise Analysis  
  Visualize spending by category using charts and graphs.

- Monthly Budget Setup  
  Set monthly expenditure limits and get notifications when nearing or exceeding your budget.

- Persistent Data Storage  
  All transactions are stored using **Room Database (SQLite)** for reliable data persistence.

- Backup & Restore  
  Export and import transaction data using internal storage (JSON format).

- Simple & Clean UI  
  Bottom navigation for quick access to **Transactions**, **Analysis**, and **Settings** pages.

🛠️ Tech Stack

- **Language:** Kotlin  
- **IDE:** Android Studio  
- **Database:** Room (SQLite)  
- **Architecture:** MVVM (Model–View–ViewModel)  
- **UI:** XML Layouts + Material Design
- **Storage:** Internal Storage (Backup/Restore), SharedPreferences (for settings)
