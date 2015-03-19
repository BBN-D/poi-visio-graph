package com.bbn.poi.xdgf.parsers;


import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

import org.apache.poi.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.bbn.poi.xdgf.geom.GeomUtils;
import com.bbn.poi.xdgf.parsers.rx.SpatialTools;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.tinkerpop.blueprints.Vertex;

public class ShapeData {

	// orders by largest first
	public static class OrderByLargestAreaFirst implements Comparator<ShapeData> {
		@Override
		public int compare(ShapeData o1, ShapeData o2) {
			return Float.compare(o2.area, o1.area);
		}
	}
	
	public Vertex vertex;
	public Long parentId = null;
	
	// in global coordinates
	public Rectangle bounds;
	public float area;
	
	// in global coordinates
	public Path2D path1D = null;
	public Point2D path1Dstart = null;
	public Point2D path1Dend = null;
	
	public Path2D path2D = null;
	
	public Point2D textCenter = null;
	
	// don't store the actual shape, just useful attributes needed later
	// -> allows us to join/split shapes at will
	public long shapeId;
	public boolean hasText;
	public boolean isTextbox;
	
	public Color lineColor;
	public Integer linePattern;
	
	public boolean removed = false;
	
	
	public ShapeData(Vertex vertex, XDGFShape shape, AffineTransform globalTransform) {
		
		// calculate bounding boxes + other geometry information we'll need later
		Path2D.Double shapeBounds = shape.getBoundsAsPath();
		
		shapeBounds.transform(globalTransform);
		shapeBounds = GeomUtils.roundPath(shapeBounds);
		
		if (shape.isShape1D()) {
			path1D = shape.getPath();
			path1D.transform(globalTransform);
			path1D = GeomUtils.roundPath(path1D);
			
			calculate1dEndpoints();
		} else {
			path2D = shapeBounds;
		}
		
		this.vertex = vertex;
		this.shapeId = shape.getID();
		this.bounds = SpatialTools.getShapeBounds(shapeBounds);
		this.area = this.bounds.area();
		
		this.lineColor = shape.getLineColor();
		this.linePattern = shape.getLinePattern();
		
		XDGFShape parentShape = shape.getParentShape();
		if (parentShape != null)
			parentId = parentShape.getID(); 
		
		hasText = shape.hasText();
		isTextbox = hasText && !shape.hasMaster() && !shape.hasMasterShape();
		
		if (hasText)
			textCenter = globalTransform.transform(shape.getText().getTextCenter(), null);
	}
	
	// clone 1d shapes
	public ShapeData(long shapeId, Vertex vertex, ShapeData other, Path2D.Double new1dPath) {
		
		this.shapeId = shapeId;
		this.vertex = vertex;
		
		parentId = other.parentId;
		lineColor = other.lineColor;
		linePattern = other.linePattern;
		
		path1D = new1dPath;
		calculate1dEndpoints();
		
		bounds = SpatialTools.getShapeBounds(new1dPath.getBounds2D());
		area = this.bounds.area();
		
		hasText = false;
		isTextbox = false;
	}
	
	public boolean is1d() {
		return path1D != null;
	}
	
	public Path2D getPath() {
		return path1D != null ? path1D : path2D;
	}
	
	protected void calculate1dEndpoints() {
		// can't use beginX et al here, as it's in parent coordinates
		double[] coords = new double[6];
		if (path1D.getPathIterator(null).currentSegment(coords) != PathIterator.SEG_MOVETO) {
			throw new POIXMLException("Invalid 1d path");
		}
		
		path1Dstart = new Point2D.Double(coords[0], coords[1]);
		path1Dend = path1D.getCurrentPoint();
	}
	
	public float getCenterX() {
		return bounds.x1() + (bounds.x2() - bounds.x1())/2.0F; 
	}
	
	public float getCenterY() {
		return bounds.y1() + (bounds.y2() - bounds.y1())/2.0F; 
	}
	
	public String toString() {
		return "[ShapeData " + shapeId + "]";
	}
}
