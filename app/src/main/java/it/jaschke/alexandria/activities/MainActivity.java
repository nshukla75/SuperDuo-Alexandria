package it.jaschke.alexandria.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.fragments.About;
import it.jaschke.alexandria.fragments.AddBook;
import it.jaschke.alexandria.fragments.BookDetail;
import it.jaschke.alexandria.fragments.ListOfBooks;
import it.jaschke.alexandria.fragments.NavigationDrawerFragment;
import it.jaschke.alexandria.services.BookService;


public class MainActivity extends AppCompatActivity implements
        NavigationDrawerFragment.Callbacks,
        ListOfBooks.Callbacks,
        BookDetail.Callbacks {

    //Fragment managing the behaviors, interactions and presentation of the navigation drawer.
    private NavigationDrawerFragment navigationDrawerFragment;

    // when we rotate from portrait to landscape on a tablet we can keep the selected book here
    private String mEan;
    private String mBookTitle;

    // used to store the last screen mTitle
    private String mTitle;

    // reference to the app title textview in the toolbar
    private TextView mToolbarTitle;

    // tablet indicator
    public static boolean IS_TABLET = false;

    // receive messages
    private BroadcastReceiver messageReciever;

    // message type constants we can receive from the booksservice
    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";

    // key constants for saving the state
    private static final String mEanStateKey = "mEan";
    private static final String mBookTitleStateKey = "mBookTitle";
    private static final String mTitleStateKey = "mTitle";

    // drawer position constants
    public static final int BOOKLIST_FRAGMENT_POSITION = 0;
    public static final int ADDBOOK_FRAGMENT_POSITION = 1;
    public static final int ABOUT_FRAGMENT_POSITION = 2;

    /**
     * On create setup layout and fragments
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if app running on a tablet
        IS_TABLET = isTablet();

        // if we run on a tablet select an alternative main layout
        if(IS_TABLET){
            setContentView(R.layout.activity_main_tablet);
        }else {
            setContentView(R.layout.activity_main);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);

        // get a reference to the navigation drawer fragment
        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // get the mTitle of the activity
        mTitle = (String)getTitle();

        // set the toolbar title to the name of the app
        mToolbarTitle.setText(R.string.app_name);

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // create receiver to receive event messages
        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);


        // send message to bookservice to delete existing not-saved books from previous session
        Intent bookIntent = new Intent(this, BookService.class);
        bookIntent.setAction(BookService.DELETE_NOT_SAVED);
        startService(bookIntent);

        // prevent the keyboard from appearing already oncreate mainactivity
        Utility.hideKeyboardFromActivity(this);

        // get the saved state vars
        if (savedInstanceState != null) {

            if (savedInstanceState.containsKey(mTitleStateKey)) {
                mTitle = savedInstanceState.getString(mTitleStateKey, null);
            }

            if (savedInstanceState.containsKey(mEanStateKey)) {
                mEan = savedInstanceState.getString(mEanStateKey, null);
            }

            if (savedInstanceState.containsKey(mBookTitleStateKey)) {
                mBookTitle = savedInstanceState.getString(mBookTitleStateKey, null);
            }

            // when rotating to landscape booklist on a tablet select the previously selected book
            if (IS_TABLET && findViewById(R.id.right_container) != null && mTitle.equals(getString(R.string.books)) && mEan != null && mBookTitle != null) {
                onItemSelected(mEan, mBookTitle);
                mEan = null;
                mBookTitle = null;
            }
        }
    }

    /**
     * Save the the current state before leaving the activity
     * @param outState Bundle
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(mTitleStateKey, mTitle);
        outState.putString(mEanStateKey, mEan);
        outState.putString(mBookTitleStateKey, mBookTitle);
    }

    /**
     * Select the fragment based on the selected position in the navigation drawer menu
     * @param position int
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // choose the selected fragment based on given position
        Fragment choosenFragment;
        switch (position){
            default:
            case BOOKLIST_FRAGMENT_POSITION:
                choosenFragment = new ListOfBooks();
                break;
            case ADDBOOK_FRAGMENT_POSITION:
                choosenFragment = new AddBook();
                break;
            case ABOUT_FRAGMENT_POSITION:
                choosenFragment = new About();
                break;

        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, choosenFragment)
                .addToBackStack(mTitle)
                .commit();
        Utility.hideKeyboardFromActivity(this);
    }

    /**
     * Set title of the activity depending on the loaded fragment
     * @param titleId int
     */
    public void setTitle(int titleId) {
        // set the local mtitle var
        mTitle = getString(titleId);

        // update the toolbar title
        mToolbarTitle.setText(mTitle);
    }

    /**
     * When creating the settings menu options
     * @param menu Menu
     * @return boolan
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // only show the settings menu when the navigation drawer is not open
        if (!navigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            // restore the toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                mToolbarTitle.setText(mTitle);
            }
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handle action bar item clicks based on the selected options menu item
     * @param item MenuItem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // get selected menu option
        int id = item.getItemId();

        // act on the selected menu option
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }else if (id == android.R.id.home) {

            // hide keyboard when the drawer is opened
            Utility.hideKeyboardFromActivity(this);

            // if we are not on a tablet in landscape mode
            if(findViewById(R.id.right_container) == null) {

                // if we are coming back from the bookdetail fragment, reset the hamburger
                if (mTitle.equals(getString(R.string.detail))) {
                    getSupportFragmentManager().popBackStack();
                    toggleToolbarDrawerIndicator(false);
                    return true;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Toogle the toolbar drawer icon between the hamburger and back icon
     * @param backToHome boolean
     */
    public void toggleToolbarDrawerIndicator(boolean backToHome) {
        // if we are not on a tablet in landscape mode
        if(findViewById(R.id.right_container) == null) {
            navigationDrawerFragment.toggleToolbarDrawerIndicator(backToHome);
        }
    }

    /**
     * Open the BookDetail fragment with given ean, using a callback interface, from fragment_list_of_books
     * @param ean String
     * @param title String
     */
    @Override
    public void onItemSelected(String ean, String title) {
        mEan = ean;
        mBookTitle = title;

        // create a bundle to pass the ean to the detail fragment
        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);
        args.putString(BookDetail.TITLE_KEY, title);

        // instantiate the detail fragment and pass it the bundle containing the ean
        BookDetail fragment = new BookDetail();
        fragment.setArguments(args);

        // select where to put the detail fragment based on mobile/tablet
        int id = R.id.container;
        if(findViewById(R.id.right_container) != null){
            id = R.id.right_container;
        }
        // replace the contents of the container element with the detail fragment and add it to the backstack
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(id, fragment);
        fragmentTransaction.addToBackStack(getString(R.string.detail));
        fragmentTransaction.commit();

        // toggle the icon for the back icon
        toggleToolbarDrawerIndicator(true);

        // hide the keyboard when we navigate to the bookdetail fragment
        Utility.hideKeyboardFromActivity(this);

    }

    /**
     * show toast on receive message
     */
    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra(MESSAGE_KEY)!=null){
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * check if app is running on tablet
     * @return boolean
     */
    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Check if we can finish the activity when the backbutton is pressed (toolbar or os)
     */
    @Override
    public void onBackPressed() {
        // close the drawer if it's opened
        if (navigationDrawerFragment.isDrawerOpen()) {
            navigationDrawerFragment.closeDrawer();
            return;
        }
        // if we are coming back from the book detail fragment, reset the icon
        if (mTitle.equals(getString(R.string.detail))) {
            toggleToolbarDrawerIndicator(false);
        }
        // if there is only 1 fragment on the backstack, it is a 'main' fragment and we have
        //  nowhere to return to, but exit
        if(getSupportFragmentManager().getBackStackEntryCount()<2){
            finish();
        }
        super.onBackPressed();
    }

    /**
     * Release resource on destroy
     */
    @Override
    protected void onDestroy() {
        // unregister the message receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

}