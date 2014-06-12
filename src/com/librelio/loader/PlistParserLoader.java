package com.librelio.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.librelio.LibrelioApplication;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.event.UpdateMagazinesEvent;
import com.librelio.model.DictItem;
import com.librelio.model.Magazine;
import com.librelio.model.PlistItem;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.AssetsUtils;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import de.greenrobot.event.EventBus;

public class PlistParserLoader extends AsyncTaskLoader<ArrayList<DictItem>> {

    private static final String TAG = "PlistParserLoader";
    private final String plistName;
    private final boolean hasTestMagazine;

    public PlistParserLoader(Context context, String plistName, boolean hasTestMagazine) {
        super(context);
        this.plistName = plistName;
        this.hasTestMagazine = hasTestMagazine;
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

        PlistItem plistItem = new PlistItem(plistName, "", getContext());

        ArrayList<DictItem> magazines = new ArrayList<DictItem>();

        if (hasTestMagazine) {
            magazines.add(new Magazine(MagazineManager.TEST_FILE_NAME, "TEST", "test", "", getContext()));
        }

        //Convert plist to String for parsing
        String pList = AssetsUtils.getStringFromFilename(getContext(), plistItem.getFilename());

        if (pList == null) {
            return null;
        }

        try {
            //Parsing
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

            for (DictItem magazine : magazines) {
                if (magazine instanceof Magazine)
                MagazineManager.updateMagazineDetails(getContext(), (Magazine) magazine);
            }
            EventBus.getDefault().post(new UpdateMagazinesEvent(plistName, magazines));

            for (final DictItem magazine : magazines) {
            	// Download thumbnail png
                File png = new File(magazine.getPngPath());
                if (!png.exists()) {
                    Log.d(TAG, "Image download: " + magazine.getPngPath());
                	Request request = new Request.Builder().url(magazine.getPngUrl()).build();
                	
                	LibrelioApplication.getOkHttpClient().newCall(request).enqueue(new Callback() {
						
						@Override
						public void onResponse(Response response) throws IOException {
							if (response.code() == 200) {
							IOUtils.copy(response.body().byteStream(), new FileOutputStream(magazine.getPngPath()));
		                    EventBus.getDefault().post(new InvalidateGridViewEvent());
							}
							
						}
						
						@Override
						public void onFailure(Request arg0, Throwable arg1) {
							Log.d(TAG, "magazine thumbnail download failed " + magazine.getPngUrl());
						}
					});
                } else {
    //                Log.d(TAG, magazine.getPngPath() + " already exist");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "plist = " + pList);
            e.printStackTrace();
        }
        return magazines;
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged())
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
