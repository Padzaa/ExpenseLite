package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.os.Bundle;          // Sadrži sačuvano stanje aktivnosti (npr. pri rotaciji ekrana)
import android.view.MenuItem;      // Predstavlja stavku u meniju (npr. dugme za povratak u toolbaru)
import android.widget.ArrayAdapter; // Adapter koji vezuje niz tekstova za padajuću listu

import androidx.appcompat.app.AppCompatActivity; // Osnovna klasa za Android aktivnost sa podrškom za AppCompat

import com.google.android.material.appbar.MaterialToolbar;          // Material Design traka sa naslovom na vrhu ekrana
import com.google.android.material.textfield.TextInputEditText;     // Polje za unos teksta unutar TextInputLayout-a
import com.google.android.material.textfield.TextInputLayout;       // Kontejner oko polja za unos koji prikazuje grešku ispod

// Aktivnost za unos novog troška. Prikazuje formu sa tri polja (naziv, iznos, kategorija)
// i dugmetom "Sačuvaj". Validira unos i snima trošak u bazu kada korisnik pritisne dugme.
public class AddExpenseActivity extends AppCompatActivity {

    // Niz dostupnih kategorija troškova – prikazuje se kao padajuća lista korisniku
    private static final String[] CATEGORIES = {"Food", "Hygiene", "Transport", "Entertainment"};

    private TextInputLayout layoutName, layoutAmount, layoutCategory; // Kontejneri polja – koriste se za prikaz grešaka ispod polja
    private TextInputEditText editName, editAmount;                    // Polja za unos teksta: naziv i iznos
    private com.google.android.material.textfield.MaterialAutoCompleteTextView dropdownCategory; // Padajuća lista za izbor kategorije
    private ExpenseDao expenseDao; // Objekat za komunikaciju sa bazom podataka

    // Poziva se kada Android kreira ovu aktivnost i prikazuje je na ekranu.
    // Učitava izgled ekrana, kreira DAO, pokrenuje sve pomoćne metode za podešavanje,
    // i vezuje dugme "Sačuvaj" za metodu onSaveClicked.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);            // Obavezno – poziva onCreate roditeljske klase
        setContentView(R.layout.activity_add_expense); // Učitava XML izgled ovog ekrana

        expenseDao = new ExpenseDao(this);   // Pravi DAO za pristup bazi podataka
        setupToolbar();                      // Podešava toolbar sa naslovom i strelicom za povratak
        bindViews();                         // Pronalazi sva polja forme po ID-u i čuva reference
        setupCategoryDropdown();             // Puni padajuću listu kategorijama

        // Vezuje pritisak dugmeta "Sačuvaj" za metodu onSaveClicked
        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
    }

    // Registruje MaterialToolbar kao action bar i uključuje strelicu za povratak (←) u gornjem levom uglu.
    // Provjera getSupportActionBar() != null je zaštitna – ne bi trebalo da bude null ovde,
    // ali API vraća nullable tip pa kompajler zahteva proveru.
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);  // Pronalazi toolbar u XML izgledu
        setSupportActionBar(toolbar);                           // Registruje ga kao zvanični action bar aktivnosti
        if (getSupportActionBar() != null) {                   // Provera pre upotrebe (zaštita od null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);          // Prikazuje strelicu ← za povratak
            getSupportActionBar().setTitle(R.string.title_add_expense);     // Postavlja naslov toolbara
        }
    }

    // Pronalazi sve elemente forme po njihovim ID-jevima i čuva ih u polju klase.
    // Na ovaj način ostale metode mogu koristiti ova polja direktno, bez ponovnog traženja po ID-u.
    private void bindViews() {
        layoutName     = findViewById(R.id.layout_name);        // Kontejner za polje "naziv" – pokazuje grešku
        layoutAmount   = findViewById(R.id.layout_amount);      // Kontejner za polje "iznos" – pokazuje grešku
        layoutCategory = findViewById(R.id.layout_category);    // Kontejner za polje "kategorija" – pokazuje grešku
        editName       = findViewById(R.id.edit_name);          // Samo polje za unos naziva
        editAmount     = findViewById(R.id.edit_amount);        // Samo polje za unos iznosa
        dropdownCategory = findViewById(R.id.dropdown_category); // Padajuća lista za izbor kategorije
    }

    // Omotava niz CATEGORIES u ArrayAdapter koristeći Android-ov ugrađeni izgled za jednoredne stavke.
    // Vezuje adapter za MaterialAutoCompleteTextView, što prikazuje padajuću listu kada korisnik tapne na polje.
    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,                                            // Kontekst – ova aktivnost
            android.R.layout.simple_dropdown_item_1line,    // Ugrađeni Android izgled za jednorednu stavku liste
            CATEGORIES                                       // Niz kategorija koji popunjava listu
        );
        dropdownCategory.setAdapter(adapter); // Vezuje adapter za padajuću listu
    }

    // Poziva se kada korisnik pritisne dugme "Sačuvaj".
    // Najpre proverava validnost unosa i odmah izlazi ako nešto nije ispravno.
    // Ako je unos validan, čita tekst iz svakog polja, pravi Expense objekat i snima ga u bazu.
    // Postavlja rezultat na RESULT_OK (signal za ExpenseListFragment da je trošak zaista snimljen),
    // pa zatvara aktivnost i vraća se na prethodni ekran.
    private void onSaveClicked() {
        if (!validateInputs()) return; // Ako validacija ne prođe, odmah izlazi bez snimanja

        String name     = editName.getText().toString().trim();        // Čita naziv i uklanja razmake sa strane
        // parseDouble je bezbedan ovde jer je validateInputs() već potvrdila da je broj validan i pozitivan
        double amount   = Double.parseDouble(editAmount.getText().toString().trim()); // Pretvara tekst iznosa u decimalni broj
        String category = dropdownCategory.getText().toString().trim(); // Čita izabranu kategoriju

        expenseDao.addExpense(new Expense(name, amount, category)); // Pravi novi Expense i snima ga u bazu
        // Signal za ExpenseListFragment (preko ActivityResultLauncher-a) da je trošak zaista snimljen, a ne samo pritisnut "Nazad"
        setResult(RESULT_OK); // Označava da je aktivnost završila uspešno
        finish();             // Zatvara ovu aktivnost i vraća se na prethodni ekran
    }

    // Proverava sva tri polja u nizu – ne staje na prvoj grešci, pa korisnik vidi sve greške odjednom.
    // Za svako polje: ako je prazno ili neispravno – setError prikazuje crvenu poruku ispod polja, valid = false.
    //                 ako je ispravno – setError(null) briše prethodnu grešku (ako je bilo).
    // Na kraju vraća acumulirani flag koji je true samo ako su sva polja ispravna.
    private boolean validateInputs() {
        boolean valid = true; // Pretpostavljamo da je sve ispravno; staje na false ako se nađe greška

        String name = editName.getText().toString().trim(); // Čita naziv (uklanja prazan prostor)
        if (name.isEmpty()) {                              // Ako je naziv prazan
            layoutName.setError(getString(R.string.error_name)); // Prikazuje crvenu grešku: "Unesite naziv"
            valid = false;                                 // Označava da validacija nije prošla
        } else {
            layoutName.setError(null); // Briše prethodnu grešku ako je naziv ispravan
        }

        String amountStr = editAmount.getText().toString().trim(); // Čita iznos kao tekst
        if (amountStr.isEmpty()) {                                 // Ako je polje iznosa prazno
            layoutAmount.setError(getString(R.string.error_amount_empty)); // "Unesite iznos"
            valid = false;
        } else if (!isValidAmount(amountStr)) {                    // Ako nije validan pozitivni broj
            layoutAmount.setError(getString(R.string.error_amount_invalid)); // "Unesite validan pozitivan iznos"
            valid = false;
        } else {
            layoutAmount.setError(null); // Briše prethodnu grešku ako je iznos ispravan
        }

        String category = dropdownCategory.getText().toString().trim(); // Čita izabranu kategoriju
        if (category.isEmpty()) {                                       // Ako kategorija nije izabrana
            layoutCategory.setError(getString(R.string.error_category)); // "Izaberite kategoriju"
            valid = false;
        } else {
            layoutCategory.setError(null); // Briše prethodnu grešku ako je kategorija izabrana
        }

        return valid; // Vraća true samo ako su sva tri polja ispravna
    }

    // Pokušava da pretvori tekst u decimalni broj i proverava da li je veći od nule.
    // Ako parseDouble baci NumberFormatException (npr. korisnik uneo "abc"),
    // catch blok vraća false. Oba slučaja neispravnosti (nije broj i nije pozitivan) vraćaju false.
    private boolean isValidAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr) > 0; // Vraća true samo ako je broj i veći je od 0
        } catch (NumberFormatException e) {
            return false; // Tekst nije broj (npr. "abc") – vraća false
        }
    }

    // Poziva se kada korisnik tapne bilo koji element toolbara.
    // Proverava da li je tapnuta strelica za povratak (android.R.id.home) i zatvara aktivnost.
    // Bez ovog override-a, strelica bi koristila Android-ovo podrazumevano ponašanje i pravila probleme sa navigation stack-om.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // Ako je tapnuta strelica ← u toolbaru
            finish();    // Zatvara ovu aktivnost i vraća na prethodni ekran
            return true; // Označava da je događaj obrađen
        }
        return super.onOptionsItemSelected(item); // Za sve ostale stavke menija – standardno ponašanje
    }

    // Poziva se kada Android potpuno uništi ovu aktivnost (npr. korisnik ode s ekrana).
    // Zatvara DAO (i time SQLite vezu) – bez ovog bi veza ostala otvorena i cureli bi resursi.
    @Override
    protected void onDestroy() {
        super.onDestroy();  // Obavezno – poziva onDestroy roditeljske klase
        expenseDao.close(); // Zatvara vezu sa bazom podataka
    }
}
