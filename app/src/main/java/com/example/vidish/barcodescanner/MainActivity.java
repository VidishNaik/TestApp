package com.example.vidish.barcodescanner;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{

    TextView textView;
    int i;
    TestAsyncTask testAsyncTask;
    List<Movies> list;
    final int movies_count = 6063;
    int count = 0;
    public static SQLiteDatabase db;
    private SearchView mSearchView;
    ListView listView;
    EditText editText;
    static ImageView image;
    ProgressDialog progressDialog;
    Button scan,create,show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        i = 1;
        list = new ArrayList<>();

        db = openOrCreateDatabase("Movies",MODE_PRIVATE,null);
        //db.execSQL("DROP TABLE IF EXISTS movie;");
        db.execSQL("CREATE TABLE IF NOT EXISTS movie (id int primary key,title varchar,image_url varchar);");
        //TODO id,imdb_code,title,slug,year,rating,image

//        testAsyncTask = new TestAsyncTask();
//        testAsyncTask.execute("https://yts.ag/api/v2/list_movies.json?limit=50&page="+i);
//        progressDialog = new ProgressDialog(MainActivity.this);
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.setIndeterminate(true);
//        progressDialog.setTitle("Fetching data");
//        progressDialog.setMessage("This may take upto 5 minutes depending on your Internet Connection.");
//        progressDialog.setCancelable(false);
//        progressDialog.show();

        listView = (ListView) findViewById(R.id.list2);

        editText = (EditText) findViewById(R.id.edittext);
        scan = (Button) findViewById(R.id.button_scan);
        create = (Button) findViewById(R.id.create);
        show = (Button) findViewById(R.id.show);
        image = (ImageView) findViewById(R.id.imageview);
        textView = (TextView) findViewById(R.id.text);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction()))
        {
            Toast.makeText(MainActivity.this, getIntent().getData().getPathSegments().get(1) + "", Toast.LENGTH_SHORT).show();
        }
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
                    fOut = openFileOutput("file.txt", MODE_PRIVATE);
                    OutputStreamWriter osw = new OutputStreamWriter(fOut);
                    for(int i = 0; i < list.size() ; i++)
                        osw.write(list.get(i).getId() + ",");
                    osw.flush();
                    osw.close();
                    Toast.makeText(MainActivity.this, "HOGAYA", Toast.LENGTH_SHORT).show();
                    textView.setText(Environment.getDataDirectory().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor result = db.rawQuery("SELECT * from movie where id = 12",null);
                result.moveToFirst();
                if(result.getCount() > 0)
                    textView.setText(result.getString(1));
                else
                    textView.setText("NOT FOUND");
                result.close();
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals(""))
                {
                    textView.setText("");
                    return;
                }
                else if(s.length() > 0)
                {
                    Cursor result = db.rawQuery("SELECT title,image_url from movie where id  = "+editText.getText().toString()+";", null);
                    result.moveToFirst();
                    if (result.getCount() > 0) {
                        volleyImageLoader(result.getString(1),MainActivity.this);
                    } else
                    {
                        textView.setText("NOT FOUND");
                        image.setImageBitmap(null);
                    }
                    result.close();
                }
                else
                    textView.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Associate searchable configuration with the SearchView
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //mSearchView.clearFocus();
                mSearchView.setQuery("",true);
            }
        });
        setupSearchView(searchItem);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() < 1)
        {
            changeView(1);
            return true;
        }
        changeView(0);
        Cursor result = db.rawQuery("SELECT title from movie where title like '%" + newText.replace("'", "''") + "%'", null);
        result.moveToFirst();
        if (result.getCount() > 0) {
            final String[] s = new String[result.getCount()];
            for (int i = 0; i < result.getCount(); i++) {
                s[i] = result.getString(0);
                result.moveToNext();
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.activity_list,s);
            listView.setAdapter(arrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(MainActivity.this, s[position], Toast.LENGTH_SHORT).show();
                }
            });
        }
        else
        {
            String[] s = {"No Movie Found"};
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.activity_list,s);
            ListView listView = (ListView) findViewById(R.id.list2);
            listView.setAdapter(arrayAdapter);
        }
        result.close();
        return false;
    }

    private void changeView(int i) {
        if(i == 1)
        {
            listView.setVisibility(View.INVISIBLE);
            image.setVisibility(View.VISIBLE);
            scan.setVisibility(View.VISIBLE);
            create.setVisibility(View.VISIBLE);
            show.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);
        }
        else
        {
            listView.setVisibility(View.VISIBLE);
            image.setVisibility(View.INVISIBLE);
            scan.setVisibility(View.INVISIBLE);
            create.setVisibility(View.INVISIBLE);
            show.setVisibility(View.INVISIBLE);
            editText.setVisibility(View.INVISIBLE);
        }
    }


    private void setupSearchView(MenuItem searchItem) {

        if (isAlwaysExpanded()) {
            mSearchView.setIconifiedByDefault(false);
        } else {
            searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM
                    | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        }

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            List<SearchableInfo> searchables = searchManager.getSearchablesInGlobalSearch();

            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            for (SearchableInfo inf : searchables) {
                if (inf.getSuggestAuthority() != null
                        && inf.getSuggestAuthority().startsWith("applications")) {
                    info = inf;
                }
            }
            mSearchView.setSearchableInfo(info);
        }

        mSearchView.setOnQueryTextListener(this);
    }

    protected boolean isAlwaysExpanded() {
        return false;
    }

    private class TestAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
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
        protected void onPostExecute(String jsonResponse) {
            extractMovies(jsonResponse);
            count = count + 50;
            if(count < movies_count)
            {
                textView.setText(""+i+" of " +(int) Math.ceil(movies_count/50)+" pages");
                i++;
                testAsyncTask = new TestAsyncTask();
                testAsyncTask.execute("https://yts.ag/api/v2/list_movies.json?limit=50&page="+i);
            }
            if(progressDialog.isShowing() && count >= movies_count)
            {
                progressDialog.dismiss();
                textView.setText("HOGAYA");
            }
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
                String title = arrayObject.getString("title");
                String url = arrayObject.getString("medium_cover_image");
                ContentValues cv = new ContentValues();
                cv.put("id",id);
                cv.put("title",title);
                cv.put("image_url",url);
                db.insertOrThrow("movie",null,cv);
                }
        } catch (JSONException e) {
            Log.v("MainActivity","JSONError " + e);
        }
    }


    public static synchronized void volleyImageLoader(String url, final Context context){
        ImageLoader imageLoader = AppSingleton.getInstance(context).getImageLoader();
        imageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Image Load Error: " + error.getMessage());
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {
                if (response.getBitmap() != null) {
                    image.setImageBitmap(response.getBitmap());
                }
            }
        });
    }
}
