#!/usr/bin/env python3
"""
MedVoice Master Pharmacopeia SQLite Compiler
Generates: medvoice_master.db (SQLite with English, Hindi, and Marathi pharmaceutical data)
"""

import sqlite3
import os
import sys

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases")
DB_FILE = os.path.join(OUTPUT_DIR, "medvoice_master.db")
ROOT_DB_FILE = os.path.join(os.path.dirname(__file__), "..", "medvoice_master.db")

def init_database(db_path):
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    if os.path.exists(db_path):
        os.remove(db_path)
        
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Enable performance settings
    cursor.execute("PRAGMA page_size = 4096;")
    cursor.execute("PRAGMA foreign_keys = ON;")
    
    # 1. Create Tables matching Room entities exactly
    cursor.executescript("""
    CREATE TABLE IF NOT EXISTS `medicines` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `brand_name` TEXT NOT NULL,
        `manufacturer` TEXT,
        `dosage_form` TEXT NOT NULL,
        `strength_mg` REAL NOT NULL,
        `primary_salt_id` INTEGER NOT NULL,
        `secondary_salt_id` INTEGER,
        `timing_rule_id` INTEGER NOT NULL,
        `is_high_risk` INTEGER NOT NULL,
        `vernacular_usage_en` TEXT NOT NULL,
        `vernacular_usage_hi` TEXT NOT NULL,
        `vernacular_usage_mr` TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `active_salts` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `salt_name` TEXT NOT NULL,
        `therapeutic_class` TEXT NOT NULL,
        `max_daily_dose_mg` REAL NOT NULL,
        `half_life_hours` REAL NOT NULL,
        `active_window_hours` REAL NOT NULL,
        `vernacular_salt_desc_en` TEXT NOT NULL,
        `vernacular_salt_desc_hi` TEXT NOT NULL,
        `vernacular_salt_desc_mr` TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `food_temporal_rules` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `rule_code` TEXT NOT NULL,
        `food_relation` TEXT NOT NULL,
        `lead_time_minutes` INTEGER NOT NULL,
        `dietary_restriction` TEXT,
        `vernacular_instruction_en` TEXT NOT NULL,
        `vernacular_instruction_hi` TEXT NOT NULL,
        `vernacular_instruction_mr` TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `salt_contraindications` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `salt_a_id` INTEGER NOT NULL,
        `salt_b_id` INTEGER NOT NULL,
        `severity_level` TEXT NOT NULL,
        `clinical_risk_mechanism` TEXT NOT NULL,
        `spoken_warning_en` TEXT NOT NULL,
        `spoken_warning_hi` TEXT NOT NULL,
        `spoken_warning_mr` TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `medication_logs` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `medicine_id` INTEGER NOT NULL,
        `scanned_brand_name` TEXT NOT NULL,
        `resolved_salt_id` INTEGER NOT NULL,
        `intake_timestamp` INTEGER NOT NULL,
        `status` TEXT NOT NULL,
        `voice_confirmed` INTEGER NOT NULL,
        `sos_sms_dispatched` INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS room_master_table (
        id INTEGER PRIMARY KEY,
        identity_hash TEXT
    );
    INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '1dbbbad710cad038638a39cf2ec24a2a');
    """)
    conn.commit()
    return conn

def populate_seed_data(conn):
    cursor = conn.cursor()
    
    # 1. Seed Timing Rules
    timing_rules = [
        ('AFTER_MEAL', 'AFTER_FOOD', 15, 'Take after solid meal',
         'Take 1 tablet with water after your meal.',
         'खाना खाने के बाद एक गोली पानी के साथ लें।',
         'जेवणानंतर एक गोळी पाण्यासोबत घ्या.'),
        ('STRICT_EMPTY_STOMACH', 'EMPTY_STOMACH', 45, 'Strictly 45 mins before morning tea/breakfast',
         'Take on an empty stomach in the morning. Avoid tea or breakfast for 45 minutes.',
         'सुबह खाली पेट लें। 45 मिनट तक चाय या नाश्ता न करें।',
         'सकाळी उपाशी पोटी घ्या. ४५ मिनिटे चहा किंवा नाश्ता करू नका.'),
        ('BEDTIME', 'BEDTIME', 30, 'Take 30 mins before sleep',
         'Take 30 minutes before going to bed at night.',
         'रात को सोने से पहले लें।',
         'रात्री झोपण्यापूर्वी घ्या.'),
        ('WITH_FOOD', 'WITH_FOOD', 0, 'Take in between meals to prevent gastric burn',
         'Take with food or in between meals to avoid stomach irritation.',
         'खाना खाते समय बीच में लें ताकि पेट में जलन न हो।',
         'जेवताना मध्येच घ्या जेणेकरून पोटात जळजळ होणार नाही.')
    ]
    cursor.executemany("""
    INSERT INTO food_temporal_rules (rule_code, food_relation, lead_time_minutes, dietary_restriction, vernacular_instruction_en, vernacular_instruction_hi, vernacular_instruction_mr)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """, timing_rules)

    # 2. Seed Master Salts
    salts = [
        ('Metformin Hydrochloride', 'ANTIDIABETIC', 2000.0, 6.5, 10.0,
         'Diabetes blood sugar control medicine',
         'शुगर नियंत्रित करने की दवा',
         'रक्तातील साखर नियंत्रित करणारे औषध'),
        ('Levothyroxine Sodium', 'THYROID', 0.2, 168.0, 24.0,
         'Thyroid hormone replacement therapy',
         'थायराइड ग्रंथि की दवा',
         'थायरॉईड ग्रंथीचे औषध'),
        ('Amlodipine Besylate', 'ANTIHYPERTENSIVE', 10.0, 35.0, 24.0,
         'Blood pressure lowering medication',
         'ब्लड प्रेशर कम करने की दवा',
         'रक्तदाब कमी करणारे औषध'),
        ('Telmisartan', 'ANTIHYPERTENSIVE', 80.0, 24.0, 24.0,
         'Blood pressure and cardiac protector',
         'बीपी और हृदय सुरक्षा की दवा',
         'बीपी आणि हृदयाच्या संरक्षणाचे औषध'),
        ('Atorvastatin', 'STATIN', 80.0, 14.0, 24.0,
         'Cholesterol lowering statin medication',
         'कोलेस्ट्रॉल कम करने की दवा',
         'कोलेस्टेरॉल कमी करणारे औषध'),
        ('Pantoprazole Sodium', 'ANTACID_PPI', 80.0, 1.5, 12.0,
         'Gastric acid and reflux relief',
         'पेट में गैस और एसिडिटी की दवा',
         'पोटातील गॅस आणि ऍसिडिटीचे औषध'),
        ('Ibuprofen', 'NSAID_ANALGESIC', 2400.0, 2.0, 8.0,
         'Pain and anti-inflammatory relief',
         'दर्द और सूजन की दवा',
         'वेदना आणि सूज कमी करणारे औषध'),
        ('Aspirin', 'ANTIPLATELET', 325.0, 0.5, 24.0,
         'Blood thinner antiplatelet agent',
         'खून पतला करने की दवा',
         'रक्त पातळ करणारे औषध'),
        ('Calcium Carbonate', 'SUPPLEMENT', 1500.0, 4.0, 12.0,
         'Bone health calcium supplement',
         'हड्डियों की मजबूती के लिए कैल्शियम',
         'हाडांच्या मजबुतीसाठी कॅल्शियम'),
        ('Ferrous Ascorbate (Iron)', 'SUPPLEMENT', 200.0, 6.0, 12.0,
         'Hemoglobin booster iron supplement',
         'खून बढ़ाने के लिए आयरन',
         'रक्तातील हिमोग्लोबिन वाढवण्यासाठी लोह')
    ]
    cursor.executemany("""
    INSERT INTO active_salts (salt_name, therapeutic_class, max_daily_dose_mg, half_life_hours, active_window_hours, vernacular_salt_desc_en, vernacular_salt_desc_hi, vernacular_salt_desc_mr)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, salts)

    # 3. Seed Indian Commercial Medicines
    medicines = [
        ('Glycomet-SR 500', 'USV Private Limited', 'TABLET', 500.0, 1, None, 1, 0,
         'This is your diabetes blood sugar tablet.',
         'यह आपकी शुगर की गोली है।',
         'हे तुमचे साखरेचे औषध आहे.'),
        ('Gluconorm-SR 500', 'Lupin Ltd', 'TABLET', 500.0, 1, None, 1, 0,
         'This is your diabetes blood sugar tablet.',
         'यह आपकी शुगर की गोली है।',
         'हे तुमचे साखरेचे औषध आहे.'),
        ('Cetapin XR 500', 'Sanofi India', 'TABLET', 500.0, 1, None, 1, 0,
         'This is your diabetes blood sugar tablet.',
         'यह आपकी शुगर की गोली है।',
         'हे तुमचे साखरेचे औषध आहे.'),
        ('Thyronorm 50mcg', 'Abbott India', 'TABLET', 0.05, 2, None, 2, 0,
         'This is your thyroid tablet to be taken early morning on an empty stomach.',
         'यह सुबह खाली पेट लेने वाली थायराइड की गोली है।',
         'हे सकाळी उपाशीपोटी घ्यायचे थायरॉईडचे औषध आहे.'),
        ('Eltroxin 50mcg', 'GSK India', 'TABLET', 0.05, 2, None, 2, 0,
         'This is your thyroid tablet to be taken early morning on an empty stomach.',
         'यह सुबह खाली पेट लेने वाली थायराइड की गोली है।',
         'हे सकाळी उपाशीपोटी घ्यायचे थायरॉईडचे औषध आहे.'),
        ('Telma 40', 'Glenmark Pharmaceuticals', 'TABLET', 40.0, 4, None, 1, 0,
         'This is your blood pressure medication.',
         'यह ब्लड प्रेशर की दवा है।',
         'हे ब्लड प्रेशरचे औषध आहे.'),
        ('Amlong 5', 'Micro Labs Ltd', 'TABLET', 5.0, 3, None, 1, 0,
         'This is your BP control medicine.',
         'यह बीपी नियंत्रित करने की दवा है।',
         'हे बीपी नियंत्रित करणारे औषध आहे.'),
        ('Atorva 10', 'Zydus Cadila', 'TABLET', 10.0, 5, None, 3, 0,
         'This is your nighttime cholesterol medicine.',
         'यह कोलेस्ट्रॉल की रात की दवा है।',
         'हे कोलेस्टेरॉलचे रात्री घ्यायचे औषध आहे.'),
        ('Pan 40', 'Alkem Laboratories', 'TABLET', 40.0, 6, None, 2, 0,
         'This is your antacid and gas relief tablet.',
         'यह गैस और एसिडिटी की गोली है।',
         'ही गॅस आणि ऍसिडिटीची गोळी आहे.'),
        ('Combiflam', 'Sanofi India', 'TABLET', 400.0, 7, None, 1, 0,
         'This is for pain and fever relief. Take strictly after food.',
         'यह दर्द और बुखार की दवा है। खाना खाकर ही लें।',
         'हे अंगदुखी आणि तापाचे औषध आहे. जेवण झाल्यावरच घ्या.'),
        ('Ecosprin 75', 'USV Ltd', 'TABLET', 75.0, 8, None, 1, 0,
         'This is your blood thinner aspirin tablet.',
         'यह खून पतला करने की गोली है।',
         'हे रक्त पातळ करण्याचे औषध आहे.'),
        ('Shelcal 500', 'Torrent Pharmaceuticals', 'TABLET', 500.0, 9, None, 1, 0,
         'This is your calcium bone supplement tablet.',
         'यह कैल्शियम की गोली है।',
         'ही कॅल्शियमची गोळी आहे.'),
        ('Orofer XT', 'Emcure Pharmaceuticals', 'TABLET', 100.0, 10, None, 1, 0,
         'This is your iron supplement tablet to increase hemoglobin.',
         'यह आयरन और खून बढ़ाने की गोली है।',
         'ही रक्तातील लोह वाढवणारी गोळी आहे.')
    ]
    cursor.executemany("""
    INSERT INTO medicines (brand_name, manufacturer, dosage_form, strength_mg, primary_salt_id, secondary_salt_id, timing_rule_id, is_high_risk, vernacular_usage_en, vernacular_usage_hi, vernacular_usage_mr)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, medicines)

    # 4. Seed Severe Contraindications
    contraindications = [
        (8, 7, 'CRITICAL', 'Aspirin + Ibuprofen induces severe gastrointestinal ulceration and internal bleeding.',
         'Warning! Taking Aspirin and Combiflam together creates a severe risk of internal stomach bleeding.',
         'सावधान! एस्पिरिन और कॉम्बीफ्लेम साथ में लेने से पेट में ब्लीडिंग का खतरा है।', 
         'सावधान! एस्पिरिन आणि कॉम्बीफ्लेम एकत्र घेतल्यास पोटात अंतर्गत रक्तस्त्रावाचा मोठा धोका आहे.'),
        (9, 10, 'WARNING', 'Calcium severely impairs Iron absorption by competitive chelation.',
         'Caution! Do not take Calcium and Iron supplements together. Keep a 2-hour gap between them.',
         'ध्यान दें! कैल्शियम और आयरन की गोली एक साथ न लें। दोनों में 2 घंटे का अंतर रखें।', 
         'लक्षात ठेवा! कॅल्शियम आणि लोहाची गोळी एकत्र घेऊ नका. दोन्हींमध्ये २ तासांचे अंतर ठेवा.')
    ]
    cursor.executemany("""
    INSERT INTO salt_contraindications (salt_a_id, salt_b_id, severity_level, clinical_risk_mechanism, spoken_warning_en, spoken_warning_hi, spoken_warning_mr)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """, contraindications)

    conn.commit()

def main():
    print("Compiling MedVoice Master Database...")
    for target in [DB_FILE, ROOT_DB_FILE]:
        conn = init_database(target)
        populate_seed_data(conn)
        conn.close()
        print(f"Generated SQLite DB: {target} (Size: {os.path.getsize(target)} bytes)")

if __name__ == "__main__":
    main()
