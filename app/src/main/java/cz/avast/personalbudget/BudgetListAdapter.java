package cz.avast.personalbudget;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import cz.avast.personalbudget.entity.Transaction;

/**
 * Adapter class for populating Budget list
 */
public class BudgetListAdapter extends ArrayAdapter<Transaction> {

    /**
     * Row view id
     */
    private static final int resource = R.layout.item_budget_list;
    /**
     * Color value for green stripe
     */
    private final int greenBackground;
    /**
     * Color value for red stripe
     */
    private final int redBackground;
    /**
     * Currency name (short form)
     */
    private final String currency;
    /**
     * List of transactions that are displayed in the list
     */
    private List<Transaction> transactions;
    /**
     * Inflater used to get the row view
     */
    private LayoutInflater mInflater;

    public BudgetListAdapter(Context context, List<Transaction> transactions) {
        super(context, resource, transactions);
        this.mInflater = LayoutInflater.from(context);
        this.transactions = transactions;
        Resources resources = context.getResources();
        this.greenBackground = resources.getColor(R.color.light_green);
        this.redBackground = resources.getColor(R.color.light_red);
        this.currency = context.getString(R.string.currency);
    }

    /**
     * Returns number of rows
     *
     * @return Number of rows
     */
    @Override
    public int getCount() {
        return transactions.size();
    }

    /**
     * Returns a view of a row filled with data
     *
     * @param position    Number of a row
     * @param convertView View of a row
     * @param parent
     * @return View of a row filled with data
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(resource, null);

            // Set up view holder for the row items
            ViewHolder holder = new ViewHolder();

            // Find views for the holder
            holder.textValue = (TextView) convertView.findViewById(R.id.textValue);
            holder.textDateTime = (TextView) convertView.findViewById(R.id.textDate);
            holder.textDescription = (TextView) convertView.findViewById(R.id.textDescription);
            holder.viewIndicator = convertView.findViewById(R.id.viewIndicator);

            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        // Get transaction for current row
        Transaction transaction = transactions.get(position);

        // Set data to the views
        holder.textValue.setText(transaction.getValue() + " " + currency);
        holder.textDescription.setText(String.valueOf(transaction.getKind()));

        // Set date and time
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(transaction.getDate());
        holder.textDateTime.setText(cal.get(Calendar.DAY_OF_MONTH) + ". " + (cal.get(Calendar.MONTH) + 1) + ". " + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));

        // Set color stripe that indicates whether the tansaction is + or -
        if (transaction.getValue() < 0) {
            holder.viewIndicator.setBackgroundColor(redBackground);
        } else {
            holder.viewIndicator.setBackgroundColor(greenBackground);
        }

        return convertView;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * View holder for holding views of the rows
     */
    static class ViewHolder {
        TextView textValue;
        TextView textDateTime;
        TextView textDescription;
        View viewIndicator;
    }
}
