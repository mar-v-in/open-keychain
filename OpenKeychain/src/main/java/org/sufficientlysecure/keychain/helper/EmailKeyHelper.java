/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.helper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Messenger;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.KeychainIntentService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmailKeyHelper {

    public static void importContacts(Context context, Messenger messenger) {
        importAll(context, messenger, ContactHelper.getContactMails(context));
    }

    public static void importAll(Context context, Messenger messenger, List<String> mails) {
        Set<ImportKeysListEntry> keys = new HashSet<ImportKeysListEntry>();
        for (String mail : mails) {
            keys.addAll(getEmailKeys(context, mail));
        }
        List<ImportKeysListEntry> toImport = new ArrayList<ImportKeysListEntry>();
        Cursor existing = context.getContentResolver().query(KeychainContract.KeyRings.buildUnifiedKeyRingsUri(),
                new String[]{KeychainContract.KeyRings.FINGERPRINT, KeychainContract.KeyRings.KEY_ID}, null, null, null);
        if (existing != null) {
            List<String> ids = new ArrayList<String>();
            while (existing.moveToNext()) {
                ids.add(PgpKeyHelper.convertFingerprintToHex(existing.getBlob(0)));
                ids.add(PgpKeyHelper.convertKeyIdToHex(existing.getLong(1)));
                ids.add(PgpKeyHelper.convertKeyIdToHexShort(existing.getLong(1)));
            }
            existing.close();
            for (ImportKeysListEntry key : keys) {
                if (key.getFingerprintHex() != null && !ids.contains(key.getFingerprintHex().toLowerCase()) ||
                        key.getKeyIdHex() != null && !ids.contains(key.getKeyIdHex().toLowerCase())) {
                    toImport.add(key);
                }
            }
        } else {
            toImport.addAll(keys);
        }

        importKeys(context, messenger, toImport);
    }

    public static List<ImportKeysListEntry> getEmailKeys(Context context, String mail) {
        Set<ImportKeysListEntry> keys = new HashSet<ImportKeysListEntry>();

        // Try _hkp._tcp SRV record first
        String[] mailparts = mail.split("@");
        if (mailparts.length == 2) {
            HkpKeyserver hkp = HkpKeyserver.resolve(mailparts[1]);
            if (hkp != null) {
                keys.addAll(getEmailKeys(mail, hkp));
            }
        }

        if (keys.isEmpty()) {
            // Most users don't have the SRV record, so ask a default server as well
            String[] servers = Preferences.getPreferences(context).getKeyServers();
            if (servers != null && servers.length != 0) {
                HkpKeyserver hkp = new HkpKeyserver(servers[0]);
                keys.addAll(getEmailKeys(mail, hkp));
            }
        }
        return new ArrayList<ImportKeysListEntry>(keys);
    }

    private static void importKeys(Context context, Messenger messenger, List<ImportKeysListEntry> keys) {
        if (!keys.isEmpty()) {
            Intent importIntent = new Intent(context, KeychainIntentService.class);
            importIntent.setAction(KeychainIntentService.ACTION_DOWNLOAD_AND_IMPORT_KEYS);
            Bundle importData = new Bundle();
            importData.putParcelableArrayList(KeychainIntentService.DOWNLOAD_KEY_LIST,
                    new ArrayList<ImportKeysListEntry>(keys));
            importIntent.putExtra(KeychainIntentService.EXTRA_DATA, importData);
            importIntent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            context.startService(importIntent);
        }
    }

    public static List<ImportKeysListEntry> getEmailKeys(String mail, Keyserver keyServer) {
        Set<ImportKeysListEntry> keys = new HashSet<ImportKeysListEntry>();
        try {
            for (ImportKeysListEntry key : keyServer.search(mail)) {
                if (key.isRevoked() || key.isExpired()) continue;
                for (String userId : key.getUserIds()) {
                    if (userId.toLowerCase().contains(mail.toLowerCase())) {
                        keys.add(key);
                    }
                }
            }
        } catch (Keyserver.QueryFailedException ignored) {
        } catch (Keyserver.QueryNeedsRepairException ignored) {
        }
        return new ArrayList<ImportKeysListEntry>(keys);
    }
}
