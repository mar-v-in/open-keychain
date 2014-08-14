package org.sufficientlysecure.keychain.provider;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

public class KeyRingInfoEntry implements Parcelable {

    public static final Creator<KeyRingInfoEntry> CREATOR = new Creator<KeyRingInfoEntry>() {
        @Override
        public KeyRingInfoEntry createFromParcel(Parcel source) {
            return new KeyRingInfoEntry(source.readByte() == 1, source.readInt(), source.readString(),
                    source.readLong(), source.readLong());
        }

        @Override
        public KeyRingInfoEntry[] newArray(int size) {
            return new KeyRingInfoEntry[size];
        }
    };

    /**
     * Default for old keyrings or if not specified
     */
    public static final int REASON_UNKNOWN = 0;

    /**
     * Downloaded in background using a contacts email address
     */
    public static final int REASON_AUTOMATIC = 1;

    /**
     * Imported from a file (via sdcard or email), from clipboard or from a keyserver using the add key dialog
     */
    public static final int REASON_MANUALLY = 2;

    /**
     * Imported from a keyserver using a secondary source as input (QR or NFC)
     */
    public static final int REASON_BY_INPUT = 3;

    private boolean visible;
    private int reason;
    private String source;
    private long importDate;
    private long updateDate;

    public KeyRingInfoEntry() {
        this("");
    }

    public KeyRingInfoEntry(String keyServer) {
        this(true, REASON_UNKNOWN, keyServer);
    }

    public KeyRingInfoEntry(boolean visible, int reason, String source) {
        this(visible, reason, source, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public KeyRingInfoEntry(boolean visible, int reason, String source, long importDate, long updateDate) {
        this.updateDate = updateDate;
        this.importDate = importDate;
        this.source = source;
        this.reason = reason;
        this.visible = visible;
    }

    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(KeychainContract.KeyRingInfo.VISIBLE, visible);
        values.put(KeychainContract.KeyRingInfo.REASON, reason);
        values.put(KeychainContract.KeyRingInfo.SOURCE, source);
        values.put(KeychainContract.KeyRingInfo.IMPORT_DATE, importDate);
        values.put(KeychainContract.KeyRingInfo.UPDATE_DATE, updateDate);
        return values;
    }

    public void resetUpdateDate() {
        updateDate = System.currentTimeMillis();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getImportDate() {
        return importDate;
    }

    public long getUpdateDate() {
        return updateDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (visible ? 1 : 0));
        dest.writeInt(reason);
        dest.writeString(source);
        dest.writeLong(importDate);
        dest.writeLong(updateDate);
    }
}
