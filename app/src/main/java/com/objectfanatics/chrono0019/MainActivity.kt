package com.objectfanatics.chrono0019

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    private lateinit var saveAndroidIconButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        saveAndroidIconButton = findViewById<View>(R.id.save_android_icon_button)
        saveAndroidIconButton.setOnClickListener(this::onSaveAndroidIconButtonClick)
    }

    private fun onSaveAndroidIconButtonClick(v: View) {
        save(getBitmapFromDrawable(getDrawable(R.mipmap.ic_launcher)!!))
    }

    private fun save(bitmap: Bitmap) {
        saveAndroidIconButton.isEnabled = false

        when {
            // FIXME: 同じ引数仕様にするので、引数オブジェクト作って使いまわしたほうがいいと思われる。
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveOnApi29OrNewer(
                bitmap = bitmap,
                subDirectory = "chrono0019",
                doOnError = Runnable { Toast.makeText( this@MainActivity, "エラーが発生しました", Toast.LENGTH_SHORT ).show() },
                doOnEvent = Runnable { saveAndroidIconButton.isEnabled = true }
            )
            else -> saveOnApi28OrOlder(
                bitmap = bitmap,
                subDirectory = "chrono0019",
                doOnError = Runnable { Toast.makeText( this@MainActivity, "エラーが発生しました", Toast.LENGTH_SHORT ).show() },
                doOnEvent = Runnable { saveAndroidIconButton.isEnabled = true }
            )
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun saveOnApi28OrOlder(
        bitmap: Bitmap,
        compressFormat: CompressFormat = CompressFormat.PNG,
        quality: Int = 100,
        standardDirectory: String = DIRECTORY_PICTURES,
        subDirectory: String? = null,
        // () -> Unit にしたいのだが、PermissionDispatcher のバグがあるためワークアラウンドをしている。
        // https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/503
        doOnSuccess: Runnable = Runnable { },
        doOnError: Runnable = Runnable { },
        doOnEvent: Runnable = Runnable { }
    ) {
        assertApi28OrOlder()

        val file = createExternalStorageFileOnApi28OrOlder(standardDirectory, compressFormat.extension, subDirectory)

        @Suppress("DEPRECATION")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, compressFormat.mimeType)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        val item = contentResolver.insert(EXTERNAL_CONTENT_URI, values)

        if (item == null) {
            doOnEvent.run()
            doOnError.run()
            return
        }

        Thread(Runnable {
            try {
                FileOutputStream(file).use { os ->
                    bitmap.compress(compressFormat, quality, os)
                    os.flush()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    doOnEvent.run()
                    doOnError.run()
                }
            }
            runOnUiThread {
                doOnEvent.run()
                doOnSuccess.run()
            }
        }).start()
    }
    // FIXME: これより上は一次精査完了 -----------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    private fun saveOnApi29OrNewer(
        bitmap: Bitmap,
        compressFormat: CompressFormat = CompressFormat.PNG,
        quality: Int = 100,
        standardDirectory: String = DIRECTORY_PICTURES,
        subDirectory: String? = null,
        // () -> Unit にしたいのだが、PermissionDispatcher のバグがあるためワークアラウンドをしている。
        // https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/503
        doOnSuccess: Runnable = Runnable { },
        doOnError: Runnable = Runnable { },
        doOnEvent: Runnable = Runnable { }
    ) {
        assertApi29OrNewer()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, createDefaultImageFileName(compressFormat.extension))
            put(MediaStore.Images.Media.RELATIVE_PATH, getRelativePath(standardDirectory, subDirectory))
            put(MediaStore.Images.Media.MIME_TYPE, compressFormat.mimeType)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val externalContentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val item = contentResolver.insert(externalContentUri, values)

        if (item == null) {
            doOnEvent.run()
            doOnError.run()
            return
        }

        Thread(Runnable {
            try {
                contentResolver.openFileDescriptor(item, "w", null).use {
                    FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                        bitmap.compress(compressFormat, quality, outputStream)
                        outputStream.flush()
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(item, values, null, null)
                Thread.sleep(2000)
            } catch (e: IOException) {
                runOnUiThread {
                    doOnEvent.run()
                    doOnError.run()
                }
            }
            runOnUiThread {
                doOnEvent.run()
                doOnSuccess.run()
            }
        }).start()
    }

    private fun assertApi29OrNewer() {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("This function is meant to be used by Android Q or newer.")
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bmp
    }
}


// FIXME: これより下は精査済み ---------------------------------------------------------------------------
@Throws(IOException::class)
fun createExternalStorageFileOnApi28OrOlder(
    standardDirectory: String = DIRECTORY_PICTURES,
    ext: String,
    relativePath: String? = null
): File {
    assertApi28OrOlder()

    // Deprecated in API level 29
    @Suppress("DEPRECATION")
    val externalStoragePublicDirectoryAbsolutePath =
        getExternalStoragePublicDirectory(standardDirectory).absolutePath

    val targetDir = File(
        when (relativePath) {
            null -> externalStoragePublicDirectoryAbsolutePath
            else -> "$externalStoragePublicDirectoryAbsolutePath/$relativePath"
        }
    )

    targetDir.mkdirs()

    return File(targetDir.absolutePath, createDefaultImageFileName(ext))
}

fun createDefaultImageFileName(ext: String): String =
    "IMG_${createDefaultTimestampString()}.$ext"

/**
 * 端末の現在時刻を用いた "yyyyMMdd_HHmmss" フォーマットのタイムスタンプ文字列を返します。
 */
fun createDefaultTimestampString(): String =
    // FIXME: これって desugar して DateTimeFormatter 使いたいですよね。
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

private fun assertApi28OrOlder() {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        error("This function is meant to be used by Android P or older.")
    }
}

private val CompressFormat.extension: String
    get() = when (this) {
        CompressFormat.JPEG -> "jpg"
        CompressFormat.PNG -> "png"
        CompressFormat.WEBP -> "webp"
    }

private val CompressFormat.mimeType: String
    get() = "image/$extension"

private fun getRelativePath(standardDirectory: String, subDirectory: String?): String =
    when (subDirectory) {
        null -> standardDirectory
        else -> "$standardDirectory/$subDirectory"
    }