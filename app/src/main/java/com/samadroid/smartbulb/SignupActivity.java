package com.samadroid.smartbulb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText etEmail, etPassword, etUsername;
    private Button btnSignup;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();

        // Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUsername = findViewById(R.id.etUsername);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        btnSignup.setOnClickListener(v -> createUser());
    }

    private void createUser() {

        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tvError.setText("❌ Email & Password required");
            return;
        }

        if (pass.length() < 6) {
            tvError.setText("❌ Password must be 6+ characters");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        // OPTIONAL: Save username in Firebase profile
                        if (!username.isEmpty() && user != null) {
                            UserProfileChangeRequest profileUpdates =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(username)
                                            .build();

                            user.updateProfile(profileUpdates);
                        }

                        Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();

                        // Go to main dashboard
                        startActivity(new Intent(this, ProjectListActivity.class));
                        finish();

                    } else {
                        tvError.setText("❌ " + task.getException().getMessage());
                    }
                });
    }
}