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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.adapter.SelectedFilesAdapter;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.util.ArrayList;
import java.util.List;

public class DecryptFileFragment extends DecryptFragment {
    public static final String ARG_URI = "uri";
    public static final String ARG_FROM_VIEW_INTENT = "view_intent";

    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    // view
    private View mAddView;
    private View mDecryptButton;
    private View mShareButton;
    private ListView mSelectedFiles;
    private SelectedFilesAdapter mAdapter = new SelectedFilesAdapter();

    // model
    private Uri mInputUri = null;
    private List<Uri> mDecryptedUris = new ArrayList<Uri>();
    private Uri mOutputUri = null;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_file_fragment, container, false);

        mShareButton = view.findViewById(R.id.action_decrypt_share);
//        mShareButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                decryptAction();
//            }
//        });
        mDecryptButton = view.findViewById(R.id.action_decrypt_file);
//        mDecryptButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                decryptAction();
//            }
//        });

        mAddView = inflater.inflate(R.layout.file_list_entry_add, null);
        mAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInputUri();
            }
        });
        mSelectedFiles = (ListView) view.findViewById(R.id.selected_files_list);
        mSelectedFiles.addFooterView(mAddView);
        mSelectedFiles.setAdapter(mAdapter);
        mAdapter.setRemoveClickListener(new SelectedFilesAdapter.RemoveClickListener() {
            @Override
            public void onRemoveClicked(int position) {
                delInputUri(position);
            }
        });

        return view;
    }

    private void delInputUri(int position) {
        Uri uri = mDecryptedUris.remove(position);
        TemporaryStorageProvider.remove(getActivity(), uri);
        mAdapter.updateUris(mDecryptedUris);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addEncryptedUri(getArguments().<Uri>getParcelable(ARG_URI));
    }

    private void addInputUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FileHelper.openDocument(DecryptFileFragment.this, "*/*", REQUEST_CODE_INPUT);
        } else {
            FileHelper.openFile(DecryptFileFragment.this, mInputUri, "*/*",
                    REQUEST_CODE_INPUT);
        }
    }

    private void addEncryptedUri(Uri inputUri) {
        if (inputUri == null) {
            return;
        }
        mInputUri = inputUri;
        decryptOriginalFilename(null);
    }

    private void addDecryptedUri(Uri uri) {
        mDecryptedUris.add(uri);
        mAdapter.updateUris(mDecryptedUris);
    }

    private void decryptAction() {
        if (mInputUri == null) {
            Notify.showNotify(getActivity(), R.string.no_file_selected, Notify.Style.ERROR);
            return;
        }

//        decryptUsingMeta();
        decryptOriginalFilename(null);
    }

    private String removeEncryptedAppend(String name) {
        if (name.endsWith(".asc") || name.endsWith(".gpg") || name.endsWith(".pgp")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void decryptUsingMeta(String filename, String mimeType) {
        if (!TextUtils.isEmpty(filename)) {
            filename = removeEncryptedAppend(FileHelper.getFilename(getActivity(), mInputUri));
        }
        mOutputUri = TemporaryStorageProvider.createFile(getActivity(), filename, mimeType);
        decryptStart(null);
    }

    private void decryptOriginalFilename(String passphrase) {
        Log.d(Constants.TAG, "decryptOriginalFilename");

        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();
        intent.setAction(KeychainIntentService.ACTION_DECRYPT_METADATA);

        // data
        Log.d(Constants.TAG, "mInputUri=" + mInputUri + ", mOutputUri=" + mOutputUri);

        data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_URI);
        data.putParcelable(KeychainIntentService.ENCRYPT_INPUT_URI, mInputUri);

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_URI);
        data.putParcelable(KeychainIntentService.ENCRYPT_OUTPUT_URI, mOutputUri);

        data.putString(KeychainIntentService.DECRYPT_PASSPHRASE, passphrase);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after decrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_decrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    PgpDecryptVerifyResult decryptVerifyResult =
                            returnData.getParcelable(KeychainIntentService.RESULT_DECRYPT_VERIFY_RESULT);

                    if (PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                        showPassphraseDialogForFilename(decryptVerifyResult.getKeyIdPassphraseNeeded());
                    } else if (PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED ==
                            decryptVerifyResult.getStatus()) {
                        showPassphraseDialogForFilename(Constants.key.symmetric);
                    } else {

                        // go on...
                        decryptUsingMeta(decryptVerifyResult.getDecryptMetadata().getFilename(),
                                decryptVerifyResult.getDecryptMetadata().getMimeType());
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    protected void showPassphraseDialogForFilename(long keyId) {
        PassphraseDialogFragment.show(getActivity(), keyId,
                new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                            String passphrase =
                                    message.getData().getString(PassphraseDialogFragment.MESSAGE_DATA_PASSPHRASE);
                            decryptOriginalFilename(passphrase);
                        }
                    }
                }
        );
    }

    @Override
    protected void decryptStart(String passphrase) {
        Log.d(Constants.TAG, "decryptStart");

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data
        Log.d(Constants.TAG, "mInputUri=" + mInputUri + ", mOutputUri=" + mOutputUri);

        data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_URI);
        data.putParcelable(KeychainIntentService.ENCRYPT_INPUT_URI, mInputUri);

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_URI);
        data.putParcelable(KeychainIntentService.ENCRYPT_OUTPUT_URI, mOutputUri);

        data.putString(KeychainIntentService.DECRYPT_PASSPHRASE, passphrase);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after decrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_decrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    PgpDecryptVerifyResult decryptVerifyResult =
                            returnData.getParcelable(KeychainIntentService.RESULT_DECRYPT_VERIFY_RESULT);

                    if (PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(decryptVerifyResult.getKeyIdPassphraseNeeded());
                    } else if (PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED ==
                            decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(Constants.key.symmetric);
                    } else {
                        // display signature result in activity
                        onResult(decryptVerifyResult);
                        addDecryptedUri(mOutputUri);
                        /*
                        if (mDeleteAfter.isChecked()) {
                            // Create and show dialog to delete original file
                            DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment.newInstance(mInputUri);
                            deleteFileDialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
                            addEncryptedUri(null);
                        }
                        */

                        /*
                        // A future open after decryption feature
                        if () {
                            Intent viewFile = new Intent(Intent.ACTION_VIEW);
                            viewFile.setData(mOutputUri);
                            startActivity(viewFile);
                        }
                        */
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addEncryptedUri(data.getData());
                }
                return;
            }
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mOutputUri = data.getData();
                    decryptStart(null);
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }
}
