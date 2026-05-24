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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) loadChart();
    }

    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter);
        toggle.check(R.id.btn_day);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId);
        });
    }

    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day) currentFromMs = getStartOfDay();
        else if (buttonId == R.id.btn_week) currentFromMs = getStartOfWeek();
        else currentFromMs = getStartOfMonth();
        loadChart();
    }

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

    private void renderPieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            colors.add(CHART_COLORS[i % CHART_COLORS.length]);
            i++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);

        pieChart.getLegend().setEnabled(false);
        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();

        for (int j = 0; j < entries.size(); j++) {
            legendLayout.addView(createLegendItem(entries.get(j).getLabel(), colors.get(j)));
        }
    }

    private TextView createLegendItem(String label, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText("● " + label);
        tv.setTextColor(color);
        tv.setTextSize(14f);
        return tv;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        expenseDao.close();
    }

    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

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
