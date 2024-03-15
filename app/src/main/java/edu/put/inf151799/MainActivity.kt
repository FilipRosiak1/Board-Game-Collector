package edu.put.inf151799

import java.util.Date
import java.io.File
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import android.os.Bundle
import android.widget.Button
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.xmlpull.v1.XmlPullParserFactory
import android.widget.TextView
import android.content.SharedPreferences
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.EditText
import java.io.IOException
import androidx.core.content.ContextCompat
import java.lang.Exception
import java.net.MalformedURLException
import android.widget.Toast
import android.Manifest
import java.net.URL
import java.io.FileWriter
import androidx.appcompat.app.AlertDialog
import android.content.pm.PackageManager
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParser
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import android.content.Intent

class MainActivity : AppCompatActivity() {
    var nazwa_uzytkownika = ""
    var posiadane_gry = 0
    var posiadane_dodatki = 0
    lateinit var ostatnia_synchronizacja: Date

    var cache: SharedPreferences? = null
    var item_names = arrayOf("boardgame","boardgameexpansion")

    val bazaDanych = BazaDanych(this)

    lateinit var przycisk_lista_gier: Button
    lateinit var przycisk_lista_dodatkow: Button
    lateinit var przycisk_synchronizuj: Button
    lateinit var przycisk_wyczysc_dane: Button

    lateinit var nazwa_uzytkownika_textview: TextView
    lateinit var posiadane_gry_textview: TextView
    lateinit var posiadane_dodatki_textview: TextView
    lateinit var ostatnia_synchronizacja_textview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        przycisk_lista_gier = findViewById(R.id.lista_gier_but)
        przycisk_lista_dodatkow = findViewById(R.id.lista_dodatkow_but)
        przycisk_synchronizuj = findViewById(R.id.synchronizacja_but)
        przycisk_wyczysc_dane = findViewById(R.id.wyczysc_dane_but)

        nazwa_uzytkownika_textview = findViewById(R.id.nazwa_uzytkownika)
        posiadane_gry_textview = findViewById(R.id.posiadane_gry)
        posiadane_dodatki_textview = findViewById(R.id.posiadane_dodatki)
        ostatnia_synchronizacja_textview = findViewById(R.id.synchronizacja)

        cache = getSharedPreferences("151799gamecache", MODE_PRIVATE)
        ostatnia_synchronizacja = Date(cache!!.getLong("ostatnia_synchronizacja",0))

        if(cache!!.getBoolean("pierwsze_uruchomienie", true)) {
            val i = Intent(this, Logowanie::class.java)
            startActivity(i)
            finish()
        }
        else {
            nazwa_uzytkownika = cache!!.getString("nazwa_uzytkownika", "default") ?: "Nie ustawiono"
            if(cache!!.getBoolean("pierwsza_synchronizacja", true)) {
                getGameData(nazwa_uzytkownika, true)
                cache!!.edit().putBoolean("pierwsza_synchronizacja", false).commit()
            }
            zaladuj_dane_z_bazy()
        }

        przycisk_lista_dodatkow.setOnClickListener{listuj_dodatki(it)}
        setupPermission()
    }

    val READ_REQUEST_CODE = 101
    val WRITE_REQUEST_CODE = 102

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

    fun wyczysc_dane(v:View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Czy na pewno chcesz usunąć dane?")
        builder.setPositiveButton("Tak") { _, _ ->
            bazaDanych.tworz_tabele()
            cache!!.edit().putBoolean("pierwsze_uruchomienie", true).commit()
            cache!!.edit().putBoolean("pierwsza_synchronizacja", true).commit()
            cache!!.edit().putString("nazwa_uzytkownika", "").commit()
            nazwa_uzytkownika_textview.text = "Witaj !"
            posiadane_gry_textview.text = "Liczba posiadanych gier: 0"
            posiadane_dodatki_textview.text = "Liczba posiadanych dodatków: 0"
            ostatnia_synchronizacja_textview.text = "Ostatnia synchronizacja: Brak"
            finish()
        }
        builder.setNegativeButton("Nie") { dialog, which ->
        }
        builder.show()
    }

    fun zaladuj_dane_z_bazy() {
        nazwa_uzytkownika_textview.text = "Witaj $nazwa_uzytkownika!"

        posiadane_gry = bazaDanych.select_all(items.GAME,"").size
        posiadane_gry_textview.text = "Liczba posiadanych gier: $posiadane_gry"

        posiadane_dodatki = bazaDanych.select_all(items.EXPANSION, "").size
        posiadane_dodatki_textview.text = "Liczba posiadanych dodatków: $posiadane_dodatki"

        val format = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a")
        val formattedDate = format.format(ostatnia_synchronizacja)
        ostatnia_synchronizacja_textview.text = "Ostatnia synchronizacja:\n$formattedDate"
    }

    fun synchronizuj(v:View) {
        val czas = Calendar.getInstance().time
        val roznica = czas.time - ostatnia_synchronizacja.time
        if (roznica < 24*60*60*1000) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Od ostatniej synchronizacji nie minął jeszcze 1 dzień")
            builder.setMessage("Czy chcesz ponownie zsynchonizować dane?")
            builder.setNegativeButton("Nie") {dialog, which ->
                Toast.makeText(this, "Synchronizacja przerwana", Toast.LENGTH_SHORT).show()
            }
            builder.setPositiveButton("Tak") {_,_ ->
                getGameData(nazwa_uzytkownika, false)
            }
            builder.show()
        } else {
            getGameData(nazwa_uzytkownika, false)
        }
    }

    fun getGameData(username: String, wyczysc_tabele: Boolean) {
        val games_url = "https://boardgamegeek.com/xmlapi2/collection?username=$username&stats=1&excludesubtype=boardgameexpansion"
        val expansion_url = "https://boardgamegeek.com/xmlapi2/collection?username=$username&stats=1&subtype=boardgameexpansion"

        Toast.makeText(applicationContext, "Trwa synchronizacja, proszę czekać", Toast.LENGTH_SHORT).show()
        if(wyczysc_tabele) bazaDanych.tworz_tabele()

        val xmlDirectory = File("$filesDir/XML")
        if(!xmlDirectory.exists()) xmlDirectory.mkdir()
        val filename = "$xmlDirectory/$username-gamedata.xml"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var url = URL(games_url)
                var reader = url.openStream().bufferedReader()
                var downloadFile = File(filename).also { it.createNewFile() }
                var writer = FileWriter(downloadFile).buffered()
                var line: String
                while(reader.readLine().also { line = it?.toString() ?: "" } != null) {
                    writer.write(line)
                }
                reader.close()
                writer.close()

                withContext(Dispatchers.Main) {
                    save_to_db(username)
                    posiadane_gry = bazaDanych.select_all(items.GAME, "").size
                }
                url = URL(expansion_url)
                reader = url.openStream().bufferedReader()
                downloadFile = File(filename)
                writer = FileWriter(downloadFile).buffered()
                while(reader.readLine().also { line = it?.toString() ?: "" } != null) {
                    writer.write(line)
                }
                reader.close()
                writer.close()

                withContext(Dispatchers.Main) {
                    save_to_db(username)
                    posiadane_dodatki = bazaDanych.select_all(items.EXPANSION, "").size
                    ostatnia_synchronizacja = Calendar.getInstance().time
                    cache!!.edit().putLong("ostatnia_synchronizacja", ostatnia_synchronizacja.time).commit()

                    zaladuj_dane_z_bazy()
                    Toast.makeText(applicationContext, "Synchronizacja zakończona", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Synchronizacja nie powiodła się", Toast.LENGTH_SHORT).show()
                    when (e) {
                        is MalformedURLException ->
                            print("Malformed URL")
                        else ->
                            print("error")
                    }
                    val incompleteFile = File(filename)
                    if (incompleteFile.exists()) incompleteFile.delete()
                }
            }
        }
    }

    fun save_to_db(user: String) {
        val filename = "$user-gamedata.xml"
        val filePath = "$filesDir/XML/$filename"
        val file = File(filePath)

        if(file.exists()) {
            val pullParserFactory: XmlPullParserFactory
            try {
                val inputStream = file.inputStream()
                pullParserFactory = XmlPullParserFactory.newInstance()
                val parser = pullParserFactory.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)

                var data: Gra? = null
                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    var name = ""
                    when (eventType) {
                        XmlPullParser.START_TAG ->{
                            name = parser.name
                            if(name == "item") {
                                data = Gra()
                                data.id = parser.getAttributeValue(null, "objectid").toInt()
                                val type = parser.getAttributeValue(null, "subtype")

                                when(type) {
                                    item_names[items.GAME.ordinal] -> data.czy_dodatek = 0
                                    item_names[items.EXPANSION.ordinal] -> data.czy_dodatek = 1
                                    else -> data = null
                                }
                            }
                            if(data != null && name == "name") data.tytul = parser.nextText()
                            if(data != null && name == "yearpublished") data.rok_wydania = parser.nextText().toInt()
                            if(data != null && name == "thumbnail") data.thumbnail = parser.nextText()
                            if(data != null && name == "rank") {
                                if(parser.getAttributeValue(null, "type") == "subtype")
                                    if(parser.getAttributeValue(null, "name") == "boardgame") {
                                        val rank = parser.getAttributeValue(null, "value").toIntOrNull()
                                        if(rank != null) data.ranking = rank
                                    }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            name = parser.name
                            if(data != null && name.equals("item", ignoreCase = true)) {
                                bazaDanych.dodaj_gre(data)
                                data = Gra()
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch(e: XmlPullParserException) {
                e.printStackTrace()
            } catch(e: IOException) {
                e.printStackTrace()
            } catch(e:Exception) {
                e.printStackTrace()
            }
        }
    }

    fun listuj_gry(v: View) {
        val i = Intent(this, ListaGier::class.java)
        i.putExtra("tryb", "gry")
        startActivity(i)
    }

    fun listuj_dodatki(v: View) {
        val i = Intent(this, ListaGier::class.java)
        i.putExtra("tryb", "dodatki")
        startActivity(i)
    }
}