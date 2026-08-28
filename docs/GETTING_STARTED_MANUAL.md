# 🚀 MedVoice — Complete Setup & Manual Launch Guide (Zero External Downloads)

**Good News:** **You do NOT need to download or install anything from a web browser!** Everything required (Android Studio, Android SDK 34, Java 21 JDK, Android Emulator, and the compiled MedVoice APK) is already installed, configured, and tested on your laptop.

---

## 📋 1. Your Laptop's Current Environment Status

Here is the exact status of your development setup:

| Component | Status on Your Machine | Location |
| :--- | :--- | :--- |
| **Android Studio** | ✅ **Installed** | `C:\Program Files\Android\Android Studio` |
| **Java 21 OpenJDK** | ✅ **Installed** | `C:\Program Files\Android\Android Studio\jbr` |
| **Android SDK 34 & Platform Tools** | ✅ **Installed** | `C:\Users\Ashraf\AppData\Local\Android\Sdk` |
| **Virtual Device Emulator** | ✅ **Created & Ready** | `Medium_Phone_API_36.1` |
| **Compiled Debug APK** | ✅ **Built & Ready** | `app/build/outputs/apk/debug/app-debug.apk` |
| **Google Android CLI** | ✅ **Installed & Ready** | `C:\Users\Ashraf\AppData\AndroidCLI\android.exe` |

---

## 🎯 2. How to Launch & Test MedVoice on Your Laptop (2 Methods)

### 🟢 Method 1: Using Android Studio (Visual & Easiest)

This is the standard, visual way used by Android developers:

1. **Open Android Studio:**
   - Press the **Windows Key** on your keyboard, type **"Android Studio"**, and press **Enter**.
2. **Open the MedVoice Project:**
   - Click **Open** (or go to **File > Open**).
   - Select the folder: `C:\Users\Ashraf\Desktop\Hackathons\iQOO` and click **OK**.
3. **Wait 10 seconds for Gradle Sync:**
   - Android Studio will display a quick status bar at the bottom saying *"Gradle Sync Successful"*.
4. **Launch the App:**
   - In the top-center toolbar, look at the device dropdown — you will see **`Medium_Phone_API_36.1`**.
   - Click the green **Run (▶)** button (or press `Shift + F10`).
5. **Result:**
   - A virtual Android smartphone will pop up on your screen.
   - MedVoice will open automatically, ready for you to interact with the buttons, test pill chips, and hear the Marathi/Hindi voice!

---

### ⚡ Method 2: Using PowerShell (Fast Command Line)

If you prefer doing everything directly from your terminal with no mouse clicks:

1. **Open PowerShell:**
   - Press `Win + X` and select **Terminal** (or PowerShell).
2. **Start the Virtual Phone (Emulator):**
   ```powershell
   & "C:\Users\Ashraf\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd "Medium_Phone_API_36.1"
   ```
   *(The virtual phone screen will appear on your desktop)*
3. **In a second PowerShell tab, install and launch MedVoice:**
   ```powershell
   cd C:\Users\Ashraf\Desktop\Hackathons\iQOO
   .\gradlew.bat installDebug
   & "C:\Users\Ashraf\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.medvoice/.MainActivity
   ```
4. **Result:**
   - MedVoice immediately opens on the virtual phone.

---

## 📱 3. How to Test on a Real Physical Android Phone (Optional)

If you want to hold a real phone in your hand to test live camera scanning with real blister packs:

1. **Enable USB Debugging on your phone:**
   - Open your Android phone **Settings > About Phone**.
   - Tap **"Build Number"** 7 times until it says *"You are now a developer!"*.
   - Go to **Settings > System > Developer Options** and turn ON **"USB Debugging"**.
2. **Plug your phone into your laptop via USB cable:**
   - On your phone screen, tap **"Allow USB Debugging"**.
3. **Install the App with one command:**
   ```powershell
   cd C:\Users\Ashraf\Desktop\Hackathons\iQOO
   .\gradlew.bat installDebug
   ```
4. MedVoice is now installed on your physical smartphone as a standalone app!

---

## 🧪 4. What to Do Inside the App (Testing Features)

Once MedVoice opens on your virtual phone or real phone:

1. **Toggle Language:**
   - Tap the top **"मराठी"** or **"हिंदी"** buttons.
2. **Test Safe Medicine (Glycomet-SR 500):**
   - Tap the **`Glycomet-SR 500`** pill chip at the top.
   - The screen turns **Green (`#00875A`)** and speaks: *"हे तुमचे साखरेचे औषध आहे. जेवणानंतर एक गोळी पाण्यासोबत घ्या."*
   - Tap the white **"घेतली (Confirm Taken)"** button.
3. **Test The Duplicate Overdose Trap (Gluconorm-SR 500):**
   - Now tap the **`Gluconorm-SR 500`** chip (the equivalent brand with Metformin).
   - The screen immediately turns **Flashing Red (`#DE350B`)** and warns: *"सावधान! थांबा! तुम्ही आधीच Glycomet-SR 500 (Metformin) घेतले आहे. हे औषध पुन्हा घेऊ नका."*
   - The confirm button is blocked to prevent accidental intake.
4. **Test Caregiver Logbook:**
   - Tap the **List icon (📋)** in the top right to view the audit record with timestamps.

---

## 🛠️ 5. Cheat Sheet of Daily Developer Commands

All commands are run from `C:\Users\Ashraf\Desktop\Hackathons\iQOO`:

| Action | PowerShell Command |
| :--- | :--- |
| **Run all Automated Unit Tests** | `.\gradlew.bat test` |
| **Build the APK binary** | `.\gradlew.bat assembleDebug` |
| **Install onto connected phone/emulator** | `.\gradlew.bat installDebug` |
| **View Live Real-Time Logs (Logcat)** | `& "C:\Users\Ashraf\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -s "MedVoice_*"` |
| **Recompile Master Database** | `python scripts/compile_master_db.py` |
