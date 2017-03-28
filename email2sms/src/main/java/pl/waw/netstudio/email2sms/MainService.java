package pl.waw.netstudio.email2sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

public class MainService extends Service {
    private static final String TAG = MainService.class.getPackage().getName();
    public Timer timer;
    public TimerTask timerTask;
    public Handler handler = new Handler();
    public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public String currentDateTime;
    public SharedPreferences lista;
    public String serwer, konto, haslo, czestotliwosc, znacznikStart, znacznikKoniec, adresNadawcy, numerOdbiorcy, trescSMS;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Usługa uruchomiona");
        Intent intent = new Intent(MainService.class.getName());
        intent.putExtra("isRunning", true);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Usługa zatrzymana");
        Intent intent = new Intent(MainService.class.getName());
        intent.putExtra("isRunning", false);
        LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(intent);
        timer.cancel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serwer = intent.getStringExtra("serwer");
        konto = intent.getStringExtra("konto");
        haslo = intent.getStringExtra("haslo");
        czestotliwosc = intent.getStringExtra("czestotliwosc");
        znacznikStart = intent.getStringExtra("znacznikStart");
        znacznikKoniec = intent.getStringExtra("znacznikKoniec");
        startTimer();
        return START_STICKY;
    }

    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        currentDateTime = sdf.format(new Date());
                        Intent intent = new Intent(MainService.class.getName());
                        intent.putExtra("tvInfo2", "Czas ostatniego wywołania usługi " + currentDateTime);
                        LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(intent);
                        sprawdz_poczte();
                    }
                });
            }
        };
        timer.schedule(timerTask, 0, Integer.parseInt(czestotliwosc) * 60000);
    }

    private void sprawdz_poczte() {
        //pobieram listę autoryzowanych numerów telefonów
        lista = getSharedPreferences("Email2SMS_lista", MODE_PRIVATE);
        final Map<String, ?> allEntries = lista.getAll();

        //sprawdzam czy lista nie jest pusta
        if (allEntries.size() == 0) {
            Toast.makeText(MainService.this, "Pusta lista numerów autoryzowanych", Toast.LENGTH_SHORT).show();
        } else {
            currentDateTime = sdf.format(new Date());
            Log.w("#log#", " ");
            Log.w("#log#", "*********** POCZATEK LOGOWANIA " + currentDateTime + " ***********");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        //przeglądam wszystkie pozycje z listy
                        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                            //pobieram adres nadawcy i numer telefonu odbiorcy z pozycji
                            adresNadawcy = entry.getValue().toString().substring(0, entry.getValue().toString().indexOf(";"));
                            numerOdbiorcy = entry.getValue().toString().substring(entry.getValue().toString().indexOf(";") + 1);
                            Log.i(TAG, "Adres nadawcy: " + adresNadawcy + ", numer odbiorcy: " + numerOdbiorcy);

                            //ustawiam konfigurację do połaczania z serwerem pocztowym
                            Properties properties = new Properties();
                            properties.setProperty("mail.store.protocol", "imaps");

                            //uruchamiam sesję serwera pocztowego
                            Session emailSession = Session.getDefaultInstance(properties);
                            Store store = emailSession.getStore("imaps");
                            store.connect(serwer, konto, haslo);

                            //otwieram skrzynkę odbiorczą na serwerze pocztowym
                            Folder inbox = store.getFolder("INBOX");
                            inbox.open(Folder.READ_WRITE);
                            inbox.getMessages();

                            //wyszukuję tylko nieprzeczytane maile
                            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                            Message[] message = inbox.search(ft);

                            //przeglądam maile w skrzynce odbiorczej
                            for (int a = 0; a < message.length; a++) {
                                //sprawdzam czy w skrzynce jest list od konkretnego nadawcy
                                if (message[a].getFrom()[0].toString().contains(adresNadawcy)) {
                                    try {
                                        //pobieram zawartość maila
                                        String trescMaila = getStringFromInputStream(message[a].getInputStream()).replaceAll("(\\r|\\n|\\t)", "").trim();
                                        int start = trescMaila.indexOf(znacznikStart.replaceAll("\\s+", "").trim()) + 2;
                                        int koniec = trescMaila.indexOf(znacznikKoniec.replaceAll("\\s+", "").trim());
                                        trescSMS = trescMaila.substring(start, koniec);

                                        //wysyłam SMS
                                        Log.i(TAG, "Treść komunikatu: " + trescSMS);
                                        sendSMS(numerOdbiorcy, trescSMS);

                                        //zmieniam flagę maila na przeczytana
                                        message[a].setFlag(Flags.Flag.SEEN, true);
                                    } catch (Exception ex) {
                                        Log.i(TAG, "Błąd sms: " + ex.getMessage());
                                        Intent intent = new Intent(MainService.class.getName());
                                        intent.putExtra("tvInfo3", "Błąd wysyłania sms: " + ex.getMessage());
                                        LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(intent);
                                    }
                                }
                            }
                            inbox.close(true);
                            store.close();
                        }
                        Log.w("#log#", "*********** KONIEC LOGOWANIA ***********");
                    } catch (Exception ex) {
                        Log.i(TAG, "Błąd połaczenia z serwerm pocztowym: " + ex.getMessage());
                        Intent intent = new Intent(MainService.class.getName());
                        intent.putExtra("tvInfo3", "Błąd połaczenia z serwerm pocztowym: " + ex.getMessage());
                        LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(intent);
                    }
                }
            }).start();
        }
    }

    private String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(Html.fromHtml(line));
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        int messageCount = parts.size();
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        for (int j = 0; j < messageCount; j++) {
            sentIntents.add(sentPI);
            deliveryIntents.add(deliveredPI);
        }

        // potwierdzenie wysłania sms
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "SMS sent");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.i(TAG, "Generic failure");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.i(TAG, "o service");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.i(TAG, "Null PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.i(TAG, "Radio off");
                        break;
                }
            }
        }, new IntentFilter(SENT));

        // potwierdzenie doręczenia sms
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {

                    case Activity.RESULT_OK:
                        Log.i(TAG, "SMS delivered");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "SMS not delivered");
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));
        sms.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
    }
}

