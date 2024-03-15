package edu.put.inf151799

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.os.Build
import android.net.Uri
import android.widget.ImageView

class PowiekszZdjecie: AppCompatActivity() {
    lateinit var zdjecie: ImageView
    lateinit var przycisk_powrot: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zdjecie)

        var uriStr = intent.getStringExtra("uri")
        val uri = Uri.parse(uriStr)

        zdjecie = findViewById(R.id.imageView)
        przycisk_powrot = findViewById(R.id.wroc)
        przycisk_powrot.setOnClickListener{cofnij_do_gry(it)}

        ustaw_zdjecie(uri)
    }

    fun ustaw_zdjecie(fota: Uri) {
        val bitmap = when {
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(this.contentResolver, fota)
            else -> {
                val source = ImageDecoder.createSource(this.contentResolver,fota)
                ImageDecoder.decodeBitmap(source)
            }
        }
        zdjecie.setImageBitmap(KonkretnaGra.BitmapScaler.scaleToFitWidth(bitmap, 1000))
    }

    fun cofnij_do_gry(v: View) {
        finish()
    }
}