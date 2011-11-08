package snappy.ui;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PApplet;
import processing.core.PFont;
import snappy.graph.TopoTree;
import snappy.graph.TopoTreeNode;

public class TopoTreeControl extends PApplet implements ComponentListener,
		TopoTreeHilightListener {

	public TopoTree m_tt = null;
	int levels = 0;
	int level_size = 0;
	int ignore_component_size = 4;

	boolean select_rect_down = false;
	int select_rect_x = -1;
	int select_rect_y = -1;
	int select_rect_w = -1;
	int select_rect_h = -1;

	static final int DEF_NODE_SIZE = 3;
	static final int SEL_NODE_SIZE = DEF_NODE_SIZE * 2;
	static final int HI_NODE_SIZE = SEL_NODE_SIZE * 2;
	static final int BORDER_SIZE = 10;
	static final int POWERS_OF_TWO = 6;

	static final int HIST_WIDTH = 100;

	static final int PRUNER_HEIGHT = 25;

	static final int PREFERRED_WIDTH = 800;
	static final int PREFERRED_HEIGHT = 1000;

	PFont prune_font = null;

	int sel_prune = 3;

	public Object cutoff_changed;
	public Object compon_changed;
	public Object hilight_changed;
	public Object select_changed;

	ArrayList<TopoTreeNode> hilightedNodes = null;

	public int[] bins = null;

	ArrayList<ChangeListener> changeListeners = null;
	ArrayList<TopoTreeSelectionListener> ttselListeners = null;
	ArrayList<TopoTreeHilightListener> tthilListeners = null;

	public TopoTreeControl(TopoTree tt, int[] bins) {

		super();

		m_tt = tt;

		ignore_component_size = (int) Math.pow(2, sel_prune);
		this.bins = bins;
		levels = bins.length;
		hilightedNodes = new ArrayList<TopoTreeNode>();
		changeListeners = new ArrayList<ChangeListener>();
		ttselListeners = new ArrayList<TopoTreeSelectionListener>();
		tthilListeners = new ArrayList<TopoTreeHilightListener>();

		cutoff_changed = new Object();
		compon_changed = new Object();
		hilight_changed = new Object();
		select_changed = new Object();
	}

	public void addTopoTreeSelectionListener(TopoTreeSelectionListener ttsl) {

		this.ttselListeners.add(ttsl);
	}

	public void addTopoTreeHilightListener(TopoTreeHilightListener tthl) {

		this.tthilListeners.add(tthl);
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

			if (ttn.num_points > ignore_component_size)
				topsize += ttn.num_points;
		}
		int left = BORDER_SIZE;
		int right = left;
		for (TopoTreeNode ttn : m_tt.roots) {

			if (ttn.num_points > ignore_component_size) {

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
			rect(getWidth() - HIST_WIDTH + 1, y, HIST_WIDTH
					* (bf_val - min_binlen) / binrange, level_size);
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
					prune_bin_width, getHeight());
			fill(255);
			String str = "" + ((int) Math.pow(2, i));
			text(str, i * prune_bin_width
					+ (prune_bin_width / 2 - textWidth(str)), getHeight()
					- PRUNER_HEIGHT
					+ (PRUNER_HEIGHT / 2 + (textAscent() + textDescent()) / 2));
		}
		strokeWeight(1.f);

		// draw selection rect

		if (select_rect_down) {

			noFill();
			stroke(128);
			rect(select_rect_x, select_rect_y, select_rect_w, select_rect_h);
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

				if (ttn.num_points > ignore_component_size)
					topsize += ttn.num_points;
			}
			int newleft = left;
			int newright = left;
			for (TopoTreeNode ttn : node.children) {

				if (ttn.num_points > ignore_component_size) {

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
		if (drawNode || node.hilighted) {

			// if (level != sel_level) {
			stroke(0);
			fill(0);
			// }
			// else {
			// stroke(0);
			// if (node.hilighted)
			// fill(199, 197, 43);
			// else
			// fill(50, 171, 90);
			// }
			if (node.selected) {

				fill(199, 197, 43);
			}
			if (node.hilighted) {

				fill(50, 171, 90);
			}

			int node_size = DEF_NODE_SIZE;
			if (node.selected)
				node_size = SEL_NODE_SIZE;
			if (node.hilighted)
				node_size = HI_NODE_SIZE;
			ellipse((left + right) / 2, level_size * (level + 1), node_size,
					node_size);
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

				for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
					for (TopoTreeNode node : nodes) {

						node.selected = false;
					}
				}

				for (ChangeListener cl : changeListeners) {

					cl.stateChanged(new ChangeEvent(select_changed));
				}

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

					if (node.num_points > ignore_component_size) {

						if (node.x > i_left && node.y > i_top
								&& node.x < i_right && node.y < i_bottom) {

							if (!node.selected)
								isDirty = true;
							node.selected = true;
						} else {

							if (node.selected)
								isDirty = true;
							node.selected = false;
						}
					}
				}
			}

			// notify listeners
			if (isDirty) {
				for (ChangeListener cl : changeListeners) {

					cl.stateChanged(new ChangeEvent(select_changed));
				}
			}

			redraw();
		}
	}

	public void mouseMoved() {

		if (mouseY < getHeight() - PRUNER_HEIGHT && mouseY >= 0 && mouseX >= 0
				&& mouseX < getWidth()) {

			int dist = Integer.MAX_VALUE;

			for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
				for (TopoTreeNode node : nodes) {

					if (node.num_points > ignore_component_size
							&& node.selected) {

						int dx = mouseX - node.x;
						int dy = mouseY - node.y;
						dist = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
						if (dist <= HI_NODE_SIZE) {

							node.hilighted = true;
							this.hilightedNodes.add(node);
							for (TopoTreeHilightListener tthl : tthilListeners) {

								tthl.hilightChanged(node);
							}
						} else {
							if (node.hilighted)
								hilightedNodes.remove(node);
							node.hilighted = false;
						}
					} else {

						if (node.hilighted)
							hilightedNodes.remove(node);
						node.hilighted = false;
					}
				}
			}

			redraw();
		}
	}

	public void mouseClicked() {

		if (mouseY < getHeight() - PRUNER_HEIGHT) {

			// sel_level =
			// Math.max(0,Math.min(bins.length-1,(mouseY-level_size/2) /
			// level_size));
			//
			// // tell everyone about it
			// for( ChangeListener cl : changeListeners ) {
			//
			// cl.stateChanged( new ChangeEvent(cutoff_changed) );
			// }

			redraw();
		} else {

			sel_prune = Math
					.min(POWERS_OF_TWO,
							(int) (mouseX / ((getWidth() - HIST_WIDTH) / POWERS_OF_TWO)));
			this.setIgnoreComponenetSize((int) Math.pow(2, sel_prune));
			// tell everyone about it
			for (ChangeListener cl : changeListeners) {

				cl.stateChanged(new ChangeEvent(compon_changed));
			}
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

		// TODO Auto-generated method stub

	}

	public Dimension getPreferredSize() {

		return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
	}

	@Override
	public void hilightChanged(TopoTreeNode node) {

		for( TopoTreeNode tnode: hilightedNodes ) {
			
			tnode.hilighted = false;
		}
		hilightedNodes.clear();
		
		if (!node.hilighted)
			hilightedNodes.add(node);

		node.hilighted = true;

		redraw();
	}

}
