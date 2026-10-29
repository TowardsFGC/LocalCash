package com.example.localcash;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MovimientosActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final long UBICACION_RECIENTE_MS = 5 * 60 * 1000L;

    private LocalCashDbHelper dbHelper;

    private RadioGroup rgCuentas;
    private RadioGroup rgTipoMovimiento;

    private EditText etMontoMovimiento;
    private EditText etDescripcionMovimiento;

    private CheckBox cbGuardarUbicacion;

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

        cbGuardarUbicacion =
                findViewById(R.id.cbGuardarUbicacion);

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
                .toString().trim().replace(",", ".");

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

        String descripcion = etDescripcionMovimiento.getText()
                .toString().trim();

        String fecha = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        if (!cbGuardarUbicacion.isChecked()) {
            guardarEnSQLite(
                    cuentaId, tipo, monto, descripcion, fecha,
                    null, null
            );
            return;
        }

        // Solicitar permiso solo si el usuario quiere guardar
        // la ubicación del movimiento.
        if (!tienePermisoUbicacion()) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );

            return;
        }

        obtenerUbicacionYGuardar(
                cuentaId, tipo, monto, descripcion, fecha
        );
    }

    private boolean tienePermisoUbicacion() {

        boolean permisoPreciso =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean permisoAproximado =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return permisoPreciso || permisoAproximado;
    }

    private void obtenerUbicacionYGuardar(
            long cuentaId,
            String tipo,
            double monto,
            String descripcion,
            String fecha) {

        LocationManager manager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean tieneUbicacionPrecisa =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        String proveedor = null;

        try {
            if (tieneUbicacionPrecisa
                    && manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                proveedor = LocationManager.GPS_PROVIDER;

            } else if (manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {

                proveedor = LocationManager.NETWORK_PROVIDER;
            }

        } catch (SecurityException | IllegalArgumentException e) {
            proveedor = null;
        }

        if (proveedor == null) {
            Toast.makeText(
                    this,
                    "Activa la ubicación del dispositivo o emulador.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {

            // En Android 11 o posterior, solicitar una ubicación actual.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                btnGuardarMovimiento.setEnabled(false);

                manager.getCurrentLocation(
                        proveedor,
                        null,
                        ContextCompat.getMainExecutor(this),
                        location -> {

                            btnGuardarMovimiento.setEnabled(true);

                            if (location == null) {
                                Toast.makeText(
                                        this,
                                        "No se pudo obtener la ubicación. " +
                                                "Configura la ubicación del emulador " +
                                                "e inténtalo otra vez.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            guardarEnSQLite(
                                    cuentaId, tipo, monto, descripcion,
                                    fecha, location.getLatitude(),
                                    location.getLongitude()
                            );
                        }
                );

            } else {

                // Compatibilidad con Android 8.1 a Android 10.
                Location ultimaUbicacion =
                        manager.getLastKnownLocation(proveedor);

                if (ubicacionEsReciente(ultimaUbicacion)) {

                    guardarEnSQLite(
                            cuentaId, tipo, monto, descripcion,
                            fecha, ultimaUbicacion.getLatitude(),
                            ultimaUbicacion.getLongitude()
                    );

                    return;
                }

                btnGuardarMovimiento.setEnabled(false);

                LocationListener listener =
                        new LocationListener() {

                            @Override
                            public void onLocationChanged(
                                    Location location) {

                                manager.removeUpdates(this);
                                btnGuardarMovimiento.setEnabled(true);

                                guardarEnSQLite(
                                        cuentaId, tipo, monto,
                                        descripcion, fecha,
                                        location.getLatitude(),
                                        location.getLongitude()
                                );
                            }

                            @Override
                            public void onStatusChanged(
                                    String provider,
                                    int status,
                                    Bundle extras) {
                            }

                            @Override
                            public void onProviderEnabled(
                                    String provider) {
                            }

                            @Override
                            public void onProviderDisabled(
                                    String provider) {
                            }
                        };

                manager.requestSingleUpdate(
                        proveedor,
                        listener,
                        Looper.getMainLooper()
                );
            }

        } catch (SecurityException | IllegalArgumentException e) {

            btnGuardarMovimiento.setEnabled(true);

            Toast.makeText(
                    this,
                    "No se pudo acceder al sensor de ubicación.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean ubicacionEsReciente(Location location) {

        if (location == null) {
            return false;
        }

        long edad = System.currentTimeMillis() - location.getTime();

        return edad >= 0 && edad <= UBICACION_RECIENTE_MS;
    }

    private void guardarEnSQLite(
            long cuentaId,
            String tipo,
            double monto,
            String descripcion,
            String fecha,
            Double latitud,
            Double longitud) {

        ContentValues values = new ContentValues();

        values.put("cuenta_id", cuentaId);
        values.put("tipo", tipo);
        values.put("monto", monto);
        values.put("descripcion", descripcion);
        values.put("fecha", fecha);

        if (latitud != null && longitud != null) {
            values.put("latitud", latitud);
            values.put("longitud", longitud);
        }

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

        String mensaje =
                latitud != null && longitud != null
                        ? "Movimiento guardado con ubicación"
                        : "Movimiento guardado correctamente";

        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
        ).show();

        etMontoMovimiento.setText("");
        etDescripcionMovimiento.setText("");
        cbGuardarUbicacion.setChecked(false);

        mostrarMovimientos();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {

            if (tienePermisoUbicacion()) {
                // Reintentar el guardado con el permiso concedido.
                guardarMovimiento();

            } else {
                cbGuardarUbicacion.setChecked(false);

                Toast.makeText(
                        this,
                        "No se concedió el permiso. " +
                                "El movimiento no se ha guardado.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
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
                        "m.fecha, c.nombre, m.latitud, m.longitud " +
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

                if (!cursor.isNull(5) && !cursor.isNull(6)) {
                    texto.append("Ubicación guardada")
                            .append("\n");
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
