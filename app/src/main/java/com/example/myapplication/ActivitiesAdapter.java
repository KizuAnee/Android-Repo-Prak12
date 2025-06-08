package com.example.myapplication;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class ActivitiesAdapter extends RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder> {
    private static final String TAG = "ActivitiesAdapter";
    private List<Activity> activities;
    private final OnActivityClickListener listener;
    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
        void onActivityLongClick(Activity activity);
    }
    public ActivitiesAdapter(OnActivityClickListener listener) {
        this.listener = listener;
        this.activities = new ArrayList<>();
    }
    public void setActivities(List<Activity> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activity = activities.get(position);
        holder.bind(activity, listener);
    }
    @Override
    public int getItemCount() {
        return activities.size();
    }
    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView dateTimeTextView;
        private final TextView descriptionTextView;
        private final TextView reminderTextView;
        private final ImageButton shareButton;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTimeTextView = itemView.findViewById(R.id.dateTimeTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            reminderTextView = itemView.findViewById(R.id.reminderTextView);
            shareButton = itemView.findViewById(R.id.shareButton);
        }
        public void bind(final Activity activity, final OnActivityClickListener listener) {
            // Set data utama
            titleTextView.setText(activity.getTitle());
            descriptionTextView.setText(activity.getDescription());
            // Format tanggal dan waktu
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String dateStr = dateFormat.format(new Date(activity.getDateTime()));
            String timeStr = timeFormat.format(new Date(activity.getDateTime()));
            dateTimeTextView.setText(String.format("%s • %s", dateStr, timeStr));
            // Handle reminder
            int reminderMinutes = activity.getReminderMinutesBefore();
            if (reminderMinutes > 0) {
                String reminderText = formatReminderText(reminderMinutes);
                reminderTextView.setText(reminderText);
                reminderTextView.setVisibility(View.VISIBLE);
            } else {
                reminderTextView.setVisibility(View.GONE);
            }
            // Click listeners
            itemView.setOnClickListener(v -> listener.onActivityClick(activity));
            itemView.setOnLongClickListener(v -> {
                listener.onActivityLongClick(activity);
                return true;
            });
            shareButton.setOnClickListener(v -> shareActivityDetails(activity));
        }
        private String formatReminderText(int minutes) {
            if (minutes >= 60) {
                int hours = minutes / 60;
                int mins = minutes % 60;
                return mins > 0 ?
                        String.format(Locale.getDefault(), "%d jam %d menit sebelum", hours, mins) :
                        String.format(Locale.getDefault(), "%d jam sebelum", hours);
            }
            return String.format(Locale.getDefault(), "%d menit sebelum", minutes);
        }
        private void shareActivityDetails(Activity activity) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                String shareContent = "📅 Aktivitas: " + activity.getTitle() + "\n" +
                        "📝 Deskripsi: " + activity.getDescription() + "\n" +
                        "🗓 Tanggal: " + dateFormat.format(new Date(activity.getDateTime())) + "\n" +
                        "⏰ Waktu: " + timeFormat.format(new Date(activity.getDateTime())) + "\n" +
                        "🔔 Pengingat: " + formatReminderText(activity.getReminderMinutesBefore());
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Bagikan Aktivitas");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
                itemView.getContext().startActivity(
                        Intent.createChooser(shareIntent, "Bagikan melalui")
                );
            } catch (Exception e) {
                Log.e(TAG, "Gagal membagikan aktivitas", e);
                Toast.makeText(itemView.getContext(),
                        "Tidak bisa membagikan aktivitas",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}