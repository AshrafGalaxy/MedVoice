# DATASET_GUIDE.md — Indian Pharmacopeia & Medicine Dataset Manual (DOC-09)

## Project: MedVoice (Edge Medication Safety & Ambient Voice Assistant)

---

## 1. Overview & Purpose

MedVoice relies on a deterministic **Indian Master Pharmacopeia SQLite Database** to perform sub-5ms local lookups for blister pack OCR matching, duplicate active salt detection, and food-drug temporal rules. 

This guide details **where to source verified Indian pharmaceutical datasets**, their **schema specifications**, and the **step-by-step ingestion pipeline** to compile them into the on-device database (`medvoice_master.db`).

---

## 2. Verified Indian Pharmaceutical Dataset Sources

You can collect and download open datasets from the following verified public repositories:

### 2.1 Primary Public Datasets

| Dataset Name | Source | Description | Size / Scope | Link / Access |
| :--- | :--- | :--- | :--- | :--- |
| **Indian Medicine Database (30,000+ Drugs)** | Kaggle (by Shashank/Snehal) | Comprehensive dataset containing Brand Names, Composition/Salts, Manufacturers, Side Effects, and Pack Sizes for Indian pharmacies. | ~30,000 Brands | [Kaggle Indian Medicine Dataset](https://www.kaggle.com/datasets/shashankasubrahmanya/indian-medicine-dataset) |
| **NLEM 2022 (National List of Essential Medicines)** | Ministry of Health & Family Welfare (MoHFW) / CDSCO | Official list of essential pharmaceutical compounds approved in India with therapeutic categories and strengths. | ~384 Essential Salts | [CDSCO Govt of India](https://cdsco.gov.in/) |
| **Jan Aushadhi Generic Medicine Catalogue** | PMBI (Pradhan Mantri Bhartiya Janaushadhi Pariyojana) | Complete mapping of generic salt compositions vs. branded equivalents in India. | ~1,800 Generic Compounds | [PMBI Official Portal](https://janaushadhi.gov.in/) |
| **OpenFDA Global Active Ingredient & NDC Mapping** | OpenFDA | Global active ingredient classifications, dosage forms, and drug-drug contraindication matrix. | 100,000+ entries | [OpenFDA Drug Database](https://open.fda.gov/data/drug/) |
| **DrugBank Open Data / ChEMBL Drug Target Matrix** | ChEMBL / DrugBank Academic | High-accuracy clinical contraindication matrix (Drug A + Drug B interaction mechanisms and severity). | Clinical Matrix | [EMBL-EBI ChEMBL](https://www.ebi.ac.uk/chembl/) |

---

## 3. Standard CSV Ingestion Schema

To ingest your collected medicines into MedVoice, format your data into a CSV file (e.g. `raw_medicines_india.csv`) with the following columns:

```csv
brand_name,salt_name,dosage_form,strength_mg,therapeutic_class,timing_rule,max_daily_dose_mg,active_window_hours,usage_en,usage_hi,instruction_en,instruction_hi
```

### Column Definitions:

1. **`brand_name`** *(String, Required)*: Commercial trade name printed on the blister pack (e.g. `Glycomet-SR 500`, `Thyronorm 50mcg`, `Telma 40`).
2. **`salt_name`** *(String, Required)*: Primary active chemical compound (e.g. `Metformin Hydrochloride`, `Levothyroxine Sodium`, `Telmisartan`).
3. **`dosage_form`** *(String)*: Formulation type (`TABLET`, `CAPSULE`, `SYRUP`, `INJECTION`, `DROPS`).
4. **`strength_mg`** *(Float)*: Active strength per single unit in milligrams (e.g. `500.0`, `50.0`, `40.0`).
5. **`therapeutic_class`** *(String)*: Medical drug classification (`ANTIDIABETIC`, `ANTIHYPERTENSIVE`, `THYROID`, `STATIN`, `NSAID_ANALGESIC`, `ANTACID_PPI`, `SUPPLEMENT`).
6. **`timing_rule`** *(String)*: Food timing code:
   - `BEFORE_MEAL` (Empty stomach / 30-45 mins before food)
   - `AFTER_MEAL` (With or immediately after food)
   - `BEDTIME` (Night before sleep)
   - `WITH_FOOD` (Taken during meal)
7. **`max_daily_dose_mg`** *(Float)*: Clinical ceiling daily dose (e.g. `2000.0` for Metformin).
8. **`active_window_hours`** *(Float)*: Duration in hours during which retaking the same salt triggers a duplicate alarm (e.g. `10.0` hours).
9. **`usage_en`** *(String)*: Simple English explanation for elderly patients (e.g. *"This is your diabetes blood sugar tablet."*).
10. **`usage_hi`** *(String)*: Simple Hindi explanation for elderly patients (e.g. *"यह आपकी शुगर की गोली है।"*).
11. **`instruction_en`** *(String)*: Spoken English dosage instruction (e.g. *"Take 1 tablet with water after your meal."*).
12. **`instruction_hi`** *(String)*: Spoken Hindi dosage instruction (e.g. *"खाना खाने के बाद एक गोली पानी के साथ लें।"*).

---

## 4. Step-by-Step Dataset Ingestion Workflow

```
[ Download CSV from Kaggle / CDSCO ]
                 │
                 ▼
[ Place CSV in scripts/data/medicines_master.csv ]
                 │
                 ▼
[ Run: python scripts/compile_master_db.py ]
                 │
                 ▼
[ Generates: app/src/main/assets/databases/medvoice_master.db ]
                 │
                 ▼
[ Room Master SQLite FTS5 Database Built with Zero Hallucination ]
```

### Step 1: Download or Prepare CSV
Download a dataset from Kaggle or CDSCO, and clean column names to match the schema above.

### Step 2: Run the Ingestion Compiler
Execute the Python compiler in your terminal:
```powershell
python scripts/compile_master_db.py
```

### Step 3: What the Compiler Does Automatically:
1. **Creates Normalized SQLite Tables**: `medicines`, `active_salts`, `food_temporal_rules`, `salt_contraindications`, and `medication_logs`.
2. **Builds FTS5 Full-Text Search Virtual Indexes**: Enables prefix search (`Glyc*`) and fuzzy token lookups in $<5\text{ ms}$.
3. **Embeds Room Verification Identity Hash**: Injects Room hash `1dbbbad710cad038638a39cf2ec24a2a` into `room_master_table` so the Android app mounts the database with zero migration errors.
4. **Outputs Ready-to-Ship Asset**: Writes the compiled binary directly to `app/src/main/assets/databases/medvoice_master.db`.

---

## 5. Clinical Safety Contraindication Rules

The contraindication matrix defines dangerous drug pairings:

| Drug A Salt | Drug B Salt | Severity | Clinical Risk Mechanism | Spoken Warning (English) | Spoken Warning (Hindi) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Aspirin (Ecosprin)** | **Ibuprofen (Combiflam)** | `CRITICAL` | Severe gastrointestinal ulceration and hemorrhage | *"Warning! Taking Aspirin and Combiflam together creates a severe risk of internal stomach bleeding."* | *"सावधान! एस्पिरिन और कॉम्बीफ्लेम साथ में लेने से पेट में ब्लीडिंग का खतरा है।"* |
| **Metformin (Glycomet)** | **Contrast Dye** | `HIGH` | Lactic acidosis risk | *"Warning! Do not take Metformin before radiologic scan."* | *"सावधान! स्कैन से पहले मेटफॉर्मिन न लें।"* |
| **Calcium (Shelcal)** | **Iron (Orofer XT)** | `MODERATE` | Chelation chews absorption | *"Take Calcium and Iron with a minimum 2-hour gap."* | *"कैल्शियम और आयरन की गोली के बीच 2 घंटे का अंतर रखें।"* |
| **Levothyroxine (Thyronorm)** | **Calcium / Antacid** | `HIGH` | Blocks thyroid hormone absorption | *"Take Thyronorm on an empty stomach. Keep 4 hours gap from Calcium."* | *"थायराइड की गोली खाली पेट लें। कैल्शियम से 4 घंटे दूर रखें।"* |

To add new contraindications, edit the `CONTRAINDICATIONS` list in `scripts/compile_master_db.py` and run the script.
