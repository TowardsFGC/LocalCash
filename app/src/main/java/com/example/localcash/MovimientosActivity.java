package com.example.localcash;

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
import java.util.Date;
import java.util.Locale;

public class MovimientosActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgCuentas;
    private RadioGroup rgTipoMovimiento;

    private EditText etMontoMovimiento;
    private EditText etDescripcionMovimiento;

    private TextView tvSinCuentas;
    private TextView tvMovimientosRegistrados;

    private Button btnGuardarMovimiento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movimientos);

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

        rgCuentas = findViewById(R.id.rgCuentas);
        rgTipoMovimiento = findViewById(R.id.rgTipoMovimiento);

        etMontoMovimiento =
                findViewById(R.id.etMontoMovimiento);

        etDescripcionMovimiento =
                findViewById(R.id.etDescripcionMovimiento);

        tvSinCuentas = findViewById(R.id.tvSinCuentas);

        tvMovimientosRegistrados =
                findViewById(R.id.tvMovimientosRegistrados);

        btnGuardarMovimiento =
                findViewById(R.id.btnGuardarMovimiento);

        Button btnVolverMovimientos =
                findViewById(R.id.btnVolverMovimientos);

        dbHelper = new LocalCashDbHelper(this);

        rgTipoMovimiento.check(R.id.rbEgreso);

        cargarCuentas();
        mostrarMovimientos();

        btnGuardarMovimiento.setOnClickListener(
                v -> guardarMovimiento());

        btnVolverMovimientos.setOnClickListener(
                v -> finish());
    }

    private void cargarCuentas() {

        rgCuentas.removeAllViews();

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
                radio.setTextSize(15);

                rgCuentas.addView(radio);

                if (cantidad == 0) {
                    rgCuentas.check(radio.getId());
                }

                cantidad++;
            }
        }

        boolean hayCuentas = cantidad > 0;

        tvSinCuentas.setVisibility(
                hayCuentas ? View.GONE : View.VISIBLE);

        btnGuardarMovimiento.setEnabled(hayCuentas);
    }

    private void guardarMovimiento() {

        int cuentaSeleccionada =
                rgCuentas.getCheckedRadioButtonId();

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

        String montoTexto = etMontoMovimiento.getText()
                .toString()
                .trim()
                .replace(",", ".");

        if (montoTexto.isEmpty()) {
            etMontoMovimiento.setError("Ingresa un monto");
            etMontoMovimiento.requestFocus();
            return;
        }

        double monto;

        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            etMontoMovimiento.setError(
                    "Ingresa un monto válido");
            etMontoMovimiento.requestFocus();
            return;
        }

        if (Double.isNaN(monto)
                || Double.isInfinite(monto)
                || monto <= 0) {

            etMontoMovimiento.setError(
                    "El monto debe ser mayor que cero");
            etMontoMovimiento.requestFocus();
            return;
        }

        int tipoSeleccionado =
                rgTipoMovimiento.getCheckedRadioButtonId();

        if (tipoSeleccionado == -1) {
            Toast.makeText(
                    this,
                    "Selecciona ingreso o gasto",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String tipo = tipoSeleccionado == R.id.rbIngreso
                ? "INGRESO"
                : "EGRESO";

        String descripcion = etDescripcionMovimiento
                .getText()
                .toString()
                .trim();

        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        ContentValues values = new ContentValues();

        values.put("cuenta_id", cuentaId);
        values.put("tipo", tipo);
        values.put("monto", monto);
        values.put("descripcion", descripcion);
        values.put("fecha", fecha);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long id = db.insert("movimientos", null, values);

        if (id == -1) {
            Toast.makeText(
                    this,
                    "No se pudo guardar el movimiento",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(
                this,
                "Movimiento guardado correctamente",
                Toast.LENGTH_SHORT
        ).show();

        etMontoMovimiento.setText("");
        etDescripcionMovimiento.setText("");

        mostrarMovimientos();
    }

    private void mostrarMovimientos() {

        StringBuilder texto = new StringBuilder();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        NumberFormat formatoMoneda =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        String consulta =
                "SELECT m.tipo, m.monto, m.descripcion, " +
                        "m.fecha, c.nombre " +
                        "FROM movimientos m " +
                        "INNER JOIN cuentas c ON c.id = m.cuenta_id " +
                        "ORDER BY m.id DESC LIMIT 10";

        try (Cursor cursor = db.rawQuery(consulta, null)) {

            if (cursor.getCount() == 0) {
                tvMovimientosRegistrados.setText(
                        "Todavía no hay movimientos.");
                return;
            }

            while (cursor.moveToNext()) {

                String tipo = cursor.getString(0);
                double monto = cursor.getDouble(1);
                String descripcion = cursor.getString(2);
                String fecha = cursor.getString(3);
                String nombreCuenta = cursor.getString(4);

                boolean esIngreso = tipo.equals("INGRESO");

                texto.append(esIngreso ? "INGRESO" : "GASTO")
                        .append(" · ")
                        .append(nombreCuenta)
                        .append("\n");

                texto.append(esIngreso ? "+ " : "- ")
                        .append(formatoMoneda.format(monto))
                        .append("\n");

                if (descripcion != null
                        && !descripcion.trim().isEmpty()) {

                    texto.append(descripcion).append("\n");
                }

                texto.append(fecha).append("\n\n");
            }
        }

        tvMovimientosRegistrados.setText(texto.toString());
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
