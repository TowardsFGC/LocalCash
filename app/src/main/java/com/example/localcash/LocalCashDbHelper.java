
package com.example.localcash;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalCashDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "localcash.db";
    private static final int DATABASE_VERSION = 3;

    public LocalCashDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE cuentas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "tipo TEXT NOT NULL, " +
                        "saldo_inicial REAL NOT NULL DEFAULT 0, " +
                        "fecha_creacion TEXT NOT NULL" +
                        ")"
        );

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

        crearTablaComprasActual(db);
        crearIndiceCompras(db);
    }

    private void crearTablaComprasActual(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE compras (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "monto REAL NOT NULL CHECK(monto > 0), " +
                        "fecha_compra TEXT NOT NULL, " +
                        "estado TEXT NOT NULL " +
                        "CHECK(estado IN ('REALIZADA', 'PLANIFICADA')), " +
                        "fecha_registro TEXT NOT NULL, " +
                        "cuenta_id INTEGER, " +
                        "movimiento_id INTEGER, " +
                        "FOREIGN KEY(cuenta_id) REFERENCES cuentas(id) " +
                        "ON DELETE SET NULL, " +
                        "FOREIGN KEY(movimiento_id) REFERENCES movimientos(id) " +
                        "ON DELETE SET NULL" +
                        ")"
        );
    }

    private void crearTablaComprasVersion2(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE compras (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "monto REAL NOT NULL CHECK(monto > 0), " +
                        "fecha_compra TEXT NOT NULL, " +
                        "estado TEXT NOT NULL " +
                        "CHECK(estado IN ('REALIZADA', 'PLANIFICADA')), " +
                        "fecha_registro TEXT NOT NULL" +
                        ")"
        );
    }

    private void crearIndiceCompras(SQLiteDatabase db) {

        db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "idx_compras_movimiento_id " +
                        "ON compras(movimiento_id) " +
                        "WHERE movimiento_id IS NOT NULL"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 2) {
            crearTablaComprasVersion2(db);
        }

        if (oldVersion < 3) {

            db.execSQL(
                    "ALTER TABLE compras ADD COLUMN " +
                            "cuenta_id INTEGER REFERENCES cuentas(id) " +
                            "ON DELETE SET NULL"
            );

            db.execSQL(
                    "ALTER TABLE compras ADD COLUMN " +
                            "movimiento_id INTEGER REFERENCES movimientos(id) " +
                            "ON DELETE SET NULL"
            );

            crearIndiceCompras(db);
        }
    }
}
