package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import snappy.graph.NodeLabeller;
import snappy.graph.SizedLabel;
import snappy.graph.TagChangeListener;
import snappy.graph.TopoTreeNode;

/*
 * Control for displaying and listening to selections in the topological tree of the graph
 * will display tree nodes as parent nodes with summary strings and tree leaves as leaf nodes 
 * with group intersection strings.
 * 
 * The icons of the nodes reflect tag membership
 */
public class NodeTree extends JPanel implements TopoTreeSelectionListener,
		TreeSelectionListener, TagChangeListener {

	public JTree tree = null;
	NodeLabeller node_labeller = null;
	ArrayList<TopoTreeSelectionListener> ttselListeners = null;
	ArrayList<ChangeListener> changeListeners = null;


	/**
	 * 
	 */
	private static final long serialVersionUID = -4957324767502224293L;

	public NodeTree(NodeLabeller node_labeller) {

		super(new BorderLayout());

		this.setBorder( BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, PrettyColors.Grey), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.white);
		
		JLabel title_label = new JLabel("Component List");
		title_label.setForeground(PrettyColors.DarkGrey);
		title_label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		this.add(title_label,"North");
		
		// construct an empty tree

		this.node_labeller = node_labeller;
		tree = new JTree();
		tree.addTreeSelectionListener(this);
		tree.setCellRenderer(new TopoNodeCellRenderer());
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setRootVisible(false);

//		ScrollPane scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
//		scrollPane.add(tree);
//		scrollPane.setPreferredSize(new Dimension(500, 100));
		 JScrollPane scrollPane = new JScrollPane(tree);
		 scrollPane.setMinimumSize(new Dimension(500,100));
		this.add(scrollPane, BorderLayout.CENTER);

		ttselListeners = new ArrayList<TopoTreeSelectionListener>();
		changeListeners = new ArrayList<ChangeListener>();

		MouseListener ml = new MouseAdapter() {
		     public void mousePressed(MouseEvent e) {
		         int selRow = tree.getRowForLocation(e.getX(), e.getY());
		         TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
		         if(selRow != -1) {
		             if(e.getClickCount() == 1 & e.getButton() == MouseEvent.BUTTON3 ) {

		            	DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
		            	if( node.getUserObject() instanceof TopoTreeNode ) {
			 				for (ChangeListener cl : changeListeners) {
	
								cl.stateChanged(new ChangeEvent(node.getUserObject()));
							}
		            	}
		             }
		         }
		     }
		 };
		 tree.addMouseListener(ml);	
		 tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
	}

	public void addChangeListener(ChangeListener cl) {

		this.changeListeners.add(cl);
	}

	public void addTopoTreeSelectionListener(TopoTreeSelectionListener ttsl) {

		this.ttselListeners.add(ttsl);
	}

	public class TopoNodeCellIcon implements Icon {

		ArrayList<Color> m_colors = null;
		
		public TopoNodeCellIcon ( ArrayList<Color> colors ) {
			
			if( colors == null ) {

				m_colors = null;
			}
			else {
				m_colors = colors;
			}			
		}
		
		@Override
		public int getIconHeight() {

			return 16;
		}

		@Override
		public int getIconWidth() {

			return 16*m_colors.size();
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			
			for( int i = 0; i < m_colors.size(); i++ ) {
				
				g.setColor(m_colors.get(i));
				g.fillRect(x+i*16,y, 16, 16);
			}
		}		
	}
	
	public class TopoNodeCellRenderer extends DefaultTreeCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1808991006152654882L;

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

			if (node.getUserObject() instanceof Integer) {

				this.setText(node_labeller.getLabel(((Integer) node
						.getUserObject()).intValue()));
				
			} else if (node.getUserObject() instanceof TopoTreeNode) {

				TopoTreeNode topo_node = (TopoTreeNode) node.getUserObject();
				String summaryLabel = " " + topo_node.num_points + ": ";
				for (SizedLabel sl : node_labeller.getSummaryLabel(topo_node)) {

					summaryLabel += sl.label + " ";
				}

				this.setText(summaryLabel);
				
				// TODO set the icon
				if( topo_node.tags.keySet().isEmpty() ) {
					
					if( expanded )
						this.setIcon(openIcon);
					else 
						this.setIcon(closedIcon);
				}
				else {
					
					this.setIcon(new TopoNodeCellIcon(new ArrayList<Color>(topo_node.tag_colors.values())) );
				}
			}
			

			return this;
		}

	}

		@Override
	public void selectionChanged(ArrayList<TopoTreeNode> nodes, 
			TopoTreeNode hilighted, 
			boolean selChanged, 
			boolean hiChanged) {

		if( selChanged ) {
			// build a model
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	
			boolean node_in_selection = false;
			DefaultMutableTreeNode hi_node = null;
			
			if (nodes != null) {
	
				for (TopoTreeNode node : nodes) {
	
					DefaultMutableTreeNode parent = new DefaultMutableTreeNode(node);
					for (int i = 0; i < node.num_points; i++) {
	
						parent.add(new DefaultMutableTreeNode(node.component.get(i)));
					}
					root.add(parent);	
					if( node == hilighted ) {
						node_in_selection = true;
						hi_node = parent;
					}
				}
			}
	
			if( ! node_in_selection && hilighted != null ) {
				
				hi_node = new DefaultMutableTreeNode(hilighted);
				for (int i = 0; i < hilighted.num_points; i++) {
	
					hi_node.add(new DefaultMutableTreeNode(hilighted.component.get(i)));
				}
				root.add(hi_node);	
			}
			
			TreeModel my_model = new DefaultTreeModel(root);
	
			tree.setModel(my_model);
			if( hilighted != null ) {
				
				TreePath path = new TreePath(hi_node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
			this.getParent().validate();
		} else if (hiChanged) {
			
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel()
					.getRoot();
			for (int i = 0; i < root.getChildCount(); i++) {

				DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) root
						.getChildAt(i);
				if (cnode.getUserObject() == hilighted) {

					TreePath path = new TreePath(cnode.getPath());
					tree.setSelectionPath(path);
					tree.scrollPathToVisible(path);
				}
			}
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent arg0) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		if( node != null ) {
			if( node.getUserObject() instanceof TopoTreeNode ) {
				TopoTreeNode tnode = (TopoTreeNode) node.getUserObject();
				for (TopoTreeSelectionListener ttsl : ttselListeners) {
	
					ttsl.selectionChanged(null, tnode, false, true);
				}
			}
			if( node.getUserObject() instanceof Integer ) {
				
				
			}
		}
	}

	@Override
	public void tagsChanged() {

		tree.repaint();		
	}
}
