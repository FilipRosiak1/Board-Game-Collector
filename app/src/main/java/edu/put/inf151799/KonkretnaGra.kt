package edu.put.inf151799

import android.content.pm.PackageManager
import android.util.Log
import android.content.ContentValues
import kotlinx.coroutines.launch
import android.widget.TextView
import android.net.Uri
import android.graphics.ImageDecoder
import androidx.core.content.FileProvider
import java.util.Objects
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.TableLayout
import android.widget.TableRow
import android.os.Bundle
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import java.io.File
import android.view.View
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Environment
import android.os.Build
import android.Manifest
import java.lang.NullPointerException
import android.graphics.Bitmap
import kotlin.Exception
import java.util.UUID
import android.content.ContentResolver
import android.widget.Button
import android.provider.MediaStore
import java.net.URL

class KonkretnaGra: AppCompatActivity() {
    lateinit var tabela_zdjec: TableLayout
    lateinit var przycisk_dodaj_apart: Button
    lateinit var przycisk_dodaj_galeria: Button

    lateinit var idGry_text: TextView
    lateinit var zdjecieGry_view: ImageView
    lateinit var rokwydaniaGry_text: TextView
    lateinit var tytulGry_text: TextView
    lateinit var rankingGry_text: TextView

    var idGry = 0
    private var rows = 0
    val bazaDanych = BazaDanych(this)

    object BitmapScaler{
        fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
            val factor = width / b.width.toFloat()
            return  Bitmap.createScaledBitmap(b,width,(b.height*factor).toInt(),true)
        }
        fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
            val factor = height / b.height.toFloat()
            return  Bitmap.createScaledBitmap(b,(b.width*factor).toInt(),height,true)
        }
    }

    var mGetContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
        result -> try {
            val state = Environment.getExternalStorageState()
            if (result != null && Environment.MEDIA_MOUNTED.equals(state)) {
                val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result)
                dodaj_zdjecia_do_tabeli(result)

                val uid = UUID.randomUUID()
                val nazwa = idGry.toString()+"-"+uid.toString()
                zapisz_zdjecie(nazwa, bitmap)
            }
        } catch (e: Exception) {
            Log.i("err", Log.getStackTraceString(e))
        }
    }

    private fun getCapturedImage(selectedPhotoUri: Uri,width: Int): Bitmap{
        val bitmap = when{
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPhotoUri)
            else ->{
                val source = ImageDecoder.createSource(this.contentResolver,selectedPhotoUri)
                ImageDecoder.decodeBitmap(source)
            }
        }
        return BitmapScaler.scaleToFitWidth(bitmap,width)
    }

    private fun initTempUri():Uri{
        val tempImagesDir = File(applicationContext.filesDir,getString(R.string.temp_images_dir))
        tempImagesDir.mkdir()
        val tempImage = File(tempImagesDir,getString(R.string.temp_image))
        return FileProvider.getUriForFile(applicationContext,getString(R.string.authorities),tempImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gra)

        idGry = intent.getIntExtra("id", 0)

        tabela_zdjec = findViewById(R.id.tabela_zdjec)

        przycisk_dodaj_galeria = findViewById(R.id.galeria)
        przycisk_dodaj_galeria.setOnClickListener{ mGetContent.launch("image/*")}
        przycisk_dodaj_apart = findViewById(R.id.zrob_zdjecie)

        zdjecieGry_view = findViewById(R.id.zdjecie_gry)
        idGry_text = findViewById(R.id.id_gry)
        rankingGry_text = findViewById(R.id.ranking)
        rokwydaniaGry_text = findViewById(R.id.rok_wydania)
        tytulGry_text = findViewById(R.id.tytul_gry)

        setupPermission()
        ustaw_pola()

        val tempImageUri = initTempUri()
        val resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            try {
                val state = Environment.getExternalStorageState()
                if(Environment.MEDIA_MOUNTED.equals(state) && it) {
                    val uid = UUID.randomUUID()
                    val nazwa = idGry.toString()+"-"+uid.toString()
                    val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), tempImageUri)
                    dodaj_zdjecia_do_tabeli(tempImageUri)
                    zapisz_zdjecie(nazwa, bitmap)
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
        przycisk_dodaj_apart.setOnClickListener {
            resultLauncher.launch(tempImageUri)
        }
        wypelnij_tabele()
    }

    fun ustaw_pola() {
        val gra = bazaDanych.znajdz_po_ID(idGry)

        try {
            if(gra != null) {
                idGry_text.text = "BGG ID: "+gra.id.toString()
                if(gra.czy_dodatek == 0) {
                    rankingGry_text.text = "Ranking: " + gra.ranking.toString()
                    rokwydaniaGry_text.text = "Rok wydania: " + gra.rok_wydania.toString()
                } else {
                    rankingGry_text.text = ""
                    rokwydaniaGry_text.text = ""
                }
                tytulGry_text.text = "Tytuł: "+gra.tytul

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val zdjecie = BitmapFactory.decodeStream(URL(gra.thumbnail).openConnection().getInputStream())
                        withContext(Dispatchers.Main) {
                            zdjecieGry_view.setImageBitmap(BitmapScaler.scaleToFitWidth(zdjecie,600))
                        }
                    } catch (e: java.lang.Exception) {
                        withContext(Dispatchers.Main) {
                            zdjecieGry_view.setImageDrawable(resources.getDrawable(R.drawable.placeholder2))
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun wypelnij_tabele() {
        val gra = bazaDanych.znajdz_po_ID(idGry)
        val zdjecia = gra?.zdjecia

        tabela_zdjec.removeAllViews()
        try {
            val nazwy_zdjec = zdjecia?.split(",")
            if(nazwy_zdjec != null) {
                for (nazwa in nazwy_zdjec) {
                    try {
                        val file = File(Environment.getExternalStorageDirectory().toString()+"/Pictures/"+nazwa+".jpg")
                        dodaj_zdjecia_do_tabeli(Uri.fromFile(file))
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else throw NullPointerException()
        } catch(e: NullPointerException) {
            e.printStackTrace()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun dodaj_zdjecia_do_tabeli(result: Uri?) {
        if(result == null) return

        val thumbnail = ImageView(this)
        thumbnail.setImageURI(result)
        thumbnail.setImageBitmap(getCapturedImage(result, 600))
        thumbnail.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT)
        thumbnail.setTag(R.string.key, result)
        thumbnail.setOnClickListener {powieksz_zdjecie(it)}

        val tr = TableRow(this)
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT)
        trParams.setMargins(0, 0, 0, 0)
        tr.layoutParams = trParams
        tr.id = rows
        rows++
        tr.setPadding(10,0,10,0)
        tr.addView(thumbnail)
        tabela_zdjec.addView(tr,trParams)
    }

    fun usun_zdjecia(v: View) {
        val gra = bazaDanych.znajdz_po_ID(idGry)
        val zdjecia = gra?.zdjecia
        tabela_zdjec.removeAllViews()

        try {
            val nazwy = zdjecia?.split(",")
            if (nazwy != null) {
                for(nazwa in nazwy) {
                    try {
                        val file = File("/storage/emulated/0/Pictures/$nazwa.jpg")
                        if(file.exists()) {
                            file.delete()
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
                gra.zdjecia = ""
                bazaDanych.zmien_zdjecia(gra)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun zapisz_zdjecie(nazwa: String, bitmap: Bitmap) {
        var zdjecia:Uri? = null
        val resolver: ContentResolver = contentResolver

        zdjecia = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        var content = ContentValues()
        content.put(MediaStore.Images.Media.DISPLAY_NAME, nazwa+".jpg")
        content.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        content.put(MediaStore.Images.Media.DATA, "/storage/emulated/0/Pictures/$nazwa.jpg")
        val imageuri = resolver.insert(zdjecia, content)

        try {
            val OutputStream = Objects.requireNonNull(imageuri)?.let{resolver.openOutputStream(it)}
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,OutputStream)
            Objects.requireNonNull(OutputStream)

            var gra = bazaDanych.znajdz_po_ID(idGry)
            var nazwy_zdjec = gra?.zdjecia
            if(nazwy_zdjec != null) {
                nazwy_zdjec = if(nazwy_zdjec == "") nazwa
                else nazwy_zdjec+","+nazwa
                if(gra != null) {
                    gra.zdjecia = nazwy_zdjec
                    bazaDanych.zmien_zdjecia(gra)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Nie zapisano zdjęcia :(", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun powieksz_zdjecie(v: View) {
        val i = Intent(this, PowiekszZdjecie::class.java)
        i.putExtra("uri", v.getTag(R.string.key).toString())
        startActivity(i)
    }

    val READ_REQUEST_CODE = 111
    val WRITE_REQUEST_CODE = 112

    private fun setupPermission() {
        val read_perm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(read_perm != PackageManager.PERMISSION_GRANTED) makeRequest(READ_REQUEST_CODE)

        val write_perm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(write_perm != PackageManager.PERMISSION_GRANTED) makeRequest(WRITE_REQUEST_CODE)
    }

    private fun makeRequest(code: Int) {
        when(code) {
            READ_REQUEST_CODE ->requestPermissions( arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),code)
            WRITE_REQUEST_CODE -> requestPermissions( arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),code)
        }
    }
}