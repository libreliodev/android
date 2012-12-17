/**
 * 
 */
package com.librelio.lib.utils;

import java.util.ArrayList;

import android.net.Uri;
import android.util.SparseArray;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.MuPDFCore;

/**
 * @author Dmitry Valetin
 *
 */
public class PDFParser {
	private MuPDFCore mCore;
	private SparseArray<LinkInfo []> mLinkInfo = new SparseArray<LinkInfo []>();
	private SparseArray<ArrayList<String>> mLinkUrls = new SparseArray<ArrayList<String>>();
	
	/**
	 * 
	 * @param pathToPDF - path within filesystem to PDF file
	 * @throws IllegalStateException - if some error occurs
	 */
	public PDFParser(String pathToPDF) throws IllegalStateException {
		try {
			mCore = new MuPDFCore(pathToPDF);
			parseLinkInfo();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
	
	private void parseLinkInfo() {
		for(int page = 0; page < mCore.countPages(); page++) {
			LinkInfo [] mPageLinkInfo = mCore.getPageURIs(page);
			if(mPageLinkInfo != null && mPageLinkInfo.length > 0) {
				ArrayList<LinkInfo> fixedLinkInfo = new ArrayList<LinkInfo>();
				LinkInfo current;
				for(int i = 0; i < mPageLinkInfo.length; i++) {
					current = mPageLinkInfo[i];
					if(current.uri != null && current.uri.startsWith("http://localhost")) {
						String path = Uri.parse(current.uri).getPath();
						if(path.contains("_") && ( path.contains("jpg") || path.contains("png"))) {
							// ops... we have a slideshow here
							int mSlideshowCount = Integer
									.valueOf(path.split("_")[1].split("\\.")[0]);
							String mSlideshowPreffix = path.split("_")[0];
							String mSlideshowSuffix = path.split("_")[1].split("\\.")[1];
							for(int j = 1; j <= mSlideshowCount; j ++) {
								LinkInfo newLink = new LinkInfo(current.left, current.top, current.right, current.bottom, "http://localhost"+mSlideshowPreffix+"_"+String.valueOf(j)+"."+mSlideshowSuffix);
								fixedLinkInfo.add(newLink);
							}
						} else {
							fixedLinkInfo.add(current);
						}
					}else {
						fixedLinkInfo.add(current);
					}
				}
				
				mLinkInfo.put(page, fixedLinkInfo.toArray(new LinkInfo[fixedLinkInfo.size()]));
			}
		}
	}
	
	/**
	 * get all URI links from PDF document
	 * @return SparseArray with all URLs by page. Each item in the array has LinkInfo[] array with links or null
	 * @see LinkInfo
	 */
	public SparseArray<LinkInfo []> getLinkInfo() {
		return mLinkInfo;
	}
	
}
