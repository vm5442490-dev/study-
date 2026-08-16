with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

content = content.replace("// Global scope to ensure downloads complete even if UI is detached \n     + kotlinx.coroutines.Dispatchers.IO)", "")

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
