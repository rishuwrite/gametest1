# Colorbound — Prism Grove

Colorbound is an original Android logic-puzzle game built around four constraints:

1. Exactly one item in every row.
2. Exactly one item in every column.
3. Exactly one item in every connected color region.
4. Items may not touch orthogonally; diagonal touching is allowed.

The visual identity, UI, symbols, palette, and interaction model are independently designed and do not reproduce the supplied reference game's artwork or branding.

## Fixed campaign

- Exactly **1,000 fixed levels**.
- Grid sizes from **8×8 through 15×15** are supported.
- Level data is shipped under `app/src/main/assets/levels/`.
- The game never generates a different puzzle at runtime.
- Every shipped level is validated by `level-generator/validate_levels.py`.
- Fixed starting clues are part of the puzzle state and are included in the uniqueness check.

## Game systems

- Three mistakes per level.
- Candidate marks on single tap; confirmation on double tap.
- Item Finder hint.
- State-aware Logic Idea hint.
- Three-Cell Reveal hint.
- Persistent offline progress.
- Level garden with 1,000 levels.
- Colorblind and high-contrast settings.

## Validation

From the repository root:

```bash
python3 level-generator/validate_levels.py
```

Expected result:

```text
1000 / 1000 LEVELS VALID
Color Connectivity: PASS
Region Item Count: PASS
Row Constraint: PASS
Column Constraint: PASS
No-Touch Constraint: PASS
Solution Validity: PASS
Solution Uniqueness (including fixed starting clues): PASS
Data Integrity: PASS
```

## Android build

The project uses Kotlin, Jetpack Compose, and Material 3. GitHub Actions installs Gradle 8.9 and JDK 17, validates all levels, runs unit tests, and builds a debug APK artifact.

The CI workflow is `.github/workflows/build-apk.yml`.

## Development generator

`generate_levels_fast.py` and `generate_large.py` are development-time tools. They are not used by the Android app at runtime. The exported JSON files are the shipped campaign data.
