package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.org.apache.xml.internal.security.keys.storage.implementations.SingleCertificateResolver;

import processing.core.PApplet;

import snappy.graph.GlimmerLayout;
import snappy.graph.TagChangeListener;
import snappy.graph.TagTable;
import snappy.graph.TopoTree;
import snappy.graph.TopoTreeNode;

public class GlimmerDrawer extends JPanel implements TagChangeListener,
		TopoTreeSelectionListener, ChangeListener, ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9002807048822851883L;
	GlimmerLayout m_glimmer_layout = null;
	TopoTree m_topo_tree = null;
	Thread m_layout_thread = null;
	GlimmerCanvas draw_panel = null;
	JPanel control_panel = null;
	TagTable m_tag_table = null;
	JCheckBox draw_edges_box = null;
	JCheckBox draw_labels_box = null;
	JSlider power_slider = null;
	JSlider point_size_slider = null;

	JButton start_stop_button = null;

	Color bkgndColor = Color.WHITE;
	Color defaultColor = PrettyColors.Grey;
	Color selectedColor = Color.BLACK;
	Color hilightColor = PrettyColors.Red;
	int boundary = 10;
	int point_radius = 7;
	boolean is_paused = false;
	int frame_rate = 2;
	JLabel squeeze_label = null;
	JLabel pointsize_label = null;
	JLabel title_label = null;

	public void doLayout() {

		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		int myWidth = (width - insets.left) - insets.right;
		int myHeight = (height - insets.top) - insets.bottom;
		
		int title_height = title_label.getPreferredSize().height;
		int control_panel_height = (int)control_panel.getPreferredSize().getHeight();
		int mds_height = myHeight - control_panel_height - title_height - 10;
		
		title_label.setBounds(insets.left, insets.top, myWidth, title_height);
		draw_panel.setBounds(insets.left, insets.top + title_height + 5, myWidth, myHeight - control_panel_height - title_height - 10 );
		control_panel.setBounds(insets.left, insets.top+mds_height+title_height+10, myWidth, control_panel_height);
	}

	public GlimmerDrawer(	GlimmerLayout glimmer_layout, 
							TopoTree topo_tree, 
							TagTable tag_table) {

		super();

		this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 1, PrettyColors.Grey), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.WHITE);
		
		m_glimmer_layout = glimmer_layout;
		m_topo_tree = topo_tree;
		m_tag_table = tag_table;
		
		// init components

		draw_panel = new GlimmerCanvas();
		control_panel = new JPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 7927535892217533170L;
			public void doLayout() {
			
				int width = getWidth();
				int height = getHeight();
				Insets insets = getInsets();
				int myWidth = (width - insets.left) - insets.right;
				int myHeight = (height - insets.top) - insets.bottom;
				
				start_stop_button.setBounds(insets.left, insets.top, myWidth/5 - 5, myHeight);
				
				int larger_label = (int) Math.max(  squeeze_label.getPreferredSize().getWidth(),
													pointsize_label.getPreferredSize().getWidth());	
				
				int larger_height = (int) Math.max(squeeze_label.getPreferredSize().getHeight(), 
						Math.max(pointsize_label.getPreferredSize().getHeight(), 
								Math.max(power_slider.getPreferredSize().getHeight(), 
										point_size_slider.getPreferredSize().getHeight())));				
				squeeze_label.setBounds(insets.left + myWidth/5, insets.top, larger_label, larger_height);
				pointsize_label.setBounds(insets.left + myWidth/5, insets.top + larger_height + 5, 
						larger_label, larger_height);
				power_slider.setBounds(insets.left + myWidth/5 + larger_label, insets.top, 4*myWidth/5 - larger_label, larger_height);
				point_size_slider.setBounds(insets.left + myWidth/5 + larger_label, insets.top + larger_height + 5, 
						4*myWidth/5 - larger_label, larger_height);
			}			
			
			public Dimension getPreferredSize() {
				
				return new Dimension((int)Math.max(  squeeze_label.getPreferredSize().getWidth(),
						pointsize_label.getPreferredSize().getWidth()), (int)Math.max(squeeze_label.getPreferredSize().getHeight(), 
								Math.max(pointsize_label.getPreferredSize().getHeight(), 
										Math.max(power_slider.getPreferredSize().getHeight(), 
												point_size_slider.getPreferredSize().getHeight())))*2+5);
			}
		};
		control_panel.setBackground(Color.WHITE);
		
		squeeze_label = new JLabel("Squeeze");
		pointsize_label = new JLabel("Point Size");
		squeeze_label.setBackground(Color.WHITE);
		pointsize_label.setBackground(Color.WHITE);
		squeeze_label.setHorizontalAlignment(SwingConstants.RIGHT);
		pointsize_label.setHorizontalAlignment(SwingConstants.RIGHT);
		
		start_stop_button = new JButton("START");
		start_stop_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				synchronized (m_layout_thread) {

					if (!m_layout_thread.isAlive()) {
						start_stop_button.setText("STOP");
						is_paused = false;
						m_layout_thread.start();
					} else {
						is_paused = !is_paused;
						if (is_paused) {

							start_stop_button.setText("START");
						}
						else {
							
							start_stop_button.setText("STOP");
							m_layout_thread.notify();
						}
					}
				}
			}
		});

		power_slider = new JSlider(1, 32, GlimmerLayout.POWER_FACTOR);
		power_slider.addChangeListener(this);
		point_size_slider = new JSlider(1,10,point_radius);
		point_size_slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
			
				point_radius = point_size_slider.getValue();
				draw_panel.redraw();
			}
		});
		
		title_label = new JLabel("MDS Scatterplot");
		title_label.setForeground(PrettyColors.DarkGrey);

		
//		draw_edges_box = new JCheckBox( "Draw Selected Edges" );
//		draw_labels_box = new JCheckBox( "Draw Selected Labels" );
//		draw_edges_box.setSelected(GlimmerLayout.DRAW_EDGES);
//		draw_labels_box.setSelected(GlimmerLayout.DRAW_LABELS);
//		draw_edges_box.addItemListener(this);
//		draw_labels_box.addItemListener(this);

		control_panel.add( point_size_slider );
		control_panel.add( power_slider );
		control_panel.add( start_stop_button );
		control_panel.add( squeeze_label );
		control_panel.add( pointsize_label );
		
		this.add(title_label);
		this.add(draw_panel);
		this.add(control_panel);
		
		draw_panel.init();

		// make the layout thread

		m_layout_thread = new Thread(new Runnable() {

			@Override
			public void run() {

				int frame = 0;
				while (true) {
					frame++;
					synchronized (Thread.currentThread()) {
						if (is_paused) {
							try {
								Thread.currentThread().wait();
							} catch (InterruptedException e) {

								e.printStackTrace();
							}
						}
					}
					m_glimmer_layout.updateLayout();
					if( frame % frame_rate == 0 ) {

						draw_panel.redraw();
					}
				}
			}
		});

	}

	@Override
	public void selectionChanged(ArrayList<TopoTreeNode> nodes,
			TopoTreeNode hilighted, boolean selChanged, boolean hiChanged) {

		draw_panel.redraw();
	}

	@Override
	public void tagsChanged() {
		
		draw_panel.redraw();
	}

	public void paintComponent( Graphics g ) {
	
		draw_panel.redraw();
	}
	
	public class GlimmerCanvas extends PApplet {

		int x_offset = 0;
		int y_offset = 0;
		
		int x_initial = 0;
		int y_initial = 0;

		int x_initial_2 = 0;
		int y_initial_2 = 0;
		
		float x_scaler = 1.f;
				
		public void mouseReleased() {
			
			if( mouseButton != LEFT ) {
				
				
			}
		}
		public void mouseDragged() {
			
			if( mouseButton == LEFT ) {
				
				x_offset += mouseX - x_initial;
				y_offset += mouseY - y_initial;
				x_initial = mouseX;
				y_initial = mouseY;			
			}
			else {
				
				int x = mouseX - x_initial_2;
				int y = mouseY - y_initial_2;
				x_initial_2 = mouseX;
				y_initial_2 = mouseY;			

				x_scaler += (Math.abs(x)>Math.abs(y)?x:y)*0.01;
//				System.out.println("x_scaler = " + x_scaler);
			}
			
			redraw();
		}
		
		public void mousePressed() {
			
			if( mouseButton == LEFT ) {
				
				x_initial = mouseX;
				y_initial = mouseY;
			}
			else {
				
				x_initial_2 = mouseX;
				y_initial_2 = mouseY;
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -8333653779999966548L;

		public void setup() {
			
			noSmooth();
//			smooth();
			noLoop();

		}
		public void draw() {

			noStroke();
			
			// clear screen

			background(255);

			// get the bounds of the points

			float min_x = Float.MAX_VALUE;
			float max_x = Float.MIN_VALUE;
			float min_y = Float.MAX_VALUE;
			float max_y = Float.MIN_VALUE;

			for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

				min_x = Math.min(min_x, m_glimmer_layout.m_embed[i * 2]);
				max_x = Math.max(max_x, m_glimmer_layout.m_embed[i * 2]);
				min_y = Math.min(min_y, m_glimmer_layout.m_embed[i * 2 + 1]);
				max_y = Math.max(max_y, m_glimmer_layout.m_embed[i * 2 + 1]);
			}

			float x_trans = (getWidth() - 2 * boundary);// / (max_x - min_x);
			float y_trans = (getHeight() - 2 * boundary);// / (max_y - min_y));

			// draw the points

			fill(defaultColor.getRed(),defaultColor.getGreen(),defaultColor.getBlue());
			for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

//				System.out.println(""+ i + " DRAW AT : (" + ((int) Math.round(boundary
//						+ (m_glimmer_layout.m_embed[i * 2] - min_x)
//						* x_trans)) + "," + ((int) Math.round(boundary
//								+ (m_glimmer_layout.m_embed[i * 2 + 1] - min_y)
//								* y_trans)) + ")");
				ellipse(
						(int) Math.round(boundary
								+ x_offset + (m_glimmer_layout.m_embed[i * 2] - min_x)
								* x_trans * x_scaler),
						(int) Math.round(boundary
								+ y_offset + (m_glimmer_layout.m_embed[i * 2 + 1] - min_y)
								* y_trans * x_scaler), point_radius, point_radius);
			}
			
//			// draw the tagged points
//			
			for( int tag_number : m_tag_table.item_map.keySet() ) {
				
				Color c = m_tag_table.color_map.get(tag_number); 
				fill(c.getRed(),c.getGreen(),c.getBlue());
				for( int item : m_tag_table.item_map.get(tag_number)) {
					
					ellipse(
							(int) Math.round(boundary
									+ x_offset +(m_glimmer_layout.m_embed[item * 2] - min_x)
									* x_trans * x_scaler),
							(int) Math.round(boundary
									+ y_offset +(m_glimmer_layout.m_embed[item * 2 + 1] - min_y)
									* y_trans * x_scaler), point_radius, point_radius);
				}
			}

			strokeWeight(2);
			stroke(selectedColor.getRed(),selectedColor.getGreen(),selectedColor.getBlue());
			noFill();
//			// draw the selected points
			for( int i = 0; i < m_glimmer_layout.m_gm.getNodeCount(); i++ ) {
				
				for( TopoTreeNode[] nodes : m_topo_tree.tree_lookup) {

					if( nodes[i].selected ) {
						
						ellipse(
								(int) Math.round(boundary
										+ x_offset +(m_glimmer_layout.m_embed[i * 2] - min_x)
										* x_trans * x_scaler),
								(int) Math.round(boundary
										+ y_offset +(m_glimmer_layout.m_embed[i * 2 + 1] - min_y)
										* y_trans * x_scaler), point_radius, point_radius);
						break;
					}
				}
			}
			
			// draw the hilighted nodes
			strokeWeight(4);
			stroke(hilightColor.getRed(),hilightColor.getGreen(),hilightColor.getBlue());
			noFill();
			for( int i = 0; i < m_glimmer_layout.m_gm.getNodeCount(); i++ ) {
				
				for( TopoTreeNode[] nodes : m_topo_tree.tree_lookup) {

					if( nodes[i].hilighted ) {
						
						ellipse(
								(int) Math.round(boundary
										+ x_offset + (m_glimmer_layout.m_embed[i * 2] - min_x)
										* x_trans * x_scaler),
								(int) Math.round(boundary
										+ y_offset + (m_glimmer_layout.m_embed[i * 2 + 1] - min_y)
										* y_trans * x_scaler), point_radius, point_radius);
						break;
					}
				}
			}
			strokeWeight(1.f);
		}
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {

		if( arg0.getSource() == power_slider)  {
			
			GlimmerLayout.POWER_FACTOR = power_slider.getValue();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {

		if( arg0.getSource() == draw_edges_box ) {
			
			GlimmerLayout.DRAW_EDGES = draw_edges_box.isSelected();
		}
		if( arg0.getSource() == draw_labels_box ) {
			
			GlimmerLayout.DRAW_LABELS = draw_labels_box.isSelected();
		}
	}
}
