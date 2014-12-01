package ru.ifmo.md.lesson8;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Double;
import java.lang.Integer;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class MyLoaderIntentService extends IntentService {

    public Uri forecastUriCurrCity;
    public Uri currCityUri;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public MyLoaderIntentService() {
        super("MyLoaderIntentService");
    }

    private String getXmlFromUrl(String urlString) {
        StringBuffer output = new StringBuffer("");
        try {
            InputStream stream;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                stream = httpConnection.getInputStream();

                BufferedReader buffer = new BufferedReader(
                        new InputStreamReader(stream));
                String s = "";
                while ((s = buffer.readLine()) != null)
                    output.append(s);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onHandleIntent(Intent intent)  {

        int mode = intent.getIntExtra("mode", 1);

        String link = intent.getStringExtra("link");
        String name = intent.getStringExtra("city_name");
        int city_id = intent.getIntExtra("city_id", 0);

        if (mode == 2) {
            Cursor c = getContentResolver().query(MyContentProvider.TABLE_CITIES_URI, null, MyContentProvider.COLUMN_CITY_NAME + " = '" + name +"'", null, null, null);

            if (c.getCount() == 0) {
                ContentValues cv = new ContentValues();
                cv.put(MyContentProvider.COLUMN_CITY_NAME, name);
                cv.put(MyContentProvider.COLUMN_WEATHER_ICON, "10d");
                cv.put(MyContentProvider.COLUMN_TEMP, "-1°C..+1°C");
                cv.put(MyContentProvider.COLUMN_WIND_SPEED, "0 mps -");
                cv.put(MyContentProvider.COLUMN_PRESSURE, "0 hPa");
                currCityUri = getContentResolver().insert(MyContentProvider.TABLE_CITIES_URI, cv);
            }
            else {
                currCityUri = Uri.withAppendedPath(MyContentProvider.TABLE_CITIES_URI, Long.toString(city_id));
            }
        }
        else {
            forecastUriCurrCity = Uri.withAppendedPath(MyContentProvider.TABLE_FORECAST_URI, Long.toString(city_id));
        }

        String xml;

        try {
            xml = getXmlFromUrl(link);
            InputStream stream = new ByteArrayInputStream(xml.getBytes());
            (new MySAXParser(mode, city_id)).parse(stream);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MySAXParser {
        private int mode, city_id;

        MySAXParser(int mode, int c) {
            this.mode = mode;
            this.city_id = c;
        }

        public void parse(InputStream is) throws IOException, SAXException, ParserConfigurationException {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

            DefaultHandler saxHandler = new DefaultHandler();

            switch (mode) {
                case 1: {
                    getContentResolver().delete(forecastUriCurrCity, null, null);
                    saxHandler = new ForecastParserHandler();
                    break;
                }
                case 2: {
                    saxHandler = new CurrentParserHandler();
                    break;
                }
            }

            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(is));
        }
    }


    public class CurrentParserHandler extends DefaultHandler {

        private ContentValues node;
        private String windSpeed;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("current")) {
                node = new ContentValues();
            }
            if (node != null) {
                if (qName.equals("city")) {
                    node.put(MyContentProvider.COLUMN_CITY_NAME, attributes.getValue("name"));
                } else if (qName.equals("pressure")) {
                    node.put(MyContentProvider.COLUMN_PRESSURE, Integer.toString(attributes.getValue("value")) + " hPa");
                } else if (qName.equals("temperature")) {
                    int temp = (int) Double.parseDouble(attributes.getValue("min"));
                    String tempString = Integer.toString(temp) + "°C..";
                    if (temp > 0) {
                        tempString = "+" + tempString;
                    }
                    temp = (int) Double.parseDouble(attributes.getValue("max"));
                    if (temp > 0) {
                        tempString += "+";
                    }
                    tempString = Integer.toString(temp) + "°C";
                    node.put(MyContentProvider.COLUMN_TEMP, tempString);
                } else if (qName.equals("speed")) {
                    windSpeed = Integer.toString((int) Double.parseDouble(attributes.getValue("value"))) + " mps ";
                } else if (qName.equals("direction")) {
                    node.put(MyContentProvider.COLUMN_WIND, windSpeed + attributes.getValue("code"));
                } else if (qName.equals("weather")) {
                    node.put(MyContentProvider.COLUMN_WEATHER_ICON, attributes.getValue("icon"));
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (node != null) {
                if (qName.equals("current")) {
                    int y = getContentResolver().update(currCityUri, node, null, null);
                    node = null;
                }
            }
        }
    }


    public class ForecastParserHandler extends DefaultHandler {

        private ContentValues node;
        private String windDirection;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("time")) {
                node = new ContentValues();
                node.put(MyContentProvider.COLUMN_DATE, attributes.getValue("day"));
            }
            if (node != null) {
                if (qName.equals("symbol")) {
                    node.put(MyContentProvider.COLUMN_WEATHER_ICON, attributes.getValue("var"));
                } else if (qName.equals("windDirection")) {
                    windDirection = attributes.getValue("code");
                } else if (qName.equals("windSpeed")) {
                    node.put(MyContentProvider.COLUMN_WIND, Integer.toString((int) Double.parseDouble(attributes.getValue("mps"))) + " mps " + windDirection);
                } else if (qName.equals("temperature")) {
                    int temp = (int) Double.parseDouble(attributes.getValue("min"));
                    String tempString = Integer.toString(temp) + "°C..";
                    if (temp > 0) {
                        tempString = "+" + tempString;
                    }
                    temp = (int) Double.parseDouble(attributes.getValue("max"));
                    if (temp > 0) {
                        tempString += "+";
                    }
                    tempString = Integer.toString(temp) + "°C";
                    node.put(MyContentProvider.COLUMN_TEMP, tempString);
                } else if (qName.equals("pressure")) {
                    node.put(MyContentProvider.COLUMN_PRESSURE, Integer.toString((int) Double.parseDouble(attributes.getValue("value"))) + " hPa");
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (node != null) {
                if (qName.equals("time")) {
                    getContentResolver().insert(forecastUriCurrCity, node);
                    node = null;
                }
            }
        }
    }
}
