//Autores
//Juan Manuel Gutierrez Ruiz
//Juan Rodriguez Cabanillas

#include <ArduinoJson.h>
#include <TimeLib.h>
#include <NtpClientLib.h>
#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <SPI.h>
#include <ThingerWifi.h>

#ifndef WIFI_CONFIG_H
#define YOUR_WIFI_SSID "iPhone de Juan"
#define YOUR_WIFI_PASSWD "3jfsnpidrqu5a"
#define EMAIL_ENDPOINT "Correo"
#ifdef ESP8266
#define Presencia_GPIO D2
#else
#define Presencia_GPIO 4
#endif
#endif // !WIFI_CONFIG_H

#define ONBOARDLED 2 // Built in LED on ESP-12/ESP-07

WiFiServer server(80); //objeto servidor que inicializaremos en el puerto 80

String json, solicitud;
String IMEI = "356265069389251";

boolean mandarMsn = false;
boolean enciende = false;
boolean mensaje = false;
const int pinLed = 5;  //5+
int i;
String horaInicial;
bool val = 0;
bool old_val = 0;

//gmail
ThingerWifi thing("JRodri", "dsh", "yl6Eecu6@9HW");

// Start NTP only after IP network is connected
void onSTAGotIP(WiFiEventStationModeGotIP ipInfo) {
  Serial.printf("Got IP: %s\r\n", ipInfo.ip.toString().c_str());
  NTP.begin("pool.ntp.org", 1, true);
  NTP.setInterval(63);
  digitalWrite(ONBOARDLED, LOW); // Turn on LED
}

// Manage network disconnection
void onSTADisconnected(WiFiEventStationModeDisconnected event_info) {
  Serial.printf("Disconnected from SSID: %s\n", event_info.ssid.c_str());
  Serial.printf("Reason: %d\n", event_info.reason);
  digitalWrite(ONBOARDLED, HIGH); // Turn off LED
  NTP.stop(); // NTP sync can be disabled to avoid sync errors
}

void processSyncEvent(NTPSyncEvent_t ntpEvent) {
  if (ntpEvent) {
    if (ntpEvent == noResponse)
      Serial.println("NTP server not reachable");
    else if (ntpEvent == invalidAddress)
      Serial.println("Invalid NTP server address");
  }
  else {
    Serial.print("Got NTP time: ");
    Serial.println(NTP.getTimeDateString(NTP.getLastNTPSync()));
  }
}

boolean syncEventTriggered = false; // True if a time even has been triggered
NTPSyncEvent_t ntpEvent; // Last triggered event


void setup()
{

  static WiFiEventHandler e1, e2;

  Serial.begin(115200);
  Serial.println();
  WiFi.mode(WIFI_STA);
  WiFi.begin(YOUR_WIFI_SSID, YOUR_WIFI_PASSWD);

  NTP.onNTPSyncEvent([](NTPSyncEvent_t event) {
    ntpEvent = event;
    syncEventTriggered = true;
  });

  // Deprecated
  WiFi.onEvent([](WiFiEvent_t e) {
    Serial.printf("Event wifi -----> %d\n", e);
  });

  e1 = WiFi.onStationModeGotIP(onSTAGotIP);// As soon WiFi is connected, start NTP Client
  e2 = WiFi.onStationModeDisconnected(onSTADisconnected);

  //Iniciar server
  server.begin();
  Serial.println("Servidor iniciado");
  Serial.print("IP del dispositivo: ");
  Serial.print(WiFi.localIP());

  pinMode(pinLed, OUTPUT);
  pinMode (Presencia_GPIO, INPUT);
  pinMode (2, OUTPUT);
  old_val = digitalRead(Presencia_GPIO);
  Serial.flush();
}


void loop()
{
  static int i = 0;
  static int last = 0;

  if (syncEventTriggered) {
    processSyncEvent(ntpEvent);
    syncEventTriggered = false;
  }

  if ((millis() - last) > 5100) {
    last = millis();
    i++;
  }

  if (WiFi.status() == WL_CONNECTED) { //Check WiFi connection status
    HTTPClient http;  //Declare an object of class HTTPClient

    http.begin("http://rcmm.esy.es/obtener_alarma_por_id.php?id=" + IMEI); //Specify request destination
    int httpCode = http.GET();                                                                  //Send the request

    if (httpCode > 0) { //Check the returning code

      String payload = http.getString();   //Get the request response payload

      DynamicJsonBuffer jsonBuffer;
      JsonObject& root = jsonBuffer.parseObject(payload);
      String fechaMovil = root["alarma"]["fecha"];

      String fechaArd = NTP.getTimeDateString();
      fechaArd.remove(5, 3);

      Serial.println("Fecha Hosting: " + fechaMovil);
      Serial.println("Fecha arduino nueva: " + fechaArd);
  
      val = digitalRead(Presencia_GPIO);
      if(val==0){
        Serial.println("Val = 0");
      }else if (val == 1){
        Serial.println("Val=1");
      }
      
        if (fechaMovil == fechaArd && enciende == false) {
          mandarMsn=true;
          if (val == 1) {
            Serial.println("DETECTADO CUERPO EN MOVIMIENTO");
            Serial.println("Encender luz");
            enciende = true;
           for (int i = 0; i < 256; i++) {
              analogWrite(pinLed, i);
              delay(455);
            }
            digitalWrite(2, HIGH);
            old_val=1;
          }
        } else if (enciende == false) {
          Serial.println("No encender luz");
          enciende = false;
          if (mensaje == false && mandarMsn==true) {    //Envio msn
              thing.handle();
              thing.call_endpoint(EMAIL_ENDPOINT, "envio");
              mensaje = true;
            }
        }
        if(horaInicial != fechaMovil){
          enciende=false;
          mensaje=false;
          mandarMsn=false;
          val=0;
          old_val=0;
        }
      horaInicial=fechaMovil;
    }
    http.end();   //Close connection
  }
  delay(3000);

  delay(0);
}
