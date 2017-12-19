package com.example.juanm.intelligentlight;

import android.Manifest;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.sql.Timestamp;
import java.util.Calendar;

import static com.example.juanm.intelligentlight.AlarmService.running;

public class MainActivity extends AppCompatActivity {
    private Button stopButton, enableButton;
    private TextView textViewAlarm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 21){
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccent));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorAccent));
        }
        setContentView(R.layout.activity_main);
        textViewAlarm = (TextView) findViewById(R.id.TextViewAlarm);


        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmService.ACTION_REQUEST_PERMISSIONS);
        filter.addAction(AlarmService.ACTION_NEW_ALARM);
        filter.addAction(AlarmService.ACTION_DELETE_ALARM);
        filter.addAction(AlarmService.ACTION_REFRESH_BUTTON);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if(intent.getAction() == AlarmService.ACTION_REFRESH_BUTTON){
                            refreshButton();
                        }else if(intent.getAction() == AlarmService.ACTION_NEW_ALARM){
                            Long time = Long.parseLong(intent.getStringExtra("alarm"));
                            printAlarm(time);
                        }else if(intent.getAction() == AlarmService.ACTION_DELETE_ALARM){
                            deleteAlarm();
                        }else if (intent.getAction() == AlarmService.ACTION_REQUEST_PERMISSIONS) {
                            requestPermissions();
                        }


                    }
                }, new IntentFilter(filter)
        );


        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View src) {
                // Stop the AlarmService using the Intent
                stopService(new Intent(getBaseContext(),AlarmService.class));

            }
        });
        enableButton = (Button) findViewById(R.id.enable_button);
        enableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View src) {
                // Start the AlarmService using the Intent
                Intent intent = new Intent(getBaseContext(),AlarmService.class);
                startService(intent);

            }
        });



        /*
        try {
            AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            am.getNextAlarmClock();
            Log.d("Nextalarm",  new Timestamp(am.getNextAlarmClock().getTriggerTime())+"");
        } catch (NoSuchMethodError e) {
            e.printStackTrace();
        }
*/
    }


    private void deleteAlarm() {
        textViewAlarm.setText("Actualmente no hay ninguna alarma fijada.");
    }

    private void printAlarm(Long time) {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String dayString="";
        String hourString = ""+hour;
        String minutesString = ""+minutes;
        switch (dayOfWeek){
            case 1: // DOMINGO
                dayString="Domingo";
                break;
            case 2: // LUNES
                dayString="Lunes";
                break;
            case 3: // MARTES
                dayString="Martes";
                break;
            case 4: // MIÉRCOLES
                dayString="Miércoles";
                break;
            case 5: // JUEVES
                dayString="Jueves";
                break;
            case 6: // VIERNES
                dayString="Viernes";
                break;
            case 7: // SÁBADO
                dayString="Sábado";
                break;
        }
        if(hour < 10){
            hourString = "0"+hour;
        }
        if(minutes < 10){
            minutesString = "0"+minutes;
        }
        textViewAlarm.setText("Se ha fijado una alarma el "+dayString+ " " + day +" a las " + hourString+":"+minutesString+".");
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 200);

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshButton();
        if(running) {
            try {
                AlarmManager am = (AlarmManager) getSystemService(getBaseContext().ALARM_SERVICE);
                am.getNextAlarmClock();
                if (am.getNextAlarmClock() != null) {
                    printAlarm(am.getNextAlarmClock().getTriggerTime());
                } else {
                    deleteAlarm();

                }
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshButton() {
        if(running){
            findViewById(R.id.enable_button).setVisibility(View.GONE);
            findViewById(R.id.stop_button).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.enable_button).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_button).setVisibility(View.GONE);
            textViewAlarm.setText("El servicio está parado. Pulse \"Activar Servicio\" para iniciar el servicio de alarmas.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
