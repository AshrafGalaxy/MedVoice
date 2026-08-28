# 📱 MedVoice — Beginner's Manual & Step-by-Step Testing Guide

Welcome to the MedVoice project! This guide is written specifically for you as a beginner to Android development. It explains how everything works under the hood, how to open and test the app on your laptop or physical Android phone, and how to verify each feature and find bugs.

---

## 📚 1. Understanding the Tech Stack (In Plain English)

| Technology | What It Is | What It Does in MedVoice |
| :--- | :--- | :--- |
| **Kotlin** | Modern Android programming language | The language used to write all the application logic, safety checks, and UI. |
| **Jetpack Compose** | Modern Android UI toolkit | Replaces old XML layouts with declarative Kotlin code to draw high-contrast screens and buttons. |
| **Google ML Kit** | On-device machine learning vision engine | Reads text from medicine strips and blister packs in real-time (100% offline). |
| **CameraX** | Android Camera framework | Connects to the smartphone camera and feeds live video frames to ML Kit. |
| **Room / SQLite (FTS5)** | On-device embedded database | Stores 30,000+ Indian drugs, salts, food rules, and contraindications right on the phone. |
| **TextToSpeech (TTS)** | Android native voice synthesizer | Speaks instructions aloud in natural Marathi (`mr-IN`) and Hindi (`hi-IN`). |
| **Gradle** | Build automation tool | Compiles the Kotlin code, resolves dependencies, and bundles everything into an `.apk` file. |

---

## 🛠️ 2. How to Open the Project in Android Studio

You already have **Android Studio** installed at `C:\Program Files\Android\Android Studio`.

1. **Launch Android Studio** from your Windows Start Menu.
2. Click on **"Open"** (or **File > Open**).
3. Navigate to: `C:\Users\Ashraf\Desktop\Hackathons\iQOO` and click **OK**.
4. Android Studio will automatically recognize the Gradle project and sync all dependencies.
5. In the top toolbar, you will see a green **Run (▶)** button and a device dropdown.

---

## 💻 3. How to Run the App on Your Laptop (Using the Emulator)

You have an Android Virtual Device (AVD) named **`Medium_Phone_API_36.1`** already configured on your machine.

### Method A: Running via Android Studio
1. In the top toolbar device selector, choose **`Medium_Phone_API_36.1`**.
2. Click the green **Run (▶)** button (or press `Shift + F10`).
3. The virtual Android phone will boot up on your screen, and MedVoice will open automatically.

### Method B: Running via PowerShell / Terminal
You can also launch the emulator and install the app entirely using commands:

```powershell
# 1. Start the Android Emulator
& "C:\Users\Ashraf\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd "Medium_Phone_API_36.1"

# 2. In a new PowerShell window, build and install the Debug APK
.\gradlew.bat installDebug

# 3. Launch MedVoice on the emulator
& "C:\Users\Ashraf\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.medvoice/.MainActivity
```

---

## 📱 4. How to Run the App on a Real Android Phone

Testing on a physical phone gives you the full CameraX live blister pack scanning experience:

1. **Enable Developer Options on your phone:**
   - Open Phone **Settings > About Phone**.
   - Tap **"Build Number"** 7 times until you see *"You are now a developer!"*.
2. **Enable USB Debugging:**
   - Go to **Settings > System > Developer Options**.
   - Turn ON **"USB Debugging"**.
3. **Connect Phone to Laptop via USB Cable:**
   - On your phone screen, a prompt will appear: *"Allow USB debugging?"* -> Tap **Allow**.
4. **Install the App:**
   - In Android Studio, select your physical phone from the device dropdown and click **Run (▶)**.
   - Or in PowerShell:
     ```powershell
     .\gradlew.bat installDebug
     ```

---

## 🧪 5. Step-by-Step Feature Testing Guide (How to Test Everything)

MedVoice is built with an **Interactive Quick-Test Bar** so you can test all clinical safety flows with a single tap, even if you are on an emulator without physical blister packs!

### 🟢 Test Scenario 1: Safe Medicine Scan & Marathi/Hindi Audio
* **Goal:** Verify that scanning a valid medicine speaks the correct dosage instructions.
* **How to Test:**
  1. On the scanner screen, tap the language button: **"मराठी"** or **"हिंदी"**.
  2. Tap the **`Glycomet-SR 500`** chip in the Quick Test bar (or point the camera at a Glycomet strip).
  3. **Expected Result:**
     - The bottom card turns **Safety Green (`#00875A`)**.
     - Display shows: *"Glycomet-SR 500"* & *"घटक: Metformin Hydrochloride"*.
     - Audio speaks in Marathi: *"हे तुमचे साखरेचे औषध आहे. जेवणानंतर एक गोळी पाण्यासोबत घ्या."*
  4. Tap the white button: **"घेतली (Confirm Taken)"**.
  5. Audio confirms: *"नोंद झाली आहे."* (Intake is logged into the local database).

---

### 🔴 Test Scenario 2: The Duplicate Overdose Trap (The "Wow" Feature)
* **Goal:** Verify that taking a substitute brand with the same active molecule is immediately blocked.
* **How to Test:**
  1. Complete Test Scenario 1 (Glycomet is now logged as taken in the active window).
  2. Now tap the **`Gluconorm-SR 500`** chip (the substitute brand containing Metformin).
  3. **Expected Result:**
     - The screen immediately flashes **Urgent Red (`#DE350B`)**.
     - Warning header: *"सावधान! पुन्हा घेऊ नका"* (Warning! Do not take again).
     - Spoken Alert: *"सावधान! थांबा! तुम्ही आधीच Glycomet-SR 500 (Metformin Hydrochloride) घेतले आहे. हे औषध पुन्हा घेऊ नका."*
     - The "Confirm" button is hidden to prevent accidental intake.
     - An emergency SMS dispatch is triggered in the background.

---

### ⚠️ Test Scenario 3: Drug-to-Drug Contraindication Alert
* **Goal:** Verify that incompatible drug combinations (e.g. Aspirin + Combiflam) trigger a critical interaction warning.
* **How to Test:**
  1. Tap **"नोंदवही (Logs Icon)"** in the top right -> tap the **Trash icon** to clear previous logs.
  2. Return to the scanner.
  3. Tap **`Ecosprin 75`** (Aspirin) -> Tap **"घेतली (Confirm Taken)"**.
  4. Now tap **`Combiflam`** (Ibuprofen).
  5. **Expected Result:**
     - The screen flashes **Urgent Red (`#DE350B`)**.
     - Warning header: *"गंभीर औषध परस्परविरोध!"* (Severe Drug Contraindication).
     - Spoken Warning: *"सावधान! एस्पिरिन आणि कॉम्बीफ्लेम एकत्र घेतल्यास पोटात अंतर्गत रक्तस्त्रावाचा मोठा धोका आहे."*

---

### ⏰ Test Scenario 4: Food & Temporal Fasting Rule
* **Goal:** Verify that medicines requiring strict fasting (e.g. Thyroxine) deliver correct timing guidance.
* **How to Test:**
  1. Tap **`Thyronorm 50mcg`** in the Quick Test bar.
  2. **Expected Result:**
     - Card turns Green.
     - Spoken Instruction: *"हे सकाळी उपाशीपोटी घ्यायचे थायरॉईडचे औषध आहे. सकाळी उपाशी पोटी घ्या. ४५ मिनिटे चहा किंवा नाश्ता करू नका."*

---

### 📋 Test Scenario 5: Caregiver Audit Dashboard
* **Goal:** Verify that family members can review taken doses and blocked duplicate attempts.
* **How to Test:**
  1. Tap the **List icon (📋)** in the top right corner.
  2. **Expected Result:**
     - Shows all today's logs with exact timestamps (`hh:mm a`).
     - Taken medicines show a **Green checkmark badge** (`घेतले`).
     - Blocked attempts show a **Red warning badge** (`🚨 दुहेरी डोस अडवला (SOS Dispatched)`).
     - Tap **"कॅमेरा स्कॅनरवर परत जा"** to return to the camera view.

---

## 🔍 6. How to Debug & View Logs (Finding Bugs)

When developing or debugging Android apps, **Logcat** is your console output.

### Viewing Logs in Android Studio:
1. At the bottom of Android Studio, click the **"Logcat"** tab.
2. In the search filter box, type: `MedVoice` or `TextAnalyzer`.
3. You will see real-time log messages printed by the app:
   - `MedVoice_App`: Database warm-up and initialization status.
   - `MedVoice_ScanVM`: Scanned token evaluation and state transitions.
   - `MedVoice_SmsDispatcher`: Offline SMS dispatch status.

### Viewing Logs via Terminal / PowerShell:
```powershell
& "C:\Users\Ashraf\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -s "MedVoice_*" "TextAnalyzer"
```

---

## 🧪 7. How to Run Automated Unit Tests

You can run automated tests anytime to verify that core algorithms and database logic are 100% correct:

```powershell
# Run all unit tests
.\gradlew.bat test
```

### What these tests verify:
- `ExpiryParserTest`: Validates that expired blister packs are flagged and batch numbers are correctly parsed.
- `SafetyEngineTest`: Validates that the clinical matrix blocks duplicate active salts within their therapeutic window and prevents drug-drug interactions.

---

## 🗄️ 8. How to Add New Medicines to the Database

If you want to add more medicines to the offline database:

1. Open [scripts/compile_master_db.py](file:///c:/Users/Ashraf/Desktop/Hackathons/iQOO/scripts/compile_master_db.py).
2. Add your new drug to the `medicines` list (Brand Name, Manufacturer, Dosage Form, Strength, Primary Salt ID, Timing Rule ID, Vernacular Hindi/Marathi instructions).
3. Run the compiler:
   ```powershell
   python scripts/compile_master_db.py
   ```
4. Rebuild the app:
   ```powershell
   .\gradlew.bat assembleDebug
   ```

---

## 💡 9. Summary Checklist for Hackathon Demos

- [ ] Turn phone to **✈️ Airplane Mode** to prove 100% on-device edge execution.
- [ ] Turn phone volume to **Maximum** so the judges can hear the Marathi/Hindi voice.
- [ ] Scan **Glycomet-SR 500** -> Confirm taken.
- [ ] Scan **Gluconorm-SR 500** -> Point out the **Red Duplicate Overdose Warning**.
- [ ] Open **Caregiver Audit Log** -> Show the recorded duplicate prevention event.
