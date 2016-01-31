package it.jaschke.alexandria.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.activities.ScanActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // reference to the ean search field
    private EditText mEanSearchField;
    // unique id for the loadermanager
    private final int LOADER_ID = 30;
    private View rootView;
    // key for storing the ean search field value in the savedinstance bundel
    private final String EAN_CONTENT="eanContent";
    // the first 3 digits of a isbn13 are always the same
    private String mEanPrefix;

    /***
     * Constructor
     */
    public AddBook(){
    }

    /***
     * Save the state
     * @param outState Bundle
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mEanSearchField!=null) {
            outState.putString(EAN_CONTENT, mEanSearchField.getText().toString());
        }
    }

    /***
     * On create view initialize the components and its listeners
     * @param inflater LayoutInflater
     * @param container ViewGroup
     * @param savedInstanceState Bundle
     * @return View
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mEanPrefix = getString(R.string.ean_13_prefix);
        // get the add book fragment
        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        // get the edittext input field and attach text changed listener
        mEanSearchField = (EditText) rootView.findViewById(R.id.ean);
        mEanSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if ((ean.length() == 10 && !ean.startsWith(mEanPrefix)) || (ean.length() == 13)) {
                    handleSubmit(false);
                } else {
                    clearFields();
                }
            }
        });
        // submit on enter
        mEanSearchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (((event != null) &&
                        (event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    // force a submit
                    handleSubmit(true);
                    // keep the focus on the textfield
                    mEanSearchField.requestFocus();
                }
                return true;
            }
        });
        // handle submit onclick on the search button
        rootView.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // force a submit
                handleSubmit(true);
                // set the focus on the textfield
                mEanSearchField.requestFocus();
            }
        });
        // get the scan button and attach the onclick to launch the scanner
        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: launch the barcode scanner,when done, remove the toast below.
                //CharSequence text = "This button should let you scan a book for its barcode!";
                // launch the barcode scanner
                if (Utility.isNetworkAvailable(getActivity())) {
                    scanBarcode();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.network_required_notice), Toast.LENGTH_SHORT).show();
                }
            }

        });
        // get the save button and attach the onclick handler to save the book to the database
        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send message to bookservice to save the book
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, mEanSearchField.getText().toString());
                bookIntent.setAction(BookService.CONFIRM_BOOK);
                getActivity().startService(bookIntent);
                // clear the search field
                mEanSearchField.setText("");
            }
        });
        // get the delete button and attach the onclick handler to let the mainactivity delete a
        //  book from the database
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEanSearchField.setText("");
            }
        });
        // load previously values from instancestate, if available
        if(savedInstanceState!=null){
            // get ean search field value and update if not empty
            String tempEan = savedInstanceState.getString(EAN_CONTENT);
            if (tempEan != null) {
                if (!tempEan.equals("")) {
                    mEanSearchField.setText(tempEan);
                }
            }
        }
        // set the toolbar title field
        getActivity().setTitle(R.string.scan);
        // set the focus on the edittext field
        mEanSearchField.requestFocus();
        return rootView;
    }

    /**
     * Trigger the fetch bookservice on various events
     * @param forced boolean show toast notices when forced is true
     */
    private void handleSubmit(boolean forced) {

        // get the value entered in the searchfield
        String tempEan = mEanSearchField.getText().toString().trim();

        // check if an isbn number was entered
        if ((tempEan.length() == 10) || (tempEan.length() == 13)) {

            // prefix isbn10 numbers
            if ((tempEan.length() == 10) && !tempEan.startsWith(mEanPrefix)) {
                tempEan = mEanPrefix + tempEan;
            }
            // if we have a string of 13 digits
            if (tempEan.length() == 13) {
                // start a bookservice intent to call the books api
                if (Utility.isNetworkAvailable(getActivity())) {
                    fetchBookFromService(tempEan);
                    // restart the loader to populate the preview view
                    restartLoader();
                } else {
                    if (forced) {
                        // show toast when we don't have a network connection
                        Toast.makeText(getActivity(), getString(R.string.network_required_notice),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (forced) {
            // show toast when no text was entered
            Toast.makeText(getActivity(), getString(R.string.text_input_required),Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start the GoogleBooksService and tell it to fetch a book with given ean
     * @param ean String
     */
    private void fetchBookFromService(String ean) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean);
        bookIntent.setAction(BookService.FETCH_BOOK);
        getActivity().startService(bookIntent);
    }

    /**
     * Restart the loader
     */
    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    /**
     * Create the loader to load full book date for entered ean
     * @param id int
     * @param args Bundle
     * @return Loader
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mEanSearchField.getText().length()>0){
            // get the string value from the ean search field
            String eanStr= mEanSearchField.getText().toString();
            // add prefix if entered value length is 10
            if(eanStr.length()==10 && !eanStr.startsWith(mEanPrefix)){
                eanStr = mEanPrefix + eanStr;
            }
            // load full book date for entered ean
            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                    null,
                    null,
                    null,
                    null
            );
        } else {
            return null;
        }
    }

    /**
     * Populate the view when finished loading
     * @param loader Loader
     * @param data Cursor
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            // populate the view items
            View view = getView();
            if (view != null) {
                // cover image
                String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
                ImageView coverView = (ImageView) view.findViewById(R.id.bookCover);
                if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
                    // load the cover image
                    Glide.with(this)
                            .load(imgUrl)
                            .error(R.drawable.cover_not_available)
                            .crossFade()
                            .into(coverView);
                } else {
                    // or set the image-not-available resource
                    coverView.setImageResource(R.drawable.cover_not_available);
                }
                coverView.setVisibility(View.VISIBLE);
                // book title
                String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
                ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);
                // subtitle
                String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
                ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);
                // authors
                String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
                if (authors != null) {
                    String[] authorsArr = authors.split(",");
                    ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
                    ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
                }
                // categories
                String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
                ((TextView) rootView.findViewById(R.id.categories)).setText(categories);
                // description
                String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
                ((TextView) view.findViewById(R.id.bookDescription)).setText(desc);

                // show the delete and save button
                rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * On reset the loader - not used
     * @param loader Loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * Helper method to clear the book preview view items
     */
    private void clearFields(){
        View view = getView();

        if (view != null) {
            ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
            ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
            ((TextView) rootView.findViewById(R.id.authors)).setText("");
            ((TextView) rootView.findViewById(R.id.categories)).setText("");
            ((TextView) rootView.findViewById(R.id.bookDescription)).setText("");
            view.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Scan a ISBN book barcode using the zxing library
     */
    private void scanBarcode() {

        // create the intent integrator to scan in the current fragment
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);

        // use a custom scanactivity that can be rotated
        integrator.setCaptureActivity(ScanActivity.class);
        integrator.setOrientationLocked(false);

        // limit scanning to only one-dimensional barcodes
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);

        // set the prompt message
        integrator.setPrompt(getString(R.string.scanner_prompt));

        // launch the scanner
        integrator.initiateScan();
    }

    /**
     * Catch the result of a scanned barcode
     * @param requestCode int
     * @param resultCode int
     * @param data Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // parse the result in a intentresult object
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(getActivity(), getString(R.string.scanner_cancelled), Toast.LENGTH_SHORT).show();
            } else {

                // get the scanned code and code format
                String ean = result.getContents();

                // inform the user about the ean number
                Toast.makeText(getActivity(),"Scanned barcode: "+ ean, Toast.LENGTH_SHORT).show();

                // once we have an isbn, add it to the search field (this will trigger the bookservice call)
                mEanSearchField.setText(ean);
            }
        }
    }

}
