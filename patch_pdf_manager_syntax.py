with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith("import androidx.lifecycle.viewModelScope"):
        continue
    new_lines.append(line)

new_lines.insert(2, "import androidx.lifecycle.viewModelScope\n")

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.writelines(new_lines)

