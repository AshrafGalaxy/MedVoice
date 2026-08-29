# 🧪 MedVoice — Judge & Evaluator Demo Test Pack

> **Welcome Judges!** MedVoice is an on-device medication safety system built for low-literacy seniors and their remote caregivers.
> You can evaluate all features in **under 2 minutes** by pointing your phone camera at the test cards below directly from your laptop screen!

---

## ⚡ Quick 2-Minute Evaluation Script

```
+---------------------------------------------------------------------------------------------------+
| STEP 1: Point camera at Card 1 (Augmentin)  ──► On-Device OCR + Spoken Vernacular Instructions    |
| STEP 2: Point camera at Card 2 (Dolo 650)   ──► Safe Oral Analgesic Intakes + Tap "Confirm Taken" |
| STEP 3: Point camera at Card 2 AGAIN        ──► 🚨 DUPLICATE DOSE BLOCKED + Cellular SOS SMS      |
| STEP 4: Point camera at Card 3 (Ketoconazole)► Topical Route Identified ("Do Not Swallow")       |
| STEP 5: Switch to Audit Log Tab             ──► Verified Timestamps, KPIs, & 1-Tap Doctor Share   |
+---------------------------------------------------------------------------------------------------+
```

---

## 📦 Test Card 1: Broad-Spectrum Antibiotic (Oral Tablet)

* **Target Formulation**: `Augmentin 625 Duo`
* **Expected Output**: Oral Tablet, Amoxicillin 500mg + Clavulanic Acid 125mg, After-Food Rule.

```
┌────────────────────────────────────────────────────────┐
│                      AUGMENTIN 625 DUO                 │
│                                                        │
│  Amoxicillin and Potassium Clavulanate Tablets IP      │
│                                                        │
│  Each film-coated tablet contains:                     │
│  Amoxicillin Trihydrate IP                             │
│  equivalent to Amoxicillin ................. 500 mg    │
│  Potassium Clavulanate IP                              │
│  equivalent to Clavulanic Acid ........... 125 mg      │
│                                                        │
│  Dosage: As directed by the Physician.                 │
│  Storage: Store in a dry place below 25°C.             │
│  Mfg Lic No: G/25/1442                                 │
│  Batch No: AG7482                                      │
│  Mfg Date: 03/2026   Exp Date: 02/2028                 │
└────────────────────────────────────────────────────────┘
```

---

## 📦 Test Card 2: Analgesic / Antipyretic (Duplicate Dose Test)

* **Target Formulation**: `Dolo 650`
* **Expected Output**: Paracetamol 650mg Oral Tablet.
* **Test Procedure**:
  1. Scan Card 2 $\rightarrow$ Tap **"✓ Confirm Taken"** (or say *"Yes"*).
  2. Scan Card 2 immediately again $\rightarrow$ **🚨 Active Duplicate Block Warning!** Audio warns the patient and dispatches an emergency SOS SMS to the caregiver.

```
┌────────────────────────────────────────────────────────┐
│                        DOLO 650                        │
│                                                        │
│  Paracetamol Tablets IP 650 mg                         │
│                                                        │
│  Each uncoated tablet contains:                        │
│  Paracetamol IP ............................ 650 mg    │
│  Excipients .................................... q.s.  │
│                                                        │
│  Indications: Fever, Headache, Bodyache.               │
│  Warning: Taking more than daily dose may cause        │
│  serious liver damage.                                 │
│  Mfg Lic No: M/732/2020                                │
│  Batch No: DL9281                                      │
│  Mfg Date: 01/2026   Exp Date: 12/2028                 │
└────────────────────────────────────────────────────────┘
```

---

## 📦 Test Card 3: Medicated Scalp Lotion (Topical Route Safeguard)

* **Target Formulation**: `Ketoconazole Scalp Lotion 2%`
* **Expected Output**: Topical Scalp Solution, Anti-Dandruff / Antifungal.
* **Safety Law**: System identifies external route and warns: *"Apply gently to the scalp. For external application only. Do not swallow."*

```
┌────────────────────────────────────────────────────────┐
│                 KETOCONAZOLE LOTION 2% w/v             │
│                                                        │
│  Ketoconazole Scalp Solution 2.0% w/v                  │
│                                                        │
│  Composition:                                          │
│  Ketoconazole IP ........................... 2.0% w/v  │
│  Lotion base ................................... q.s.  │
│                                                        │
│  FOR EXTERNAL TOPICAL APPLICATION ONLY                 │
│  NOT FOR ORAL CONSUMPTION - DO NOT SWALLOW             │
│                                                        │
│  Apply to wet scalp, leave for 5 minutes, rinse.       │
│  Batch No: KT4012    Exp: 10/2027                      │
└────────────────────────────────────────────────────────┘
```

---

## 📦 Test Card 4: Non-Drug Cosmetic Rejection Test

* **Target Product**: `Minimalist B5 Face Cleanser`
* **Expected Output**: Non-pharmaceutical product rejection / safe clarification prompt (*"Not a prescribed medicine"*) rather than guessing or hallucinating medical facts.

```
┌────────────────────────────────────────────────────────┐
│                 MINIMALIST FOAMING CLEANSER            │
│                                                        │
│  Vitamin B5 + Hyaluronic Acid Face Wash                │
│  Gentle daily facial cleanser for dry skin             │
│                                                        │
│  Ingredients: Aqua, Glycerin, Panthenol,               │
│  Sodium Lauroyl Sarcosinate, Citric Acid.              │
│                                                        │
│  Net Vol: 100 ml                                       │
│  Cosmetic formulation - Not for medicinal use          │
└────────────────────────────────────────────────────────┘
```

---

## 🛡️ Edge-Cloud Hybrid Architecture Verification

| Component | Hardware Route | Execution Latency |
| :--- | :--- | :--- |
| **OCR & Vision** | Google ML Kit (On-Device) | `< 80 ms` |
| **Pharmacopeia & Contraindications** | 30,000-Drug SQLite FTS5 (Local) | `< 5 ms` |
| **Vernacular TTS** | Google Neural TTS Engine (Hindi / English) | `< 120 ms` |
| **Neural LLM Reasoning** | On-Device Qwen 1.5B (or Cloud Gateway Qwen 27B) | `< 350 ms` |
| **Emergency SOS Guardrail** | Direct Cellular `SmsManager` | Direct Telephony |
