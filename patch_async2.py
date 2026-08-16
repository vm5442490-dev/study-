import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

content = content.replace("kotlinx.coroutines.async(downloadScope.coroutineContext) {", "downloadScope.async {")
content = "import kotlinx.coroutines.async\n" + content

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
