import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "private const val TAG" in line:
        line = line.replace("const ", "")
    elif "// Global scope to ensure downloads complete even if UI is detached" in line:
        continue
    elif "+ kotlinx.coroutines.Dispatchers.IO)" in line and "val activeDownloads" not in line:
        continue
    
    new_lines.append(line)

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.writelines(new_lines)
