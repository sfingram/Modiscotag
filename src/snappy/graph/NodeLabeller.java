package snappy.graph;

public interface NodeLabeller {

	public SizedLabel[] getSummaryLabel(GraphLayout gl);
	public String getLabel( int node_number );
	public void resetLabels();
}
