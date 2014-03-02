package com.librelio.model;


public class WebAddressItem extends DictItem {
	
	private String webAddress;

    public WebAddressItem(String webAddress, String title) {
        this.title = title;
        this.webAddress = webAddress;
    }

	public String getWebAddress() {
		return webAddress;
	}

}
