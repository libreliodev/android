package com.librelio.event;

import com.librelio.model.dictitem.DictItem;

import java.util.ArrayList;

public class UpdateMagazinesEvent {
	private String plistName;
    private ArrayList<DictItem> magazines;

    public UpdateMagazinesEvent(String plistName, ArrayList<DictItem> magazines) {
    	this.plistName = plistName;
        this.magazines = magazines;
    }

	public String getPlistName() {
		return plistName;
	}

    public ArrayList<DictItem> getMagazines() {
        return magazines;
    }
}
