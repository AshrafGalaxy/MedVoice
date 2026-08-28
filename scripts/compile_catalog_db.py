#!/usr/bin/env python3
"""
MedVoice Master Pharmacopeia SQLite Catalog Compiler
Downloads the 30k Indian Medicines Dataset and compiles a lightweight SQLite FTS5 catalog.
Outputs to: app/src/main/assets/databases/medvoice_master.db
"""

import csv
import os
import sqlite3
import urllib.request

CSV_URL = "https://raw.githubusercontent.com/junioralive/Indian-Medicine-Dataset/main/DATA/indian_medicine_data.csv"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(SCRIPT_DIR, "data")
RAW_FILE = os.path.join(RAW_DIR, "indian_medicine_data.csv")
OUTPUT_DB = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets", "databases", "medvoice_master.db")

def download_raw_data():
    os.makedirs(RAW_DIR, exist_ok=True)
    if not os.path.exists(RAW_FILE) or os.path.getsize(RAW_FILE) < 1000:
        print("Downloading GitHub Indian Medicine Dataset from junioralive/Indian-Medicine-Dataset...")
        opener = urllib.request.build_opener()
        opener.addheaders = [('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)')]
        urllib.request.install_opener(opener)
        urllib.request.urlretrieve(CSV_URL, RAW_FILE)
        print(f"Download complete: {RAW_FILE} ({os.path.getsize(RAW_FILE)} bytes)")
    else:
        print(f"Using cached raw dataset: {RAW_FILE} ({os.path.getsize(RAW_FILE)} bytes)")

def build_database():
    output_dir = os.path.dirname(OUTPUT_DB)
    os.makedirs(output_dir, exist_ok=True)
    if os.path.exists(OUTPUT_DB):
        try:
            os.remove(OUTPUT_DB)
        except Exception as e:
            print(f"Warning: Could not remove existing DB: {e}")

    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.cursor()

    cursor.execute("PRAGMA page_size = 4096;")
    cursor.execute("PRAGMA synchronous = OFF;")
    cursor.execute("PRAGMA journal_mode = MEMORY;")

    cursor.executescript("""
    CREATE TABLE IF NOT EXISTS medicines (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        brand_name TEXT NOT NULL COLLATE NOCASE,
        raw_composition TEXT NOT NULL,
        manufacturer TEXT,
        dosage_form TEXT NOT NULL
    );

    CREATE VIRTUAL TABLE IF NOT EXISTS medicines_fts USING fts5(
        brand_name,
        raw_composition,
        content='medicines',
        content_rowid='id',
        tokenize='unicode61 remove_diacritics 2'
    );

    CREATE TABLE IF NOT EXISTS medication_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        medicine_id INTEGER NOT NULL,
        scanned_text TEXT NOT NULL,
        parsed_salts TEXT NOT NULL,
        intake_timestamp INTEGER NOT NULL,
        status TEXT NOT NULL,
        voice_confirmed INTEGER NOT NULL,
        sos_sms_dispatched INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS room_master_table (
        id INTEGER PRIMARY KEY,
        identity_hash TEXT
    );
    INSERT OR REPLACE INTO room_master_table (id, identity_hash) 
    VALUES(42, 'eb32bad5c1e563f6971e7e6fa20a64c9');
    """)

    batch = []
    seen_brands = set()

    # Core essential baseline seed records to guarantee immediate offline testing
    seed_medicines = [
        ("Glycomet-SR 500", "Metformin Hydrochloride 500mg SR", "USV Ltd", "TABLET"),
        ("Brufen 400", "Ibuprofen 400mg", "Abbott Healthcare", "TABLET"),
        ("Thyronorm 50", "Levothyroxine Sodium 50mcg", "Abbott Healthcare", "TABLET"),
        ("Combiflam", "Ibuprofen 400mg + Paracetamol 325mg", "Sanofi India", "TABLET"),
        ("Crocin Advance 500", "Paracetamol 500mg Fast Release", "GlaxoSmithKline", "TABLET"),
        ("Calpol 650", "Paracetamol 650mg", "GlaxoSmithKline", "TABLET"),
        ("Dolo 650", "Paracetamol 650mg", "Micro Labs Ltd", "TABLET"),
        ("Ecosprin 75", "Aspirin 75mg Gastro-resistant", "USV Ltd", "TABLET"),
        ("Disprin", "Aspirin 350mg Effervescent", "Reckitt Benckiser", "TABLET"),
        ("Pan 40", "Pantoprazole Sodium 40mg", "Alkem Laboratories", "TABLET"),
        ("Pantocid 40", "Pantoprazole 40mg", "Sun Pharma", "TABLET"),
        ("Omez 20", "Omeprazole 20mg", "Dr. Reddy's Laboratories", "TABLET"),
        ("Maritima Euphrasia Eye Drops", "Cineraria Maritima + Euphrasia Ophthalmic 10ml", "SBL Pvt Ltd", "EYE_DROPS"),
        ("Moxicip Eye Drops", "Moxifloxacin Hydrochloride 0.5% w/v 5ml", "Cipla Ltd", "EYE_DROPS"),
        ("Benadryl Cough Formula", "Dextromethorphan HBr 10mg + Diphenhydramine HCl 100ml", "Johnson & Johnson", "SYRUP"),
        ("Liv 52 Tonic", "Himsra + Kasani Herbal Liver Tonic 200ml", "Himalaya Wellness", "SYRUP"),
        ("Volini Pain Relief Gel", "Diclofenac Diethylamine 1.16% + Methyl Salicylate 30gm", "Sun Pharma", "GEL"),
        ("Otrivin Oxy Fast Relief", "Oxymetazoline Hydrochloride 0.05% Nasal Spray 10ml", "GSK Consumer", "NASAL_SPRAY"),
        ("Asthalin Inhaler", "Salbutamol 100mcg CFC-Free Inhaler 200 MDI", "Cipla Ltd", "INHALER"),
        ("Telma 40", "Telmisartan 40mg", "Glenmark Pharmaceuticals", "TABLET"),
        ("Amlong 5", "Amlodipine Besylate 5mg", "Micro Labs Ltd", "TABLET"),
        ("Atorva 10", "Atorvastatin Calcium 10mg", "Zydus Cadila", "TABLET"),
        ("Augmentin 625 Duo", "Amoxicillin 500mg + Clavulanic Acid 125mg", "GlaxoSmithKline", "TABLET"),
        ("Azithral 500", "Azithromycin 500mg", "Alembic Pharmaceuticals", "TABLET"),
        ("Januvia 100", "Sitagliptin Phosphate 100mg", "MSD Pharmaceuticals", "TABLET"),
        ("Galvus 50", "Vildagliptin 50mg", "Novartis India", "TABLET"),
        ("Jardiance 10", "Empagliflozin 10mg", "Boehringer Ingelheim", "TABLET"),
        ("Forxiga 10", "Dapagliflozin Propanediol 10mg", "AstraZeneca Pharma", "TABLET"),
        ("Rosuvas 10", "Rosuvastatin Calcium 10mg", "Ranbaxy Laboratories", "TABLET"),
        ("Storvas 20", "Atorvastatin 20mg", "Sun Pharma", "TABLET")
    ]

    for brand, comp, manuf, form in seed_medicines:
        batch.append((brand, comp, manuf, form))
        seen_brands.add(brand.strip().lower())

    if os.path.exists(RAW_FILE):
        print(f"Ingesting records from {RAW_FILE}...")
        with open(RAW_FILE, mode='r', encoding='utf-8', errors='ignore') as f:
            reader = csv.DictReader(f)
            for row in reader:
                brand = row.get('name') or row.get('product_name') or row.get('brand_name')
                comp = row.get('short_composition1') or row.get('salt_composition') or row.get('composition') or row.get('raw_composition')
                manuf = row.get('manufacturer_name') or row.get('manufacturer') or "Standard Pharma"
                
                if brand and comp:
                    brand_clean = brand.strip()
                    comp_clean = comp.strip()
                    manuf_clean = manuf.strip() if manuf else "Standard Pharma"
                    
                    # Detect dosage form
                    name_upper = (brand_clean + " " + comp_clean).upper()
                    if "EYE DROP" in name_upper or "OPHTHALMIC" in name_upper:
                        form = "EYE_DROPS"
                    elif "EAR DROP" in name_upper:
                        form = "EAR_DROPS"
                    elif "NASAL" in name_upper or "SPRAY" in name_upper:
                        form = "NASAL_SPRAY"
                    elif "SYRUP" in name_upper or "TONIC" in name_upper or "SUSPENSION" in name_upper or "LIQUID" in name_upper:
                        form = "SYRUP"
                    elif "GEL" in name_upper or "OINTMENT" in name_upper or "CREAM" in name_upper or "EMULGEL" in name_upper:
                        form = "GEL"
                    elif "INHALER" in name_upper or "RESPICAPS" in name_upper or "MDI" in name_upper:
                        form = "INHALER"
                    elif "CAPSULE" in name_upper or "CAP" in name_upper:
                        form = "CAPSULE"
                    else:
                        form = "TABLET"

                    brand_key = brand_clean.lower()
                    if brand_key not in seen_brands:
                        seen_brands.add(brand_key)
                        batch.append((brand_clean, comp_clean, manuf_clean, form))

    print(f"Writing {len(batch)} medicines to SQLite catalog...")
    cursor.executemany("""
    INSERT INTO medicines (brand_name, raw_composition, manufacturer, dosage_form)
    VALUES (?, ?, ?, ?)
    """, batch)

    cursor.execute("""
    INSERT INTO medicines_fts(rowid, brand_name, raw_composition)
    SELECT id, brand_name, raw_composition FROM medicines;
    """)

    conn.commit()
    conn.close()
    
    file_size_mb = os.path.getsize(OUTPUT_DB) / (1024 * 1024)
    print(f"Catalog DB compiled successfully ({len(batch)} records, {file_size_mb:.2f} MB) at: {OUTPUT_DB}")

if __name__ == "__main__":
    download_raw_data()
    build_database()
