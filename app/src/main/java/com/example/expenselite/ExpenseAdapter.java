package com.example.expenselite; // Paket u kome se nalazi ova klasa

import android.view.LayoutInflater; // Pretvara XML izgled u stvarne Android View objekte
import android.view.View;           // Osnovna klasa za sve elemente korisničkog interfejsa
import android.view.ViewGroup;      // Kontejner koji sadrži druge View-ove (roditeljski View)
import android.widget.TextView;     // Element za prikazivanje teksta na ekranu

import androidx.annotation.NonNull;               // Anotacija koja govori da argument ne sme biti null
import androidx.recyclerview.widget.RecyclerView; // Lista koja reciklira redove radi efikasnosti pri skrolovanju

import java.util.List; // Interfejs koji opisuje listu elemenata

// Adapter koji vezuje listu Expense objekata za RecyclerView.
// RecyclerView ne zna ništa o Expense klasi – sve o prikazu podataka nalazi se u ovom adapteru.
// Reciklira redove koji su otišli van ekrana i popunjava ih novim podacima, umesto da kreira novi red za svaki element.
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    // Interfejs (ugovor) za brisanje troška – ko god hoće da koristi ovaj adapter,
    // mora da implementira ovu metodu i kaže šta treba uraditi kada korisnik dugim pritiskom obriše trošak.
    public interface OnDeleteListener {
        void onDelete(Expense expense); // Poziva se kada korisnik dugim pritiskom odabere brisanje troška
    }

    private List<Expense> expenses;          // Lista troškova koji se prikazuju u RecyclerView-u
    private final OnDeleteListener deleteListener; // Callback koji se poziva kada korisnik hoće da obriše trošak

    // Konstruktor – čuva inicijalnu listu troškova i callback za brisanje za kasniju upotrebu.
    public ExpenseAdapter(List<Expense> expenses, OnDeleteListener deleteListener) {
        this.expenses = expenses;             // Čuva referencu na listu troškova
        this.deleteListener = deleteListener; // Čuva callback koji će biti pozvan pri brisanju
    }

    // Zamenjuje trenutnu listu novom i obaveštava RecyclerView da su se podaci promenili.
    // notifyDataSetChanged() kaže RecyclerView-u da ponovo prikaže sve vidljive redove.
    // Za male liste je u redu; za veće bi se koristio DiffUtil koji izračunava minimalne promene.
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;  // Zamenjuje staru listu novom
        notifyDataSetChanged();    // Obaveštava RecyclerView da osvoji sve vidljive redove
    }

    // Poziva se kada RecyclerView treba novi red koji nije dostupan u pool-u za recikliranje.
    // Pretvara item_expense.xml u View objekat, umotava ga u ViewHolder i vraća ga.
    // Argument false znači: ne zakači View za parent – RecyclerView to sam radi.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_expense, parent, false); // Pretvara XML fajl jednog reda liste u View objekat
        return new ViewHolder(view); // Omotava View u ViewHolder koji drži reference na tekstualna polja
    }

    // Poziva se svaki put kada red postane vidljiv – bilo da je tek kreiran ili recikliran.
    // Uzima trošak sa zadate pozicije, popunjava tri tekstualna polja i vezuje dugi pritisak za brisanje.
    // Vraćanje true iz setOnLongClickListener označava da je događaj obrađen i sprečava pokretanje kratkog pritiska.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);                          // Uzima trošak sa zadate pozicije u listi
        holder.textName.setText(expense.getName());                        // Prikazuje naziv troška
        holder.textCategory.setText(expense.getCategory());                // Prikazuje kategoriju troška
        holder.textAmount.setText(String.format("%.2f €", expense.getAmount())); // Prikazuje iznos sa 2 decimale i simbolom €
        // Dugi pritisak na red otvara dijalog za brisanje; vraćamo true da označimo da je događaj obrađen
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(expense); // Poziva callback za brisanje ovog troška
            return true;                      // Označava da je dugi pritisak obrađen (ne širi se dalje)
        });
    }

    // Vraća ukupan broj elemenata u listi – RecyclerView ovo koristi da zna koliko redova treba napraviti.
    @Override
    public int getItemCount() {
        return expenses.size(); // Broj troškova u listi
    }

    // ViewHolder čuva direktne reference na TextView-ove unutar jednog reda liste.
    // Ovo ubrzava prikaz jer se findViewById poziva samo jednom pri kreiranju reda,
    // a ne ponovo pri svakom recikliranju reda (što bi bilo sporo).
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;     // Tekstualno polje za naziv troška u redu
        TextView textCategory; // Tekstualno polje za kategoriju u redu
        TextView textAmount;   // Tekstualno polje za iznos u redu

        ViewHolder(View itemView) {
            super(itemView);                                                         // Obavezno – prosleđuje View roditeljskoj klasi
            textName     = itemView.findViewById(R.id.text_expense_name);           // Pronalazi polje za naziv
            textCategory = itemView.findViewById(R.id.text_expense_category);       // Pronalazi polje za kategoriju
            textAmount   = itemView.findViewById(R.id.text_expense_amount);         // Pronalazi polje za iznos
        }
    }
}
