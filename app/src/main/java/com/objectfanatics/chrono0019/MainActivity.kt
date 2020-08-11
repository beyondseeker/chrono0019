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
import com.objectfanatics.commons.android.provideer.mediaStore.SaveImageArgs
import com.objectfanatics.commons.android.provideer.mediaStore.saveImageOnApi28OrOlder
import com.objectfanatics.commons.android.provideer.mediaStore.saveImageOnApi29OrNewer
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun onSaveAndroidIconButtonClick(v: View) {
        saveImage(getBitmapFromDrawable(getDrawable(R.mipmap.ic_launcher)!!))
    }

    private fun saveImage(bitmap: Bitmap) {
        runOnUiThread {  }
        val args = SaveImageArgs(
            bitmap = bitmap,
            compressFormat = CompressFormat.PNG,
            standardDirectory = DIRECTORY_PICTURES,
            subDirectory = "chrono0019",
            doOnSuccess = Runnable { Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show() },
            doOnError = Runnable { Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show() },
            doOnEvent = Runnable { saveAndroidIconButton.isEnabled = true }
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                saveAndroidIconButton.isEnabled = false
                saveImageOnApi29OrNewer(args)
            }
            else -> saveImageOnApi28OrOlderWrapperWithPermissionCheck(args)
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
    fun saveImageOnApi28OrOlderWrapper(args: SaveImageArgs) {
        saveAndroidIconButton.isEnabled = false
        saveImageOnApi28OrOlder(args)
    }
}