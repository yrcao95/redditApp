package youtube.android.Comments;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import youtube.android.Account.CheckLogin;
import youtube.android.Account.LoginActivity;
import youtube.android.ExtractXML;
import youtube.android.FeedAPI;
import youtube.android.MainActivity;
import youtube.android.R;
import youtube.android.URLS;
import youtube.android.WebViewActivity;
import youtube.android.model.Feed;
import youtube.android.model.entry.Entry;

public class CommentsActivity  extends AppCompatActivity {

    private static final String TAG = "CommentsActivity";

    URLS urls=new URLS();

    private static final String BASE_URL = "https://www.reddit.com/r/";

    private static String postURL;

    private static String postTitle;

    private static String postAuthor;

    private static String postUpdated;

    private static String postThumbnailURL;

    private static String postID;

    private String modhash;
    private String cookie;
    private String username;

    private int defaultImage;

    private String currentFeed;
    private ListView mListView;

    private ArrayList<Comment> mComments;

    private ProgressBar mProgressBar;

    private TextView progressText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        mProgressBar=(ProgressBar)findViewById(R.id.commentsLoadingProgressBar);
        progressText=(TextView) findViewById(R.id.progressText);
        Log.d(TAG, "onCreate: Started ...");
        setupToolBar();

        getSessionParams();

        mProgressBar.setVisibility(View.VISIBLE);
        setupImageLoader();

        initPost();

        init();

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
                        Intent intent=new Intent(CommentsActivity.this, LoginActivity.class);
                        startActivity(intent);
                }
                return false;
            }
        });
    }

    private void init() {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(urls.BASE_URL)
                .addConverterFactory(SimpleXmlConverterFactory.create()).build();

        FeedAPI feedAPI = retrofit.create(FeedAPI.class);

        Call<Feed> call=feedAPI.getFeed(currentFeed);

        call.enqueue(new Callback<Feed>() {
            @Override
            public void onResponse(Call<Feed> call, Response<Feed> response) {
                Log.d(TAG, "on Response: Server Response: " + response.toString());
                mComments=new ArrayList<>();
                List<Entry> entrys=response.body().getEntrys();
                for (int i=0;i<entrys.size();i++) {
                    ExtractXML extractXML=new ExtractXML("<div class=\"md\"><p>", entrys.get(i).getContent(), "</p>");

                    List<String> commentDetails=extractXML.start();

                    try{
                        mComments.add(new Comment(
                            commentDetails.get(0),
                            entrys.get(i).getAuthor().getName(),
                            entrys.get(i).getUpdated(),
                            entrys.get(i).getId()
                        ));
                    } catch (IndexOutOfBoundsException e) {
                        mComments.add(new Comment(
                                "ERROR READING COMMENT",
                                "NONE",
                                "NONE",
                                "NONE"
                        ));
                        Log.e(TAG, "onResponse: IndexOutOfBoundsException: "+e.getMessage());
                    } catch (NullPointerException e) {
                        mComments.add(new Comment(
                                commentDetails.get(0),
                                "NONE",
                                entrys.get(i).getUpdated(),
                                entrys.get(i).getId()
                        ));
                        Log.e(TAG, "onResponse: IndexOutOfBoundsException: "+e.getMessage());
                    }

                }
                mListView=(ListView)findViewById(R.id.commentsListView);
                CommentListAdapter adapter = new CommentListAdapter(CommentsActivity.this, R.layout.comments_layout, mComments);
                mListView.setAdapter(adapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        getUserComment(mComments.get(i).getId());
                    }
                });
                mProgressBar.setVisibility(View.GONE);
                progressText.setText("");

            }

            @Override
            public void onFailure(Call<Feed> call, Throwable t) {
                Log.e(TAG, "onFailure: Unable to retrieve RSS: " + t.getMessage());
                Toast.makeText(CommentsActivity.this, "An Error Occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initPost() {
        Intent incomingIntent=getIntent();
        postURL=incomingIntent.getStringExtra("@string/post_url");

        postThumbnailURL=incomingIntent.getStringExtra("@string/post_thumbnail");

        postTitle=incomingIntent.getStringExtra("@string/post_title");

        postAuthor=incomingIntent.getStringExtra("@string/post_author");

        postUpdated=incomingIntent.getStringExtra("@string/post_updated");

        postID=incomingIntent.getStringExtra("@string/post_id");

        TextView title=(TextView) findViewById(R.id.postTitle);
        TextView author=(TextView) findViewById(R.id.postAuthor);
        TextView updated=(TextView) findViewById(R.id.postUpdated);
        ImageView thumbnail=(ImageView) findViewById(R.id.postThumbnail);
        Button btnReply=(Button)findViewById(R.id.btnPostReply);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.postLoadingProgressBar);

        title.setText(postTitle);
        author.setText(postAuthor);
        updated.setText(postUpdated);
        displayImage(postThumbnailURL, thumbnail, progressBar);
        try {
            Log.d(TAG, postURL);
            String[] splitURL=postURL.split(urls.BASE_URL);
            currentFeed=splitURL[1];
            Log.d(TAG, "initPost: current feed: "+currentFeed);}
        catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "init post: ArrayIndexOutOfBoundsException: "+ e.getMessage());
        }

        btnReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: reply: ");
                getUserComment(postID);
            }
        });

        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: OpeningURL in webview: "+postURL);
                Intent intent=new Intent(CommentsActivity.this, WebViewActivity.class);
                intent.putExtra("url", postURL);
                startActivity(intent);
            }
        });

    }


    private void getUserComment(final String post_id) {
        final Dialog dialog = new Dialog(CommentsActivity.this);
        dialog.setTitle("dialog");
        dialog.setContentView(R.layout.comment_input_dialog);

        int width=(int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height=(int)(getResources().getDisplayMetrics().heightPixels*0.95);

        dialog.getWindow().setLayout(width,height);
        dialog.show();

        Button btnPostComment=(Button) dialog.findViewById(R.id.btnPostComment);
        final EditText comment=(EditText)dialog.findViewById(R.id.dialogComment);

        btnPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Attempting to post comment. ");
                // post comment stuff for retrofit
                Retrofit retrofit=new Retrofit.Builder()
                        .baseUrl(urls.COMMENT_URL)
                        .addConverterFactory(GsonConverterFactory.create()).build();

                FeedAPI feedAPI = retrofit.create(FeedAPI.class);

                HashMap<String, String> headerMap=new HashMap<>();
                headerMap.put("User-Agent", username);
                headerMap.put("X-Modhash", modhash);
                headerMap.put("cookie", "reddit_session=" + cookie);

                Log.d(TAG, "onClick: btnPostComment: \n" +
                        "username: " + username + "\n" +
                        "modhash: " + modhash + "\n" +
                        "cookie" + cookie + "\n");


                String theComment=comment.getText().toString();
                Log.d(TAG, "onClick: Trying to sumbit comment: "+ theComment);



                Call<CheckComment> call=feedAPI.submitComment(headerMap, "comment", post_id, theComment);

                call.enqueue(new Callback<CheckComment>() {
                    @Override
                    public void onResponse(Call<CheckComment> call, Response<CheckComment> response) {
                        try {
                            Log.d(TAG, "onResponse: Server Response: " + response.toString());
                            String postSuccess = response.body().getSuccess();
                            if (postSuccess.equals("true")) {
                                dialog.dismiss();
                                Toast.makeText(CommentsActivity.this, "Comment posted successfully!", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                dialog.dismiss();
                                Toast.makeText(CommentsActivity.this, "Comment not posted!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NullPointerException e ) {
                            Log.e(TAG, "onResponse: onResponse: NullPointerException: " +e.getMessage() );
                        }
                    }

                    @Override
                    public void onFailure(Call<CheckComment> call, Throwable t) {
                        Log.e(TAG, "onFailure: Unable to retrieve RSS: " +t.getMessage());
                        Toast.makeText(CommentsActivity.this, "An Error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void displayImage(String imageURL, ImageView imageView, final ProgressBar progressBar) {
        ImageLoader imageLoader = ImageLoader.getInstance();

//        int defaultImage = CommentsActivity.this.getResources().getIdentifier("@drawable/image_failed",null,CommentsActivity.this.getPackageName());

        //create display options
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true)
                .cacheOnDisc(true).resetViewBeforeLoading(true)
                .showImageForEmptyUri(defaultImage)
                .showImageOnFail(defaultImage)
                .showImageOnLoading(defaultImage).build();

        //download and display image from url
        imageLoader.displayImage(imageURL, imageView, options, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupImageLoader(){
        // UNIVERSAL IMAGE LOADER SETUP
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                CommentsActivity.this)
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .discCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);
        // END - UNIVERSAL IMAGE LOADER SETUP

        defaultImage = CommentsActivity.this.getResources().getIdentifier("@drawable/image_failed",null,CommentsActivity.this.getPackageName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_menu, menu);
        return true;
    }

    private void getSessionParams () {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CommentsActivity.this);
        username=preferences.getString("@string/SessionUsername", "");
        cookie=preferences.getString("@string/SessionCookie", "");
        modhash=preferences.getString("@string/SessionModhash", "");

        Log.d(TAG, "getSessionParams: Fetching session variables: \n" +
                "username: " + username + "\n" +
                "modhash: " + modhash + "\n" +
                "cookie: " + cookie + "\n");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume: Resuming Activity: ");
        getSessionParams();
    }
}
