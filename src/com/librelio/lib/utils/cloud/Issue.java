package com.librelio.lib.utils.cloud;

public class Issue {
	
	/**
	 * Class describing the individual issue of the magazine
	 */
	
//	public static final int STATE_UNLOADED		= 0;  // 00000000
	public static final int STATE_LOADED  		= 1;  // 00000001
//	public static final int STATE_UNPURCHISED 	= 3;  // 00000010
	public static final int STATE_PURCHASED		= 2;  // 00000010
	long magazine_id;
	int state;
	String name;
	String date;
	String price;
	String pdf_url;
	String preview_path;
	String cover_path;
	String issue_path;
	String sku;
	long id = -1;
	
	
	/**
	 * 
	 * Return SKU of the issue
	 * @return the String containing individual SKU of the issue
	 * 
	 */
	public String getSku() {
		return sku;
	}

	/**
	 * Set the SKU of the Issue
	 * @param sku
	 */
	public void setSku(String sku) {
		this.sku = sku;
	}

	/**
	 * Get the local ID of the issue.This method return the record ID in the database
	 * @return the Long, containing the ID of the issue in the device local database
	 * @see Ocean, DBHelper
	 */

	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Issue(){
		
	}

	public long getMagazineId() {
		return magazine_id;
	}

	public void setMagazineId(long l) {
		this.magazine_id = l;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public boolean isLoaded(){
		return state==STATE_LOADED;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getPdf_url() {
		return pdf_url;
	}

	public void setPdf_url(String pdf_url) {
		this.pdf_url = pdf_url;
	}

	public String getPreview_path() {
		return preview_path;
	}

	public void setPreview_path(String preview_path) {
		this.preview_path = preview_path;
	}

	public String getCover_path() {
		return cover_path;
	}

	public void setCover_path(String cover_path) {
		this.cover_path = cover_path;
	}

	public String getIssue_path() {
		return issue_path;
	}

	public void setIssue_path(String issue_path) {
		this.issue_path = issue_path;
	}
}
