#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from off_parquet import (
    PARQUET_COLUMNS,
    country_tags,
    image_small_url,
    load_dataset,
    normalize_text,
    nutriment_map,
    nutriment_value,
    parse_bool_arg,
    pick_localized_text,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extract a slim Meili import JSON from an OpenFoodFacts parquet dump."
    )
    parser.add_argument("--input", default="food.parquet", help="Path to the parquet file.")
    parser.add_argument(
        "--output",
        default="data/seed/foods.de.cleaned.json",
        help="Path to the generated JSON file.",
    )
    parser.add_argument(
        "--country",
        default="Germany",
        help="Country filter, matched against OFF country tags. Defaults to Germany.",
    )
    parser.add_argument(
        "--min-completeness",
        type=float,
        default=0.2,
        help="Minimum OFF completeness score. Defaults to 0.2.",
    )
    parser.add_argument(
        "--require-image",
        default="false",
        help="Only keep products with a front image. true/false.",
    )
    parser.add_argument(
        "--require-full-macros",
        default="true",
        help="Require kcal, protein, fat and carbs. true/false.",
    )
    parser.add_argument("--limit", type=int, help="Optional export limit.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    require_image = parse_bool_arg(args.require_image, False)
    require_full_macros = parse_bool_arg(args.require_full_macros, True)
    country_tag = f"en:{args.country.strip().lower()}" if args.country else None

    dataset = load_dataset(args.input)

    stats = {
        "total": 0,
        "exported": 0,
        "rejected_missing_name": 0,
        "rejected_missing_code": 0,
        "rejected_missing_calories": 0,
        "rejected_missing_macros": 0,
        "rejected_completeness": 0,
        "rejected_country": 0,
        "rejected_image": 0,
    }

    records: list[dict[str, object]] = []

    for batch in dataset.to_batches(columns=PARQUET_COLUMNS):
        for row in batch.to_pylist():
            stats["total"] += 1

            barcode = normalize_text(row.get("code"))
            if not barcode:
                stats["rejected_missing_code"] += 1
                continue

            name = pick_localized_text(row.get("product_name"))
            if not name:
                stats["rejected_missing_name"] += 1
                continue

            completeness_raw = row.get("completeness")
            completeness = float(completeness_raw) if completeness_raw is not None else 0.0
            if completeness < args.min_completeness:
                stats["rejected_completeness"] += 1
                continue

            if country_tag and country_tag not in country_tags(row):
                stats["rejected_country"] += 1
                continue

            image_url = image_small_url(row)
            if require_image and not image_url:
                stats["rejected_image"] += 1
                continue

            nutrients = nutriment_map(row)
            calories = nutriment_value(nutrients, "energy-kcal")
            protein = nutriment_value(nutrients, "proteins")
            fat = nutriment_value(nutrients, "fat")
            carbs = nutriment_value(nutrients, "carbohydrates")

            if calories is None:
                stats["rejected_missing_calories"] += 1
                continue

            if require_full_macros and (protein is None or fat is None or carbs is None):
                stats["rejected_missing_macros"] += 1
                continue

            record = {
                "id": f"off-{barcode}",
                "barcode": barcode,
                "name": name,
                "brand": normalize_text(row.get("brands")) or None,
                "caloriesPer100g": calories,
                "proteinPer100g": 0.0 if protein is None else protein,
                "fatPer100g": 0.0 if fat is None else fat,
                "carbsPer100g": 0.0 if carbs is None else carbs,
                "servingSize": normalize_text(row.get("serving_size")) or None,
                "servingQuantity": None if not normalize_text(row.get("serving_quantity")) else float(normalize_text(row.get("serving_quantity"))),
                "packageSize": normalize_text(row.get("quantity")) or None,
                "packageQuantity": None if not normalize_text(row.get("product_quantity")) else float(normalize_text(row.get("product_quantity"))),
            }
            records.append(record)
            stats["exported"] += 1

            if args.limit and len(records) >= args.limit:
                break
        if args.limit and len(records) >= args.limit:
            break

    output_path = Path(args.output).expanduser().resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(records, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    print(f"Input rows: {stats['total']}")
    print(f"Exported rows: {stats['exported']}")
    print(f"Rejected missing name: {stats['rejected_missing_name']}")
    print(f"Rejected missing code: {stats['rejected_missing_code']}")
    print(f"Rejected missing calories: {stats['rejected_missing_calories']}")
    print(f"Rejected missing macros: {stats['rejected_missing_macros']}")
    print(f"Rejected completeness: {stats['rejected_completeness']}")
    print(f"Rejected country: {stats['rejected_country']}")
    print(f"Rejected image: {stats['rejected_image']}")
    print(f"Output file: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
