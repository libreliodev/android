/**
 * 
 */
package com.librelio.lib.utils;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;

import com.artifex.mupdfdemo.LinkInfo;
import com.artifex.mupdfdemo.LinkInfoExternal;
import com.artifex.mupdfdemo.MuPDFCore;
import com.librelio.utils.PlistUtils;

import java.util.ArrayList;

/**
 * @author Dmitry Valetin
 *
 */
public class PDFParser {
	private MuPDFCore mCore;
	private SparseArray<LinkInfoExternal []> mLinkInfo = new SparseArray<LinkInfoExternal []>();
	private SparseArray<ArrayList<String>> mLinkUrls = new SparseArray<ArrayList<String>>();
	private Context context;
	private String pathToPDF;

	/**
	 * 
	 * @param pathToPDF - path within filesystem to PDF file
	 * @throws IllegalStateException - if some error occurs
	 */
	public PDFParser(Context context, String pathToPDF) throws IllegalStateException {
		this.context = context;
		this.pathToPDF = pathToPDF;
		try {
			mCore = new MuPDFCore(pathToPDF);
			parseLinkInfo();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
	
	private void parseLinkInfo() {
		int nPages = mCore.countPages();
		for(int page = 0; page < nPages; page++) {
			LinkInfo [] mPageLinkInfo = mCore.getPageLinks(page);
			if(mPageLinkInfo != null && mPageLinkInfo.length > 0) {
				ArrayList<LinkInfoExternal> fixedLinkInfo = new ArrayList<LinkInfoExternal>();
				LinkInfoExternal current;
				for(int i = 0; i < mPageLinkInfo.length; i++) {
					try {
						if( !(mPageLinkInfo[i] instanceof LinkInfoExternal))
							continue;
						current = (LinkInfoExternal)mPageLinkInfo[i];
						if(current.url != null && current.url.startsWith("http://localhost")) {
							String path = Uri.parse(current.url).getPath();
							if(path.contains("_") && ( path.contains("jpg") || path.contains("png"))) {
								// ops... we have a slideshow here
								int mSlideshowCount = Integer
										.valueOf(path.split("_")[1].split("\\.")[0]);
								String mSlideshowPreffix = path.split("_")[0];
								String mSlideshowSuffix = path.split("_")[1].split("\\.")[1];
						        for (int j = 1; j <= mSlideshowCount; j++) {
						            LinkInfoExternal newLink = new LinkInfoExternal(current.rect.left, current.rect.top,
						                    current.rect.right, current.rect.bottom, "http://localhost" +
						                    mSlideshowPreffix + "_" + String.valueOf(j) + "." + mSlideshowSuffix);
						            fixedLinkInfo.add(newLink);
						        }
						    } else {
								fixedLinkInfo.add(current);
							}
						}else {
							fixedLinkInfo.add(current);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				mLinkInfo.put(page, fixedLinkInfo.toArray(new LinkInfoExternal[fixedLinkInfo.size()]));
			}
		}
	}
	
	/**
	 * get all URI links from PDF document
	 * @return SparseArray with all URLs by page. Each item in the array has LinkInfo[] array with links or null
	 * @see LinkInfo
	 */
	public SparseArray<LinkInfoExternal []> getLinkInfo() {
		return mLinkInfo;
	}
	
}
