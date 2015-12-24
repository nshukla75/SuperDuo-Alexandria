package it.jaschke.alexandria.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.adapter.BookListAdapter;
import it.jaschke.alexandria.adapter.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;


public class ListOfBooks extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // use classname when logging
    private final String LOG_TAG = ListOfBooks.class.getSimpleName();

    private BookListAdapter bookListAdapter;
    private ListView bookList;
    private int mListPosition = ListView.INVALID_POSITION;
    private EditText searchText;

    private final int LOADER_ID = 10;

    // key constants for saving the state
    private static final String mListPositionStateKey = "listPosition";

    public ListOfBooks() {
    }

    public interface Callbacks {
        /**
         * Open a bookdetail fragment from given ean
         * @param ean String
         * @param title String
         */
        void onItemSelected(String ean, String title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Cursor cursor = getActivity().getContentResolver().query(
                AlexandriaContract.BookEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                AlexandriaContract.BookEntry.SAVED +" = ? ", // cols for "where" clause
                new String[] {"1"}, // values for "where" clause
                null  // sort order
        );


        View rootView = inflater.inflate(R.layout.fragment_list_of_books, container, false);
        searchText = (EditText) rootView.findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            /**
             * Search/auto-complete the booklist when typing
             * @param text Editable
             */
            @Override
            public void afterTextChanged(Editable text) {
                restartLoader();
            }
        });

        // on enter search the list
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (((event != null) &&
                        (event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {

                    // get the value of the edittext and search if the length is minimum 2 chars
                    String query = searchText.getText().toString().trim();
                    if (query.length() > 1) {
                        restartLoader();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.search_list_notice), Toast.LENGTH_SHORT).show();
                    }

                    // keep the focus on the textfield
                    searchText.requestFocus();
                }

                return true;
            }
        });

        rootView.findViewById(R.id.searchButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String query = searchText.getText().toString().trim();
                        if (query.length() > 1) {
                            restartLoader();
                        } else {

                            // show the toast
                            Toast.makeText(getActivity(), getString(R.string.search_list_notice), Toast.LENGTH_SHORT).show();

                            // set the focus in the edittext
                            searchText.requestFocus();
                        }
                    }
                }
        );

        // get the emptyview for the books listview
        View emptyView = rootView.findViewById(R.id.listview_empty);

        bookListAdapter = new BookListAdapter(getActivity(), cursor, 0,emptyView);

        bookList = (ListView) rootView.findViewById(R.id.listOfBooks);
        bookList.setAdapter(bookListAdapter);

        bookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = bookListAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    // get the clicked listitem position
                            mListPosition = position;
                    ((Callback)getActivity())
                            .onItemSelected(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
                }
            }
        });
        // set the title of the mainactivity toolbar
        getActivity().setTitle(R.string.books);

        // set the focus on the edittext field
        searchText.requestFocus();

        // get the saved state vars
        if (savedInstanceState != null) {

            // get the listview scroll position
            if (savedInstanceState.containsKey(mListPositionStateKey)) {
                mListPosition = savedInstanceState.getInt(mListPositionStateKey);
            }
        }

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the selected books listitem position
        if (mListPosition != ListView.INVALID_POSITION) {
            outState.putInt(mListPositionStateKey, mListPosition);
        }
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // query saved books only
        String selection = AlexandriaContract.BookEntry.SAVED +" = ? ";
        String saved = "1";

        // get the search query string from the textfield
        String searchString = searchText.getText().toString();

        // if not empty, use it to query the books
        if(searchString.length() > 0) {
            // search in the book title and subtitle
            selection += " AND (" + AlexandriaContract.BookEntry.TITLE + " LIKE ? OR " + AlexandriaContract.BookEntry.SUBTITLE + " LIKE ? )";
            searchString = "%" + searchString + "%";


            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{saved, searchString, searchString},
                    null
            );

        }else {
            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{saved},
                    null
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        bookListAdapter.swapCursor(data);
        if (mListPosition != ListView.INVALID_POSITION) {
            bookList.smoothScrollToPosition(mListPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        bookListAdapter.swapCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

}
