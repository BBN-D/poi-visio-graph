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

import org.apache.poi.xdgf.usermodel.XDGFShape;

/**
 * The idea is that when 
 */
public class SemanticHelper {

	// called when a shape is created
	public void onCreate(ShapeData newShapeData, XDGFShape shape) {
		
	}
	
	// called when a shape was about to be created, but it was decided that
	// the text should be reassigned to its parent instead
	public void onReassignToParent(ShapeData parentShapeData, XDGFShape shape) {
		
	}
	
	// called when text is assigned to a new shape, and the old shape is about
	// to be removed
	public void onAssignText(ShapeData oldShape, ShapeData newShape) {
		
	}
	
	// if true, allow inference, otherwise disallow
	public boolean onTextInference(ShapeData textNode, ShapeData potentialMatch) {
		return true;
	}
	
	// called when text is
	public void onClone1d(ShapeData oldShape, ShapeData newShape) {
		
	}

	// return true to use the visio document's connections, or false
	// to only use inferred connection mechanisms
	public boolean useRealConnections() {
		return true;
	}

	// return the maximum distance that an object can be from the text
	// in order to assign the text to that shape
	public double textInferenceDistance(ShapeData shapeData) {
		return 0.3;	 // in inches
	}

	
	
}
