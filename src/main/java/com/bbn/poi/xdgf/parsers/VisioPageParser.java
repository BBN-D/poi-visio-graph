package com.bbn.poi.xdgf.parsers;


import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFConnection;
import org.apache.poi.xdgf.usermodel.XDGFPage;
import org.apache.poi.xdgf.usermodel.XDGFPageContents;
import org.apache.poi.xdgf.usermodel.XDGFShape;
import org.apache.poi.xdgf.usermodel.XDGFText;
import org.apache.poi.xdgf.usermodel.shape.ShapeDataAcceptor;
import org.apache.poi.xdgf.usermodel.shape.ShapeVisitor;

import rx.Observable;

import com.bbn.poi.xdgf.geom.GeomUtils;
import com.bbn.poi.xdgf.parsers.rx.Rx;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Turns a visio page into a graph
 */
public class VisioPageParser {

	protected Graph graph;
	
	protected class SplitData {
		public ShapeData s1;
		public ShapeData s2;
		
		public SplitData(ShapeData s1, ShapeData s2) {
			this.s1 = s1;
			this.s2 = s2;
		}
	}
	
	protected class IntersectionData {
		public ShapeData other;
		public Point2D point;

		public IntersectionData(ShapeData other, Point2D point) {
			this.other = other;
			this.point = point;
		}
	}
	
	// indices
	protected RTree<ShapeData, Rectangle> rtree = RTree.create();
	protected final Map<Long, ShapeData> shapesMap = new HashMap<>();
	protected final List<ShapeData> shapes = new ArrayList<>();
	protected final Map<String, Edge> edges = new HashMap<>();
	
	// convenience
	protected final long pageId;
	protected final String pageName;
	protected final XDGFPageContents pageContents;
	
	// for allocating new shapes -- decrement each time a new shape is created
	protected long shapeIdAllocator = -42;

	public VisioPageParser(XDGFPage page) {
		this(page, new TinkerGraph());
	}
	
	public VisioPageParser(XDGFPage page, Graph graph) {
		this.graph = graph;
		
		pageId = page.getID();
		pageName = page.getName();
		pageContents = page.getContent();
	}
	
	public Graph getGraph() {
		return graph;
	}
	
	// processes the page and creates a graph from it
	public void process() {
		
		// TODO: there are a lot of O(N) operations here... 
		
		collectShapes();
		collectConnections();
		
		joinGroupedShapes();
		addGroupLabels();
		inferConnections();
		associateText();
	}
	
	// create vertices from interesting shapes
	protected void collectShapes() {
		
		pageContents.visitShapes(new ShapeVisitor() {
			
			@Override
			public org.apache.poi.xdgf.usermodel.shape.ShapeVisitorAcceptor getAcceptor() {
				return new ShapeDataAcceptor();
			};
			
			@Override
			public void visit(XDGFShape shape, AffineTransform globalTransform, int level) {
				
				// apply labels to parents
				if (shape.hasText() && shape.hasParent()) {
					
					XDGFShape parent = shape.getParentShape();
					ShapeData parentData = shapesMap.get(parent.getID());
					if (parentData != null && !parentData.hasText && Math.abs(shape.getWidth() - parent.getWidth()) < 0.001)
					{
						XDGFText text = shape.getText();
						
						parentData.vertex.setProperty("label", text.getTextContent());
						parentData.vertex.setProperty("textRef", shape.getID());
						parentData.hasText = true;
						parentData.textCenter = text.getTextCenter();
						return;
					}
				}
				
				String id = pageId + ": " + shape.getID();
				Vertex vertex = graph.addVertex(id);
				
				// useful properties for later... 
				vertex.setProperty("label", shape.getTextAsString());
				vertex.setProperty("shapeId", shape.getID());
				
				vertex.setProperty("group", "");
				vertex.setProperty("is1d", shape.isShape1D());
				vertex.setProperty("name", shape.getName());
				vertex.setProperty("pageName", pageName);
				vertex.setProperty("symbolName", shape.getSymbolName());
				vertex.setProperty("type", shape.getShapeType());
				
				// this isn't actually accurate
				//vertex.setProperty("visible", shape.isVisible());
				
				// add to the tree
				// - RTree only deals with bounding rectangles, so we need
				//   to get the bounding rectangle in local coordinates, put
				//   into a polygon (to allow accounting for rotation), and
				//   then get the minimum bounding rectangle.
				
				// TODO: is it worth setting up a custom geometry?
				// -> problem with a custom geometry is that calculating the
				//    distance between objects would be annoying
				
				// local coordinates
				
				ShapeData shapeData = new ShapeData(vertex, shape, globalTransform);
				rtree = rtree.add(shapeData, shapeData.bounds);

				shapesMap.put(shape.getID(), shapeData);
				shapes.add(shapeData);
			}
		});
		
		Collections.sort(shapes, new ShapeData.OrderByLargestAreaFirst());
	}
	
	// create a list of the existing ('real', not inferred) connections
	protected void collectConnections() {
		for (XDGFConnection conn: pageContents.getConnections()) {
			createEdge(conn.getFromShape(), conn.getToShape(), "real");
		}
	}
	
	protected void joinGroupedShapes() {
		
		// find groups of shapes that are visually together
		// - Must overlap
		// - Must be same type
		// - Must not contain each other
		// - Only one must have text
		
		// we can't actually make a group here, as that makes things like
		// bounding rectangles annoying. Instead, just link them together
		// with an edge 
		
		// insert naive implementation here
		for (final ShapeData shapeData: shapes) {
			
			if (shapeData.is1d())
				continue;
			
			final String symbolName = shapeData.vertex.getProperty("symbolName");
			if (symbolName.equals(""))
				continue;
			
			Observable<Entry<ShapeData, Rectangle>> entries = rtree.search(shapeData.bounds);
			
			entries.forEach(new Rx.RTreeAction() {

				@Override
				public void call(Entry<ShapeData, Rectangle> entry) {
					ShapeData other = entry.value();
					
					if (other == shapeData || other.is1d())
						return;
					
					float area = Math.min(shapeData.area, other.area);
					
					// if the intersection is equal to the area of the smallest, then
					// we can assume one of them contains the other
					// .. don't want those to be joined
					
					if (!other.vertex.getProperty("symbolName").equals(symbolName) || 
						shapeData.bounds.intersectionArea(other.bounds) >= area) {
						return;
					}
					
					// but if it doesn't contain, then link them together
					createEdge(shapeData, other, "linked");
				}
			});
		}
	}
	
	protected void addGroupLabels() {
		
		// this step finds user defined shapes that contain other shapes
		// visually, but aren't organized as groups
		
		// for those shapes, we pretend they're being used as groups, and set
		// the 'group' key to something
		
		// additionally, if a shape denotes a group, it is excluded from being
		// connected to, and its vertex is removed from the tree/shapedata
		
		for (final ShapeData shapeData: shapes) {
			
			if (shapeData.is1d() || !shapeData.hasText)
				continue;
			
			final long shapeTopLevelId = findTopmostParent(shapeData).shapeId;
			
			final List<ShapeData> containedShapes = new LinkedList<>();
			
			Observable<Entry<ShapeData, Rectangle>> entries = rtree.search(shapeData.bounds);
			
			entries.forEach(new Rx.RTreeAction() {

				@Override
				public void call(Entry<ShapeData, Rectangle> e) {
					ShapeData other = e.value();
					
					// include 1d shapes? no
					if (other.is1d())
						return;
					
					// if it visually contains it
					if (shapeData.bounds.intersectionArea(other.bounds) >= other.area) {
						
						// AND if they're not in the same hierarchy
						if (shapeTopLevelId != findTopmostParent(other).shapeId) { 
							containedShapes.add(other);
						}
					}
					
				}
			});
			
			if (!containedShapes.isEmpty()) {
				String groupName = shapeData.vertex.getProperty("label");
				
				for (ShapeData other: containedShapes) {
					other.vertex.setProperty("group", groupName);
				}
				
				removeShape(shapeData);
			}
		}
		
		cleanShapes();
	}
	
	
	
	protected void inferConnections() {
		
		// first, infer connections between 1d objects and 2d objects,
		// split the 1d objects where they join the 2d objects, and make
		// connections. Do this first because this case is easier to deal
		// with than the combined case
		
		LinkedList<ShapeData> newShapes = new LinkedList<>();
		
		for (ShapeData shapeData: shapes) {
			if (shapeData.is1d()) {
				infer2dConnections(shapeData, newShapes);
			}
		}
		
		// add the new shapes, remove the old shapes
		cleanShapes();
		
		for (ShapeData shapeData: newShapes) {
			shapes.add(shapeData);
			shapesMap.put(shapeData.shapeId, shapeData);
		}
		
		//
		// Do a dumb algorithm to find 1d lines that overlap but aren't
		// connected, and connect them
		//
		
		for (ShapeData shapeData: shapes) {
			if (shapeData.is1d()) {
				infer1dConnections(shapeData);
			}
		}
		
		// next, try to collect all 1d networks, and replace the lines
		// with new lines more fully representing the connectedness of
		// the graph
		
		/**
		 * Alternate idea for dealing with 1D lines:
		 * 
		 * - Gather all the neighborhoods of connected lines
		 * - For each neighborhood:
		 * 		- Break into path components
		 * 		- Make appropriate connections to 2d objects
		 *      - Iterate path components, find intersections
		 *      - Create a mini-graph from this
		 *      - Create an elided graph by traversal? 
		 * 
		 */
	}
	
	protected void infer2dConnections(final ShapeData shapeData, LinkedList<ShapeData> newShapes) {
		
		final Set<ShapeData> connections = new HashSet<>();
		
		// create a list of real things that I'm attached to
		final Set<Vertex> attached = Sets.newHashSet(shapeData.vertex.getVertices(Direction.BOTH, "real"));
		
		// identify any shapes that it overlaps with
		// add that shape to the list of connections
		Observable<Entry<ShapeData, Rectangle>> entries = rtree.search(shapeData.bounds);
		
		entries.subscribe(new Rx.RTreeSubscriber() {

			@Override
			public void onNext(Entry<ShapeData, Rectangle> e) {
				
				ShapeData other = e.value();
				
				// discard 1d shapes, shapes that are already attached, or shapes
				// that don't intersect
				if (other.is1d())
					return;
				
				if (attached.contains(other.vertex))
					return;
				
				if (!GeomUtils.pathIntersects(shapeData.path1D, other.path2D))
					return;
				
				// if we get here, then we've inferred a new connection
				
				// if either of this line's endpoints are inside the 2d shape,
				// then just create a connection and be done with it
				if (other.path2D.contains(shapeData.path1Dstart) || other.path2D.contains(shapeData.path1Dend)) {
					createEdge(shapeData, other, "inferred-2d");
				} else {
					connections.add(other);
				}
			}
		});
		
		if (connections.isEmpty())
			return;
		
		// add existing connections to the list, removing those edges
		// -> would like to avoid this... not sure how
		
		// ok, the existing connection data has a record of the connection
		// point, so we can use that if we stored it... but we don't
		
		List<ShapeData> connectedToStart = new LinkedList<>();
		List<ShapeData> connectedToEnd = new LinkedList<>();
		
		for (Edge edge: shapeData.vertex.getEdges(Direction.BOTH)) {
			ShapeData in = getShapeFromEdge(edge, Direction.IN);
			ShapeData out = getShapeFromEdge(edge, Direction.OUT);
			ShapeData other;
			Path2D otherPath;
			
			if (in != shapeData) {
				other = in;
			} else if (out != shapeData) {
				other = out;
			} else {
				throw new POIXMLException("Internal error processing existing connections");
			}
			
			otherPath = other.getPath();
			
			if (otherPath.contains(shapeData.path1Dstart))
				connectedToStart.add(other);
			else if (otherPath.contains(shapeData.path1Dend))
				connectedToEnd.add(other);
			else
				connections.add(in);
				
			graph.removeEdge(edge);
		}
		
		// at this point, all items in connections must be in the middle somewhere,
		// and will cause a split of some kind
		
		// list of new shapedata
		Path2D.Double currentPath = new Path2D.Double();
		
		ShapeData lastShape = null;
		
		Point2D textCenter = shapeData.textCenter;
		ShapeData textShape = null;
		double textDistance = Double.MAX_VALUE;
		
		PathIterator pit = shapeData.path1D.getPathIterator(null, 0.01);
		double[] coords = new double[6];
        double lastX = 0, lastY = 0;
        final Point2D firstPt = shapeData.path1Dstart;
		
		while (!pit.isDone()) {
			
			int type = pit.currentSegment(coords);
            switch(type) {
            	case PathIterator.SEG_MOVETO:
            		currentPath.moveTo(coords[0], coords[1]);
            		break;
            	case PathIterator.SEG_LINETO:
            		
            		Line2D.Double line = new Line2D.Double(lastX, lastY,
							   coords[0], coords[1]);
            		
            		List<IntersectionData> intersections = new ArrayList<>();
            		
            		for (ShapeData connectedShape: connections) {
            			
            			List<Point2D> points = new LinkedList<>();
            			
            			if (GeomUtils.findIntersections(connectedShape.getPath(), line, points, 0.01)) {
            				// found a split point, add it to the list
            				for (Point2D point: points) {
            					intersections.add(new IntersectionData(connectedShape, point));
            				}
            			}
            		}
            		
            		if (intersections.isEmpty()) {
            			currentPath.lineTo(coords[0], coords[1]);
            		} else {
            			// sort the points from start to finish
            			Collections.sort(intersections, new Comparator<IntersectionData>() {
							@Override
							public int compare(IntersectionData o1, IntersectionData o2) {
								
								double d = firstPt.distance(o1.point) - firstPt.distance(o2.point);
								if (d < 0)
									return -1;
								else if (d > 0)
									return 1;
								else
									return 0;  
							}
            			});
            			
            			for (IntersectionData intersection: intersections) {
	            			
            				// elide connections to self
            				if (lastShape == intersection.other)
            					continue;
            				
            				// ok. create a path and stuff.
            				currentPath.lineTo(intersection.point.getX(), intersection.point.getY());
	            			
	            			// create a new shapeData
	            			ShapeData thisShape = clone1dShape(currentPath, shapeData);
	            			newShapes.add(thisShape);
	            			
	            			// see if this is closest to the text
	            			if (shapeData.hasText) {
		            			double thisTextDistance = GeomUtils.pathDistance(thisShape.path1D, textCenter);
		            			if (thisTextDistance < textDistance)
		            				textShape = thisShape;
	            			}
	            			
	            			// create edges joining them
	            			if (lastShape == null) {
	            				for (ShapeData shape: connectedToStart)
	            					createEdge(thisShape, shape, "inferred2d-split-start");
	            				
	            			} else {
	            				createEdge(lastShape, thisShape, "inferred2d-split-middle");
	            			}
	            			
	            			createEdge(thisShape, intersection.other, "inferred2d-split-middle");
	            			
	            			lastShape = intersection.other;
            			
	            			currentPath = new Path2D.Double();
	            			currentPath.moveTo(intersection.point.getX(), intersection.point.getY());
            			}
            			
            			currentPath.lineTo(coords[0], coords[1]);
            		}
            		
            		break;
            	default:
            		throw new POIXMLException();
            }
            
            lastX = coords[0];
            lastY = coords[1];
			
			pit.next();
		}
		
		
		// finish off the path here
		ShapeData thisShape = clone1dShape(currentPath, shapeData);
		newShapes.add(thisShape);
		
		// see if this is closest to the text
		if (shapeData.hasText) {
			double thisTextDistance = GeomUtils.pathDistance(thisShape.path1D, textCenter);
			if (thisTextDistance < textDistance)
				textShape = thisShape;
			
			// ok, now that it's done, reassociate the text
			textShape.hasText = true;
			textShape.textCenter = shapeData.textCenter;
			textShape.vertex.setProperty("label", shapeData.vertex.getProperty("label"));
			textShape.vertex.setProperty("textRef", shapeData.shapeId);
		}
		
		createEdge(lastShape, thisShape, "inferred2d-split-next-end");
		
		for (ShapeData shape: connectedToEnd)
			createEdge(thisShape, shape, "inferred2d-split-end");
		
		removeShape(shapeData);
	}
	
	protected void infer1dConnections(final ShapeData shapeData) {
		
		// create a list of real things that I'm attached to
		final Set<Vertex> attached = Sets.newHashSet(shapeData.vertex.getVertices(Direction.BOTH, "real"));
		
		// identify any shapes that it overlaps with
		// add that shape to the list of connections
		Observable<Entry<ShapeData, Rectangle>> entries = rtree.search(shapeData.bounds);
		
		entries.subscribe(new Rx.RTreeSubscriber() {
			
			@Override
			public void onNext(Entry<ShapeData, Rectangle> e) {
				
				ShapeData other = e.value();
				
				if (other == shapeData || other.removed || !other.is1d())
					return;
				
				// don't infer connections between lines of different colors
				// or different line patterns
				if (!shapeData.lineColor.equals(other.lineColor) || shapeData.linePattern != other.linePattern) {
					return;
				}
				
				// compute if they intersect
				if (!GeomUtils.pathIntersects(shapeData.path1D, other.path1D)) {
					return;
				}
				
				// TODO
				// if they are both dynamic connectors, don't create connections
				// unless their intersection is at the end of a line?
				// alternatively, try to check if the intersection happens at an
				// 'arcto' point. if so, discard, as that's a 'clear' visual indicator
				// that it should not be connected
				
				// do not create edges between 1d objects IF they both have
				// connections to the same 2d object
				// .. and their intersection is at that object?
				
				for (Vertex v: other.vertex.getVertices(Direction.BOTH)) {
					if (attached.contains(v) && (boolean)v.getProperty("is1d") == false) {
						return;
					}
				}
				
				// ok, we've gotten here, create a connection between the two lines
				createEdge(shapeData, other, "inferred-1d");
			}
		});
	}
	
	protected void associateText() {
		
		// ordered by largest first
		for (ShapeData shapeData: shapes) {
			
			if (shapeData.hasText || shapeData.removed)
				continue;
			
			associateTextWithShape(shapeData);
		}
		
		cleanShapes();
	}
	
	
	/**
	 * this takes a shape that doesn't have text associated with it
	 */
	protected void associateTextWithShape(final ShapeData shapeData) {
		
		final Vertex vertex = shapeData.vertex; 
		
		// limit the search to some reasonable number/distance (TODO: what is reasonable)
		
		Observable<Entry<ShapeData, Rectangle>> entries = rtree.nearest(shapeData.bounds, .6, rtree.size());
		
		entries.subscribe(new Rx.RTreeSubscriber() {
			
			@Override
			public void onNext(Entry<ShapeData, Rectangle> e) {
				
				ShapeData other = e.value();
				
				// find the nearest textbox, and steal its text
				
				if (!other.isTextbox || other.removed)
					return;
				
				vertex.setProperty("label", other.vertex.getProperty("label"));
				vertex.setProperty("textRef", other.shapeId);
				shapeData.hasText = true;
				shapeData.textCenter = other.textCenter;
				
				// move any edges from other to us
				for (Edge edge: other.vertex.getEdges(Direction.BOTH)) {
					
					ShapeData in = getShapeFromEdge(edge, Direction.IN);
					ShapeData out = getShapeFromEdge(edge, Direction.OUT);
					
					if (in != shapeData && out != shapeData) {
						if (in == other)
							createEdge(out, shapeData, "reparent");
						else if (out == other)
							createEdge(in, shapeData, "reparent");
						else
							throw new POIXMLException("Internal error");
					}
					
					graph.removeEdge(edge);
				}
				
				// remove the textbox from the tree so others can't use it
				removeShape(other);
				
				// TODO: probably want to be more intelligent, and assign the text to
				//       things that are nearer in a particular direction, taking 
				//       advantage to how a human might naturally align the text..
				
				// done with this
				unsubscribe();
			}
		});
	}
	
	
	protected void createEdge(XDGFShape shape1, XDGFShape shape2, String edgeType) {
		
		ShapeData sd1 = findShape(shape1.getID());
		ShapeData sd2 = findShape(shape2.getID());
		
		if (sd1 == null) 
			throw new POIXMLException("Cannot find from node " + shape1.getID());
		
		if (sd2 == null) 
			throw new POIXMLException("Cannot find to node " + shape2.getID());
		
		// TODO: how to deal with from/to being null? Might happen.
		
		createEdge(sd1, sd2, edgeType);
	}
	
	
	// edgeType is a string describing where the edge came from
	protected void createEdge(ShapeData sd1, ShapeData sd2, String edgeType) {
	
		// note: visio doesn't always support direction, and neither do we. So, to
		//       save time, and make sure we don't accidentally create duplicate 
		//       edges, we sort by id
		
		ShapeData from = sd1;
		ShapeData to = sd2;
		
		if (sd1.shapeId > sd2.shapeId) {
			from = sd2;
			to = sd1;
		}
		
		String eId = getConnId(from, to);
		
		Edge edge = graph.getEdge(eId);
		if (edge == null) {
			edge = graph.addEdge(eId, from.vertex, to.vertex, edgeType);
		}
	}
	
	
	protected ShapeData findShape(long id) {
		
		ShapeData sd = shapesMap.get(id);
		if (sd != null)
			return sd;
		
		// find a parent that is in the graph already
		XDGFShape shape = pageContents.getShapeById(id);
		
		while (sd == null) {
			shape = shape.getParentShape();
			if (shape == null)
				break;
			
			sd = shapesMap.get(shape.getID());
		}
		
		return sd;
	}
	
	protected ShapeData findTopmostParent(ShapeData shapeData) {
		while (shapeData.parentId != null)
			shapeData = shapesMap.get(shapeData.parentId);
		
		return shapeData;
	}
	
	protected String getConnId(ShapeData from, ShapeData to) {
		
		String fromId = "" + from.shapeId;
		String toId = "" + to.shapeId;
		
		return pageId + ": " + fromId + " -> " + toId;
	}
	
	protected ShapeData getShapeFromEdge(Edge edge, Direction direction) {
		return shapesMap.get((Long)edge.getVertex(direction).getProperty("shapeId"));
	}
	
	protected void cleanShapes() {
		
		Iterator<ShapeData> i = shapesMap.values().iterator();
		
		while (i.hasNext()) {
			if (i.next().removed)
				i.remove();
		}
		
		i = shapes.iterator();
		
		while (i.hasNext()) {
			if (i.next().removed)
				i.remove();
		}
	}
	
	protected void removeShape(ShapeData shapeData) {
		shapeData.removed = true;
		graph.removeVertex(shapeData.vertex);
		rtree = rtree.delete(new Entry<ShapeData, Rectangle>(shapeData, shapeData.bounds));
	}
	
	protected ShapeData clone1dShape(Path2D.Double newPath, ShapeData oldShape) {
		
		long shapeId = shapeIdAllocator--;
		
		Vertex oldVertex = oldShape.vertex;
		Vertex vertex = graph.addVertex(pageId + ": " + shapeId);
		
		// copy properties
		for (String p: oldVertex.getPropertyKeys())
			vertex.setProperty(p, oldVertex.getProperty(p));
		
		vertex.setProperty("label", "");
		vertex.setProperty("shapeId", shapeId);
		
		ShapeData newShape = new ShapeData(shapeId, vertex, oldShape, newPath);
		rtree = rtree.add(newShape, newShape.bounds);
		
		return newShape;
	}
	
}
