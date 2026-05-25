package com.example.expenselite;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ExpenseListFragment expenseListFragment;
    private ChartFragment chartFragment;

    // Inflates the root layout and registers the toolbar as the action bar.
    // Then branches on savedInstanceState: null means the activity is being created for
    // the first time so fragments are built fresh; non-null means the system is recreating
    // the activity after a configuration change (e.g. rotation) and the fragments already
    // exist in the fragment manager, so we just retrieve references to them by tag.
    // Finally sets up the bottom navigation bar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // On first launch savedInstanceState is null, so we create the fragments fresh.
        // After a configuration change (rotation) the system already restored them, so we
        // just re-acquire the references by tag to avoid creating duplicates.
        if (savedInstanceState == null) {
            initFragments();
        } else {
            restoreFragments();
        }

        setupBottomNav();
    }

    // Creates fresh instances of both fragments and commits them to the same container
    // in a single transaction (one back-stack entry). ChartFragment is hidden in the same
    // transaction so the expense list is visible by default. Both fragments are given string
    // tags ("list" and "chart") so they can be retrieved after a configuration change.
    private void initFragments() {
        expenseListFragment = new ExpenseListFragment();
        chartFragment = new ChartFragment();

        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, expenseListFragment, "list")
            .add(R.id.fragment_container, chartFragment, "chart")
            .hide(chartFragment)
            .commit();
    }

    // After a configuration change the system automatically recreates the fragments that
    // were previously committed. This method retrieves those recreated instances by their
    // tags so the fields point to the live objects again. Their show/hide state is also
    // restored automatically by the system — no extra work needed.
    private void restoreFragments() {
        expenseListFragment = (ExpenseListFragment)
            getSupportFragmentManager().findFragmentByTag("list");
        chartFragment = (ChartFragment)
            getSupportFragmentManager().findFragmentByTag("chart");
    }

    // Attaches a selection listener to the bottom navigation bar. When the Expenses tab is
    // selected, shows the list fragment and hides the chart, and vice versa for the Chart tab.
    // Returns true to confirm the selection was handled; returns false for unrecognised items
    // (which shouldn't happen but satisfies the listener contract).
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

    // Performs a fragment transaction that shows one fragment and hides the other.
    // show/hide is used instead of replace so each fragment's view state (scroll position,
    // selected filter button) is preserved when the user switches tabs — replace would
    // destroy and recreate the hidden fragment's view, losing that state.
    private void showFragment(Fragment show, Fragment hide) {
        getSupportFragmentManager().beginTransaction()
            .show(show)
            .hide(hide)
            .commit();
    }
}