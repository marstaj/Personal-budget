package cz.avast.personalbudget.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper class handles database structure
 * Created by mastajner on 04/04/14.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

    /**
     * Name of the database table for Transactions
     */
    public static final String TABLE_TRANSACTIONS = "transactions";

    /**
     * Column names of the database table for Transactions.
     */
    public static final String COLUMN_GUID = "guid";
    public static final String COLUMN_VALUE = "value";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_KIND = "kind";
    public static final String COLUMN_DELETED = "deleted";
    public static final String COLUMN_PENDING = "pending";
    /**
     * SQL code for creating the table for Transactions
     */
    private static final String DATABASE_CREATE = "create table " + TABLE_TRANSACTIONS + "("
            + COLUMN_GUID + " text primary key not null, "
            + COLUMN_VALUE + " real not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_KIND + " text not null, "
            + COLUMN_DELETED + " integer not null, "
            + COLUMN_PENDING + " integer not null);";
    /**
     * Database name
     */
    private static final String DATABASE_NAME = "budget.db";
    /**
     * Database version number. This is used when structure of the database changes and it is necessary to adjust the data.
     */
    private static final int DATABASE_VERSION = 1;


    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // Creates tables in the database
        database.execSQL(DATABASE_CREATE);
    }

    /**
     * When structure of the database changes, this method is called to do all necessary adjustments to the data and the database. OldVersion number indicates how old is the database before changes so we know how much needs to be changed to meet the new version requirements.
     *
     * @param db         Database in which adjustments are to be made.
     * @param oldVersion Old version of the database.
     * @param newVersion New version of the database.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

        // For now, only drop the old table and create a new one.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }
}
