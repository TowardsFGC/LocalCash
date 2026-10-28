
package com.example.localcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class GastosCompartidosActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgCuentasGasto;
    private LinearLayout llContactosGasto;

    private EditText etNombreGasto;
    private EditText etMontoGasto;

    private TextView tvSinCuentasGasto;
    private TextView tvSinContactosGasto;
    private TextView tvGastosCompartidos;

    private Button btnGuardarGastoCompartido;

    private boolean hayCuentas;
    private boolean hayContactos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gastos_compartidos);

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

        rgCuentasGasto = findViewById(R.id.rgCuentasGasto);
        llContactosGasto = findViewById(R.id.llContactosGasto);

        etNombreGasto = findViewById(R.id.etNombreGasto);
        etMontoGasto = findViewById(R.id.etMontoGasto);

        tvSinCuentasGasto = findViewById(R.id.tvSinCuentasGasto);
        tvSinContactosGasto =
                findViewById(R.id.tvSinContactosGasto);

        tvGastosCompartidos =
                findViewById(R.id.tvGastosCompartidos);

        btnGuardarGastoCompartido =
                findViewById(R.id.btnGuardarGastoCompartido);

        Button btnVolverGastoCompartido =
                findViewById(R.id.btnVolverGastoCompartido);

        dbHelper = new LocalCashDbHelper(this);

        cargarCuentas();
        cargarContactos();
        mostrarGastosCompartidos();

        btnGuardarGastoCompartido.setOnClickListener(
                v -> guardarGastoCompartido());

        btnVolverGastoCompartido.setOnClickListener(
                v -> finish());
    }

    private void cargarCuentas() {

        rgCuentasGasto.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int cantidad = 0;

        try (Cursor cursor = db.query(
                "cuentas",
                new String[]{"id", "nombre", "tipo"},
                null,
                null,
                null,
                null,
                "nombre ASC")) {

            while (cursor.moveToNext()) {

                long cuentaId = cursor.getLong(0);
                String nombre = cursor.getString(1);
                String tipo = cursor.getString(2);

                RadioButton radio = new RadioButton(this);

                radio.setId(View.generateViewId());
                radio.setTag(cuentaId);
                radio.setText(nombre + " (" + tipo + ")");
                radio.setTextSize(13);

                rgCuentasGasto.addView(radio);

                if (cantidad == 0) {
                    rgCuentasGasto.check(radio.getId());
                }

                cantidad++;
            }
        }

        hayCuentas = cantidad > 0;

        tvSinCuentasGasto.setVisibility(
                hayCuentas ? View.GONE : View.VISIBLE);

        actualizarEstadoBoton();
    }

    private void cargarContactos() {

        llContactosGasto.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int cantidad = 0;

        try (Cursor cursor = db.query(
                "contactos",
                new String[]{"id", "nombre"},
                null,
                null,
                null,
                null,
                "nombre COLLATE NOCASE ASC")) {

            while (cursor.moveToNext()) {

                long contactoId = cursor.getLong(0);
                String nombre = cursor.getString(1);

                CheckBox checkBox = new CheckBox(this);

                checkBox.setId(View.generateViewId());
                checkBox.setTag(contactoId);
                checkBox.setText(nombre);
                checkBox.setTextSize(14);

                llContactosGasto.addView(checkBox);

                cantidad++;
            }
        }

        hayContactos = cantidad > 0;

        tvSinContactosGasto.setVisibility(
                hayContactos ? View.GONE : View.VISIBLE);

        actualizarEstadoBoton();
    }

    private void actualizarEstadoBoton() {
        btnGuardarGastoCompartido.setEnabled(
                hayCuentas && hayContactos);
    }

    private void guardarGastoCompartido() {

        int cuentaSeleccionada =
                rgCuentasGasto.getCheckedRadioButtonId();

        if (cuentaSeleccionada == -1) {
            Toast.makeText(
                    this,
                    "Selecciona la cuenta que pagó",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        RadioButton radioCuenta =
                findViewById(cuentaSeleccionada);

        long cuentaId =
                ((Number) radioCuenta.getTag()).longValue();

        String nombre = etNombreGasto.getText()
                .toString().trim();

        if (nombre.isEmpty()) {
            etNombreGasto.setError(
                    "Ingresa el nombre del gasto");
            etNombreGasto.requestFocus();
            return;
        }

        String montoTexto = etMontoGasto.getText()
                .toString().trim()
                .replace(",", ".");

        if (montoTexto.isEmpty()) {
            etMontoGasto.setError("Ingresa el monto total");
            etMontoGasto.requestFocus();
            return;
        }

        double montoIngresado;

        try {
            montoIngresado = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            etMontoGasto.setError("Ingresa un monto válido");
            etMontoGasto.requestFocus();
            return;
        }

        if (Double.isNaN(montoIngresado)
                || Double.isInfinite(montoIngresado)
                || montoIngresado <= 0
                || montoIngresado > Long.MAX_VALUE) {

            etMontoGasto.setError(
                    "El monto debe ser mayor que cero");
            etMontoGasto.requestFocus();
            return;
        }

        long montoTotal = Math.round(montoIngresado);

        if (Math.abs(montoIngresado - montoTotal) > 0.000001) {
            etMontoGasto.setError(
                    "Introduce el monto en pesos enteros");
            etMontoGasto.requestFocus();
            return;
        }

        ArrayList<Long> contactosSeleccionados =
                new ArrayList<>();

        for (int i = 0; i < llContactosGasto.getChildCount(); i++) {

            View elemento = llContactosGasto.getChildAt(i);

            if (elemento instanceof CheckBox) {

                CheckBox checkBox = (CheckBox) elemento;

                if (checkBox.isChecked()) {

                    long contactoId =
                            ((Number) checkBox.getTag()).longValue();

                    contactosSeleccionados.add(contactoId);
                }
            }
        }

        if (contactosSeleccionados.isEmpty()) {
            Toast.makeText(
                    this,
                    "Selecciona al menos un contacto",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // El total se reparte entre el usuario y sus contactos.
        int totalParticipantes = contactosSeleccionados.size() + 1;

        long parteBase = montoTotal / totalParticipantes;
        long resto = montoTotal % totalParticipantes;

        if (parteBase <= 0) {
            Toast.makeText(
                    this,
                    "El monto es demasiado pequeño para repartirlo",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
        ).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        boolean guardado = false;

        db.beginTransaction();

        try {
            // Registrar el pago total como un único egreso.
            ContentValues movimiento = new ContentValues();

            movimiento.put("cuenta_id", cuentaId);
            movimiento.put("tipo", "EGRESO");
            movimiento.put("monto", (double) montoTotal);
            movimiento.put(
                    "descripcion",
                    "Gasto compartido: " + nombre);
            movimiento.put("fecha", fecha);

            long movimientoId = db.insert(
                    "movimientos",
                    null,
                    movimiento
            );

            if (movimientoId == -1) {
                throw new IllegalStateException(
                        "No se pudo registrar el egreso");
            }

            // Crear el gasto compartido y vincular el movimiento.
            ContentValues gasto = new ContentValues();

            gasto.put("nombre", nombre);
            gasto.put("monto_total", (double) montoTotal);
            gasto.put("fecha", fecha);
            gasto.put("cuenta_id", cuentaId);
            gasto.put("movimiento_id", movimientoId);
            gasto.put("fecha_registro", fecha);

            long gastoId = db.insert(
                    "gastos_compartidos",
                    null,
                    gasto
            );

            if (gastoId == -1) {
                throw new IllegalStateException(
                        "No se pudo registrar el gasto compartido");
            }

            // Cada contacto debe su parte.
            // El usuario asume su parte y el resto del reparto.
            for (Long contactoId : contactosSeleccionados) {

                ContentValues deuda = new ContentValues();

                deuda.put("gasto_id", gastoId);
                deuda.put("contacto_id", contactoId);
                deuda.put("monto_deuda", (double) parteBase);
                deuda.put("estado", "PENDIENTE");

                long deudaId = db.insert(
                        "deudas_compartidas",
                        null,
                        deuda
                );

                if (deudaId == -1) {
                    throw new IllegalStateException(
                            "No se pudo registrar una de las deudas");
                }
            }

            db.setTransactionSuccessful();
            guardado = true;

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No se pudo guardar el gasto compartido",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            db.endTransaction();
        }

        if (!guardado) {
            return;
        }

        Toast.makeText(
                this,
                "Gasto guardado. Cada contacto debe " +
                        NumberFormat.getCurrencyInstance(
                                        new Locale("es", "CL"))
                                .format(parteBase) +
                        ". Tu parte es " +
                        NumberFormat.getCurrencyInstance(
                                        new Locale("es", "CL"))
                                .format(parteBase + resto),
                Toast.LENGTH_LONG
        ).show();

        etNombreGasto.setText("");
        etMontoGasto.setText("");

        for (int i = 0; i < llContactosGasto.getChildCount(); i++) {

            View elemento = llContactosGasto.getChildAt(i);

            if (elemento instanceof CheckBox) {
                ((CheckBox) elemento).setChecked(false);
            }
        }

        mostrarGastosCompartidos();
    }

    private void mostrarGastosCompartidos() {

        StringBuilder texto = new StringBuilder();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        NumberFormat formatoMoneda =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        String consulta =
                "SELECT g.id, g.nombre, g.monto_total, g.fecha, a.nombre " +
                        "FROM gastos_compartidos g " +
                        "INNER JOIN cuentas a ON a.id = g.cuenta_id " +
                        "ORDER BY g.id DESC LIMIT 3";

        try (Cursor cursor = db.rawQuery(consulta, null)) {

            if (cursor.getCount() == 0) {
                tvGastosCompartidos.setText(
                        "Todavía no hay gastos compartidos.");
                return;
            }

            while (cursor.moveToNext()) {

                long gastoId = cursor.getLong(0);
                String nombre = cursor.getString(1);
                double montoTotal = cursor.getDouble(2);
                String fecha = cursor.getString(3);
                String cuenta = cursor.getString(4);

                texto.append(nombre)
                        .append("\nTotal: ")
                        .append(formatoMoneda.format(montoTotal))
                        .append("\nCuenta: ")
                        .append(cuenta)
                        .append("\nFecha: ")
                        .append(fecha)
                        .append("\nDeudas pendientes:\n");

                try (Cursor deudas = db.rawQuery(
                        "SELECT c.nombre, d.monto_deuda, d.estado " +
                                "FROM deudas_compartidas d " +
                                "INNER JOIN contactos c " +
                                "ON c.id = d.contacto_id " +
                                "WHERE d.gasto_id = ? " +
                                "ORDER BY c.nombre COLLATE NOCASE",
                        new String[]{String.valueOf(gastoId)})) {

                    while (deudas.moveToNext()) {

                        String contacto = deudas.getString(0);
                        double montoDeuda = deudas.getDouble(1);
                        String estado = deudas.getString(2);

                        texto.append("• ")
                                .append(contacto)
                                .append(": ")
                                .append(formatoMoneda.format(montoDeuda))
                                .append(" (")
                                .append(estado.equals("PENDIENTE")
                                        ? "Pendiente"
                                        : "Pagada")
                                .append(")\n");
                    }
                }

                texto.append("\n");
            }
        }

        tvGastosCompartidos.setText(texto.toString());
    }

    @Override
    protected void onDestroy() {

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
