
package com.example.localcash;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.text.NumberFormat;
import java.util.Locale;

public class MapaActivity extends AppCompatActivity {

    private MapView mapView;
    private LocalCashDbHelper dbHelper;
    private TextView tvEstadoMapa;

    private static final LatLng SANTIAGO =
            new LatLng(-33.4489, -70.6693);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);

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

        mapView = findViewById(R.id.mapView);
        tvEstadoMapa = findViewById(R.id.tvEstadoMapa);

        Button btnVolverMapa =
                findViewById(R.id.btnVolverMapa);

        btnVolverMapa.setOnClickListener(v -> finish());

        dbHelper = new LocalCashDbHelper(this);

        mapView.onCreate(savedInstanceState);

        String apiKey = BuildConfig.MAPTILER_API_KEY;

        if (apiKey == null || apiKey.trim().isEmpty()) {

            tvEstadoMapa.setText(
                    "Configura la clave API de MapTiler.");

            return;
        }

        String estiloMapa =
                "https://api.maptiler.com/maps/streets-v4/style.json?key="
                        + apiKey;

        mapView.getMapAsync(mapLibreMap -> {

            mapLibreMap.setStyle(estiloMapa, style -> {

                // Cargar los movimientos después de cargar el mapa.
                cargarMovimientosEnMapa(mapLibreMap);
            });
        });
    }

    private void cargarMovimientosEnMapa(
            MapLibreMap mapa) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        NumberFormat formatoMoneda =
                NumberFormat.getCurrencyInstance(
                        new Locale("es", "CL"));

        formatoMoneda.setMaximumFractionDigits(0);
        formatoMoneda.setMinimumFractionDigits(0);

        String consulta =
                "SELECT m.id, c.nombre, m.tipo, m.monto, " +
                        "m.descripcion, m.fecha, m.latitud, m.longitud " +
                        "FROM movimientos m " +
                        "INNER JOIN cuentas c ON c.id = m.cuenta_id " +
                        "WHERE m.latitud IS NOT NULL " +
                        "AND m.longitud IS NOT NULL " +
                        "ORDER BY m.id DESC";

        int cantidad = 0;
        LatLng primeraUbicacion = null;

        try (Cursor cursor = db.rawQuery(consulta, null)) {

            while (cursor.moveToNext()) {

                double latitud = cursor.getDouble(6);
                double longitud = cursor.getDouble(7);

                // Descartar coordenadas fuera de rango.
                if (latitud < -90 || latitud > 90
                        || longitud < -180 || longitud > 180) {
                    continue;
                }

                String cuenta = cursor.getString(1);
                String tipo = cursor.getString(2);
                double monto = cursor.getDouble(3);

                String descripcion = cursor.isNull(4)
                        ? ""
                        : cursor.getString(4);

                String fecha = cursor.getString(5);

                LatLng posicion =
                        new LatLng(latitud, longitud);

                if (primeraUbicacion == null) {
                    primeraUbicacion = posicion;
                }

                String tipoVisible =
                        tipo.equals("INGRESO")
                                ? "Ingreso"
                                : "Gasto";

                String titulo =
                        tipoVisible + " · " + cuenta;

                String detalle =
                        formatoMoneda.format(monto);

                if (!descripcion.trim().isEmpty()) {
                    detalle += "\n" + descripcion;
                }

                detalle += "\nFecha: " + fecha;

                // Crear el marcador de este movimiento.
                mapa.addMarker(
                        new MarkerOptions()
                                .position(posicion)
                                .title(titulo)
                                .snippet(detalle)
                );

                cantidad++;
            }
        }

        if (cantidad == 0) {

            // Centro inicial únicamente; no es una transacción.
            mapa.setCameraPosition(
                    new CameraPosition.Builder()
                            .target(SANTIAGO)
                            .zoom(11.0)
                            .build()
            );

            tvEstadoMapa.setText(
                    "No hay movimientos geolocalizados. " +
                            "Marca 'Guardar ubicación actual' " +
                            "al registrar un ingreso o gasto.");

        } else {

            mapa.setCameraPosition(
                    new CameraPosition.Builder()
                            .target(primeraUbicacion)
                            .zoom(cantidad == 1 ? 14.0 : 12.0)
                            .build()
            );

            tvEstadoMapa.setText(
                    "Movimientos con ubicación: " + cantidad +
                            ". Toca un marcador para ver sus detalles.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }

        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {

        if (mapView != null) {
            mapView.onDestroy();
        }

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
