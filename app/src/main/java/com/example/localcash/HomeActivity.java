
package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                });

        // Obtener los botones del menú principal.
        Button btnCuentas =
                findViewById(R.id.btnCuentas);

        Button btnCompras =
                findViewById(R.id.btnCompras);

        Button btnGastosCompartidos =
                findViewById(R.id.btnGastosCompartidos);

        Button btnContactos =
                findViewById(R.id.btnContactos);

        Button btnMapa =
                findViewById(R.id.btnMapa);

        // Abrir el módulo de cuentas.
        btnCuentas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    CuentasActivity.class
            );

            startActivity(intent);
        });

        // Abrir el módulo de compras.
        btnCompras.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ComprasActivity.class
            );

            startActivity(intent);
        });

        // Abrir los gastos compartidos.
        btnGastosCompartidos.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    GastosCompartidosActivity.class
            );

            startActivity(intent);
        });

        // Abrir la agenda de contactos.
        btnContactos.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ContactosActivity.class
            );

            startActivity(intent);
        });

        // Abrir el mapa de movimientos.
        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    MapaActivity.class
            );

            startActivity(intent);
        });
    }
}
