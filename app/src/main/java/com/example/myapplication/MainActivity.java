package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import androidx.appcompat.widget.SwitchCompat;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements ActivitiesAdapter.OnActivityClickListener {
    private RecyclerView activitiesRecyclerView;
    private ActivitiesAdapter adapter;
    private AppDatabase db;
    private SwitchCompat switchFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        initViews();
        checkAndRequestNotificationPermission();
        loadActivities();
    }

    private void initViews() {
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivitiesAdapter(this);
        activitiesRecyclerView.setAdapter(adapter);

        FloatingActionButton addActivityFab = findViewById(R.id.addActivityFab);
        addActivityFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            startActivity(intent);
        });
        // Switch untuk fingerprint
        SwitchCompat switchFingerprint = findViewById(R.id.switch_fingerprint);
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isFingerprintEnabled = preferences.getBoolean("fingerprint_enabled", false);
        switchFingerprint.setChecked(isFingerprintEnabled);

        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("fingerprint_enabled", isChecked).apply();
        });
    }

    private void loadActivities() {
        new Thread(() -> {
            List<Activity> activities = db.activityDao().getAllActivities();
            runOnUiThread(() -> adapter.setActivities(activities));
        }).start();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationPermissionHelper.areNotificationsEnabled(this)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionExplanation();
                } else {
                    NotificationPermissionHelper.requestNotificationPermission(this);
                }
            }
        }
    }

    private void showNotificationPermissionExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("Izin Notifikasi Diperlukan")
                .setMessage("Aplikasi membutuhkan izin untuk menampilkan notifikasi pengingat aktivitas.")
                .setPositiveButton("Setuju", (d, w) ->
                        NotificationPermissionHelper.requestNotificationPermission(this))
                .setNegativeButton("Nanti", null)
                .show();
    }
    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak, fitur reminder tidak akan bekerja",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void onActivityClick(Activity activity) {
        Intent intent = new Intent(this, AddEditActivity.class);
        intent.putExtra("activity_id", activity.getId());
        startActivity(intent);
    }
    @Override
    public void onActivityLongClick(Activity activity) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Aktivitas")
                .setMessage("Yakin ingin menghapus aktivitas ini?")
                .setPositiveButton("Hapus", (d, w) -> deleteActivity(activity))
                .setNegativeButton("Batal", null)
                .show();
    }
    private void deleteActivity(Activity activity) {
        new Thread(() -> {
            db.activityDao().delete(activity);
            runOnUiThread(() -> {
                loadActivities();
                Toast.makeText(this, "Aktivitas dihapus", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadActivities();
    }
}