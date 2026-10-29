package com.example.localcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContactosActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;

    private EditText etNombreContacto;
    private EditText etTelefonoContacto;
    private EditText etCorreoContacto;

    private TextView tvContactosRegistrados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contactos);

        View mainView = findViewById(R.id.main);

        final int paddingLeft = mainView.getPaddingLeft();
        final int paddingTop = mainView.getPaddingTop();
        final int paddingRight = mainView.getPaddingRight();
        final int paddingBottom = mainView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                mainView, (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            paddingLeft + systemBars.left,
                            paddingTop + systemBars.top,
                            paddingRight + systemBars.right,
                            paddingBottom + systemBars.bottom
                    );

                    return insets;
                });

        etNombreContacto = findViewById(R.id.etNombreContacto);
        etTelefonoContacto = findViewById(R.id.etTelefonoContacto);
        etCorreoContacto = findViewById(R.id.etCorreoContacto);

        tvContactosRegistrados =
                findViewById(R.id.tvContactosRegistrados);

        Button btnGuardarContacto =
                findViewById(R.id.btnGuardarContacto);

        Button btnVolverContactos =
                findViewById(R.id.btnVolverContactos);

        dbHelper = new LocalCashDbHelper(this);

        btnGuardarContacto.setOnClickListener(
                v -> guardarContacto());

        btnVolverContactos.setOnClickListener(
                v -> finish());

        mostrarContactos();
    }

    private void guardarContacto() {

        String nombre = etNombreContacto.getText()
                .toString().trim();

        String telefono = etTelefonoContacto.getText()
                .toString().trim();

        String correo = etCorreoContacto.getText()
                .toString().trim();

        if (nombre.isEmpty()) {
            etNombreContacto.setError(
                    "Ingresa el nombre del contacto");
            etNombreContacto.requestFocus();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Evitar duplicar el mismo nombre con el mismo teléfono.
        try (Cursor cursor = db.query(
                "contactos",
                new String[]{"id"},
                "nombre = ? COLLATE NOCASE " +
                        "AND COALESCE(telefono, '') = ?",
                new String[]{nombre, telefono},
                null,
                null,
                null,
                "1")) {

            if (cursor.moveToFirst()) {
                etNombreContacto.setError(
                        "Este contacto ya está registrado");
                etNombreContacto.requestFocus();

                Toast.makeText(
                        this,
                        "El contacto ya existe con ese teléfono",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
        }

        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("telefono",
                telefono.isEmpty() ? null : telefono);
        values.put("correo",
                correo.isEmpty() ? null : correo);
        values.put("fecha_registro", fecha);

        long id = db.insert("contactos", null, values);

        if (id == -1) {
            Toast.makeText(
                    this,
                    "No se pudo guardar el contacto",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(
                this,
                "Contacto guardado correctamente",
                Toast.LENGTH_SHORT
        ).show();

        etNombreContacto.setText("");
        etTelefonoContacto.setText("");
        etCorreoContacto.setText("");

        mostrarContactos();
    }

    private void mostrarContactos() {

        StringBuilder texto = new StringBuilder();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "contactos",
                new String[]{
                        "nombre",
                        "telefono",
                        "correo"
                },
                null,
                null,
                null,
                null,
                "nombre COLLATE NOCASE ASC")) {

            if (cursor.getCount() == 0) {
                tvContactosRegistrados.setText(
                        "Todavía no hay contactos.");
                return;
            }

            int numero = 1;

            while (cursor.moveToNext()) {

                String nombre = cursor.getString(0);
                String telefono = cursor.getString(1);
                String correo = cursor.getString(2);

                texto.append(numero)
                        .append(". ")
                        .append(nombre)
                        .append("\n");

                if (telefono != null && !telefono.isEmpty()) {
                    texto.append("Teléfono: ")
                            .append(telefono)
                            .append("\n");
                }

                if (correo != null && !correo.isEmpty()) {
                    texto.append("Correo: ")
                            .append(correo)
                            .append("\n");
                }

                texto.append("\n");
                numero++;
            }
        }

        tvContactosRegistrados.setText(texto.toString());
    }


    protected void onResume() {
        super.onResume();

        if (dbHelper != null && tvContactosRegistrados != null) {
            mostrarContactos();
        }
    }


    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
