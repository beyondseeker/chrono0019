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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.save_android_icon_button).setOnClickListener(this::onSaveAndroidIconButtonClick)
    }

    private fun onSaveAndroidIconButtonClick(v: View) {
        save(getBitmapFromDrawable(getDrawable(R.mipmap.ic_launcher)!!))
    }

    private fun save(bitmap: Bitmap) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveOnApi29OrNewer(bitmap, "Pictures/Minimo")
            else -> saveOnApi28OrOlder(
                bitmap = bitmap,
                relativePath = "chrono0019",
                doOnError = Runnable { Toast.makeText(this@MainActivity, "エラーが発生しました", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun saveOnApi28OrOlder(
        bitmap: Bitmap,
        compressFormat: CompressFormat = CompressFormat.PNG,
        quality: Int = 100,
        type: String = DIRECTORY_PICTURES,
        relativePath: String? = null,
        // () -> Unit にしたいのだが、PermissionDispatcher のバグがあるためワークアラウンドをしている。
        // https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/503
        doOnSuccess: Runnable = Runnable { },
        doOnError: Runnable = Runnable { },
        doOnEvent: Runnable = Runnable { }
    ) {
        assertApi28OrOlder()

        val file = createExternalStorageFileOnApi28OrOlder(type, compressFormat.extension, relativePath)

        @Suppress("DEPRECATION")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, compressFormat.mimeType)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        val item = contentResolver.insert(EXTERNAL_CONTENT_URI, values)

        // FIXME: これより上は一次精査完了 -----------------------------------------------------------------------
        // FIXME: IOは非同期で処理して結果はMainThreadで返す必要がある。
        if (item == null) {
            doOnEvent.run()
            doOnError.run()
            return
        }

        try {
            FileOutputStream(file).use { os ->
                bitmap.compress(compressFormat, quality, os)
                os.flush()
            }
        } catch (e: IOException) {
            doOnEvent.run()
            doOnError.run()
        }

        doOnEvent.run()
        doOnSuccess.run()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    /**
     * @param relativePath starts with "DCIM" or "Pictures"
     */
    private fun saveOnApi29OrNewer(bitmap: Bitmap, relativePath: String = "Pictures") {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("This function is meant to be used by Android Q or newer.")
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, createDefaultImageFileName("jpg"))
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val item = resolver.insert(collection, values)

        if (item == null) {
            // FIXME: lambda 受け取って失敗時の処理やりましょう。
            Toast.makeText(this, "item is null", Toast.LENGTH_SHORT).show()
            return
        }

        resolver.openFileDescriptor(item, "w", null).use {
            FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                bitmap.compress(CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
        }

        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(item, values, null, null)
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
    type: String = DIRECTORY_PICTURES,
    ext: String,
    relativePath: String? = null
): File {
    assertApi28OrOlder()

    // Deprecated in API level 29
    @Suppress("DEPRECATION")
    val externalStoragePublicDirectoryAbsolutePath =
        getExternalStoragePublicDirectory(type).absolutePath

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