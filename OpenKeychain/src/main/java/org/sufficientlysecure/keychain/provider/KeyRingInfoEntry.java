package org.sufficientlysecure.keychain.provider;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import org.sufficientlysecure.keychain.provider.KeychainContract;

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

    private final boolean visible;
    private final int reason;
    private final String source;
    private final long importDate;
    private final long updateDate;

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
