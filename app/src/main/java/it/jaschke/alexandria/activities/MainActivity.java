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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.adapter.Callback;
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
    // use classname when logging
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    // when we rotate from portrait to landscape on a tablet we can keep the selected book here
    private String mEan;
    private String mBookTitle;

    /**
     * Used to store the last screen title.
     */
    // used to store the last screen mTitle
    private String mTitle;

    // reference to the app title textview in the toolbar
    private TextView mToolbarTitle;

    public static boolean IS_TABLET = false;

    private BroadcastReceiver messageReciever;

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";

    // drawer position constants
    public static final int BOOKLIST_FRAGMENT_POSITION = 0;
    public static final int ADDBOOK_FRAGMENT_POSITION = 1;
    public static final int ABOUT_FRAGMENT_POSITION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IS_TABLET = isTablet();
        if(IS_TABLET){
            setContentView(R.layout.activity_main_tablet);
        }else {
            setContentView(R.layout.activity_main);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = (String)getTitle();

        // set the toolbar title to the name of the app
        mToolbarTitle.setText(R.string.app_name);

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // send message to bookservice to delete existing not-saved books from previous session
        Intent bookIntent = new Intent(this, BookService.class);
        bookIntent.setAction(BookService.DELETE_NOT_SAVED);
        startService(bookIntent);

        // prevent the keyboard from appearing already oncreate mainactivity
        Utility.hideKeyboardFromActivity(this);
    }

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
                .addToBackStack((String) mTitle)
                .commit();
        Utility.hideKeyboardFromActivity(this);
    }

    public void setTitle(int titleId) {
        mTitle = getString(titleId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }else if (item.getItemId() == android.R.id.home) {

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

    public void toggleToolbarDrawerIndicator(boolean backToHome) {
        // if we are not on a tablet in landscape mode
        if(findViewById(R.id.right_container) == null) {
            navigationDrawerFragment.toggleToolbarDrawerIndicator(backToHome);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

    @Override
    public void onItemSelected(String ean, String title) {
        mEan = ean;
        mBookTitle = title;
        // create a bundle to pass the ean to the detail fragment
        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);
        args.putString(BookDetail.TITLE_KEY, title);

        BookDetail fragment = new BookDetail();
        fragment.setArguments(args);

        int id = R.id.container;
        if(findViewById(R.id.right_container) != null){
            id = R.id.right_container;
        }
        // replace the contents of the container element with the detail fragment and add it to the backstack
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(id, fragment);
        fragmentTransaction.addToBackStack(getString(R.string.detail));
        fragmentTransaction.commit();

        // toggle the hamburger icon for the back icon
        toggleToolbarDrawerIndicator(true);

        // hide the keyboard when we navigate to the bookdetail fragment
        Utility.hideKeyboardFromActivity(this);

    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra(MESSAGE_KEY)!=null){
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }


    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount()<2){
            finish();
        }
        super.onBackPressed();
    }


}