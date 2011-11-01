package snappy.data;

/**
 * Just a simple structure to hold sampled edge data
 * 
 * @author sfingram
 *
 */
public class SimpleEdge {

	public int src;
	public int dst;
	public float w;
	
	public String toString() {
		
		return "(" + src + "," + dst + ") = " + w; 
	}
	
	public SimpleEdge( int src, int dst, float w ) {
		
		this.src = src;
		this.dst = dst;
		this.w   = w;
	}
}
