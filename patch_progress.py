import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

content = content.replace("private suspend fun performActualDownload(context: Context, rawNormalized: String, targetFile: File): Result<File> {", "private suspend fun performActualDownload(context: Context, rawNormalized: String, targetFile: File, onProgress: (Int) -> Unit): Result<File> {")
content = content.replace("val savedFile = saveResponseBodyToFile(retryResp.body!!, targetFile) { }", "val savedFile = saveResponseBodyToFile(retryResp.body!!, targetFile, onProgress)")
content = content.replace("val savedFile = saveResponseBodyToFile(body, targetFile) { }", "val savedFile = saveResponseBodyToFile(body, targetFile, onProgress)")
content = content.replace("val result = performActualDownload(context, rawNormalized, targetFile)", "val result = performActualDownload(context, rawNormalized, targetFile, onProgress)")

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)

