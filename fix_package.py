with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    lines = f.readlines()

if lines[0].startswith("import kotlinx.coroutines.async"):
    # Swap line 0 and line 1
    lines[0], lines[1] = lines[1], lines[0]

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.writelines(lines)
