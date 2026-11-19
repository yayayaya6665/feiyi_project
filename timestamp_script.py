#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
File Timestamp Batch Modifier
Target: D:\SOFTWARE\test\3_feiyi\
Rules:
  CreationTime:     ALL files -> 2026-03-03 + random time (10:00-23:00)
  LastWriteTime:    before 2026-03-03 -> 2026-03-04 + random time
                    other -> unchanged
  LastAccessTime:   same as LastWriteTime
"""

import os
import sys
import random
import datetime
import ctypes
from ctypes import wintypes

# ============================================================
# Windows API for setting file creation time
# ============================================================
kernel32 = ctypes.windll.kernel32

def windows_set_file_times(filepath, creation_ts, access_ts, write_ts):
    """Set all three file timestamps on Windows using kernel32.SetFileTime"""
    EPOCH_FILETIME = 116444736000000000  # Jan 1, 1601 as 100-ns FILETIME

    def ts_to_filetime(ts):
        return int(ts * 10000000) + EPOCH_FILETIME

    c_ft = ts_to_filetime(creation_ts)
    a_ft = ts_to_filetime(access_ts)
    w_ft = ts_to_filetime(write_ts)

    handle = kernel32.CreateFileW(
        filepath,
        0x100 | 0x80,  # GENERIC_WRITE | FILE_READ_ATTRIBUTES (need write attrs access)
        0x7,            # FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE
        None,           # Security
        3,              # OPEN_EXISTING
        0x02000000,     # FILE_FLAG_BACKUP_SEMANTICS (needed for setting times)
        None
    )

    if handle == -1 or handle == 0xFFFFFFFF:
        return False

    c = ctypes.c_ulonglong(c_ft)
    a = ctypes.c_ulonglong(a_ft)
    w = ctypes.c_ulonglong(w_ft)

    result = kernel32.SetFileTime(
        ctypes.c_void_p(handle),
        ctypes.byref(c),   # lpCreationTime
        ctypes.byref(a),   # lpLastAccessTime
        ctypes.byref(w)    # lpLastWriteTime
    )

    kernel32.CloseHandle(ctypes.c_void_p(handle))
    return result != 0


# ============================================================
# Configuration
# ============================================================
TARGET_DIR = r"D:\SOFTWARE\test\3_feiyi"
REPORT_PATH = os.path.join(TARGET_DIR, "timestamp_preview_report.txt")

CREATION_DATE = datetime.date(2026, 3, 3)
MODIFIED_DATE = datetime.date(2026, 3, 4)
CUTOFF_DATE = datetime.date(2026, 3, 3)
RANGE_START = datetime.date(2026, 6, 1)
RANGE_END = datetime.date(2026, 7, 9)

# ============================================================
# Helpers
# ============================================================
def random_time():
    """Random time: hour 10-23, minute 0-59, second 0-59. Avoid xx:00:00"""
    h = random.randint(10, 23)
    mi = random.randint(0, 59)
    s = random.randint(0, 59)
    if mi == 0 and s == 0:
        mi = random.randint(1, 59)
        s = random.randint(1, 59)
    return (h, mi, s)

def make_ts(date_obj, h, mi, s):
    dt = datetime.datetime(date_obj.year, date_obj.month, date_obj.day, h, mi, s)
    return dt.timestamp()

def needs_change(dt):
    """True if datetime is before 2026-03-03"""
    if isinstance(dt, datetime.datetime):
        return dt.date() < CUTOFF_DATE
    if isinstance(dt, float) or isinstance(dt, int):
        return datetime.datetime.fromtimestamp(dt).date() < CUTOFF_DATE
    return False

def fmt_ts(ts):
    return datetime.datetime.fromtimestamp(ts).strftime("%Y-%m-%d %H:%M:%S")

def fmt_pair(old_ts, new_ts, unchanged=False):
    if unchanged:
        return f"{fmt_ts(old_ts)} -> UNCHANGED"
    return f"{fmt_ts(old_ts)} -> {fmt_ts(new_ts)}"

# ============================================================
# Phase 1 & 2: Scan + Classify
# ============================================================
EXECUTE = "--execute" in sys.argv

print("=" * 60)
print("  File Timestamp Modification" + (" - EXECUTING" if EXECUTE else " - PREVIEW"))
print("=" * 60)
print(f"  Target: {TARGET_DIR}")
print()

print("Scanning files...")
all_files = []
for root, dirs, files in os.walk(TARGET_DIR):
    for name in files:
        all_files.append(os.path.join(root, name))

total = len(all_files)
print(f"Found {total} files")
print()

# Stats
change_ct = 0
change_mt = 0
change_at = 0
before_cutoff = 0
middle_range = 0
in_range = 0
after_range = 0

for fp in all_files:
    try:
        st = os.stat(fp)
    except:
        continue
    change_ct += 1  # All creation times change

    mdate = datetime.datetime.fromtimestamp(st.st_mtime).date()
    adate = datetime.datetime.fromtimestamp(st.st_atime).date()

    if mdate < CUTOFF_DATE:
        before_cutoff += 1
        change_mt += 1
    elif CUTOFF_DATE <= mdate < RANGE_START:
        middle_range += 1
    elif RANGE_START <= mdate <= RANGE_END:
        in_range += 1
    else:
        after_range += 1

    if adate < CUTOFF_DATE:
        change_at += 1

# ============================================================
# Phase 3: Preview Report
# ============================================================
lines = []
lines.append("=" * 60)
lines.append("  PREVIEW REPORT")
lines.append("=" * 60)
lines.append(f"  Total files:               {total}")
lines.append(f"  CreationTime to modify:    {change_ct}  (ALL -> 2026-03-03)")
lines.append(f"  LastWriteTime to modify:   {change_mt}   (before 2026-03-03 -> 2026-03-04)")
lines.append(f"  LastAccessTime to modify:  {change_at}   (before 2026-03-03 -> 2026-03-04)")
lines.append("")
lines.append("  [Modification Time Distribution]")
lines.append(f"  Before 2026-03-03:         {before_cutoff}  -> CHANGE to 2026-03-04")
lines.append(f"  2026-03-03 ~ 2026-05-31:   {middle_range}  -> UNCHANGED")
lines.append(f"  2026-06-01 ~ 2026-07-09:   {in_range}  -> UNCHANGED")
lines.append(f"  After 2026-07-09:          {after_range}  -> UNCHANGED")
lines.append("")
lines.append("  [First 20 Examples (Before -> After)]")
lines.append(f"  {'Path':<55} {'CreationTime':<30} {'LastWriteTime':<30} {'LastAccessTime':<30}")
lines.append("-" * 150)

for i, fp in enumerate(all_files[:20]):
    rel = fp.replace(TARGET_DIR, ".")
    if len(rel) > 52:
        rel = "..." + rel[-49:]

    try:
        st = os.stat(fp)
    except:
        continue

    old_ct = st.st_ctime
    old_mt = st.st_mtime
    old_at = st.st_atime

    h, mi, s = random_time()
    new_ct = make_ts(CREATION_DATE, h, mi, s)

    nc_mt = needs_change(st.st_mtime)
    if nc_mt:
        h, mi, s = random_time()
        new_mt = make_ts(MODIFIED_DATE, h, mi, s)
    else:
        new_mt = old_mt

    nc_at = needs_change(st.st_atime)
    if nc_at:
        h, mi, s = random_time()
        new_at = make_ts(MODIFIED_DATE, h, mi, s)
    else:
        new_at = old_at

    ct_s = fmt_pair(old_ct, new_ct)
    mt_s = fmt_pair(old_mt, new_mt, unchanged=not nc_mt)
    at_s = fmt_pair(old_at, new_at, unchanged=not nc_at)

    lines.append(f"  {rel:<55} {ct_s:<30} {mt_s:<30} {at_s:<30}")

lines.append("")
lines.append("=" * 60)
if not EXECUTE:
    lines.append("  THIS IS PREVIEW ONLY - No changes were made")
    lines.append("  To execute: python timestamp_script.py --execute")
lines.append("=" * 60)

for ln in lines:
    print(ln)

with open(REPORT_PATH, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print(f"\nReport saved: {REPORT_PATH}")

# ============================================================
# Phase 4: Execute
# ============================================================
if not EXECUTE:
    print()
    print(">>> PREVIEW MODE - No changes made <<<")
    print('>>> To execute: python "d:/SOFTWARE/test/3_feiyi/timestamp_script.py" --execute')
    sys.exit(0)

print()
print("=" * 60)
print("  EXECUTING TIMESTAMP CHANGES...")
print("=" * 60)

processed = 0
errors = 0
for idx, fp in enumerate(all_files):
    if (idx + 1) % 500 == 0:
        print(f"  Progress: {idx + 1}/{total}")

    try:
        st = os.stat(fp)
    except:
        errors += 1
        continue

    old_mt = st.st_mtime
    old_at = st.st_atime

    # 1) Creation time: ALL -> 2026-03-03 random
    h, mi, s = random_time()
    new_ct = make_ts(CREATION_DATE, h, mi, s)

    # 2) Modified time
    if needs_change(old_mt):
        h, mi, s = random_time()
        new_mt = make_ts(MODIFIED_DATE, h, mi, s)
    else:
        new_mt = old_mt

    # 3) Access time
    if needs_change(old_at):
        h, mi, s = random_time()
        new_at = make_ts(MODIFIED_DATE, h, mi, s)
    else:
        new_at = old_at

    try:
        ok = windows_set_file_times(fp, new_ct, new_at, new_mt)
        if not ok:
            errors += 1
    except:
        errors += 1

    processed += 1

print()
print("=" * 60)
print(f"  COMPLETE! {processed} files processed, {errors} errors")
print("=" * 60)
