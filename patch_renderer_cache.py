import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

renderer_cache_code = """
    // Mutex to prevent multiple parallel PdfRenderer native allocations which cause OOM
    private val pdfRenderMutex = kotlinx.coroutines.sync.Mutex()
    
    private val openRenderers = java.util.concurrent.ConcurrentHashMap<String, Pair<ParcelFileDescriptor, PdfRenderer>>()
    
    fun closeRenderer(file: File?) {
        if (file == null) return
        val path = file.absolutePath
        openRenderers.remove(path)?.let { (fd, renderer) ->
            try { renderer.close() } catch (e: Exception) {}
            try { fd.close() } catch (e: Exception) {}
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        getApplication<android.app.Application>().unregisterComponentCallbacks(this)
        pageBitmapCache.evictAll()
        openRenderers.keys().toList().forEach { path ->
            closeRenderer(File(path))
        }
    }
"""

content = content.replace(
"""    // Mutex to prevent multiple parallel PdfRenderer native allocations which cause OOM
    private val pdfRenderMutex = kotlinx.coroutines.sync.Mutex()""", renderer_cache_code)

content = content.replace(
"""    override fun onCleared() {
        super.onCleared()
        getApplication<android.app.Application>().unregisterComponentCallbacks(this)
        pageBitmapCache.evictAll()
    }""", "")

old_render = """                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
                    PdfRenderer(fileDescriptor).use { renderer ->
                        if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                            return@withContext Result.failure(
                                IllegalArgumentException("Page index $pageIndex out of range (Total: ${renderer.pageCount})")
                            )
                        }

                        renderer.openPage(pageIndex).use { page ->"""

new_render = """                val path = file.absolutePath
                var rendererPair = openRenderers[path]
                if (rendererPair == null) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val newRenderer = PdfRenderer(fd)
                    rendererPair = Pair(fd, newRenderer)
                    openRenderers[path] = rendererPair
                }
                val renderer = rendererPair.second
                
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Page index $pageIndex out of range (Total: ${renderer.pageCount})")
                    )
                }

                renderer.openPage(pageIndex).use { page ->"""

content = content.replace(old_render, new_render)

# Remove the extra `} }` corresponding to `.use {` that we removed
extra_braces = """                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {"""

new_extra_braces = """                        }
            } catch (e: kotlinx.coroutines.CancellationException) {"""

content = content.replace(extra_braces, new_extra_braces)

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
