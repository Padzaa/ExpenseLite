package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.content.Context;                  // Potrebno za pristup Android sistemskim resursima (fajl-sistem, resursi)
import android.database.sqlite.SQLiteDatabase;   // Klasa koja predstavlja samu SQLite bazu podataka
import android.database.sqlite.SQLiteOpenHelper; // Nadklasa koja automatski upravlja kreiranjem i verzijama baze

// Klasa koja upravlja bazom podataka: kreira je pri prvom pokretanju i nadograđuje je kada se promeni verzija.
// Nasleđuje SQLiteOpenHelper – Android-ov alat koji olakšava rad sa SQLite bazama.
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "expense_lite.db"; // Ime fajla baze podataka koji Android čuva na uređaju
    public static final int DB_VERSION = 1;                  // Verzija šeme baze; kada se poveća, Android poziva onUpgrade

    public static final String TABLE_EXPENSES = "expenses";   // Ime tabele u kojoj se čuvaju troškovi
    public static final String COL_ID         = "id";         // Ime kolone za jedinstveni redni broj (primarni ključ)
    public static final String COL_NAME       = "name";       // Ime kolone za naziv troška
    public static final String COL_AMOUNT     = "amount";     // Ime kolone za iznos troška
    public static final String COL_CATEGORY   = "category";   // Ime kolone za kategoriju troška
    public static final String COL_CREATED_AT = "created_at"; // Ime kolone za vreme kreiranja (ceo broj – milisekunde)

    // Konstruktor – prosleđuje naziv baze i broj verzije Android sistemu.
    // Android na osnovu toga odlučuje: da li da kreira novu bazu ili da pokrene onUpgrade.
    // Treći argument (null) znači: koristi podrazumevanu implementaciju kursora za čitanje redova.
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // Poziva se samo jednom – kada aplikacija prvi put pokuša da otvori bazu, a ona još ne postoji na uređaju.
    // Izvršava SQL naredbu koja kreira tabelu sa svim kolonama:
    // - id       : ceo broj koji se automatski uvećava za 1 pri svakom novom redu (1, 2, 3...)
    // - name     : tekst koji ne sme biti prazan (NOT NULL)
    // - amount   : decimalni broj (REAL je SQLite tip za decimale, NOT NULL)
    // - category : tekst koji ne sme biti prazan
    // - created_at: ceo broj koji čuva milisekunde – to omogućava brzo poređenje datuma bez parsiranja
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(                                                // Izvršava SQL tekst direktno na bazi podataka
            "CREATE TABLE " + TABLE_EXPENSES + " (" +             // Kreira tabelu "expenses"
            COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " + // id: primarni ključ, automatski se povećava
            COL_NAME       + " TEXT NOT NULL, " +                 // name: obavezan tekstualni unos
            COL_AMOUNT     + " REAL NOT NULL, " +                 // amount: obavezan decimalni broj
            COL_CATEGORY   + " TEXT NOT NULL, " +                 // category: obavezan tekstualni unos
            COL_CREATED_AT + " INTEGER NOT NULL)"                 // created_at: obavezan ceo broj (milisekunde)
        );
    }

    // Poziva se automatski kada se DB_VERSION poveća u kodu – to znači da se promenila šema baze.
    // Briše postojeću tabelu zajedno sa svim podacima, a zatim je kreira ponovo pozivanjem onCreate.
    // Brisanje svih podataka je prihvatljivo u fazi razvoja aplikacije – kod ozbiljne migracije čuvali bismo podatke.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES); // Briše tabelu "expenses" ako postoji
        onCreate(db);                                          // Poziva onCreate da kreira tabelu ponovo sa novom šemom
    }
}
