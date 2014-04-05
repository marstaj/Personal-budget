package cz.avast.personalbudget;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import cz.avast.personalbudget.entity.Transaction;

/**
 * Created by mastajner on 02/04/14.
 */
public class TransactionDetailFragment extends Fragment {


    /**
     * String constant indicating fragment type
     */
    public static final String TAG = TransactionDetailFragment.class.getSimpleName();

    /**
     * Constant for getting transaction data through arguments
     */
    public static final String TRANSACTION = "TRANSACTION";

    /**
     * Constant for getting color value through screen rotation
     */
    private static final String TEXT_VALUE_COLOR = "TEXT_VALUE_COLOR";

    /**
     * Constant for getting calendar data through screen rotation
     */
    private static final String CALENDAR = "CALENDAR";

    /**
     * Listener which receives calls from this fragment. In this case parent activity
     */
    private TransactionDetailFragmentListener parentActivity;

    /**
     * Views for displaying data on the screen
     */
    private EditText editValue;
    private EditText editKind;
    private EditText editDate;
    private EditText editTime;
    private RadioButton radioIn;
    private RadioButton radioOut;
    private Transaction oldTransaction;
    private TextView textValue;

    /**
     * Calendar for holdnig time information
     */
    private Calendar calendar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Set fragment container
            this.parentActivity = (TransactionDetailFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TransactionDetailFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get desired fragment's view to fill with data
        View rootView = inflater.inflate(R.layout.fragment_transaction_detail, container, false);

        // Init Views
        editValue = (EditText) rootView.findViewById(R.id.editValue);
        editKind = (EditText) rootView.findViewById(R.id.editKind);
        editDate = (EditText) rootView.findViewById(R.id.editDate);
        editTime = (EditText) rootView.findViewById(R.id.editTime);
        radioIn = (RadioButton) rootView.findViewById(R.id.radioIn);
        radioOut = (RadioButton) rootView.findViewById(R.id.radioOut);
        textValue = (TextView) rootView.findViewById(R.id.textValue);

        // Set click listener to date field
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create and display Date picker dialog for easy date input
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        // Modify calendar data accordingly and display in the form
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        setDateToEditText(editDate, calendar);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        // Set click listener to time field
        editTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create and display Time picker dialog for easy time input
                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        // Modify calendar data accordingly and display in the form
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        setTimeToEditText(editTime, calendar);
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(getActivity()));
                dialog.show();
            }
        });

        // Get fragment arguments and check if it contains Transaction
        Bundle arguments = getArguments();
        if (arguments != null) {
            oldTransaction = (Transaction) arguments.getSerializable(TRANSACTION);
        }

        // If rotation happened, only set back data
        if (savedInstanceState != null) {

            calendar = (Calendar) savedInstanceState.getSerializable(CALENDAR);
            textValue.setTextColor(savedInstanceState.getInt(TEXT_VALUE_COLOR));

        } else {
            // If transaction was pulled out of arguments, set up screen with data from it.
            if (oldTransaction != null) {
                fillData(oldTransaction);
            } else {
                // Set up empty screen
                calendar = Calendar.getInstance();

                // Set actual date and time to the date and time forms
                setDateToEditText(editDate, calendar);
                setTimeToEditText(editTime, calendar);
            }
        }

        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Sets date information to the date edittext
     *
     * @param editText edittext to set the data to
     * @param cal      Calendar
     */
    private void setDateToEditText(EditText editText, Calendar cal) {
        editText.setText(cal.get(Calendar.DAY_OF_MONTH) + ". " + (cal.get(Calendar.MONTH) + 1) + ". " + cal.get(Calendar.YEAR));
    }

    /**
     * Sets time information to the time edittext
     *
     * @param editText edittext to set the time to
     * @param cal      Calendar
     */
    private void setTimeToEditText(EditText editText, Calendar cal) {
        editText.setText(cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
    }

    /**
     * Fills data from transaction to the screen views
     *
     * @param transaction Transaction
     */
    private void fillData(Transaction transaction) {
        double value = transaction.getValue();
        // Check the right radio button
        if (value < 0) {
            value = value * -1;
            radioIn.setChecked(false);
            radioOut.setChecked(true);
        } else {
            radioIn.setChecked(true);
            radioOut.setChecked(false);
        }
        editValue.setText(String.valueOf(value));
        editKind.setText(transaction.getKind());

        // Set date and time to the date and time forms
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.getDate());
        setDateToEditText(editDate, calendar);
        setTimeToEditText(editTime, calendar);
    }

    /**
     * Validate form and if valid, create new Transaction or alter the old one.
     */
    private void processForm() {
        if (validateData()) {
            // Create new transaction from screen data
            Transaction newTransaction = new Transaction();
            newTransaction.setKind(editKind.getText().toString());
            newTransaction.setDate(calendar.getTimeInMillis());
            if (radioIn.isChecked()) {
                newTransaction.setValue(Double.valueOf(editValue.getText().toString()));
            } else {
                newTransaction.setValue(Double.valueOf(editValue.getText().toString()) * -1);
            }

            if (oldTransaction == null) {
                // Cerating new Transaction
                DataModel.saveNewlyCreatedTransaction(newTransaction);
                parentActivity.onTransactionDetailExit(true);
            } else {

                // Compare screen data with the transaction supplied by the arguments. Id they match, leave with no action. Else alter the data
                if (isDataDifferent(oldTransaction, newTransaction)) {
                    DataModel.saveAlteredTransaction(oldTransaction, newTransaction);
                    parentActivity.onTransactionDetailExit(true);
                } else {
                    parentActivity.onTransactionDetailExit(false);
                }
            }
        } else {
            // Inform user about error in the form
            Toast.makeText(getActivity(), R.string.toast_form_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Compares two transactions if they differ at least in one field
     *
     * @param tr1 Transaction 1
     * @param tr2 Transaction 2
     * @return Whether they are the same or not
     */
    private boolean isDataDifferent(Transaction tr1, Transaction tr2) {
        if (tr1.getValue() != tr2.getValue()) {
            return true;
        }
        if (tr1.getDate() != tr2.getDate()) {
            return true;
        }
        return !tr1.getKind().equals(tr2.getKind());
    }

    /**
     * Validate form data
     *
     * @return Whether the form is valid or not
     */
    private boolean validateData() {
        boolean ok = true;

        String formValue = editValue.getText().toString();
        if (formValue.equals("") || Double.valueOf(formValue) <= 0) {
            // Set color to red indicating error
            textValue.setTextColor(getResources().getColor(R.color.red));
            ok = false;
        } else {
            textValue.setTextColor(getResources().getColor(R.color.black));
        }

        return ok;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_transaction_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cancel: {
                // Inform container that this screen is done
                parentActivity.onTransactionDetailExit(false);
                break;
            }
            case R.id.action_confirm: {
                // Process data from the screen
                processForm();
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save some data through screen orientation
        outState.putSerializable(CALENDAR, calendar);
        outState.putInt(TEXT_VALUE_COLOR, textValue.getCurrentTextColor());
        super.onSaveInstanceState(outState);
    }

    /**
     * Container Activity must implement this interface which methods are called by this fragment
     */
    public interface TransactionDetailFragmentListener {
        /**
         * Gets called when user saves or cancels the screen
         *
         * @param needSyncWithBackend Whether to run automatic sync after
         */
        public void onTransactionDetailExit(boolean needSyncWithBackend);
    }
}