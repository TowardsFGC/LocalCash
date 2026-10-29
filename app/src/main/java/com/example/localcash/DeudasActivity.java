
package com.example.localcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class DeudasActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 5;

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgDeudasPendientes;
    private TextView tvResumenDeudas;
    private TextView tvSinDeudas;
    private TextView tvPaginaDeudas;

    private Button btnAnteriorDeudas;
    private Button btnSiguienteDeudas;
    private Button btnMarcarDeudaPagada;

    private int paginaActual = 0;
    private int totalPaginas = 1;
    private int totalPendientes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deudas);

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

        rgDeudasPendientes =
                findViewById(R.id.rgDeudasPendientes);

        tvResumenDeudas = findViewById(R.id.tvResumenDeudas);
        tvSinDeudas = findViewById(R.id.tvSinDeudas);
        tvPaginaDeudas = findViewById(R.id.tvPaginaDeudas);

        btnAnteriorDeudas =
                findViewById(R.id.btnAnteriorDeudas);

        btnSiguienteDeudas =
                findViewById(R.id.btnSiguienteDeudas);

        btnMarcarDeudaPagada =
                findViewById(R.id.btnMarcarDeudaPagada);

        Button btnVolverDeudas =
                findViewById(R.id.btnVolverDeudas);

        dbHelper = new LocalCashDbHelper(this);

        btnAnteriorDeudas.setOnClickListener(v -> {
            if (paginaActual > 0) {
                paginaActual--;
                mostrarDeudasPendientes();
            }
        });

        btnSiguienteDeudas.setOnClickListener(v -> {
            if (paginaActual + 1 < totalPaginas) {
                paginaActual++;
                mostrarDeudasPendientes();
            }
        });

        btnMarcarDeudaPagada.setOnClickListener(
                v -> registrarPago());

        btnVolverDeudas.setOnClickListener(v -> finish());

        mostrarDeudasPendientes();
    }

    private NumberFormat obtenerFormatoMoneda() {
        NumberFormat formato =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);

        return formato;
    }

    private void mostrarDeudasPendientes() {

        rgDeudasPendientes.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double montoTotalPendiente = 0;

        try (Cursor resumen = db.rawQuery(
                "SELECT COUNT(*), " +
                        "COALESCE(SUM(monto_deuda), 0) " +
                        "FROM deudas_compartidas " +
                        "WHERE estado = 'PENDIENTE'",
                null)) {

            if (resumen.moveToFirst()) {
                totalPendientes = resumen.getInt(0);
                montoTotalPendiente = resumen.getDouble(1);
            }
        }

        totalPaginas = Math.max(
                1,
                (totalPendientes + PAGE_SIZE - 1) / PAGE_SIZE
        );

        if (paginaActual >= totalPaginas) {
            paginaActual = totalPaginas - 1;
        }

        NumberFormat formato = obtenerFormatoMoneda();

        tvResumenDeudas.setText(
                "Deudas pendientes: " + totalPendientes +
                        "\nTotal por cobrar: " +
                        formato.format(montoTotalPendiente)
        );

        tvSinDeudas.setVisibility(
                totalPendientes == 0 ? View.VISIBLE : View.GONE);

        btnAnteriorDeudas.setEnabled(paginaActual > 0);

        btnSiguienteDeudas.setEnabled(
                paginaActual + 1 < totalPaginas);

        tvPaginaDeudas.setText(
                "Página " + (paginaActual + 1) +
                        " de " + totalPaginas
        );

        btnMarcarDeudaPagada.setEnabled(
                totalPendientes > 0);

        if (totalPendientes == 0) {
            return;
        }

        String consulta =
                "SELECT d.id, c.nombre, g.nombre, " +
                        "d.monto_deuda, a.nombre " +
                        "FROM deudas_compartidas d " +
                        "INNER JOIN contactos c " +
                        "ON c.id = d.contacto_id " +
                        "INNER JOIN gastos_compartidos g " +
                        "ON g.id = d.gasto_id " +
                        "INNER JOIN cuentas a " +
                        "ON a.id = g.cuenta_id " +
                        "WHERE d.estado = 'PENDIENTE' " +
                        "ORDER BY d.id DESC LIMIT ? OFFSET ?";

        try (Cursor cursor = db.rawQuery(
                consulta,
                new String[]{
                        String.valueOf(PAGE_SIZE),
                        String.valueOf(paginaActual * PAGE_SIZE)
                })) {

            while (cursor.moveToNext()) {

                long deudaId = cursor.getLong(0);
                String contacto = cursor.getString(1);
                String gasto = cursor.getString(2);
                double monto = cursor.getDouble(3);
                String cuenta = cursor.getString(4);

                RadioButton radio = new RadioButton(this);

                radio.setId(View.generateViewId());
                radio.setTag(deudaId);

                radio.setText(
                        contacto + "\n" +
                                gasto + " · " + formato.format(monto) +
                                "\nCuenta: " + cuenta
                );

                radio.setTextSize(13);

                rgDeudasPendientes.addView(radio);

                if (rgDeudasPendientes.getChildCount() == 1) {
                    rgDeudasPendientes.check(radio.getId());
                }
            }
        }

        btnMarcarDeudaPagada.setEnabled(
                rgDeudasPendientes.getChildCount() > 0);
    }

    private void registrarPago() {

        int seleccionado =
                rgDeudasPendientes.getCheckedRadioButtonId();

        if (seleccionado == -1) {
            Toast.makeText(
                    this,
                    "Selecciona una deuda pendiente",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        RadioButton radio =
                findViewById(seleccionado);

        long deudaId =
                ((Number) radio.getTag()).longValue();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        boolean guardado = false;
        String error = null;

        double monto = 0;
        long cuentaId = -1;
        String nombreContacto = "";
        String nombreGasto = "";

        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
        ).format(new Date());

        db.beginTransaction();

        try {

            // Leer la deuda y comprobar que sigue pendiente.
            try (Cursor cursor = db.rawQuery(
                    "SELECT d.monto_deuda, d.estado, " +
                            "c.nombre, g.nombre, g.cuenta_id " +
                            "FROM deudas_compartidas d " +
                            "INNER JOIN contactos c " +
                            "ON c.id = d.contacto_id " +
                            "INNER JOIN gastos_compartidos g " +
                            "ON g.id = d.gasto_id " +
                            "WHERE d.id = ?",
                    new String[]{String.valueOf(deudaId)})) {

                if (!cursor.moveToFirst()) {

                    error = "No se encontró la deuda.";

                } else if (!"PENDIENTE".equals(
                        cursor.getString(1))) {

                    error = "Esta deuda ya fue pagada.";

                } else {

                    monto = cursor.getDouble(0);
                    nombreContacto = cursor.getString(2);
                    nombreGasto = cursor.getString(3);
                    cuentaId = cursor.getLong(4);
                }
            }

            if (error == null) {

                // Actualización condicional para evitar
                // procesar la misma deuda dos veces.
                ContentValues estadoPagado = new ContentValues();
                estadoPagado.put("estado", "PAGADA");

                int filasActualizadas = db.update(
                        "deudas_compartidas",
                        estadoPagado,
                        "id = ? AND estado = 'PENDIENTE'",
                        new String[]{String.valueOf(deudaId)}
                );

                if (filasActualizadas != 1) {

                    error = "La deuda ya no está pendiente.";

                } else {

                    // Registrar el reembolso como un ingreso.
                    ContentValues ingreso = new ContentValues();

                    ingreso.put("cuenta_id", cuentaId);
                    ingreso.put("tipo", "INGRESO");
                    ingreso.put("monto", monto);

                    ingreso.put(
                            "descripcion",
                            "Reembolso de " + nombreContacto +
                                    " por " + nombreGasto
                    );

                    ingreso.put("fecha", fecha);

                    long movimientoId = db.insert(
                            "movimientos",
                            null,
                            ingreso
                    );

                    if (movimientoId == -1) {

                        error = "No se pudo registrar el ingreso.";

                    } else {

                        db.setTransactionSuccessful();
                        guardado = true;
                    }
                }
            }

        } catch (Exception e) {

            error = "No se pudo registrar el pago. " +
                    "No se realizaron cambios.";

        } finally {
            // Si hubo un error, SQLite revierte tanto
            // el cambio de estado como el ingreso.
            db.endTransaction();
        }

        if (!guardado) {

            Toast.makeText(
                    this,
                    error == null
                            ? "No se pudo completar el pago."
                            : error,
                    Toast.LENGTH_LONG
            ).show();

            mostrarDeudasPendientes();
            return;
        }

        Toast.makeText(
                this,
                "Pago registrado: " +
                        obtenerFormatoMoneda().format(monto) +
                        " recibido de " + nombreContacto,
                Toast.LENGTH_LONG
        ).show();

        mostrarDeudasPendientes();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
