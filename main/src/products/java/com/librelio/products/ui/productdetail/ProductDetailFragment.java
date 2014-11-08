package com.librelio.products.ui.productdetail;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.librelio.products.ui.BaseNivealesFragment;
import com.librelio.products.ui.popup.ActionItem;
import com.librelio.products.ui.popup.ImageZoomPopup;
import com.librelio.products.ui.CustomizationHelper;
import com.librelio.products.ui.CustomizationHelper.ProductDetailConstants;
import com.librelio.products.utils.db.ProductsDBHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class ProductDetailFragment extends BaseNivealesFragment {

	private static final String ZOOM_TOUCHMOVE = "zoom://touchmove/";
	private static final String ZOOM_FINISH = "zoom://finish";
	private static final String ZOOM_TOUCHSTART = "zoom://touchstart";
	private static final String ZOOM_TOUCHEND = "zoom://touchend";

	View rootView;
	String htmlBasePage; // text from assets html page to customize
	String customizedHTMLPage; // page after customization
	ProductsDBHelper helper;
	private String productId;
	Cursor productCursor;
	String pic;
	private int productDetailLayout;
	private int webViewId;
	private int favoriteId;
	private int shareId;
	ShareProductListener listener;
	private String[] columnKeys;
	private String[] htmlKeys;
	private ImageButton mPrevButton;
	private WebView webView;
	private ImageButton mNextButton;
	// private ImageView mProductImage;
	private ImageZoomPopup mProductImagePopup;
	private int bitmapWidth;
	private int bitmapHeight;
	private Bitmap mHiResBitmap;
	private int webPageStringResourceId;
	protected float downX;
	protected float downY;
	protected boolean isZoomStarted = false;
	private ArrayList<ActionItem> mActionItems = new ArrayList<ActionItem>();
	private CheckBox mFavoriteCheckBox;

	private int[] mZoomCoords;

    private void init(int productDetailLayout, int webViewId,
			int webPageStringResourceId, String[] columnKeys,
			String[] htmlKeys, int favoriteCheckboxId, int shareButtonId) {
		this.helper = ProductsDBHelper.getDBHelper();
		this.productDetailLayout = productDetailLayout;
		this.webViewId = webViewId;

		this.favoriteId = favoriteCheckboxId;
		this.shareId = shareButtonId;
		this.htmlKeys = htmlKeys;
		this.columnKeys = columnKeys;
		this.webPageStringResourceId = webPageStringResourceId;
	}

	public void setProductCursor(Cursor c) {
		this.productCursor = c;
	}

	public void setOnShareProductListener(ShareProductListener l) {
		this.listener = l;
	}

	public String readHTML() {
		BufferedInputStream bin;
		try {
			bin = new BufferedInputStream(getActivity().getAssets().open(
					getActivity().getString(webPageStringResourceId)));

			InputStreamReader in = new InputStreamReader(bin, "UTF-8");
			StringWriter w = new StringWriter();
			char[] buffer = new char[1024];
			int count = 0;
			while ((count = in.read(buffer, 0, 1024)) > 0) {
				w.write(buffer, 0, count);
			}

			in.close();
			w.close();
			return w.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	String getHTMLPage(Cursor c) {
		productId = c.getString(c.getColumnIndexOrThrow("id_modele"));
		String htmlString = new String(htmlBasePage);
		for (int i = 0; i < columnKeys.length; i++) {
			int columnIndex = c.getColumnIndex(columnKeys[i]);
			if (columnIndex != -1) {
				String value = c.getString(columnIndex);
				if (htmlKeys[i].startsWith("%icone") && !value.equals("")) {
					value = "<img src=\"" + CustomizationHelper.ASSETS_URI
							+ value + "\"/>";
				} else {
					if (value != null && (value.endsWith("png") || value.endsWith("jpg"))) {
						// product image
						value = CustomizationHelper.ASSETS_PHOTOS_URI + value;
					}
				}
                if (value != null) {
                    htmlString = htmlString.replace(htmlKeys[i], value);
                }
			}
		}
		Log.d("HTML", htmlString);
		return htmlString;
	}

	/*
	 * private View.OnTouchListener mProductImageTouchListener = new
	 * View.OnTouchListener() { private float mx; private float my; private
	 * float deltaX, deltaY; int maxX = 0, maxY = 0;
	 * 
	 * @Override public boolean onTouch(View pV, MotionEvent pEvent) {
	 * 
	 * float curX, curY;
	 * 
	 * switch (pEvent.getAction()) {
	 * 
	 * case MotionEvent.ACTION_DOWN: mx = pEvent.getX(); my = pEvent.getY();
	 * deltaX = deltaY = 0; maxX = Math.abs(bitmapWidth -
	 * mProductImage.getWidth()) / 2; maxY = Math.abs(bitmapHeight -
	 * mProductImage.getHeight()) / 2; break; case MotionEvent.ACTION_MOVE: curX
	 * = pEvent.getX(); curY = pEvent.getY(); deltaX = mx - curX; deltaY = my -
	 * curY; mx = curX; my = curY; float scrollX = mProductImage.getScrollX();
	 * float scrollY = mProductImage.getScrollY(); if (scrollX + deltaX < -maxX)
	 * scrollX = -maxX; else if (scrollX + deltaX > maxX) scrollX = maxX; else
	 * scrollX += deltaX;
	 * 
	 * if (scrollY + deltaY < -maxY) scrollY = -maxY; else if (scrollY + deltaY
	 * > maxY) scrollY = maxY; else scrollY += deltaY;
	 * 
	 * mProductImage.scrollTo((int) scrollX, (int) scrollY); break; case
	 * MotionEvent.ACTION_UP: curX = pEvent.getX(); curY = pEvent.getY(); deltaX
	 * = mx - curX; deltaY = my - curY; break; }
	 * 
	 * return true; } };
	 */

	public void loadProduct(Cursor c) {
		webView.loadDataWithBaseURL(CustomizationHelper.ASSETS_URI,
				getHTMLPage(c), "text/html", "UTF-8", null);
		mFavoriteCheckBox.setChecked(helper.isFavorite(productId));
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		init(ProductDetailConstants.PRODUCT_DETAIL_LAYOUT,
				ProductDetailConstants.PRODUCT_DETAIL_WEBVIEW_VIEW_ID,
				ProductDetailConstants.PRODUCT_DETAIL_WEBPAGE_FILE_URI,
				ProductDetailConstants.PRODUCT_DETAIL_COLUMN_KEYS,
				ProductDetailConstants.PRODUCT_DETAIL_HTML_FILE_KEYS,
				ProductDetailConstants.PRODUCT_DETAIL_FAVORITE_CKECKBOX_VIEW_ID,
				ProductDetailConstants.PRODUCT_DETAIL_SHARE_BUTTON_VIEW_ID);
		rootView = inflater.inflate(productDetailLayout, container, false);

		htmlBasePage = readHTML();
		webView = (WebView) rootView.findViewById(webViewId);

		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.startsWith("zoom://")) {
					// view.loadDataWithBaseURL(Consts.ASSETS_URI, text,
					// "text/html", "UTF-8", null);
					showLargeImage(url);
					return true;
				}
				return false;
			}
		});

		webView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View pV, MotionEvent pEvent) {
				if (isZoomStarted) {
					if(pEvent.getAction() == MotionEvent.ACTION_MOVE){
						float density = getActivity().getResources()
								.getDisplayMetrics().density;
						float touchX = Math.round(pEvent.getX())/density;
						float touchY = Math.round(pEvent.getY())/density;
						
						if(touchX >= mZoomCoords[4] && touchY >= mZoomCoords[2] &&
								touchX <= mZoomCoords[5] + mZoomCoords[4] && touchY <= mZoomCoords[3] + mZoomCoords[2]) {
							String zoomString = ZOOM_TOUCHMOVE+"?"+Math.round(touchX)+","+Math.round(touchY)+","+mZoomCoords[2]+","+mZoomCoords[3]+","+mZoomCoords[4]+","+mZoomCoords[5];
							showLargeImage(zoomString);
						}
					}
					if(pEvent.getAction() == MotionEvent.ACTION_UP) {
						showLargeImage(ZOOM_FINISH);
					}
					return true;
				}
				return false;
			}
		});

		ImageView shareButton = (ImageView) rootView.findViewById(shareId);
		shareButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				listener.onShareProduct(productCursor);
			}
		});
		mFavoriteCheckBox = (CheckBox) rootView.findViewById(favoriteId);
		mFavoriteCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton pButtonView,
							boolean pIsChecked) {
						if (!pButtonView.isPressed())
							return;
						if (pIsChecked) {
							helper.addFavorite(productId);
						} else {
							helper.deleteFavorite(productId);
						}
					}
				});

		mPrevButton = (ImageButton) rootView
				.findViewById(CustomizationHelper.ProductDetailConstants.PRODUCT_DETAIL_PREVBUTTON_VIEW_ID);
		mPrevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View pV) {
				if (!productCursor.isFirst()) {
					productCursor.move(-1);
					// recycleImageViewBitmap(mProductImage);
					if(mProductImagePopup != null)
						mProductImagePopup.dismiss();
					// loadImageBitmap();
					loadProduct(productCursor);
				}
			}
		});
		
		mNextButton = (ImageButton) rootView
				.findViewById(CustomizationHelper.ProductDetailConstants.PRODUCTDETAIL_NEXTBUTTON_VIEW_ID);
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View pV) {
				if (!productCursor.isLast()) {
					productCursor.move(1);
					// recycleImageViewBitmap(mProductImage);
					if(mProductImagePopup != null)
						mProductImagePopup.dismiss();
					// loadImageBitmap();
					loadProduct(productCursor);
				}
			}
		});

		loadProduct(productCursor);
		return rootView;
	}

	protected void showLargeImage(String pUrl) {
		if (pUrl.startsWith(ZOOM_TOUCHSTART)) {
			String[] coordStr = pUrl.split("\\?")[1].split(",");
			int[] coords = new int[coordStr.length];
			for (int i = 0; i < coords.length; i++)
				coords[i] = Integer.valueOf(coordStr[i]);
			this.mZoomCoords = coords;
			mProductImagePopup = new ImageZoomPopup(webView, coords) {
				@Override
				public void onDismiss() {
					super.onDismiss();
					isZoomStarted = false;
				}
			};
			mProductImagePopup.setBackgroundDrawable(new ColorDrawable(
					Color.WHITE));
			mProductImagePopup.setImageBitmap(getProductBitmap());
			mProductImagePopup.show();
			isZoomStarted = true;
			DownloadHiResTask task = new DownloadHiResTask();
			task.execute(productCursor.getString(productCursor
					.getColumnIndexOrThrow("imgHR")));
		}

		if (pUrl.startsWith(ZOOM_FINISH) || pUrl.startsWith(ZOOM_TOUCHEND)) {
			// this.mProductImageGoneHandler.removeCallbacks(mProductImageGoneRunnable);
			// mProductImage.setVisibility(View.GONE);
			mProductImagePopup.dismiss();
			isZoomStarted = false;
		}
		// if(pUrl.startsWith(ZOOM_TOUCHEND)) {
		// mProductImage.setVisibility(View.GONE);
		// isZoomStarted = false;
		// }
		if (pUrl.startsWith(ZOOM_TOUCHMOVE)) {
			if (isZoomStarted) {
				String[] coordStr = pUrl.split("\\?")[1].split(",");
				int[] coords = new int[coordStr.length];
				for (int i = 0; i < coords.length; i++)
					coords[i] = Integer.valueOf(coordStr[i]);
				mProductImagePopup.scroll(coords);
			}
		}
		Log.d("zoom", pUrl);
		Log.d("DPI", String.valueOf(getActivity().getResources()
				.getDisplayMetrics().density));
	}

	public Bitmap getProductBitmap() {
        try {
            pic = productCursor.getString(productCursor
                    .getColumnIndexOrThrow("imgLR"));
            Bitmap b = BitmapFactory.decodeFile(CustomizationHelper.ASSETS_PHOTOS_URI + pic);
            this.bitmapWidth = b.getWidth();
            this.bitmapHeight = b.getHeight();
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	public void recycleImageViewBitmap(ImageView i) {
		if (i != null) {
			Drawable d = i.getDrawable();
			if (d instanceof BitmapDrawable) {
				BitmapDrawable bd = (BitmapDrawable) d;
				bd.getBitmap().recycle();
			}
		}
		mHiResBitmap = null;
	}

	public void onShareStarted() {

	}

    public interface ShareProductListener {
		public void onShareProduct(Cursor productCursor);
	}

	public Cursor getCursor() {
		return productCursor;
	}

	public class DownloadHiResTask extends AsyncTask<String, Void, Bitmap> {

		String lastError = "Unknown Error";

		@Override
		protected Bitmap doInBackground(String... pParams) {
			try {
				Uri uri = Uri.parse(pParams[0]);
				String fileName = uri.getLastPathSegment();
				URL url = new URL(pParams[0]);
				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
				urlConnection.connect();
				File tempFile = new File(getActivity().getCacheDir()
						.getAbsolutePath() + "/" + fileName);

				int totalSize = urlConnection.getContentLength();
				int downloaded = 0;
				if (tempFile.exists()) {
					// check the existing file size
					if (tempFile.length() == totalSize) {
						Bitmap b = BitmapFactory
								.decodeStream(new FileInputStream(tempFile));
						return b;
					} else {
						tempFile.delete();
					}
				}
				tempFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(tempFile);
				InputStream is = urlConnection.getInputStream();
				byte[] buf = new byte[1024];
				int count = 0;

				while ((count = is.read(buf)) > 0) {
					fos.write(buf, 0, count);
					downloaded += count;
				}

				fos.close();
				is.close();

				Bitmap b = BitmapFactory.decodeStream(new FileInputStream(
						tempFile));
				return b;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				lastError = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				lastError = "Network error";
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				if (result.getWidth() > 2048) {
					float scale = 2048f / (float) result.getWidth();
					float height = (float) result.getHeight();
					height = height * scale;
					Bitmap scaledBitmap = Bitmap.createScaledBitmap(result,
							2048, Math.round(height), true);
					result.recycle();
					result = scaledBitmap;
				}
				bitmapWidth = result.getWidth();
				bitmapHeight = result.getHeight();
				mHiResBitmap = result;
				if (mProductImagePopup != null) {
					// mProductImage.setScaleType(ScaleType.CENTER);
					mProductImagePopup.setImageBitmap(mHiResBitmap);
				}
			} else {
				Toast.makeText(getActivity(), lastError, Toast.LENGTH_SHORT)
						.show();
			}
		}
	};
}
