package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Conectar el botón con la autenticación biométrica.
        findViewById(R.id.btnHuella).setOnClickListener(
                v -> iniciarAutenticacionBiometrica()
        );
    }

    private void iniciarAutenticacionBiometrica() {

        BiometricManager biometricManager =
                BiometricManager.from(this);

        int resultado = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        if (resultado != BiometricManager.BIOMETRIC_SUCCESS) {
            mostrarErrorBiometrico(resultado);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    // si reconoce huella
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {

                        super.onAuthenticationSucceeded(result);

                        Intent intent = new Intent(
                                MainActivity.this,
                                HomeActivity.class
                        );

                        startActivity(intent);
                        finish();
                    }

                    // si falla huella
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();

                        Toast.makeText(
                                MainActivity.this,
                                "La huella no coincide. Inténtalo de nuevo.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }


                    public void onAuthenticationError(
                            int errorCode,
                            CharSequence errString) {

                        super.onAuthenticationError(
                                errorCode, errString);

                        Toast.makeText(
                                MainActivity.this,
                                errString,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Acceso a LocalCash")
                        .setSubtitle(
                                "Confirma tu identidad para continuar")
                        .setDescription(
                                "Usa la huella registrada en este dispositivo.")
                        .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        .setNegativeButtonText("Cancelar")
                        .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void mostrarErrorBiometrico(int resultado) {

        String mensaje;

        switch (resultado) {

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                mensaje = "Este dispositivo no tiene sensor biométrico.";
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                mensaje = "El sensor biométrico no está disponible.";
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                mensaje = "No hay huellas registradas. Configura una "
                        + "huella en Ajustes del dispositivo.";
                break;

            default:
                mensaje = "La autenticación biométrica no está "
                        + "disponible en este momento.";
                break;
        }

        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_LONG
        ).show();
    }


}