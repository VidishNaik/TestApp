package com.example.vidish.barcodescanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    int i;
    TestAsyncTask testAsyncTask;
    List<Movies> list;
    final int movies_count = 6020;
    int count = 0;
    public static SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        i = 1;
        list = new ArrayList<>();

        db = openOrCreateDatabase("Movies",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS movie (id int,title varchar);");
        db.execSQL("insert into movie values (5,'DONE');");
        db.execSQL("insert into movie values (6,'DONE HOGAYA');");
        db.execSQL("insert into movie values (7,'HOGAYA');");

        Cursor result = db.rawQuery("select * from movie where title like 'DONE%'",null);
        result.moveToFirst();
        Button scan = (Button) findViewById(R.id.scan);
        Button create = (Button) findViewById(R.id.create);
        Button show = (Button) findViewById(R.id.show);
        textView = (TextView) findViewById(R.id.text);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction()))
        {
            Toast.makeText(MainActivity.this, getIntent().getData().getPathSegments().get(1) + "", Toast.LENGTH_SHORT).show();
        }
        String abc = "";
//        while(!result.isLast())
//        {
//            abc = abc + result.getInt(0) + " " + result.getString(1);
//        }
        for(int i = 0; i < result.getCount() ; i = i + 2) {
            abc = abc + result.getInt(i%2) + " " + result.getString((i+1)%2);
            result.moveToNext();
        }
        textView.setText(abc + "Count : " + result.getCount());
        result.close();
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testAsyncTask = new TestAsyncTask();
                testAsyncTask.execute("https://yts.ag/api/v2/list_movies.json?limit=50&page="+i);
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileOutputStream fOut;
                try {
                    fOut = openFileOutput("file.txt",Context.MODE_PRIVATE);
                    OutputStreamWriter osw = new OutputStreamWriter(fOut);
                    for(int i = 0; i < list.size() ; i++)
                        osw.write(list.get(i).getId() + ",");
                    osw.flush();
                    osw.close();
                    Toast.makeText(MainActivity.this, "HOGAYA", Toast.LENGTH_SHORT).show();
                    textView.setText(Environment.getDataDirectory().toString());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputStream inputStream = openFileInput("file.txt");

                    if ( inputStream != null ) {
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String receiveString = "";
                        StringBuilder stringBuilder = new StringBuilder();

                        while ( (receiveString = bufferedReader.readLine()) != null ) {
                            stringBuilder.append(receiveString);
                        }

                        inputStream.close();
                        textView.setText(stringBuilder.toString());
                    }
                }
                catch (FileNotFoundException e) {
                    Log.e("login activity", "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e("login activity", "Can not read file: " + e.toString());
                }

            }
        });
    }


    private class TestAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        @Override
        protected String doInBackground(String... urls) {
            publishProgress();
            if (urls.length < 1 || urls[0] == null)
                return null;
            URL url;
            try {
                url = new URL(urls[0]);
            } catch (MalformedURLException exception) {
                Log.e("ClassSelector", "Error with creating URL", exception);
                return null;
            }
            String jsonResponse = "";
            publishProgress();
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
            }
            if (jsonResponse == null) {
                return null;
            }
            return jsonResponse;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Fetching data");
            progressDialog.setMessage("This may take upto 2 minutes depending on your Internet Connection.");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            extractMovies(jsonResponse);
            count = count + 50;
            if(count < movies_count)
            {
                Toast.makeText(MainActivity.this, "Page no : " + i, Toast.LENGTH_SHORT).show();
                i++;
                testAsyncTask = new TestAsyncTask();
                testAsyncTask.execute("https://yts.ag/api/v2/list_movies.json?limit=50&page="+i);
            }
            else
                textView.setText("Size = " + list.size() );
            progressDialog.dismiss();
        }
    }

    private String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        if (url == null) {
            return jsonResponse;
        }
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            jsonResponse = readFromStream(inputStream);
        } catch (IOException e) {
            jsonResponse = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private void extractMovies(String jsonResponse)
    {
        try {

            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONObject data=rootObject.optJSONObject("data");
            JSONArray movies = data.optJSONArray("movies");
            for(int i = 0 ; i < movies.length() ; i++)
            {
                JSONObject arrayObject=movies.getJSONObject(i);
                String id = arrayObject.getString("id");
                String imdb = arrayObject.getString("imdb_code");
                String title = arrayObject.getString("title");
                String slug = arrayObject.getString("slug");
                String rating = arrayObject.getString("rating");
                String runtime = arrayObject.getString("runtime");
                JSONArray genre = arrayObject.getJSONArray("genres");
                String[] genres = new String[genre.length()];
                for(int j = 0; j < genres.length ; j++)
                {
                    genres[j] = (String) genre.get(j);
                }
                String description = arrayObject.getString("description_full");
                String youtube = arrayObject.getString("yt_trailer_code");
                String cover = arrayObject.getString("large_cover_image");
                JSONArray torrents = arrayObject.getJSONArray("torrents");
                List<Torrent> torrentList = new ArrayList<>();
                for(int j = 0 ; j < torrents.length() ; j++)
                {
                    JSONObject torobj = torrents.getJSONObject(j);
                    String url = torobj.getString("url");
                    String hash = torobj.getString("hash");
                    String quality = torobj.getString("quality");
                    String seeds = torobj.getString("seeds");
                    String peers = torobj.getString("peers");
                    String size = torobj.getString("size");
                    torrentList.add(new Torrent(url,hash,quality,seeds,peers,size));
                }
                if(torrentList.size() == 1)
                {
                    list.add(new Movies(id,imdb,title,slug,rating,runtime,genres,description,youtube,cover,torrentList.get(0),null,null));
                }
                else if(torrentList.size() == 2)
                {
                    list.add(new Movies(id,imdb,title,slug,rating,runtime,genres,description,youtube,cover,torrentList.get(0),torrentList.get(1),null));
                }
                else if(torrentList.size() == 3)
                {
                    list.add(new Movies(id,imdb,title,slug,rating,runtime,genres,description,youtube,cover,torrentList.get(0),torrentList.get(1),torrentList.get(2)));
                }
                Log.v("MainActivity","ID = "+ id);
            }


        } catch (JSONException e) {
            Log.v("MainActivity","JSONError " + e);
        }
    }
}
