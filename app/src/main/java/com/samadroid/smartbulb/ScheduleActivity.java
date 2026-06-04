package com.samadroid.smartbulb;

import android.app.TimePickerDialog;
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
        btnOnTime.setText(onTime.isEmpty()  ? "ON time select karo"  : "ON:  " + onTime);
        btnOffTime.setText(offTime.isEmpty() ? "OFF time select karo" : "OFF: " + offTime);

        btnOnTime.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
                onTime = String.format("%02d:%02d", h, m);
                btnOnTime.setText("ON:  " + onTime);
            }, 7, 0, true);
            tpd.show();
        });

        btnOffTime.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
                offTime = String.format("%02d:%02d", h, m);
                btnOffTime.setText("OFF: " + offTime);
            }, 22, 0, true);
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
            Toast.makeText(this, "✅ Schedule save ho gaya!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}