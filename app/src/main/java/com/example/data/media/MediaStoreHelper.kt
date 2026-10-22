package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.data.model.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreHelper {

    fun generateFileName(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        return "Lens_${sdf.format(Date(timestamp))}.jpg"
    }

    suspend fun savePhotoToPictures(
        context: Context,
        bitmap: Bitmap,
        qualityPercent: Int,
        timestamp: Long
    ): PhotoEntity = withContext(Dispatchers.IO) {
        val fileName = generateFileName(timestamp)
        var savedUri: Uri? = null
        var fileSize = 0L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lens")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_ADDED, timestamp / 1000)
                put(MediaStore.MediaColumns.DATE_TAKEN, timestamp)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to insert media row")

            savedUri = uri

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, outStream)
                outStream.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            // Query file size
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } else {
            // Android 9 and below
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val lensDir = File(picturesDir, "Lens").apply { if (!exists()) mkdirs() }
            val file = File(lensDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, out)
                out.flush()
            }
            fileSize = file.length()

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                put(MediaStore.Images.Media.SIZE, fileSize)
            }
            savedUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: file.toUri()
        }

        PhotoEntity(
            id = timestamp,
            uriString = savedUri.toString(),
            fileName = fileName,
            sizeBytes = if (fileSize > 0) fileSize else 1024L,
            dateTaken = timestamp,
            width = bitmap.width,
            height = bitmap.height
        )
    }

    suspend fun exportToDownloads(context: Context, photoUri: Uri, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(photoUri) ?: return@withContext false
            val dlName = if (fileName.endsWith(".jpg")) fileName else "$fileName.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, dlName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false

                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    inputStream.copyTo(outStream)
                    outStream.flush()
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dlDir.exists()) dlDir.mkdirs()
                val outFile = File(dlDir, dlName)
                FileOutputStream(outFile).use { outStream ->
                    inputStream.copyTo(outStream)
                    outStream.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sharePhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, null)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun deletePhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }
}
