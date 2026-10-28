package com.example.localcash;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CuentasActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;
    private EditText etNombreCuenta;
    private EditText etSaldoInicial;
    private RadioGroup rgTipoCuenta;
    private TextView tvCuentasRegistradas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuentas);

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

        etNombreCuenta = findViewById(R.id.etNombreCuenta);
        etSaldoInicial = findViewById(R.id.etSaldoInicial);
        rgTipoCuenta = findViewById(R.id.rgTipoCuenta);
        tvCuentasRegistradas =
                findViewById(R.id.tvCuentasRegistradas);

        Button btnGuardarCuenta =
                findViewById(R.id.btnGuardarCuenta);

        Button btnMovimientos =
                findViewById(R.id.btnMovimientos);

        Button btnVolver = findViewById(R.id.btnVolver);

        dbHelper = new LocalCashDbHelper(this);

        rgTipoCuenta.check(R.id.rbEfectivo);

        btnGuardarCuenta.setOnClickListener(
                v -> guardarCuenta());

        btnMovimientos.setOnClickListener(v -> {
            Intent intent = new Intent(
                    CuentasActivity.this,
                    MovimientosActivity.class
            );

            startActivity(intent);
        });

        btnVolver.setOnClickListener(v -> finish());

        mostrarCuentas();
    }

    private void guardarCuenta() {

        String nombre = etNombreCuenta.getText()
                .toString().trim();

        String saldoTexto = etSaldoInicial.getText()
                .toString().trim().replace(",", ".");

        if (nombre.isEmpty()) {
            etNombreCuenta.setError(
                    "Ingresa el nombre de la cuenta");
            etNombreCuenta.requestFocus();
            return;
        }

        if (saldoTexto.isEmpty()) {
            etSaldoInicial.setError(
                    "Ingresa el saldo inicial");
            etSaldoInicial.requestFocus();
            return;
        }

        double saldoInicial;

        try {
            saldoInicial = Double.parseDouble(saldoTexto);
        } catch (NumberFormatException e) {
            etSaldoInicial.setError("Ingresa un monto válido");
            etSaldoInicial.requestFocus();
            return;
        }

        if (Double.isNaN(saldoInicial)
                || Double.isInfinite(saldoInicial)
                || saldoInicial < 0) {

            etSaldoInicial.setError(
                    "El saldo no puede ser negativo");
            etSaldoInicial.requestFocus();
            return;
        }

        int tipoId = rgTipoCuenta.getCheckedRadioButtonId();

        if (tipoId == -1) {
            Toast.makeText(
                    this,
                    "Selecciona un tipo de cuenta",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        RadioButton radioTipo = findViewById(tipoId);
        String tipo = radioTipo.getText().toString();
        // Evitar cuentas con nombres repetidos.
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try (Cursor cursor = db.query(
                "cuentas",
                new String[]{"id"},
                "nombre = ? COLLATE NOCASE",
                new String[]{nombre},
                null,
                null,
                null,
                "1")) {

            if (cursor.moveToFirst()) {
                etNombreCuenta.setError(
                        "Ya existe una cuenta con ese nombre");
                etNombreCuenta.requestFocus();

                Toast.makeText(
                        this,
                        "Esta cuenta ya está registrada",
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
        values.put("tipo", tipo);
        values.put("saldo_inicial", saldoInicial);
        values.put("fecha_creacion", fecha);

        long id = db.insert("cuentas", null, values);

        if (id == -1) {
            Toast.makeText(
                    this,
                    "No se pudo guardar la cuenta",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(
                this,
                "Cuenta guardada correctamente",
                Toast.LENGTH_SHORT
        ).show();

        etNombreCuenta.setText("");
        etSaldoInicial.setText("");
        rgTipoCuenta.check(R.id.rbEfectivo);

        mostrarCuentas();
    }

    private void mostrarCuentas() {

        StringBuilder texto = new StringBuilder();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String consulta =
                "SELECT c.nombre, c.tipo, " +
                        "c.saldo_inicial + COALESCE((" +
                        "SELECT SUM(CASE " +
                        "WHEN m.tipo = 'INGRESO' THEN m.monto " +
                        "ELSE -m.monto END) " +
                        "FROM movimientos m " +
                        "WHERE m.cuenta_id = c.id" +
                        "), 0) AS saldo_actual " +
                        "FROM cuentas c " +
                        "ORDER BY c.id DESC";

        NumberFormat formatoMoneda =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        try (Cursor cursor = db.rawQuery(consulta, null)) {

            if (cursor.getCount() == 0) {
                tvCuentasRegistradas.setText(
                        "Todavía no hay cuentas registradas.");
                return;
            }

            int numero = 1;

            while (cursor.moveToNext()) {

                String nombre = cursor.getString(0);
                String tipo = cursor.getString(1);
                double saldoActual = cursor.getDouble(2);

                texto.append(numero)
                        .append(". ")
                        .append(nombre)
                        .append(" (")
                        .append(tipo)
                        .append(")")
                        .append("\nSaldo actual: ")
                        .append(formatoMoneda.format(saldoActual))
                        .append("\n\n");

                numero++;
            }
        }

        tvCuentasRegistradas.setText(texto.toString());
    }


    protected void onResume() {
        super.onResume();

        if (dbHelper != null && tvCuentasRegistradas != null) {
            mostrarCuentas();
        }
    }


    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
