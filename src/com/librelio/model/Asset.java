package com.librelio.model;

public class Asset {
	
	public long id;
	public String filename;
	public String assetfilename;
	public String assetUrl;
	public int retryCount;
	
	public Asset(long id, String filename, String assetfilename, String assetUrl, int retryCount) {
		super();
		this.id = id;
		this.filename = filename;
		this.assetfilename = assetfilename;
		this.assetUrl = assetUrl;
		this.retryCount = retryCount;
	}

}
