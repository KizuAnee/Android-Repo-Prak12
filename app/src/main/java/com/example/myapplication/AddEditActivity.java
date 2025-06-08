package com.example.myapplication;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
public class AddEditActivity extends AppCompatActivity {
    private TextInputEditText titleEditText, descriptionEditText, dateEditText, timeEditText, reminderEditText;
    private AppDatabase db;
    private Activity currentActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);
        db = AppDatabase.getInstance(this);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        reminderEditText = findViewById(R.id.reminderEditText);
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveActivity());
        // Check if we're editing an existing activity
        int activityId = getIntent().getIntExtra("activity_id", -1);
        if (activityId != -1) {
            loadActivity(activityId);
        }
        setupDateTimePickers();
    }
    private void loadActivity(int id) {
        new Thread(() -> {
            currentActivity = db.activityDao().getActivityById(id);
            runOnUiThread(() -> {
                if (currentActivity != null) {
                    titleEditText.setText(currentActivity.getTitle());
                    descriptionEditText.setText(currentActivity.getDescription());
                    SimpleDateFormat dateFormat = new SimpleDateFormat
                            ("EEE, MMM d, yyyy", Locale.getDefault());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    dateEditText.setText(dateFormat.format(new Date(currentActivity.getDateTime())));
                    timeEditText.setText(timeFormat.format(new Date(currentActivity.getDateTime())));
                    reminderEditText.setText(String.valueOf(currentActivity.getReminderMinutesBefore()));
                }
            });
        }).start();
    }
    private void setupDateTimePickers() {
        dateEditText.setOnClickListener(v -> showDatePicker());
        timeEditText.setOnClickListener(v -> showTimePicker());
    }
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (currentActivity != null) {
            calendar.setTimeInMillis(currentActivity.getDateTime());
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat
                            ("EEE, MMM d, yyyy", Locale.getDefault());
                    dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        if (currentActivity != null) {
            calendar.setTimeInMillis(currentActivity.getDateTime());
        }
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    timeEditText.setText(timeFormat.format(selectedTime.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }
    private void saveActivity() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String dateStr = dateEditText.getText().toString().trim();
        String timeStr = timeEditText.getText().toString().trim();
        String reminderStr = reminderEditText.getText().toString().trim();
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }
        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        // Cek izin sebelum menyimpan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationPermissionHelper.areNotificationsEnabled(this)) {
                Toast.makeText
                        (this, "Aktifkan izin notifikasi terlebih dahulu", Toast.LENGTH_LONG).show();
                return;
            }
        }
        try {
            // Parse date and time
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat
                    ("EEE, MMM d, yyyy h:mm a", Locale.getDefault());
            Date dateTime = dateTimeFormat.parse(dateStr + " " + timeStr);
            int reminderMinutes = 10; // default
            if (!reminderStr.isEmpty()) {
                reminderMinutes = Integer.parseInt(reminderStr);
            }
            if (currentActivity == null) {
                // Create new activity
                currentActivity = new Activity(title, description, dateTime.getTime(), reminderMinutes);
            } else {
                // Update existing activity
                currentActivity.setTitle(title);
                currentActivity.setDescription(description);
                currentActivity.setDateTime(dateTime.getTime());
                currentActivity.setReminderMinutesBefore(reminderMinutes);
            }
            new Thread(() -> {
                if (currentActivity.getId() == 0) {
                    db.activityDao().insert(currentActivity);
                } else {
                    db.activityDao().update(currentActivity);
                }
                // Schedule notification
                scheduleNotification(currentActivity);
                runOnUiThread(() -> {
                    Toast.makeText
                            (AddEditActivity.this, "Activity saved", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        } catch (ParseException e) {
            Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    private void scheduleNotification(Activity activity) {
        long reminderTime = activity.getDateTime() - (activity.getReminderMinutesBefore() * 60 * 1000);
        long delay = reminderTime - System.currentTimeMillis();
        if (delay > 0) {
            // Data untuk worker
            Data inputData = new Data.Builder()
                    .putString("title", activity.getTitle())
                    .putString("description", activity.getDescription())
                    .putInt("id", activity.getId())
                    .build();
            // Buat work request
            OneTimeWorkRequest notificationWork =
                    new OneTimeWorkRequest.Builder(NotificationWorker.class)
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .build();
            // Enqueue dengan unique name
            WorkManager.getInstance(this)
                    .enqueueUniqueWork(
                            "reminder_" + activity.getId(),
                            ExistingWorkPolicy.REPLACE,
                            notificationWork
                    );
            // Log untuk debugging
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Log.d("WorkManager", "Notifikasi dijadwalkan untuk: " + sdf.format(new Date(reminderTime)));
            Log.d("WorkManager", "Delay: " + delay + " ms (" + (delay/1000/60) + " menit dari sekarang)");

        } else {
            Log.d("WorkManager", "Waktu reminder sudah lewat: " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date(reminderTime)));
            Toast.makeText(this, "Waktu reminder sudah lewat", Toast.LENGTH_SHORT).show();
        }
    }
}
