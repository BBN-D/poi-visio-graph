package com.bbn.poi.xdgf.parsers.rx;

import com.bbn.poi.xdgf.parsers.ShapeData;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Rectangle;

import rx.Subscriber;
import rx.functions.Action1;

// convenience functions
public class Rx {
	
	public static interface RTreeAction extends Action1<Entry<ShapeData, Rectangle>> {
		
	}
	
	public static abstract class RTreeSubscriber extends Subscriber<Entry<ShapeData, Rectangle>> {
		
		@Override
		public void onCompleted() {
			
		}

		@Override
		public void onError(Throwable e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
