package com.example.jegyzetapp.util;

import android.util.Log;

import com.example.jegyzetapp.model.Note;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Segédosztály komplex Firestore lekérdezésekhez
 * 
 * MEGJEGYZÉS: Ez az osztály még nincs aktivan használva, de későbbi fejlesztésekhez lett előkészítve.
 * Kényelmes komplex lekérdezési mintákat tartalmaz a Firestore adatbázishoz.
 */
public class FirestoreQueryHelper {

    private static final String TAG = "FirestoreQueryHelper";

    /**
     * A mai napon létrehozott jegyzetek lekérdezése
     * 
     * @param userId A felhasználó azonosítója
     * @param callback Visszahívás a jegyzetek listájával
     */
    public static void getTodaysNotes(String userId, Consumer<List<Note>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Mai nap kezdetének és végének meghatározása
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();
        
        // Komplex lekérdezés: felhasználó szerint + tartomány szűrés + rendezés
        db.collection("notes")
            .whereEqualTo("uid", userId)
            .whereGreaterThanOrEqualTo("creationDate", startOfDay)
            .whereLessThanOrEqualTo("creationDate", endOfDay)
            .orderBy("creationDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Note> noteList = new ArrayList<>();
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    Note note = document.toObject(Note.class);
                    if (note != null) {
                        note.setId(document.getId());
                        noteList.add(note);
                    }
                }
                Log.d(TAG, "Mai jegyzetek száma: " + noteList.size());
                callback.accept(noteList);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Hiba a mai jegyzetek lekérdezésekor", e);
                callback.accept(new ArrayList<>());
            });
    }

    /**
     * Lapozott jegyzetek lekérdezése (paginálás implementációja)
     * Egy adott limit szerint visszaadja a jegyzeteket, majd jelzi, hogy van-e még adat
     * 
     * @param userId A felhasználó azonosítója
     * @param limit A jegyzetek maximális száma
     * @param lastDocumentSnapshot Az utolsó jegyzet a lapozáshoz (null az első oldalhoz)
     * @param callback Visszahívás a jegyzetek listájával és a következő lapozási ponttal
     */
    public static void getPaginatedNotes(
            String userId, 
            int limit, 
            DocumentSnapshot lastDocumentSnapshot,
            PaginationCallback callback) {
            
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Query query = db.collection("notes")
                .whereEqualTo("uid", userId)
                .orderBy("creationDate", Query.Direction.DESCENDING)
                .limit(limit);
                
        // Ha van korábbi dokumentum, akkor attól kezdjük a lekérdezést
        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot);
        }
        
        query.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Note> noteList = new ArrayList<>();
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    Note note = document.toObject(Note.class);
                    if (note != null) {
                        note.setId(document.getId());
                        noteList.add(note);
                    }
                }
                
                // Az utolsó dokumentum elmentése a következő lapozáshoz
                DocumentSnapshot lastVisible = null;
                if (!queryDocumentSnapshots.isEmpty()) {
                    lastVisible = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.size() - 1);
                }
                
                // Ellenőrizzük, van-e még adat a következő oldalhoz
                boolean hasMoreData = noteList.size() >= limit;
                
                Log.d(TAG, "Lapozott jegyzetek száma: " + noteList.size() + 
                        ", van még: " + hasMoreData);
                
                callback.onResult(noteList, lastVisible, hasMoreData);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Hiba a lapozott jegyzetek lekérdezésekor", e);
                callback.onResult(new ArrayList<>(), null, false);
            });
    }
    
    /**
     * Visszahívási interfész a lapozott lekérdezéshez
     */
    public interface PaginationCallback {
        /**
         * Visszahívás a lapozott lekérdezés eredményével
         * 
         * @param notes A jegyzetek listája
         * @param lastDocumentSnapshot Az utolsó dokumentum a következő lapozáshoz
         * @param hasMoreData Van-e még adat a következő oldalhoz
         */
        void onResult(List<Note> notes, DocumentSnapshot lastDocumentSnapshot, boolean hasMoreData);
    }
}
