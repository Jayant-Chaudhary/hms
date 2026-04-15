package com.example.hms.activities.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hms.R;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<String> monthLabels = new ArrayList<>();
    private final List<Entry> monthlyRevenue = new ArrayList<>();
    private final List<Entry> monthlyExpense = new ArrayList<>();
    private final List<Entry> monthlyNet = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        ViewPager2 pager = findViewById(R.id.viewPagerAnalytics);
        pager.setAdapter(new ChartPagerAdapter());
        loadSeriesAndRefresh(pager);
    }

    private void loadSeriesAndRefresh(ViewPager2 pager) {
        // Aggregate from Firestore by monthKey (last 12 months).
        repo.finance()
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(400)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Map<String, Double> revByMonth = new HashMap<>();
                    Map<String, Double> expByMonth = new HashMap<>();

                    List<String> last12 = lastNMonthKeys(12);
                    for (String mk : last12) {
                        revByMonth.put(mk, 0d);
                        expByMonth.put(mk, 0d);
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String mk = doc.getString("monthKey");
                        if (mk == null || !revByMonth.containsKey(mk)) continue;
                        String type = doc.getString("type");
                        Double amount = doc.getDouble("amount");
                        double a = amount == null ? 0 : amount;
                        if ("expense".equalsIgnoreCase(type)) {
                            expByMonth.put(mk, expByMonth.get(mk) + a);
                        } else {
                            revByMonth.put(mk, revByMonth.get(mk) + a);
                        }
                    }

                    monthLabels.clear();
                    monthlyRevenue.clear();
                    monthlyExpense.clear();
                    monthlyNet.clear();

                    for (int i = 0; i < last12.size(); i++) {
                        String mk = last12.get(i);
                        double r = revByMonth.get(mk);
                        double e = expByMonth.get(mk);
                        float x = i;
                        monthLabels.add(prettyMonth(mk));
                        monthlyRevenue.add(new Entry(x, (float) r));
                        monthlyExpense.add(new Entry(x, (float) e));
                        monthlyNet.add(new Entry(x, (float) (r - e)));
                    }

                    if (pager.getAdapter() != null) pager.getAdapter().notifyDataSetChanged();
                });
    }

    private class ChartPagerAdapter extends RecyclerView.Adapter<ChartPagerAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chart_page, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            if (position == 0) bindRevenueVsExpense(h);
            if (position == 1) bindSingle(h, "Monthly Net (Revenue - Expenses)", monthlyNet, 0xFF7B61FF);
            if (position == 2) bindSingle(h, "Monthly Expenses", monthlyExpense, 0xFFB00020);
        }

        private void bindRevenueVsExpense(Holder h) {
            h.tvTitle.setText("Monthly Revenue vs Expenses");
            LineDataSet rev = makeSet(monthlyRevenue, "Revenue", 0xFF2E7D32);
            LineDataSet exp = makeSet(monthlyExpense, "Expenses", 0xFFB00020);
            LineData data = new LineData(rev, exp);
            data.setDrawValues(false);
            applyChartStyle(h.chart);
            h.chart.setData(data);
            h.chart.invalidate();
        }

        private void bindSingle(Holder h, String title, List<Entry> entries, int color) {
            h.tvTitle.setText(title);
            LineDataSet ds = makeSet(entries, title, color);
            LineData data = new LineData(ds);
            data.setDrawValues(false);
            applyChartStyle(h.chart);
            h.chart.setData(data);
            h.chart.invalidate();
        }

        private LineDataSet makeSet(List<Entry> entries, String label, int color) {
            LineDataSet ds = new LineDataSet(entries, label);
            ds.setColor(color);
            ds.setCircleColor(color);
            ds.setLineWidth(2.2f);
            ds.setCircleRadius(3.2f);
            ds.setDrawCircleHole(false);
            ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            ds.setDrawFilled(true);
            ds.setFillColor(color);
            ds.setFillAlpha(35);
            ds.setHighlightEnabled(false);
            return ds;
        }

        private void applyChartStyle(LineChart chart) {
            chart.getDescription().setEnabled(false);
            chart.setNoDataText("No finance data yet");
            chart.setTouchEnabled(true);
            chart.setPinchZoom(true);
            chart.setExtraOffsets(10f, 10f, 10f, 10f);
            chart.setDrawGridBackground(false);

            Legend legend = chart.getLegend();
            legend.setEnabled(true);
            legend.setTextSize(12f);
            legend.setForm(Legend.LegendForm.LINE);

            XAxis x = chart.getXAxis();
            x.setPosition(XAxis.XAxisPosition.BOTTOM);
            x.setDrawGridLines(false);
            x.setGranularity(1f);
            x.setLabelCount(Math.min(6, monthLabels.size()), true);
            x.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    int idx = Math.round(value);
                    if (idx < 0 || idx >= monthLabels.size()) return "";
                    return monthLabels.get(idx);
                }
            });

            YAxis left = chart.getAxisLeft();
            left.setDrawGridLines(true);
            left.setGridColor(0x22FFFFFF);
            left.setTextSize(12f);
            left.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    // Compact: 1.2k / 3.4L
                    float abs = Math.abs(value);
                    if (abs >= 100000f) return String.format(Locale.getDefault(), "%.1fL", value / 100000f);
                    if (abs >= 1000f) return String.format(Locale.getDefault(), "%.1fk", value / 1000f);
                    return String.format(Locale.getDefault(), "%.0f", value);
                }
            });

            chart.getAxisRight().setEnabled(false);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            LineChart chart;
            Holder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvChartTitle);
                chart = itemView.findViewById(R.id.lineChart);
            }
        }
    }

    private static List<String> lastNMonthKeys(int n) {
        List<String> out = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        for (int i = 0; i < n; i++) {
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH) + 1;
            out.add(String.format(Locale.US, "%04d-%02d", y, m));
            cal.add(Calendar.MONTH, -1);
        }
        // oldest -> newest for chart
        Collections.sort(out, Comparator.naturalOrder());
        return out;
    }

    private static String prettyMonth(String monthKey) {
        // monthKey yyyy-MM -> Apr '26 (short)
        try {
            String[] p = monthKey.split("-");
            int year = Integer.parseInt(p[0]);
            int month = Integer.parseInt(p[1]);
            String[] names = new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            String yy = String.valueOf(year).substring(2);
            return names[Math.max(0, Math.min(11, month - 1))] + " '" + yy;
        } catch (Exception e) {
            return monthKey;
        }
    }
}
