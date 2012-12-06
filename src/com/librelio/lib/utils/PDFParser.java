/**
 * 
 */
package com.librelio.lib.utils;

import java.util.ArrayList;

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
	
	
	
	/**
	 * 
	 * @return all available URLs from pdf
	 */
	@Deprecated
	public SparseArray<ArrayList<String>> getAllUrlsFromPDF() {
		return mLinkUrls;
	}
	
	
	/**
	 * 
	 * @param page - page of the document (starting from 0)
	 * @return - ArrayList of String with urls on the page or null if no urls on the page exists. 
	 */
	@Deprecated
	public ArrayList<String> getUrlsByPage(int page) {
		return mLinkUrls.get(page);
	}
	
	private void parseLinkInfo() {
		for(int page = 0; page < mCore.countPages(); page++) {
			LinkInfo [] mPageLinkInfo = mCore.getPageURIs(page);
			if(mPageLinkInfo.length > 0) {
				mLinkInfo.put(page, mPageLinkInfo);
				ArrayList<String> mPageUrlList = new ArrayList<String>();
				String url;
				for(int j = 0; j < mPageLinkInfo.length; j++) {
					LinkInfo l = mPageLinkInfo[j];
					if((url = mCore.getUriLink(j, (l.left + l.right)/2, (l.top + l.bottom)/2)) != null) {
						mPageUrlList.add(url);
					}
				}
				if(mPageUrlList.size() > 0)
					mLinkUrls.put(page, mPageUrlList);
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
