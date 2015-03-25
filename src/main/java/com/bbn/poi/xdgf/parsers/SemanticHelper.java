package com.bbn.poi.xdgf.parsers;

import org.apache.poi.xdgf.usermodel.XDGFShape;

/**
 * The idea is that when 
 */
public class SemanticHelper {

	public void onCreate(ShapeData newShapeData, XDGFShape shape) {
		
	}
	
	public void onAssignText(ShapeData shapeData) {
		
	}
	
	// if true, allow inference, otherwise disallow
	public boolean onTextInference(ShapeData textNode, ShapeData potentialMatch) {
		return true;
	}
	
	public void onClone1d(ShapeData oldShape, ShapeData newShape) {
		
	}

	public boolean useRealConnections() {
		return true;
	}

	public double textInferenceDistance(ShapeData shapeData) {
		return 0.3;	 // in inches
	}
	
}
