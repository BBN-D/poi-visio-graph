/*
 * Copyright (c) 2015 Raytheon BBN Technologies Corp. All rights reserved.
 */

package com.bbn.poi.xdgf.parsers;

import java.io.IOException;

import com.google.common.collect.Iterators;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;

public class Util {

	public static void saveToGraphml(Graph graph, String filename) {
		
		GraphMLWriter writer = new GraphMLWriter(graph);
		writer.setNormalize(true);
		
		writer.setEdgeLabelKey("label");
		
		System.out.println("** Writing graph to " + filename);
		System.out.println("** -> " + Iterators.size(graph.getVertices().iterator()) + " Nodes" +
		                              Iterators.size(graph.getEdges().iterator()) + " Edges");
		
		try {
			writer.outputGraph(filename);
		} catch (IOException e) {
			System.err.println("Error writing to " + filename + ": " + e.getMessage());
		}
	}
	
}
