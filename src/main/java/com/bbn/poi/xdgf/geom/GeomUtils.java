package com.bbn.poi.xdgf.geom;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GeomUtils {

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
	
	public static boolean pathIntersects(Path2D path, Point2D pt) {
		return pathIntersects(path, pt, 0.01);
	}
	
	// determine if a point lies along a path
	public static boolean pathIntersects(Path2D path, Point2D pt, double flatness) {
		
        PathIterator pit = path.getPathIterator(null, flatness);
        double[] coords = new double[6];
        double ptX = pt.getX(), ptY = pt.getY();
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
                    
                    if (next.intersects(ptX, ptY, ptX, ptY)) {
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
	
	// rounds path to 4 significant places
	public static Path2D.Double roundPath(Path2D inPath) {
		
		Path2D.Double newPath = new Path2D.Double(inPath.getWindingRule());
		
		PathIterator pit = inPath.getPathIterator(null);
		double[] coords = new double[6];
		
		while (!pit.isDone()) {
			int type = pit.currentSegment(coords);
			switch (type) {
				case PathIterator.SEG_CLOSE:
					newPath.closePath();
					break;
				case PathIterator.SEG_CUBICTO:
					newPath.curveTo(trunc4(coords[0]),
						   	   	    trunc4(coords[1]),
						   	   	    trunc4(coords[2]),
						   	   	    trunc4(coords[3]),
						   	   	    trunc4(coords[4]),
						   	   	    trunc4(coords[5]));
					break;
				case PathIterator.SEG_LINETO:
					newPath.lineTo(trunc4(coords[0]),
								   trunc4(coords[1]));
					break;
				case PathIterator.SEG_MOVETO:
					newPath.moveTo(trunc4(coords[0]),
							   	   trunc4(coords[1]));
					break;
				case PathIterator.SEG_QUADTO:
					newPath.quadTo(trunc4(coords[0]),
						   	   	   trunc4(coords[1]),
						   	   	   trunc4(coords[2]),
						   	   	   trunc4(coords[3]));
					break;
				default:
					throw new RuntimeException();
			}
			pit.next();
		}
		
		return newPath;
	}
	
	// copied from groovy-core; Apache 2.0 license
	public static double trunc(Double number, int precision) {
        return Math.floor(number *Math.pow(10,precision))/Math.pow(10,precision);
    }
	
	public static double trunc4(Double number) {
        return Math.floor(number *Math.pow(10,4))/Math.pow(10,4);
    }
	
}
