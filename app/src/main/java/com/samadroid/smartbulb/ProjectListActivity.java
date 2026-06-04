package com.samadroid.smartbulb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectListActivity extends AppCompatActivity {

    private LinearLayout projectContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);
        projectContainer = findViewById(R.id.projectContainer);

        findViewById(R.id.btnAddProject).setOnClickListener(v ->
                startActivity(new Intent(this, AddProjectActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void loadProjects() {
        projectContainer.removeAllViews();
        try {
            JSONArray arr = new JSONArray(prefs.getString("projects", "[]"));

            if (arr.length() == 0) { showEmptyState(); return; }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                addProjectCard(
                        obj.getString("name"),
                        obj.getString("token"),
                        obj.optString("ssid", ""),
                        obj.optString("wifipass", ""),
                        i
                );
            }
        } catch (Exception e) {
            showEmptyState();
        }
    }

    private void addProjectCard(String name, String token, String ssid, String wifipass, int index) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.parseColor("#161B22"));
        card.setRadius(24f);
        card.setCardElevation(6f);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(40, 36, 24, 36);

        TextView tvName = new TextView(this);
        tvName.setText("💡  " + name);
        tvName.setTextSize(16f);
        tvName.setTextColor(Color.parseColor("#C9D1D9"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("✏️");
        btnEdit.setTextSize(18f);
        btnEdit.setPadding(16, 8, 16, 8);
        btnEdit.setOnClickListener(v -> editProject(index));

        TextView btnDelete = new TextView(this);
        btnDelete.setText("🗑️");
        btnDelete.setTextSize(18f);
        btnDelete.setPadding(8, 8, 8, 8);
        btnDelete.setOnClickListener(v -> confirmDelete(index, name));

        TextView tvArrow = new TextView(this);
        tvArrow.setText("›");
        tvArrow.setTextSize(22f);
        tvArrow.setTextColor(Color.parseColor("#FFD700"));
        tvArrow.setPadding(8, 0, 0, 0);

        inner.addView(tvName);
        inner.addView(btnEdit);
        inner.addView(btnDelete);
        inner.addView(tvArrow);
        card.addView(inner);

        card.setOnClickListener(v -> openProject(name, token, ssid, wifipass));
        projectContainer.addView(card);
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("Koi project nahi hai.\n\n+ button se naya add karo!");
        tv.setTextColor(Color.parseColor("#8B949E"));
        tv.setTextSize(15f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        projectContainer.addView(tv);
    }

    private void openProject(String name, String token, String ssid, String wifipass) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("AUTH_TOKEN",   token);
        intent.putExtra("PROJECT_NAME", name);
        intent.putExtra("SSID",         ssid);
        intent.putExtra("WIFI_PASS",    wifipass);
        startActivity(intent);
    }

    private void editProject(int index) {
        Intent intent = new Intent(this, AddProjectActivity.class);
        intent.putExtra("EDIT_INDEX", index);
        startActivity(intent);
    }

    private void confirmDelete(int index, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project?")
                .setMessage("\"" + name + "\" delete karna chahte ho?")
                .setPositiveButton("Delete", (d, w) -> deleteProject(index))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProject(int index) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("projects", "[]"));
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (i != index) newArr.put(arr.get(i));
            }
            prefs.edit().putString("projects", newArr.toString()).apply();
            loadProjects();
            Toast.makeText(this, "Project deleted!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { /* ignore */ }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        prefs.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}