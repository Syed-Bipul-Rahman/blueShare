package me.bipul.blueshare.core.model

import android.net.Uri

/**
 * Represents a file to be transferred.
 * Immutable data class with file metadata.
 *
 * @property uri Content URI of the file
 * @property name File name (sanitized)
 * @property size File size in bytes
 * @property mimeType MIME type of the file
 */
data class TransferFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?
) {
    /**
     * Returns a sanitized file name to prevent path traversal attacks.
     * Removes any path separators and ensures the name is safe.
     */
    val safeName: String
        get() = name.replace(Regex("[/\\\\]"), "_")
}