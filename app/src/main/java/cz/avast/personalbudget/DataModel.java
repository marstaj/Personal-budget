package cz.avast.personalbudget;

import android.content.Context;
import android.util.Log;

import com.bean_keeper.Proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import cz.avast.personalbudget.db.BudgetDataSource;
import cz.avast.personalbudget.entity.Transaction;

/**
 * Fragment for displaying Budget transactions in list
 * Created by mastajner on 04/04/14.
 */
public class DataModel {

    /**
     * String constant indicating class type
     */
    public static final String TAG = DataModel.class.getSimpleName();
    /**
     * Comparator for comparing transactions. Transactions are sorted according their date and time
     */
    private static final Comparator<? super Transaction> transactionComparator = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction transaction, Transaction transaction2) {
            if (transaction.getDate() == transaction2.getDate()) {
                return 0;
            } else {
                // If transaction is older then transaction2
                if (transaction.getDate() > transaction2.getDate()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    };
    /**
     * Application context
     */
    private static Context context;
    /**
     * Stored balance
     */
    private static double balance;
    /**
     * Stored list of active transactions. Transaction displayed in the budget list.
     */
    private static List<Transaction> activeTransactions;
    /**
     * Stored list of pending transactions. New transactions or old that were altered and deleted
     */
    private static List<Transaction> pendingTransactions;
    /**
     * Instance of a persistent data source.
     */
    private static BudgetDataSource dataSource;

    /**
     * Initialization method. This needs to be called from within the application class before any other method from this class is called.
     *
     * @param applicationContext Application context
     */
    public static void init(Context applicationContext) {
        context = applicationContext;

        // Get instance of persisted data source
        dataSource = new BudgetDataSource(context);

        // Load up required data transactions
        dataSource.open();
        activeTransactions = dataSource.loadNotDeletedTransactions();
        pendingTransactions = dataSource.loadPendingTransactions();
        dataSource.close();

        // Calculate balance
        balance = 0;
        for (Transaction transaction : activeTransactions) {
            balance += transaction.getValue();
        }
    }

    /**
     * Get acutal balance
     *
     * @return balance value
     */
    public static double getBalance() {
        return balance;
    }

    /**
     * Get list of displayed transactions
     *
     * @return List of transactions
     */
    public static List<Transaction> getActiveTransactions() {
        return activeTransactions;
    }

    /**
     * Get list of pending transactions
     *
     * @return List of transactions
     */
    public static List<Transaction> getPendingTransactions() {
        return pendingTransactions;
    }

    /**
     * Mark transaction as deleted, add it to the pending list and remove it from displayed list
     *
     * @param transaction Transaction deleted by user
     */
    public static void setTransactionForDeletion(Transaction transaction) {
        transaction.setDeleted(true);
        transaction.setPending(true);

        pendingTransactions.add(transaction);

        dataSource.open();
        dataSource.saveTranstaction(transaction);
        dataSource.close();
    }

    /**
     * Generate sync data that are going to be sent to the backend server
     *
     * @return Proto.AccountDelta sync data
     */
    public static Proto.AccountDelta generateSendingData() {
        long serverTimestamp = PrefManager.getServerTimestamp(context);

        Proto.AccountDelta.Builder accountDeltaBuilder = Proto.AccountDelta.newBuilder();
        // Set old server sycnc timestamp
        accountDeltaBuilder.setServerTimestamp(serverTimestamp);

        // Set transactions data
        for (Transaction transaction : pendingTransactions) {
            Proto.Transaction.Builder tr = Proto.Transaction.newBuilder();
            tr.setGuid(transaction.getGuid());
            tr.setValue(transaction.getValue());
            tr.setKind(transaction.getKind());
            tr.setDate(transaction.getDate());
            tr.setDeleted(transaction.isDeleted());
            accountDeltaBuilder.addAddedOrModified(tr);
        }
        Proto.AccountDelta accountDelta = accountDeltaBuilder.build();

        Log.d(TAG, "Number of sent transactions:             " + accountDelta.getAddedOrModifiedList().size());
        Log.d(TAG, "Sent server timestamp:     " + accountDelta.getServerTimestamp());
        return accountDelta;
    }

    /**
     * Data was successfully sent to the server and it is necessary to handle them in the device. Deleted transactions by the user can now be safely deleted from the database and new or modified transactions can be removed from pending list.
     */
    public static void onSyncSuccess() {
        List<Transaction> deletedTransactions = new ArrayList<Transaction>();
        List<Transaction> modifiedOrNewTransactions = new ArrayList<Transaction>();

        for (Transaction transaction : pendingTransactions) {
            if (transaction.isDeleted()) {
                deletedTransactions.add(transaction);
            } else {
                transaction.setPending(false);
                modifiedOrNewTransactions.add(transaction);
            }
        }
        // Clear the pending list, all the data was synced
        pendingTransactions.clear();

        // Save the information to the database and delete deleted transactions
        dataSource.open();
        dataSource.saveTranstactions(modifiedOrNewTransactions);
        dataSource.deleteTransactions(deletedTransactions);
        dataSource.close();
    }

    /**
     * Data was successfully received from the server and it is necessary to handle them in the device. Deleted transactions have to be deleted from the database and removed from displayed list and altered or new transactions have to be added or updated.
     *
     * @param accountDelta Server data
     */
    public static void onReceivedData(Proto.AccountDelta accountDelta) {
        List<Transaction> transactionsToInsertOrUpdate = new ArrayList<Transaction>(accountDelta.getAddedOrModifiedCount());
        List<Transaction> transactionsToDelete = new ArrayList<Transaction>(accountDelta.getAddedOrModifiedCount());

        // Save new server sync timestamp
        PrefManager.setServerTimestamp(context, accountDelta.getServerTimestamp());

        for (Proto.Transaction prTransaction : accountDelta.getAddedOrModifiedList()) {
            // Create new transaction from server data and decide what to do with it
            Transaction transaction = new Transaction();
            transaction.setGuid(prTransaction.getGuid());
            transaction.setValue(prTransaction.getValue());
            transaction.setDate(prTransaction.getDate());
            transaction.setKind(prTransaction.getKind());
            transaction.setDeleted(prTransaction.getDeleted());
            transaction.setPending(false);

            if (transaction.isDeleted()) {
                transactionsToDelete.add(transaction);

                // If this there is displayed transaction with the same GUID, we have to remove it and adjust the balance.
                if (activeTransactions.contains(transaction)) {
                    int oldIndex = activeTransactions.indexOf(transaction);
                    Transaction old = activeTransactions.get(oldIndex);
                    activeTransactions.remove(oldIndex);
                    balance -= old.getValue();
                }

            } else {
                transactionsToInsertOrUpdate.add(transaction);

                // If this there is displayed transaction with the same GUID, we have to alter it and adjust the balance.
                if (activeTransactions.contains(transaction)) {
                    int oldIndex = activeTransactions.indexOf(transaction);
                    Transaction old = activeTransactions.get(oldIndex);
                    activeTransactions.remove(oldIndex);
                    balance -= old.getValue();
                    balance += transaction.getValue();
                    activeTransactions.add(transaction);

                } else {
                    // If this there is displayed transaction with the same GUID, we have to add it and adjust the balance.
                    activeTransactions.add(transaction);
                    balance += transaction.getValue();
                }
            }
        }

        // Sort the displayed data by datetime
        Collections.sort(activeTransactions, transactionComparator);

        // Save new information into the database and delete deleted transactions
        dataSource.open();
        dataSource.saveTranstactions(transactionsToInsertOrUpdate);
        dataSource.deleteTransactions(transactionsToDelete);
        dataSource.close();

        Log.d(TAG, "Received server timestamp: " + accountDelta.getServerTimestamp());
        Log.d(TAG, "Number of received transactions:         " + accountDelta.getAddedOrModifiedList().size());
        Log.d(TAG, "Number of transactions to delete:        " + transactionsToDelete.size());
        Log.d(TAG, "Number of transactions to add or update: " + transactionsToInsertOrUpdate.size());
    }

    /**
     * Save newly created transaction persistently into the database and update it in the budget list
     *
     * @param transaction Transaction
     */
    public static void saveNewlyCreatedTransaction(Transaction transaction) {
        // Generate GUID and set flags
        transaction.setGuid(UUID.randomUUID().toString());
        transaction.setDeleted(false);
        transaction.setPending(true);

        // Adjust balance
        balance += transaction.getValue();

        // Display and add to pending list for sync
        pendingTransactions.add(transaction);
        activeTransactions.add(transaction);

        // Sort the displayed data by datetime
        Collections.sort(activeTransactions, transactionComparator);


        // Save the information to the database
        dataSource.open();
        dataSource.saveTranstaction(transaction);
        dataSource.close();
    }

    /**
     * Save altered transaction persistently into the database and update it in the budget list. Method creates new transaction and replaces the old one.
     *
     * @param oldTransaction Transaction
     * @param newTransaction Transaction
     */
    public static void saveAlteredTransaction(Transaction oldTransaction, Transaction newTransaction) {
        // Remove old transaction and adjust the balance
        activeTransactions.remove(oldTransaction);
        balance -= oldTransaction.getValue();

        // set additional data to the new transaction
        newTransaction.setGuid(oldTransaction.getGuid());
        newTransaction.setDeleted(false);
        newTransaction.setPending(true);

        // Adjust the balance
        balance += newTransaction.getValue();

        // Display and add to pending list for sync
        pendingTransactions.add(newTransaction);
        activeTransactions.add(newTransaction);

        // Sort the displayed data by datetime
        Collections.sort(activeTransactions, transactionComparator);

        // Save the information to the database
        dataSource.open();
        dataSource.saveTranstaction(oldTransaction);
        dataSource.close();
    }

    /**
     * Removes transaction from the budget list and adjusts balance
     *
     * @param transaction Transaction
     */
    public static void OnListRemove(Transaction transaction) {
        balance -= transaction.getValue();
        activeTransactions.remove(transaction);
    }

    /**
     * Puts transactions to the budget list at specific index and ajusts balance
     *
     * @param position    Index for the insertion
     * @param transaction Transaction
     */
    public static void OnListReinsert(int position, Transaction transaction) {
        balance += transaction.getValue();
        activeTransactions.add(position, transaction);
    }
}