package com.example.jegyzetapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jegyzetapp.R;
import com.example.jegyzetapp.model.Note;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class NoteDetailActivity extends AppCompatActivity {

    private static final String TAG = "NoteDetailActivity";
    private EditText etTitle, etNoteContent;
    private FirebaseFirestore db;
    private String noteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        etTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        Button btnSave = findViewById(R.id.btnSaveNote);

        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("noteId")) {
            noteId = getIntent().getStringExtra("noteId");
            loadNoteData(noteId);
        }

        btnSave.setOnClickListener(view -> {
            Log.d(TAG, "Mentés gomb megnyomva, saveNote() hívása...");
            saveNote();
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
            db.collection("notes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Új jegyzet sikeresen létrehozva, Document ID: " + documentReference.getId());
                        Toast.makeText(NoteDetailActivity.this, "Jegyzet létrehozva", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hiba új jegyzet mentésekor: ", e);
                        Log.e(TAG, "note.getUid() = " + note.getUid());
                        Toast.makeText(NoteDetailActivity.this, "Hiba a mentésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Meglévő jegyzet frissítése: itt nem módosítjuk a uid értékét, mert az rögzítve marad.
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
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hiba jegyzet frissítésekor: ", e);
                        Toast.makeText(NoteDetailActivity.this, "Hiba a frissítésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}