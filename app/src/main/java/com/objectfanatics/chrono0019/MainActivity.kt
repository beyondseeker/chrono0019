package com.objectfanatics.chrono0019

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.objectfanatics.commons.android.provideer.mediastore.saveOnApi28OrOlder
import com.objectfanatics.commons.android.provideer.mediastore.saveOnApi29OrNewer
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

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

        val doOnSuccess = Runnable { }
        val doOnError = Runnable { Toast.makeText(this@MainActivity, "エラーが発生しました", Toast.LENGTH_SHORT).show() }
        val doOnEvent = Runnable { saveAndroidIconButton.isEnabled = true }
        when {
            // FIXME: 同じ引数仕様にするので、引数オブジェクト作って使いまわしたほうがいいと思われる。
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                saveOnApi29OrNewer(
                    bitmap = bitmap,
                    compressFormat = CompressFormat.PNG,
                    standardDirectory = DIRECTORY_PICTURES,
                    subDirectory = "chrono0019",
                    doOnSuccess = doOnSuccess,
                    doOnError = doOnError,
                    doOnEvent = doOnEvent
                )
            else ->
                saveOnApi28OrOlderWrapper(
                    bitmap = bitmap,
                    compressFormat = CompressFormat.PNG,
                    quality = 100,
                    standardDirectory = DIRECTORY_PICTURES,
                    subDirectory = "chrono0019",
                    doOnSuccess = doOnSuccess,
                    doOnError = doOnError,
                    doOnEvent = doOnEvent
                )
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

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun saveOnApi28OrOlderWrapper(bitmap: Bitmap, compressFormat: CompressFormat, quality: Int, standardDirectory: String, subDirectory: String?, doOnSuccess: Runnable, doOnError: Runnable, doOnEvent: Runnable) {
        saveOnApi28OrOlder(bitmap, compressFormat, quality, standardDirectory, subDirectory, doOnSuccess, doOnError, doOnEvent)
    }
}