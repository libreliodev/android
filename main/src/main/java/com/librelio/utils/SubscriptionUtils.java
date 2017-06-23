package com.librelio.utils;

import android.content.Context;
import java.io.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

/**
 * Created by odin on 19/06/2017.
 */

public class SubscriptionUtils {

    public static String getSubscriptionsCode(Context context, String wantedCode) {
        String subscriptionSCodesPlistURL = "subscriptions_codes.plist?waupdate=30";
        PlistDownloader.updateFromServer(context, subscriptionSCodesPlistURL, true, false);
        String pList = StorageUtils.getFilePathFromAssetsOrLocalStorage(context, "subscriptions_codes.plist");

        if (pList == null) {
            return null;
        }

        DocumentBuilderFactory factory;
        DocumentBuilder builder;
        InputStream is;
        Document dom;
        String code = "";
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();

            is = new ByteArrayInputStream(pList.getBytes("UTF-8"));

            dom = builder.parse(is);
            NodeList dictContent = dom.getElementsByTagName("dict").item(0).getChildNodes();
            int length = dictContent.getLength();
            String previousKey = "";
            for (int i = 0; i < length; i++) {
                if (dictContent.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) dictContent.item(i);
                    if (el.getNodeName().contains("key")) {
                        previousKey = el.getTextContent();
                    }
                    if(previousKey.startsWith(wantedCode)) {
                        if (el.getNodeName().contains("string")) {
                            code = el.getTextContent();
                        }
                    }
                }
            }
        }
        catch(Exception e){

        }

        return code;
    }
}
