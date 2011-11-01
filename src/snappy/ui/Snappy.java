package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.ScrollPane;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import snappy.data.DistanceFunction;
import snappy.data.DistanceReader;
import snappy.data.FeatureList;
import snappy.data.NZData;
import snappy.data.SparseReader;
import snappy.graph.EdgeIntersectionLabeller;
import snappy.graph.GraphLayout;
import snappy.graph.GraphManager;
import snappy.graph.NodeLabeller;
import snappy.graph.SimpleNodeLabeller;
import snappy.graph.GraphLayout.LayoutType;
import snappy.graph.TopoTree;

public class Snappy extends JFrame implements ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6484320090318536140L;
	public static String VERSION_STRING = "0.0.1";
	public static int DISTANCE_BINS 	= 25;
	
	int default_component_bin = 1;
	
	String nonzero_data_filename = "";
	String df_data_filename 	 = "";
	String point_label_filename  = "";
	String nz_feature_label_filename = "";
	
	float[] current_histo = null;			// current state of the distance histogram
	HistSlider component_slider = null;		// slider that controls which components we see
	HistSlider distance_slider  = null;		// slider that controls which distance we permit in the graph
	TopoTreeControl tt_control = null;		// draws the topological component tree
	SparseReader sparse_reader  = null;		// reads the distance data
	DistanceFunction distance_function = null;
	NZData nz_data 					   = null;

	GraphManager graph_manager = null;		
	TopoTree topo_tree		   = null;
	float initial_cutoff_value = 1.f;
	GraphDrawer graph_drawer = null;
	
	ArrayList<GraphLayout> graph_layouts = null;
	
	boolean is_sparse = false;				// controls if we're reading sparse or not
	
	FeatureList point_feature_list = null;
	FeatureList edge_feature_list = null;
	NodeLabeller node_labeller = null;
	
	public void parseArgs(String[] args) {
				
		boolean inNZ 	= false;	// 'N' next argument is for nonzero data
		boolean inDM 	= false;	// 'D' next argument is for distance matrix
		boolean inIND 	= false;	// 'I' next argument is for indifferentiated value
		boolean inPF 	= false;	// 'P' next argument is for point label file
		boolean inNZF 	= false;	// 'Z' next argument is for nonzero label file
		
		for( String arg : args ) {
			
			// determine if the argument has a "dash switch"
			
			if( arg.length() > 1 && arg.charAt(0) == '-' ) {

				// the next argument should *not* be a dash if we're expecting input
				
				if( inNZ || inDM || inIND ) {
					
					System.out.println("PARSE ERROR: Trouble parsing argument \"" + arg + "\"");
					System.exit(0);
				}

				// version switch
				
				if( arg.charAt(1) == 'V' ) {
					
					System.out.println("Snappy Version " + Snappy.VERSION_STRING );
					System.exit(0);
				}

				// nonzero data switch
				
				else if( arg.charAt(1) == 'N' ) {
					
					is_sparse = true;
					inNZ = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inNZ = true;
					}
					else {
						
						nonzero_data_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'D' ) {
					
					inDM = true; 	// we are using nonzero data
					is_sparse = false;

					if( arg.length() == 2 ) {
						
						inDM = true;
					}
					else {
						
						df_data_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'I' ) {
					
					inIND = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inIND = true;
					}
					else {
						
						initial_cutoff_value = Float.parseFloat(arg.substring(2));
					}
				}
				else if( arg.charAt(1) == 'P' ) {
					
					inPF = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inPF = true;
					}
					else {
						
						point_label_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'Z' ) {
					
					inNZF = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inNZF = true;
					}
					else {
						
						nz_feature_label_filename = arg.substring(2);
					}
				}
				
				// error switch
				
				else {
					
					System.out.println("PARSE ERROR: Trouble parsing argument \"" + arg + "\"");
					System.exit(0);
				}
			}
			
			else if( inNZ ) {
				
				is_sparse = true;
				nonzero_data_filename = arg;
				inNZ = false;
			}			
			
			else if( inDM ) {
				
				is_sparse = false;
				df_data_filename = arg;
				inDM = false;
			}			
			
			else if( inIND ) {

				initial_cutoff_value = Float.parseFloat(arg);
				
				inIND = false;
			}			
			
			else if( inPF ) {
				
				point_label_filename = arg;
				inPF = false;
			}			
			
			else if( inNZF ) {
				
				nz_feature_label_filename = arg;
				inNZF = false;
			}			
		}
		
		// check if we never got an expected argument
		
		if( inNZ || inDM || inIND || inPF || inNZF ) {
			
			System.err.println("Error parsing input arguments.");
			System.exit(0);
		}
	}
	
	public Snappy( String[] args ) {
		
		super("Snappy");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.print("Parsing Command Line Args...");
		this.parseArgs(args);
		System.out.println("done.");
		
		// load data based on the arguments

		if( is_sparse ) {
			
			System.out.print("Loading Sparse Data...");
			try {
				
				nz_data = SparseReader.readNZData( new FileReader(nonzero_data_filename) );
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			
			graph_manager = new GraphManager( nz_data ); 
			System.out.println("done.");
		}
		else {
			
			System.out.print("Loading Distance Matrix Data from " + df_data_filename +"...");
			try {
				
				int ptCount = DistanceReader.readDistancePointSize(new FileReader( df_data_filename ));
				distance_function = DistanceReader.readSortedDistanceData(new FileReader( df_data_filename ),ptCount);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			graph_manager = new GraphManager( distance_function );
			System.out.println("done.");
		}
		
		// load label data
		
		if( point_label_filename != "" ) {
			
			System.out.print("Loading Point Features...");
			try {
				point_feature_list = FeatureList.readFeatureList(new FileReader(point_label_filename));
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			System.out.println("done.");
		}
		if( nz_feature_label_filename != "" ) {
			System.out.print("Loading Nonzero Features...");
			try {
				edge_feature_list = FeatureList.readFeatureList(new FileReader(nz_feature_label_filename));
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			System.out.println("done.");
		}
		
		// let the graph manager run for a moment
		
		System.out.print("Loading Initial Edges...");
		long start_time = System.currentTimeMillis();
		while( System.currentTimeMillis() - start_time < 5000L && !graph_manager.updateGraph() );
		System.out.println("done.");

		// compute the topo tree
		
		System.out.print("Computing TopoTree...");
		float[] mylevels = new float[DISTANCE_BINS];
		for( int i = DISTANCE_BINS; i > 0 ; i-- ) {
			
			mylevels[DISTANCE_BINS-i] = ((float)i) / ((float)DISTANCE_BINS);
		}
		topo_tree = new TopoTree(graph_manager, mylevels);
		System.out.println("done.");
		graph_manager.setCutoff(1.f);
		
		// perform component count and component layout
		
		System.out.print("Performing component count...");
		int component_count = graph_manager.countComponents();
		System.out.println("Number of components = " + component_count);
		System.out.println("done.");
		
		System.out.print("Performing component layout...");
		graph_layouts = new ArrayList<GraphLayout>();
		for( TopoTree.TopoTreeNode node : topo_tree.level_lookup.get(0)) {
			
			if(node.num_points > (int)Math.pow(2, 3) )
				graph_layouts.add(new GraphLayout(node,LayoutType.SUMMARY));
		}
//		for( int component = 0; component < component_count; component++ ) {
//			
//			System.out.println("Component Size = " + graph_manager.getSubComponents().get(component).size() );
//			if( graph_manager.getSubComponents().get(component).size() > default_component_bin )
//				graph_layouts.add(new GraphLayout(graph_manager,component,LayoutType.SUMMARY));
//		}
		System.out.println("done.");
		
		// build the initial histograms
		
		System.out.print("Building Histograms...");
//		int[] trashhisto_comp = graph_manager.getComponentHisto();
//		for( int i = 0; i < trashhisto_comp.length; i++ ) {
//			System.out.println(""+i+":" + trashhisto_comp[i] );
//		}
		component_slider = new HistSlider(graph_manager.getComponentHisto(),default_component_bin);
		distance_slider  = new HistSlider(graph_manager.getHisto(Snappy.DISTANCE_BINS),0.f,1.f,DISTANCE_BINS-1);
		distance_slider.isLog = true;
		distance_slider.useAbsolute = true;
		component_slider.useAbsolute = true;
		component_slider.isLog = true;
		
		tt_control = new TopoTreeControl(topo_tree,graph_manager.getHisto(Snappy.DISTANCE_BINS));
		
//		int[] trashhisto = graph_manager.getHisto(Snappy.DISTANCE_BINS);
		current_histo = new float[Snappy.DISTANCE_BINS];
		for(int i = 0; i < Snappy.DISTANCE_BINS; i++ ) {
//			System.out.println(""+i+":"+trashhisto[i]);
			current_histo[i] = (i+1)*(1.f / (float)Snappy.DISTANCE_BINS);
		}
		System.out.println("done.");
		
		// connect the graph and the sliders
		
		graph_manager.addChangeListener(this);
		component_slider.addChangeListener(this);
		distance_slider.addChangeListener(this);
		tt_control.addChangeListener(this);
		
		// build the graph drawing component
		
		System.out.print("Initializing graph drawer...");
		if( point_feature_list != null ) {
			node_labeller = new SimpleNodeLabeller(point_feature_list);
		}
		if( edge_feature_list!= null ) {
			node_labeller = new EdgeIntersectionLabeller(graph_manager, edge_feature_list, nz_data);
		}
		graph_drawer = new GraphDrawer( node_labeller );
		System.out.println("done.");
		
		// lay out the components
		
		JPanel slider_panels = new JPanel();
		//slider_panels.setLayout(new GridLayout(2,1) );
		slider_panels.setLayout(new GridLayout(1,1) );
//		slider_panels.add(distance_slider);
		//slider_panels.add(component_slider);
		slider_panels.add(tt_control);
		
		this.getContentPane().setLayout(new BorderLayout(5,5));		
		this.getContentPane().add( slider_panels, "West");
		
		//JScrollPane scroll_pane = new JScrollPane(graph_drawer);
		ScrollPane scroll_pane2 = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scroll_pane2.setPreferredSize(new Dimension(500,100));
		scroll_pane2.add(graph_drawer);
		scroll_pane2.getVAdjustable().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				graph_drawer.redraw();
				
			}} );
		this.getContentPane().add( scroll_pane2, "Center" );

		// init the processing apps
		
		distance_slider.init();
		component_slider.init();
		graph_drawer.init();
		tt_control.init();

		// set the proper sizes for the graph drawer
		graph_drawer.setRectangles(graph_layouts);

		this.pack();
		this.setVisible(true);
	}
	
	public static void main( final String[] args ) {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
            	Snappy snappy = new Snappy(args);
            }
        } );
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {

		if( arg0.getSource() == tt_control.hilight_changed ) {

		    SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		          // Set the preferred size so that the layout managers can handle it
					graph_drawer.redraw();
		        }
		      });
		}
		if( arg0.getSource() == tt_control.cutoff_changed ) {
			
			graph_manager.setCutoff(current_histo[(current_histo.length-tt_control.getSelLevel())-1]);
		}
		if( arg0.getSource() == tt_control.compon_changed ) {

			graph_layouts.clear();
			for( TopoTree.TopoTreeNode node : tt_control.m_tt.level_lookup.get(tt_control.getSelLevel())) {
				
				if(node.num_points > tt_control.getIgnoreComponentSize() )
					graph_layouts.add(new GraphLayout(node,LayoutType.SUMMARY));
			}
//			for( int component = 0; component < graph_manager.getSubComponents().size(); component++ ) {
//				
//				if( graph_manager.getSubComponents().get(component).size() > tt_control.getIgnoreComponentSize() )
//					graph_layouts.add(new GraphLayout(graph_manager,component,LayoutType.SUMMARY));
//			}
			graph_drawer.setRectangles(graph_layouts);
			graph_drawer.packup();
			graph_drawer.redraw();
		}
		if( arg0.getSource() == graph_manager ) {
			
//			graph_layouts.clear();
//			for( int component = 0; component < graph_manager.getSubComponents().size(); component++ ) {
//				
//				if( graph_manager.getSubComponents().get(component).size() > component_slider.cutoff )
//					graph_layouts.add(new GraphLayout(graph_manager,component,LayoutType.SUMMARY));
//			}
			
			// the graph has changed, update the controls
			
			component_slider.setBins(graph_manager.getComponentHisto());
			distance_slider.setBins(graph_manager.getHisto(Snappy.DISTANCE_BINS));

			//
			graph_layouts = new ArrayList<GraphLayout>();
			int component_count = graph_manager.countComponents();
			for( TopoTree.TopoTreeNode node : tt_control.m_tt.level_lookup.get(tt_control.getSelLevel())) {
				
				if(node.num_points > tt_control.getIgnoreComponentSize() )
					graph_layouts.add(new GraphLayout(node,LayoutType.SUMMARY));
			}
//			for( int component = 0; component < component_count; component++ ) {
//				
////				System.out.println("Component Size = " + graph_manager.getSubComponents().get(component).size() );
//				//if( graph_manager.getSubComponents().get(component).size() > component_slider.cutoff )
//				if( graph_manager.getSubComponents().get(component).size() > tt_control.getIgnoreComponenetSize() )
//					graph_layouts.add(new GraphLayout(graph_manager,component,LayoutType.SUMMARY));
//			}
			graph_drawer.setRectangles(graph_layouts);
			graph_drawer.packup();
			graph_drawer.redraw();
		}
	}
}
