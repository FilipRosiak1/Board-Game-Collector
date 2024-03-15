package edu.put.inf151799

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class Logowanie: AppCompatActivity() {
    var nazwa_uzytkownika = ""
    var cache: SharedPreferences? = null
    lateinit var editText: EditText
    lateinit var przycisk_potwierdz: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logowanie)

        editText = findViewById(R.id.pole_uzytkownik)
        przycisk_potwierdz = findViewById(R.id.potwierdz)
        cache = getSharedPreferences("151799gamecache", MODE_PRIVATE)

        przycisk_potwierdz.setOnClickListener {zapisz_uzytkownika(it)}
    }

    fun zapisz_uzytkownika(v: View) {
        nazwa_uzytkownika = editText.text.toString()
        cache!!.edit().putString("nazwa_uzytkownika", nazwa_uzytkownika).commit()
        cache!!.edit().putBoolean("pierwsze_uruchomienie", false).commit()
        cache!!.edit().putBoolean("pierwsza_synchronizacja", true).commit()

        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
        Handler().postDelayed({},100)
        finish()

    }
}