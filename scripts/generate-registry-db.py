#!/usr/bin/env python3
import argparse
import gzip
import json
import re
import sqlite3
import tempfile
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT_DIR / "app/src/main/assets/registry.db.compressed"
DEFAULT_SCHEMA = ROOT_DIR / "app/schemas/com.shapeshed.aerial.data.RegistryDatabase/1.json"

WORD_TO_DIGIT = {
    "one": "1", "un": "1", "une": "1", "uno": "1", "una": "1",
    "ein": "1", "eine": "1", "um": "1", "uma": "1",
    "two": "2", "deux": "2", "zwei": "2", "dos": "2", "due": "2", "dois": "2", "duas": "2",
    "three": "3", "trois": "3", "drei": "3", "tres": "3", "tre": "3", "tr\u00eas": "3",
    "four": "4", "quatre": "4", "vier": "4", "cuatro": "4", "quattro": "4", "quatro": "4",
    "five": "5", "cinq": "5", "f\u00fcnf": "5", "cinco": "5", "cinque": "5",
    "six": "6", "sechs": "6", "seis": "6", "sei": "6",
    "seven": "7", "sept": "7", "sieben": "7", "siete": "7", "sette": "7", "sete": "7",
    "eight": "8", "huit": "8", "acht": "8", "ocho": "8", "otto": "8", "oito": "8",
    "nine": "9", "neuf": "9", "neun": "9", "nueve": "9", "nove": "9",
    "ten": "10", "dix": "10", "zehn": "10", "diez": "10", "dieci": "10", "dez": "10",
    "eleven": "11", "onze": "11", "elf": "11", "once": "11", "undici": "11",
    "twelve": "12", "douze": "12", "zw\u00f6lf": "12", "doce": "12", "dodici": "12", "doze": "12",
    "thirteen": "13", "treize": "13", "dreizehn": "13", "trece": "13", "tredici": "13", "treze": "13",
    "fourteen": "14", "quatorze": "14", "vierzehn": "14", "catorce": "14", "quattordici": "14", "catorze": "14",
    "fifteen": "15", "quinze": "15", "f\u00fcnfzehn": "15", "quince": "15", "quindici": "15",
    "sixteen": "16", "seize": "16", "sechzehn": "16", "sedici": "16", "dezasseis": "16", "dezesseis": "16",
    "seventeen": "17", "dix-sept": "17", "siebzehn": "17", "diciassette": "17", "dezassete": "17", "dezessete": "17",
    "eighteen": "18", "dix-huit": "18", "achtzehn": "18", "dieciocho": "18", "diciotto": "18", "dezoito": "18",
    "nineteen": "19", "dix-neuf": "19", "neunzehn": "19", "diecinueve": "19", "diciannove": "19", "dezenove": "19",
    "twenty": "20", "vingt": "20", "zwanzig": "20", "veinte": "20", "venti": "20", "vinte": "20",
}


def normalize_numbers(text):
    words = []
    for word in text.split(" "):
        stripped = word.rstrip(",.-;:!")
        punctuation = word[len(stripped):]
        words.append(WORD_TO_DIGIT.get(stripped.lower(), stripped) + punctuation)
    return " ".join(words)


def read_registry(path):
    data = Path(path).read_bytes()
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    return json.loads(data.decode("utf-8"))


def read_identity_hash(path):
    schema = json.loads(Path(path).read_text(encoding="utf-8"))
    identity_hash = schema.get("database", {}).get("identityHash")
    if not identity_hash:
        raise SystemExit(f"Room identity hash not found in {path}")
    return identity_hash


def clean(value):
    return str(value or "").strip()


def tags_text(value):
    if isinstance(value, list):
        return " ".join(clean(item) for item in value if clean(item))
    return clean(value)


def station_rows(items):
    for item in items:
        name = clean(item.get("name"))
        stream_url = clean(item.get("stream_url"))
        if not name or not stream_url:
            continue
        tags = tags_text(item.get("tags"))
        yield (
            name,
            stream_url,
            clean(item.get("logo_url")),
            clean(item.get("country")),
            clean(item.get("country_code")),
            tags,
            clean(item.get("provider")),
            clean(item.get("provider_id")),
            clean(item.get("description")),
            normalize_numbers(f"{name} {tags}"),
        )


def create_database(output_path, rows, identity_hash):
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(prefix="registry-", suffix=".db", dir=output_path.parent, delete=False) as tmp:
        tmp_path = Path(tmp.name)

    try:
        with sqlite3.connect(tmp_path) as db:
            db.executescript(
                """
                PRAGMA journal_mode=OFF;
                PRAGMA synchronous=OFF;

                CREATE TABLE IF NOT EXISTS `registry_stations` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  `name` TEXT NOT NULL,
                  `streamUrl` TEXT NOT NULL,
                  `logoUrl` TEXT NOT NULL,
                  `country` TEXT NOT NULL,
                  `countryCode` TEXT NOT NULL,
                  `tags` TEXT NOT NULL,
                  `provider` TEXT NOT NULL,
                  `providerId` TEXT NOT NULL,
                  `description` TEXT NOT NULL,
                  `searchText` TEXT NOT NULL
                );

                CREATE INDEX IF NOT EXISTS `index_registry_stations_countryCode`
                  ON `registry_stations` (`countryCode`);
                CREATE INDEX IF NOT EXISTS `index_registry_stations_provider_providerId`
                  ON `registry_stations` (`provider`, `providerId`);
                CREATE INDEX IF NOT EXISTS `index_registry_stations_providerId`
                  ON `registry_stations` (`providerId`);
                CREATE INDEX IF NOT EXISTS `index_registry_stations_streamUrl`
                  ON `registry_stations` (`streamUrl`);

                CREATE VIRTUAL TABLE IF NOT EXISTS `registry_stations_fts` USING FTS4(
                  `searchText` TEXT NOT NULL,
                  `description` TEXT NOT NULL,
                  `country` TEXT NOT NULL,
                  tokenize=unicode61 `remove_diacritics=1`,
                  content=`registry_stations`
                );

                CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_registry_stations_fts_BEFORE_UPDATE
                  BEFORE UPDATE ON `registry_stations`
                  BEGIN DELETE FROM `registry_stations_fts` WHERE `docid`=OLD.`rowid`; END;
                CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_registry_stations_fts_BEFORE_DELETE
                  BEFORE DELETE ON `registry_stations`
                  BEGIN DELETE FROM `registry_stations_fts` WHERE `docid`=OLD.`rowid`; END;
                CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_registry_stations_fts_AFTER_UPDATE
                  AFTER UPDATE ON `registry_stations`
                  BEGIN INSERT INTO `registry_stations_fts`(`docid`, `searchText`, `description`, `country`)
                  VALUES (NEW.`rowid`, NEW.`searchText`, NEW.`description`, NEW.`country`); END;
                CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_registry_stations_fts_AFTER_INSERT
                  AFTER INSERT ON `registry_stations`
                  BEGIN INSERT INTO `registry_stations_fts`(`docid`, `searchText`, `description`, `country`)
                  VALUES (NEW.`rowid`, NEW.`searchText`, NEW.`description`, NEW.`country`); END;

                CREATE TABLE IF NOT EXISTS room_master_table (
                  id INTEGER PRIMARY KEY,
                  identity_hash TEXT
                );
                """
            )
            db.executemany(
                """
                INSERT INTO `registry_stations` (
                  `name`, `streamUrl`, `logoUrl`, `country`, `countryCode`, `tags`,
                  `provider`, `providerId`, `description`, `searchText`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                rows,
            )
            db.execute("INSERT INTO `registry_stations_fts`(`registry_stations_fts`) VALUES('rebuild')")
            db.execute(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                (identity_hash,),
            )
            db.execute("PRAGMA user_version = 1")
            db.commit()
            db.execute("VACUUM")
        if output_path.suffix in {".compressed", ".gz"}:
            with tmp_path.open("rb") as source, gzip.open(output_path, "wb", compresslevel=9) as target:
                target.writelines(source)
            tmp_path.unlink()
        else:
            tmp_path.replace(output_path)
    except Exception:
        tmp_path.unlink(missing_ok=True)
        raise


def main():
    parser = argparse.ArgumentParser(description="Generate the bundled Aerial registry SQLite database.")
    parser.add_argument("--input", required=True, help="registry JSON or JSON.GZ input")
    parser.add_argument("--output", default=DEFAULT_OUTPUT, help="SQLite database output")
    parser.add_argument("--schema", default=DEFAULT_SCHEMA, help="Room RegistryDatabase schema JSON")
    args = parser.parse_args()

    registry = read_registry(args.input)
    if not isinstance(registry, list):
        raise SystemExit("Registry JSON must be an array")
    rows = list(station_rows(registry))
    if not rows:
        raise SystemExit("Registry JSON contained no valid stations")

    create_database(args.output, rows, read_identity_hash(args.schema))
    print(f"Wrote {args.output} with {len(rows)} stations.")


if __name__ == "__main__":
    main()
