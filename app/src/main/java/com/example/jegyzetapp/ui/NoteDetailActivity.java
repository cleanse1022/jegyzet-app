package com.example.jegyzetapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.jegyzetapp.R;
import com.example.jegyzetapp.model.Note;
import com.example.jegyzetapp.util.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class NoteDetailActivity extends AppCompatActivity {

    private static final String TAG = "NoteDetailActivity";
    private EditText etTitle, etNoteContent;
    private FirebaseFirestore db;
    private String noteId = null;

    // Értesítési engedély kérésére ActivityResultLauncher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Értesítési engedély megadva");
                } else {
                    Log.d(TAG, "Értesítési engedély megtagadva");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        etTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        Button btnSave = findViewById(R.id.btnSaveNote);
        Button btnShare = findViewById(R.id.btnShareNote);
        Button btnSetReminder = findViewById(R.id.btnSetReminder);

        db = FirebaseFirestore.getInstance();

        // Értesítési csatorna inicializálása
        NotificationHelper.createNotificationChannel(this);
        
        // Értesítési engedély ellenőrzése
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (getIntent().hasExtra("noteId")) {
            noteId = getIntent().getStringExtra("noteId");
            loadNoteData(noteId);
        }

        btnSave.setOnClickListener(view -> {
            Log.d(TAG, "Mentés gomb megnyomva, saveNote() hívása...");
            saveNote();
        });
        
        // Megosztás gomb eseménykezelő
        btnShare.setOnClickListener(view -> {
            shareNote();
        });
        
        // Emlékeztető gomb eseménykezelő
        btnSetReminder.setOnClickListener(view -> {
            if (noteId == null) {
                Toast.makeText(this, "Előbb mentsd el a jegyzetet az emlékeztető beállításához!", Toast.LENGTH_SHORT).show();
                return;
            }
            showTimePickerDialog();
        });
    }

    private void loadNoteData(String noteId) {
        db.collection("notes")
                .document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Note note = documentSnapshot.toObject(Note.class);
                        if (note != null) {
                            etTitle.setText(note.getTitle());
                            etNoteContent.setText(note.getContent());
                            Log.d(TAG, "Jegyzet adatok betöltve.");
                        } else {
                            Log.d(TAG, "loadNoteData: A note objektum null.");
                        }
                    } else {
                        Log.d(TAG, "loadNoteData: A dokumentum nem létezik.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hiba a jegyzet betöltésekor: ", e);
                    Toast.makeText(NoteDetailActivity.this, "Hiba a jegyzet betöltésekor", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(NoteDetailActivity.this, "A jegyzet üres!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() && content.length() > 20) {
            title = content.substring(0, 20) + "...";
        }

        // Létrehozzuk a Note objektumot
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setCreationDate(new Date());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó, nem menthető a jegyzet!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId == null) {
            note.setUid(currentUser.getUid());
            final String finalTitle = title; // Final változó a lambda kifejezéshez
            db.collection("notes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Új jegyzet sikeresen létrehozva, Document ID: " + documentReference.getId());
                        Toast.makeText(NoteDetailActivity.this, "Jegyzet létrehozva", Toast.LENGTH_SHORT).show();
                        
                        // Értesítés küldése új jegyzet létrehozásáról
                        NotificationHelper.showNoteReminderNotification(
                                NoteDetailActivity.this,
                                "Új jegyzet létrehozva",
                                finalTitle);
                                
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hiba új jegyzet mentésekor: ", e);
                        Log.e(TAG, "note.getUid() = " + note.getUid());
                        Toast.makeText(NoteDetailActivity.this, "Hiba a mentésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Meglévő jegyzet frissítése: itt nem módosítjuk a uid értékét, mert az rögzítve marad.
            final String finalTitle = title; // Final változó a lambda kifejezéshez
            db.collection("notes")
                    .document(noteId)
                    .update(
                            "title", title,
                            "content", content,
                            "creationDate", new Date()
                    )
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Jegyzet sikeresen frissítve");
                        Toast.makeText(NoteDetailActivity.this, "Jegyzet frissítve", Toast.LENGTH_SHORT).show();
                        
                        // Értesítés küldése jegyzet frissítéséről
                        NotificationHelper.showNoteReminderNotification(
                                NoteDetailActivity.this,
                                "Jegyzet frissítve",
                                finalTitle);
                                
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hiba jegyzet frissítésekor: ", e);
                        Toast.makeText(NoteDetailActivity.this, "Hiba a frissítésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    
    /**
     * Jegyzet megosztása más alkalmazásokkal + Időválasztó dialógus megjelenítése emlékeztető beállításához
     */
    private void showTimePickerDialog() {
        // Aktuális idő beállítása
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, selectedMinute) -> {
                    // Az időválasztó dialógus eredménye
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        String content = etNoteContent.getText().toString().trim();
                        if (content.length() > 20) {
                            title = content.substring(0, 20) + "...";
                        } else {
                            title = "Jegyzet";
                        }
                    }
                    
                    // Emlékeztető beállítása
                    com.example.jegyzetapp.util.ReminderHelper.setReminder(
                            this, 
                            noteId, 
                            title, 
                            hourOfDay, 
                            selectedMinute
                    );
                },
                hour,
                minute,
                true // 24 órás formátum
        );
        
        timePickerDialog.setTitle("Válassz időpontot az emlékeztetőhöz");
        timePickerDialog.show();
    }
    
    private void shareNote() {
        String title = etTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        
        if (content.isEmpty()) {
            Toast.makeText(this, "Nincs mit megosztani!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Ha nincs cím, akkor az első 20 karakter lesz a cím
        if (title.isEmpty() && content.length() > 20) {
            title = content.substring(0, 20) + "...";
        } else if (title.isEmpty()) {
            title = "Jegyzet";
        }
        
        final String finalTitle = title; // Final változó
        final String finalContent = content; // Final változó
        
        String shareText = finalTitle + "\n\n" + finalContent;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, finalTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, "Jegyzet megosztása"));
    }
}