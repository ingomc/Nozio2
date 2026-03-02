from __future__ import annotations

from pathlib import Path


def load_dataset(input_path: str):
    try:
        import pyarrow.dataset as ds
    except ModuleNotFoundError as error:
        raise SystemExit(
            "pyarrow is not installed. Create a venv and install it with "
            "`python3 -m venv .venv-parquet && .venv-parquet/bin/pip install -r scripts/meili-import/requirements-parquet.txt`."
        ) from error

    return ds.dataset(str(Path(input_path).expanduser().resolve()), format="parquet")


def normalize_text(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, bytes):
        return value.decode("utf-8", "replace").strip()
    return str(value).strip()


def parse_bool_arg(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.lower() == "true"


def split_csv_field(value: str) -> list[str]:
    return [entry.strip() for entry in value.split(",") if entry.strip()]


def pick_localized_text(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        for entry in value:
            if isinstance(entry, dict):
                text = normalize_text(entry.get("text"))
                if text:
                    return text
        return ""
    return normalize_text(value)


def country_tags(row: dict[str, object]) -> list[str]:
    return [
        normalize_text(entry).lower()
        for entry in (row.get("countries_tags") or [])
        if normalize_text(entry)
    ]


def brand_entries(value: str) -> list[str]:
    return [entry.lower() for entry in split_csv_field(value)]


def nutriment_map(row: dict[str, object]) -> dict[str, dict[str, object]]:
    nutriments = row.get("nutriments") or []
    mapped: dict[str, dict[str, object]] = {}
    if isinstance(nutriments, list):
        for entry in nutriments:
            if not isinstance(entry, dict):
                continue
            name = normalize_text(entry.get("name")).lower()
            if name:
                mapped[name] = entry
    return mapped


def nutriment_value(nutrients: dict[str, dict[str, object]], name: str) -> float | None:
    entry = nutrients.get(name)
    if not entry:
        return None

    value = entry.get("100g")
    if value is None:
        value = entry.get("value")
    if value is None:
        return None

    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None

    return parsed


def image_small_url(row: dict[str, object]) -> str:
    code = normalize_text(row.get("code"))
    if len(code) < 4:
        return ""

    images = row.get("images") or []
    if not isinstance(images, list):
        return ""

    for image in images:
        if not isinstance(image, dict):
            continue
        key = normalize_text(image.get("key"))
        if not key.startswith("front"):
            continue
        rev = normalize_text(image.get("rev"))
        if not rev:
            continue
        return (
            "https://images.openfoodfacts.org/images/products/"
            f"{code[0:3]}/{code[3:6]}/{code[6:9]}/{code[9:]}/{key}.{rev}.200.jpg"
        )

    return ""


PARQUET_COLUMNS = [
    "code",
    "product_name",
    "brands",
    "countries_tags",
    "quantity",
    "product_quantity",
    "serving_size",
    "serving_quantity",
    "images",
    "nutrition_data_per",
    "nutriments",
    "completeness",
    "last_modified_t",
]
