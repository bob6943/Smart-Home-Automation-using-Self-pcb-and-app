package com.samadroid.smartbulb;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AddProjectActivity extends AppCompatActivity {

    private EditText etName, etToken, etSSID, etWifiPass;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        etName     = findViewById(R.id.etProjectName);
        etToken    = findViewById(R.id.etToken);
        etSSID     = findViewById(R.id.etSSID);
        etWifiPass = findViewById(R.id.etWifiPass);
        Button btnSave      = findViewById(R.id.btnSaveProject);
        Button btnSendToESP = findViewById(R.id.btnSendToESP);
        tvError = findViewById(R.id.tvAddError);

        int editIndex = getIntent().getIntExtra("EDIT_INDEX", -1);
        if (editIndex >= 0) {
            try {
                SharedPreferences prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);
                JSONArray arr = new JSONArray(prefs.getString("projects", "[]"));
                JSONObject obj = arr.getJSONObject(editIndex);
                etName.setText(obj.getString("name"));
                etToken.setText(obj.getString("token"));
                etSSID.setText(obj.optString("ssid", ""));
                etWifiPass.setText(obj.optString("wifipass", ""));
                btnSave.setText("UPDATE PROJECT");
                btnSendToESP.setText("Update & Send to ESP32");
            } catch (Exception e) { /* ignore */ }
        }

        btnSave.setOnClickListener(v -> {
            if (!validate()) return;
            saveProject(getIntent().getIntExtra("EDIT_INDEX", -1));
            finish();
        });

        btnSendToESP.setOnClickListener(v -> {
            if (!validate()) return;
            saveProject(getIntent().getIntExtra("EDIT_INDEX", -1));
            sendCredentialsToESP(
                    etSSID.getText().toString().trim(),
                    etWifiPass.getText().toString().trim(),
                    etToken.getText().toString().trim()
            );
        });
    }

    private boolean validate() {
        String name     = etName.getText().toString().trim();
        String token    = etToken.getText().toString().trim();
        String ssid     = etSSID.getText().toString().trim();
        String wifipass = etWifiPass.getText().toString().trim();

        if (name.isEmpty())        { tvError.setText("❌ Project name daalo!");        return false; }
        if (token.length() < 10)   { tvError.setText("❌ Sahi token daalo!");          return false; }
        if (ssid.isEmpty())        { tvError.setText("❌ WiFi naam daalo!");            return false; }
        if (wifipass.length() < 8) { tvError.setText("❌ WiFi password min 8 chars!"); return false; }

        return true;
    }

    private void saveProject(int editIndex) {
        try {
            String name     = etName.getText().toString().trim();
            String token    = etToken.getText().toString().trim();
            String ssid     = etSSID.getText().toString().trim();
            String wifipass = etWifiPass.getText().toString().trim();

            SharedPreferences prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);
            JSONArray arr = new JSONArray(prefs.getString("projects", "[]"));

            JSONObject obj = new JSONObject();
            obj.put("name",     name);
            obj.put("token",    token);
            obj.put("ssid",     ssid);
            obj.put("wifipass", wifipass);

            if (editIndex >= 0) arr.put(editIndex, obj);
            else arr.put(obj);

            prefs.edit().putString("projects", arr.toString()).apply();

        } catch (Exception e) {
            tvError.setText("Save error: " + e.getMessage());
        }
    }

    private void sendCredentialsToESP(String ssid, String pass, String token) {
        String url = "http://192.168.4.1/setup?ssid=" + ssid
                + "&pass=" + pass
                + "&token=" + token;

        tvError.setText("📶 ESP32 ko bhej raha hoon...");

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(this, "✅ ESP32 ko credentials bhej diye!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        tvError.setText("❌ ESP32 error: " + response.code());
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        tvError.setText("❌ ESP32 se connect nahi hua — AP mode mein hai?")
                );
            }
        }).start();
    }
}