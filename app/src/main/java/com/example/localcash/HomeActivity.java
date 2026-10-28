package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

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

        Button btnCuentas = findViewById(R.id.btnCuentas);
        Button btnCompras = findViewById(R.id.btnCompras);

        Button btnGastosCompartidos =
                findViewById(R.id.btnGastosCompartidos);

        Button btnContactos = findViewById(R.id.btnContactos);

        btnCuentas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    CuentasActivity.class
            );

            startActivity(intent);
        });

        btnCompras.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ComprasActivity.class
            );

            startActivity(intent);
        });

        btnGastosCompartidos.setOnClickListener(v ->
                mostrarMensaje("Gastos compartidos y deudas"));

        btnContactos.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ContactosActivity.class
            );

            startActivity(intent);
        });
    }

    private void mostrarMensaje(String modulo) {
        Toast.makeText(
                this,
                modulo + ": módulo en desarrollo",
                Toast.LENGTH_SHORT
        ).show();
    }
}
