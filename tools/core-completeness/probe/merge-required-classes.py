"""Restore <RequiredClasses> values from the canonical DefaultDataFiles cache into a
runtime ItemCache.xml copy by item id, preserving BOM/CRLF and all existing content.

Usage: merge-required-classes.py <canonical ItemCache.xml> <runtime ItemCache.xml>
The runtime file is updated in place (the probes run on a temporary copy).
"""
import sys
import xml.etree.ElementTree as ET

canonical_path, runtime_path = sys.argv[1], sys.argv[2]

canonical = {}
for item in ET.parse(canonical_path).getroot().findall('Item'):
    iid = item.findtext('Id')
    req = (item.findtext('RequiredClasses') or '').strip()
    if iid and req:
        canonical[int(iid)] = req

with open(runtime_path, 'r', encoding='utf-8-sig', newline='') as fh:
    lines = fh.readlines()

out = []
current_id = None
has_required = False
type_index = None
inserted = 0
missing_item_id = 0

for line in lines:
    stripped = line.strip()
    if stripped == '<Item>':
        current_id = None
        has_required = False
        type_index = None
    elif stripped.startswith('<Id>'):
        current_id = int(stripped[len('<Id>'):-len('</Id>')])
    elif stripped.startswith('<RequiredClasses>'):
        has_required = True
    elif stripped.startswith('<Type>'):
        type_index = len(out)

    if stripped == '</Item>' and current_id is not None and not has_required:
        req = canonical.get(current_id)
        if req:
            if type_index is None:
                raise SystemExit(f'item {current_id} has no <Type> line to insert after')
            out.insert(type_index + 1, f'    <RequiredClasses>{req}</RequiredClasses>\r\n')
            inserted += 1
        elif current_id in canonical:
            pass
        else:
            missing_item_id += 1

    out.append(line)

with open(runtime_path, 'w', encoding='utf-8-sig', newline='') as fh:
    fh.writelines(out)

print(f'inserted {inserted} RequiredClasses entries; {missing_item_id} runtime items absent from canonical')
