package it.jaschke.alexandria.adapter;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.DownloadImage;

/**
 * Created by saj on 11/01/15.
 */
public class BookListAdapter extends CursorAdapter {

    // keep the activity context
    final private Context mContext;

    // empty view for when we have no data and want to inform the user
    final private View mEmptyView;

    public BookListAdapter(Context context, Cursor c, int flags, View emptyView) {
        super(context, c, flags);
        mContext = context;
        mEmptyView = emptyView;

        // show or hide the empty view, depending on empty cursor
        mEmptyView.setVisibility(c.getCount() == 0 ? View.VISIBLE : View.GONE);

    }

    public static class ViewHolder {
        public final ImageView bookCover;
        public final TextView bookTitle;
        public final TextView bookSubTitle;

        public ViewHolder(View view) {
            bookCover = (ImageView) view.findViewById(R.id.fullBookCover);
            bookTitle = (TextView) view.findViewById(R.id.listBookTitle);
            bookSubTitle = (TextView) view.findViewById(R.id.listBookSubTitle);
        }
    }



    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String imgUrl = cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        Glide.with(mContext)
                .load(imgUrl)
                .error(R.drawable.cover_not_available)
                .crossFade()
                .into(viewHolder.bookCover);

        String bookTitle = cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        viewHolder.bookTitle.setText(bookTitle);

        String bookSubTitle = cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        viewHolder.bookSubTitle.setText(bookSubTitle);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.book_list_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {

        // show or hide the empty view, depending on empty cursor
        if (newCursor != null) {
            mEmptyView.setVisibility(newCursor.getCount() == 0 ? View.VISIBLE : View.GONE);
        }

        return super.swapCursor(newCursor);
    }
}
