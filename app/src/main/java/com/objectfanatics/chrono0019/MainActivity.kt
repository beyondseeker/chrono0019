package com.objectfanatics.chrono0019

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val iconImageView = findViewById<ImageView>(R.id.icon_image_view)

//        val bitmap = (iconImageView.drawable as AdaptiveIconDrawable).bitmap
        val bitmap = getBitmapFromDrawable(getDrawable(R.mipmap.ic_launcher)!!)

        save(bitmap)

        println("bitmap = $bitmap")
    }

    private fun save(bitmap: Bitmap) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveOnApi29OrNewer(bitmap)
            else -> saveOnApi28OrOlder(bitmap)
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun saveOnApi28OrOlder(bitmap: Bitmap) {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            error("This function is meant to be used by Android P or older.")
        }

        val file = createImageFileForMinimo(this)!!

        // FIXME: たぶん、MediaStore 系の、item の属性情報だと思われる。
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            // API 29 から deprecated。このメソッドは API 28 以前専用なので OK。
            // https://developer.android.com/reference/kotlin/android/provider/MediaStore.MediaColumns#data
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        val resolver = contentResolver

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        resolver.insert(collection, values)!!

        val stream: OutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()


//        resolver.openFileDescriptor(item, "w", null).use {
//            // Write data into the pending audio file.
//            // FIXME: disk full とかで IO例外出得るよね。でも、IO例外が出る裏付けがないんだよな、、、。
//            //        ということで、とりあえず例外処理しないでおくけど、ちゃんと考えましょう。
//            FileOutputStream(it!!.fileDescriptor).use { outputStream ->
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//            }
//        }

//        // FIXME: 排他制御の停止だと思われ。
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            values.clear()
//            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
//            resolver.update(item, values, null, null)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    private fun saveOnApi29OrNewer(bitmap: Bitmap) {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("This function is meant to be used by Android Q or newer.")
        }

        // FIXME: たぶん、MediaStore 系の、item の属性情報だと思われる。
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "testdayo2.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val item = resolver.insert(collection, values)!!
        resolver.openFileDescriptor(item, "w", null).use {
            // Write data into the pending audio file.
            // FIXME: disk full とかで IO例外出得るよね。でも、IO例外が出る裏付けがないんだよな、、、。
            //        ということで、とりあえず例外処理しないでおくけど、ちゃんと考えましょう。
            FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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


@Throws(IOException::class)
fun createImageFileForMinimo(context: Context): File? {
    // Create an image file name
    val timeStamp =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "IMG_$timeStamp.jpg"
    // FIXME: これは Deprecated
    val storageDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    //        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    val dir = File(storageDir.absolutePath + "/Minimo")
    dir.mkdir()
    // TODO: #5658: ここで『java.io.IOException: Permission denied』が出る。
    return File(dir.absolutePath, imageFileName)
}
