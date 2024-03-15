package edu.put.inf151799

import kotlinx.coroutines.CoroutineScope
import java.net.URL
import android.widget.TableRow
import android.widget.TextView
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.widget.TableLayout
import kotlinx.coroutines.launch
import android.widget.Button
import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.Gravity

class ListaGier: AppCompatActivity() {
    val bazaDanych = BazaDanych(this)
    var lista_gier = ArrayList<Gra>()
    lateinit var tabela: TableLayout
    lateinit var  tryb: items

    lateinit var przycisk_sortuj_po_nazwie: Button
    lateinit var przycisk_sortuj_po_roku: Button

    lateinit var naglowek_text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista)

        val modeStr = intent.getStringExtra("tryb")

        naglowek_text = findViewById(R.id.naglowek)
        tabela = findViewById(R.id.tabela)
        przycisk_sortuj_po_nazwie = findViewById(R.id.sortuj_po_tytule)
        przycisk_sortuj_po_roku = findViewById(R.id.sortuj_po_roku)

        when(modeStr) {
            "gry" -> {
                naglowek_text.text = "Posiadane Gry"
                tryb = items.GAME
                przycisk_sortuj_po_roku.isEnabled = true
            }
            "dodatki" -> {
                naglowek_text.text = "Posiadane Dodatki"
                tryb = items.EXPANSION
                przycisk_sortuj_po_roku.isEnabled = false
            }
            else -> {
                naglowek_text.text = "Posiadane Gry"
                tryb = items.GAME
                przycisk_sortuj_po_roku.isEnabled = true
            }
        }
        sortuj_gry(R.id.sortuj_po_tytule)
    }

    fun sortuj_gry(sort_by: Int) {
        Toast.makeText(applicationContext, "Trwa sortowanie", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            when (sort_by) {
                R.id.sortuj_po_tytule -> lista_gier = bazaDanych.select_all(tryb, "tytul_org")
                R.id.sortuj_po_roku -> lista_gier = bazaDanych.select_all(tryb, "rok_wydania")
                else -> lista_gier = bazaDanych.select_all(tryb, "tytul_org")
            }
            withContext(Dispatchers.Main) {
                showGames()
                Toast.makeText(applicationContext, "Sortowanie zakończone", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun showGames() {
        tabela.removeAllViews()

        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0

        val tv = TextView(this)
        tv.text = "Lp."
        tv.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        tv.setPadding(20,15,20,15)
        tv.gravity = Gravity.START


        val tv2 = TextView(this)
        tv2.text = "Zdjęcie gry"
        tv2.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        tv2.setPadding(20,15,20,15)
        tv2.gravity = Gravity.CENTER


        val tv3 = TextView(this)
        tv3.text = "Tytuł"
        val tv3params: TableRow.LayoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT)
        tv3params.weight = 1.0f
        tv3.layoutParams = tv3params
        tv3.setPadding(20,15,20,15)
        tv3.gravity = Gravity.START


        val tr = TableRow(this)
        val trParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT)
        tr.id = -1
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        tr.layoutParams = trParams
        tr.setPadding(10,0,10,0)
        tr.addView(tv)
        tr.addView(tv2)
        tr.addView(tv3)

        if(tryb == items.GAME) {
            val tv4 = TextView(this)
            tv4.text = "Rok wydania"
            tv4.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT)
            tv4.setPadding(20,15,20,15)
            tv4.gravity = Gravity.START
            tr.addView(tv4)
        }

        tabela.addView(tr, trParams)

        for( i in 0 until lista_gier.size) {
            val lp = TextView(this)
            val thumbnail = ImageView(this)
            val tytul = TextView(this)
            val data = TextView(this)

            lp.text = (i+1).toString()
            lp.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT)
            lp.setPadding(0, 10, 0, 10)
            lp.gravity = Gravity.CENTER
            lp.setTag(R.string.key,lista_gier[i].id)
            lp.setOnClickListener{ rozwin_gre(lp)}

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(lista_gier[i].thumbnail)
                    val thumb = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    withContext(Dispatchers.Main) {
                        if (thumb == null) {

                            thumbnail.setImageDrawable(resources.getDrawable(R.drawable.placeholder1))
                        } else {
                            thumbnail.setImageBitmap(thumb)
                        }
                    }
                } catch (e: Exception) {
                    thumbnail.setImageDrawable(resources.getDrawable(R.drawable.placeholder1))
                }
            }
            thumbnail.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT)
            thumbnail.setPadding(20, 15, 20, 15)
            thumbnail.setTag(R.string.key, lista_gier[i].id)
            thumbnail.setOnClickListener{rozwin_gre(thumbnail)}

            val gra = lista_gier[i]

            tytul.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT)
            tytul.text = "${gra.tytul}"
            val tytulparams: TableRow.LayoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT)
            tytulparams.weight = 1.0f
            tytul.layoutParams = tytulparams
            tytul.setPadding(20, 15, 20, 15)
            tytul.setTag(R.string.key, lista_gier[i].id)
            tytul.setOnClickListener { rozwin_gre(tytul) }


            val tr2 = TableRow(this)
            val tr2Params = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT)
            tr2Params.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
            tr2.layoutParams = tr2Params
            tr2.id = i+1
            tr2.setPadding(10, 0, 10, 0)
            tr2.addView(lp)
            tr2.addView(thumbnail)
            tr2.addView(tytul)

            if(tryb == items.GAME) {
                data.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT)
                data.text = "${gra.rok_wydania}"
                data.setPadding(20, 15, 20, 15)
                data.setTag(R.string.key, lista_gier[i].id)
                data.setOnClickListener { rozwin_gre(data) }
                tr2.addView(data)
            }
            tabela.addView(tr2, tr2Params)
        }
    }

    fun sortuj(v:View) {
        sortuj_gry(v.id)
    }

    fun rozwin_gre(v: View) {
        val i = Intent(this, KonkretnaGra::class.java)
        i.putExtra("id", v.getTag(R.string.key) as Int)
        startActivity(i)
    }
}