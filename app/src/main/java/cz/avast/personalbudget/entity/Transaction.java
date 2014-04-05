package cz.avast.personalbudget.entity;

import java.io.Serializable;

/**
 * Created by mastajner on 02/04/14.
 * <p/>
 * Entity for holding Transaction data
 */
public class Transaction implements Serializable {

    /**
     * Transaction GUID
     */
    private String guid;

    /**
     * Payment value
     */
    private double value;

    /**
     * Date and time of a transaction in the form of timestamp.
     */
    private long date;

    /**
     * Description of the transaction
     */
    private String kind;

    /**
     * Whether was this transaction deleted or not.
     */
    private boolean deleted;

    /**
     * Whether is this transaction waiting for sync with backend
     */
    private boolean pending;

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Compares this transaction with the one supplied in the arguments. Transaction is considered the same if GUIDs match.
     *
     * @param object Comparing object (Should be Transaction.class)
     * @return True if transactions have the same GUID.
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof Transaction) {
            if (guid.equals(((Transaction) object).getGuid())) {
                return true;
            }
        }
        return super.equals(object);
    }
}
