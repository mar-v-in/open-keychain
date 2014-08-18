package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.OtherHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SelectedFilesAdapter extends BaseAdapter {

    public interface RemoveClickListener {
        void onRemoveClicked(int position);
    }

    private final Map<Uri, Bitmap> mThumbnailCache = new HashMap<Uri, Bitmap>();
    private List<Uri> mUris = Collections.emptyList();
    private Context mContext;
    private RemoveClickListener mRemoveClickListener;

    public void updateUris(List<Uri> uris) {
        mUris = uris == null ? Collections.<Uri>emptyList() : uris;

        // Clear cache if needed
        for (Uri uri : new HashSet<Uri>(mThumbnailCache.keySet())) {
            if (!mUris.contains(uri)) {
                mThumbnailCache.remove(uri);
            }
        }

        notifyDataSetChanged();
    }

    public void setRemoveClickListener(RemoveClickListener removeClickListener) {
        mRemoveClickListener = removeClickListener;
    }

    @Override
    public int getCount() {
        return mUris.size();
    }

    @Override
    public Object getItem(int position) {
        return mUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Uri inputUri = mUris.get(position);
        View view;
        if (convertView == null) {
            view = View.inflate(parent.getContext(), R.layout.file_list_entry, null);
        } else {
            view = convertView;
        }
        ((TextView) view.findViewById(R.id.filename)).setText(FileHelper.getFilename(parent.getContext(), inputUri));
        long size = FileHelper.getFileSize(parent.getContext(), inputUri);
        if (size == -1) {
            ((TextView) view.findViewById(R.id.filesize)).setText("");
        } else {
            ((TextView) view.findViewById(R.id.filesize)).setText(FileHelper.readableFileSize(size));
        }
        view.findViewById(R.id.action_remove_file_from_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRemoveClickListener != null) mRemoveClickListener.onRemoveClicked(position);
            }
        });
        int px = OtherHelper.dpToPx(parent.getContext(), 48);
        if (!mThumbnailCache.containsKey(inputUri)) {
            mThumbnailCache.put(inputUri, FileHelper.getThumbnail(parent.getContext(), inputUri, new Point(px, px)));
        }
        Bitmap bitmap = mThumbnailCache.get(inputUri);
        if (bitmap != null) {
            ((ImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(bitmap);
        } else {
            ((ImageView) view.findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_doc_generic_am);
        }
        return view;
    }
}
