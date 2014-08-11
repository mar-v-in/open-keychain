package org.sufficientlysecure.keychain.provider;

import android.content.ContentValues;
import org.sufficientlysecure.keychain.provider.KeychainContract;

public class KeyRingInfoEntry {
    private final boolean visible;
    private final int reason;
    private final String source;
    private final long importDate;
    private final long updateDate;

    public KeyRingInfoEntry(long updateDate, long importDate, String source, int reason, boolean visible) {
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
}
