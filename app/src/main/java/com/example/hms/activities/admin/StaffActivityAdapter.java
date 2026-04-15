package com.example.hms.activities.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.AdminActivityItem;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StaffActivityAdapter extends RecyclerView.Adapter<StaffActivityAdapter.Holder> {

    public interface ActionListener {
        void onApprove(String documentId);

        void onDeny(String documentId);
    }

    private final List<AdminActivityItem> items = new ArrayList<>();
    private final ActionListener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public StaffActivityAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AdminActivityItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_activity_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AdminActivityItem item = items.get(position);
        String actor = item.actorName != null ? item.actorName : "";
        String sum = item.summary != null ? item.summary : "";
        h.tvLine.setText(actor.isEmpty() ? sum : actor + " — " + sum);

        if (item.createdAt != null) {
            h.tvTime.setText(relativeTime(item.createdAt.toDate()));
        } else {
            h.tvTime.setText("");
        }

        if (item.isAccessRequestPending()) {
            h.rowActions.setVisibility(View.VISIBLE);
            h.btnApprove.setOnClickListener(v -> listener.onApprove(item.id));
            h.btnDeny.setOnClickListener(v -> listener.onDeny(item.id));
        } else {
            h.rowActions.setVisibility(View.GONE);
            h.btnApprove.setOnClickListener(null);
            h.btnDeny.setOnClickListener(null);
        }

        if (AdminActivityItem.TYPE_SYSTEM.equalsIgnoreCase(item.activityType)) {
            h.avatar.setBackgroundResource(R.drawable.bg_concierge_chip);
        } else {
            h.avatar.setBackgroundResource(R.drawable.bg_admin_room_cell);
        }
    }

    private String relativeTime(Date d) {
        long diff = System.currentTimeMillis() - d.getTime();
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        }
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toMinutes(diff) + " minutes ago";
        }
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toHours(diff) + " hours ago";
        }
        return fmt.format(d);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ShapeableImageView avatar;
        final TextView tvLine;
        final TextView tvTime;
        final View rowActions;
        final View btnApprove;
        final View btnDeny;

        Holder(View v) {
            super(v);
            avatar = v.findViewById(R.id.ivStaffAvatar);
            tvLine = v.findViewById(R.id.tvActivityLine);
            tvTime = v.findViewById(R.id.tvActivityTime);
            rowActions = v.findViewById(R.id.rowAccessActions);
            btnApprove = v.findViewById(R.id.btnActivityApprove);
            btnDeny = v.findViewById(R.id.btnActivityDeny);
        }
    }
}
