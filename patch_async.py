import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

content = content.replace("downloadScope.async {", "kotlinx.coroutines.async(downloadScope.coroutineContext) {")
with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
