package snappy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PApplet;
import processing.core.PFont;
import snappy.graph.TagChangeListener;
import snappy.graph.TagTable;
import snappy.graph.TopoTree;
import snappy.graph.TopoTreeNode;

public class TopoTreeControl extends PApplet implements ComponentListener,
		TopoTreeSelectionListener, ChangeListener, TagChangeListener {

	boolean shift_key_down = false;
	boolean option_key_down = false;
	
	public TopoTree m_tt = null;
	int levels = 0;
	int level_size = 0;
	int ignore_component_size = 4;

	boolean select_rect_down = false;
	int select_rect_x = -1;
	int select_rect_y = -1;
	int select_rect_w = -1;
	int select_rect_h = -1;

	static final int DEF_NODE_SIZE = 1;
	static final int SEL_NODE_SIZE = DEF_NODE_SIZE * 2;
	static final int HI_NODE_SIZE = SEL_NODE_SIZE;
	static final int BORDER_SIZE = 10;
	static final int POWERS_OF_TWO = 7;

	static final int HIST_WIDTH = 50;

	static final int PRUNER_HEIGHT = 30;

	static final int PREFERRED_WIDTH = 800;
	static final int PREFERRED_HEIGHT = 1000;

	PFont prune_font = null;

	int sel_prune = 3;

	public Object cutoff_changed;
	public Object compon_changed;
	public Object hilight_changed;
	public Object select_changed;

	public TopoTreeNode hilightedNode = null;
	public ArrayList<TopoTreeNode> selectedNodes = null;

	public int[] bins = null;

	ArrayList<ChangeListener> changeListeners = null;
	ArrayList<TopoTreeSelectionListener> ttselListeners = null;
	
	TagTable m_ttable = null;
	
	public ArrayList<TopoTreeNode> getSelectionSuperset() {
		
		ArrayList<TopoTreeNode> taggedNodes = null;
		if( selectedNodes.isEmpty() ) {
			taggedNodes = new ArrayList<TopoTreeNode>();
		}
		else {
			taggedNodes = new ArrayList<TopoTreeNode>(selectedNodes);
		}
		if(hilightedNode != null && !selectedNodes.contains(hilightedNode)) {
			taggedNodes.add(hilightedNode);
		}
		
		return taggedNodes;
	}
	public void setTagTable( TagTable ttable ) {
		
		m_ttable = ttable;
	}

	public boolean updateHilight( TopoTreeNode node ) {
		
		if( node != null ) {
			if( node.hilighted ) {
				
				return false;
			}
			if( hilightedNode != null ) {
				
				hilightedNode.hilighted = false;
			}
			hilightedNode = node;
			hilightedNode.hilighted = true;
		}
		else {
			
			if( hilightedNode == null ) {
				
				return false;
			}
			hilightedNode.hilighted = false;
			hilightedNode = null;
		}
		
		return true;
	}
	
	public boolean removeFromSelectSet( TopoTreeNode node ) {
		
		if( !node.selected ) {
			
			return false;
		}
		
		node.selected = false;
		this.selectedNodes.remove(node);
		if( node.hilighted ) {
			
			updateHilight(null);
		}
		
		
		return true;
	}
	
	/*
	 * returns if it is dirty or not
	 */
	public boolean addToSelectSet( TopoTreeNode node ) {
		
		if( node.selected ) {
			
			return false;
		}
		
		node.selected = true;
		this.selectedNodes.add(node);
		
		return true;
	}
	
	public TopoTreeControl(TopoTree tt, int[] bins) {

		super();

		m_tt = tt;

		ignore_component_size = (int) Math.pow(2, sel_prune);
		this.bins = bins;
		levels = bins.length;
		selectedNodes = new ArrayList<TopoTreeNode>();
		changeListeners = new ArrayList<ChangeListener>();
		ttselListeners = new ArrayList<TopoTreeSelectionListener>();

		cutoff_changed = new Object();
		compon_changed = new Object();
		hilight_changed = new Object();
		select_changed = new Object();
	}

	public void addTopoTreeSelectionListener(TopoTreeSelectionListener ttsl) {

		this.ttselListeners.add(ttsl);
	}

	public void addChangeListener(ChangeListener cl) {

		this.changeListeners.add(cl);
	}

	public void setTopoTree(TopoTree tt) {

		m_tt = tt;

		levels = -1;
		for (TopoTreeNode ttn : m_tt.roots) {

			levels = Math.max(levels, count_depth(ttn));
		}
		redraw();
	}

	public int getIgnoreComponentSize() {

		return ignore_component_size;
	}

	public void setIgnoreComponenetSize(int x) {

		ignore_component_size = x;
		redraw();
	}

	// simple recursive routine to count the depth of a tree
	public int count_depth(TopoTreeNode node) {

		if (node.children.size() > 0) {

			int depth = -1;
			for (TopoTreeNode ttn : node.children) {

				depth = Math.max(depth, count_depth(ttn));
			}

			return 1 + depth;
		}

		return 0;
	}

	public void setup() {

		smooth();

		prune_font = createFont("SansSerif", 12);
		textFont(prune_font);
		noLoop();
		this.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
	}

	public void draw() {

		background(255);

		// break up the scene into levels (at which the tree will slice things
		// up)

		level_size = (int) Math.round(((float) getHeight() - PRUNER_HEIGHT)
				/ (levels + 1.f));

		// draw the levels

		stroke(128 + 64 + 32);

		for (int i = 0; i < levels; i++) {

			int y = level_size + i * level_size;
			line(0, y, getWidth() - HIST_WIDTH, y);
		}

		// slice up the top level

		int topsize = 0;
		for (TopoTreeNode ttn : m_tt.roots) {

			if (ttn.num_points >= ignore_component_size)
				topsize += ttn.num_points;
		}
		int left = BORDER_SIZE;
		int right = left;
		for (TopoTreeNode ttn : m_tt.roots) {

			if (ttn.num_points >= ignore_component_size) {

				right += (int) Math
						.round(((getWidth() - HIST_WIDTH) - 2 * BORDER_SIZE)
								* ((float) ttn.num_points) / ((float) topsize));
				drawNode(ttn, left, right, 0, -1, -1, true);
				left = right;
			}
		}

		// draw the histogram on the side

		fill(255);
		stroke(255);
		rect(getWidth() - HIST_WIDTH, 0, HIST_WIDTH, getHeight()
				- PRUNER_HEIGHT);

		// do some normalization

		float max_binlen = Float.MIN_VALUE;
		float min_binlen = Float.MAX_VALUE;
		for (int i = 0; i < bins.length; i++) {

			if (bins[i] > 0) {

				max_binlen = Math.max(max_binlen, (float) Math.log10(bins[i]));
				min_binlen = Math.min(min_binlen, (float) Math.log10(bins[i]));
			} else {
				max_binlen = Math.max(max_binlen, -2.f);
				min_binlen = Math.min(min_binlen, -2.f);
			}
		}
		max_binlen += 1.0;

		stroke(255);
		fill(0x25, 0x8B, 0xC1);

		float binrange = max_binlen - min_binlen;

		for (int i = 0; i < bins.length; i++) {

			int y = level_size / 2 + i * level_size;
			float bf_val = 0.f;
			if (bins[(bins.length - 1) - i] == 0) {

				bf_val = -2.f;
			} else {
				bf_val = (float) Math.log10(bins[(bins.length - 1) - i]);
			}
			if( i == 0 ) {
				rect(getWidth() - HIST_WIDTH + 1, y, HIST_WIDTH
						* (max_binlen - min_binlen) / binrange, level_size);
			}
			else if ( i < bins.length-1 ) {
				rect(getWidth() - HIST_WIDTH + 1, y, HIST_WIDTH
						* (bf_val - min_binlen) / binrange, level_size);				
			}
		}

		// draw the pruner control

		fill(157, 106, 94);
		stroke(255);
		strokeWeight(5.f);

		int prune_bin_width = (getWidth() - HIST_WIDTH) / POWERS_OF_TWO;
		for (int i = 0; i < POWERS_OF_TWO; i++) {

			if (i > sel_prune) {
				fill(157, 106, 94);
				stroke(255);
			} else if (i < sel_prune) {
				fill(145, 144, 144);
				stroke(255);
			} else if (i == sel_prune) {
				fill(145, 144, 144);
				stroke(5, 199, 215);
			}

			rect(i * prune_bin_width, getHeight() - PRUNER_HEIGHT,
					prune_bin_width-5, getHeight()-5);
			
			fill(255);
			String str = "" + ((int) Math.pow(2, i));
			text(str, i * prune_bin_width
					+ (prune_bin_width / 2 - textWidth(str)), getHeight()
					- PRUNER_HEIGHT
					+ (PRUNER_HEIGHT / 2 + (textAscent() + textDescent()) / 2));

			fill(0);
			stroke(0);
			strokeWeight(1.f);
			int rad = SEL_NODE_SIZE + (int)Math.round(SEL_NODE_SIZE * Math.log(((int) Math.pow(2, i))));
			ellipse(i * prune_bin_width
					+ (prune_bin_width / 2 - textWidth(str)) - rad - 2, getHeight()
					- PRUNER_HEIGHT
					+ (PRUNER_HEIGHT / 2 + (textAscent() + textDescent()) / 2) - rad,rad,rad);
			strokeWeight(5.f);
		}
		strokeWeight(1.f);

		// draw selection rect

		if (select_rect_down) {

			noFill();
			stroke(128);
			rect(select_rect_x, select_rect_y, select_rect_w, select_rect_h);
		}
		
		if( this.hasFocus() ) {
			
			noFill();
			stroke(128);
			strokeWeight(3.f);
			rect(3/2,3/2,getWidth()-3,getHeight()-3);
			strokeWeight(1.f);
		}
	}

	// recursive draw routine

	public void drawNode(TopoTreeNode node, int left, int right, int level,
			int px, int py, boolean drawNode) {

		// draw the edge to its parent
		if (px != -1) {

			stroke(64);
			line((left + right) / 2, level_size * (level + 1), px, py);
		}

		// break out if there's no room to recurse

		if (left != right) {

			// slice up the remaining space

			int topsize = 0;
			for (TopoTreeNode ttn : node.children) {

				if (ttn.num_points >= ignore_component_size)
					topsize += ttn.num_points;
			}
			int newleft = left;
			int newright = left;
			for (TopoTreeNode ttn : node.children) {

				if (ttn.num_points >= ignore_component_size) {

					newright += (int) Math.round((right - left)
							* ((float) ttn.num_points) / ((float) topsize));
					drawNode(ttn, newleft, newright, level + 1,
							(left + right) / 2, level_size * (level + 1), true /*
																				 * ttn
																				 * .
																				 * num_points
																				 * !=
																				 * node
																				 * .
																				 * num_points
																				 */);
					newleft = newright;
				}
			}
		}

		node.x = (left + right) / 2;
		node.y = level_size * (level + 1);

		// draw the node
		if ((drawNode || node.hilighted) && !node.isSameAsChild) {

			// if (level != sel_level) {
			
			// chose color based on tags
			if( node.tags.keySet().size() > 0 ) {
				int selected_tag = 0;
				int max_tag_count = -1;
				for( Integer tag : node.tags.keySet() ) {

					if( max_tag_count < node.tags.get(tag) ) {
						
						max_tag_count = node.tags.get(tag);
						selected_tag = tag;
					}
					if( max_tag_count == node.tags.get(tag) ) {
						
						selected_tag = m_ttable.getTopTag(tag, selected_tag);
					}
				}
				
				Color tagColor = node.tag_colors.get(selected_tag);
				fill(tagColor.getRed(),tagColor.getGreen(), tagColor.getBlue());
			}
			else {
				fill(PrettyColors.Grey.getRed(), PrettyColors.Grey.getGreen(), PrettyColors.Grey.getBlue());
			}
			stroke(0);

			if (node.selected) {

				stroke(Color.black.getRed(), Color.black.getGreen(), Color.black.getBlue());
				strokeWeight(5.f);
			}
			if (node.hilighted) {

				stroke(PrettyColors.Red.getRed(), PrettyColors.Red.getGreen(), PrettyColors.Red.getBlue());
				strokeWeight(5.f);
			}
			if( !node.selected && !node.hilighted )
				noStroke();

			int node_size = DEF_NODE_SIZE + (int)Math.round(DEF_NODE_SIZE * Math.log(node.num_points));
			if (node.selected)
				node_size = Math.max(6, SEL_NODE_SIZE + (int)Math.round(SEL_NODE_SIZE * Math.log(node.num_points)))-5;
			if (node.hilighted) {
				node_size = Math.max(6, HI_NODE_SIZE + (int)Math.round(HI_NODE_SIZE * Math.log(node.num_points)))-5;
//				System.out.println("HINODESIZE = "+node_size);
			}
			ellipse((left + right) / 2, level_size * (level + 1), node_size,
					node_size);
			stroke(0);
			strokeWeight(1.f);
		}
	}

	public void keyReleased() {
		
		if (key == CODED) {
			if( keyCode == ALT ) {
				
				option_key_down = false;
			}
			if( keyCode == SHIFT ) {
				
				shift_key_down = false;
			}
		}		
	}
	public void keyPressed() {
		
		if (key == CODED) {
			if( keyCode == ALT ) {
				
				option_key_down = true;
			}
			if( keyCode == SHIFT ) {
				
				shift_key_down = true;
			}
			if( keyCode == UP ) {
				if( hilightedNode != null && hilightedNode.diffParent != null && 
						hilightedNode.diffParent.num_points >= ignore_component_size  && !hilightedNode.isSameAsChild) {

					updateHilight(hilightedNode.diffParent);
					
					for (TopoTreeSelectionListener ttsl : ttselListeners) {
						
						ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
					}
					redraw();
				}				
			}
			if( keyCode == DOWN ) {
				if( hilightedNode != null && 
						!hilightedNode.diffChildren.isEmpty() ) {
					
					
					TopoTreeNode node = null;
					
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffChildren )
						//node = (node == null )?((child.selected&&child.num_points >= ignore_component_size)?child:null):node;
						node = (node == null )?((child.num_points >= ignore_component_size && !child.isSameAsChild)?child:null):node;

					if( node != null ) {
						
						updateHilight( node );
						
						for (TopoTreeSelectionListener ttsl : ttselListeners) {
							
							ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
						}
					}
					redraw();
				}
			}
			if( keyCode == LEFT ) {
				
				//System.out.println("LEFT");
				if( hilightedNode != null && hilightedNode.diffParent != null ) {
					
					
					TopoTreeNode node = null;
					
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffParent.diffChildren ) {
						
						if( child == hilightedNode ) {
							break;
						}
						else if( /*child.selected && */child.num_points >= ignore_component_size && !child.isSameAsChild) {
							node = child;
						}
					}

					if( node != null ) {
						
						updateHilight( node );
						
						for (TopoTreeSelectionListener ttsl : ttselListeners) {
							
							ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
						}
					}
					redraw();
				}
			}
			if( keyCode == RIGHT) {
				//System.out.println("RIGHT");
				if( hilightedNode != null && hilightedNode.diffParent != null ) {
					
					
					TopoTreeNode node = null;
					boolean haveFoundSelf = false;
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffParent.diffChildren ) {
						
						if( child == hilightedNode ) {
							haveFoundSelf = true;
						}
						else if( /*child.selected && */haveFoundSelf && child.num_points >= ignore_component_size && !child.isSameAsChild) {
							node = child;
							break;
						}
					}

					if( node != null ) {
						
						updateHilight( node );
						
						for (TopoTreeSelectionListener ttsl : ttselListeners) {
							
							ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
						}
					}
					redraw();
				}
			}
		}
		else {
			if( key == '.') {
				if( hilightedNode != null && addDescendents(hilightedNode) ) {
					
					for (TopoTreeSelectionListener ttsl : ttselListeners) {
						
						ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
					}
				}
				redraw();
			}
			if( key == ',') {
				
				if( hilightedNode != null && removeDescendents(hilightedNode) ) {
					
					for (TopoTreeSelectionListener ttsl : ttselListeners) {
						
						ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
					}
				}
				redraw();
			}
			if( key =='[') {
				
				this.pruneTree(Math.max(0,sel_prune-1));
			}
			if( key ==']') {
				
				this.pruneTree(Math.min(POWERS_OF_TWO-1,sel_prune+1));
			}
		}
	}
	
	public void mouseReleased() {

		if (select_rect_down) {

			select_rect_down = false;
			redraw();
		}
	}

	public void mousePressed() {

		if (mouseY < getHeight() - PRUNER_HEIGHT) {

			if (!select_rect_down) {

//				for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
//					for (TopoTreeNode node : nodes) {
//
//						node.selected = false;
//					}
//				}
//
//				for (ChangeListener cl : changeListeners) {
//
//					cl.stateChanged(new ChangeEvent(select_changed));
//				}

				select_rect_down = true;
				select_rect_x = mouseX;
				select_rect_y = mouseY;
				select_rect_w = 0;
				select_rect_h = 0;
				redraw();
			}
		}
	}

	public void mouseDragged() {

		if (select_rect_down) {

			// update selection rect dimensions

			select_rect_w = -(select_rect_x - mouseX);
			select_rect_h = -(select_rect_y - mouseY);

			if( Math.max(Math.abs(select_rect_w),Math.abs(select_rect_h)) < 2 ) {
				
				return;
			}
			
			// update the selection details

			int i_top = Math.min(select_rect_y, select_rect_y + select_rect_h);
			int i_left = Math.min(select_rect_x, select_rect_x + select_rect_w);
			int i_bottom = Math.max(select_rect_y, select_rect_y
					+ select_rect_h);
			int i_right = Math
					.max(select_rect_x, select_rect_x + select_rect_w);

			boolean isDirty = false;

			for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
				for (TopoTreeNode node : nodes) {

					if (node.num_points >= ignore_component_size && !node.isSameAsChild) {

						if (node.x > i_left && node.y > i_top
								&& node.x < i_right && node.y < i_bottom) {

							if( option_key_down && shift_key_down ) {

								isDirty = removeFromSelectSet(node) || isDirty;
							}
							else {
								
								isDirty = addToSelectSet(node) || isDirty;
							}
						} else if ( !option_key_down ) {

							isDirty = removeFromSelectSet(node) || isDirty;
						}
					}
				}
			}
			
			if( hilightedNode == null && !selectedNodes.isEmpty()) {
				
				isDirty = updateHilight(selectedNodes.get(0)) || isDirty;
			}

			// notify listeners
			if (isDirty) {
				for (TopoTreeSelectionListener ttsl : ttselListeners) {
					
					ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
				}
			}

			redraw();
		}
	}

	public void mouseMoved() {
	}

	public void mouseClicked() {

		boolean isDirty = false;
		
		if (mouseY < getHeight() - PRUNER_HEIGHT && mouseY >= 0 && mouseX >= 0
				&& mouseX < getWidth()) {

			int mindist = Integer.MAX_VALUE;
			TopoTreeNode minnode = null;

			for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
				for (TopoTreeNode node : nodes) {

					if( option_key_down ) {
						
						if (node.num_points >= ignore_component_size  && !node.isSameAsChild) {
	
							int dx = mouseX - node.x;
							int dy = mouseY - node.y;
							int dist = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
							if (dist <= HI_NODE_SIZE + (int)Math.round(HI_NODE_SIZE * Math.log(node.num_points)) && dist < mindist) {
	
								mindist = dist;
								minnode = node;
							}
						}
					}
					else {
						if (node.num_points >= ignore_component_size  && !node.isSameAsChild
								/*&& node.selected*/) {
	
							int dx = mouseX - node.x;
							int dy = mouseY - node.y;
							int dist = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
							if (dist <= HI_NODE_SIZE + (int)Math.round(HI_NODE_SIZE * Math.log(node.num_points)) && dist < mindist) {
	
								mindist = dist;
								isDirty = updateHilight(node) || isDirty;
							} else {
								if (node.hilighted) {
									isDirty = updateHilight(null) || isDirty;
								}
							}
						} 
					}
				}
			}

			if( option_key_down && minnode != null ) {
				
				if( minnode.selected ) {
					isDirty = removeFromSelectSet(minnode) || isDirty;
				}
				else {
					isDirty = addToSelectSet(minnode) || isDirty;
				}
				if( minnode.hilighted && !minnode.selected) {
					isDirty = updateHilight(null) || isDirty;
				}
			}
			
			if( hilightedNode == null && !selectedNodes.isEmpty()) {
				
				isDirty = updateHilight(selectedNodes.get(0)) || isDirty;
			}

			if( isDirty ) {
				for (TopoTreeSelectionListener ttsl : ttselListeners) {
					
					ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
				}
			}
			
			redraw();
		} else {

			pruneTree(Math
					.min(POWERS_OF_TWO,
							(int) (mouseX / ((getWidth() - HIST_WIDTH) / POWERS_OF_TWO))));
			
		}
	}
	
	public void pruneTree( int prune_amount ) {
		
		sel_prune = prune_amount;
		this.setIgnoreComponenetSize((int) Math.pow(2, sel_prune));
		
		// remove from the selection set those things that have been removed from the tree
		
		boolean isDirty = false;
		ArrayList<TopoTreeNode> remNodes = new ArrayList<TopoTreeNode>();
		for( TopoTreeNode node : selectedNodes ) {
		
			if( node.num_points < ignore_component_size  && !node.isSameAsChild) {
				
				remNodes.add(node);
			}
		}
		for( TopoTreeNode node : remNodes ) {
			
			isDirty = removeFromSelectSet(node) || isDirty;				
		}
		
		if( isDirty ) {
			for (TopoTreeSelectionListener ttsl : ttselListeners) {
				
				ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
			}
		}
		redraw();
		
		// tell everyone about it
		for (ChangeListener cl : changeListeners) {

			cl.stateChanged(new ChangeEvent(compon_changed));
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2650830987573047590L;

	@Override
	public void componentHidden(ComponentEvent arg0) {

		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {

		// TODO Auto-generated method stub

	}

	@Override
	public void componentResized(ComponentEvent arg0) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				redraw();
			}
		});
	}

	@Override
	public void componentShown(ComponentEvent arg0) {

	}

	public Dimension getPreferredSize() {

		return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
	}
	
	public boolean removeDescendents( TopoTreeNode node ) {
		
		boolean isDirty = removeFromSelectSet(node);
		if( ! node.diffChildren.isEmpty() ) {
						
			for( TopoTreeNode child : node.diffChildren ) {

				if( child.num_points >= ignore_component_size)
					isDirty = removeDescendents( child ) || isDirty;
			}
		}
		return isDirty;
	}

	public boolean addDescendents( TopoTreeNode node ) {
		
//		System.out.println(""+node);
		boolean isDirty = addToSelectSet(node);
		if( ! node.diffChildren.isEmpty() ) {
						
			for( TopoTreeNode child : node.diffChildren ) {

//				System.out.println("child: " + child + " # = " + child.num_points);
				if( child.num_points >= ignore_component_size)
					isDirty = addDescendents( child ) || isDirty;
			}
		}
//		System.out.println("isDirty = " + isDirty);
		return isDirty;
	}
	
	@Override
	public void stateChanged(ChangeEvent arg0) {

		if( arg0.getSource() instanceof TopoTreeNode ) {
		
			if( addDescendents((TopoTreeNode) arg0.getSource()) ) {
				
				for (TopoTreeSelectionListener ttsl : ttselListeners) {
					
					ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
				}
			}
			redraw();
		}		
	}

	@Override
	public void selectionChanged(ArrayList<TopoTreeNode> nodes,
			TopoTreeNode hilighted, boolean selChanged, boolean hiChanged) {

		if( hiChanged ) {
			
			if( updateHilight(hilighted) ) {
				
				for (TopoTreeSelectionListener ttsl : ttselListeners) {
					
					ttsl.selectionChanged(selectedNodes, hilightedNode, true, true);
				}
			}
		}	
		
		redraw();				
	}

	@Override
	public void tagsChanged() {

		redraw();
	}

}
