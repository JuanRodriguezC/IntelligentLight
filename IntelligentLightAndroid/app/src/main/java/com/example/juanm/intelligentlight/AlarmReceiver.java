package com.example.juanm.intelligentlight;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.util.Log;

/**
 * Created by juanm on 18/10/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "onReceive: ");
        if(intent.getAction()== AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED){
            // Start the AlarmService
            Intent i = new Intent(context, AlarmService.class);
            context.startService(i);
        }

        /*
        if (intent.getAction().equals(AlarmClock.ACTION_SET_ALARM)) {
            Log.i("TAGALARM",  "SE HA ESTABLECIDO UNA ALARMA");
        //Enviar datos de la alarma al servidor remoto
        } else if (intent.getAction().equals(AlarmClock.ACTION_DISMISS_ALARM)) {
            Log.i("TAGALARM",  "SE HA BORRADO UNA ALARMA");
        //Borrar datos de la alarma del servidor remoto
        }*/
    }
}
