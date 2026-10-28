package com.example.localcash;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalCashDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "localcash.db";
    private static final int DATABASE_VERSION = 1;

    public LocalCashDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }


    public void onCreate(SQLiteDatabase db) {

        // Tabla de cuentas financieras.
        db.execSQL(
                "CREATE TABLE cuentas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "tipo TEXT NOT NULL, " +
                        "saldo_inicial REAL NOT NULL DEFAULT 0, " +
                        "fecha_creacion TEXT NOT NULL" +
                        ")"
        );

        // Tabla de ingresos y egresos.
        db.execSQL(
                "CREATE TABLE movimientos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "cuenta_id INTEGER NOT NULL, " +
                        "tipo TEXT NOT NULL " +
                        "CHECK(tipo IN ('INGRESO', 'EGRESO')), " +
                        "monto REAL NOT NULL CHECK(monto > 0), " +
                        "descripcion TEXT, " +
                        "fecha TEXT NOT NULL, " +
                        "latitud REAL, " +
                        "longitud REAL, " +
                        "FOREIGN KEY(cuenta_id) REFERENCES cuentas(id) " +
                        "ON DELETE CASCADE" +
                        ")"
        );
    }


    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        // Aquí incorporaremos migraciones cuando evolucione
        // la estructura de la base de datos.
    }
}
