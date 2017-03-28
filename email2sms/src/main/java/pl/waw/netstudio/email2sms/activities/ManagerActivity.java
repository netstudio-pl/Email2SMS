package pl.waw.netstudio.email2sms.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pl.waw.netstudio.email2sms.R;

public class ManagerActivity extends AppCompatActivity {
    public ListView lvLista;
    public ArrayAdapter<String> adapter;
    public List<String> listaPozycji;
    public String[] pozycja = new String[]{};
    public SharedPreferences lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lvLista = (ListView) findViewById(R.id.lvLista);
        listaPozycji = new ArrayList<String>(Arrays.asList(pozycja));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaPozycji);
        lvLista.setAdapter(adapter);

        lvLista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder okienko = new AlertDialog.Builder(ManagerActivity.this);
                okienko.setTitle("Pozycja nr " + lvLista.getItemIdAtPosition(position + 1));
                okienko.setMessage((String)lvLista.getItemAtPosition(position));
                okienko.setPositiveButton("OK", null);
                okienko.show();
            }
        });

        lvLista.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                listaPozycji.remove(position);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        pobierz_liste();
    }

    private void pobierz_liste() {
        lista = getSharedPreferences("Email2SMS_lista", MODE_PRIVATE);
        Map<String, ?> allEntries = lista.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            listaPozycji.add(entry.getValue().toString());
        }
    }

    public void btnDodajPozycjeClick(View view) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.dialog_box, null);
        AlertDialog.Builder dialogUserInput = new AlertDialog.Builder(this);
        dialogUserInput.setView(mView);

        final EditText userInput = (EditText) mView.findViewById(R.id.dialogInput);
        dialogUserInput
                .setCancelable(false)
                .setPositiveButton("Dodaj", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        if (userInput.getText().toString().length()>0){
                            listaPozycji.add(userInput.getText().toString());
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .setNegativeButton("Anuluj",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });
        AlertDialog alertDialogAndroid = dialogUserInput.create();
        alertDialogAndroid.show();
    }

    public void btnZapiszListeClick(View view) {
        lista = getSharedPreferences("Email2SMS_lista", MODE_PRIVATE);
        SharedPreferences.Editor list_editor = lista.edit();
        list_editor.clear();

        for (int a = 0; a < listaPozycji.size(); a++)
        {
            list_editor.putString("pozycja" + a, (String)lvLista.getItemAtPosition(a));
        }

        list_editor.commit();
        if (listaPozycji.size()>0) Toast.makeText(this, "Lista została zapisana", Toast.LENGTH_SHORT).show();
    }
}
