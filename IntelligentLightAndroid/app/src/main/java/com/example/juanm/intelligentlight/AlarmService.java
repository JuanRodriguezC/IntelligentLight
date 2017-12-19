package com.example.juanm.intelligentlight;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;


/**
 * Created by juanm on 18/10/2017.
 */

public class AlarmService extends Service {
    public static final String
            ACTION_NEW_ALARM = AlarmService.class.getName() + "NewAlarm",
            ACTION_DELETE_ALARM = AlarmService.class.getName() + "DeleteAlarm",
            ACTION_REFRESH_BUTTON = AlarmService.class.getName() + "RefreshButton";
    private BroadcastReceiver receiver;
    private int mStartId;
    public static boolean running;
    private boolean existe;

    public static final String ACTION_REQUEST_PERMISSIONS = AlarmService.class.getName() + "RequestPermissions";


    // IP de la Url
    String IP = "http://rcmm.esy.es";
    // Rutas de los Web Services
    String GET_BY_ID = IP + "/obtener_alarma_por_id.php";
    String UPDATE = IP + "/actualizar_alarma.php";
    String INSERT = IP + "/insertar_alarma.php";
    String BORRAR = IP + "/borrar_alarma.php";


    ObtenerWebService hiloconexion;

    @Override
    public void onCreate() {
        existe=false;
        running=true;
        Intent intent = new Intent(ACTION_REFRESH_BUTTON);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // REGISTER RECEIVER THAT HANDLES ALARMS LOGIC
    /*    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getBaseContext(), 0, intent,
                0);

        Log.d("AlarmService", "Setup the alarm");

        // Getting current time and add the seconds in it
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());


        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 10*1000, pendingIntent);
*/
        IntentFilter filter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        //filter.addAction(AlarmClock.ACTION_DISMISS_ALARM);
        receiver=new AlarmReceiver();
        registerReceiver(receiver, filter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AlarmService", "onStartCommand: ");
         mStartId=startId;

        boolean permissionGranted = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

        if(permissionGranted) {


            try {
                AlarmManager am = (AlarmManager) getSystemService(getBaseContext().ALARM_SERVICE);
                am.getNextAlarmClock();
                TelephonyManager mngr = (TelephonyManager) getSystemService(getBaseContext().TELEPHONY_SERVICE);
                Log.d("IMEI", "IMEI: " + mngr.getDeviceId());
                if (am.getNextAlarmClock() != null) {
                    printAlarm(am.getNextAlarmClock().getTriggerTime()+"");
                    Log.d("Nextalarm", new Timestamp(am.getNextAlarmClock().getTriggerTime()) + "");
//FORMATO ARDUINO 13:19 12/12/2017
                    Calendar cal = Calendar.getInstance();
                    long tenMinutes=600000;
                    cal.setTimeInMillis(am.getNextAlarmClock().getTriggerTime() - tenMinutes);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    int minutes = cal.get(Calendar.MINUTE);
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    int month = cal.get(Calendar.MONTH);
                    int year = cal.get(Calendar.YEAR);
                    String hourString = ""+hour;
                    String minutesString = ""+minutes;
                    if(hour < 10){
                        hourString = "0"+hour;
                    }
                    if(minutes < 10){
                        minutesString = "0"+minutes;
                    }
                    month=month+1;
                    String date= hourString+":"+minutesString+" "+day+"/"+month+"/"+year;
                    //Consultamos si existe alarma para ese usuario
                    hiloconexion = new ObtenerWebService();
                 //   String cadenallamada = GET_BY_ID + "?id=" + mngr.getDeviceId().toString();
                   // hiloconexion.execute(cadenallamada,"1");   // Parámetros que recibe doInBackground
                   if(!existe){


                        hiloconexion = new ObtenerWebService();
                        hiloconexion.execute(INSERT,"2",mngr.getDeviceId().toString(),date);
                       Log.d("Existe", "onStartCommand: NOOO EXISTEEEEEEEEEEEE");
                       existe=true;
                    }else{//UPDATE
                       hiloconexion = new ObtenerWebService();
                       hiloconexion.execute(BORRAR, "4", mngr.getDeviceId().toString());
                        hiloconexion = new ObtenerWebService();
                       hiloconexion.execute(INSERT,"2",mngr.getDeviceId().toString(),date);
                       //  hiloconexion.execute(UPDATE,"3",mngr.getDeviceId().toString(),String.valueOf(am.getNextAlarmClock().getTriggerTime()));   // Parámetros que recibe doInBackground
                       Log.d("Existe", "onStartCommand: EXISTEEEEEEEEEEEE");

                   }
                }else{
                    printAlarm("-1");
                    hiloconexion = new ObtenerWebService();
                    hiloconexion.execute(BORRAR, "4", mngr.getDeviceId().toString());
                    existe=false;
                }
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            }
        }else{
            requestPermissions();
        }
        return Service.START_STICKY;
    }

    private void printAlarm(String triggerTime) {
        if(triggerTime.equals("-1")){
            Intent intent = new Intent(ACTION_DELETE_ALARM);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            Intent intent = new Intent(ACTION_NEW_ALARM);
            intent.putExtra("alarm", triggerTime);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }


    private void requestPermissions() {
        Intent intent = new Intent(ACTION_REQUEST_PERMISSIONS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
       /* Intent intent2 = new Intent(getBaseContext(),AlarmService.class);
        getBaseContext().startService(intent2);
    */}

    @Override
    public void onDestroy() {
        super.onDestroy();
        running=false;
        Intent intent = new Intent(ACTION_REFRESH_BUTTON);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        unregisterReceiver(receiver);
        stopSelf(mStartId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}



class ObtenerWebService extends AsyncTask<String,Void,String> {

    @Override
    protected String doInBackground(String... params) {

        String cadena = params[0];
        URL url = null; // Url de donde queremos obtener información
        String devuelve = "";


        if (params[1] == "1") {    // consulta por id

            try {
                url = new URL(cadena);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Abrir la conexión
                connection.setRequestProperty("User-Agent", "Mozilla/5.0" +
                        " (Linux; Android 1.5; es-ES) Ejemplo HTTP");
                //connection.setHeader("content-type", "application/json");

                int respuesta = connection.getResponseCode();
                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {


                    InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                    // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                    // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                    // StringBuilder.

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);        // Paso toda la entrada al StringBuilder
                    }

                    //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                    JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                    //Accedemos al vector de resultados

                    String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON

                    if (resultJSON == "1") {      // hay una alarma que mostrar
                        Log.d("devuelve", "doInBackground: "+ respuestaJSON.getString("id"));
                        devuelve = devuelve + respuestaJSON.getString("id") + " " +
                                respuestaJSON.getString("fecha");
                    } else if (resultJSON == "2") {
                        devuelve = "-1";
                    }
                    Log.d("DEVUELVE", "doInBackground: " + devuelve);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return devuelve;


        } else if (params[1] == "2") {    // insert

            try {
                HttpURLConnection urlConn;

                DataOutputStream printout;
                DataInputStream input;
                url = new URL(cadena);
                urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");
                urlConn.connect();
                //Creo el Objeto JSON
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("id", params[2]);
                jsonParam.put("fecha", params[3]);
                // Envio los parámetros post.
                OutputStream os = urlConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();

                int respuesta = urlConn.getResponseCode();


                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {

                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                        //response+=line;
                    }

                    //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                    JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                    //Accedemos al vector de resultados
                    String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON

                    if (resultJSON == "1") {      // hay un alumno que mostrar
                        devuelve = "AlarmaOK";

                    } else if (resultJSON == "2") {
                        devuelve = "AlarmaNO";
                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return devuelve;


        } else if (params[1] == "3") {    // update

            try {
                HttpURLConnection urlConn;

                DataOutputStream printout;
                DataInputStream input;
                url = new URL(cadena);
                urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");
                urlConn.connect();
                //Creo el Objeto JSON
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("id", params[2]);
                jsonParam.put("fecha", params[3]);
                // Envio los parámetros post.
                OutputStream os = urlConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();

                int respuesta = urlConn.getResponseCode();


                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {

                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                        //response+=line;
                    }

                    //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                    JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                    //Accedemos al vector de resultados

                    String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON

                    if (resultJSON == "1") {      // hay un alumno que mostrar
                        devuelve = "AlarmaOK";

                    } else if (resultJSON == "2") {
                        devuelve = "AlarmaNO";
                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return devuelve;
        }
        else if (params[1] == "4") {    // ELIMINAR ALARMA
            try {
                HttpURLConnection urlConn;
                url = new URL(cadena);
                urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");
                urlConn.connect();
                //Creo el Objeto JSON
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("id", params[2]);
                // Envio los parámetros post.
                OutputStream os = urlConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();

                int respuesta = urlConn.getResponseCode();


                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {

                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject respuestaJSON = new JSONObject(result.toString());

                    String resultJSON = respuestaJSON.getString("estado");
                    if (resultJSON == "1") {
                        devuelve = "AlarmDeleteOK";

                    } else if (resultJSON == "2") {
                        devuelve = "AlarmDeleteNO";
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return devuelve;
        }
        return null;
    }
    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

    @Override
    protected void onPostExecute(String s) {
        Log.d("POSTEXECUTED", "onPostExecute: " + s);
        super.onPostExecute(s);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}