package com.samadroid.smartbulb;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    boolean[] state = {false, false, false, false};

    int[] cards       = {R.id.cardCh1,     R.id.cardCh2,     R.id.cardCh3,     R.id.cardCh4};
    int[] bulbs       = {R.id.bulbCh1,     R.id.bulbCh2,     R.id.bulbCh3,     R.id.bulbCh4};
    int[] rays        = {R.id.raysCh1,     R.id.raysCh2,     R.id.raysCh3,     R.id.raysCh4};
    int[] statuses    = {R.id.statusCh1,   R.id.statusCh2,   R.id.statusCh3,   R.id.statusCh4};
    int[] nameIds     = {R.id.nameCh1,     R.id.nameCh2,     R.id.nameCh3,     R.id.nameCh4};
    int[] scheduleIds = {R.id.scheduleCh1, R.id.scheduleCh2, R.id.scheduleCh3, R.id.scheduleCh4};

    int[] bgOn  = {R.drawable.bg_ch1_on,  R.drawable.bg_ch2_on,  R.drawable.bg_ch3_on,  R.drawable.bg_ch4_on};
    int[] bgOff = {R.drawable.bg_ch1_off, R.drawable.bg_ch2_off, R.drawable.bg_ch3_off, R.drawable.bg_ch4_off};

    String[] defaultNames = {"Living Room", "Bedroom", "Kitchen", "Study"};
    String[] channelNames = new String[4];

    int[] colorsOn;

    private static final String BLYNK_URL    = "https://blynk.cloud/external/api/update?token=";
    private static final int    LOCATION_REQ = 1001;

    private String authToken  = "";
    private String projectKey = "";
    private String localEspIp = null;

    private OkHttpClient httpClient;
    private SharedPreferences prefs;
    private Handler scheduleHandler = new Handler();

    String ssid, wifipass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);

        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        if (authToken == null) authToken = "";

        projectKey = "proj_" + authToken.substring(0, Math.min(8, authToken.length()));

        String projectName = getIntent().getStringExtra("PROJECT_NAME");
        if (projectName != null) setTitle(projectName);

        ssid     = getIntent().getStringExtra("SSID");
        wifipass = getIntent().getStringExtra("WIFI_PASS");

        colorsOn = new int[]{
                Color.parseColor("#FFD600"),
                Color.parseColor("#00D4FF"),
                Color.parseColor("#FF6B35"),
                Color.parseColor("#B06DFF")
        };

        loadChannelNames();

        for (int i = 0; i < 4; i++) {
            final int index = i;
            findViewById(cards[i]).setOnClickListener(v -> toggleBulb(index));
            findViewById(nameIds[i]).setOnClickListener(v -> editChannelName(index));
            findViewById(scheduleIds[i]).setOnClickListener(v -> openSchedule(index));
        }

        findViewById(R.id.btnAllOn).setOnClickListener(v -> {
            for (int i = 0; i < 4; i++) if (!state[i]) toggleBulb(i);
        });

        findViewById(R.id.btnAllOff).setOnClickListener(v -> {
            for (int i = 0; i < 4; i++) if (state[i]) toggleBulb(i);
        });

        android.view.View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logout());

        // ✅ Location permission check — ESP scan ke liye zaruri
        checkLocationPermissionAndScan();

        startScheduleChecker();
    }

    // ── Permission ────────────────────────────────────────────

    void checkLocationPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission hai — seedha scan karo
            initEspConnection();
        } else {
            // Permission nahi — maango
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    }, LOCATION_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQ) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission mili — scan karo
                initEspConnection();
            } else {
                // Permission nahi mili — Blynk only mode
                Toast.makeText(this,
                        "⚠️ Location permission nahi — Sirf Blynk mode",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    void initEspConnection() {
        localEspIp = "192.168.0.100";
        prefs.edit().putString(projectKey + "_esp_ip", "192.168.0.100").apply();

    }
    // ── ESP Discovery ──────────────────────────────────────────

    void scanForEsp() {
        Toast.makeText(this, "🔍 ESP32 dhundh raha hoon...", Toast.LENGTH_SHORT).show();

        EspScanner.scan(this, ip -> runOnUiThread(() -> {
            if (ip != null) {
                localEspIp = ip;
                prefs.edit().putString(projectKey + "_esp_ip", ip).apply();
                Toast.makeText(this, "✅ ESP32 mila: " + ip, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ ESP32 nahi mila — Blynk use karega", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    void verifyOrRescan(String savedIp) {
        String url = "http://" + savedIp + "/ping";
        asyncRequest(url, new RequestCallback() {
            @Override public void onSuccess() { /* IP sahi hai */ }
            @Override public void onFailure() {
                runOnUiThread(() -> {
                    localEspIp = null;
                    prefs.edit().remove(projectKey + "_esp_ip").apply();
                    scanForEsp();
                });
            }
        });
    }

    // ── Channel Names ──────────────────────────────────────────

    void loadChannelNames() {
        for (int i = 0; i < 4; i++) {
            channelNames[i] = prefs.getString(projectKey + "_name_" + i, defaultNames[i]);
            ((TextView) findViewById(nameIds[i])).setText(channelNames[i]);
        }
    }

    void editChannelName(int index) {
        EditText input = new EditText(this);
        input.setText(channelNames[index]);
        input.setTextColor(Color.WHITE);
        input.setSelection(channelNames[index].length());

        new AlertDialog.Builder(this)
                .setTitle("CH " + (index + 1) + " ka naam badlo")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        channelNames[index] = newName;
                        prefs.edit().putString(projectKey + "_name_" + index, newName).apply();
                        ((TextView) findViewById(nameIds[index])).setText(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Schedule ───────────────────────────────────────────────

    void openSchedule(int channelIndex) {
        Intent intent = new Intent(this, ScheduleActivity.class);
        intent.putExtra("CHANNEL_INDEX", channelIndex);
        intent.putExtra("CHANNEL_NAME",  channelNames[channelIndex]);
        intent.putExtra("PROJECT_KEY",   projectKey);
        startActivity(intent);
    }

    void startScheduleChecker() {
        scheduleHandler.postDelayed(new Runnable() {
            @Override public void run() {
                checkSchedules();
                scheduleHandler.postDelayed(this, 30000);
            }
        }, 5000);
    }

    void checkSchedules() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentHour   = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(java.util.Calendar.MINUTE);
        String currentTime = String.format("%02d:%02d", currentHour, currentMinute);

        for (int i = 0; i < 4; i++) {
            boolean enabled = prefs.getBoolean(projectKey + "_sch_en_" + i, false);
            if (!enabled) continue;

            String onTime  = prefs.getString(projectKey + "_sch_on_"  + i, "");
            String offTime = prefs.getString(projectKey + "_sch_off_" + i, "");

            final int idx = i;
            if (currentTime.equals(onTime)  && !state[idx]) runOnUiThread(() -> toggleBulb(idx));
            else if (currentTime.equals(offTime) && state[idx]) runOnUiThread(() -> toggleBulb(idx));
        }
    }

    // ── Toggle ────────────────────────────────────────────────

    void toggleBulb(int i) {
        state[i] = !state[i];
        updateUI(i);
        sendRequest(i + 1, state[i] ? 1 : 0, i);
    }

    void updateUI(int i) {
        LinearLayout card = findViewById(cards[i]);
        ImageView bulb    = findViewById(bulbs[i]);
        ImageView ray     = findViewById(rays[i]);
        TextView status   = findViewById(statuses[i]);

        if (state[i]) {
            card.setBackgroundResource(bgOn[i]);
            bulb.setColorFilter(colorsOn[i]);
            ray.setColorFilter(colorsOn[i]);
            status.setText("● ON");
            status.setTextColor(colorsOn[i]);
            ray.animate().alpha(1f).setDuration(300).start();
            bulb.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120)
                    .withEndAction(() ->
                            bulb.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    ).start();
        } else {
            card.setBackgroundResource(bgOff[i]);
            bulb.setColorFilter(Color.parseColor("#2A2A2E"));
            status.setText("○ OFF");
            status.setTextColor(Color.parseColor("#333333"));
            ray.animate().alpha(0f).setDuration(300).start();
        }
    }

    // ── Network ───────────────────────────────────────────────

    void sendRequest(int pin, int value, int channelIndex) {
        if (authToken.isEmpty()) {
            Toast.makeText(this, "Auth Token missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (localEspIp != null) {
            String localUrl = "http://" + localEspIp + "/relay?pin=" + pin + "&value=" + value;
            asyncRequest(localUrl, new RequestCallback() {
                @Override public void onSuccess() { }
                @Override public void onFailure() {
                    runOnUiThread(() -> {
                        localEspIp = null;
                        prefs.edit().remove(projectKey + "_esp_ip").apply();
                        scanForEsp();
                    });
                    tryBlynk(pin, value, channelIndex);
                }
            });
        } else {
            tryBlynk(pin, value, channelIndex);
        }
    }

    void tryBlynk(int pin, int value, int channelIndex) {
        String blynkUrl = BLYNK_URL + authToken + "&v" + pin + "=" + value;
        asyncRequest(blynkUrl, new RequestCallback() {
            @Override public void onSuccess() { }
            @Override public void onFailure() {
                runOnUiThread(() -> {
                    state[channelIndex] = !state[channelIndex];
                    updateUI(channelIndex);
                    Toast.makeText(MainActivity.this,
                            "❌ No internet & ESP nahi mila", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ── Async Request Helper ──────────────────────────────────

    interface RequestCallback {
        void onSuccess();
        void onFailure();
    }

    void asyncRequest(String url, RequestCallback callback) {
        Request request = new Request.Builder().url(url).get().build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess();
                else callback.onFailure();
                response.close();
            }
            @Override public void onFailure(Call call, IOException e) {
                callback.onFailure();
            }
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduleHandler.removeCallbacksAndMessages(null);
    }

    void logout() {
        FirebaseAuth.getInstance().signOut();
        prefs.edit().clear().apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}