package pl.waw.netstudio.email2sms.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import pl.waw.netstudio.email2sms.MainService;
import pl.waw.netstudio.email2sms.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getPackage().getName();
    public TextView tvInfo1, tvInfo2, tvInfo3;
    public Button btnWlaczUsluge;
    public boolean isRunning = false;
    public boolean doubleBackToExit = false;
    public String serwer, konto, haslo, czestotliwosc, znacznikStart, znacznikKoniec;
    public SharedPreferences ustawienia, stan_aplikacji;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        //Sprawdzam wersję SDK systemu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 1);
        } else {
            //Android w wersji niżej niż 6.0 lub zezwolenia są nadane
        }

        tvInfo1 = (TextView) findViewById(R.id.tvInfo1);
        tvInfo2 = (TextView) findViewById(R.id.tvInfo2);
        tvInfo3 = (TextView) findViewById(R.id.tvInfo3);
        btnWlaczUsluge = (Button) findViewById(R.id.btnWlaczUsluge);

        pobierz_ustawienia();
        pobierz_stan_aplikacji();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(MainService.class.getName()));
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "Zapis stanu aplikacji");
        stan_aplikacji = getSharedPreferences("Email2SMS_stan_aplikacji", MODE_PRIVATE);
        SharedPreferences.Editor stan_editor = stan_aplikacji.edit();
        stan_editor.putBoolean("isRunning", isRunning);
        stan_editor.putString("tvInfo1", tvInfo1.getText().toString());
        stan_editor.putInt("tvInfo1_color", tvInfo1.getCurrentTextColor());
        stan_editor.putString("tvInfo2", tvInfo2.getText().toString());
        stan_editor.putString("btnWlaczUsluge", btnWlaczUsluge.getText().toString());
        stan_editor.commit();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tvInfo2.setText(intent.getStringExtra("tvInfo2"));
            tvInfo3.setText(intent.getStringExtra("tvInfo3"));
        }
    };

    private void pobierz_ustawienia() {
        Log.i(TAG, "Odczyt ustawień aplikacji");
        ustawienia = getSharedPreferences("Email2SMS_ustawienia", MODE_PRIVATE);
        serwer = ustawienia.getString("serwer", "");
        konto = ustawienia.getString("konto", "");
        haslo = ustawienia.getString("haslo", "");
        czestotliwosc = ustawienia.getString("czestotliwosc", "");
        znacznikStart = ustawienia.getString("znacznik_start", "");
        znacznikKoniec = ustawienia.getString("znacznik_koniec", "");
    }

    private void pobierz_stan_aplikacji() {
        Log.i(TAG, "Odczyt stanu aplikacji");
        stan_aplikacji = getSharedPreferences("Email2SMS_stan_aplikacji", MODE_PRIVATE);
        isRunning = stan_aplikacji.getBoolean("isRunning", false);
        tvInfo1.setText(stan_aplikacji.getString("tvInfo1", ""));
        tvInfo1.setTextColor(stan_aplikacji.getInt("tvInfo1_color", Color.WHITE));
        tvInfo2.setText(stan_aplikacji.getString("tvInfo2", ""));
        btnWlaczUsluge.setText(stan_aplikacji.getString("btnWlaczUsluge", "WŁĄCZ USŁUGĘ"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_manager) {
            Intent intent = new Intent(MainActivity.this, ManagerActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExit) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExit = true;
        Toast.makeText(this, "Naciśnij ponownie, aby zamknąć program", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExit = false;
            }
        }, 2000);
    }

    public void btnWlaczUslugeClick(View view) {
        pobierz_ustawienia();
        if (serwer.length() == 0 || konto.length() == 0 || haslo.length() == 0 || czestotliwosc.length() == 0 || znacznikStart.length() == 0 || znacznikKoniec.length() == 0) {
            Toast.makeText(this, "Nieprawidłowe ustawienia aplikacji", Toast.LENGTH_SHORT).show();
        } else {
            if (isRunning) {
                isRunning = false;
                btnWlaczUsluge.setText("Włącz usługę");
                tvInfo1.setTextColor(Color.RED);
                tvInfo1.setText("USŁUGA ZATRZYMANA");
                stopService(new Intent(MainActivity.this, MainService.class));
            } else {
                isRunning = true;
                btnWlaczUsluge.setText("Wyłącz usługę");
                tvInfo1.setTextColor(Color.GREEN);
                tvInfo1.setText("USŁUGA URUCHOMIONA");
                Intent serviceIntent = new Intent(MainActivity.this, MainService.class);
                serviceIntent.putExtra("serwer", serwer);
                serviceIntent.putExtra("konto", konto);
                serviceIntent.putExtra("haslo", haslo);
                serviceIntent.putExtra("czestotliwosc", czestotliwosc);
                serviceIntent.putExtra("znacznikStart", znacznikStart);
                serviceIntent.putExtra("znacznikKoniec", znacznikKoniec);
                this.startService(serviceIntent);
            }
        }
    }
}
