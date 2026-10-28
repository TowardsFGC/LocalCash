
package com.example.localcash;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalCashDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "localcash.db";
    private static final int DATABASE_VERSION = 5;

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
        crearTablaContactos(db);
        crearTablasGastosCompartidos(db);
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

    private void crearTablaContactos(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS contactos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "telefono TEXT, " +
                        "correo TEXT, " +
                        "fecha_registro TEXT NOT NULL" +
                        ")"
        );
    }

    private void crearTablasGastosCompartidos(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS gastos_compartidos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "monto_total REAL NOT NULL CHECK(monto_total > 0), " +
                        "fecha TEXT NOT NULL, " +
                        "cuenta_id INTEGER NOT NULL, " +
                        "movimiento_id INTEGER, " +
                        "fecha_registro TEXT NOT NULL, " +
                        "FOREIGN KEY(cuenta_id) REFERENCES cuentas(id), " +
                        "FOREIGN KEY(movimiento_id) REFERENCES movimientos(id) " +
                        "ON DELETE SET NULL" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS deudas_compartidas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "gasto_id INTEGER NOT NULL, " +
                        "contacto_id INTEGER NOT NULL, " +
                        "monto_deuda REAL NOT NULL CHECK(monto_deuda > 0), " +
                        "estado TEXT NOT NULL DEFAULT 'PENDIENTE' " +
                        "CHECK(estado IN ('PENDIENTE', 'PAGADA')), " +
                        "FOREIGN KEY(gasto_id) " +
                        "REFERENCES gastos_compartidos(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY(contacto_id) REFERENCES contactos(id), " +
                        "UNIQUE(gasto_id, contacto_id)" +
                        ")"
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

        if (oldVersion < 4) {
            crearTablaContactos(db);
        }

        if (oldVersion < 5) {
            crearTablasGastosCompartidos(db);
        }
    }
}
