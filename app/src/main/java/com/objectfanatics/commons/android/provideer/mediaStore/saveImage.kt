package com.objectfanatics.commons.android.provideer.mediaStore

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.objectfanatics.chrono0019.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class SaveImageArgs(
    val bitmap: Bitmap,
    val compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    val quality: Int = 100,
    val standardDirectory: String = Environment.DIRECTORY_PICTURES,
    val subDirectory: String? = null,
    val fileName: String = createDefaultImageFileName(compressFormat.extension),
    // () -> Unit にしたいのだが、PermissionDispatcher のバグがあるためワークアラウンドをしている。
    // https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/503
    val doOnSuccess: Runnable = Runnable { },
    val doOnError: Runnable = Runnable { },
    val doOnEvent: Runnable = Runnable { }
)

@RequiresApi(Build.VERSION_CODES.Q)
@Throws(IOException::class)
fun Context.saveImageOnApi29OrNewer(args: SaveImageArgs) {
    with(args) {
        assertApi29OrNewer()

        val saveAsync: () -> Unit = {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, args.fileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, getRelativePath(standardDirectory, subDirectory))
                put(MediaStore.Images.Media.MIME_TYPE, compressFormat.mimeType)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val externalContentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val item = contentResolver.insert(externalContentUri, values)

            if (item == null) {
                runOnMainThread {
                    doOnEvent.run()
                    doOnError.run()
                }
            } else {
                contentResolver.openFileDescriptor(item, "w", null).use {
                    FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                        bitmap.compress(compressFormat, quality, outputStream)
                        outputStream.flush()
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(item, values, null, null)
            }
        }

        execute(saveAsync, doOnSuccess, doOnError, doOnEvent)
    }
}

fun Context.saveImageOnApi28OrOlder(args: SaveImageArgs) {
    with(args) {
        assertApi28OrOlder()

        val saveAsync: () -> Unit = {
            val file = createExternalStorageFileOnApi28OrOlder(standardDirectory, args.fileName, subDirectory)

            // Deprecated in API level 29
            @Suppress("DEPRECATION")
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, compressFormat.mimeType)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }

            val item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (item == null) {
                runOnMainThread {
                    doOnEvent.run()
                    doOnError.run()
                }
            } else {
                FileOutputStream(file).use { os ->
                    bitmap.compress(compressFormat, quality, os)
                    os.flush()
                }
            }
        }

        execute(saveAsync, doOnSuccess, doOnError, doOnEvent)
    }
}

private fun assertApi28OrOlder() {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        error("This function is meant to be used by Android P or older.")
    }
}

private fun assertApi29OrNewer() {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        error("This function is meant to be used by Android Q or newer.")
    }
}

private fun createDefaultImageFileName(ext: String): String =
    "IMG_${createDefaultTimestampString()}.$ext"

/**
 * 端末の現在時刻を用いた "yyyyMMdd_HHmmss" フォーマットのタイムスタンプ文字列を返します。
 */
private fun createDefaultTimestampString(): String =
    // FIXME: これって desugar して DateTimeFormatter 使いたいですよね。
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

private val Bitmap.CompressFormat.extension: String
    get() = when (this) {
        Bitmap.CompressFormat.JPEG -> "jpg"
        Bitmap.CompressFormat.PNG -> "png"
        Bitmap.CompressFormat.WEBP -> "webp"
    }

private val Bitmap.CompressFormat.mimeType: String
    get() = "image/$extension"

@Throws(IOException::class)
private fun createExternalStorageFileOnApi28OrOlder(
    standardDirectory: String = Environment.DIRECTORY_PICTURES,
    fileName: String,
    relativePath: String? = null
): File {
    assertApi28OrOlder()

    // Deprecated in API level 29
    @Suppress("DEPRECATION")
    val externalStoragePublicDirectoryAbsolutePath =
        Environment.getExternalStoragePublicDirectory(standardDirectory).absolutePath

    val targetDir = File(
        when (relativePath) {
            null -> externalStoragePublicDirectoryAbsolutePath
            else -> "$externalStoragePublicDirectoryAbsolutePath/$relativePath"
        }
    )

    targetDir.mkdirs()

    return File(targetDir.absolutePath, fileName)
}

private fun getRelativePath(standardDirectory: String, subDirectory: String?): String =
    when (subDirectory) {
        null -> standardDirectory
        else -> "$standardDirectory/$subDirectory"
    }

private fun execute(
    save: () -> Unit,
    doOnSuccess: Runnable,
    doOnError: Runnable,
    doOnEvent: Runnable
) {
    Thread(Runnable {
        try {
            save()
        } catch (e: IOException) {
            runOnMainThread {
                doOnEvent.run()
                doOnError.run()
            }
        }
        runOnMainThread {
            doOnEvent.run()
            doOnSuccess.run()
        }
    }).start()
}

private fun runOnMainThread(run: () -> Unit) {
    if (!isUiThread) {
        Handler(Looper.getMainLooper()).post { run() }
    } else {
        run()
    }
}

val isUiThread: Boolean
    get() = Thread.currentThread() == Looper.getMainLooper().thread