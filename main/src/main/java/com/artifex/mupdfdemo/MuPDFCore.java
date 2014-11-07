package com.artifex.mupdfdemo;

import java.io.File;
import java.security.MessageDigest;

import org.apache.commons.io.FileUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class MuPDFCore {
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}

	private static final String TAG = "MuPDFCore";

	/* Readable members */
	private int pageNum = -1;;
	private int numPages = -1;
	private int displayPages = 1;
	public float pageWidth;
	public float pageHeight;
	private String mFileName;

	private long globals;

	/* The native functions */
	/* The native functions */
	private native long openFile(String filename);
	private native long openBuffer();
	private native int countPagesInternal();
	private native void gotoPageInternal(int localActionPageNum);
	private native float getPageWidth();
	private native float getPageHeight();
	private native void drawPage(Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native void updatePageInternal(Bitmap bitmap,
			int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native RectF[] searchPage(String text);
	private native int passClickEventInternal(int page, float x, float y);
	private native void setFocusedWidgetChoiceSelectedInternal(String [] selected);
	private native String [] getFocusedWidgetChoiceSelected();
	private native String [] getFocusedWidgetChoiceOptions();
	private native int setFocusedWidgetTextInternal(String text);
	private native String getFocusedWidgetTextInternal();
	private native int getFocusedWidgetTypeInternal();
	private native LinkInfo [] getPageLinksInternal(int page);
	private native RectF[] getWidgetAreasInternal(int page);
	private native OutlineItem [] getOutlineInternal();
	private native boolean hasOutlineInternal();
	private native boolean needsPasswordInternal();
	private native boolean authenticatePasswordInternal(String password);
	private native MuPDFAlertInternal waitForAlertInternal();
	private native void replyToAlertInternal(MuPDFAlertInternal alert);
	private native void startAlertsInternal();
	private native void stopAlertsInternal();
	private native void destroying();
	private native boolean hasChangesInternal();
	private native void saveInternal();

	public MuPDFCore(String filename) throws Exception {
		mFileName = filename;
		globals = openFile(filename);
		
		if (globals == 0)
		{
			// Logging
			File file = new File(filename);
			Log.e(TAG, "Filename: " + filename);
			Log.e(TAG, "File exists: " + file.exists());
			Log.e(TAG, "File size: " + file.length());
			byte[] hash = MessageDigest.getInstance("md5").digest(FileUtils.readFileToByteArray(file));
			StringBuilder hexString = new StringBuilder();
	        for (int i = 0; i < hash.length; i++) {
	            if ((0xff & hash[i]) < 0x10) {
	                hexString.append("0"
	                        + Integer.toHexString((0xFF & hash[i])));
	            } else {
	                hexString.append(Integer.toHexString(0xFF & hash[i]));
	            }
	        }
			Log.e(TAG, "File md5sum: " + hexString.toString());
			
			throw new Exception("Failed to open "+filename);
		}
	}

	public String getFileName() {
		return mFileName;
	}

	public String getFileDirectory() {
		return (new File(getFileName())).getParent();
	}

	public int countPages() {
		if (numPages < 0)
			numPages = countPagesSynchronized();
		if(displayPages == 1)
			return numPages;
		if(numPages % 2 == 0) {
			return numPages / 2 + 1;
		}
		int toReturn = numPages / 2;
		return toReturn + 1;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	public void gotoPage(int page) {
		if (page > numPages - 1)
			page = numPages - 1;
		else if (page < 0)
			page = 0;
//		if (this.pageNum == page)
//			return;
		gotoPageInternal(page);
		this.pageNum = page;
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized PointF getPageSize(int page) {
		// If we have only one page (portrait), or if is the first or the last page, we show only one page (centered).
		if (displayPages == 1 || page==0 || (displayPages==2 && page == numPages/2)) {
			gotoPage(page);
			return new PointF(pageWidth, pageHeight);
		} else {
			gotoPage(page);
			if (page == numPages - 1 || page == 0) {
				// last page
				return new PointF(pageWidth * 2, pageHeight);
			}
			float leftWidth = pageWidth;
			float leftHeight = pageHeight;
			gotoPage(page + 1);
			float screenWidth = leftWidth + pageWidth;
			float screenHeight = Math.max(leftHeight, pageHeight);
			return new PointF(screenWidth, screenHeight);
		}
	}

	public synchronized void onDestroy() {
		destroying();
		globals = 0;
	}
	
	public synchronized PointF getSinglePageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}
	
	public synchronized void drawPageSynchrinized(int page, Bitmap bitmap, int pageW,
			int pageH, int patchX, int patchY, int patchW, int patchH) {
		gotoPage(page);
		Log.d(TAG,"drawPageSynchrinized page:"+page);
		drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
	}
	
	public synchronized void drawSinglePage(int page, Bitmap bitmap, int pageW,
			int pageH) {

				drawPageSynchrinized(page, bitmap, pageW, pageH, 0, 0, pageW, pageH);
	}

	public synchronized Bitmap drawPage(final int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		
		Canvas canvas = null;
		Bitmap bitmap = null;
		try {
			bitmap = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
			canvas = new Canvas(bitmap);
			canvas.drawColor(Color.TRANSPARENT);
			Log.d(TAG, "drawPage "+page);
			
			Log.d(TAG,"canvas: "+canvas);
			// If we have only one page (portrait), or if is the first, we show only one page (centered).
			if (displayPages == 1 || page==0) {
				gotoPage(page);
				drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
				return bitmap;
				// If we are on two pages mode (landscape), and at the last page, we show only one page (centered).
			} else if (displayPages==2 && page == numPages/2) {
				gotoPage(page*2+1); // need to multiply per 2, because page counting is being divided by 2 (landscape mode)
				drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
				return bitmap;
			} else {
				final int drawPage = (page == 0) ? 0 : page * 2 - 1;
				int leftPageW = pageW / 2;
				int rightPageW = pageW - leftPageW;

				// If patch overlaps both bitmaps (left and right) - return the
				// width of overlapping left bitpam part of the patch
				// or return full patch width if it's fully inside left bitmap
				int leftBmWidth = Math.min(leftPageW, leftPageW - patchX);

				// set left Bitmap width to zero if patch is fully overlay right
				// Bitmap
				leftBmWidth = (leftBmWidth < 0) ? 0 : leftBmWidth;

				// set the right part of the patch width, as a rest of the patch
				int rightBmWidth = patchW - leftBmWidth;

				if (drawPage == numPages - 1) {
					// draw only left page
					canvas.drawColor(Color.BLACK);
					if (leftBmWidth > 0) {
						Bitmap leftBm = Bitmap.createBitmap(leftBmWidth, patchH,
								getBitmapConfig());
						gotoPage(drawPage);
						drawPage(leftBm, leftPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, leftBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
					}
				} else if (drawPage == 0) {
					// draw only right page
					canvas.drawColor(Color.BLACK);
					if (rightBmWidth > 0) {
						Bitmap rightBm = Bitmap.createBitmap(rightBmWidth, patchH,
								getBitmapConfig());
						gotoPage(drawPage);
						drawPage(rightBm, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);
						rightBm.recycle();
					}
				} else {
					// Need to draw two pages one by one: left and right
					Log.d("bitmap width", "" + bitmap.getWidth());
//					canvas.drawColor(Color.BLACK);
					Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
					if (leftBmWidth > 0) {
						Bitmap leftBm = Bitmap.createBitmap(leftBmWidth,
								patchH, getBitmapConfig());
						gotoPage(drawPage);
						drawPage(leftBm, leftPageW, pageH, patchX, patchY,
								leftBmWidth, patchH);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
					}
					if (rightBmWidth > 0) {
						Bitmap rightBm = Bitmap.createBitmap(rightBmWidth,
								patchH, getBitmapConfig());
						gotoPage(drawPage+1);
						drawPage(rightBm, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);

						canvas.drawBitmap(rightBm, (float) leftBmWidth, 0,
								paint);
						rightBm.recycle();
					}

				}
				return bitmap;
			}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "draw page " + page + "failed", e);
			if(canvas != null) canvas.drawColor(Color.TRANSPARENT);
			return bitmap;
		}
	}
	
	public synchronized Bitmap updatePage(BitmapHolder h, int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		Bitmap bitmap = null;
		Bitmap old_bm = h.getBm();

		Log.d(TAG, "updatePage " + h.getBm());
		if (old_bm == null || old_bm.isRecycled())
			return null;

		bitmap = old_bm.copy(Bitmap.Config.ARGB_8888, true);
		old_bm = null;
		
		// updatePageInternal(bitmap, page, pageW, pageH, patchX, patchY, patchW, patchH);
		Canvas canvas = null;
		
		try {
			canvas = new Canvas(bitmap);
			canvas.drawColor(Color.TRANSPARENT);
			Log.d(TAG,"canvas: "+canvas);
			if (displayPages == 1) {
				
				updatePageInternal(bitmap, page, pageW, pageH, patchX, patchY, patchW, patchH);
				return bitmap;
			} else {
				page = (page == 0) ? 0 : page * 2 - 1;
				int leftPageW = pageW / 2;
				int rightPageW = pageW - leftPageW;

				// If patch overlaps both bitmaps (left and right) - return the
				// width of overlapping left bitpam part of the patch
				// or return full patch width if it's fully inside left bitmap
				int leftBmWidth = Math.min(leftPageW, leftPageW - patchX);

				// set left Bitmap width to zero if patch is fully overlay right
				// Bitmap
				leftBmWidth = (leftBmWidth < 0) ? 0 : leftBmWidth;

				// set the right part of the patch width, as a rest of the patch
				int rightBmWidth = patchW - leftBmWidth;

				if (page == numPages - 1) {
					// draw only left page
//					canvas.drawColor(Color.BLACK);
					if (leftBmWidth > 0) {
						Bitmap leftBm = Bitmap.createBitmap(bitmap, 0, 0, leftBmWidth, patchH);
						updatePageInternal(leftBm, page, leftPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, leftBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
					}
				} else if (page == 0) {
					// draw only right page
//					canvas.drawColor(Color.BLACK);
					if (rightBmWidth > 0) {
						Bitmap rightBm = Bitmap.createBitmap(bitmap, leftBmWidth, 0, rightBmWidth, patchH);
						gotoPage(page);
						updatePageInternal(rightBm, page, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);
						rightBm.recycle();
					}
				} else {
					// Need to draw two pages one by one: left and right
					Log.d("bitmap width", "" + bitmap.getWidth());
//					canvas.drawColor(Color.BLACK);
					Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
					if (leftBmWidth > 0) {
						Bitmap leftBm = Bitmap.createBitmap(bitmap, 0, 0, (leftBmWidth < bitmap.getWidth()) ? leftBmWidth : bitmap.getWidth(),
								patchH);
						updatePageInternal(leftBm, page, leftPageW, pageH, patchX, patchY,
								leftBmWidth, patchH);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
					}
					if (rightBmWidth > 0) {
						Bitmap rightBm = Bitmap.createBitmap(bitmap, leftBmWidth, 0, rightBmWidth,
								patchH);
						updatePageInternal(rightBm, page, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);

						canvas.drawBitmap(rightBm, (float) leftBmWidth, 0,
								paint);
						rightBm.recycle();
					}

				}
				return bitmap;
			}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "update page " + page + "failed", e);
			if(canvas != null) canvas.drawColor(Color.TRANSPARENT);
			return bitmap;
		}
	}
	
	/*

	public synchronized void drawPage(int page, Bitmap bitmap, int pageW, int pageH, int patchX, int patchY, int patchW, int patchH) {
		Canvas canvas = new Canvas(bitmap);
		Log.d(TAG,"canvas: "+canvas);
		try {
			if (displayPages == 1) {
				drawPageSynchrinized(page, bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
			} else {
				page = (page == 0) ? 0 : page * 2 - 1;
				int leftPageW = pageW / 2;
				int rightPageW = pageW - leftPageW;

				// If patch overlaps both bitmaps (left and right) - return the
				// width of overlapping left bitpam part of the patch
				// or return full patch width if it's fully inside left bitmap
				int leftBmWidth = Math.min(leftPageW, leftPageW - patchX);

				// set left Bitmap width to zero if patch is fully overlay right
				// Bitmap
				leftBmWidth = (leftBmWidth < 0) ? 0 : leftBmWidth;

				// set the right part of the patch width, as a rest of the patch
				int rightBmWidth = patchW - leftBmWidth;

				if (page == numPages - 1) {
					// draw only left page
					canvas.drawColor(Color.BLACK);
					if (leftBmWidth > 0) {
						Bitmap bm = Bitmap.createBitmap(leftBmWidth, patchH,
								getBitmapConfig());
						drawPageSynchrinized(page, bm, leftPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, leftBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(bm, 0, 0, paint);
						bm.recycle();
					}
				} else if (page == 0) {
					canvas.drawColor(Color.BLACK);
					if (rightBmWidth > 0) {
						Bitmap bm = Bitmap.createBitmap(rightBmWidth, patchH,
								getBitmapConfig());
						drawPageSynchrinized(page, bm, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(bm, leftBmWidth, 0, paint);
						bm.recycle();
					}
				} else {
					Log.d("bitmap width", "" + bitmap.getWidth());
					canvas.drawColor(Color.BLACK);
					Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
					if (leftBmWidth > 0) {
						Bitmap leftBm = Bitmap.createBitmap(leftBmWidth,
								patchH, getBitmapConfig());
						drawPageSynchrinized(page, leftBm, leftPageW, pageH, patchX, patchY,
								leftBmWidth, patchH);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
					}
					if (rightBmWidth > 0) {
						Bitmap rightBm = Bitmap.createBitmap(rightBmWidth,
								patchH, getBitmapConfig());
						drawPageSynchrinized(page + 1, rightBm, rightPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, rightBmWidth, patchH);

						canvas.drawBitmap(rightBm, (float) leftBmWidth, 0,
								paint);
						rightBm.recycle();
					}

				}
			}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "draw page " + page + "failed", e);
			canvas.drawColor(Color.TRANSPARENT);
		}
		System.gc();
	}

	 */
	
	public synchronized int hitLinkPage(int page, float x, float y) {
		LinkInfo[] pageLinks = getPageLinks(page);
		for(LinkInfo pageLink: pageLinks) {
			if(pageLink instanceof LinkInfoInternal) {
				LinkInfoInternal internalLink = (LinkInfoInternal) pageLink;
				if(internalLink.rect.contains(x, y))
					return internalLink.pageNumber;
			}
		}
		return -1;
	}

//	public synchronized String hitLinkUri(int page, float x, float y) {
//		if(displayPages == 1)
//			return getUriLink(page, x, y);
//		int rightPage = page * 2;
//		int leftPage = rightPage - 1;
//		int count = countPages() * 2;
//		if(x < pageWidth && leftPage > 0) {
//			return getUriLink(leftPage, x, y);
//		} else if(rightPage < count) {
//			return getUriLink(rightPage, x - pageWidth, y);
//		}
//		return null;
//	}

	public synchronized LinkInfo[] getPageLinks(int page) {
		if(displayPages == 1)
			return getPageLinksInternal(page);
		LinkInfo[] leftPageLinkInfo = new LinkInfo[0];
		LinkInfo[] rightPageLinkInfo = new LinkInfo[0];
		LinkInfo[] combinedLinkInfo;
		int combinedSize = 0;
		int rightPage = page * 2;
		int leftPage = rightPage - 1;
		int count = countPages() * 2;
		if( leftPage > 0 ) {
			LinkInfo[] leftPageLinkInfoInternal = getPageLinksInternal(leftPage);
			if (null != leftPageLinkInfoInternal) {
				leftPageLinkInfo = leftPageLinkInfoInternal;
				combinedSize += leftPageLinkInfo.length;
			}
		}
		if( rightPage < count ) {
			LinkInfo[] rightPageLinkInfoInternal = getPageLinksInternal(rightPage);
			if (null != rightPageLinkInfoInternal) {
				rightPageLinkInfo = rightPageLinkInfoInternal;
				combinedSize += rightPageLinkInfo.length;
			}
		}
		
		combinedLinkInfo = new LinkInfo[combinedSize];
		for(int i = 0; i < leftPageLinkInfo.length; i++) {
			combinedLinkInfo[i] = leftPageLinkInfo[i];
		}
		
		LinkInfo temp;
		for(int i = 0, j = leftPageLinkInfo.length; i < rightPageLinkInfo.length; i++, j++) {
			temp = rightPageLinkInfo[i];
			temp.rect.left += pageWidth;
			temp.rect.right += pageWidth;
			combinedLinkInfo[j] = temp;
		}
		for (LinkInfo linkInfo: combinedLinkInfo) {
			if(linkInfo instanceof LinkInfoExternal)
				Log.d(TAG, "return " + ((LinkInfoExternal)linkInfo).url);
		}
		return combinedLinkInfo;
	}

//	public synchronized LinkInfo[] getPageURIs(int page) {
//		return getPageURIsInternal(page);
//	}

	public synchronized RectF[] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized OutlineItem[] getOutline() {
		return getOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}
	
	public int getDisplayPages() {
		return displayPages;
	}
	
	private Config getBitmapConfig(){
		return Config.ARGB_8888;
	} 

	/**
	 * @return
	 */
	public int countDisplays() {
		int pages = countPages();
		if(pages % 2 == 0) {
			return pages / 2 + 1;
		} else 
			return pages / 2;
	}
	
	public void setDisplayPages(int pages) throws IllegalStateException {
		if(pages <=0 || pages > 2) {
			throw new IllegalStateException("MuPDFCore can only handle 1 or 2 pages per screen!");
		}
		displayPages = pages;
	}

	/**
	 * @return
	 */
	public int countSinglePages() {
		// TODO Auto-generated method stub
		return numPages;
	}
}
