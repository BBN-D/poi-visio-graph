package com.bbn.poi.xdgf.geom;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class GeomUtils {

	
	public static interface IntersectionVisitor {
		
		public void moveto(double x, double y);
		
		// points are intersection points with this line
		public void lineto(double x, double y, List<Point2D> points);
	}
	
	// splits a path into multiple paths based on intersections
	public static class IntersectionSplitter implements IntersectionVisitor {
		
		List<Path2D.Double> newPaths = new ArrayList<Path2D.Double>();
		Path2D.Double path = new Path2D.Double();
		
		public List<Path2D.Double> getPaths() {
			if (path != null) {
				newPaths.add(path);
				path = null;
			}
			
			return newPaths;
		}
		
		@Override
		public void moveto(double x, double y) {
			path.moveTo(x, y);
		}

		@Override
		public void lineto(double x, double y, List<Point2D> points) {
			if (!points.isEmpty()) {
				// need to sort the points relative to each other
				// FUU. Collections.sort(points);
				for (Point2D pt: points) {
					path.lineTo(pt.getX(), pt.getY());
					newPaths.add(path);
					
					path = new Path2D.Double();
					path.moveTo(pt.getX(), pt.getY());
				}
			}
			
			path.lineTo(x, y);
		}
	}
	
	
	
	// determine if two paths intersect each other, and return the
	// points where they intersect
	public static boolean findIntersections(Path2D path1, Path2D path2, IntersectionVisitor visitor) {
		return findIntersections(path1, path2, visitor, 0.01);
	}
	
	// determine if two paths intersect each other, and return the
	// points where they intersect
	// -> Modified from code at https://community.oracle.com/thread/1263985
	public static boolean findIntersections(Path2D path1, Path2D path2, IntersectionVisitor visitor, double flatness) {
		
        PathIterator pit = path1.getPathIterator(null, flatness);
        double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        boolean retval = false;
        List<Point2D> points = new ArrayList<Point2D>();
        
        while(!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch(type) {
                case PathIterator.SEG_MOVETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    visitor.moveto(lastX, lastY);
                    break;
                case PathIterator.SEG_LINETO:
                    Line2D.Double line = new Line2D.Double(lastX, lastY,
                                                           coords[0], coords[1]);
                    lastX = coords[0];
                    lastY = coords[1];
                    
                    findIntersections(path2, line, points, flatness);
                    visitor.lineto(lastX, lastY, points);
                    retval = retval || !points.isEmpty();
                    points.clear();
            }
            pit.next();
        }
        
        return retval;
    }
	
	// determine if a line intersects a path, and return the points where
	// they intersect
	// -> Modified from code at https://community.oracle.com/thread/1263985
	public static boolean findIntersections(Path2D path, Line2D line, List<Point2D> points, double flatness) {
		
        PathIterator pit = path.getPathIterator(null, flatness);
        double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        while(!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch(type) {
                case PathIterator.SEG_MOVETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    Line2D.Double next = new Line2D.Double(lastX, lastY,
                                                           coords[0], coords[1]);
                    
                    Point2D point = getLineIntersection(next, line);
                    if(point != null) {
                    	points.add(point);
                    }
                    
                    lastX = coords[0];
                    lastY = coords[1];
            }
            pit.next();
        }
        
        return !points.isEmpty();
    }
	
	public static String getLineRepr(Line2D line) {
		return "Line2D[x1=" + line.getX1() + ", y1=" + line.getY1() + ", x2=" + line.getX2() + ", y2=" + line.getY2() + "]";
	}
	
	// find the point where two lines intersect
	// -> Modified from code at https://community.oracle.com/thread/1264395
	protected static Point2D getLineIntersection(Line2D line1, Line2D line2) {
        
		// this code will be correct, use it as a check
		if (!line1.intersectsLine(line2))
			return null;
		
		double px = line1.getX1(),
			   py = line1.getY1(),
			   rx = line1.getX2() - px,
			   ry = line1.getY2() - py;
		double qx = line2.getX1(),
			   qy = line2.getY1(),
			   sx = line2.getX2() - qx,
			   sy = line2.getY2() - qy;
		
		double det = sx * ry - sy * rx;
		if (det == 0) {
			// this means the lines are parallel -- but we know there is some
			// intersection, so logically one of the endpoints must be within
			// the other line. There's probably a more mathy way to do this..
			
			if (line2.ptLineDistSq(px, py) == 0.0) {
				return line1.getP1();
			} else if (line2.ptLineDistSq(line1.getP2()) == 0.0) {
				return line1.getP2();
			} else if (line1.ptLineDistSq(qx, qy) == 0.0) {
				return line2.getP1();
			} else {
				return line2.getP2();
			}
			
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			//if (z == 0 || z == 1)
			//	return null; // intersection at end point!
			return new Point2D.Double((px + z * rx), (py + z * ry));
		}
    }
	
	// determine if two paths intersect each other
	public static boolean pathIntersects(Path2D path1, Path2D path2) {
		return pathIntersects(path1, path2, 0.01);
	}
	
	// determine if two paths intersect each other
	// -> Modified from code at https://community.oracle.com/thread/1263985
	public static boolean pathIntersects(Path2D path1, Path2D path2, double flatness) {
		
        PathIterator pit = path1.getPathIterator(null, flatness);
        double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        while(!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch(type) {
                case PathIterator.SEG_MOVETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    Line2D.Double line = new Line2D.Double(lastX, lastY,
                                                           coords[0], coords[1]);
                    if (pathIntersects(path2, line, flatness)) {
                    	return true;
                    }
                    lastX = coords[0];
                    lastY = coords[1];
            }
            pit.next();
        }
        
        return false;
    }
	
	// determine if a line intersects a path
	// -> Modified from code at https://community.oracle.com/thread/1263985
	public static boolean pathIntersects(Path2D path, Line2D line, double flatness) {
		
        PathIterator pit = path.getPathIterator(null, flatness);
        double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        while(!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch(type) {
                case PathIterator.SEG_MOVETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    Line2D.Double next = new Line2D.Double(lastX, lastY,
                                                           coords[0], coords[1]);
                    if(next.intersectsLine(line)) {
                        return true;
                    }
                    
                    lastX = coords[0];
                    lastY = coords[1];
            }
            pit.next();
        }
        
        return false;
    }
	
	// this is terrible
	public static double pathDistance(Path2D path, Point2D pt) {
		
		PathIterator pit = path.getPathIterator(null, 0.01);
		double distance = Double.MAX_VALUE;
		
		double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        while(!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch(type) {
                case PathIterator.SEG_MOVETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    Line2D.Double next = new Line2D.Double(lastX, lastY,
                                                           coords[0], coords[1]);
                    distance = Math.min(distance, next.ptSegDistSq(pt));
                    
                    lastX = coords[0];
                    lastY = coords[1];
            }
            pit.next();
        }
        
        return distance;
	}
	
}
