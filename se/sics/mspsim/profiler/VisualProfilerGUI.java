package se.sics.mspsim.profiler;

import java.util.List;

import javax.swing.JFrame;

import se.sics.mspsim.util.RecordReader.TreeNode;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

public class VisualProfilerGUI extends JFrame {

	private static final long serialVersionUID = 7634634590456849332L;
	private static mxGraph graph;
	private static Object parent;
	private static int xcoord;
	private static int ycoord;
	private static final int NODE_XSIZE = 200;
	private static final int NODE_YSIZE = 90;
	private static final int SPACE = 250;
	private static mxCell[] cells;
	private static int lastcell;
	private int xsize;
	private int ysize;
	private static VisualProfilerGUI gui;
	private static final String DARKGREEN = "#006400";
	private static final String GREEN = "#00EE00";
	private static final String SHAPE = mxConstants.STYLE_SHAPE + "=" + mxConstants.SHAPE_ELLIPSE + ";";
	private static final String BOXCOLOR = mxConstants.STYLE_FILLCOLOR + "=" + "#008B8B" + ";";
	private static final String FONTCOLOR = mxConstants.STYLE_FONTCOLOR + "=" + "#ffffff" + ";";
	private static final String STYLE = SHAPE + FONTCOLOR + BOXCOLOR;
	
	private VisualProfilerGUI(int xsize, int ysize) {
		super("Program Flow");
		graph = new mxGraph();
		parent = graph.getDefaultParent();
		
		this.xsize = xsize;
		this.ysize = ysize;
		xcoord = xsize / 3 - NODE_XSIZE;
		ycoord = 100;
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(xsize, ysize);
		this.setVisible(true);
		cells = new mxCell[100]; // arbitary number for now
	}
	
	public static VisualProfilerGUI getInstance() {
		if (gui == null) {
			gui = new VisualProfilerGUI(1200, 800);
		}
		return gui;
	}
	
	public void addVerticesWithEdges (String function, List<TreeNode> dependencies) {
		graph.getModel().beginUpdate();
		//mxCell parent = new mxCell();
		mxCell base = null;
		boolean newcell = true;
		
		try {
			Object [] children = graph.getChildCells(parent);
			for (Object child : children) {
				if (function.equals(((mxCell) child).getId())) {
					base = (mxCell) child;
					newcell = false;
				}
			}
			if (newcell)
				base = (mxCell) graph.insertVertex(parent, function, function, xcoord, ycoord, NODE_XSIZE, NODE_YSIZE, STYLE);
			xcoord = 0;
			ycoord += 150;
			for (TreeNode dependency : dependencies) {
				int xspacing = xsize / dependencies.size();
				mxCell v2 = (mxCell) graph.insertVertex(parent, dependency.getValue(), dependency.getValue(), xcoord, ycoord, NODE_XSIZE, NODE_YSIZE, STYLE);
				graph.insertEdge(parent, null, "", base, v2); // may want to change name of edge to number of cycles/time since last call
				xcoord += xspacing;
			}
		} finally {
			mxCompactTreeLayout layout = new mxCompactTreeLayout(graph);
			layout.setHorizontal(false);
			layout.execute(graph.getDefaultParent());
			graph.getModel().endUpdate();
		}
		
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		getContentPane().add(graphComponent);
		this.revalidate();
		//this.pack();
	}
	
	public void addVertex (String function) {
		graph.getModel().beginUpdate();
		try
		{
			if (xcoord + SPACE + NODE_XSIZE >= xsize) {
				xcoord = 20;
				ycoord += SPACE;
			}
			mxCell v2 = null;
			
			if (lastcell == 0) // if this is our initial vertex... 
			{
				v2 = (mxCell) graph.insertVertex(parent, null, function, xcoord, ycoord, NODE_XSIZE, NODE_YSIZE);
			} else {
				// Move the next mxCell to the right of the previous mxCell
				xcoord += SPACE;
				v2 = (mxCell) graph.insertVertex(parent, null, function, xcoord, ycoord, NODE_XSIZE, NODE_YSIZE);
				graph.insertEdge(parent, null, "", cells[lastcell-1], v2); // may want to change name of edge to number of cycles/time since last call
			}
			addCell(v2);
		} finally {
			graph.getModel().endUpdate();
		}
		
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		getContentPane().add(graphComponent);
		this.revalidate();
	}
	
	public boolean colorVertex (String function, VisualProfiler.STATE state) {
		mxCell vertex = null;
		
		Object [] children = graph.getChildCells(parent);
		for (Object child : children) {
			if (function.equals(((mxCell) child).getId())) {
				vertex = (mxCell) child;
			}
		}
		
		if (vertex == null) {
			// This is for functions not parsed by ekhoshim.py...
			return false;
		} else {
			if (state == VisualProfiler.STATE.INFUNCTION) {
				graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, GREEN, new Object [] {vertex});
			} else if (state == VisualProfiler.STATE.LEAVEFUNCTION) {
				graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, DARKGREEN, new Object [] {vertex});
			}
			this.revalidate();
			this.repaint();
			return true;
		}	
	}
	
	public void removeLastNode () {
		mxCell[] node = {cells[--lastcell]}; 
		graph.removeCells(node);
		if (lastcell > 0)
			xcoord -= SPACE;
		this.revalidate();
	}

	private static void addCell (mxCell cell) {
		cells[lastcell++] = cell;
	}
	
	public static void main(String[] args)
	{
		VisualProfilerGUI frame = new VisualProfilerGUI(1200, 820);
		frame.addVertex("main");
		frame.addVertex("_Z18create_sync_packethht");
		frame.addVertex("_ZN11CC1101Radio4InitEv");
		frame.addVertex("_ZN11CC1101Radio7SpiInitEv");
		frame.removeLastNode();
	}
}
