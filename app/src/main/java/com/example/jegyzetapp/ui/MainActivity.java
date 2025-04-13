package com.example.jegyzetapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.jegyzetapp.R;
import com.example.jegyzetapp.adapter.NoteAdapter;
import com.example.jegyzetapp.model.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NoteAdapter noteAdapter;
    private List<Note> noteList;
    private List<Note> originalNoteList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration notesListener;
    private ActionMode actionMode;

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Törlés megerősítése")
                        .setMessage("Biztosan törli a kiválasztott jegyzet(ek)et?")
                        .setPositiveButton("Igen", (dialog, which) -> {
                            deleteSelectedNotes();
                            mode.finish();
                        })
                        .setNegativeButton("Nem", null)
                        .show();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            noteAdapter.clearSelection();
            actionMode = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddNote);
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Kijelentkezés
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("stayLoggedIn", false)
                    .apply();

            Toast.makeText(MainActivity.this, "Sikeres kijelentkezés", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        searchView.setOnCloseListener(() -> {
            searchView.setQuery("", false);
            filterNotes("");
            return true;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNotes(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        noteList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        noteAdapter = new NoteAdapter(
                noteList,
                note -> {
                    Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                    intent.putExtra("noteId", note.getId());
                    startActivity(intent);
                },
                this::deleteNote,
                noteId -> {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    if (noteAdapter.getSelectedIds().isEmpty() && actionMode != null) {
                        actionMode.finish();
                    }
                }
        );
        recyclerView.setAdapter(noteAdapter);

        loadNotes();

        fabAdd.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, NoteDetailActivity.class)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SearchView searchView = findViewById(R.id.searchView);
        outState.putString("query", searchView.getQuery().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String query = savedInstanceState.getString("query", "");
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setQuery(query, false);
        filterNotes(query);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Valós idejű figyelés
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            notesListener = db.collection("notes")
                    .whereEqualTo("uid", uid)
                    .orderBy("creationDate", Query.Direction.DESCENDING)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        noteList.clear();
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                Note note = document.toObject(Note.class);
                                if (note != null) {
                                    note.setId(document.getId());
                                    noteList.add(note);
                                }
                            }
                        }
                        noteAdapter.notifyDataSetChanged();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        SearchView searchView = findViewById(R.id.searchView);
        searchView.clearFocus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }
    }

    private void filterNotes(String query) {
        List<Note> filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(originalNoteList);
        } else {
            for (Note note : originalNoteList) {
                if (note.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        note.getContent().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(note);
                }
            }
        }
        noteList.clear();
        noteList.addAll(filteredList);
        noteAdapter.notifyDataSetChanged();
    }

    private void loadNotes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(noteAdapter);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down));

        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("notes")
                    .whereEqualTo("uid", uid)
                    .orderBy("creationDate", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        noteList.clear();
                        originalNoteList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Note note = document.toObject(Note.class);
                            if (note != null) {
                                note.setId(document.getId());
                                noteList.add(note);
                            }
                        }
                        originalNoteList.addAll(noteList);
                        noteAdapter.notifyDataSetChanged();
                        recyclerView.scheduleLayoutAnimation();

                        // Ha volt keresési kifejezés, újraszűrjük
                        SearchView searchView = findViewById(R.id.searchView);
                        String query = searchView.getQuery().toString();
                        if (!query.trim().isEmpty()) {
                            filterNotes(query);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hiba a jegyzetek betöltésekor", e);
                        Toast.makeText(MainActivity.this, "Hiba a jegyzetek betöltésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void deleteNote(Note note) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Törlés megerősítése")
                .setMessage("Biztosan törli ezt a jegyzetet?")
                .setPositiveButton("Igen", (dialog, which) -> db.collection("notes").document(note.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            noteList.remove(note);
                            noteAdapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "Jegyzet törölve", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Hiba a törlésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Törlés hiba:", e);
                        }))
                .setNegativeButton("Nem", null)
                .show();
    }

    private void deleteSelectedNotes() {
        Set<String> selectedIds = noteAdapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            return;
        }
        for (String id : selectedIds) {
            db.collection("notes").document(id)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Jegyzet törölve: " + id))
                    .addOnFailureListener(e -> Log.e(TAG, "Hiba jegyzet törlésénél: " + id, e));
        }

        List<Note> toRemove = new ArrayList<>();
        for (Note n : noteList) {
            if (selectedIds.contains(n.getId())) {
                toRemove.add(n);
            }
        }
        noteList.removeAll(toRemove);
        noteAdapter.clearSelection();
        noteAdapter.notifyDataSetChanged();
        Toast.makeText(MainActivity.this, "Kiválasztott jegyzet(ek) törölve", Toast.LENGTH_SHORT).show();
    }
}
