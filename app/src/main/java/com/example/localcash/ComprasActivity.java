
package com.example.localcash;

import android.app.DatePickerDialog;
import android.content.ContentValues;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ComprasActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgCuentasCompra;
    private RadioGroup rgEstadoCompra;

    private EditText etNombreCompra;
    private EditText etMontoCompra;

    private TextView tvFechaCompra;
    private TextView tvComprasRegistradas;
    private TextView tvSinCuentasCompra;

    private Button btnGuardarCompra;

    private String fechaSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compras);

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

        rgCuentasCompra = findViewById(R.id.rgCuentasCompra);
        rgEstadoCompra = findViewById(R.id.rgEstadoCompra);

        etNombreCompra = findViewById(R.id.etNombreCompra);
        etMontoCompra = findViewById(R.id.etMontoCompra);

        tvFechaCompra = findViewById(R.id.tvFechaCompra);
        tvComprasRegistradas =
                findViewById(R.id.tvComprasRegistradas);
        tvSinCuentasCompra =
                findViewById(R.id.tvSinCuentasCompra);

        Button btnSeleccionarFecha =
                findViewById(R.id.btnSeleccionarFecha);

        btnGuardarCompra = findViewById(R.id.btnGuardarCompra);

        Button btnVolverCompras =
                findViewById(R.id.btnVolverCompras);

        dbHelper = new LocalCashDbHelper(this);

        fechaSeleccionada = obtenerFechaActual();
        actualizarTextoFecha();

        rgEstadoCompra.check(R.id.rbRealizada);

        cargarCuentas();
        mostrarCompras();

        btnSeleccionarFecha.setOnClickListener(
                v -> mostrarSelectorFecha());

        btnGuardarCompra.setOnClickListener(
                v -> guardarCompra());

        btnVolverCompras.setOnClickListener(
                v -> finish());
    }

    private String obtenerFechaActual() {

        SimpleDateFormat formato =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        return formato.format(new Date());
    }

    private void actualizarTextoFecha() {
        tvFechaCompra.setText(
                "Fecha seleccionada: " + fechaSeleccionada);
    }

    private void mostrarSelectorFecha() {

        Calendar calendario = Calendar.getInstance();

        try {
            String[] partes = fechaSeleccionada.split("-");

            calendario.set(
                    Integer.parseInt(partes[0]),
                    Integer.parseInt(partes[1]) - 1,
                    Integer.parseInt(partes[2])
            );

        } catch (Exception e) {
            calendario = Calendar.getInstance();
        }

        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog selector = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar seleccion = Calendar.getInstance();

                    seleccion.set(year, month, dayOfMonth);

                    SimpleDateFormat formato =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd", Locale.US);

                    fechaSeleccionada = formato.format(
                            seleccion.getTime());

                    actualizarTextoFecha();
                },
                anio,
                mes,
                dia
        );

        selector.show();
    }

    private void cargarCuentas() {

        rgCuentasCompra.removeAllViews();

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
                radio.setTextSize(14);

                rgCuentasCompra.addView(radio);

                if (cantidad == 0) {
                    rgCuentasCompra.check(radio.getId());
                }

                cantidad++;
            }
        }

        boolean hayCuentas = cantidad > 0;

        tvSinCuentasCompra.setVisibility(
                hayCuentas ? View.GONE : View.VISIBLE);

        btnGuardarCompra.setEnabled(hayCuentas);
    }

    private void guardarCompra() {

        int cuentaSeleccionada =
                rgCuentasCompra.getCheckedRadioButtonId();

        if (cuentaSeleccionada == -1) {
            Toast.makeText(
                    this,
                    "Selecciona una cuenta",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        RadioButton radioCuenta =
                findViewById(cuentaSeleccionada);

        long cuentaId =
                ((Number) radioCuenta.getTag()).longValue();

        String nombre = etNombreCompra.getText()
                .toString().trim();

        String montoTexto = etMontoCompra.getText()
                .toString().trim()
                .replace(",", ".");

        if (nombre.isEmpty()) {
            etNombreCompra.setError(
                    "Ingresa el nombre de la compra");
            etNombreCompra.requestFocus();
            return;
        }

        if (montoTexto.isEmpty()) {
            etMontoCompra.setError("Ingresa el monto");
            etMontoCompra.requestFocus();
            return;
        }

        double monto;

        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            etMontoCompra.setError("Ingresa un monto válido");
            etMontoCompra.requestFocus();
            return;
        }

        if (Double.isNaN(monto)
                || Double.isInfinite(monto)
                || monto <= 0) {

            etMontoCompra.setError(
                    "El monto debe ser mayor que cero");
            etMontoCompra.requestFocus();
            return;
        }

        int estadoSeleccionado =
                rgEstadoCompra.getCheckedRadioButtonId();

        if (estadoSeleccionado == -1) {
            Toast.makeText(
                    this,
                    "Selecciona el estado de la compra",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String estado =
                estadoSeleccionado == R.id.rbPlanificada
                        ? "PLANIFICADA"
                        : "REALIZADA";

        String hoy = obtenerFechaActual();

        if (estado.equals("PLANIFICADA")
                && fechaSeleccionada.compareTo(hoy) < 0) {

            Toast.makeText(
                    this,
                    "La fecha planificada no puede estar en el pasado",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (estado.equals("REALIZADA")
                && fechaSeleccionada.compareTo(hoy) > 0) {

            Toast.makeText(
                    this,
                    "Una compra realizada no puede tener una fecha futura",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String fechaRegistro = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
        ).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        boolean guardado = false;

        db.beginTransaction();

        try {
            ContentValues compra = new ContentValues();

            compra.put("nombre", nombre);
            compra.put("monto", monto);
            compra.put("fecha_compra", fechaSeleccionada);
            compra.put("estado", estado);
            compra.put("fecha_registro", fechaRegistro);
            compra.put("cuenta_id", cuentaId);

            long compraId = db.insert(
                    "compras",
                    null,
                    compra
            );

            if (compraId == -1) {
                throw new IllegalStateException(
                        "No se pudo guardar la compra");
            }

            if (estado.equals("REALIZADA")) {

                ContentValues movimiento = new ContentValues();

                movimiento.put("cuenta_id", cuentaId);
                movimiento.put("tipo", "EGRESO");
                movimiento.put("monto", monto);
                movimiento.put(
                        "descripcion",
                        "Compra: " + nombre);
                movimiento.put(
                        "fecha",
                        fechaSeleccionada + " 12:00:00");

                long movimientoId = db.insert(
                        "movimientos",
                        null,
                        movimiento
                );

                if (movimientoId == -1) {
                    throw new IllegalStateException(
                            "No se pudo registrar el egreso");
                }

                ContentValues enlace = new ContentValues();
                enlace.put("movimiento_id", movimientoId);

                int actualizado = db.update(
                        "compras",
                        enlace,
                        "id = ?",
                        new String[]{String.valueOf(compraId)}
                );

                if (actualizado != 1) {
                    throw new IllegalStateException(
                            "No se pudo enlazar el movimiento");
                }
            }

            db.setTransactionSuccessful();
            guardado = true;

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "No se pudo guardar la compra y su movimiento",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            db.endTransaction();
        }

        if (!guardado) {
            return;
        }

        String mensaje = estado.equals("REALIZADA")
                ? "Compra guardada y egreso registrado"
                : "Compra planificada sin descontar dinero";

        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_LONG
        ).show();

        etNombreCompra.setText("");
        etMontoCompra.setText("");

        rgEstadoCompra.check(R.id.rbRealizada);

        fechaSeleccionada = obtenerFechaActual();
        actualizarTextoFecha();

        mostrarCompras();
    }

    private void mostrarCompras() {

        StringBuilder texto = new StringBuilder();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        NumberFormat formatoMoneda =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        String consulta =
                "SELECT c.nombre, c.monto, c.fecha_compra, " +
                        "c.estado, a.nombre " +
                        "FROM compras c " +
                        "LEFT JOIN cuentas a ON a.id = c.cuenta_id " +
                        "ORDER BY c.id DESC LIMIT 5";

        try (Cursor cursor = db.rawQuery(consulta, null)) {

            if (cursor.getCount() == 0) {
                tvComprasRegistradas.setText(
                        "Todavía no hay compras.");
                return;
            }

            while (cursor.moveToNext()) {

                String nombre = cursor.getString(0);
                double monto = cursor.getDouble(1);
                String fecha = cursor.getString(2);
                String estado = cursor.getString(3);
                String cuenta = cursor.getString(4);

                String estadoVisible =
                        estado.equals("PLANIFICADA")
                                ? "Planificada"
                                : "Realizada";

                texto.append(nombre)
                        .append("\nEstado: ")
                        .append(estadoVisible)
                        .append("\nCuenta: ")
                        .append(cuenta == null
                                ? "Sin cuenta asociada"
                                : cuenta)
                        .append("\nMonto: ")
                        .append(formatoMoneda.format(monto))
                        .append("\nFecha: ")
                        .append(fecha)
                        .append("\n\n");
            }
        }

        tvComprasRegistradas.setText(texto.toString());
    }

    @Override
    protected void onDestroy() {

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
