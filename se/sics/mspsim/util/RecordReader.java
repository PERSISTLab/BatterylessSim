package se.sics.mspsim.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import se.sics.mspsim.profiler.VisualProfilerGUI;

import com.opencsv.CSVReader;

public class RecordReader {

	private static class DAG {
		private List<TreeNode> allNodes = new ArrayList<>();
		private TreeNode root;
		
		public DAG () {};
		
		public void addNode (String value, List<String> dependencies) {
			TreeNode treenode = null;
			
			if (allNodes.contains(new TreeNode(value))) {
				for (TreeNode node : allNodes) {
					if (node.getValue().equals(value)) {
						treenode = node;
					}
				}
			} else {
				treenode = new TreeNode(value);
				if (value.equals("main")) {
					root = treenode;
				}
				allNodes.add(treenode);
			}
			for (String dep : dependencies) {
				boolean seen_before = false;
				for (TreeNode node : allNodes) {
					if (node.getValue().equals(dep)) {
						treenode.addChildren(node);
						seen_before = true;
					}
				}
				if (!seen_before) {
					TreeNode newnode = new TreeNode(dep);
					treenode.addChildren(newnode);
					allNodes.add(newnode);
				}
			}
		}
		
		public String toString () {
			String ret = "Root node: " + root.getValue() + "\n";
			
			ret += toStringRecur(root.getChildren(), 1);
			
			return ret; 
		}
		
		public String toStringRecur(List<TreeNode> nodes, int indent) {
			String ret = "";
			
			for (TreeNode node : nodes) {
				for (int i = 0; i < indent; i++)
					ret += "\t";
				
				ret += "Child node: " + node.getValue() + "\n";
				ret += toStringRecur(node.getChildren(), indent + 1);
			}
			
			return ret;
		}
		
		public List<TreeNode> getAllNodes() {
			return allNodes;
		}
		
		public TreeNode getRoot () {
			return root;
		}
	}
	
	public static class TreeNode {
		private List<TreeNode> children = new LinkedList<>();
		private String value;
				
		public TreeNode (String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public void addChildren (TreeNode node) {
			children.add(node);
		}
		
		public List<TreeNode> getChildren ()
	    {
			return children;
	    }
		
		@Override
		public boolean equals(Object v) {
			boolean retVal = false;

			if (v instanceof TreeNode){
				TreeNode ptr = (TreeNode) v;
				retVal = ptr.value.equals(this.value);
			}

			return retVal;
		}
	}
	
	private CSVReader reader;
	public DAG dag;
	private static final String HEADER = "Function Name";
	private List<TreeNode> nodes_from_main;
	private VisualProfilerGUI frame = VisualProfilerGUI.getInstance();
	
	public RecordReader(String record) {
		try {
			reader = new CSVReader(new FileReader(record));
		} catch (FileNotFoundException e) {
			System.out.println("CSV record file not found!");
		}
		dag = new DAG();
		
		try {
			readRecordCSV();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nodes_from_main = new ArrayList<>();
	}
	
	private void readRecordCSV () throws IOException {
		String [] nextLine;
		List<String> ids = new ArrayList<>();
		while ((nextLine = reader.readNext()) != null) {
			List<String> dependencies = new ArrayList<>();
				
			if (!ids.contains(nextLine[0])) { // This is a hack... the issue is that the ekhoshim does not handle switch statements properly
											  // I'd like to continue to look at this, but for the time being this works.
				for (String dependency : nextLine[5].split(",")) {
					if (!dependency.equals("Dependencies") && dependency.length() > 2) {
						String d = dependency.substring(dependency.indexOf("'") + 1, dependency.indexOf("'", 2));
						dependencies.add(d);
					}
				}
				
				if (nextLine[4].length() > 0 && !(nextLine[4].equals(HEADER))) {
					String parent = nextLine[4];
					dag.addNode(parent, dependencies);
				}
			}
			ids.add(nextLine[0]);
		}
	}
	
	public void dagToFrame () {
		TreeNode root = dag.getRoot();
		dagToFrameRecur(root);
		
		List<TreeNode> anotherDag = new ArrayList<>();
		for (TreeNode node : dag.getAllNodes()) {
			if (nodes_from_main.contains(node)) {
				
			} else {
				anotherDag.add(node);
			}
		}
		
		for (TreeNode node : anotherDag) {
			dagToFrameRecur(node);
		}
	}
	
	public void dagToFrameRecur (TreeNode root) {
		nodes_from_main.add(root);
		frame.addVerticesWithEdges(root.getValue(), root.getChildren());
		for (TreeNode child : root.getChildren()) {
			dagToFrameRecur(child);
		}
	}
	
	public static void main (String [] args) {
		RecordReader reader = new RecordReader("firmware/exp6989/ekhoshim/record.txt");
		try {
			reader.readRecordCSV();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println(reader.dag.toString());

		reader.dagToFrame();
		
		//frame.addVerticesWithEdges("main", reader.funcMap.get("main"));
	}
}
