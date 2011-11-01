package snappy.graph;


import snappy.data.FeatureList;

public class SimpleNodeLabeller implements NodeLabeller {

	private FeatureList m_fl = null;

	public SimpleNodeLabeller( FeatureList fl ) {

		this.m_fl = fl;	
	}

	
	@Override
	public String getLabel(int node_number) {
		return m_fl.featureAt(node_number);
	}

	@Override
	public void resetLabels() {
		// do nothing
	}


	@Override
	public SizedLabel[] getSummaryLabel(GraphLayout gl) {
		
		return null;
	}

}
