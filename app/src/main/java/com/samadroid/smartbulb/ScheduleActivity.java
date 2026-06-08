package com.samadroid.smartbulb;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class ScheduleActivity extends AppCompatActivity {

    private String onTime  = "";
    private String offTime = "";
    private String projectKey = "";
    private int channelIndex  = 0;

    private Button btnOnTime, btnOffTime;
    private SwitchCompat switchEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        channelIndex = getIntent().getIntExtra("CHANNEL_INDEX", 0);
        projectKey   = getIntent().getStringExtra("PROJECT_KEY");
        String channelName = getIntent().getStringExtra("CHANNEL_NAME");

        ((TextView) findViewById(R.id.tvScheduleTitle)).setText(channelName + " Schedule");

        btnOnTime    = findViewById(R.id.btnOnTime);
        btnOffTime   = findViewById(R.id.btnOffTime);
        switchEnable = findViewById(R.id.switchEnable);
        Button btnSave = findViewById(R.id.btnSaveSchedule);

        // Load saved values
        SharedPreferences prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(projectKey + "_sch_en_" + channelIndex, false);
        onTime  = prefs.getString(projectKey + "_sch_on_"  + channelIndex, "");
        offTime = prefs.getString(projectKey + "_sch_off_" + channelIndex, "");

        switchEnable.setChecked(enabled);
        btnOnTime.setText(onTime.isEmpty()   ? "ON time select karo"  : "ON:  " + formatDisplay(onTime));
        btnOffTime.setText(offTime.isEmpty() ? "OFF time select karo" : "OFF: " + formatDisplay(offTime));

        // ✅ AM/PM mode — false = 12-hour with AM/PM
        btnOnTime.setOnClickListener(v -> {
            int defHour = 7, defMin = 0;
            if (!onTime.isEmpty()) {
                defHour = Integer.parseInt(onTime.split(":")[0]);
                defMin  = Integer.parseInt(onTime.split(":")[1]);
            }
            TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
                onTime = String.format("%02d:%02d", h, m); // 24hr format save karo internally
                btnOnTime.setText("ON:  " + formatDisplay(onTime));
            }, defHour, defMin, false); // ✅ false = AM/PM mode
            tpd.show();
        });

        btnOffTime.setOnClickListener(v -> {
            int defHour = 22, defMin = 0;
            if (!offTime.isEmpty()) {
                defHour = Integer.parseInt(offTime.split(":")[0]);
                defMin  = Integer.parseInt(offTime.split(":")[1]);
            }
            TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
                offTime = String.format("%02d:%02d", h, m);
                btnOffTime.setText("OFF: " + formatDisplay(offTime));
            }, defHour, defMin, false); // ✅ false = AM/PM mode
            tpd.show();
        });

        btnSave.setOnClickListener(v -> {
            if (switchEnable.isChecked() && (onTime.isEmpty() || offTime.isEmpty())) {
                Toast.makeText(this, "ON aur OFF dono time daalo!", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit()
                    .putBoolean(projectKey + "_sch_en_"  + channelIndex, switchEnable.isChecked())
                    .putString(projectKey  + "_sch_on_"  + channelIndex, onTime)
                    .putString(projectKey  + "_sch_off_" + channelIndex, offTime)
                    .apply();

            // ✅ Foreground Service start/stop karo schedule ke basis pe
            updateScheduleService(prefs);

            Toast.makeText(this, "✅ Schedule save ho gaya!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // ✅ 24hr → 12hr AM/PM display format
    private String formatDisplay(String time24) {
        try {
            int h = Integer.parseInt(time24.split(":")[0]);
            int m = Integer.parseInt(time24.split(":")[1]);
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12;
            if (h12 == 0) h12 = 12;
            return String.format("%d:%02d %s", h12, m, ampm);
        } catch (Exception e) {
            return time24;
        }
    }

    // ✅ Check karo koi bhi schedule enabled hai — Service start/stop karo
    void updateScheduleService(SharedPreferences prefs) {
        boolean anyEnabled = false;
        for (int i = 0; i < 4; i++) {
            if (prefs.getBoolean(projectKey + "_sch_en_" + i, false)) {
                anyEnabled = true;
                break;
            }
        }

        Intent serviceIntent = new Intent(this, ScheduleService.class);
        serviceIntent.putExtra("PROJECT_KEY", projectKey);

        if (anyEnabled) {
            startForegroundService(serviceIntent); // ✅ Service start karo
        } else {
            stopService(serviceIntent); // ✅ Koi schedule nahi — service band karo
        }
    }
}