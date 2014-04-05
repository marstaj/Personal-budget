package cz.avast.personalbudget.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cz.avast.personalbudget.entity.Transaction;

/**
 * Class for easier manipulation with the database
 * Created by mastajner on 04/04/14.
 */
public class BudgetDataSource {

    /**
     * Instance of a database.
     */
    private SQLiteDatabase database;

    /**
     * Instance of a database helper which provides access to the database and tables.
     */
    private MySQLiteHelper dbHelper;

    /**
     * BudgetDataSource constructor
     *
     * @param context Context
     */
    public BudgetDataSource(Context context) {
        // Get database instance
        dbHelper = new MySQLiteHelper(context);
    }

    /**
     * Opens the database for writing.
     *
     * @throws SQLException The database cannot be opened for writing
     */
    public void open() throws SQLException {
        Log.i(BudgetDataSource.class.getName(), "Opening DB");
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Closes the database after writing.
     */
    public void close() {
        Log.i(BudgetDataSource.class.getName(), "Closing DB");
        dbHelper.close();
    }

    /**
     * Saves transaction into the database.
     *
     * @param transaction Transaction
     */
    public synchronized void saveTranstaction(Transaction transaction) {
        database.beginTransaction();

        // Prepare data for saving
        ContentValues values = transactionToContentValues(transaction);

        // Insert into the database
        database.insertWithOnConflict(MySQLiteHelper.TABLE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Saves List of transactions to the database
     *
     * @param transactions List of transactions
     */
    public synchronized void saveTranstactions(List<Transaction> transactions) {
        database.beginTransaction();

        for (Transaction transaction : transactions) {

            // Prepare data for saving
            ContentValues values = transactionToContentValues(transaction);

            // Insert into the database
            database.insertWithOnConflict(MySQLiteHelper.TABLE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Removes transaction from database.
     *
     * @param transaction Transaction
     */
    public synchronized void deleteTransaction(Transaction transaction) {
        database.beginTransaction();

        // Removes from database
        database.delete(MySQLiteHelper.TABLE_TRANSACTIONS, MySQLiteHelper.COLUMN_GUID + " = \"" + transaction.getGuid() + "\"", null);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Removes list od transactions from database.
     *
     * @param transactions List of transactions
     */
    public synchronized void deleteTransactions(List<Transaction> transactions) {
        database.beginTransaction();

        for (Transaction transaction : transactions) {
            // Removes from database
            database.delete(MySQLiteHelper.TABLE_TRANSACTIONS, MySQLiteHelper.COLUMN_GUID + " = \"" + transaction.getGuid() + "\"", null);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Selects all transactions from the database.
     *
     * @return List of transactions
     */
    public synchronized List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<Transaction>();
        database.beginTransaction();

        // Get cursor which points to table rows
        Cursor cursor = database.query(MySQLiteHelper.TABLE_TRANSACTIONS, null, null, null, null, null, MySQLiteHelper.COLUMN_DATE + " DESC");
        cursor.moveToFirst();

        // Iterate through the rows
        while (!cursor.isAfterLast()) {
            // Costruct the Transaction object and add it to the result
            result.add(cursorToTransaction(cursor));
            cursor.moveToNext();
        }

        cursor.close();

        database.setTransactionSuccessful();
        database.endTransaction();
        return result;
    }

    /**
     * Selects from the database all transactions, that are not marked as deleted.
     *
     * @return List of transactions
     */
    public synchronized List<Transaction> loadNotDeletedTransactions() {
        database.beginTransaction();
        List<Transaction> result = new ArrayList<Transaction>();

        // Get cursor which points to table rows
        Cursor cursor = database.query(MySQLiteHelper.TABLE_TRANSACTIONS, null, MySQLiteHelper.COLUMN_DELETED + " = 0 ", null, null, null, MySQLiteHelper.COLUMN_DATE + " DESC");
        cursor.moveToFirst();

        // Iterate through the rows
        while (!cursor.isAfterLast()) {
            // Costruct the Transaction object and add it to the result
            result.add(cursorToTransaction(cursor));
            cursor.moveToNext();
        }

        cursor.close();

        database.setTransactionSuccessful();
        database.endTransaction();
        return result;
    }

    /**
     * Selects from the database all transactions, that are marked as pending for sync.
     *
     * @return List of transactions
     */
    public synchronized List<Transaction> loadPendingTransactions() {
        database.beginTransaction();
        List<Transaction> result = new ArrayList<Transaction>();

        // Get cursor which points to table rows
        Cursor cursor = database.query(MySQLiteHelper.TABLE_TRANSACTIONS, null, MySQLiteHelper.COLUMN_PENDING + " = 1 ", null, null, null, MySQLiteHelper.COLUMN_DATE + " DESC");
        cursor.moveToFirst();

        // Iterate through the rows
        while (!cursor.isAfterLast()) {
            // Costruct the Transaction object and add it to the result
            result.add(cursorToTransaction(cursor));
            cursor.moveToNext();
        }

        cursor.close();

        database.setTransactionSuccessful();
        database.endTransaction();
        return result;
    }

    /**
     * Creates ContentValues object from the transaction. The ContentValues object is saved into the database.
     *
     * @param transaction Transaction
     * @return ContentValues object
     */
    private ContentValues transactionToContentValues(Transaction transaction) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_GUID, transaction.getGuid());
        values.put(MySQLiteHelper.COLUMN_VALUE, transaction.getValue());
        values.put(MySQLiteHelper.COLUMN_DATE, transaction.getDate());
        values.put(MySQLiteHelper.COLUMN_KIND, transaction.getKind());
        values.put(MySQLiteHelper.COLUMN_DELETED, transaction.isDeleted() ? 1 : 0);
        values.put(MySQLiteHelper.COLUMN_PENDING, transaction.isPending() ? 1 : 0);
        return values;
    }

    /**
     * Creates a Transaction object from cursor pointer, which points to a table row.
     *
     * @param cursor Pointer
     * @return Transaction
     */
    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction transaction = new Transaction();
        transaction.setGuid(cursor.getString(0));
        transaction.setValue(cursor.getDouble(1));
        transaction.setDate(cursor.getLong(2));
        transaction.setKind(cursor.getString(3));
        transaction.setDeleted(cursor.getInt(4) == 1);
        transaction.setPending(cursor.getInt(5) == 1);
        return transaction;
    }
}
