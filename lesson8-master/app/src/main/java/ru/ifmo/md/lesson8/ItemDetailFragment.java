package ru.ifmo.md.lesson8;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


import ru.ifmo.md.lesson8.dummy.DummyContent;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    public static final String linkPart1 = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
    public static final String linkPart2 = "&mode=xml&units=metric&cnt=7";

    String city_name;

    SimpleCursorAdapter cursorAdapter;
    Uri uri;
    /**
     * The dummy content this fragment is presenting.
     */

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    void fetchCityForecast(String city, int city_id) {
        String link = linkPart1 + city + linkPart2;
        Intent loadFeed = new Intent(getActivity(), MyLoaderIntentService.class);
        loadFeed.putExtra("link", link);
        loadFeed.putExtra("city_name", city);
        loadFeed.putExtra("city_id", city_id);
        loadFeed.putExtra("mode", 1);
        getActivity().startService(loadFeed);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);
        ListView forecastListView = (ListView) rootView.findViewById(R.id.forecast_listview);
        forecastListView.setAdapter(cursorAdapter);
        getLoaderManager().initLoader(1, null, this);
        return rootView;
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] Columns = new String[]{
                MyContentProvider.COLUMN_ID,
                MyContentProvider.COLUMN_CITY_ID,
                MyContentProvider.COLUMN_DATE,
                MyContentProvider.COLUMN_WEATHER_ICON,
                MyContentProvider.COLUMN_TEMP,
                MyContentProvider.COLUMN_WIND,
                MyContentProvider.COLUMN_PRESSURE};
        return new android.support.v4.content.CursorLoader(getActivity(), uri, Columns, null, null, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            int city_id = Integer.parseInt(getArguments().getString(ARG_ITEM_ID));
            city_name = getArguments().getString("city_name");

            fetchCityForecast(city_name, city_id);

            uri = Uri.withAppendedPath(MyContentProvider.TABLE_FORECAST_URI, Integer.toString(city_id));
            String[] Columns = new String[] {MyContentProvider.COLUMN_DATE, MyContentProvider.COLUMN_WEATHER_ICON,
                    MyContentProvider.COLUMN_TEMP, MyContentProvider.COLUMN_WIND, MyContentProvider.COLUMN_PRESSURE};
            int[] elements = new int[] {R.id.forecast_date, R.id.forecast_image,
                    R.id.forecast_temperature, R.id.forecast_wind, R.id.forecast_pressure};
            cursorAdapter = new SimpleCursorAdapter(getActivity(), R.layout.forecast_list_element, null, Columns, elements, 0);

            cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int i) {
                    if (view.getId() == R.id.forecast_image) {
                        ImageView imgView = (ImageView) view;
                        String imgName = "a" + cursor.getString(4);
                        int imgResource = getResources().getIdentifier(imgName, "drawable", getActivity().getPackageName());
                        imgView.setImageBitmap(BitmapFactory.decodeResource(getResources(), imgResource));
                        return true;
                    }
                    return false;
                }
            });

        }
    }
}
