
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
 
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
 
import java.net.URL;
import java.io.IOException;
import java.awt.Dimension;
import java.awt.GridLayout;
 
public class TreeDemo extends JPanel
                      implements TreeSelectionListener {
	
	private Tree decisionTree;
    private JEditorPane htmlPane;
    private JTree tree;
    private static boolean DEBUG = false;
 
    //Optionally play with line styles.  Possible values are
    //"Angled" (the default), "Horizontal", and "None".
    private static boolean playWithLineStyle = false;
    private static String lineStyle = "Horizontal";
     
    //Optionally set the look and feel.
    private static boolean useSystemLookAndFeel = false;
 
    public TreeDemo(Tree dTree) {
        super(new GridLayout(1,0));
        
        decisionTree = dTree;
 
        //Create the nodes.
        DefaultMutableTreeNode top =
            new DefaultMutableTreeNode("Root");
        createNodes(top);
 
        //Create a tree that allows one selection at a time.
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
 
        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);
 
        if (playWithLineStyle) {
            System.out.println("line style = " + lineStyle);
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        }
 
        //Create the scroll pane and add the tree to it. 
        JScrollPane treeView = new JScrollPane(tree);
 
        //Create the HTML viewing pane.
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
       
        JScrollPane htmlView = new JScrollPane(htmlPane);
 
        //Add the scroll panes to a split pane.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(htmlView);
 
        Dimension minimumSize = new Dimension(100, 50);
        htmlView.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);
        splitPane.setDividerLocation(100); 
        splitPane.setPreferredSize(new Dimension(500, 300));
 
        //Add the split pane to this panel.
        add(splitPane);
        
        show();
    }
 
    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
    	System.out.println("Clicked!");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                           tree.getLastSelectedPathComponent();
 
        if (node == null) return;
 
        Object nodeInfo = node.getUserObject();
        NodeInfo node1 = (NodeInfo) nodeInfo;
        display(node1.nodeDes);
    }
 
    private class NodeInfo {
        public String nodeName;
        public String nodeDes;
 
        public NodeInfo(String name, String text) {
            nodeName = name;
            nodeDes = text;
        }
 
        public String toString() {
            return nodeName;
        }
    }
 
 
    private void display(String text) {
        htmlPane.setText(text);
    }
    
    private String makeText(Node node) {
    	String str = "";
    	str += "Infomation Gain: " + node.information_gain + "\n";
    	str += "Pivot: " + node.pivot + "\n";
    	return str;
    }
    
    private void addNode(DefaultMutableTreeNode parent, Node child) {
    	DefaultMutableTreeNode cat;
    	if(child.Attr_name.equals("")) {
    		cat = new DefaultMutableTreeNode(new NodeInfo(new Integer(child.class_number).toString(), makeText(child)));
            parent.add(cat);
    	} else {
    		cat = new DefaultMutableTreeNode(new NodeInfo(child.Attr_name, makeText(child)));
            parent.add(cat);
    	}
  
        if(child.left != null) {
    		addNode(cat, child.left);
    	}
    	if(child.right != null) {
    		addNode(cat, child.right);
    	}
    }
 
    private void createNodes(DefaultMutableTreeNode top) {
    	
        Node root = decisionTree.getRoot();
        addNode(top, root);
       
    }
         
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private void createAndShowGUI() {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }
 
        //Create and set up the window.
        JFrame frame = new JFrame("Decision Tree");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Add content to the window.
        frame.add(this);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
 
    
    public void show() {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
                createAndShowGUI();
            }
        });
    }
}
