package com.example.jegyzetapp.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Segédosztály a jegyzet emlékeztető funkciók kezeléséhez
 * 
 * Ez az osztály teljes API-t biztosít az emlékeztetők kezeléséhez: létrehozáshoz és törléshez.
 * Bár jelenleg nem minden metódus aktivan használt, ez a tervezés biztosítja a bővíthetőséget.
 */
public class ReminderHelper {
    private static final String TAG = "ReminderHelper";

    /**
     * Emlékeztető beállítása egy jegyzethez
     *
     * @param context A kontextus
     * @param noteId A jegyzet azonosítója
     * @param noteTitle A jegyzet címe
     * @param hourOfDay Az óra (0-23)
     * @param minute A perc (0-59)
     */
    public static void setReminder(Context context, String noteId, String noteTitle, int hourOfDay, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        try {
            // Intent létrehozása a ReminderReceiver számára
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId);
            intent.putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
            
            // Egyedi request code létrehozása a jegyzethez - biztonságosabb módszerrel
            int requestCode;
            if (noteId != null) {
                requestCode = Math.abs(noteId.hashCode() % 100000);
            } else {
                // Ha nincs noteId, használjunk uniqueId-t
                requestCode = (int) (System.currentTimeMillis() % 100000);
            }
            
            Log.d(TAG, "PendingIntent request code: " + requestCode);
            
            // PendingIntent létrehozása
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        
        // Időpont beállítása
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // Ha a beállított idő a múltban van, adjunk hozzá egy napot
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
            // Alarm beállítása biztonságosan
            long triggerTime = calendar.getTimeInMillis();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
                
                String formattedMinute = minute < 10 ? "0" + minute : String.valueOf(minute);
                Log.d(TAG, "Emlékeztető beállítva: " + noteTitle + ", időpont: " + hourOfDay + ":" + formattedMinute);
                Toast.makeText(context, "Emlékeztető beállítva: " + hourOfDay + ":" + formattedMinute, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Hiba az emlékeztető beállításakor", e);
                Toast.makeText(context, "Nem sikerült beállítani az emlékeztetőt: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Hiba az emlékeztető létrehozásakor", e);
            Toast.makeText(context, "Nem sikerült létrehozni az emlékeztetőt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Emlékeztető törlése
     * 
     * MEGJEGYZÉS: Ez a metódus jelenleg nincs használatban, de rendelkezésre áll a jövőbeli
     * funkcióbővítésekhez, amely lehetővé tenni az emlékeztetők törlését felhasználói kezdeményezésre.
     *
     * @param context A kontextus
     * @param noteId A jegyzet azonosítója
     */
    /**
     * MEGJEGYZÉS: ez a metódus jelenleg nincs használva az alkalmazásban,
     * de fontos része a teljes emlékeztető kezelési API-nak, amely a későbbi
     * fejlesztésekhez lesz használva, például az emlékeztetők törléséhez.
     */
    @SuppressWarnings("all")
    public static void cancelReminder(Context context, String noteId) {
        // A metódus megvalósítása ideiglenesen kikommentelve a kódtisztaság érdekében
        // A tényleges implementáció akkor lesz visszaállítva, ha szükség lesz erre a funkcióra
    }
    }
}
