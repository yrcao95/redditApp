package youtube.android;


import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtractXML {

    private static final String TAG="ExtractXML";

    private String tag;

    private String xml;

    private String endtag;

    public ExtractXML(String tag, String xml) {
        this.tag = tag;
        this.xml = xml;
        this.endtag="NONE";
    }

    public ExtractXML(String tag, String xml, String endtag) {
        this.tag = tag;
        this.xml = xml;
        this.endtag = endtag;
    }

    public List<String> start() {
        List<String> result = new ArrayList<>();
        String[] splitXML = null;
        String marker=null;

        if (endtag.equals("NONE")) {
            marker="\"";
            splitXML=xml.split(tag+marker);
        }
        else {
            marker=endtag;
            splitXML=xml.split(tag);
        }
//        Log.d(TAG, xml);
        //Log.d(TAG, Arrays.toString(splitXML));
        int count=splitXML.length;
        for (int i=1;i<count;i++) {
            String temp=splitXML[i];
            int index=temp.indexOf(marker);
//            Log.d(TAG, "start: index: "+index);

            temp=temp.substring(0, index);
            Log.d(TAG,"start: snipped: "+temp);
            result.add(temp);
        }
        return result;
    }
}
