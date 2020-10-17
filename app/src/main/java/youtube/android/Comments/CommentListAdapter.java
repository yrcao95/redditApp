package youtube.android.Comments;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;

import youtube.android.Post;
import youtube.android.R;

/**
 * Created by User on 4/4/2017.
 */

public class CommentListAdapter extends ArrayAdapter<Comment> {

    private static final String TAG = "CustomListAdapter";

    private Context mContext;
    private int mResource;
    private int lastPosition = -1;

    /**
     * Holds variables in a View
     */
    private static class ViewHolder {
        TextView commentTitle;

        TextView author;

        TextView date_updated;

        ProgressBar mProgressBar;

    }

    /**
     * Default constructor for the PersonListAdapter
     * @param context
     * @param resource
     * @param objects
     */
    public CommentListAdapter(Context context, int resource, ArrayList<Comment> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {



        //get the persons information
        String title = getItem(position).getComment();
        String author =getItem(position).getAuthor();
        String date_updated=getItem(position).getUpdated();


        try{


            //create the view result for showing the animation
            final View result;

            //ViewHolder object
            final ViewHolder holder;

            if(convertView == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(mResource, parent, false);
                holder= new ViewHolder();
                holder.commentTitle = (TextView) convertView.findViewById(R.id.comment);
                holder.author=(TextView)convertView.findViewById(R.id.commentAuthor);
                holder.date_updated=(TextView)convertView.findViewById(R.id.commentUpdated);
                holder.mProgressBar=(ProgressBar)convertView.findViewById(R.id.commentProgressBar);

                result = convertView;

                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
                result = convertView;
                holder.mProgressBar.setVisibility(View.VISIBLE);
            }


            lastPosition = position;

            holder.commentTitle.setText(title);
            holder.author.setText(author);
            holder.date_updated.setText(date_updated);
            holder.mProgressBar.setVisibility(View.GONE);

            //create the imageloader object


            return convertView;
        }catch (IllegalArgumentException e){
            Log.e(TAG, "getView: IllegalArgumentException: " + e.getMessage() );
            return convertView;
        }

    }

    /**
     * Required for setting up the Universal Image loader Library
     */

}