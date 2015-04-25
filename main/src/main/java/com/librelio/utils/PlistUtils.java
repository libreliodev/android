package com.librelio.utils;

import android.content.Context;
import android.util.Log;

import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.PlistItem;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

public class PlistUtils {

    private static final String TAG = "PlistParserLoader";

    public static ArrayList<DictItem> parsePlist(Context context, String plistName) {

        PlistItem plistItem = new PlistItem(context, "", plistName);

        ArrayList<DictItem> magazines = new ArrayList<DictItem>();

        //Convert plist to String for parsing
        String pList = StorageUtils.getFilePathFromAssetsOrLocalStorage(context, plistItem
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
                String pathBit = FilenameUtils.getPath(plistName);
                DictItem item = DictItem.parse(context, dict, pathBit);
                magazines.add(item);
            }
        } catch (Exception e) {
            Log.d(TAG, "plist = " + pList);
            e.printStackTrace();
        }

//    	if (BuildConfig.DEBUG) {
//    		Log.d(getClass().getSimpleName(), "finished parsing plist: " + plistName);
//    	}
        return magazines;
    }
}
