package youtube.android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import youtube.android.Account.LoginActivity;
import youtube.android.Comments.CommentsActivity;
import youtube.android.model.Feed;
import youtube.android.model.entry.Entry;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.ToolbarWidgetWrapper;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String BASE_URL = "https://www.reddit.com/r/";

    URLS urls= new URLS();

    private Button btnRefreshFeed;

    private EditText mFeedName;

    private String currentFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"onCreate: starting.");
        btnRefreshFeed=(Button)findViewById(R.id.btnRefreshFeed);
        mFeedName=(EditText)findViewById(R.id.etFeedName);

        setupToolBar();

        init();

        btnRefreshFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String feedName=mFeedName.getText().toString();
                if (!feedName.equals("")) {
                    currentFeed=feedName;
                    init();
                }
                else {
                    init();
                }
            }
        });

    }



    private void setupToolBar() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "onMenuItemClick: clicked menu item: "+item);
                switch (item.getItemId()) {
                    case R.id.navLogin:
                        Intent intent=new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                }
                return false;
            }
        });
    }

    private void init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urls.BASE_URL)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build();

        FeedAPI feedAPI = retrofit.create(FeedAPI.class);

        Call<Feed> call = feedAPI.getFeed(currentFeed);

        call.enqueue(new Callback<Feed>() {
            @Override
            public void onResponse(Call<Feed> call, Response<Feed> response) {
                // Log.d(TAG, "on Response: feed: " + response.body().toString());
                Log.d(TAG, "on Response: Server Response: " + response.toString());

                List<Entry> entrys=response.body().getEntrys();
                Log.d(TAG, "on Response: entrys: " + response.body().getEntrys());
//                Log.d(TAG, "on Response: author: " + entrys.get(0).getAuthor());
//                Log.d(TAG, "on Response: updated: " + entrys.get(0).getUpdated());
//                Log.d(TAG, "on Response: title: " + entrys.get(0).getTitle());

                final ArrayList<Post> posts=new ArrayList<>();

                for (int i=0;i<entrys.size();i++) {
                    ExtractXML extractXML1 = new ExtractXML("<a href=", entrys.get(i).getContent());
                    List<String> postContent = extractXML1.start();
                    String realPostURL=postContent.get(postContent.size()-1);
                    ExtractXML extractXML2 = new ExtractXML("<img src=", entrys.get(i).getContent());
                    try {
                        postContent.add(extractXML2.start().get(0));
                    } catch (NullPointerException e) {
                        postContent.add(null);
                        Log.e(TAG, "onResponse: NullPointerException: " + e.getMessage());
                    }
                    catch (IndexOutOfBoundsException e) {
                        postContent.add(null);
                        Log.e(TAG, "onResponse: IndexOutOfBoundsException: " + e.getMessage());
                    }
                    int lastPosition=postContent.size();
                    try {
                        posts.add(new Post(entrys.get(i).getTitle(),
                            entrys.get(i).getAuthor().getName(),
                            entrys.get(i).getUpdated(),
                            realPostURL,
                            postContent.get(lastPosition-1), entrys.get(i).getId()));
                    } catch (NullPointerException e) {
                        posts.add(new Post(entrys.get(i).getTitle(),
                                "None",
                                entrys.get(i).getUpdated(),
                                postContent.get(0),
                                postContent.get(lastPosition-1), entrys.get(i).getId()));
                        Log.e(TAG, "onResponse: NullPointerException: "+e.getMessage());
                    }

                }

                for (int j=0;j<posts.size();j++) {
                    Log.d(TAG, "onResponse: \n " +
                            "PostURL: "+posts.get(j).getPostURL()+"\n "+
                            "ThumbnailURL: "+ posts.get(j).getThumbnailURL()+"\n "+
                            "Title: "+posts.get(j).getTitle()+"\n "+
                            "Author: "+posts.get(j).getAuthor()+"\n "+
                            "updated: " +posts.get(j).getDate_updated()+"\n " +
                            "Post ID: " +posts.get(j).getId()+"\n ");
                }
                ListView listView=(ListView) findViewById(R.id.ListView);
                CustomListAdapter customListAdapter = new CustomListAdapter(MainActivity.this, R.layout.card_layout_main, posts);
                listView.setAdapter(customListAdapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Log.d(TAG, "onItemClick: "+ posts.get(i).toString());
                        Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
                        intent.putExtra("@string/post_url", posts.get(i).getPostURL());
                        intent.putExtra("@string/post_thumbnail", posts.get(i).getThumbnailURL());
                        intent.putExtra("@string/post_title", posts.get(i).getTitle());
                        intent.putExtra("@string/post_author", posts.get(i).getAuthor());
                        intent.putExtra("@string/post_updated", posts.get(i).getDate_updated());
                        intent.putExtra("@string/post_id", posts.get(i).getId());
                        startActivity(intent);
                    }
                });

            }

            @Override
            public void onFailure(Call<Feed> call, Throwable t) {
                Log.e(TAG, "onFailure: Unable to retrieve RSS: " + t.getMessage());
                Toast.makeText(MainActivity.this, "An Error Occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_menu, menu);
        return true;
    }
}