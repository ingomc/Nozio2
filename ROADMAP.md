# Nozio Roadmap

## 2. Barcode Scanner harden
- Scan overlay + clear target frame (focus area in camera preview).
- Duplicate-scan protection and cooldown to avoid double triggers.
- Better error handling (camera denied, no barcode found, network/API fail).
- Optional haptic feedback on successful scan.

## 3. Search UX polish
- Replace temporary success snackbar with Material You top banner.
- Add animated timer/progress bar and auto-dismiss behavior.
- Keep keyboard/insets behavior stable across devices and orientations.
- Improve empty/error states and result transition animations.

## 4. Portion defaults from API
- Extend OpenFoodFacts mapping with portion metadata (`serving_size`, `serving_quantity` when available).
- Preselect amount in add-food sheet from API default portion instead of static `100g`.
- Fallback strategy:
  - If portion in grams exists -> use it.
  - If only textual serving size exists -> parse best effort.
  - If no valid portion is available -> fallback to `100g`.
- Optional: store normalized default portion in local DB to avoid repeated parsing.
