/*
 * Copyright (c) 2015 Raytheon BBN Technologies Corp
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified from code at https://community.oracle.com/thread/1264395
 * Modified from code at https://community.oracle.com/thread/1263985
 */

package com.bbn.poi.xdgf.geom;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.List;

public class GeomUtils {

	public static boolean arePointsEqual(double x1, double y1, double x2, double y2) {
		return Math.abs(x1 - x2) < 0.0001 && Math.abs(y1 - y2) < 0.0001;
	}
	
	public static boolean isInsideOrOnBoundary(Path2D path, Point2D pt) {
		return path.contains(pt) || pathIntersects(path, pt);
	}
	
	// determine if a path intersects a path, and return the points where
	// they intersect
	public static boolean findIntersections(Path2D path1, Path2D path2, List<Point2D> points, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path1, null, flatness); 
		
		while (psit.next()) {
			if (psit.pt != null) {
				findIntersections(path2, psit.pt, points, flatness);
			} else {
				findIntersections(path2, psit.line, points, flatness);
			}
		}
		
        return !points.isEmpty();
    }
	
	// determine if a line intersects a path, and return the points where
	// they intersect
	public static boolean findIntersections(Path2D path, Line2D line, List<Point2D> points, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path, null, flatness); 
		
		while (psit.next()) {
			
			if (psit.intersects(line)) {
				if (psit.pt != null)
					points.add(psit.pt);
				else
					points.add(getLineIntersection(psit.line, line));
			}
		}
		
        return !points.isEmpty();
    }
	
	// determine if a line intersects a path, and return the points where
	// they intersect
	public static boolean findIntersections(Path2D path, Point2D pt, List<Point2D> points, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path, null, flatness); 
		
		while (psit.next()) {
			if (psit.intersects(pt)) {
				points.add(psit.pt);
			}
		}
		
        return !points.isEmpty();
	}
	
	public static String getLineRepr(Line2D line) {
		return "Line2D[x1=" + line.getX1() + ", y1=" + line.getY1() + ", x2=" + line.getX2() + ", y2=" + line.getY2() + "]";
	}
	
	// find the point where two lines intersect
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
	public static boolean pathIntersects(Path2D path1, Path2D path2, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path1, null, flatness); 
		
		while (psit.next()) {
			if (psit.pt != null) {
				if (pathIntersects(path2, psit.pt, flatness)) {
                	return true;
                }
			} else {
				if (pathIntersects(path2, psit.line, flatness))
					return true;
			}
		}
		    
        return false;
    }
	
	// determine if a line intersects a path
	public static boolean pathIntersects(Path2D path, Line2D line, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path, null, flatness); 
		
		while (psit.next()) {
			if (psit.intersects(line)) {
				return true;
			}
		}
        
        return false;
    }
	
	public static boolean pathIntersects(Path2D path, Point2D pt) {
		return pathIntersects(path, pt, 0.01);
	}
	
	// determine if a point lies along a path
	public static boolean pathIntersects(Path2D path, Point2D pt, Double flatness) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path, null, flatness); 
		double ptX = pt.getX(), ptY = pt.getY();
		
		while (psit.next()) {
			if (psit.pt != null) {
				if (psit.pt.equals(pt)) {
                	return true;
                }
			} else {
				if (psit.line.intersects(ptX - 0.00001, ptY - 0.00001, 0.00002, 0.00002)) {
                	return true;
                }
			}
		}
		    
        return false;
    }
	
	// this is terrible
	public static double pathDistance(Path2D path, Point2D pt) {
		
		PathSegmentIterator psit = new PathSegmentIterator(path, null, 0.01); 
		double distance = Double.MAX_VALUE;
		
		while (psit.next()) {
			if (psit.pt != null) {
				distance = Math.min(distance, psit.pt.distance(pt));
			} else {
				distance = Math.min(distance, psit.line.ptSegDist(pt));
			}
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
	// -> However, Groovy uses Math.floor... but that fails for what we need 
	public static double trunc(Double number, int precision) {
        return Math.round(number *Math.pow(10,precision))/Math.pow(10,precision);
    }
	
	public static double trunc4(Double number) {
        return Math.round(number *Math.pow(10,4))/Math.pow(10,4);
    }
	
	
	public static class PathSegmentIterator {
		
		public Point2D pt = null;
		public Line2D line = null;
		
		final PathIterator pit;
		
		double[] coords = new double[6];
		double lastX, lastY;
		
		
		public PathSegmentIterator(Path2D path, AffineTransform at, Double flatness) {
			if (flatness == null)
				pit = path.getPathIterator(at);
			else
				pit = path.getPathIterator(at, flatness);
		}
		
		// returns true if a point or line is available, false otherwise
		public boolean next() {
			
			while(!pit.isDone()) {
	            int type = pit.currentSegment(coords);
	            switch(type) {
	                case PathIterator.SEG_MOVETO:
	                    lastX = coords[0];
	                    lastY = coords[1];
	                    break;
	                case PathIterator.SEG_LINETO:
	                	if (arePointsEqual(lastX, lastY, coords[0], coords[1])) {
	                		line = null;
	                		pt = new Point2D.Double(lastX, lastY);
	                	} else {
	                		pt = null;
		                    line = new Line2D.Double(lastX, lastY,
		                                             coords[0], coords[1]);
	                	}
	                	
	                	lastX = coords[0];
	                    lastY = coords[1];
	                	pit.next();
	                	return true;
	                case PathIterator.SEG_QUADTO:
	                	lastX = coords[4];
	                    lastY = coords[5];
	                	break;
	                case PathIterator.SEG_CUBICTO:
	                	lastX = coords[2];
	                    lastY = coords[3];
	                	break;
	            }
	            
	            pit.next();
			}
			
			return false;
		}
		
		// only applies to current segment
		boolean intersects(Line2D line) {
			if (this.pt != null) {
				return line.ptLineDist(pt) == 0.0;
			} else {
				return this.line.intersectsLine(line);
			}
		}
		
		// only applies to current segment
		boolean intersects(Point2D pt) {
			if (this.pt != null) {
				return this.pt.equals(pt);
			} else {
				return line.ptLineDist(pt) == 0.0;
			}
		}
	}
}
