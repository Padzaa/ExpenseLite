package com.example.expenselite; // Paket (folder) u kome se nalazi ova klasa

// Klasa koja predstavlja jedan trošak – služi kao "šablon" (model) za sve troškove u aplikaciji.
// Svaki objekat ove klase čuva podatke o jednom trošku: naziv, iznos, kategoriju i vreme unosa.
public class Expense {
    private int id;          // Jedinstven broj koji baza podataka automatski dodeljuje svakom trošku
    private String name;     // Naziv troška koji je korisnik uneo (npr. "kafa", "gorivo")
    private double amount;   // Iznos troška u evrima – decimalni broj (npr. 3.50)
    private String category; // Kategorija kojoj trošak pripada (npr. "Hrana", "Transport")
    private long createdAt;  // Tačno vreme unosa, izraženo u milisekundama od 1. januara 1970. (Unix timestamp)

    // Konstruktor koji se koristi kada korisnik unosi NOVI trošak.
    // id se ne zadaje – baza podataka će ga sama dodeliti pri upisu u tabelu.
    // createdAt se automatski postavlja na trenutno vreme sistemskim satom.
    public Expense(String name, double amount, String category) {
        this.name = name;                           // Čuva naziv koji je korisnik uneo
        this.amount = amount;                       // Čuva iznos koji je korisnik uneo
        this.category = category;                   // Čuva kategoriju koju je korisnik izabrao
        this.createdAt = System.currentTimeMillis(); // Beleži tačno vreme kreiranja u milisekundama
    }

    // Konstruktor koji se koristi kada se trošak ČITA iz baze podataka.
    // Sva polja su već poznata jer su prethodno sačuvana u bazi.
    public Expense(int id, String name, double amount, String category, long createdAt) {
        this.id = id;               // Postavlja ID koji je baza podataka dodelila ovom redu
        this.name = name;           // Postavlja sačuvani naziv troška
        this.amount = amount;       // Postavlja sačuvani iznos
        this.category = category;   // Postavlja sačuvanu kategoriju
        this.createdAt = createdAt; // Postavlja sačuvano vreme kreiranja
    }

    // Metode za čitanje vrednosti (getteri) – dozvoljavaju ostalim klasama da pročitaju polja
    public int getId()          { return id; }         // Vraća jedinstveni ID troška iz baze
    public String getName()     { return name; }       // Vraća naziv troška
    public double getAmount()   { return amount; }     // Vraća iznos troška
    public String getCategory() { return category; }   // Vraća kategoriju kojoj trošak pripada
    public long getCreatedAt()  { return createdAt; }  // Vraća vreme kreiranja u milisekundama
}
