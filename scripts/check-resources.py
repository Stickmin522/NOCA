#!/usr/bin/env python3
"""Check language registration, translations, and bundled artwork references."""

import json
import csv
import pathlib
import xml.etree.ElementTree as ET

root = pathlib.Path(__file__).resolve().parents[1]
assets = root / "app/src/main/assets"
languages = assets / "languages"
index = json.loads((languages / "index.json").read_text(encoding="utf-8"))
english = json.loads((languages / "en.json").read_text(encoding="utf-8"))
files = {p.stem for p in languages.glob("*.json") if p.stem != "index"}
assert files == set(index), "Language index does not match bundled translations"
registered = {
    node.attrib["{http://schemas.android.com/apk/res/android}name"]
    for node in ET.parse(root / "app/src/main/res/xml/locales_config.xml").getroot()
}
assert registered == files, "Android locale configuration is out of sync"
for language in files:
    strings = json.loads((languages / f"{language}.json").read_text(encoding="utf-8"))
    assert strings.keys() == english.keys(), f"Incomplete translation: {language}"
    assert all(isinstance(value, str) and value.strip() for value in strings.values()), language
    qualifier = {"he": "values-iw", "id": "values-in"}.get(language, "values-b+" + language.replace("-", "+"))
    resource = root / "app/src/main/res" / qualifier / "strings.xml"
    assert resource.is_file(), f"Missing Android locale resource: {language}"

artworks = json.loads((assets / "cards/catalog.json").read_text(encoding="utf-8"))
assert len({art["id"] for art in artworks}) == len(artworks), "Duplicate artwork ID"
for art in artworks:
    if art.get("file"):
        assert pathlib.Path(art["file"]).name == art["file"], "Invalid artwork path"
        assert (assets / "cards" / art["file"]).is_file(), art["id"]
    if "crop" in art:
        assert len(art["crop"]) == 4 and all(isinstance(n, int) for n in art["crop"])
        assert min(art["crop"]) >= 0 and min(art["crop"][2:]) > 0
    if "notched" in art:
        assert isinstance(art["notched"], bool), "Invalid card edge shape"

with (assets / "stations/stations.tsv").open(encoding="utf-8", newline="") as stream:
    stations = list(csv.DictReader(stream, delimiter="\t"))
keys = set()
for station in stations:
    key = tuple(int(station[column]) for column in ("area", "line", "station"))
    assert key[0] in range(4) and all(n in range(256) for n in key[1:]), key
    assert key not in keys, f"Duplicate station code: {key}"
    keys.add(key)
    assert all(station[column].strip() for column in ("operator", "line_name", "station_name"))
assert len(stations) == 6954, "Station directory is incomplete"
assert (assets / "stations/LICENSE.txt").is_file(), "Missing station data license"

print(f"Checked {len(files)} languages, {len(artworks)} artwork entries, and {len(stations)} station codes.")
