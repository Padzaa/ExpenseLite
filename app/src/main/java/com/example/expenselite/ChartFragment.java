package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.graphics.Color;     // Klasa za kreiranje boja od RGB vrednosti
import android.os.Bundle;          // Sadrži sačuvano stanje fragmenta
import android.view.LayoutInflater; // Pretvara XML izgled u View objekte
import android.view.View;           // Osnovna klasa svih UI elemenata
import android.view.ViewGroup;      // Kontejner koji drži druge View-ove
import android.widget.LinearLayout; // Raspored koji stavlja decu u red ili kolonu
import android.widget.TextView;     // Element za prikazivanje teksta

import androidx.fragment.app.Fragment; // Osnovna klasa za fragment – "podekran" unutar aktivnosti

import com.github.mikephil.charting.charts.PieChart;  // MPAndroidChart: widget koji iscrtava kružni grafikon
import com.github.mikephil.charting.data.PieData;     // Omot koji drži PieDataSet za prosleđivanje grafikonu
import com.github.mikephil.charting.data.PieDataSet;  // Skup podataka za kružni grafikon (kriške i boje)
import com.github.mikephil.charting.data.PieEntry;    // Jedan podatak u kružnom grafikonu (vrednost + oznaka)
import com.google.android.material.button.MaterialButtonToggleGroup; // Grupa dugmića za filter perioda

import java.util.ArrayList; // Implementacija liste sa promenljivim brojem elemenata
import java.util.Calendar;  // Klasa za rad sa datumima i vremenima
import java.util.List;      // Interfejs koji opisuje listu
import java.util.Map;       // Interfejs koji opisuje mapu (ključ → vrednost)

// Fragment koji prikazuje kružni grafikon troškova po kategorijama za izabrani vremenski period.
// Sadrži iste dugmiće za filter perioda kao ExpenseListFragment i prilagođenu legendu ispod grafikona.
public class ChartFragment extends Fragment {

    // Paleta boja za kriške grafikona – po jedna boja za svaku kategoriju.
    // Modulo operator u renderPieChart-u omogućava da se paleta "zavrti" ako ima više od 4 kategorije.
    private static final int[] CHART_COLORS = {
        Color.rgb(76, 175, 80),   // Zelena  – za prvu kategoriju
        Color.rgb(33, 150, 243),  // Plava   – za drugu kategoriju
        Color.rgb(255, 152, 0),   // Narandžasta – za treću kategoriju
        Color.rgb(156, 39, 176)   // Ljubičasta  – za četvrtu kategoriju
    };

    private ExpenseDao expenseDao;   // Objekat za komunikaciju sa bazom podataka
    private PieChart pieChart;       // Widget kružnog grafikona iz MPAndroidChart biblioteke
    private TextView textNoData;     // Poruka "Nema podataka" koja se prikazuje kada nema troškova
    private LinearLayout legendLayout; // Kontejner u koji se dinamički dodaju labele legende
    private long currentFromMs;      // Unix timestamp od kog se filtriraju troškovi za grafikon

    // Kreira i vraća View za ovaj fragment iz XML fajla.
    // Nema logike ovde – reference se postavljaju u onViewCreated.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false); // Pretvara XML u View i vraća ga
    }

    // Poziva se nakon što je View potpuno kreiran.
    // Kreira DAO, pronalazi Views-ove, podešava filter dugmiće, postavlja inicijalni period na danas i učitava grafikon.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);           // Obavezno – poziva roditeljsku metodu
        expenseDao    = new ExpenseDao(requireContext());         // Kreira DAO za pristup bazi podataka
        pieChart      = view.findViewById(R.id.pie_chart);       // Pronalazi widget kružnog grafikona
        textNoData    = view.findViewById(R.id.text_no_data);    // Pronalazi poruku "Nema podataka"
        legendLayout  = view.findViewById(R.id.layout_legend);   // Pronalazi kontejner za legendu

        setupFilterToggle(view); // Podešava dugmiće Dan/Sedmica/Mesec

        currentFromMs = getStartOfDay(); // Postavlja inicijalni filter na ponoć danas
        loadChart();                     // Učitava grafikon za danas
    }

    // Fragmenti koji se upravljaju kroz show/hide ne prolaze kroz onResume kada postanu vidljivi,
    // pa onHiddenChanged je pravo mesto za osvežavanje podataka.
    // Kada korisnik pređe na tab grafikona (hidden = false), grafikon se ponovo učitava
    // kako bi prikazao troškove koji su eventualno dodati dok je bio skriven.
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);   // Obavezno – poziva roditeljsku metodu
        if (!hidden) loadChart();        // Osvežava grafikon samo kada fragment postaje VIDLJIV (ne i kada se skriva)
    }

    // Pronalazi grupu dugmića za filter, programski bira "Dan" i registruje listener.
    // Guard na isChecked sprečava dvostruko učitavanje jer listener puca i za odznačavanje i za označavanje.
    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter); // Pronalazi grupu dugmića
        toggle.check(R.id.btn_day); // Programski označava dugme "Dan" kao početni izbor
        // Guard: listener se aktivira za oba događaja (check I uncheck);
        // bez isChecked provere grafikon bi se učitavao duplo pri svakom tapnuću
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId); // Reaguje samo kada se dugme OZNAČAVA
        });
    }

    // Prevodi ID označenog dugmića u timestamp početka tog perioda,
    // čuva ga i ponovo crta grafikon.
    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day)        currentFromMs = getStartOfDay();   // Dan: od ponoći danas
        else if (buttonId == R.id.btn_week)  currentFromMs = getStartOfWeek();  // Sedmica: od ponedeljka
        else                                 currentFromMs = getStartOfMonth(); // Mesec: od prvog u mesecu
        loadChart(); // Crta grafikon za novi period
    }

    // Čisti legendu (briše sve prethodno dodane labele) jer se ona gradi iznova pri svakom učitavanju.
    // Ako baza nema podataka za period, skriva grafikon i prikazuje poruku "Nema podataka".
    // Ako ima podataka, prikazuje grafikon i delegira crtanje renderPieChart metodi.
    private void loadChart() {
        legendLayout.removeAllViews(); // Briše sve labele legende da ne bi se duplirale pri promeni filtera
        Map<String, Double> data = expenseDao.getExpensesForChart(currentFromMs); // Dohvata zbirove po kategorijama
        if (data.isEmpty()) { // Ako nema troškova za izabrani period
            pieChart.setVisibility(View.GONE);       // Skriva kružni grafikon
            textNoData.setVisibility(View.VISIBLE);  // Prikazuje poruku "Nema podataka"
            return;                                  // Završava metodu – nema šta da se crta
        }
        textNoData.setVisibility(View.GONE);    // Skriva poruku "Nema podataka" (ima troškova)
        pieChart.setVisibility(View.VISIBLE);   // Prikazuje kružni grafikon
        renderPieChart(data);                   // Crta grafikon sa podacima
    }

    // Prolazi kroz mapu kategorija i iznosa.
    // Za svaki unos kreira PieEntry (tačka podatka: float vrednost + string oznaka) i
    // dodeljuje boju iz CHART_COLORS koristeći modulo da paleta kruži za više od 4 kategorije.
    // Kreira PieDataSet, isključuje ugrađenu legendu biblioteke (koristimo prilagođenu),
    // primenjuje podatke, prisilno crta grafikon, pa dodaje obojene labele u legendLayout.
    private void renderPieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();  // Lista tačaka podataka za kružni grafikon
        List<Integer>  colors  = new ArrayList<>();  // Lista boja – po jedna za svaku kriššku
        int i = 0;                                   // Brojač za izbor boje
        for (Map.Entry<String, Double> entry : data.entrySet()) { // Prolazi kroz svaki par kategorija → iznos
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey())); // Dodaje kriššku (vrednost + oznaka)
            // Modulo osigurava da se paleta "zavrti" ako ima više od 4 kategorije
            colors.add(CHART_COLORS[i % CHART_COLORS.length]); // Dodaje odgovarajuću boju
            i++; // Povećava brojač za sledeću boju
        }

        PieDataSet dataSet = new PieDataSet(entries, ""); // Grupiše sve kriške u jedan skup podataka
        dataSet.setColors(colors);      // Dodeljuje boje krišpama
        dataSet.setValueTextSize(12f);  // Postavlja veličinu teksta oznake unutar kriške (12sp)

        // Isključuje ugrađenu legendu MPAndroidChart biblioteke – koristimo prilagođenu legendu (legendLayout) ispod
        pieChart.getLegend().setEnabled(false);
        pieChart.setData(new PieData(dataSet));    // Predaje podatke grafikonu
        pieChart.getDescription().setEnabled(false); // Isključuje opis u uglu grafikona (podrazumevano prazan tekst)
        pieChart.invalidate(); // Prisilno ponovo crta grafikon sa novim podacima

        for (int j = 0; j < entries.size(); j++) { // Za svaku kriššku dodaje jednu labelu u legendu
            legendLayout.addView(createLegendItem(entries.get(j).getLabel(), colors.get(j))); // Kreira i dodaje labelu
        }
    }

    // Kreira jedan TextView za prilagođenu legendu.
    // Predočava obojeni krug (●) ispred oznake kategorije kako bi korisnik vizuelno
    // mogao da poveže labelu sa odgovarajućom krišškom na grafikonu.
    private TextView createLegendItem(String label, int color) {
        TextView tv = new TextView(requireContext()); // Kreira novi TextView u kontekstu fragmenta
        tv.setText("● " + label);  // Postavlja tekst: obojeni krug + naziv kategorije
        tv.setTextColor(color);    // Boji tekst (i krug) bojom odgovarajuće kriške
        tv.setTextSize(14f);       // Postavlja veličinu fonta na 14sp
        return tv;                 // Vraća kreiran TextView koji će biti dodat u legendLayout
    }

    // Poziva se kada Android uklanja View ovog fragmenta.
    // Zatvara DAO da ne bi curela SQLite veza.
    @Override
    public void onDestroyView() {
        super.onDestroyView();  // Obavezno – poziva roditeljsku metodu
        expenseDao.close();     // Oslobađa SQLite vezu
    }

    // Uzima trenutni momenat, nulluje sate/minute/sekunde/milisekunde i vraća Unix timestamp –
    // tj. ponoć od danas.
    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();   // Kreira Calendar sa trenutnim vremenom
        cal.set(Calendar.HOUR_OF_DAY, 0);       // Sat → 0 (ponoć)
        cal.set(Calendar.MINUTE, 0);            // Minute → 0
        cal.set(Calendar.SECOND, 0);            // Sekunde → 0
        cal.set(Calendar.MILLISECOND, 0);       // Milisekunde → 0
        return cal.getTimeInMillis();            // Vraća Unix timestamp za ponoć danas
    }

    // Izračunava timestamp za ponoć ponedeljka tekuće sedmice.
    // Isti algoritam kao u ExpenseListFragment.getStartOfWeek().
    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance(); // Kreira Calendar sa trenutnim vremenom
        // Calendar.DAY_OF_WEEK: 1=ned ... 7=sub. Formulom (vrednost + 5) % 7 dobijamo 0=pon ... 6=ned
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Broj dana od poslednjeg ponedeljka
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday); // Pomera datum unazad na ponedeljak
        cal.set(Calendar.HOUR_OF_DAY, 0);                 // Sat → 0
        cal.set(Calendar.MINUTE, 0);                      // Minute → 0
        cal.set(Calendar.SECOND, 0);                      // Sekunde → 0
        cal.set(Calendar.MILLISECOND, 0);                 // Milisekunde → 0
        return cal.getTimeInMillis();                      // Vraća Unix timestamp za ponoć ponedeljka
    }

    // Isti princip kao getStartOfDay ali i postavlja dan na 1 –
    // rezultat je ponoć prvog dana tekućeg meseca.
    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance();  // Kreira Calendar sa trenutnim vremenom
        cal.set(Calendar.DAY_OF_MONTH, 1);     // Dan u mesecu → 1 (prvi)
        cal.set(Calendar.HOUR_OF_DAY, 0);      // Sat → 0
        cal.set(Calendar.MINUTE, 0);           // Minute → 0
        cal.set(Calendar.SECOND, 0);           // Sekunde → 0
        cal.set(Calendar.MILLISECOND, 0);      // Milisekunde → 0
        return cal.getTimeInMillis();           // Vraća Unix timestamp za ponoć prvog dana meseca
    }
}
