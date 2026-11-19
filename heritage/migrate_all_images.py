#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
全模块图片迁移脚本（跳过已存在的图片）
1. 扫描 sys_file_info 中所有非 SHOP_PRODUCT 的图片记录
2. 对比 vue3/public/image/ 已有文件，只复制缺失的
3. 只更新旧路径的记录，已迁移的跳过
4. 生成完整报告
"""

import os
import shutil
import sys
import csv
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# ========== 配置 ==========
BASE_DIR = r"d:\SOFTWARE\test\3_feiyi"
FILES_DIR = os.path.join(BASE_DIR, "springboot", "files")
TARGET_DIR = os.path.join(BASE_DIR, "vue3", "public", "image")
TSV_FILE = os.path.join(BASE_DIR, "temp_all_data.tsv")
SQL_FILE = os.path.join(BASE_DIR, "update_all_image_paths.sql")
REPORT_FILE = os.path.join(BASE_DIR, "migration_report.txt")

os.makedirs(TARGET_DIR, exist_ok=True)

# ========== 读取导出数据 ==========
records = []
with open(TSV_FILE, "r", encoding="latin-1") as f:
    reader = csv.DictReader(f, delimiter="\t")
    for row in reader:
        records.append(row)

print(f"Total non-SHOP_PRODUCT records: {len(records)}")
print()

# ========== 统计模块 ==========
module_stats = {}
for rec in records:
    bt = rec["business_type"].strip()
    if bt not in module_stats:
        module_stats[bt] = {"total": 0, "already_migrated": 0, "exists_in_target": 0, "copied": 0, "source_missing": 0, "updated_db": 0}
    module_stats[bt]["total"] += 1

# ========== 处理每条记录 ==========
sql_lines = [
    "-- ============================================",
    "-- 全模块图片路径迁移 SQL",
    "-- 自动生成，只更新未迁移的记录",
    "-- ============================================",
    "",
]

report_lines = [
    "=" * 60,
    "  全模块图片迁移报告",
    "=" * 60,
    "",
    f"生成时间: {__import__('datetime').datetime.now()}",
    f"目标目录: {TARGET_DIR}",
    "",
]

copied_count = 0
skipped_exists = 0
source_missing_count = 0
updated_db_count = 0
already_migrated_count = 0

# 先扫描目标目录所有文件
existing_target_files = set(os.listdir(TARGET_DIR))
print(f"Target directory has {len(existing_target_files)} files")
print()

for i, rec in enumerate(records):
    file_id = rec["id"].strip()
    original_name = rec["original_name"].strip()
    file_path = rec["file_path"].strip()
    business_type = rec["business_type"].strip()
    business_id = rec["business_id"].strip()
    business_field = rec["business_field"].strip()

    # 确定扩展名
    _, ext = os.path.splitext(file_path)
    if not ext:
        _, ext = os.path.splitext(original_name)
    if not ext:
        ext = ".jpg"

    # 新文件名: {businessType}_{businessId}_{businessField}_{fileId}.{ext}
    new_filename = f"{business_type}_{business_id}_{business_field}_{file_id}{ext}"
    new_full_path = os.path.join(TARGET_DIR, new_filename)
    new_db_path = f"/image/{new_filename}"

    # === 检查 1: 数据库路径是否已经迁移 ===
    if file_path.startswith("/image/"):
        already_migrated_count += 1
        module_stats[business_type]["already_migrated"] += 1
        # 检查目标文件是否真的存在
        if new_filename in existing_target_files:
            module_stats[business_type]["exists_in_target"] += 1
        continue

    # === 检查 2: 目标文件是否已存在 ===
    if new_filename in existing_target_files:
        skipped_exists += 1
        module_stats[business_type]["exists_in_target"] += 1
        # 数据库路径还没更新 -> 需要更新
        sql_lines.append(f"-- {business_type} / {business_id} / {business_field} (文件已存在,仅更新路径)")
        sql_lines.append(f"UPDATE sys_file_info SET file_path = '{new_db_path}' WHERE id = {file_id};")
        sql_lines.append("")
        updated_db_count += 1
        module_stats[business_type]["updated_db"] += 1
        continue

    # === 检查 3: 源文件是否存在 ===
    src_filename = os.path.basename(file_path)
    # 根据 business_type 推断源目录
    src_dir_name = business_type.lower()
    src_full_path = os.path.join(FILES_DIR, "bussiness", src_dir_name, src_filename)

    # 尝试其他可能的路径
    if not os.path.exists(src_full_path):
        # 可能存储在 commom 目录
        alt_path = os.path.join(FILES_DIR, "bussiness", "commom", src_filename)
        if os.path.exists(alt_path):
            src_full_path = alt_path
        else:
            # 尝试直接查找文件名
            found = False
            for root, dirs, files in os.walk(FILES_DIR):
                if src_filename in files:
                    src_full_path = os.path.join(root, src_filename)
                    found = True
                    break
            if not found:
                source_missing_count += 1
                module_stats[business_type]["source_missing"] += 1
                msg = f"  [MISSING] ID={file_id} | {business_type}/{business_id}/{business_field} | src={src_filename} not found"
                report_lines.append(msg)
                continue

    # === 复制文件 ===
    try:
        shutil.copy2(src_full_path, new_full_path)
        copied_count += 1
        module_stats[business_type]["copied"] += 1
        existing_target_files.add(new_filename)

        # 生成 SQL
        sql_lines.append(f"-- {business_type} / {business_id} / {business_field}")
        sql_lines.append(f"UPDATE sys_file_info SET file_path = '{new_db_path}' WHERE id = {file_id};")
        sql_lines.append("")
        updated_db_count += 1
        module_stats[business_type]["updated_db"] += 1

        msg = f"  [COPY] ID={file_id} | {business_type}/{business_id}/{business_field} | {src_filename} -> {new_filename}"
        report_lines.append(msg)

    except Exception as e:
        source_missing_count += 1
        module_stats[business_type]["source_missing"] += 1
        msg = f"  [ERROR] ID={file_id} | {business_type}/{business_id}/{business_field} | {e}"
        report_lines.append(msg)

# ========== 生成详细报告 ==========
report_lines.append("")
report_lines.append("=" * 60)
report_lines.append("  模块统计")
report_lines.append("=" * 60)
report_lines.append("")
report_lines.append(f"{'模块':<30} {'总数':>6} {'已迁移':>6} {'目标已存在':>10} {'本次复制':>8} {'源缺失':>6} {'更新DB':>6}")
report_lines.append("-" * 80)

total_all = {"total": 0, "already_migrated": 0, "exists_in_target": 0, "copied": 0, "source_missing": 0, "updated_db": 0}
for bt in sorted(module_stats.keys()):
    s = module_stats[bt]
    for k in total_all:
        if k in s:
            total_all[k] += s[k]
    report_lines.append(f"{bt:<30} {s['total']:>6} {s['already_migrated']:>6} {s['exists_in_target']:>10} {s['copied']:>8} {s['source_missing']:>6} {s['updated_db']:>6}")

report_lines.append("-" * 80)
report_lines.append(f"{'TOTAL':<30} {total_all['total']:>6} {total_all['already_migrated']:>6} {total_all['exists_in_target']:>10} {total_all['copied']:>8} {total_all['source_missing']:>6} {total_all['updated_db']:>6}")

report_lines.append("")
report_lines.append("=" * 60)
report_lines.append("  汇总")
report_lines.append("=" * 60)
report_lines.append(f"  总记录数 (非SHOP_PRODUCT): {len(records)}")
report_lines.append(f"  ✅ 已迁移 (DB路径已是/image/):   {already_migrated_count}")
report_lines.append(f"  ✅ 目标文件已存在 (仅更新DB):    {skipped_exists}")
report_lines.append(f"  📥 本次复制成功:                 {copied_count}")
report_lines.append(f"  ❌ 源文件缺失:                   {source_missing_count}")
report_lines.append(f"  📝 需更新数据库的记录:           {updated_db_count}")
report_lines.append("")

# ========== 写入文件 ==========
sql_lines.append(f"-- 共 {updated_db_count} 条 UPDATE 语句")
sql_lines.append("-- 执行此 SQL 以更新数据库路径")

with open(SQL_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

with open(REPORT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(report_lines))

# 打印报告
for line in report_lines:
    print(line)

print(f"\nSQL 文件: {SQL_FILE}")
print(f"报告文件: {REPORT_FILE}")
print(f"\n执行 SQL:")
print(f'  mysql -uroot -pmysql123456 -h127.0.0.1 -P3306 heritage < "{SQL_FILE}"')
