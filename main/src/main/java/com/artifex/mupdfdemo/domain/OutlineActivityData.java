package com.artifex.mupdfdemo.domain;

import com.artifex.mupdfdemo.OutlineItem;

//TODO: Added multi-thread safe for singleton property
public class OutlineActivityData {
	public OutlineItem items[];
	public int position;
	static private OutlineActivityData singleton;

	static public void set(OutlineActivityData d) {
		singleton = d;
	}

	static public OutlineActivityData get() {
		if (singleton == null)
			singleton = new OutlineActivityData();
		return singleton;
	}
}
