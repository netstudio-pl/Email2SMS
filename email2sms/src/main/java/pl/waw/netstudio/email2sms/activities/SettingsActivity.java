package pl.waw.netstudio.email2sms.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import pl.waw.netstudio.email2sms.R;

public class SettingsActivity extends AppCompatActivity {
    public SharedPreferences ustawienia;
    public EditText etSerwer, etKonto, etHaslo, etCzestotliwosc, etZnacznikStart, etZnacznikKoniec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etSerwer = (EditText) findViewById(R.id.etSerwer);
        etKonto = (EditText) findViewById(R.id.etKonto);
        etHaslo = (EditText) findViewById(R.id.etHaslo);
        etCzestotliwosc = (EditText) findViewById(R.id.etCzestotliwosc);
        etZnacznikStart = (EditText) findViewById(R.id.etZnacznikStart);
        etZnacznikKoniec = (EditText) findViewById(R.id.etZnacznikKoniec);

        pobierz_ustawienia();
    }

    public void pobierz_ustawienia() {
        ustawienia = getSharedPreferences("Email2SMS_ustawienia", MODE_PRIVATE);
        etSerwer.setText(ustawienia.getString("serwer",""));
        etKonto.setText(ustawienia.getString("konto",""));
        etHaslo.setText(ustawienia.getString("haslo",""));
        etCzestotliwosc.setText(ustawienia.getString("czestotliwosc",""));
        etZnacznikStart.setText(ustawienia.getString("znacznik_start",""));
        etZnacznikKoniec.setText(ustawienia.getString("znacznik_koniec",""));
    }

    public void btnZapiszUstawieniaClick(View view) {
        ustawienia = getSharedPreferences("Email2SMS_ustawienia", MODE_PRIVATE);
        SharedPreferences.Editor config_editor = ustawienia.edit();
        config_editor.putString("serwer", etSerwer.getText().toString());
        config_editor.putString("konto", etKonto.getText().toString());
        config_editor.putString("haslo", etHaslo.getText().toString());
        config_editor.putString("czestotliwosc", etCzestotliwosc.getText().toString());
        config_editor.putString("znacznik_start", etZnacznikStart.getText().toString());
        config_editor.putString("znacznik_koniec", etZnacznikKoniec.getText().toString());
        config_editor.commit();
        Toast.makeText(this, "Ustawienia zostały zapisane", Toast.LENGTH_SHORT).show();
    }
}
