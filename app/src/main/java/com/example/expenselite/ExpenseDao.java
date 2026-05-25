package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.content.ContentValues;            // Android-ov rečnik (mapa) za bezbedno pakovanje vrednosti u SQL upite
import android.content.Context;                  // Potrebno za kreiranje DatabaseHelper-a
import android.database.Cursor;                  // Pokazivač koji se kreće kroz redove rezultata SQL upita
import android.database.sqlite.SQLiteDatabase;   // Klasa koja predstavlja otvorenu SQLite bazu podataka

import java.util.ArrayList;     // Implementacija liste sa promenljivim brojem elemenata
import java.util.LinkedHashMap; // Mapa koja čuva redosled unosa (važno za isti raspored boja u grafikonu)
import java.util.List;          // Interfejs koji opisuje listu elemenata
import java.util.Map;           // Interfejs koji opisuje mapu (ključ → vrednost)

// DAO (Data Access Object) – klasa koja sadrži sve operacije nad bazom podataka za troškove.
// Ostale klase ne znaju ništa o SQL-u – sve pozivaju samo metode ove klase.
public class ExpenseDao {

    private final DatabaseHelper dbHelper; // Pomaže u otvaranju baze podataka; čuva se kao polje da bi se mogao zatvoriti

    // Kreira DatabaseHelper koji registruje naziv i verziju baze kod Android sistema,
    // ali još ne otvara fajl baze – veza se otvara tek pri prvom pozivu getWritableDatabase() ili getReadableDatabase().
    public ExpenseDao(Context context) {
        dbHelper = new DatabaseHelper(context); // Pravi novi DatabaseHelper za zadati kontekst (npr. aktivnost)
    }

    // Otvara bazu za pisanje, pakuje sva polja troška u ContentValues mapu,
    // a zatim upisuje novi red u tabelu. Vraća ID novog reda koji je baza automatski dodelila, ili -1 ako upisivanje nije uspelo.
    public long addExpense(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();        // Otvara bazu u režimu pisanja
        ContentValues values = new ContentValues();                // Kreira praznu mapu za vrednosti kolona
        values.put(DatabaseHelper.COL_NAME,       expense.getName());       // Dodaje naziv troška u mapu
        values.put(DatabaseHelper.COL_AMOUNT,     expense.getAmount());     // Dodaje iznos troška u mapu
        values.put(DatabaseHelper.COL_CATEGORY,   expense.getCategory());   // Dodaje kategoriju u mapu
        values.put(DatabaseHelper.COL_CREATED_AT, expense.getCreatedAt());  // Dodaje vreme kreiranja u mapu
        return db.insert(DatabaseHelper.TABLE_EXPENSES, null, values);      // Upisuje red u tabelu i vraća novi ID
    }

    // Dohvata sve troškove čije je vreme kreiranja >= fromMs, sortirane od najnovijeg ka najstarijem.
    // Umesto direktnog umetanja fromMs u SQL tekst (što bi bio bezbednosni rizik), koristi se ? kao placeholder –
    // Android sam bezbedno zamenjuje ? sa stvarnom vrednošću i time sprečava SQL injection napad.
    public List<Expense> getExpenses(long fromMs) {
        List<Expense> list = new ArrayList<>();                             // Prazna lista u koju će se dodavati troškovi
        SQLiteDatabase db = dbHelper.getReadableDatabase();                 // Otvara bazu samo za čitanje
        String where = DatabaseHelper.COL_CREATED_AT + " >= ?";            // Uslov filtriranja: created_at >= fromMs
        String[] args = {String.valueOf(fromMs)};                           // Vrednost koja zamenjuje ? u uslovu
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_EXPENSES,          // Iz tabele "expenses"
            null,                                   // null = uzmi sve kolone
            where,                                  // WHERE uslov
            args,                                   // Vrednosti za ? placeholdere
            null,                                   // GROUP BY – ne grupišemo
            null,                                   // HAVING – nema
            DatabaseHelper.COL_CREATED_AT + " DESC" // ORDER BY created_at DESC – najnoviji prvi
        );
        while (cursor.moveToNext()) {               // Dok ima redova u rezultatu, pomeri se na sledeći
            list.add(cursorToExpense(cursor));       // Pretvori trenutni red u Expense objekat i dodaj ga u listu
        }
        cursor.close();                             // Oslobodi memoriju kursora – obavezno da ne bi curela memorija
        return list;                                // Vrati popunjenu listu troškova
    }

    // Briše tačno jedan red čiji id odgovara zadatom.
    // Koristi ? placeholder da bi se osiguralo da se briše samo taj jedan red, a ne više.
    public void deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase(); // Otvara bazu za pisanje (brisanje menja bazu)
        db.delete(
            DatabaseHelper.TABLE_EXPENSES,                  // Iz tabele "expenses"
            DatabaseHelper.COL_ID + " = ?",                 // WHERE id = ?
            new String[]{String.valueOf(id)}                // Vrednost koja zamenjuje ?
        );
    }

    // Izvršava SELECT SUM(amount) sa filtrom za vremenski period.
    // SUM uvek vraća tačno jedan red, ali ako nijedan trošak ne odgovara filtru,
    // taj red sadrži NULL umesto 0. cursor.isNull(0) sprečava čitanje NULL-a kao 0.0
    // što bi bio tihi pogrešan rezultat.
    public double getTotalAmount(long fromMs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // Otvara bazu samo za čitanje
        String query =
            "SELECT SUM(" + DatabaseHelper.COL_AMOUNT + ")" +             // Sabira sve iznose
            " FROM " + DatabaseHelper.TABLE_EXPENSES +                     // Iz tabele "expenses"
            " WHERE " + DatabaseHelper.COL_CREATED_AT + " >= ?";           // Samo za zadati period
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fromMs)}); // Izvršava upit sa vrednosću filtera
        double total = 0;                                                   // Podrazumevana vrednost ako nema podataka
        if (cursor.moveToFirst() && !cursor.isNull(0)) {                    // Ako postoji rezultat i nije NULL
            total = cursor.getDouble(0);                                    // Čita zbir iz prve (i jedine) kolone rezultata
        }
        cursor.close();                                                     // Oslobađa memoriju kursora
        return total;                                                       // Vraća ukupan iznos (ili 0 ako nema troškova)
    }

    // Izvršava GROUP BY category upit koji sabira troškove po kategorijama za zadati vremenski period.
    // Vraća LinkedHashMap (ne HashMap) jer čuva redosled unosa – bitan je jer se isti redosled
    // koristi i u grafikonu i u legendi, pa boje moraju biti usklađene između kriški i oznaka.
    public Map<String, Double> getExpensesForChart(long fromMs) {
        Map<String, Double> map = new LinkedHashMap<>();     // Mapa koja čuva redosled: kategorija → ukupan iznos
        SQLiteDatabase db = dbHelper.getReadableDatabase();  // Otvara bazu samo za čitanje
        String query =
            "SELECT " + DatabaseHelper.COL_CATEGORY +                      // Uzima naziv kategorije
            ", SUM(" + DatabaseHelper.COL_AMOUNT + ")" +                   // I zbir iznosa za tu kategoriju
            " FROM " + DatabaseHelper.TABLE_EXPENSES +                     // Iz tabele "expenses"
            " WHERE " + DatabaseHelper.COL_CREATED_AT + " >= ?" +          // Samo za zadati period
            " GROUP BY " + DatabaseHelper.COL_CATEGORY;                    // Grupiše redove po kategoriji
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fromMs)}); // Izvršava upit
        while (cursor.moveToNext()) {                        // Prolazi kroz svaki red rezultata (svaka kategorija)
            map.put(cursor.getString(0), cursor.getDouble(1)); // Dodaje par kategorija → zbir u mapu
        }
        cursor.close();                                      // Oslobađa memoriju kursora
        return map;                                          // Vraća mapu sa zbirovima po kategorijama
    }

    // Zatvara SQLite vezu. Mora se pozvati u onDestroyView/onDestroy komponente koja koristi ovaj DAO,
    // jer neoslobođena veza troši sistemske resurse (curenje resursa / resource leak).
    public void close() {
        dbHelper.close(); // Zatvara DatabaseHelper, što zatvara i vezu sa bazom
    }

    // Čita jedan red iz baze podataka na trenutnoj poziciji kursora i pretvara ga u Expense objekat.
    // getColumnIndexOrThrow se koristi umesto getColumnIndex jer baca izuzetak sa jasnom porukom
    // ako kolona ne postoji (npr. zbog greške u pisanju), umesto da tiho vrati -1 i pročita pogrešnu kolonu.
    private Expense cursorToExpense(Cursor cursor) {
        int id          = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));         // Čita ID iz kolone "id"
        String name     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));    // Čita naziv iz kolone "name"
        double amount   = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));  // Čita iznos iz kolone "amount"
        String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY)); // Čita kategoriju
        long createdAt  = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)); // Čita vreme kreiranja
        return new Expense(id, name, amount, category, createdAt); // Pravi novi Expense objekat sa pročitanim vrednostima
    }
}
