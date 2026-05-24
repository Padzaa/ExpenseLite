package com.example.expenselite;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ExpenseListFragment expenseListFragment;
    private ChartFragment chartFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            initFragments();
        } else {
            restoreFragments();
        }

        setupBottomNav();
    }

    private void initFragments() {
        expenseListFragment = new ExpenseListFragment();
        chartFragment = new ChartFragment();

        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, expenseListFragment, "list")
            .add(R.id.fragment_container, chartFragment, "chart")
            .hide(chartFragment)
            .commit();
    }

    private void restoreFragments() {
        expenseListFragment = (ExpenseListFragment)
            getSupportFragmentManager().findFragmentByTag("list");
        chartFragment = (ChartFragment)
            getSupportFragmentManager().findFragmentByTag("chart");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_expenses) {
                showFragment(expenseListFragment, chartFragment);
                return true;
            } else if (id == R.id.nav_chart) {
                showFragment(chartFragment, expenseListFragment);
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment show, Fragment hide) {
        getSupportFragmentManager().beginTransaction()
            .show(show)
            .hide(hide)
            .commit();
    }
}