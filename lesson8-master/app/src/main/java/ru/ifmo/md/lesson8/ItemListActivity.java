package ru.ifmo.md.lesson8;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details
 * (if present) is a {@link ItemDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ItemListFragment.Callbacks} interface
 * to listen for item selections.
 */

public class ItemListActivity extends FragmentActivity
        implements ItemListFragment.Callbacks, LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    final String[] cities = new String[] {"New-Work", "Rio-de-Janeiro", "London", "Paris", "Berlin",
        "Moscow", "Cairo", "Delhi", "Beijing", "Tokyo", "Sydney"};
    final String request = "http://api.openweathermap.org/data/2.5/weather?q=";

    private boolean mTwoPane;

    SimpleCursorAdapter cursorAdapter;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] Columns = new String[] {MyContentProvider.COLUMN_ID, MyContentProvider.COLUMN_CITY_NAME,
                MyContentProvider.COLUMN_WEATHER_ICON, MyContentProvider.COLUMN_TEMP,
                MyContentProvider.COLUMN_WIND, MyContentProvider.COLUMN_PRESSURE};
        return new CursorLoader(this, MyContentProvider.TABLE_CITIES_URI, Columns, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        for (int i = 0; i < cities.length; i++) {
            fetchCurrCityWeather(cities[i], i + 1);
        }

        String[] Columns = new String[] {MyContentProvider.COLUMN_CITY_NAME, MyContentProvider.COLUMN_WEATHER_ICON,
                MyContentProvider.COLUMN_TEMP, MyContentProvider.COLUMN_WIND, MyContentProvider.COLUMN_PRESSURE};
        int[] elements = new int[] {R.id.city_name, R.id.current_image,
                R.id.current_temperature, R.id.current_wind, R.id.current_pressure};

        cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_element, null, Columns, elements, 0);
        ((ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list)).setListAdapter(cursorAdapter);

        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                    if (view.getId() == R.id.current_image) {
                        ImageView imageView = (ImageView) view;
                        String imgName = "a" + cursor.getString(3);
                        int imgResource = getResources().getIdentifier(imgName, "drawable", getPackageName());
                        imgView.setImageBitmap(BitmapFactory.decodeResource(getResources(), imgResource));
                        return true;
                }
                return false;
            }
        });

        Button updateButton = (Button) findViewById(R.id.update_button);
        Log.d("Button", Boolean.toString(updateButton == null));
        updateButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < cities.length; i++) {
                    fetchCurrCityWeather(cities[i], i + 1);
                }
                Toast.makeText(getApplicationContext(), "Update started", Toast.LENGTH_LONG).show();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list)).setActivateOnItemClick(true);
        }
    }

    void fetchCurrCityWeather(String city, int city_id) {
        String link = request + city + "&mode=xml&units=metric";
        Intent loadFeed = new Intent(ItemListActivity.this, MyLoaderIntentService.class);
        loadFeed.putExtra("link", link);
        loadFeed.putExtra("city_name", city);
        loadFeed.putExtra("city_id", city_id);
        loadFeed.putExtra("mode", 2);
        startService(loadFeed);
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
            arguments.putString("city_name", cities[Integer.parseInt(id) - 1]);
            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(ItemDetailFragment.ARG_ITEM_ID, Integer.toString(Integer.parseInt(id) - 1));
            Log.d("onItemSelected",cities[Integer.parseInt(id) - 1]+ " "+ (Integer.parseInt(id) - 1));
            detailIntent.putExtra("city_name", cities[Integer.parseInt(id) - 1]);
            startActivity(detailIntent);
        }
    }
}
