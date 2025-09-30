package me.bipul.blueshare.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import me.bipul.blueshare.core.model.TransferFile

/**
 * Utility object for file operations.
 * Follows DRY principle - centralized file handling logic.
 */
object FileUtils {

    /**
     * Extracts file information from a content URI.
     *
     * @param context Android context
     * @param uri Content URI of the file
     * @return TransferFile object with file metadata, or null if extraction fails
     */
    fun getFileFromUri(context: Context, uri: Uri): TransferFile? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                cursor.moveToFirst()

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "unknown"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val mimeType = context.contentResolver.getType(uri)

                TransferFile(
                    uri = uri,
                    name = sanitizeFileName(name),
                    size = size,
                    mimeType = mimeType
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sanitizes a file name to prevent path traversal and ensure filesystem compatibility.
     *
     * @param fileName Original file name
     * @return Sanitized file name
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[/\\\\:*?\"<>|]"), "_") // Remove invalid characters
            .trim()
            .take(255) // Limit to filesystem max length
            .ifEmpty { "unnamed_file" }
    }

    /**
     * Formats file size to human-readable string.
     *
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * Formats transfer speed to human-readable string.
     *
     * @param bytesPerSecond Transfer speed in bytes per second
     * @return Formatted string (e.g., "5.2 MB/s")
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    /**
     * Formats duration to human-readable string.
     *
     * @param milliseconds Duration in milliseconds
     * @return Formatted string (e.g., "2m 30s")
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}