package com.librelio.event;

import com.librelio.model.DictItem;

import java.util.ArrayList;

public class UpdateMagazinesEvent {
    private ArrayList<DictItem> magazines;

    public UpdateMagazinesEvent(ArrayList<DictItem> magazines) {
        this.magazines = magazines;
    }

    public ArrayList<DictItem> getMagazines() {
        return magazines;
    }
}
