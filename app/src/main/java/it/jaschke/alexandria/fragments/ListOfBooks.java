package it.jaschke.alexandria.fragments;

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
import it.jaschke.alexandria.adapter.BookListAdapter;
import it.jaschke.alexandria.data.AlexandriaContract;


public class ListOfBooks extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // adapter for population of the booklist view
    private BookListAdapter bookListAdapter;
    // reference to the books listview
    private ListView bookList;
    // books listitem position
    private int mListPosition = ListView.INVALID_POSITION;
    // reference to the search textfield
    private EditText searchText;
    // unique id for the loadermanager
    private final int LOADER_ID = 10;
    // key constants for saving the state
    private static final String mListPositionStateKey = "listPosition";

    /**
     * Constructor
     */
    public ListOfBooks() {
    }

    /**
     * Callback interface to be used in the mainactivity, for selecting a book from the listview
     */
    public interface Callbacks {
        /**
         * Open a bookdetail fragment from given ean
         * @param ean String
         * @param title String
         */
        void onItemSelected(String ean, String title);
    }

    /**
     *
     * @param savedInstanceState Bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * On create setup fragment layout items, data and clickhandlers
     * @param inflater LayoutInflater
     * @param container ViewGroup
     * @param savedInstanceState Bundle
     * @return View
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // get the booklist fragment
        View rootView = inflater.inflate(R.layout.fragment_list_of_books, container, false);
        // get a reference to the search field and add handler to search the list after typing
        searchText = (EditText) rootView.findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            /**
             *
             * @param charSequence
             * @param i
             * @param i1
             * @param i2
             */
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            /**
             *
             * @param charSequence
             * @param i
             * @param i1
             * @param i2
             */
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

        // get the button and attach onclick handler to load the books (based on optionally entered search string)
        rootView.findViewById(R.id.searchButton).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                // get the value of the edittext and search if the length is minimum 2 chars
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
        // get cursor containing all the saved books
        Cursor cursor = getActivity().getContentResolver().query(
            AlexandriaContract.BookEntry.CONTENT_URI,
            null, // leaving "columns" null just returns all the columns.
            AlexandriaContract.BookEntry.SAVED +" = ? ", // cols for "where" clause
            new String[] {"1"}, // values for "where" clause
            null  // sort order
        );

        // get the emptyview for the books listview
        View emptyView = rootView.findViewById(R.id.listview_empty);
        // create the booklist adapter and populate it with the loaded books
        bookListAdapter = new BookListAdapter(getActivity(), cursor, 0,emptyView);
        // get the booklist view, attach the adapter, and attach onclick handler on the list items to open the bookdetail fragment
        bookList = (ListView) rootView.findViewById(R.id.listOfBooks);
        bookList.setAdapter(bookListAdapter);
        bookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = bookListAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    // get the clicked listitem position
                    mListPosition = position;
                    // call the mainactivity method to open the bookdetail fragment
                    ((Callbacks)getActivity())
                            .onItemSelected(
                                    cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)),
                                    cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry.TITLE)));
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

    /**
     * Restart the loader
     */
    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    /**
     * Save the the current state before leaving the activity
     * @param outState Bundle
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the selected books listitem position
        if (mListPosition != ListView.INVALID_POSITION) {
            outState.putInt(mListPositionStateKey, mListPosition);
        }
    }

    /**
     * Create the loader, optionally based on a given search string
     * @param id int
     * @param args Bundle
     * @return Loader
     */
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

            // create the loader with given criteria
            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{saved, searchString, searchString},
                    null
            );
        }else {
            // else return all the books
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

    /**
     * Update the booklist adapter cursor when finished loading
     * @param loader Loader
     * @param data Cursor
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        bookListAdapter.swapCursor(data);
        // scroll to the saved list scroll position
        if (mListPosition != ListView.INVALID_POSITION) {
            bookList.smoothScrollToPosition(mListPosition);
        }
    }

    /**
     * On reset clear the cursor of the booklist adapter
     * @param loader Loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        bookListAdapter.swapCursor(null);
    }

    /**
     * Destroy Activity
     */
    @Override
    public void onDestroy() {
        super.onDestroy();


    }

}
