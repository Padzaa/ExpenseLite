package com.example.expenselite;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class ChartFragment extends Fragment {

    private static final int[] CHART_COLORS = {
        Color.rgb(76, 175, 80),   // green
        Color.rgb(33, 150, 243),  // blue
        Color.rgb(255, 152, 0),   // orange
        Color.rgb(156, 39, 176)   // purple
    };

    private ExpenseDao expenseDao;
    private PieChart pieChart;
    private TextView textNoData;
    private LinearLayout legendLayout;
    private long currentFromMs;

    // Inflates the XML layout for this fragment and returns the root view.
    // No logic here — view references are assigned in onViewCreated once the view tree is ready.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    // Runs after the view is fully created. Creates the DAO, finds the chart, no-data label,
    // and legend container by ID. Sets up the filter toggle, defaults the filter to today,
    // then performs the first chart load.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseDao = new ExpenseDao(requireContext());
        pieChart = view.findViewById(R.id.pie_chart);
        textNoData = view.findViewById(R.id.text_no_data);
        legendLayout = view.findViewById(R.id.layout_legend);

        setupFilterToggle(view);

        currentFromMs = getStartOfDay();
        loadChart();
    }

    // Fragments managed with show/hide don't go through onResume when they become visible,
    // so onHiddenChanged is the correct hook to refresh stale chart data. When the user
    // switches to the chart tab, hidden becomes false and the chart reloads with current data.
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) loadChart();
    }

    // Finds the toggle button group, programmatically selects Day as the initial state,
    // and registers a listener. Guards on isChecked because the event fires once for the
    // button being deselected and once for the new button being selected — without the guard,
    // loadChart would run twice per tap.
    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter);
        toggle.check(R.id.btn_day);
        // Guard on isChecked: the listener fires for both check and uncheck, so without this
        // we'd reload the chart twice — once when the old button deselects, once when the new one selects.
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId);
        });
    }

    // Maps the selected button's ID to a start-of-period timestamp, stores it in
    // currentFromMs, then reloads the chart for that new time window.
    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day) currentFromMs = getStartOfDay();
        else if (buttonId == R.id.btn_week) currentFromMs = getStartOfWeek();
        else currentFromMs = getStartOfMonth();
        loadChart();
    }

    // Clears all legend views first because they are rebuilt from scratch on every load —
    // without this, switching filters would stack duplicate labels. Queries the DAO for
    // category totals. If the result is empty, hides the chart and shows the no-data label,
    // then returns early. Otherwise hides the label, shows the chart, and delegates
    // the actual rendering to renderPieChart.
    private void loadChart() {
        legendLayout.removeAllViews();
        Map<String, Double> data = expenseDao.getExpensesForChart(currentFromMs);
        if (data.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            textNoData.setVisibility(View.VISIBLE);
            return;
        }
        textNoData.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);
        renderPieChart(data);
    }

    // Iterates the category-to-total map. For each entry it creates a PieEntry (the library's
    // data point: a float value + a string label) and picks a color from CHART_COLORS using
    // modulo so the palette wraps around for more than 4 categories. Builds a PieDataSet
    // from the entries and color list, disables the library's built-in legend in favour of
    // the custom legendLayout, applies the data, calls invalidate() to force a redraw,
    // then adds one colored text label per entry to the legend layout.
    private void renderPieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            // Wrap around with modulo so more than 4 categories still get a color.
            colors.add(CHART_COLORS[i % CHART_COLORS.length]);
            i++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);

        // The library's built-in legend is disabled in favour of the custom legendLayout below,
        // which gives full control over positioning and styling.
        pieChart.getLegend().setEnabled(false);
        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate(); // forces a redraw with the new data

        for (int j = 0; j < entries.size(); j++) {
            legendLayout.addView(createLegendItem(entries.get(j).getLabel(), colors.get(j)));
        }
    }

    // Creates a single TextView for the custom legend. Prepends a colored bullet (●) so
    // there is a clear visual link between the legend label and its corresponding pie slice.
    private TextView createLegendItem(String label, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText("● " + label);
        tv.setTextColor(color);
        tv.setTextSize(14f);
        return tv;
    }

    // Called when the fragment's view is being torn down. Closes the DAO to release
    // the SQLite connection; not doing this would leak the connection.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        expenseDao.close();
    }

    // Takes the current moment, zeroes out hours, minutes, seconds, and milliseconds,
    // and returns the result as a Unix millisecond timestamp — i.e. midnight of today.
    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Calculates the timestamp for midnight of the Monday that started the current week.
    // Calendar.DAY_OF_WEEK is 1 (Sun) through 7 (Sat). Adding 5 then taking mod 7 remaps
    // that to 0 (Mon) through 6 (Sun), giving the number of days since Monday regardless
    // of locale. Subtracting that count from today lands on Monday, zeroed to midnight.
    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        // Calendar.DAY_OF_WEEK is 1 (Sun) … 7 (Sat). Adding 5 then taking mod 7 remaps that
        // to 0 (Mon) … 6 (Sun), giving locale-independent days-since-Monday.
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Same concept as getStartOfDay but also sets DAY_OF_MONTH to 1,
    // landing on the first of the current month at midnight.
    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
