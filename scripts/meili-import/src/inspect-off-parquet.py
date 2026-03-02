#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

from off_parquet import (
    PARQUET_COLUMNS,
    brand_entries,
    country_tags,
    image_small_url,
    load_dataset,
    normalize_text,
    nutriment_map,
    nutriment_value,
    pick_localized_text,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Inspect OpenFoodFacts parquet data for matching products.")
    parser.add_argument("--input", required=True, help="Path to the parquet file.")
    parser.add_argument("--query", default="Deit", help="Substring to match in product_name or brands.")
    parser.add_argument("--brand-exact", help="Optional exact brand match after splitting comma-separated brands.")
    parser.add_argument("--country", help="Optional country filter against OFF country tags.")
    parser.add_argument("--limit", type=int, default=20, help="Maximum number of matches to export.")
    parser.add_argument("--output-json", default="data/seed/off-parquet-inspect.json", help="Where to write the JSON result sample.")
    parser.add_argument("--output-csv", default="data/seed/off-parquet-inspect.csv", help="Where to write the CSV result sample.")
    return parser.parse_args()


def row_matches(
    row: dict[str, object],
    query: str,
    brand_exact: str | None,
    country: str | None,
) -> bool:
    brand = normalize_text(row.get("brands"))
    name = pick_localized_text(row.get("product_name"))
    brand_lower = brand.lower()
    name_lower = name.lower()

    if query and query not in brand_lower and query not in name_lower:
        return False

    if brand_exact:
        if brand_exact not in brand_entries(brand):
            return False

    if country:
        if country not in country_tags(row):
            return False

    return True


def project_row(row: dict[str, object]) -> dict[str, object]:
    nutrients = nutriment_map(row)

    return {
        "code": normalize_text(row.get("code")),
        "product_name": pick_localized_text(row.get("product_name")),
        "brands": normalize_text(row.get("brands")),
        "countries_en": ",".join(
            normalize_text(entry) for entry in (row.get("countries_tags") or []) if normalize_text(entry)
        ),
        "quantity": normalize_text(row.get("quantity")),
        "product_quantity": normalize_text(row.get("product_quantity")),
        "serving_size": normalize_text(row.get("serving_size")),
        "serving_quantity": normalize_text(row.get("serving_quantity")),
        "energy-kj_100g": normalize_text(nutriment_value(nutrients, "energy-kj")),
        "energy-kcal_100g": normalize_text(nutriment_value(nutrients, "energy-kcal")),
        "fat_100g": normalize_text(nutriment_value(nutrients, "fat")),
        "saturated-fat_100g": normalize_text(nutriment_value(nutrients, "saturated-fat")),
        "carbohydrates_100g": normalize_text(nutriment_value(nutrients, "carbohydrates")),
        "sugars_100g": normalize_text(nutriment_value(nutrients, "sugars")),
        "fiber_100g": normalize_text(nutriment_value(nutrients, "fiber")),
        "proteins_100g": normalize_text(nutriment_value(nutrients, "proteins")),
        "salt_100g": normalize_text(nutriment_value(nutrients, "salt")),
        "sodium_100g": normalize_text(nutriment_value(nutrients, "sodium")),
        "image_small_url": image_small_url(row),
        "image_nutrition_url": "",
        "nutrition_data_prepared_per": "",
        "nutrition_data_per": normalize_text(row.get("nutrition_data_per")),
        "completeness": normalize_text(row.get("completeness")),
        "last_modified_t": normalize_text(row.get("last_modified_t")),
        "last_modified_datetime": "",
    }


def load_matches(args: argparse.Namespace) -> list[dict[str, object]]:
    dataset = load_dataset(args.input)

    query = args.query.strip().lower()
    brand_exact = args.brand_exact.strip().lower() if args.brand_exact else None
    country = f"en:{args.country.strip().lower()}" if args.country else None

    matches: list[dict[str, object]] = []
    for batch in dataset.to_batches(columns=PARQUET_COLUMNS):
        batch_rows = batch.to_pylist()
        for row in batch_rows:
            if not row_matches(row, query, brand_exact, country):
                continue
            matches.append(project_row(row))
            if len(matches) >= args.limit:
                return matches
    return matches


def write_outputs(matches: list[dict[str, object]], json_path: Path, csv_path: Path) -> None:
    json_path.parent.mkdir(parents=True, exist_ok=True)
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    json_path.write_text(json.dumps(matches, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    fieldnames = list(matches[0].keys()) if matches else []
    with csv_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(matches)


def main() -> int:
    args = parse_args()
    matches = load_matches(args)

    output_json = Path(args.output_json).expanduser().resolve()
    output_csv = Path(args.output_csv).expanduser().resolve()
    write_outputs(matches, output_json, output_csv)

    print(f"Matches written: {len(matches)}")
    print(f"JSON output: {output_json}")
    print(f"CSV output: {output_csv}")

    for index, row in enumerate(matches[:3], start=1):
        print(f"ROW {index}")
        print(f"code={row['code']}")
        print(f"product_name={row['product_name']}")
        print(f"brands={row['brands']}")
        print(f"countries_en={row['countries_en']}")
        print(f"energy-kj_100g={row['energy-kj_100g']}")
        print(f"energy-kcal_100g={row['energy-kcal_100g']}")
        print(f"fat_100g={row['fat_100g']}")
        print(f"carbohydrates_100g={row['carbohydrates_100g']}")
        print(f"proteins_100g={row['proteins_100g']}")
        print(f"salt_100g={row['salt_100g']}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
