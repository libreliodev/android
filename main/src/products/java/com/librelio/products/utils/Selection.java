package com.librelio.products.utils;

public class Selection {

	int creteriaGroupId;
	String keyField;
	long keyFieldValue;
	String selectionName;

	public Selection(int creteriaGroupId, String keyField,
			long keyFieldValue, String selectionName) {
		super();
		this.creteriaGroupId = creteriaGroupId;
		this.keyField = keyField;
		this.keyFieldValue = keyFieldValue;
		this.selectionName = selectionName;
	}

	public int getCreteriaGroupId() {
		return creteriaGroupId;
	}

	public String getKeyField() {
		return keyField;
	}

	public long getKeyFieldValue() {
		return keyFieldValue;
	}

	public String getSelectionName() {
		return selectionName;
	}

	public void setCreteriaGroupId(int creteriaGroupId) {
		this.creteriaGroupId = creteriaGroupId;
	}

	public void setKeyField(String keyField) {
		this.keyField = keyField;
	}

	public void setKeyFieldValue(long keyFieldValue) {
		this.keyFieldValue = keyFieldValue;
	}

	public void setSelectionName(String selectionName) {
		this.selectionName = selectionName;
	}

}