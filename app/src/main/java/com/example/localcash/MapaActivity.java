
package com.example.localcash;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.module.http.HttpRequestUtil;
import org.maplibre.android.offline.OfflineManager;
import org.maplibre.android.offline.OfflineRegion;
import org.maplibre.android.offline.OfflineRegionError;
import org.maplibre.android.offline.OfflineRegionStatus;
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

public class MapaActivity extends AppCompatActivity {

    private static final String USER_AGENT =
            "LocalCash/1.0 (com.example.localcash)";

    private static final String ESTILO_MAPA =
            "https://tiles.openfreemap.org/styles/liberty";

    private static final String REGION_METADATA =
            "localcash-santiago-centro-v1";

    private static final long MAX_TILES = 3000L;
    private static final long MAX_BYTES = 50L * 1024L * 1024L;

    private static final LatLng SANTIAGO =
            new LatLng(-33.4489, -70.6693);

    private MapView mapView;
    private LocalCashDbHelper dbHelper;
    private OfflineManager offlineManager;

    private TextView tvEstadoMapa;
    private TextView tvResumenMovimientos;
    private Button btnDescargarMapaOffline;

    private boolean descargaEnCurso = false;
    private boolean limiteDescargaResuelto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        configurarClienteHTTP();

        offlineManager = OfflineManager.getInstance(this);
        offlineManager.setOfflineMapboxTileCountLimit(MAX_TILES);

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
        tvResumenMovimientos =
                findViewById(R.id.tvResumenMovimientos);

        btnDescargarMapaOffline =
                findViewById(R.id.btnDescargarMapaOffline);

        Button btnVolverMapa =
                findViewById(R.id.btnVolverMapa);

        dbHelper = new LocalCashDbHelper(this);

        btnVolverMapa.setOnClickListener(v -> finish());

        btnDescargarMapaOffline.setOnClickListener(
                v -> descargarMapaOffline());

        tvEstadoMapa.setText(
                "Comprobando disponibilidad del mapa offline...");

        // Comprobar si existe una región descargada al abrir
        // la pantalla, para actualizar correctamente el botón.
        actualizarEstadoOfflineAlAbrir();

        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapa -> {

            mapa.setStyle(ESTILO_MAPA, estilo -> {

                CameraPosition posicion =
                        new CameraPosition.Builder()
                                .target(SANTIAGO)
                                .zoom(12.0)
                                .build();

                mapa.setCameraPosition(posicion);

                cargarMovimientosEnMapa(mapa);
            });
        });
    }

    private void configurarClienteHTTP() {

        Dispatcher dispatcher = new Dispatcher();

        dispatcher.setMaxRequests(4);
        dispatcher.setMaxRequestsPerHost(4);

        OkHttpClient cliente = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .addInterceptor(chain -> {

                    Request request = chain.request()
                            .newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build();

                    return chain.proceed(request);
                })
                .build();

        new HttpRequestUtil().setOkHttpClient(cliente);
    }

    private boolean hayConexionInternet() {

        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        Network redActiva = manager.getActiveNetwork();

        if (redActiva == null) {
            return false;
        }

        NetworkCapabilities capacidades =
                manager.getNetworkCapabilities(redActiva);

        return capacidades != null
                && capacidades.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // Consultar el estado del mapa al abrir la pantalla.
    private void actualizarEstadoOfflineAlAbrir() {

        offlineManager.listOfflineRegions(
                new OfflineManager.ListOfflineRegionsCallback() {


                    public void onList(OfflineRegion[] regiones) {

                        OfflineRegion existente =
                                buscarRegionGuardada(regiones);

                        if (existente == null) {

                            runOnUiThread(() -> {

                                btnDescargarMapaOffline.setEnabled(true);

                                btnDescargarMapaOffline.setText(
                                        "Descargar mapa para usar sin Internet");

                                if (hayConexionInternet()) {
                                    tvEstadoMapa.setText(
                                            "El mapa offline aún no está descargado.");
                                } else {
                                    tvEstadoMapa.setText(
                                            "No hay mapa offline descargado. " +
                                                    "Conéctate a Internet para descargarlo.");
                                }
                            });

                            return;
                        }

                        existente.getStatus(
                                new OfflineRegion.OfflineRegionStatusCallback() {


                                    public void onStatus(
                                            OfflineRegionStatus status) {

                                        runOnUiThread(() -> {

                                            btnDescargarMapaOffline.setEnabled(true);

                                            if (status != null
                                                    && status.isComplete()) {

                                                btnDescargarMapaOffline.setText(
                                                        "Comprobar mapa offline");

                                                tvEstadoMapa.setText(
                                                        "Mapa offline disponible. " +
                                                                "Puedes desactivar Internet " +
                                                                "para probarlo.");

                                            } else if (hayConexionInternet()) {

                                                btnDescargarMapaOffline.setText(
                                                        "Continuar descarga offline");

                                                tvEstadoMapa.setText(
                                                        "La descarga está incompleta. " +
                                                                "Pulsa el botón para continuar.");

                                            } else {

                                                btnDescargarMapaOffline.setText(
                                                        "Reintentar descarga offline");

                                                tvEstadoMapa.setText(
                                                        "La descarga está incompleta. " +
                                                                "Conéctate a Internet para continuar.");
                                            }
                                        });
                                    }


                                    public void onError(String error) {

                                        runOnUiThread(() -> {
                                            btnDescargarMapaOffline.setEnabled(true);

                                            tvEstadoMapa.setText(
                                                    "No se pudo comprobar el mapa guardado.");
                                        });
                                    }
                                });
                    }


                    public void onError(String error) {

                        runOnUiThread(() -> {
                            btnDescargarMapaOffline.setEnabled(true);

                            tvEstadoMapa.setText(
                                    "No se pudo consultar el mapa offline.");
                        });
                    }
                });
    }

    private void descargarMapaOffline() {

        if (descargaEnCurso) {
            return;
        }

        btnDescargarMapaOffline.setEnabled(false);

        tvEstadoMapa.setText(
                "Comprobando si ya existe un mapa descargado...");

        offlineManager.listOfflineRegions(
                new OfflineManager.ListOfflineRegionsCallback() {


                    public void onList(OfflineRegion[] regiones) {

                        OfflineRegion existente =
                                buscarRegionGuardada(regiones);

                        if (existente != null) {
                            comprobarRegionExistente(existente);
                            return;
                        }

                        if (!hayConexionInternet()) {

                            runOnUiThread(() -> {
                                tvEstadoMapa.setText(
                                        "Necesitas Internet para descargar " +
                                                "el mapa por primera vez.");

                                btnDescargarMapaOffline.setText(
                                        "Descargar mapa para usar sin Internet");

                                btnDescargarMapaOffline.setEnabled(true);
                            });

                            return;
                        }

                        crearRegionOffline();
                    }


                    public void onError(String error) {

                        runOnUiThread(() -> {
                            tvEstadoMapa.setText(
                                    "No se pudieron consultar los mapas guardados. " +
                                            "Inténtalo nuevamente.");

                            btnDescargarMapaOffline.setEnabled(true);
                        });
                    }
                });
    }

    private OfflineRegion buscarRegionGuardada(
            OfflineRegion[] regiones) {

        if (regiones == null) {
            return null;
        }

        for (OfflineRegion region : regiones) {

            byte[] metadata = region.getMetadata();

            if (metadata == null) {
                continue;
            }

            String nombreRegion =
                    new String(metadata, StandardCharsets.UTF_8);

            if (REGION_METADATA.equals(nombreRegion)) {
                return region;
            }
        }

        return null;
    }

    private void comprobarRegionExistente(
            OfflineRegion region) {

        region.getStatus(
                new OfflineRegion.OfflineRegionStatusCallback() {


                    public void onStatus(OfflineRegionStatus status) {

                        if (status == null) {

                            runOnUiThread(() -> {
                                btnDescargarMapaOffline.setEnabled(true);

                                tvEstadoMapa.setText(
                                        "No se pudo comprobar el mapa guardado.");
                            });

                            return;
                        }

                        if (status.isComplete()) {

                            region.setDownloadState(
                                    OfflineRegion.STATE_INACTIVE);

                            runOnUiThread(() -> {
                                btnDescargarMapaOffline.setEnabled(true);

                                // CORRECCIÓN: actualizar también el texto.
                                btnDescargarMapaOffline.setText(
                                        "Comprobar mapa offline");

                                tvEstadoMapa.setText(
                                        "Mapa offline disponible. " +
                                                "Puedes desactivar Internet para probarlo.");
                            });

                        } else if (!hayConexionInternet()) {

                            region.setDownloadState(
                                    OfflineRegion.STATE_INACTIVE);

                            runOnUiThread(() -> {
                                btnDescargarMapaOffline.setEnabled(true);

                                btnDescargarMapaOffline.setText(
                                        "Reintentar descarga offline");

                                tvEstadoMapa.setText(
                                        "La descarga está incompleta. " +
                                                "Conéctate a Internet para continuar.");
                            });

                        } else {

                            iniciarDescargaRegion(region);
                        }
                    }


                    public void onError(String error) {

                        runOnUiThread(() -> {
                            btnDescargarMapaOffline.setEnabled(true);

                            tvEstadoMapa.setText(
                                    "No se pudo comprobar el estado del mapa.");
                        });
                    }
                });
    }

    private void crearRegionOffline() {

        // Zona reducida alrededor del centro de Santiago.
        // Niveles de zoom desde 10 hasta 14.
        LatLngBounds limites = new LatLngBounds.Builder()
                .include(new LatLng(-33.48, -70.71))
                .include(new LatLng(-33.42, -70.63))
                .build();

        OfflineTilePyramidRegionDefinition definicion =
                new OfflineTilePyramidRegionDefinition(
                        ESTILO_MAPA,
                        limites,
                        10.0,
                        14.0,
                        1.0f
                );

        byte[] metadata =
                REGION_METADATA.getBytes(StandardCharsets.UTF_8);

        tvEstadoMapa.setText(
                "Preparando la descarga del centro de Santiago...");

        offlineManager.createOfflineRegion(
                definicion,
                metadata,
                new OfflineManager.CreateOfflineRegionCallback() {


                    public void onCreate(OfflineRegion region) {
                        iniciarDescargaRegion(region);
                    }


                    public void onError(String error) {

                        runOnUiThread(() -> {
                            btnDescargarMapaOffline.setEnabled(true);

                            tvEstadoMapa.setText(
                                    "No se pudo preparar el mapa. " +
                                            "Revisa la conexión e inténtalo nuevamente.");
                        });
                    }
                });
    }

    private void iniciarDescargaRegion(OfflineRegion region) {

        if (!hayConexionInternet()) {

            runOnUiThread(() -> {
                btnDescargarMapaOffline.setEnabled(true);

                tvEstadoMapa.setText(
                        "Conéctate a Internet para descargar el mapa.");
            });

            return;
        }

        descargaEnCurso = true;
        limiteDescargaResuelto = false;

        btnDescargarMapaOffline.setEnabled(false);

        btnDescargarMapaOffline.setText(
                "Descargando mapa offline...");

        tvEstadoMapa.setText(
                "Descargando mapa offline. No cierres la aplicación.");

        region.setDeliverInactiveMessages(true);

        region.setObserver(
                new OfflineRegion.OfflineRegionObserver() {


                    public void onStatusChanged(
                            OfflineRegionStatus status) {

                        if (status == null || limiteDescargaResuelto) {
                            return;
                        }

                        long bytesDescargados =
                                status.getCompletedResourceSize();

                        if (bytesDescargados > MAX_BYTES) {

                            eliminarRegionPorLimite(
                                    region,
                                    "Se superó el límite de 50 MB. " +
                                            "Se canceló la descarga; utiliza una zona más pequeña."
                            );

                            return;
                        }

                        if (status.isComplete()) {

                            region.setDownloadState(
                                    OfflineRegion.STATE_INACTIVE);

                            descargaEnCurso = false;

                            runOnUiThread(() -> {
                                btnDescargarMapaOffline.setEnabled(true);

                                btnDescargarMapaOffline.setText(
                                        "Comprobar mapa offline");

                                tvEstadoMapa.setText(
                                        "¡Mapa offline descargado! " +
                                                "Puedes desactivar Internet para probarlo.");
                            });

                        } else {

                            long completados =
                                    status.getCompletedResourceCount();

                            long requeridos =
                                    status.getRequiredResourceCount();

                            runOnUiThread(() -> {

                                if (requeridos > 0) {

                                    tvEstadoMapa.setText(
                                            "Descargando mapa offline: " +
                                                    completados + " de " +
                                                    requeridos + " recursos.");

                                } else {

                                    tvEstadoMapa.setText(
                                            "Descargando mapa offline...");
                                }
                            });
                        }
                    }


                    public void onError(OfflineRegionError error) {

                        region.setDownloadState(
                                OfflineRegion.STATE_INACTIVE);

                        descargaEnCurso = false;

                        runOnUiThread(() -> {
                            btnDescargarMapaOffline.setEnabled(true);

                            btnDescargarMapaOffline.setText(
                                    "Reintentar descarga offline");

                            tvEstadoMapa.setText(
                                    "Hubo un problema durante la descarga. " +
                                            "Comprueba Internet e inténtalo de nuevo.");
                        });
                    }


                    public void mapboxTileCountLimitExceeded(
                            long limite) {

                        eliminarRegionPorLimite(
                                region,
                                "Se alcanzó el límite de teselas. " +
                                        "Reduce el área descargada e inténtalo otra vez."
                        );
                    }
                });

        region.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    private void eliminarRegionPorLimite(
            OfflineRegion region,
            String mensaje) {

        if (limiteDescargaResuelto) {
            return;
        }

        limiteDescargaResuelto = true;

        region.setDownloadState(OfflineRegion.STATE_INACTIVE);

        region.delete(new OfflineRegion.OfflineRegionDeleteCallback() {


            public void onDelete() {

                runOnUiThread(() -> {
                    descargaEnCurso = false;

                    btnDescargarMapaOffline.setEnabled(true);

                    btnDescargarMapaOffline.setText(
                            "Descargar mapa para usar sin Internet");

                    tvEstadoMapa.setText(mensaje);
                });
            }


            public void onError(String error) {

                runOnUiThread(() -> {
                    descargaEnCurso = false;

                    btnDescargarMapaOffline.setEnabled(true);

                    tvEstadoMapa.setText(
                            mensaje + " No se pudo eliminar " +
                                    "completamente la descarga parcial.");
                });
            }
        });
    }

    private void cargarMovimientosEnMapa(MapLibreMap mapa) {

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

                String titulo = (tipo.equals("INGRESO")
                        ? "Ingreso"
                        : "Gasto") + " · " + cuenta;

                String detalle = formatoMoneda.format(monto);

                if (!descripcion.trim().isEmpty()) {
                    detalle += "\n" + descripcion;
                }

                detalle += "\nFecha: " + fecha;

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

            mapa.setCameraPosition(
                    new CameraPosition.Builder()
                            .target(SANTIAGO)
                            .zoom(12.0)
                            .build()
            );

            tvResumenMovimientos.setText(
                    "No hay movimientos geolocalizados.");

        } else {

            mapa.setCameraPosition(
                    new CameraPosition.Builder()
                            .target(primeraUbicacion)
                            .zoom(cantidad == 1 ? 14.0 : 12.0)
                            .build()
            );

            tvResumenMovimientos.setText(
                    "Movimientos con ubicación: " + cantidad +
                            ". Toca un marcador para ver sus detalles.");
        }
    }


    protected void onStart() {
        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }


    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }


    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }


    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }

        super.onStop();
    }


    public void onLowMemory() {
        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
        }
    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }


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
