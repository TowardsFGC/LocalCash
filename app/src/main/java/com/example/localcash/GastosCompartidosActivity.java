package com.example.localcash;

import android.content.ContentValues;
import android.content.Intent;
import android.text.InputType;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

public class GastosCompartidosActivity extends AppCompatActivity {

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgCuentasGasto;
    private LinearLayout llContactosGasto;

    private EditText etNombreGasto;
    private EditText etMontoGasto;
    private EditText etDetalleGasto;
    private CheckBox cbRepartoPersonalizado;

    private final ArrayList<ContactoGastoUI> filasContactos =
            new ArrayList<>();

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
        etDetalleGasto = findViewById(R.id.etDetalleGasto);
        cbRepartoPersonalizado = findViewById(R.id.cbRepartoPersonalizado);

        tvSinCuentasGasto =
                findViewById(R.id.tvSinCuentasGasto);

        tvSinContactosGasto =
                findViewById(R.id.tvSinContactosGasto);

        tvGastosCompartidos =
                findViewById(R.id.tvGastosCompartidos);

        btnGuardarGastoCompartido =
                findViewById(R.id.btnGuardarGastoCompartido);

        Button btnGestionarDeudas =
                findViewById(R.id.btnGestionarDeudas);

        Button btnVolverGastoCompartido =
                findViewById(R.id.btnVolverGastoCompartido);

        dbHelper = new LocalCashDbHelper(this);

        cbRepartoPersonalizado.setOnCheckedChangeListener(
                (buttonView, checked) -> actualizarVisibilidadDeudasPersonalizadas());

        cargarCuentas();
        cargarContactos();
        mostrarGastosCompartidos();

        btnGuardarGastoCompartido.setOnClickListener(
                v -> guardarGastoCompartido());

        // Abrir la pantalla de gestión de deudas.
        btnGestionarDeudas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    GastosCompartidosActivity.this,
                    DeudasActivity.class
            );

            startActivity(intent);
        });

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
        filasContactos.clear();

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

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
                fila.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                CheckBox checkBox = new CheckBox(this);
                checkBox.setId(View.generateViewId());
                checkBox.setTag(contactoId);
                checkBox.setText(nombre);
                checkBox.setTextSize(14);
                checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f));

                EditText etDeuda = new EditText(this);
                etDeuda.setId(View.generateViewId());
                etDeuda.setHint("Deuda CLP");
                etDeuda.setSingleLine(true);
                etDeuda.setInputType(InputType.TYPE_CLASS_NUMBER);
                etDeuda.setTextSize(13);
                etDeuda.setPadding(8, 4, 8, 4);
                etDeuda.setLayoutParams(new LinearLayout.LayoutParams(
                        dpApx(112),
                        dpApx(44)));
                etDeuda.setVisibility(View.GONE);

                ContactoGastoUI contactoUI =
                        new ContactoGastoUI(contactoId, checkBox, etDeuda);
                filasContactos.add(contactoUI);

                checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
                    boolean mostrarMonto = checked
                            && cbRepartoPersonalizado.isChecked();
                    etDeuda.setVisibility(mostrarMonto ? View.VISIBLE : View.GONE);
                    if (!checked) {
                        etDeuda.setText("");
                    }
                });

                fila.addView(checkBox);
                fila.addView(etDeuda);
                llContactosGasto.addView(fila);
                cantidad++;
            }
        }

        hayContactos = cantidad > 0;
        tvSinContactosGasto.setVisibility(
                hayContactos ? View.GONE : View.VISIBLE);

        actualizarEstadoBoton();
        actualizarVisibilidadDeudasPersonalizadas();
    }

    private int dpApx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void actualizarVisibilidadDeudasPersonalizadas() {
        boolean personalizado = cbRepartoPersonalizado.isChecked();
        for (ContactoGastoUI contacto : filasContactos) {
            boolean seleccionado = contacto.checkBox.isChecked();
            contacto.etDeuda.setVisibility(
                    personalizado && seleccionado ? View.VISIBLE : View.GONE);
            if (!personalizado) {
                contacto.etDeuda.setText("");
            }
        }
    }

    private void actualizarEstadoBoton() {

        btnGuardarGastoCompartido.setEnabled(
                hayCuentas && hayContactos);
    }

    private void guardarGastoCompartido() {

        int cuentaSeleccionada = rgCuentasGasto.getCheckedRadioButtonId();
        if (cuentaSeleccionada == -1) {
            Toast.makeText(this, "Selecciona la cuenta que pagó", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton radioCuenta = findViewById(cuentaSeleccionada);
        long cuentaId = ((Number) radioCuenta.getTag()).longValue();

        String nombre = etNombreGasto.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombreGasto.setError("Ingresa el nombre del gasto");
            etNombreGasto.requestFocus();
            return;
        }

        String detalleTexto = etDetalleGasto.getText().toString().trim();
        ArrayList<String> detalles = new ArrayList<>();
        for (String linea : detalleTexto.split("\\r?\\n")) {
            String detalle = linea.trim();
            if (!detalle.isEmpty()) {
                detalles.add(detalle);
            }
        }
        if (detalles.isEmpty()) {
            etDetalleGasto.setError(
                    "Agrega al menos un producto o servicio, uno por línea");
            etDetalleGasto.requestFocus();
            return;
        }

        String montoTexto = etMontoGasto.getText().toString().trim().replace(",", ".");
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
            etMontoGasto.setError("El monto debe ser mayor que cero");
            etMontoGasto.requestFocus();
            return;
        }

        long montoTotal = Math.round(montoIngresado);
        if (Math.abs(montoIngresado - montoTotal) > 0.000001) {
            etMontoGasto.setError("Introduce el monto en pesos enteros");
            etMontoGasto.requestFocus();
            return;
        }

        ArrayList<ContactoGastoUI> seleccionados = new ArrayList<>();
        for (ContactoGastoUI contacto : filasContactos) {
            if (contacto.checkBox.isChecked()) {
                seleccionados.add(contacto);
            }
        }

        if (seleccionados.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un contacto", Toast.LENGTH_SHORT).show();
            return;
        }

        // El mapa guarda el monto que cada contacto debe devolver.
        Map<Long, Long> montosDeuda = new LinkedHashMap<>();
        long sumaDeudas = 0L;

        if (cbRepartoPersonalizado.isChecked()) {
            for (ContactoGastoUI contacto : seleccionados) {
                String deudaTexto = contacto.etDeuda.getText().toString().trim();
                if (deudaTexto.isEmpty()) {
                    contacto.etDeuda.setError("Indica cuánto debe esta persona");
                    contacto.etDeuda.requestFocus();
                    return;
                }

                long montoDeuda;
                try {
                    montoDeuda = Long.parseLong(deudaTexto);
                } catch (NumberFormatException e) {
                    contacto.etDeuda.setError("Monto entero válido");
                    contacto.etDeuda.requestFocus();
                    return;
                }

                if (montoDeuda <= 0 || montoDeuda > montoTotal - sumaDeudas) {
                    contacto.etDeuda.setError(
                            "La deuda debe ser positiva y no superar el total restante");
                    contacto.etDeuda.requestFocus();
                    return;
                }

                montosDeuda.put(contacto.contactoId, montoDeuda);
                sumaDeudas += montoDeuda;
            }
        } else {
            int totalParticipantes = seleccionados.size() + 1;
            long parteBase = montoTotal / totalParticipantes;
            if (parteBase <= 0) {
                Toast.makeText(this,
                        "El monto es demasiado pequeño para repartirlo",
                        Toast.LENGTH_LONG).show();
                return;
            }
            for (ContactoGastoUI contacto : seleccionados) {
                montosDeuda.put(contacto.contactoId, parteBase);
                sumaDeudas += parteBase;
            }
        }

        long parteUsuario = montoTotal - sumaDeudas;
        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean guardado = false;
        db.beginTransaction();

        try {
            ContentValues movimiento = new ContentValues();
            movimiento.put("cuenta_id", cuentaId);
            movimiento.put("tipo", "EGRESO");
            movimiento.put("monto", (double) montoTotal);
            movimiento.put("descripcion", "Gasto compartido: " + nombre);
            movimiento.put("fecha", fecha);

            long movimientoId = db.insert("movimientos", null, movimiento);
            if (movimientoId == -1) {
                throw new IllegalStateException("No se pudo registrar el egreso");
            }

            ContentValues gasto = new ContentValues();
            gasto.put("nombre", nombre);
            gasto.put("monto_total", (double) montoTotal);
            gasto.put("fecha", fecha);
            gasto.put("cuenta_id", cuentaId);
            gasto.put("movimiento_id", movimientoId);
            gasto.put("fecha_registro", fecha);

            long gastoId = db.insert("gastos_compartidos", null, gasto);
            if (gastoId == -1) {
                throw new IllegalStateException("No se pudo registrar el gasto compartido");
            }

            // Guardar un registro por producto o servicio indicado en el formulario.
            for (String detalle : detalles) {
                ContentValues item = new ContentValues();
                item.put("gasto_id", gastoId);
                item.put("descripcion", detalle);
                if (db.insert("gastos_compartidos_detalles", null, item) == -1) {
                    throw new IllegalStateException("No se pudo guardar el detalle del gasto");
                }
            }

            for (Map.Entry<Long, Long> deudaContacto : montosDeuda.entrySet()) {
                ContentValues deuda = new ContentValues();
                deuda.put("gasto_id", gastoId);
                deuda.put("contacto_id", deudaContacto.getKey());
                deuda.put("monto_deuda", (double) deudaContacto.getValue());
                deuda.put("estado", "PENDIENTE");

                if (db.insert("deudas_compartidas", null, deuda) == -1) {
                    throw new IllegalStateException("No se pudo registrar una de las deudas");
                }
            }

            db.setTransactionSuccessful();
            guardado = true;

        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo guardar el gasto compartido. Revisa los datos e inténtalo otra vez.",
                    Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        if (!guardado) {
            return;
        }

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        String mensaje;
        if (cbRepartoPersonalizado.isChecked()) {
            mensaje = "Gasto guardado con deudas personalizadas. Tu parte es "
                    + formatoMoneda.format(parteUsuario);
        } else {
            long parteContacto = montosDeuda.isEmpty()
                    ? 0L : montosDeuda.values().iterator().next();
            mensaje = "Gasto guardado. Cada contacto debe "
                    + formatoMoneda.format(parteContacto)
                    + ". Tu parte es " + formatoMoneda.format(parteUsuario);
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

        etNombreGasto.setText("");
        etMontoGasto.setText("");
        etDetalleGasto.setText("");
        cbRepartoPersonalizado.setChecked(false);
        for (ContactoGastoUI contacto : filasContactos) {
            contacto.checkBox.setChecked(false);
            contacto.etDeuda.setText("");
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
                "SELECT g.id, g.nombre, g.monto_total, " +
                        "g.fecha, a.nombre " +
                        "FROM gastos_compartidos g " +
                        "INNER JOIN cuentas a ON a.id = g.cuenta_id " +
                        "ORDER BY g.id DESC";

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
                        .append("\nProductos/servicios:");

                boolean tieneDetalles = false;
                try (Cursor detalles = db.rawQuery(
                        "SELECT descripcion FROM gastos_compartidos_detalles " +
                                "WHERE gasto_id = ? ORDER BY id ASC",
                        new String[]{String.valueOf(gastoId)})) {
                    while (detalles.moveToNext()) {
                        tieneDetalles = true;
                        texto.append("\n  - ").append(detalles.getString(0));
                    }
                }
                if (!tieneDetalles) {
                    texto.append("\n  (Sin detalle registrado)");
                }
                texto.append("\nDeudas:\n");

                try (Cursor deudas = db.rawQuery(
                        "SELECT c.nombre, d.monto_deuda, d.estado " +
                                "FROM deudas_compartidas d " +
                                "INNER JOIN contactos c " +
                                "ON c.id = d.contacto_id " +
                                "WHERE d.gasto_id = ? " +
                                "ORDER BY c.nombre COLLATE NOCASE",
                        new String[]{
                                String.valueOf(gastoId)
                        })) {

                    while (deudas.moveToNext()) {

                        String contacto = deudas.getString(0);
                        double montoDeuda = deudas.getDouble(1);
                        String estado = deudas.getString(2);

                        texto.append("- ")
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

    private static class ContactoGastoUI {
        final long contactoId;
        final CheckBox checkBox;
        final EditText etDeuda;

        ContactoGastoUI(long contactoId, CheckBox checkBox, EditText etDeuda) {
            this.contactoId = contactoId;
            this.checkBox = checkBox;
            this.etDeuda = etDeuda;
        }
    }


    protected void onResume() {
        super.onResume();

        if (dbHelper != null && tvGastosCompartidos != null) {
            mostrarGastosCompartidos();
        }
    }


    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
