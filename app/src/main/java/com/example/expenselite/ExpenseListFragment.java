package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.app.Activity;       // Sadrži konstantu RESULT_OK – signal da je dodavanje troška uspešno završeno
import android.content.Intent;     // Poruka kojom se pokreće druga aktivnost (AddExpenseActivity)
import android.os.Bundle;          // Sadrži sačuvano stanje fragmenta pri rotaciji ili obnavljanju
import android.view.LayoutInflater; // Pretvara XML izgled u View objekte
import android.view.View;           // Osnovna klasa svih UI elemenata
import android.view.ViewGroup;      // Kontejner koji sadrži druge View-ove
import android.widget.TextView;     // Element za prikazivanje teksta

import androidx.activity.result.ActivityResultLauncher;                    // Savremeni API za pokretanje aktivnosti i primanje rezultata
import androidx.activity.result.contract.ActivityResultContracts;          // Ugovor koji opisuje tip aktivnosti i rezultata
import androidx.appcompat.app.AlertDialog;                                 // Dijalog za potvrdu brisanja
import androidx.fragment.app.Fragment;                                     // Osnovna klasa za fragment – "podekran" unutar aktivnosti
import androidx.recyclerview.widget.LinearLayoutManager;                   // Raspored koji prikazuje redove vertikalno, jedan ispod drugog
import androidx.recyclerview.widget.RecyclerView;                          // Lista koja reciklira redove radi efikasnosti

import com.google.android.material.button.MaterialButtonToggleGroup;       // Grupa dugmića za izbor perioda (Dan / Sedmica / Mesec)
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Plutajuće dugme "+" za dodavanje troška

import java.util.ArrayList; // Implementacija liste sa promenljivim brojem elemenata
import java.util.Calendar;  // Klasa za rad sa datumima i vremenom
import java.util.List;      // Interfejs koji opisuje listu

// Fragment koji prikazuje listu troškova za izabrani vremenski period (dan, sedmicu ili mesec).
// Sadrži ukupan iznos, dugmiće za filter perioda, samu listu i plutajuće dugme za dodavanje.
public class ExpenseListFragment extends Fragment {

    private ExpenseDao expenseDao;     // Objekat za komunikaciju sa bazom podataka
    private ExpenseAdapter adapter;    // Adapter koji vezuje listu Expense objekata za RecyclerView
    private TextView textTotal;        // Tekstualno polje koje prikazuje ukupan iznos troškova
    private TextView textEmpty;        // Tekstualno polje koje se prikazuje kada nema troškova za period
    private RecyclerView recyclerView; // Lista koja prikazuje troškove
    private long currentFromMs;        // Unix timestamp (milisekunde) od kog se prikazuju troškovi

    // Launcher mora biti registrovan pre nego što se fragment prikači na aktivnost (pre onAttach),
    // jer Android Result API zahteva rano registrovanje.
    // Callback se poziva kada se AddExpenseActivity zatvori:
    // - ako je rezultat RESULT_OK (trošak je snimljen), lista se osvežava
    // - ako je korisnik pritisnuo Nazad bez snimanja, lista se ne menja
    private final ActivityResultLauncher<Intent> addExpenseLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) { // Ako je trošak uspešno snimljen
                loadExpenses(); // Ponovo učitava listu da bi se prikazao novi trošak
            }
        });

    // Kreira i vraća View za ovaj fragment iz XML fajla.
    // Nema logike ovde – reference na View-ove se postavljaju u onViewCreated kada je stablo Views-ova potpuno formirano.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false); // Pretvara XML u View i vraća ga
    }

    // Poziva se nakon što je View potpuno kreiran. Kreira DAO, pronalazi Views-ove po ID-u,
    // delegira podešavanje trima pomoćnim metodama, postavlja inicijalni filter na početak danas
    // i učitava troškove za taj period.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);          // Obavezno – poziva roditeljsku metodu
        expenseDao    = new ExpenseDao(requireContext());        // Kreira DAO za pristup bazi podataka
        textTotal     = view.findViewById(R.id.text_total);     // Pronalazi polje za ukupan iznos
        textEmpty     = view.findViewById(R.id.text_empty);     // Pronalazi polje za poruku "Nema troškova"
        recyclerView  = view.findViewById(R.id.recycler_expenses); // Pronalazi RecyclerView za listu

        setupRecyclerView();       // Podešava RecyclerView sa adapterom i layoutom
        setupFilterToggle(view);   // Podešava dugmiće Dan/Sedmica/Mesec
        setupFab(view);            // Podešava plutajuće dugme "+"

        currentFromMs = getStartOfDay(); // Postavlja filter na početak hoje (ponoć)
        loadExpenses();                  // Učitava troškove za danas
    }

    // Kreira adapter sa praznom listom i vezuje showDeleteDialog za dugi pritisak (brisanje).
    // Vezuje LinearLayoutManager (vertikalni raspored) za RecyclerView i postavlja adapter.
    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(new ArrayList<>(), this::showDeleteDialog); // Novi adapter sa praznom listom
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext())); // Vertikalni raspored stavki
        recyclerView.setAdapter(adapter); // Vezuje adapter za RecyclerView
    }

    // Pronalazi grupu dugmića za filter, programski bira "Dan" kao početni izbor,
    // i registruje listener. Guard na isChecked sprečava dvostruko učitavanje:
    // listener se poziva i za dugme koje se odznačuje i za dugme koje se označava – bez guard-a bi se lista
    // učitavala dva puta pri svakom kliku.
    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter); // Pronalazi grupu dugmića
        toggle.check(R.id.btn_day); // Programski označava dugme "Dan" kao inicijalni izbor
        // Listener se aktivira i za označavanje i za odznačavanje dugmića;
        // guard na isChecked osigurava da reagujemo samo jednom po tapnuću
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId); // Poziva se samo kada se dugme OZNAČAVA (ne i odznačava)
        });
    }

    // Vezuje klik na plutajuće dugme "+" za otvaranje AddExpenseActivity.
    // Koristi addExpenseLauncher (ne startActivity) da bi se callback pozvao kada se aktivnost zatvori.
    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_add); // Pronalazi plutajuće dugme "+"
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddExpenseActivity.class); // Kreira nameru za otvaranje ekrana za unos
            addExpenseLauncher.launch(intent); // Otvara AddExpenseActivity i čeka na rezultat
        });
    }

    // Prevodi ID označenog dugmića u Unix timestamp početka tog perioda,
    // čuva ga u currentFromMs i ponovo učitava listu za novi period.
    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day)        currentFromMs = getStartOfDay();   // Dan: od ponoći danas
        else if (buttonId == R.id.btn_week)  currentFromMs = getStartOfWeek();  // Sedmica: od ponedeljka ove sedmice
        else                                 currentFromMs = getStartOfMonth(); // Mesec: od prvog u mesecu
        loadExpenses(); // Osvežava listu sa novim filterom
    }

    // Jedina tačka za osvežavanje liste i ukupnog iznosa nakon bilo kakve promene podataka.
    // Dohvata troškove iz baze za trenutni period, predaje listu adapteru,
    // i menja vidljivost: ako je lista prazna prikazuje poruku "Nema troškova", inače prikazuje listu.
    private void loadExpenses() {
        List<Expense> expenses = expenseDao.getExpenses(currentFromMs); // Dohvata troškove za trenutni period
        adapter.setExpenses(expenses); // Predaje novu listu adapteru i osvežava prikaz
        boolean empty = expenses.isEmpty(); // true ako nema troškova za izabrani period
        // Prikazuje poruku o praznoj listi ili samu listu – nikad oba istovremeno
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);   // Poruka "Nema troškova" vidljiva samo kad je lista prazna
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE); // Lista vidljiva samo kad ima troškova
        updateTotal(); // Ažurira prikaz ukupnog iznosa
    }

    // Pita DAO za zbir svih iznosa u trenutnom periodu i prikazuje ga u textTotal
    // koristeći format iz resursa (npr. "Ukupno: 42.00 €").
    private void updateTotal() {
        double total = expenseDao.getTotalAmount(currentFromMs);        // Dohvata ukupan iznos iz baze
        textTotal.setText(getString(R.string.total_format, total));     // Formatira i prikazuje tekst sa iznosom
    }

    // Prikazuje dijalog za potvrdu brisanja pre nego što se trošak zaista obriše.
    // Pozitivno dugme ("Obriši") poziva deleteExpense; negativno dugme ("Otkaži") zatvara dijalog bez akcije.
    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_title)           // Naslov dijaloga (npr. "Brisanje troška")
            .setMessage(R.string.delete_message)       // Tekst pitanja (npr. "Obrisati ovaj trošak?")
            .setPositiveButton(R.string.btn_confirm, (d, w) -> deleteExpense(expense)) // Dugme "Obriši"
            .setNegativeButton(R.string.btn_cancel, null) // Dugme "Otkaži" – null znači samo zatvori dijalog
            .show(); // Prikazuje dijalog korisniku
    }

    // Briše trošak iz baze po njegovom ID-u, pa ponovo učitava listu
    // da bi obrisani trošak nestao sa ekrana i ukupan iznos bio ažuriran.
    private void deleteExpense(Expense expense) {
        expenseDao.deleteExpense(expense.getId()); // Briše red iz baze podataka
        loadExpenses();                             // Osvežava listu i ukupan iznos
    }

    // Poziva se kada Android uklanja View ovog fragmenta (npr. korisnik navigira na drugi ekran).
    // Zatvara DAO da ne bi curela SQLite veza.
    @Override
    public void onDestroyView() {
        super.onDestroyView();  // Obavezno – poziva roditeljsku metodu
        expenseDao.close();     // Oslobađa SQLite vezu
    }

    // Uzima trenutni momenat, setuje sate, minute, sekunde i milisekunde na 0
    // i vraća rezultat kao Unix timestamp – tj. ponoć od danas.
    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();          // Kreira Calendar sa trenutnim vremenom
        cal.set(Calendar.HOUR_OF_DAY, 0);              // Postavlja sat na 0 (ponoć)
        cal.set(Calendar.MINUTE, 0);                   // Postavlja minute na 0
        cal.set(Calendar.SECOND, 0);                   // Postavlja sekunde na 0
        cal.set(Calendar.MILLISECOND, 0);              // Postavlja milisekunde na 0
        return cal.getTimeInMillis();                   // Vraća Unix timestamp u milisekundama (ponoć danas)
    }

    // Izračunava timestamp za ponoć ponedeljka koji je startovao tekuću sedmicu.
    // Calendar.DAY_OF_WEEK daje 1 (ned) do 7 (sub). Sabiranjem 5 i uzimanjem mod 7
    // dobija se 0 (pon) do 6 (ned) – broj dana od ponedeljka, nezavisno od lokale.
    // Oduzimanjem tog broja od danas dolazi se na ponedeljak, koji se zatim nulluje na ponoć.
    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance(); // Kreira Calendar sa trenutnim vremenom
        // Calendar.DAY_OF_WEEK: 1=ned ... 7=sub. Formulom (vrednost + 5) % 7 dobijamo 0=pon ... 6=ned
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Broj dana koji su prošli od poslednjeg ponedeljka
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday); // Pomera datum unazad na ponedeljak
        cal.set(Calendar.HOUR_OF_DAY, 0);                 // Postavlja sat na 0 (ponoć)
        cal.set(Calendar.MINUTE, 0);                      // Postavlja minute na 0
        cal.set(Calendar.SECOND, 0);                      // Postavlja sekunde na 0
        cal.set(Calendar.MILLISECOND, 0);                 // Postavlja milisekunde na 0
        return cal.getTimeInMillis();                      // Vraća Unix timestamp za ponoć ponedeljka
    }

    // Isti princip kao getStartOfDay, ali postavlja i DAY_OF_MONTH na 1,
    // što daje ponoć prvog dana tekućeg meseca.
    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance(); // Kreira Calendar sa trenutnim vremenom
        cal.set(Calendar.DAY_OF_MONTH, 1);    // Postavlja dan na 1 (prvi u mesecu)
        cal.set(Calendar.HOUR_OF_DAY, 0);     // Postavlja sat na 0 (ponoć)
        cal.set(Calendar.MINUTE, 0);          // Postavlja minute na 0
        cal.set(Calendar.SECOND, 0);          // Postavlja sekunde na 0
        cal.set(Calendar.MILLISECOND, 0);     // Postavlja milisekunde na 0
        return cal.getTimeInMillis();          // Vraća Unix timestamp za ponoć prvog dana meseca
    }
}
