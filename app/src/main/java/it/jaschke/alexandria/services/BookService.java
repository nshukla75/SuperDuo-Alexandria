package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.activities.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    // use classname when logging
    private final String LOG_TAG = BookService.class.getSimpleName();

    // service actions
    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";
    public static final String CONFIRM_BOOK = "it.jaschke.alexandria.services.action.CONFIRM_BOOK";
    public static final String DELETE_NOT_SAVED = "it.jaschke.alexandria.services.action.DELETE_NOT_SAVED";

    // ean book variable name
    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    /**
     * Constructor
     */
    public BookService() {
        super("Alexandria");
    }

    /**
     * Handle the given intent by fetching or deleting a book, depending given action
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String ean = intent.getStringExtra(EAN);

            if ((ean != null) && (ean.length() == 10) && !ean.startsWith(getString(R.string.ean_13_prefix))) {
                ean = getString(R.string.ean_13_prefix) + ean;
            }

            if (FETCH_BOOK.equals(action)) {
                // fetch book with given ean from the google books api and save it to the database
                fetchBook(ean);
            }  else if (CONFIRM_BOOK.equals(action)) {
                // mark the book that already exists in the database as saved
                int saved = saveBook(ean);
                if (saved != 0) {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_saved));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                }
            } else if (DELETE_BOOK.equals(action)) {
                int deleted = deleteBook(ean);
                if (deleted != 0) {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_deleted));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                }
            } else if (DELETE_NOT_SAVED.equals(action)) {
                // cleanup books table by removing books that were previously cached, but not added to the booklist
                deleteNotSavedBooks();
            }
        }
    }

    /**
     * Mark a temporarily inserted book as saved
     * @param ean String
     * @return int
     */
    private int saveBook(String ean) {
        if (ean != null) {
            ContentValues values = new ContentValues();
            values.put(AlexandriaContract.BookEntry.SAVED, 1);
            return getContentResolver().update(
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    values,
                    AlexandriaContract.BookEntry._ID +" = ?",
                    new String[] { ean }
            );
        } else {
            return 0;
        }
    }

    /**
     * Delete book with given ean number from the database
     * @param ean String
     * @return int
     */
    private int deleteBook(String ean) {
        if(ean!=null) {
            return getContentResolver().delete(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                    null,
                    null);
        }else{
            return 0;
        }
    }

    /**
     * Remove the previously fetched, but not saved, books from the books table
     * @return int
     */
    private int deleteNotSavedBooks() {
        return getContentResolver().delete(
                AlexandriaContract.BookEntry.CONTENT_URI,
                AlexandriaContract.BookEntry.SAVED +" = ?",
                new String[] {"0"}
        );
    }

    /**
     * Fetch book with given ean from the google books api and save it to the database
     * @param ean String
     * @return boolean
     */
    private boolean fetchBook(String ean) {

        boolean found = false;
        boolean cached = false;
        // only continue if ean number has 13 digits
        if (ean.length() != 13) {
            return found;
        }
        // create an intent for broadcasting the result back to the mainactivity
        Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        // exit if book already exists in the database
        if (bookEntry != null) {
            if (bookEntry.getCount() > 0) {
                if (bookEntry.moveToFirst()) {
                    int saved = bookEntry.getInt(bookEntry.getColumnIndex(AlexandriaContract.BookEntry.SAVED));
                    if (saved == 1) {
                        found = true;
                        messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_saved_before));
                    } else {
                        cached = true;
                    }
                }
            }
            bookEntry.close();
        }

        if (!found) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {
                final String BOOKS_BASE_URL = getString(R.string.googleapis_books_api);
                final String QUERY_PARAM = "q";

                final String ISBN_PARAM = "isbn:" + ean;

                Uri builtUri = Uri.parse(BOOKS_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream != null) {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                        buffer.append("\n");
                    }

                    if (buffer.length() != 0) {
                        bookJsonString = buffer.toString();
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }

            }

            final String ITEMS = "items";
            final String VOLUME_INFO = "volumeInfo";
            final String TITLE = "title";
            final String SUBTITLE = "subtitle";
            final String AUTHORS = "authors";
            final String DESC = "description";
            final String CATEGORIES = "categories";
            final String IMG_URL_PATH = "imageLinks";
            final String IMG_URL = "thumbnail";

            try {
                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;
                if (!bookJson.has(ITEMS)) {
                    // if json has no items, send message with intent to main activity to show a toast
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_not_found));
                } else {
                    // get the books from the json
                    bookArray = bookJson.getJSONArray(ITEMS);
                    JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);
                    // get title
                    String title = bookInfo.getString(TITLE);
                    // get subtitle
                    String subtitle = "";
                    if (bookInfo.has(SUBTITLE)) {
                        subtitle = bookInfo.getString(SUBTITLE);
                    }
                    // get description
                    String desc = "";
                    if (bookInfo.has(DESC)) {
                        desc = bookInfo.getString(DESC);
                    }
                    // get image url
                    String imgUrl = "";
                    if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                        imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
                    }
                    // insert a book in the database
                    if (!cached) {
                        writeBackBook(ean, title, subtitle, desc, imgUrl);
                    }
                    // insert the books authors in the database
                    if (bookInfo.has(AUTHORS)) {
                        writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
                    }
                    // insert the books categories in the database
                    if (bookInfo.has(CATEGORIES)) {
                        writeBackCategories(ean, bookInfo.getJSONArray(CATEGORIES));
                    }
                    found = true;
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
        return found;
    }

    /**
     * Insert a book in the database
     * @param ean String
     * @param title String
     * @param subtitle String
     * @param desc String
     * @param imgUrl String
     * @return Uri
     */
    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.SAVED, 0);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    /**
     * Insert list of author names in the database for given book ean
     * @param ean String
     * @param authors JSONArray
     * @throws JSONException
     */
    private void writeBackAuthors(String ean, JSONArray authors) throws JSONException {
        // loop through array containing the authors
        for (int i = 0; i < authors.length(); i++) {
            // create contentvalues object and add fields for database insert
            ContentValues values= new ContentValues();
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, authors.getString(i));
            // insert author in the database
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);

        }
    }

    /**
     * Insert list of categories in the database for given book ean
     * @param ean String
     * @param categories JSONArray
     * @throws JSONException
     */
    private void writeBackCategories(String ean, JSONArray categories) throws JSONException {
        // loop through array containing the categories
        for (int i = 0; i < categories.length(); i++) {
            // create contentvalues object and add fields for database insert
            ContentValues values= new ContentValues();
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, categories.getString(i));
            // insert category in the database
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
        }
    }
 }