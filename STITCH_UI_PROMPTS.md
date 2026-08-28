# 🎨 MedVoice — Google Stitch Master UI Design Prompts

> **How to Use This Document:**  
> This file contains copy-pasteable, production-grade prompts crafted for **Google Stitch** (and modern AI UI generators like v0, Figma AI, or Galileo). Each prompt is structured with explicit UI tokens, component hierarchy, senior-accessible typography, and precise English/Hindi text copy.

---

## 📐 SECTION 1: MedVoice Global Design System Tokens

Copy and paste this **System Directive** into Google Stitch before generating individual screens to establish the global style guide.

```text
[DESIGN SYSTEM DIRECTIVE FOR STITCH]
You are a Principal Healthcare & Senior Accessibility UI/UX Designer.
Design an ultra-premium, dark-mode, WCAG AAA compliant Android mobile application called "MedVoice" (100% On-Device Medication Safety & Vernacular Voice Assistant for Seniors and Caregivers).

DEVICE CONSTRAINTS:
- Screen Aspect Ratio: Modern 20:9 Android Mobile Display (1080 x 2400 px / 6.43-inch to 6.7-inch displays)
- Orientation: Portrait
- Corner Radius: 16dp on cards, 12dp on interactive pills, 28dp on floating badges

COLOR PALETTE:
- Background Master: #0B0F17 (Deep Medical Slate Canvas)
- Card Surface Default: #151D2A (High-Contrast Slate Container)
- Card Surface Elevated: #1E293B (Active / Highlighted Component Container)
- Accent Border: #334155 (Subtle 1px boundary lines)
- Primary Safe / Brand Green: #10B981 (Vibrant Emerald - Verified Safe Dose)
- Primary Medical Cyan: #06B6D4 (High-Tech Reticle & Audio Visualizer)
- Primary Alert / Danger Red: #EF4444 (Critical Duplicate Salt Blocked & Conflict Warning)
- Warning Amber: #F59E0B (Dietary / Food Timing Rule)
- Text Primary: #FFFFFF (Pure Crisp White, 100% Opacity)
- Text Secondary / Muted: #94A3B8 (High-Legibility Slate Muted, 7:1 Contrast Ratio)

TYPOGRAPHY & ACCESSIBILITY LAWS:
- Font Family: Inter / Noto Sans Devanagari (Clean, legible, zero-serif)
- Minimum Touch Target: 48dp to 56dp for all clickable buttons and action cards
- Senior Visibility: Bold typographic hierarchy, large status badges, zero subtle gray-on-gray clipping
- Bilingual Support: Integrated English and Hindi (हिंदी) labels and audio guidance tags
```

---

## 📱 SECTION 2: Master Screen-by-Screen Stitch Prompts

---

### 🟢 SCREEN 1: Senior Onboarding & Baseline Setup Wizard

```text
[PROMPT FOR STITCH - SCREEN 1: ONBOARDING WIZARD]
Design an accessible 3-step introductory onboarding wizard screen for the "MedVoice" Android app on a #0B0F17 dark slate background.

HEADER AREA:
- Top bar with a progress indicator showing "Step 1 of 3" with a glowing emerald active indicator (#10B981) and muted remaining dots.
- Centered Logo: A stylized rounded gradient badge with bold white monogram "MV" and emerald-cyan glowing border.
- Main Title: "Welcome to MedVoice" in 24sp Bold White text.
- Subtitle: "Offline on-device medication safety & voice assistant for Indian seniors" in 14sp #94A3B8.

STEP 1: LANGUAGE SELECTION CARDS:
- Card A (Selected): Large #1E293B card with a 2px #10B981 border. Inside: English flag icon, Title "English", Subtitle "Clear spoken voice guidance", and a glowing green checkmark icon.
- Card B (Unselected): Large #151D2A card with a 1px #334155 border. Inside: Hindi Devanagari icon, Title "हिंदी (Hindi)", Subtitle "सरल हिंदी में बोलकर दवा के निर्देश".

STEP 2: VOICE PERSONALIZATION PREVIEW (Visible lower section):
- Section Title: "Voice Style & Senior Clarity"
- 2 Selection Pills: "Female (Warm & Slow)" and "Male (Clear & Confident)"
- Speech Speed Pill Selector: [0.75x Slow] | [0.88x Senior (Active)] | [1.0x Normal]
- Audio Sample Button: A wide #06B6D4 cyan button with a Play icon and text "Listen to Voice Preview (आवाज सुनें)".

BOTTOM ACTION BAR:
- A high-contrast, full-width 54dp #10B981 vibrant emerald button with bold white text "Continue (आगे बढ़ें)" and a right arrow icon.
```

---

### 🟢 SCREEN 2: Home Dashboard & Dynamic Daily Medication Timeline

```text
[PROMPT FOR STITCH - SCREEN 2: HOME DASHBOARD]
Design the main Home Dashboard screen for "MedVoice" Android app on a #0B0F17 deep slate background.

TOP APP BAR:
- Left: An elegant 40x40dp rounded gradient logo badge ("MV" in bold white over #10B981 to #06B6D4 gradient). Next to it, text column: "Good Afternoon ☀️" in 12sp #06B6D4 and "Dadi (आजी)" in 20sp Bold White.
- Right: A compact segmented language toggle pill in #1E293B with "EN" (active emerald green background) and "हिंदी" (neutral).

HERO ACTION CARD (POINT & SCAN):
- A prominent, highly clickable 90dp card with a 2px glowing #10B981 emerald border and #151D2A background.
- Left: A 52x52dp vibrant emerald circular icon with a white Camera lens glyph.
- Center: Title "Point & Scan Blister Pack" in 18sp Bold White, with subtitle "Instant vernacular spoken safety rules" in 13sp #94A3B8.
- Right: A glowing right chevron icon.

SAFETY STATUS BANNER:
- A sleek #1E293B container with an Emerald Shield icon on the left.
- Text: "100% On-Device Edge Safety Active • 0 Drug Conflicts".
- Status Badge: Pill badge with green dot and text "0 Conflicts / Safe".

TODAY'S MEDICATION SCHEDULE SECTION:
- Section Header: "Today's Medication Schedule" in 17sp Bold White with a clock icon.
- List of Prescribed Prescription Cards:
  1. Card 1 (Taken): #1E293B container at 70% opacity. Timing tag "AFTER BREAKFAST" in #10B981. Medicine "Glycomet-SR 500 (Metformin)", Subtitle "Take with or immediately after food". Right side: Green pill badge "✓ TAKEN 08:30 AM".
  2. Card 2 (Pending): #151D2A container with 1px #334155 border. Timing tag "EMPTY STOMACH" in #06B6D4. Medicine "Thyronorm 50mcg", Subtitle "Take 30 mins before morning tea/food". Right side: Emerald "Scan" action button.
  3. Card 3 (Evening): #151D2A container. Timing tag "AFTER DINNER". Medicine "Pan 40 (Pantoprazole)". Right side: Emerald "Scan" action button.

CAREGIVER EMERGENCY SOS BAR:
- A compact footer card in #151D2A with a Phone icon, text "Caregiver SOS: +91 98765 43210", and a "Test SOS" button.

BOTTOM NAVIGATION BAR (5 TABS):
- #0B0F17 dark background with top border #1E293B.
- 5 Icons:
  1. Home (Active Emerald Green icon + label)
  2. Scanner (Camera icon with cyan reticle ring)
  3. Cabinet (Pill bottle icon)
  4. Caregiver (Audit log checklist icon)
  5. Settings (Gear icon)
```

---

### 🟢 SCREEN 3: Live Camera Scanner — Active Reticle Viewport (State A)

```text
[PROMPT FOR STITCH - SCREEN 3: SCANNER ACTIVE RETICLE]
Design the Live Camera Scanner interface for "MedVoice" during active camera analysis.

TOP OVERLAY BAR (TRANSPARENT SLATE):
- Back icon, Title "Live Camera Scanner", Subtitle "Hold medicine strip within frame", and the [EN | हिंदी] toggle pill in the top-right corner.

CAMERA VIEWPORT (CENTER):
- Realistic photo background showing a blurred medicine blister pack (e.g. Glycomet-SR strip) being held in a senior's hand.
- High-Contrast Dynamic Reticle: A glowing cyan rounded rectangle (#06B6D4, 3px border, 260x150dp) with corner tick marks and a subtle laser scanning beam animation across the center.
- Floating Flashlight / Torch Toggle: A circular 44dp translucent slate button with a yellow Flash icon in the top-right of the camera frame.

QUICK TEST SCENARIO HORIZONTAL CHIP STRIP (BELOW HEADER):
- Smooth horizontal scrolling chips in #151D2A with 1px borders:
  - [Glycomet-SR 500 (Safe)]
  - [Gluconorm-SR 500 (Metformin Trap)]
  - [Thyronorm 50mcg (Rule)]
  - [Combiflam (Conflict)]

BOTTOM ACTION CARD (STATUS: READY):
- A dark card (#151D2A) with rounded 16dp top corners.
- Animated pulsating soundwave graphic in #06B6D4 cyan.
- Text: "Point camera at medicine blister pack... Spoken guidance will play automatically." in 15sp SemiBold White.
```

---

### 🟢 SCREEN 4: Scanner Feedback — Safe Dose Detected (State B)

```text
[PROMPT FOR STITCH - SCREEN 4: SAFE DOSE VERIFIED]
Design the Live Camera Scanner result view for MedVoice when a SAFE prescription is scanned.

CAMERA VIEWPORT (UPPER HALF):
- Blister pack in focus with a glowing solid Emerald Green bounding reticle (#10B981) enclosing the brand name text.

BOTTOM ACTION CONTAINER (EMERALD SAFE OVERLAY):
- Full-width container colored in vibrant Emerald Green (#10B981) with 20dp top rounded corners and high-contrast styling.
- Top Header: Green Shield icon with text "SAFE MEDICATION VERIFIED (दवा सुरक्षित है)".
- Large Brand Name: "Glycomet-SR 500" in 24sp Bold White text.
- Active Salt Pill Badge: #0B0F17 dark pill containing "Active: Metformin Hydrochloride 500mg".
- Spoken Audio Transcript Box (Semi-transparent white container):
  - Speaker icon animating soundwaves.
  - Text: "यह ग्लाइकोमेट 500mg है। इसे भोजन के तुरंत बाद एक गिलास पानी के साथ लें।" (English subtitle: "This is Glycomet 500mg. Take with food.")
- GIANT CONFIRMATION BUTTON:
  - 60dp full-width Pure White button (#FFFFFF) with bold #10B981 emerald text: "✓ Confirm Taken (ले ली)" and large checkmark icon.
```

---

### 🟢 SCREEN 5: Scanner Feedback — Duplicate Salt Trap Blocked (State C)

```text
[PROMPT FOR STITCH - SCREEN 5: DUPLICATE TRAP BLOCKED]
Design the high-alert, critical warning screen for MedVoice when a dangerous DUPLICATE DOSE / BRAND TRAP is detected.

CAMERA VIEWPORT (UPPER HALF):
- Camera background showing a scanned strip of "Gluconorm-SR 500" enclosed in a pulsating bright Coral Red reticle (#EF4444).

BOTTOM ACTION CONTAINER (ALERT RED CONTAINER):
- Full-width container colored in high-visibility Alert Red (#EF4444) with bold warning iconography.
- Top Warning Banner: Large Warning Triangle icon with text "🚨 CRITICAL WARNING: DO NOT RETAKE!".
- Scanned Brand Name: "Gluconorm-SR 500" in 22sp Bold White text.
- Duplicate Salt Mechanism Box (Dark Slate #151D2A container with red border):
  - Text in 14sp Bold White: "Contains Active Salt: METFORMIN (500mg)".
  - Warning Detail: "You already took 'Glycomet-SR 500' (Metformin) 42 minutes ago at 08:30 AM. Taking this will cause dangerous Hypoglycemia overdose."
- Spoken Voice Audio Alert Box:
  - Speaker with red wave glyph: "सावधान! यह दवा न लें। आपने 42 मिनट पहले यही साल्ट ग्लाइकोमेट लिया था।"
- EMERGENCY SOS CELLULAR DISPATCH BADGE:
  - Yellow badge: "📡 Emergency SMS alert dispatched to Caregiver (+91 98765 43210)".
- ACTION BUTTON:
  - 54dp Pure White button with bold #EF4444 red text: "Scan Next Medicine (अगली दवा स्कैन करें)".
```

---

### 🟢 SCREEN 6: My Medicine Cabinet & Prescriptions Catalogue

```text
[PROMPT FOR STITCH - SCREEN 6: MEDICINE CABINET]
Design the "My Medicine Cabinet" screen for the MedVoice Android app on a #0B0F17 canvas.

HEADER:
- Medical Cross icon in emerald green, Title "My Medicine Cabinet", Subtitle "Active Prescriptions & Spoken Dosage Rules".

SEARCH BAR:
- Sleek 50dp search input with #151D2A container, #334155 border, search magnifying glass icon, and placeholder "Search medicine, salt, or therapeutic class...".

MEDICINE INVENTORY CARDS (SCROLLABLE LIST):
- Card 1: #151D2A background, 14dp rounded corners.
  - Top Row: Brand "Glycomet-SR 500" in 17sp Bold White, Class "Antidiabetic", and a circular 40dp #10B981 speaker button (Tap to Read Aloud).
  - Subtitle: "Active: Metformin Hydrochloride (500mg)" in #06B6D4 cyan.
  - Timing Banner: #1E293B container with clock icon: "Rule: Take immediately after meals with water".
  - Stock Indicator: "Active Prescription Strip • 10 Tablets remaining".

- Card 2: #151D2A background.
  - Top Row: Brand "Thyronorm 50mcg", Class "Thyroid Hormone", and Green speaker button.
  - Subtitle: "Active: Levothyroxine Sodium (50mcg)" in #06B6D4.
  - Timing Banner: #1E293B container with warning amber icon: "Strict Rule: Empty stomach, 30 mins before food/tea".

- Card 3: #151D2A background.
  - Top Row: Brand "Ecosprin 75", Class "Antiplatelet / Blood Thinner", and Green speaker button.
  - Subtitle: "Active: Aspirin (75mg)" in #06B6D4.
  - Timing Banner: "Rule: Take with water after lunch".

BOTTOM NAVIGATION BAR: Active tab is "Cabinet".
```

---

### 🟢 SCREEN 7: Caregiver Audit Screen & Intake History Log

```text
[PROMPT FOR STITCH - SCREEN 7: CAREGIVER AUDIT LOG]
Design the Caregiver Audit & Medication Compliance Log screen for MedVoice on #0B0F17 canvas.

HEADER:
- Left: History checklist icon in #10B981, Title "Caregiver Audit Log", Subtitle "Patient: Dadi (आजी)".
- Right: Trash icon to "Clear Logs".

SAFETY GUARDRAIL CARD:
- #1E293B container with Green Shield icon.
- Text: "100% On-Device Edge Safety Active • Direct Cellular SOS connected to +91 98765 43210".

TIMELINE AUDIT CARDS:
- Entry 1 (Success): #151D2A card.
  - Left: Green Checkmark icon.
  - Middle: "Glycomet-SR 500", Badges: [✓ TAKEN] [🎤 Voice Confirmed].
  - Right: Timestamp "08:30 AM Today".

- Entry 2 (Blocked Duplicate): #151D2A card with 1px #EF4444 red accent.
  - Left: Red Warning icon.
  - Middle: "Gluconorm-SR 500", Badges: [🚫 BLOCKED - DUPLICATE] [🚨 SOS SMS Sent].
  - Right: Timestamp "09:12 AM Today".

- Entry 3 (Success): #151D2A card.
  - Left: Green Checkmark icon.
  - Middle: "Thyronorm 50mcg", Badges: [✓ TAKEN] [🎤 Voice Confirmed].
  - Right: Timestamp "06:45 AM Today".

PRIMARY ACTION BUTTON:
- A balanced 50dp #10B981 emerald button at the bottom: Camera icon with text "Open Camera Scanner".
```

---

### 🟢 SCREEN 8: Settings & Vernacular Voice Studio

```text
[PROMPT FOR STITCH - SCREEN 8: SETTINGS & VOICE STUDIO]
Design the Settings & Voice Studio configuration screen for MedVoice on #0B0F17 canvas.

HEADER:
- Title "Settings & Voice Studio" in 22sp Bold White, Subtitle "Configure vernacular speech, Caregiver SOS & MedGemma AI".

CARD 1: APP LANGUAGE:
- 2 equal buttons: [English (Active #10B981)] | [हिंदी (Hindi - #1E293B)]

CARD 2: VERNACULAR VOICE STUDIO:
- Voice Gender: [Female (Warm & Gentle) - Active #10B981] | [Male (Clear & Resonant) - #1E293B]
- Speech Rate: [0.75x Slow] | [0.88x Senior Clarity (Active)] | [1.0x Normal]
- TTS Provider Selection:
  - 3 Tabs: [On-Device Offline (Active)] | [Sarvam AI (Bulbul)] | [ElevenLabs]
- Bottom Buttons:
  - Cyan button: "▶ Test Voice (आवाज सुनें)"
  - Slate button: "⚙ System TTS Settings"

CARD 3: MEDGEMMA MEDICAL AI ENGINE:
- Title: "MedGemma Medical AI Model" with Shield glyph.
- Tier Selector:
  - [⚡ On-Device INT4 (LiteRT) - Active #10B981]
  - [☁️ Cloud MedGemma (Vertex / Private Endpoint) - #1E293B]
- Privacy Guardrail Row:
  - Text: "Zero Cloud Data Egress (100% On-Device Only)" with an Active Green Switch.

CARD 4: CAREGIVER EMERGENCY SOS DETAILS:
- Patient Name: Clean outlined textfield with label "Patient Name (रोगी का नाम)" containing value "Dadi (आजी)".
- Caregiver Mobile: Clean outlined textfield with label "Caregiver Mobile (फोन नंबर)" containing value "+91 98765 43210".
- Save Button: Full-width #10B981 emerald button with Save icon and text "Save Caregiver Details".
```

---

## 🚀 SECTION 3: One-Shot Master Canvas Prompt (All Screens in One)

If you want Google Stitch to generate the **entire cohesive MedVoice app flow in a single master canvas**, copy and paste this:

```text
[ONE-SHOT MASTER CANVAS PROMPT FOR GOOGLE STITCH]
Create an ultra-premium, cohesive 8-screen UI design flow for an Android app called "MedVoice" (An On-Device Medication Safety & Vernacular Voice Assistant for Indian Seniors & Caregivers).

GLOBAL THEME:
Dark Medical Slate (#0B0F17 background, #151D2A cards, #1E293B elevated, #10B981 vibrant emerald green primary, #06B6D4 cyan accents, #EF4444 alert red). Typography is Inter/Noto Sans Devanagari with large senior-friendly 48dp+ touch targets and high-contrast WCAG AAA compliance.

RENDER THE FOLLOWING 8 SCREENS SIDE-BY-SIDE IN A UNIFIED DESIGN SYSTEM:
1. ONBOARDING WIZARD: Step 1 of 3 language and voice speed selection (English/Hindi pills, 0.88x speed, audio preview button).
2. HOME DASHBOARD: "MV" logo badge, "Good Afternoon Dadi (आजी)", "Point & Scan Blister Pack" hero card, dynamic prescription timeline (Glycomet, Thyronorm, Pan 40 with food timing rules), and Caregiver SOS bar.
3. SCANNER ACTIVE RETICLE: Camera overlay with glowing cyan reticle, flash toggle, quick-test scenario chips (Glycomet, Gluconorm trap, Thyronorm rule, Combiflam conflict).
4. SCANNER SAFE VERIFICATION: Scanned Glycomet-SR 500, solid emerald green container, active salt Metformin 500mg, spoken Hindi audio transcript box, and giant white "✓ Confirm Taken (ले ली)" button.
5. SCANNER DUPLICATE TRAP BLOCKED: High-alert red container for Gluconorm-SR 500, warning "You already took Metformin 42 mins ago", audio warning text, yellow "SMS alert sent to Caregiver", and "Scan Next" button.
6. MEDICINE CABINET: Search bar, active medicine cards with therapeutic class tags, food timing banners, and green speaker buttons to read dosage aloud.
7. CAREGIVER AUDIT LOG: Intake timeline with [TAKEN] green badges, [BLOCKED] red badges, voice confirmed icons, SOS SMS sent indicators, and "Open Camera Scanner" button.
8. SETTINGS & VOICE STUDIO: Language toggle, Voice Gender (Female/Male), Speech rate (0.75x, 0.88x, 1.0x), TTS Provider (On-Device, Sarvam AI, ElevenLabs), MedGemma INT4 vs Cloud tier toggle, Privacy Guardrail switch, and Caregiver emergency contact fields.
```
