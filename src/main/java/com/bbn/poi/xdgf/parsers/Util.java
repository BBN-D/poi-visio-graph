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
