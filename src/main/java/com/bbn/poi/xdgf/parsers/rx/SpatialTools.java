/*
 * Copyright (c) 2015 Raytheon BBN Technologies Corp. All rights reserved.
 */

package com.bbn.poi.xdgf.parsers.rx;

import java.awt.Shape;

import rx.Observable;

import com.github.davidmoten.rtree.Comparators;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rx.operators.SortedOutputQueue;

public class SpatialTools {
	
	public static com.github.davidmoten.rtree.geometry.Rectangle convertRect(java.awt.geom.Rectangle2D r) {
		return com.github.davidmoten.rtree.geometry.Rectangle.create(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
	}
	
	public static com.github.davidmoten.rtree.geometry.Rectangle getShapeBounds(Shape p) {
		return convertRect(p.getBounds2D());
	}
	
	public static <T, S extends Geometry> Observable<Entry<T, S>> nearest(RTree<T, S> rtree, final Rectangle r, final double maxDistance, int maxCount) {
        return rtree.search(r, maxDistance).lift(
                new SortedOutputQueue<Entry<T, S>>(maxCount, Comparators
                        .<T, S> ascendingDistance(r)));
    }

}
