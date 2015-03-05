package com.bbn.poi.xdgf.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.xdgf.usermodel.XDGFPage;
import org.apache.poi.xdgf.usermodel.XmlVisioDocument;
import org.apache.poi.xdgf.usermodel.shape.ShapeDataAcceptor;
import org.apache.poi.xdgf.usermodel.shape.ShapeDebuggerRenderer;
import org.apache.poi.xdgf.util.VsdxToPng;

import com.tinkerpop.blueprints.Graph;

/*
 * To get POI log messages, set the following Java VM properties:
 * 
 * -Dorg.apache.poi.util.POILogger=org.apache.poi.util.SystemOutLogger -Dpoi.log.level=1
 * 
 */
public class VisioParser {

	/*
	 new ShapeRenderer(graphics) {
			
        	@Override
        	public org.apache.poi.xdgf.usermodel.shape.ShapeVisitorAcceptor getAcceptor() {
        		return new ShapeDataAcceptor();
        	};
        	
        	@Override
        	public void drawPath(XDGFShape shape) {
        		_graphics.setStroke(new BasicStroke(shape.getLineWeight().floatValue()));
        		
        		//System.out.println("Shape: " + shape);
        		if (shape.isShape1D())
        			_graphics.draw(shape.getPath());
        		else
        			_graphics.draw(shape.getBounds());
        	}
		}
	 */
	
	
	XmlVisioDocument xmlDoc;

	public VisioParser(File vsdxFile) throws FileNotFoundException, IOException
	{
		this(new FileInputStream(vsdxFile));
	}
	
	public VisioParser(FileInputStream vsdxFile) throws IOException {
		xmlDoc = new XmlVisioDocument(vsdxFile);
		
		processPages();
	}
	
	protected void processPages() {
		for (XDGFPage page: xmlDoc.getPages()) {
			
			System.out.println(page.getID() + " " + page.getName());
			
			try {
				ShapeDebuggerRenderer renderer = new ShapeDebuggerRenderer();
				renderer.setDebugAcceptor(new ShapeDataAcceptor());
				
				VsdxToPng.renderToPngDir(page, new File("pngdir"), 2000.0/11.0, renderer);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			Graph graph = processPage(page);
			
			Util.saveToGraphml(graph, "pngdir/output.graphml");
			
			break;
		}
	}
	
	protected Graph processPage(XDGFPage page) {
		VisioPageParser parser = new VisioPageParser(page);
		parser.process();
		return parser.getGraph();
	}
	
	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) {
			System.err.println("Usage: VisioParser infile outfile pngdir");
			System.exit(1);
		}
		
		String inFilename = args[0];
		String outFilename = args[1];
		String pngDir = args[2];
		
		VisioParser parser = new VisioParser(new File(inFilename));
		//Util.saveToGraphml(parser.graph, outFilename);
		
		//VsdxToPng.renderToPng(parser.xmlDoc, pngDir, 2000/11.0);
		
		System.out.println("Done.");
	}

}
