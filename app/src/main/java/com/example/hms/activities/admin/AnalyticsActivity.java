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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<Entry> monthlyNet = new ArrayList<>();
    private final List<Entry> dailyRevenue = new ArrayList<>();
    private final List<Entry> dailyExpense = new ArrayList<>();

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
        repo.finance().get().addOnSuccessListener(snapshots -> {
            float monthly = 0;
            float revDay = 0;
            float expDay = 0;
            int i = 1;
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                String type = doc.getString("type");
                Double amount = doc.getDouble("amount");
                float a = amount == null ? 0 : amount.floatValue();
                if ("expense".equalsIgnoreCase(type)) {
                    monthly -= a;
                    expDay += a;
                } else {
                    monthly += a;
                    revDay += a;
                }
                if (i <= 12) monthlyNet.add(new Entry(i, monthly));
                if (i <= 30) {
                    dailyRevenue.add(new Entry(i, revDay));
                    dailyExpense.add(new Entry(i, expDay));
                }
                i++;
            }
            if (pager.getAdapter() != null) {
                pager.getAdapter().notifyDataSetChanged();
            }
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
            if (position == 0) bindChart(h, "Monthly Net", monthlyNet, 0xFF1E88E5);
            if (position == 1) bindChart(h, "Daily Revenue", dailyRevenue, 0xFF2E7D32);
            if (position == 2) bindChart(h, "Daily Expense", dailyExpense, 0xFFB00020);
        }

        private void bindChart(Holder h, String title, List<Entry> entries, int color) {
            h.tvTitle.setText(title);
            LineDataSet ds = new LineDataSet(entries, title);
            ds.setColor(color);
            ds.setValueTextColor(color);
            ds.setLineWidth(2f);
            ds.setCircleRadius(3f);
            h.chart.setData(new LineData(ds));
            h.chart.getDescription().setEnabled(false);
            h.chart.invalidate();
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
}
