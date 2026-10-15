#!/usr/bin/env python3
"""Check language registration, translations, and bundled artwork references."""

import json
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

print(f"Checked {len(files)} languages and {len(artworks)} artwork entries.")
