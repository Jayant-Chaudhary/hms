package com.example.hms.activities.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.FinanceCategory;

import java.util.ArrayList;
import java.util.List;

class FinanceCategoryAdapter extends RecyclerView.Adapter<FinanceCategoryAdapter.Holder> {

    interface Listener {
        void onEdit(FinanceCategory c);
    }

    private final List<FinanceCategory> items = new ArrayList<>();
    private final Listener listener;

    FinanceCategoryAdapter(List<FinanceCategory> initial, Listener listener) {
        if (initial != null) items.addAll(initial);
        this.listener = listener;
    }

    void setItems(List<FinanceCategory> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finance_category, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        FinanceCategory c = items.get(position);
        h.tvName.setText(c.name == null ? "" : c.name);
        int subs = c.subCategories == null ? 0 : c.subCategories.size();
        h.tvMeta.setText(subs == 1 ? "1 subcategory" : (subs + " subcategories"));
        h.itemView.setOnClickListener(v -> listener.onEdit(c));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvMeta;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCatName);
            tvMeta = itemView.findViewById(R.id.tvCatMeta);
        }
    }
}

