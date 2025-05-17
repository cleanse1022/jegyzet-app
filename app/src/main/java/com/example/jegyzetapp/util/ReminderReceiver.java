package com.example.jegyzetapp.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver az emlékeztetők fogadására
 */
public class ReminderReceiver extends BroadcastReceiver {
    
    private static final String TAG = "ReminderReceiver";
    public static final String EXTRA_NOTE_TITLE = "note_title";
    public static final String EXTRA_NOTE_ID = "note_id";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Emlékeztető érkezett");
        
        // Kinyerjük a jegyzet adatokat az intentből
        String noteTitle = intent.getStringExtra(EXTRA_NOTE_TITLE);
        String noteId = intent.getStringExtra(EXTRA_NOTE_ID);
        
        if (noteTitle == null || noteTitle.isEmpty()) {
            noteTitle = "Emlékeztető";
        }
        
        Log.d(TAG, "Jegyzet ID: " + noteId + ", Cím: " + noteTitle);
        
        // Létrehozunk egy Intent-et, ami a jegyzet részletek képernyőre visz
        Intent noteDetailIntent = new Intent(context, com.example.jegyzetapp.ui.NoteDetailActivity.class);
        if (noteId != null) {
            noteDetailIntent.putExtra("noteId", noteId);
        }
        
        // A PendingIntent a jó bájttokén az értesítéshez
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                noteId != null ? Math.abs(noteId.hashCode() % 100000) : 0,
                noteDetailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Értesítést jelenítünk meg, ami a megfelelő jegyzetre mutat
        NotificationHelper.showNoteReminderNotification(
                context,
                "Emlékeztető: " + noteTitle,
                "Kattints a jegyzet megnyitásához",
                pendingIntent
        );
    }
}
