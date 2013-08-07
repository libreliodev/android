package com.librelio.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.model.DictItem;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.StorageUtils;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import de.greenrobot.event.EventBus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

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

    public static int downloadFromUrl(String sUrl, String filePath) {
        int count = -1;
        try {
            URL url = new URL(sUrl);
            URLConnection connection = url.openConnection();
            connection.connect();

//            int lenghtOfFile = connection.getContentLength();
//            Log.d(TAG, "downloadFromUrl Lenght of file: " + lenghtOfFile);

            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(filePath);

            byte data[] = new byte[1024];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            Log.e(TAG, "Problem with download: " + filePath, e);
        }
        return count;
    }

    @Override
    public ArrayList<DictItem> loadInBackground() {

        ArrayList<DictItem> magazines = extractMagazinesFromPlist(plistName);
        return magazines;
    }

    @Override
    public void deliverResult(ArrayList<DictItem> data) {
        super.deliverResult(data);
    }

    private ArrayList<DictItem> extractMagazinesFromPlist(String plistName) {
        long startTime = System.currentTimeMillis();

        ArrayList<DictItem> magazines = new ArrayList<DictItem>();

        if (hasTestMagazine) {
            magazines.add(new Magazine(MagazineManager.TEST_FILE_NAME, "TEST", "test", "", getContext()));
        }

        //Convert plist to String for parsing
        String pList = StorageUtils.getStringFromFile(StorageUtils.getStoragePath(getContext()) + plistName);

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
        Log.d("time", (System.currentTimeMillis() - startTime) + " ");
//        EventBus.getDefault().post(new InvalidateGridViewEvent());
        for (DictItem magazine : magazines) {
            //saving png
            File png = new File(magazine.getPngPath());
            if (!png.exists()) {
//                if (isOnline() && !useStaticMagazines) {
                downloadFromUrl(magazine.getPngUrl(), magazine.getPngPath());
//                }
                Log.d(TAG, "Image download: " + magazine.getPngPath());
                EventBus.getDefault().post(new InvalidateGridViewEvent());
            } else {
//                Log.d(TAG, magazine.getPngPath() + " already exist");
            }
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
