package cz.avast.personalbudget;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.bean_keeper.Proto;
import com.google.protobuf.InvalidProtocolBufferException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;

import java.text.DecimalFormat;
import java.util.List;

import cz.avast.personalbudget.entity.Transaction;
import de.timroes.android.listview.EnhancedListView;

/**
 * Fragment for displaying Budget transactions in list
 * Created by mastajner on 04/04/14.
 */
public class BudgetListFragment extends Fragment {

    /**
     * String constant indicating fragment type
     */
    public static final String TAG = BudgetListFragment.class.getSimpleName();

    /**
     * List view to display data
     */
    private EnhancedListView listBudget;

    /**
     * Listener which receives calls from this fragment. In this case parent activity
     */
    private BudgetListFragmentListener parentActivity;

    /**
     * List adapter for populating list rows with data
     */
    private BudgetListAdapter adapterBudget;

    /**
     * Http request for synchronizing data with backend server
     */
    private RequestHandle request;

    /**
     * TextView for displaying actual balance
     */
    private TextView textBalance;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Set fragment container
            this.parentActivity = (BudgetListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BudgetListFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get desired fragment's view to fill with data
        View rootView = inflater.inflate(R.layout.fragment_budget_list, container, false);

        // Get list of transactions for filling the list
        List<Transaction> transactions = DataModel.getActiveTransactions();

        // Init Views
        listBudget = (EnhancedListView) rootView.findViewById(R.id.listBudget);
        textBalance = (TextView) rootView.findViewById(R.id.textBalance);

        // Set up list adapter
        adapterBudget = new BudgetListAdapter(getActivity(), transactions);
        listBudget.setAdapter(adapterBudget);

        // Set up swipe-to-dismiss callback
        listBudget.setDismissCallback(new EnhancedListView.OnDismissCallback() {

            /**
             * Listener gets called when user swipes the transaction out
             *
             * @param listView The {@link EnhancedListView} the item has been deleted from.
             * @param position The position of the item to delete from your adapter.
             * @return EnhancedListView.Undoable object for undoing the swipe
             */
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {
                // Get the swiped transaction
                final Transaction item = adapterBudget.getItem(position);

                if (item != null) {
                    // Remove the transaction temporally and update list
                    DataModel.OnListRemove(item);
                    adapterBudget.notifyDataSetChanged();

                    // Display new balance
                    displayBalance(DataModel.getBalance());

                    // Return an Undoable for undoing the swipe
                    return new EnhancedListView.Undoable() {

                        /**
                         * Puts the removed item back to the list
                         */
                        @Override
                        public void undo() {
                            DataModel.OnListReinsert(position, item);

                            // update the list
                            adapterBudget.notifyDataSetChanged();

                            // Display new balance
                            displayBalance(DataModel.getBalance());
                        }

                        // Return a string for your item

                        /**
                         * Get the "Deleted" message
                         * @return Message
                         */
                        @Override
                        public String getTitle() {
                            return getString(R.string.toast_transaction_deleted);
                        }

                        // Delete item completely from your persistent storage

                        /**
                         * Delete transaction permanently, because user did not click undo.
                         */
                        @Override
                        public void discard() {
                            DataModel.setTransactionForDeletion(item);
                        }
                    };
                } else {
                    return null;
                }
            }
        });
        listBudget.setRequireTouchBeforeDismiss(false);
        // Set swipe to the right
        listBudget.setSwipeDirection(EnhancedListView.SwipeDirection.END);
        listBudget.enableSwipeToDismiss();

        // Set on transaction click callback
        listBudget.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Notifies the fragments container that user selected a transaction
                parentActivity.onTransactionClicked(adapterBudget.getItem(position));
            }
        });

        // Display new balance
        displayBalance(DataModel.getBalance());

        // If if user has no transactions in the application, run the synchronization
        if (adapterBudget.getTransactions().isEmpty()) {
            synchronizeWithBackend();
        }

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onStop() {
        // Remove the option to undo the swipe
        if (listBudget != null) {
            listBudget.discardUndo();
        }

        // Cancel the the request if running
        if (request != null && !request.isFinished()) {
            request.cancel(true);
        }

        super.onStop();
    }

    /**
     * Prepare for and run the sync with the backend server
     */
    public void synchronizeWithBackend() {
        // Remove the option to undo the swipe because we want all deleted transactions to be part of the sync
        if (listBudget != null) {
            listBudget.discardUndo();
        }

        // Generate data for the backend server
        Proto.AccountDelta accountDelta = DataModel.generateSendingData();

        // Create the http client
        AsyncHttpClient client = new AsyncHttpClient();

        // Allow the correct content type for the response
        String[] allowedContentTypes = new String[]{"application/octet-stream"};

        //  Run the POST request with data included
        request = client.post(getActivity(), getString(R.string.backend_url), new ByteArrayEntity(accountDelta.toByteArray()), "application/octet-stream", new BinaryHttpResponseHandler(allowedContentTypes) {

            /**
             * Gets called before request starts
             */
            @Override
            public void onStart() {
                Log.d(TAG, "BinaryHttpResponseHandler.onStart");
                // Turn on the progress
                parentActivity.onProgressOn();
                super.onStart();
            }

            /**
             * Gets called after the request successfully finished
             * @param fileData Response data
             */
            @Override
            public void onSuccess(byte[] fileData) {
                Log.d(TAG, "BinaryHttpResponseHandler.onSuccess");

                // Try to convert response data into usable objects
                Proto.AccountDelta accountDelta;
                try {
                    accountDelta = Proto.AccountDelta.parseFrom(fileData);
                } catch (InvalidProtocolBufferException e) {
                    Log.d(TAG, "Error while parsing AccoundDelta data");
                    e.printStackTrace();
                    return;
                }

                // Handle data that were sent to the server
                DataModel.onSyncSuccess();

                // Handle data that were receiver from the server
                DataModel.onReceivedData(accountDelta);

                // Update list and display the new budget
                adapterBudget.notifyDataSetChanged();
                displayBalance(DataModel.getBalance());

                // Inform user about successful sync
                Toast.makeText(getActivity(), R.string.toast_succesfull_changes, Toast.LENGTH_SHORT).show();
            }

            /**
             * Gets called when request fails
             * @param statusCode Http response status code
             * @param headers Http response headers
             * @param binaryData Http response content data
             * @param error Http response error
             */
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                Log.d(TAG, "BinaryHttpResponseHandler.onFailure");
                super.onFailure(statusCode, headers, binaryData, error);
                // Inform user about failed sync
                Toast.makeText(getActivity(), R.string.toast_error_synchronize, Toast.LENGTH_SHORT).show();
            }

            /**
             * Gets called after request finishes
             */
            @Override
            public void onFinish() {
                Log.d(TAG, "BinaryHttpResponseHandler.onFinish");
                super.onFinish();
                // Turn off the progress
                parentActivity.onProgressOff();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_budget_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ic_action_new: {
                // Inform container that user clicked on "New transaction"
                parentActivity.onNewTransactionClicked();
                break;
            }
            case R.id.ic_action_refresh: {
                // Run manual sync
                synchronizeWithBackend();
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    /**
     * Displays balance into the balance textview. The balance is rounded up to 2 decimal places for clarity
     *
     * @param balance Balance value
     */
    private void displayBalance(double balance) {
        DecimalFormat df2 = new DecimalFormat("###.##");
        textBalance.setText(df2.format(balance) + " " + getString(R.string.currency));
    }

    /**
     * Container Activity must implement this interface which methods are called by this fragment
     */
    public interface BudgetListFragmentListener {

        /**
         * Gets called when user selects a transaction from the list
         *
         * @param transaction Selected Transaction
         */
        public void onTransactionClicked(Transaction transaction);

        /**
         * Gets called when user clicks a "Add new transaction" button.
         */
        public void onNewTransactionClicked();

        /**
         * Gets called when progress circle is needed
         */
        public void onProgressOn();

        /**
         * Gets called when progress circle is no more needed
         */
        public void onProgressOff();
    }
}