package com.example.jegyzetapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jegyzetapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private boolean isPasswordVisible = false;

    private EditText etEmail, etPassword, etPasswordConfirm;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmailRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);

        Button btnRegister = findViewById(R.id.btnRegister);

        // ***** Beállítjuk a jelszómezőn az OnTouchListener–t kívül a regisztráció gomb OnClickListener–én  *****
        etPassword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_END = 2; // jobb oldali drawable indexe
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Ha már beállítottad az XML-ben a drawable-t, akkor itt elvárjuk, hogy ne legyen null.
                    if (etPassword.getCompoundDrawablesRelative()[DRAWABLE_END] != null) {
                        int drawableWidth = etPassword.getCompoundDrawablesRelative()[DRAWABLE_END].getBounds().width();
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

        // (Ha szeretnéd, a megerősítő jelszó mezőhöz is hasonló OnTouchListener-t adhatsz, de lehet, hogy külön kezelni szeretnéd.)
        etPasswordConfirm.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_END = 2; // jobb oldali drawable indexe
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (etPasswordConfirm.getCompoundDrawablesRelative()[DRAWABLE_END] != null) {
                        int drawableWidth = etPasswordConfirm.getCompoundDrawablesRelative()[DRAWABLE_END].getBounds().width();
                        if (event.getRawX() >= (etPasswordConfirm.getRight() - drawableWidth - etPasswordConfirm.getPaddingEnd())) {
                            // Használhatsz külön logikát, vagy akár ugyanazt az állapotváltoztatást.
                            boolean isVisible = etPasswordConfirm.getTransformationMethod() instanceof HideReturnsTransformationMethod;
                            if (!isVisible) {
                                etPasswordConfirm.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                etPasswordConfirm.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye_visible, 0);
                            } else {
                                etPasswordConfirm.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                etPasswordConfirm.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                            }
                            etPasswordConfirm.setSelection(etPasswordConfirm.getText().length());
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // ****** Regisztráció gomb OnClickListener ******
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String passConfirm = etPasswordConfirm.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty() || passConfirm.isEmpty()) {
                Toast.makeText(this, "Minden mező kitöltése kötelező", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pass.equals(passConfirm)) {
                Toast.makeText(this, "A jelszavak nem egyeznek!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();
                        // Regisztráció után rögtön beléptet a user, mehetsz a MainActivity-be:
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        if (e.getMessage().contains("already in use")) {
                            Toast.makeText(this, "Ezzel az e-mail címmel már regisztráltak!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Hiba a regisztrációnál: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}