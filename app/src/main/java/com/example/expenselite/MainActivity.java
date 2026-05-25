package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.os.Bundle; // Sadrži sačuvano stanje aktivnosti (koristi se pri rotaciji ekrana)

import androidx.appcompat.app.AppCompatActivity; // Osnovna klasa za aktivnost sa podrškom za AppCompat i toolbar
import androidx.fragment.app.Fragment;           // Osnovna klasa za fragment – "podekran" unutar aktivnosti

import com.google.android.material.appbar.MaterialToolbar;           // Traka sa naslovom aplikacije (Material Design)
import com.google.android.material.bottomnavigation.BottomNavigationView; // Traka za navigaciju na dnu ekrana sa ikonama

// Glavna aktivnost – prvi ekran koji se prikazuje pri pokretanju aplikacije.
// Sadrži toolbar na vrhu, fragmenti ekran u sredini i navigacionu traku na dnu.
// Upravlja prebacivanjem između dva fragmenta: ExpenseListFragment i ChartFragment.
public class MainActivity extends AppCompatActivity {

    private ExpenseListFragment expenseListFragment; // Fragment koji prikazuje listu troškova
    private ChartFragment chartFragment;             // Fragment koji prikazuje kružni grafikon

    // Poziva se kada Android kreira ovu aktivnost i prikazuje je na ekranu.
    // Učitava XML izgled, podešava toolbar, kreira ili obnavlja fragmente,
    // i podešava navigacionu traku na dnu ekrana.
    // savedInstanceState je null pri prvom pokretanju, a sadrži podatke pri rotaciji ekrana.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   // Obavezno – poziva onCreate roditeljske klase
        setContentView(R.layout.activity_main); // Učitava XML izgled ovog ekrana

        MaterialToolbar toolbar = findViewById(R.id.toolbar); // Pronalazi toolbar po ID-u
        setSupportActionBar(toolbar); // Registruje toolbar kao zvanični action bar aktivnosti

        // Ako je savedInstanceState null, aktivnost se kreira prvi put – pravimo fragmente od nule.
        // Ako nije null, Android je već obnovio fragmente (npr. posle rotacije) – samo pronalazimo reference.
        if (savedInstanceState == null) {
            initFragments();    // Prvi put – kreira nove fragmente i dodaje ih u ekran
        } else {
            restoreFragments(); // Posle rotacije – pronalazi već obnovljene fragmente po tagu
        }

        setupBottomNav(); // Podešava klikove na navigacionu traku na dnu
    }

    // Kreira nove instance oba fragmenta i dodaje ih u isti kontejner u jednoj transakciji.
    // ChartFragment se odmah skriva da bi lista bila vidljiva po default-u.
    // Oba fragmenta dobijaju string tagove ("list" i "chart") kako bi se mogli pronaći posle rotacije.
    private void initFragments() {
        expenseListFragment = new ExpenseListFragment(); // Kreira novi fragment liste troškova
        chartFragment       = new ChartFragment();       // Kreira novi fragment grafikona

        getSupportFragmentManager().beginTransaction()                  // Počinje transakciju fragmenata
            .add(R.id.fragment_container, expenseListFragment, "list") // Dodaje fragment liste sa tagom "list"
            .add(R.id.fragment_container, chartFragment, "chart")      // Dodaje fragment grafikona sa tagom "chart"
            .hide(chartFragment)                                        // Skriva grafikon – lista je vidljiva po default-u
            .commit();                                                  // Primenjuje transakciju
    }

    // Posle rotacije ekrana Android sam obnavlja fragmente koji su bili prikazani.
    // Ova metoda samo pronalazi te već obnovljene instance po tagovima i čuva reference u poljima,
    // jer su stare reference postale nevažeće kada je aktivnost bila uništena i ponovo kreirana.
    private void restoreFragments() {
        expenseListFragment = (ExpenseListFragment)
            getSupportFragmentManager().findFragmentByTag("list");  // Pronalazi fragment liste po tagu "list"
        chartFragment = (ChartFragment)
            getSupportFragmentManager().findFragmentByTag("chart"); // Pronalazi fragment grafikona po tagu "chart"
    }

    // Pronalazi navigacionu traku i registruje listener za klikove na stavke.
    // Klik na "Troškovi" prikazuje listu i skriva grafikon; klik na "Grafikon" radi obrnuto.
    // Vraća true da potvrdi da je klik obrađen; false za nepoznate stavke (ne bi trebalo da se desi).
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav); // Pronalazi navigacionu traku po ID-u
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();              // Uzima ID tapnute stavke
            if (id == R.id.nav_expenses) {          // Ako je tapnuta stavka "Troškovi"
                showFragment(expenseListFragment, chartFragment); // Prikazuje listu, skriva grafikon
                return true;                        // Potvrđuje da je klik obrađen
            } else if (id == R.id.nav_chart) {      // Ako je tapnuta stavka "Grafikon"
                showFragment(chartFragment, expenseListFragment); // Prikazuje grafikon, skriva listu
                return true;                        // Potvrđuje da je klik obrađen
            }
            return false; // Nepoznata stavka – nije obrađena (ne bi trebalo da se desi)
        });
    }

    // Prikazuje jedan fragment i skriva drugi u jednoj transakciji.
    // Koristi se show/hide umesto replace jer replace uništava i ponovo kreira skriveni fragment,
    // čime se gube stanje skrolovanja i izabrani filter – show/hide čuva to stanje.
    private void showFragment(Fragment show, Fragment hide) {
        getSupportFragmentManager().beginTransaction() // Počinje transakciju fragmenata
            .show(show) // Prikazuje izabrani fragment
            .hide(hide) // Skriva drugi fragment (ali ne uništava ga – čuva stanje)
            .commit();  // Primenjuje transakciju
    }
}
