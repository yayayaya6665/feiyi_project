# ============================================================
# 文件时间属性批量修改脚本
# 目录: D:\SOFTWARE\test\3_feiyi\
# ============================================================
#
# 规则:
#   创建时间(CreationTime):     全部 → 2026-03-03 + 随机时间(10:00-23:00)
#   修改时间(LastWriteTime):    早于2026-03-03 → 2026-03-04 + 随机时间
#                              2026-06-01 ~ 2026-07-09 → 不变
#                              2026-03-03 ~ 2026-05-31 → 不变
#                              其他 → 不变
#   访问时间(LastAccessTime):   规则同修改时间
# ============================================================

param(
    [switch]$Execute  # 不带此参数 = 仅预览；带 -Execute = 真正执行
)

$TargetDir = "D:\SOFTWARE\test\3_feiyi"
$ReportPath = Join-Path $TargetDir "timestamp_preview_report.txt"

# 日期常量
$CreationDate = Get-Date "2026-03-03"
$ModifiedDate = Get-Date "2026-03-04"
$RangeStart   = Get-Date "2026-06-01"
$RangeEnd     = Get-Date "2026-07-09"

# ============================================================
# 辅助函数：生成随机时间 (10:00:00 ~ 23:00:00，避免整点)
# ============================================================
function Get-RandomTime {
    $hour   = Get-Random -Minimum 10 -Maximum 24       # 10 ~ 23
    $minute = Get-Random -Minimum 0  -Maximum 60       # 0 ~ 59
    $second = Get-Random -Minimum 0  -Maximum 60       # 0 ~ 59
    # 如果恰好命中整点 (00分00秒)，重新随机一次
    if ($minute -eq 0 -and $second -eq 0) {
        $minute = Get-Random -Minimum 1 -Maximum 60
        $second = Get-Random -Minimum 1 -Maximum 60
    }
    return [PSCustomObject]@{ Hour = $hour; Minute = $minute; Second = $second }
}

# ============================================================
# 阶段1: 扫描所有文件（不包含目录）
# ============================================================
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  文件时间属性修改 - 预览模式" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "正在扫描 $TargetDir ..." -ForegroundColor Yellow

$AllFiles = Get-ChildItem -Path $TargetDir -File -Recurse -ErrorAction SilentlyContinue
$TotalFiles = $AllFiles.Count

Write-Host "扫描完成，共找到 $TotalFiles 个文件" -ForegroundColor Green
Write-Host ""

# ============================================================
# 阶段2: 分类统计
# ============================================================
$WillChangeCreation    = @()   # 创建时间需修改的文件
$WillChangeModified    = @()   # 修改时间需修改的文件
$WillChangeAccess      = @()   # 访问时间需修改的文件
$KeepModifiedUnchanged = @()   # 修改时间不变的文件
$KeepAccessUnchanged   = @()   # 访问时间不变的文件

foreach ($file in $AllFiles) {
    # --- 创建时间：全部修改 ---
    $WillChangeCreation += $file

    # --- 修改时间判断 ---
    $mtime = $file.LastWriteTime
    $changeModified = $false
    if ($mtime -lt (Get-Date "2026-03-03")) {
        $changeModified = $true
    }
    # (2026-03-03 ~ 2026-05-31 和 2026-06-01 ~ 2026-07-09 以及 2026-07-09 之后都不变)

    if ($changeModified) {
        $WillChangeModified += $file
    } else {
        $KeepModifiedUnchanged += $file
    }

    # --- 访问时间判断（同修改时间） ---
    $atime = $file.LastAccessTime
    $changeAccess = $false
    if ($atime -lt (Get-Date "2026-03-03")) {
        $changeAccess = $true
    }

    if ($changeAccess) {
        $WillChangeAccess += $file
    } else {
        $KeepAccessUnchanged += $file
    }
}

$ChangeCreationCount = $WillChangeCreation.Count
$ChangeModifiedCount = $WillChangeModified.Count
$ChangeAccessCount   = $WillChangeAccess.Count
$KeepModifiedCount   = $KeepModifiedUnchanged.Count
$KeepAccessCount     = $KeepAccessUnchanged.Count

# ============================================================
# 阶段3: 生成预览报告
# ============================================================
$PreviewLines = @()
$PreviewLines += "============================================================"
$PreviewLines += "  文件时间属性修改 - 预览报告"
$PreviewLines += "  生成时间: $(Get-Date)"
$PreviewLines += "  目标目录: $TargetDir"
$PreviewLines += "============================================================"
$PreviewLines += ""
$PreviewLines += "【统计概览】"
$PreviewLines += "  总文件数量:              $TotalFiles"
$PreviewLines += "  创建时间需修改:           $ChangeCreationCount   (全部文件 → 2026-03-03)"
$PreviewLines += "  修改时间需修改:           $ChangeModifiedCount    (早于2026-03-03 → 2026-03-04)"
$PreviewLines += "  修改时间保持不变:         $KeepModifiedCount"
$PreviewLines += "  访问时间需修改:           $ChangeAccessCount      (早于2026-03-03 → 2026-03-04)"
$PreviewLines += "  访问时间保持不变:         $KeepAccessCount"
$PreviewLines += ""

# 修改时间分布统计
$BeforeCount  = ($AllFiles | Where-Object { $_.LastWriteTime -lt (Get-Date "2026-03-03") }).Count
$MiddleCount  = ($AllFiles | Where-Object { $_.LastWriteTime -ge (Get-Date "2026-03-03") -and $_.LastWriteTime -lt (Get-Date "2026-06-01") }).Count
$InRangeCount = ($AllFiles | Where-Object { $_.LastWriteTime -ge (Get-Date "2026-06-01") -and $_.LastWriteTime -le (Get-Date "2026-07-09") }).Count
$AfterCount   = ($AllFiles | Where-Object { $_.LastWriteTime -gt (Get-Date "2026-07-09") }).Count

$PreviewLines += "【修改时间分布】"
$PreviewLines += "  早于 2026-03-03:         $BeforeCount   → 改为 2026-03-04 随机时间"
$PreviewLines += "  2026-03-03 ~ 2026-05-31: $MiddleCount   → 不变"
$PreviewLines += "  2026-06-01 ~ 2026-07-09: $InRangeCount  → 不变"
$PreviewLines += "  2026-07-09 之后:         $AfterCount    → 不变"
$PreviewLines += ""

# 前20个修改示例
$PreviewLines += "【前20个文件示例 (修改前 → 修改后)】"
$PreviewLines += ("{0,-55} {1,-25} {2,-25} {3,-25}" -f "文件路径(相对)", "创建时间", "修改时间", "访问时间")
$PreviewLines += "-" * 155

$sampleCount = [Math]::Min(20, $TotalFiles)
$samples = $AllFiles | Select-Object -First $sampleCount
$sampleIdx = 0

foreach ($file in $samples) {
    $sampleIdx++
    $relPath = $file.FullName.Replace($TargetDir, ".").Replace("\", "/")
    if ($relPath.Length -gt 52) {
        $relPath = "..." + $relPath.Substring($relPath.Length - 49)
    }

    # 新创建时间
    $ct = Get-RandomTime
    $newCT = Get-Date "$CreationDate $($ct.Hour):$($ct.Minute.ToString('00')):$($ct.Second.ToString('00'))"

    # 新修改时间
    $oldMT = $file.LastWriteTime
    if ($oldMT -lt (Get-Date "2026-03-03")) {
        $mt = Get-RandomTime
        $newMT = Get-Date "$ModifiedDate $($mt.Hour):$($mt.Minute.ToString('00')):$($mt.Second.ToString('00'))"
        $mtDisplay = $newMT.ToString("yyyy-MM-dd HH:mm:ss")
    } else {
        $newMT = $oldMT
        $mtDisplay = "不变"
    }

    # 新访问时间
    $oldAT = $file.LastAccessTime
    if ($oldAT -lt (Get-Date "2026-03-03")) {
        $at = Get-RandomTime
        $newAT = Get-Date "$ModifiedDate $($at.Hour):$($at.Minute.ToString('00')):$($at.Second.ToString('00'))"
        $atDisplay = $newAT.ToString("yyyy-MM-dd HH:mm:ss")
    } else {
        $newAT = $oldAT
        $atDisplay = "不变"
    }

    $oldCTDisplay = $file.CreationTime.ToString("yyyy-MM-dd HH:mm:ss")
    $oldMTDisplay = $oldMT.ToString("yyyy-MM-dd HH:mm:ss")
    $oldATDisplay = $oldAT.ToString("yyyy-MM-dd HH:mm:ss")
    $newCTDisplay = $newCT.ToString("yyyy-MM-dd HH:mm:ss")

    $previewCT = "$oldCTDisplay → $newCTDisplay"
    $previewMT = "$oldMTDisplay → $mtDisplay"
    $previewAT = "$oldATDisplay → $atDisplay"

    $PreviewLines += ("{0,-55} {1,-30} {2,-30} {3,-30}" -f $relPath, $previewCT, $previewMT, $previewAT)
}

$PreviewLines += ""
$PreviewLines += "============================================================"
$PreviewLines += "  注意：以上仅为预览，实际时间随机生成，每次运行结果不同"
if (-not $Execute) {
    $PreviewLines += "  要真正执行修改，请运行: .\timestamp_script.ps1 -Execute"
}
$PreviewLines += "============================================================"

# 输出到控制台
foreach ($line in $PreviewLines) {
    Write-Host $line
}

# 保存报告文件
$PreviewLines | Out-File -FilePath $ReportPath -Encoding UTF8
Write-Host ""
Write-Host "预览报告已保存到: $ReportPath" -ForegroundColor Green

# ============================================================
# 阶段4: 真正执行 (仅当 -Execute 参数存在时)
# ============================================================
if ($Execute) {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "  正在执行修改..." -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red

    $processed = 0
    $totalToProcess = $ChangeCreationCount + $ChangeModifiedCount + $ChangeAccessCount

    # 预生成一组随机时间（避免在循环中重复调用 Get-Random）
    $randomTimes = @()
    $maxRandomNeeded = [Math]::Max($ChangeCreationCount, [Math]::Max($ChangeModifiedCount, $ChangeAccessCount))
    for ($i = 0; $i -lt $maxRandomNeeded; $i++) {
        $randomTimes += Get-RandomTime
    }

    $idx = 0
    foreach ($file in $AllFiles) {
        $processed++
        if ($processed % 500 -eq 0) {
            Write-Host "  进度: $processed / $TotalFiles" -ForegroundColor Yellow
        }

        try {
            # 1. 修改创建时间（全部）
            $rt = $randomTimes[$idx % $maxRandomNeeded]
            $newCT = Get-Date "$CreationDate $($rt.Hour):$($rt.Minute.ToString('00')):$($rt.Second.ToString('00'))"
            $file.CreationTime = $newCT

            # 2. 修改时间（早于 2026-03-03 的）
            $oldMT = $file.LastWriteTime
            if ($oldMT -lt (Get-Date "2026-03-03")) {
                $rt = $randomTimes[($idx + 1) % $maxRandomNeeded]
                $newMT = Get-Date "$ModifiedDate $($rt.Hour):$($rt.Minute.ToString('00')):$($rt.Second.ToString('00'))"
                $file.LastWriteTime = $newMT
            }

            # 3. 访问时间（同修改时间）
            $oldAT = $file.LastAccessTime
            if ($oldAT -lt (Get-Date "2026-03-03")) {
                $rt = $randomTimes[($idx + 2) % $maxRandomNeeded]
                $newAT = Get-Date "$ModifiedDate $($rt.Hour):$($rt.Minute.ToString('00')):$($rt.Second.ToString('00'))"
                $file.LastAccessTime = $newAT
            }

            $idx++
        } catch {
            Write-Host "  跳过 (无权限): $($file.FullName)" -ForegroundColor DarkGray
        }
    }

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  修改完成! 共处理 $TotalFiles 个文件" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host ">>> 这是预览模式，未执行任何修改 <<<" -ForegroundColor Yellow
    Write-Host ">>> 确认无误后，运行以下命令执行修改: " -ForegroundColor Yellow
    Write-Host ""
    Write-Host "    powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Execute" -ForegroundColor Cyan
    Write-Host ""
}
