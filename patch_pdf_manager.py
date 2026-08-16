import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

# 1. Change object to class
content = content.replace("object PdfDownloadManager {", "class PdfDownloadManager(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {")

# 2. Add viewModelScope
if "androidx.lifecycle.viewModelScope" not in content:
    content = "import androidx.lifecycle.viewModelScope\n" + content

# 3. Replace downloadScope with viewModelScope
content = re.sub(r'private val downloadScope = kotlinx\.coroutines\.CoroutineScope\(.*?\)', '', content)
content = content.replace("downloadScope.async", "viewModelScope.async")

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
