package com.bbn.poi.xdgf.parsers.rx;

import java.awt.Shape;

public class SpatialTools {
	
	public static com.github.davidmoten.rtree.geometry.Rectangle convertRect(java.awt.geom.Rectangle2D r) {
		return com.github.davidmoten.rtree.geometry.Rectangle.create(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
	}
	
	public static com.github.davidmoten.rtree.geometry.Rectangle getShapeBounds(Shape p) {
		return convertRect(p.getBounds2D());
	}

}
