package cz.avast.personalbudget;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import cz.avast.personalbudget.entity.Transaction;


/**
 * Activity is the fist to display
 */
public class MainActivity extends ActionBarActivity implements BudgetListFragment.BudgetListFragmentListener, TransactionDetailFragment.TransactionDetailFragmentListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set progress circle usage
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getSupportActionBar();

        // Set view
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            // Show budget list screen
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.container, new BudgetListFragment(), BudgetListFragment.TAG);
            fragmentTransaction.commit();
        }
    }

    /**
     * Callback registers that user clicked on a transaction in the budget list and displays transactions detail
     *
     * @param transaction Selected Transaction
     */
    @Override
    public void onTransactionClicked(Transaction transaction) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = new TransactionDetailFragment();
        // Put the transaction in arguments for the detail fragment
        Bundle arguments = new Bundle();
        arguments.putSerializable(TransactionDetailFragment.TRANSACTION, transaction);
        fragment.setArguments(arguments);
        // Shows transaction detail screen
        fragmentTransaction.replace(R.id.container, fragment, TransactionDetailFragment.TAG);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Callback registers that user clicked on "New transaction" and displays transaction detail screen with no supplied data
     */
    @Override
    public void onNewTransactionClicked() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, new TransactionDetailFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Callback for turning on the progress circle
     */
    @Override
    public void onProgressOn() {
        // There would be some kind of counting here if it was possible for more fragments request progress circle.
        setSupportProgressBarIndeterminateVisibility(true);
    }

    /**
     * Callback for turning off the progress circle
     */
    @Override
    public void onProgressOff() {
        // There would be some kind of counting here if it was possible for more fragments request progress circle.
        setSupportProgressBarIndeterminateVisibility(false);
    }

    /**
     * Callback for dismissing the detail fragment. Goes back in stack and calls sync if reqested
     *
     * @param needSyncWithBackend Whether to sync with backend
     */
    @Override
    public void onTransactionDetailExit(boolean needSyncWithBackend) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        // Pops last fragment in the stack
        fragmentManager.popBackStack();

        if (needSyncWithBackend) {
            // Set the listener so we know when the stack changed
            fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    // Remove the listener, check whether the active fragment is indeed Budget list and if so, cal sync with backend server
                    fragmentManager.removeOnBackStackChangedListener(this);
                    Fragment fragment = fragmentManager.findFragmentByTag(BudgetListFragment.TAG);
                    if (fragment.isVisible()) {
                        ((BudgetListFragment) fragment).synchronizeWithBackend();
                    }
                }
            });
        }
    }
}
