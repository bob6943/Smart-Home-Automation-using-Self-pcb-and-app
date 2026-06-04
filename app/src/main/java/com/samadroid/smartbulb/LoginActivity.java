package com.samadroid.smartbulb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private LinearLayout btnGoogle;
    private TextView tvForgot, tvError, tvSignup;
    private ProgressBar progressBar;

    private SharedPreferences prefs;

    // Yeh flag batata hai — signup se wapas aaye hain ya fresh open hai
    private boolean comingFromSignup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail    = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnGoogle  = findViewById(R.id.btnGoogleSignIn);
        tvForgot   = findViewById(R.id.tvForgotPassword);
        tvSignup   = findViewById(R.id.tvSignup);
        tvError    = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        prefs = getSharedPreferences("SBPrefs", MODE_PRIVATE);

        // Auto login — sirf fresh open par
        if (mAuth.getCurrentUser() != null) {
            goToNext();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        tvForgot.setOnClickListener(v -> resetPassword());

        tvSignup.setOnClickListener(v -> {
            comingFromSignup = true;
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Signup se wapas aaye hain toh auto-login mat karo
        if (comingFromSignup) {
            comingFromSignup = false;
            // Firebase se sign out karo taaki login page properly dikhe
            mAuth.signOut();
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tvError.setText("❌ Email/Password empty");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        prefs.edit().putBoolean("loggedIn", true).apply();
                        goToNext();
                    } else {
                        tvError.setText("❌ " + task.getException().getMessage());
                    }
                });
    }

    private void signupUser() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tvError.setText("❌ Email/Password empty");
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
                        Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();
                        prefs.edit().putBoolean("loggedIn", true).apply();
                        goToNext();
                    } else {
                        tvError.setText("❌ " + task.getException().getMessage());
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (Exception e) {
                tvError.setText("❌ Google Sign-In Failed");
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        goToNext();
                    } else {
                        tvError.setText("❌ Firebase Auth Failed");
                    }
                });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset link sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToNext() {
        startActivity(new Intent(this, ProjectListActivity.class));
        finish();
    }
}