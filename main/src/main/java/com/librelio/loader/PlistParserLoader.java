package com.librelio.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.librelio.event.UpdateMagazinesEvent;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.PlistItem;
import com.librelio.utils.StorageUtils;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class PlistParserLoader extends AsyncTaskLoader<ArrayList<DictItem>> {

    private static final String TAG = "PlistParserLoader";
    private final String plistName;

    public PlistParserLoader(Context context, String plistName) {
        super(context);
        this.plistName = plistName;
        setUpdateThrottle(2000);
        onContentChanged();
    }

    @Override
    public ArrayList<DictItem> loadInBackground() {

        ArrayList<DictItem> magazines = parsePlist(plistName);
        return magazines;
    }

    @Override
    public void deliverResult(ArrayList<DictItem> data) {
        super.deliverResult(data);
    }

    private ArrayList<DictItem> parsePlist(String plistName) {
    	
//    	if (BuildConfig.DEBUG) {
//    		Log.d(getClass().getSimpleName(), "parsing plist: " + plistName);
//    	}

        PlistItem plistItem = new PlistItem(getContext(), "", plistName);

        ArrayList<DictItem> magazines = new ArrayList<DictItem>();

        //Convert plist to String for parsing
        String pList = StorageUtils.getFilePathFromAssetsOrLocalStorage(getContext(), plistItem
                .getItemFileName());

        if (pList == null) {
            return null;
        }

        try {
            //Parse plist
            PListXMLHandler handler = new PListXMLHandler();
            PListXMLParser parser = new PListXMLParser();
            parser.setHandler(handler);
            parser.parse(pList);
            PList list = ((PListXMLHandler) parser.getHandler()).getPlist();
            Array arr = (Array) list.getRootElement();
            for (int i = 0; i < arr.size(); i++) {
                Dict dict = (Dict) arr.get(i);
                DictItem item = DictItem.parse(getContext(), dict);
                magazines.add(item);
            }

//            for (DictItem magazine : magazines) {
//                if (magazine instanceof Magazine) {
                	// TODO don't always need to save details in database
//                MagazineManager.updateMagazineDetails(getContext(), (Magazine) magazine);
//                }
//            }
            EventBus.getDefault().post(new UpdateMagazinesEvent(plistName, magazines));
        } catch (Exception e) {
            Log.d(TAG, "plist = " + pList);
            e.printStackTrace();
        }
        
//    	if (BuildConfig.DEBUG) {
//    		Log.d(getClass().getSimpleName(), "finished parsing plist: " + plistName);
//    	}
        return magazines;
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
