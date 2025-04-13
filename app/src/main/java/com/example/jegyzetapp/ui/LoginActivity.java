package com.example.jegyzetapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jegyzetapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private boolean isPasswordVisible = false;
    private EditText etEmail, etPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);
        CheckBox cbStayLoggedIn = findViewById(R.id.cbStayLoggedIn);

        // Bejelentkezés gombra kattintás:
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Kérem, töltse ki az e-mailt és jelszót", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (cbStayLoggedIn.isChecked()) {
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("stayLoggedIn", true)
                                    .apply();
                        } else {
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("stayLoggedIn", false)
                                    .apply();
                        }
                        Toast.makeText(this, "Sikeres bejelentkezés", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba a bejelentkezésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        etPassword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_END = 2; // jobb oldali drawable indexe
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (etPassword.getCompoundDrawables()[DRAWABLE_END] != null) {
                        int drawableWidth = etPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width();
                        if (event.getRawX() >= (etPassword.getRight() - drawableWidth - etPassword.getPaddingEnd())) {
                            isPasswordVisible = !isPasswordVisible;
                            if (isPasswordVisible) {
                                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                // Ha van külön ikond a látható állapothoz:
                                etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye_visible, 0);
                            } else {
                                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                            }
                            etPassword.setSelection(etPassword.getText().length());
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // Átirányítás a regisztrációs oldalra
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
