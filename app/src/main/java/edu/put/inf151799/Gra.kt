package edu.put.inf151799

enum class items{
    GAME, EXPANSION
}

class Gra {
    var id: Int? = null
    var tytul: String? = null
    var thumbnail: String? = null
    var rok_wydania: Int? = null
    var ranking: Int? = null
    var czy_dodatek: Int = 0
    var zdjecia: String = ""

    constructor()

    constructor(
        id: Int?, tytul: String?, thumbnail: String?, rok_wydania: Int?, ranking: Int?, czy_dodatek: Int, zdjecia: String
    ) {
        this.id = id
        this.tytul = tytul
        this.thumbnail = thumbnail
        this.rok_wydania = rok_wydania
        this.ranking = ranking
        this.czy_dodatek = czy_dodatek
        this.zdjecia = zdjecia
    }

    constructor(
        id: Int?, tytul: String?, thumbnail: String?, rok_wydania: Int?, ranking: Int?, czy_dodatek: Int
    ) {
        this.id = id
        this.tytul = tytul
        this.thumbnail = thumbnail
        this.rok_wydania = rok_wydania
        this.ranking = ranking
        this.czy_dodatek = czy_dodatek
    }
}