package snappy.graph;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import snappy.ui.PrettyColors;

public class TagTable {

	public class TagQuery {
		
		public boolean isPartial;
		public boolean isFull;
		public Color fullColor;
		public Color partialColor;
		
		public boolean hasTag;
		public int queuePos;
		
		public TagQuery() {
			
			isPartial = false;
			hasTag = false;
			isFull = false;
			fullColor = Color.black;
			partialColor = Color.black;
			queuePos = -1;
		}
	}
	
	TopoTree m_tree = null;									// tree containing all the nodes

	public class Tag {
		
		
		public HashSet<Integer> items = null;
		public HashSet<TopoTreeNode> full_components = null;
		public HashSet<TopoTreeNode> part_components = null;
		
		public Color tag_color = null;
		public String name = null;
		
		public boolean is_select = false;
		public boolean is_item = false;
		
		public String to_file( ) {

			String str = name+"::";
			str += tag_color.getRed() + "," + tag_color.getGreen() + "," + tag_color.getBlue() + "::";
			int k = 0;
			for( Integer item : items ) {
				
				if( k < items.size()-1 )
					str += (item+",");
				else
					str += item;
					
				k++;
			}
			
			return str;
		}
		
		public Tag( String file_line ) {
			
			this("",Color.black);
			
			// load from string
			String[] fields = file_line.split("::");			
			name = fields[0];
			
			String[] color_fields = fields[1].split(",");
			tag_color = new Color(Integer.parseInt(color_fields[0]),
							Integer.parseInt(color_fields[1]),
							Integer.parseInt(color_fields[2]));
			
			if( fields.length > 2) {
			
				String[] itemnum_fields = fields[2].split(",");
				ArrayList<Integer> item_input = new ArrayList<Integer>();
				for( String itemstr : itemnum_fields ) {
	
					item_input.add( Integer.parseInt(itemstr));
				}
				
				addItem(item_input);
			}
		}

		public void addComponent( TopoTreeNode node ) {
			
			// add the sub items
			
			items.addAll(node.component);
			
			// add to full
			
			addSubtreeToFull(node);
			
			// add parents
			
			TopoTreeNode parent = node.parent;
			while( parent != null ) {
				
				if( full_components.contains(parent) )
					break;
				
				if( part_components.contains(parent) ) {
				
					// determine if we've filled the component to full
					
					boolean isFull = parent.containsOnlyItems(items);
					
					// if we didn't fill it up, then 
					
					if( isFull ) {

						part_components.remove(parent);
						full_components.add(parent);
					}
					
					break;
				}
				else {
				
					part_components.add(parent);
				}
				
				parent = parent.parent;
			}
			
		}
		
		public void addItem( ArrayList<Integer> add_items ) {
			
			items.addAll(add_items);
			
			// let's update the components
			
			Stack<TopoTreeNode> stack = new Stack<TopoTreeNode>();
			for( TopoTreeNode root : m_tree.roots ) {
				
				stack.push(root);
			}
			while( ! stack.isEmpty() ) {
				
				TopoTreeNode node = stack.pop();
				if( node.containsAnyItems(add_items)) {
					
					if( node.containsOnlyItems(add_items) ) {
						
						// add the remaining to the full
						
						addSubtreeToFull(node);
					}
					else {
						
						part_components.add(node);
						
						// add the children
						for( TopoTreeNode child : node.children ) {
							
							stack.push(child);
						}
					}
				}
			}
		}

		private void removeSubtreeFromAll( TopoTreeNode node  ) {

			if( part_components.contains(node) ) {
				part_components.remove(node);
			}
			if( full_components.contains(node) ) {
				full_components.remove(node);
			}
			
			for( TopoTreeNode child : node.children ) {

				removeSubtreeFromAll(child);
			}
		}
		private void addSubtreeToFull( TopoTreeNode node ) {
			
			full_components.add( node );
			
			if( part_components.contains(node) ) {
				part_components.remove(node);
			}
			
			for( TopoTreeNode child : node.children ) {

				addSubtreeToFull(child);
			}
		}
		
		public void removeItem( ArrayList<Integer> rem_items ) {
			
			items.removeAll(rem_items);
			
			// let's update the components
			
			ArrayList<TopoTreeNode> later_nodes = new ArrayList<TopoTreeNode>();
			Stack<TopoTreeNode> stack = new Stack<TopoTreeNode>();
			for( TopoTreeNode root : m_tree.roots ) {
				
				stack.push(root);
			}
			while( ! stack.isEmpty() ) {
				
				TopoTreeNode node = stack.pop();
				if( node.containsAnyItems(rem_items)) {
					
					if( node.containsOnlyItems(rem_items) ) {
						
						later_nodes.add(node);
					}
					else if(node.children.size()>0) {
						
						// add the children
						for( TopoTreeNode child : node.children ) {
							
							stack.push(child);
						}
					}
					else {
						later_nodes.add(node);
					}
				}
			}
			
			for( TopoTreeNode node : later_nodes ) {
				
				removeComponent(node);
			}
		}
		
		public void removeComponent( TopoTreeNode node ) {
			
			if( ! full_components.contains(node) && ! part_components.contains(node) ) 
				return;
			
			items.removeAll(node.component);
			
			removeSubtreeFromAll( node );
			
			TopoTreeNode parent = node.parent;
			while( parent != null ) {
				
				if( full_components.contains(parent) ) {
					
					full_components.remove(parent);
					part_components.add(parent);
				}
				
				boolean remove_from_part = true;
				for( TopoTreeNode child : parent.children ) {

					if( part_components.contains(child) || full_components.contains(child) ) {
						
						remove_from_part = false;
					}
				}
				if( remove_from_part ) {

					part_components.remove(parent);
				}
				
				parent = parent.parent;
			}
		}
		
		public Tag( String name, Color tag_color ) {
			
			this.name = name;
			this.tag_color = tag_color;
			
			items = new HashSet<Integer>();
			full_components = new HashSet<TopoTreeNode>();
			part_components = new HashSet<TopoTreeNode>();
		}
		
		public String toString() {
			
			String val = "";
			
			val += "Name: "  + name + "\n";
			val += "Color: " + tag_color + "\n";
			val += "Items: ";
			for( Integer item : items ) {
				
				val += (item + ",");
			}
			val += "\n";
			val += "Partial: ";
			for( TopoTreeNode node : part_components ) {
				
				val += "\t[";
				for( Integer item : node.component ) {
					
					val += (item + ",");
				}
				val += "]\n";
			}
			val += "\n";
			val += "Full: ";
			for( TopoTreeNode node : full_components ) {
				
				val += "\t[";
				for( Integer item : node.component ) {
					
					val += (item + ",");
				}
				val += "]\n";
			}
			val += "\n";
			
			return val;
		}
	}
	
	public ArrayList<Tag> tag_queue = null;	// list of tags	
	public ArrayList<Tag> tag_order_added = null;	// list of tags	
	ArrayList<TagChangeListener> tagChangeListeners = null; 
	String m_tagFilename = null;							// tag filename to which we save the tag info

	public void promoteTag( Tag tag ) {
		
		int current_idx = tag_queue.indexOf( tag );
		if( current_idx > 0 ) {
			
			tag_queue.remove(current_idx);
			tag_queue.add(0,tag);
		}
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}		
	}
	
	public void promoteTagSilent( Tag tag ) {
		
		int current_idx = tag_queue.indexOf( tag );
		if( current_idx > 0 ) {
			
			tag_queue.remove(current_idx);
			tag_queue.add(0,tag);
		}		
	}
	
	public TagTable(TopoTree tree) {

		// create lists
		
		tagChangeListeners = new ArrayList<TagChangeListener>();
		
		tag_queue = new ArrayList<TagTable.Tag>();		
		tag_order_added = new ArrayList<TagTable.Tag>();
		
		m_tree = tree;
		
		newTag("ITEM");
		topTag().is_item= true;
		topTag().tag_color = Color.black;
		newTag("Selected Nodes");
		promoteTag(tag_queue.get(tag_queue.size()-1));
		topTag().is_select = true;
		topTag().tag_color = PrettyColors.Red;
		
	}
	
	public Tag getSelTag() {
		
		for( Tag t : tag_queue ) {
			
			if( t.is_select )
				return t;
		}
		
		return null;
	}
	
	public Tag getItemTag() {
		
		for( Tag t : tag_queue ) {
			
			if( t.is_item )
				return t;
		}
		
		return null;
	}
	
	public void addTagChangeListener( TagChangeListener listener ) {
		
		tagChangeListeners.add( listener );
	}
	
	public void killTag( Tag tag ) {
		
		tag_queue.remove( tag );
		tag_order_added.remove(tag);
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}		
	}
	
	public void newTag( String tagName ) {
		
		Tag tag = new Tag( tagName, PrettyColors.colorFromInt(tag_queue.size()+1) );
		tag_queue.add(tag);
		tag_order_added.add(tag);
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}
	}
	
	/*
	 * Checks if a given tag exists in the tag set
	 */
	public boolean tagExists( String tagName ) {
		
		boolean tag_exists = false;
		
		for( Tag tag: tag_queue ) {
			
			if( tagName.compareTo( tag.name ) == 0 ) {
				
				tag_exists = true;
				break;
			}
		}
		
		return tag_exists;
	}

	
	/*
	 * Constructs and applies tags from a file
	 */
	public void loadTagFile( String tagFileName ) {
		
		m_tagFilename = tagFileName;
		
		try{
			
			BufferedReader breader = new BufferedReader( new FileReader(tagFileName) );
			
			String lineStr = breader.readLine();
			while( lineStr != null && lineStr.length() > 0 ) {

				// parse the tag line 
				
				Tag newTag = new Tag( lineStr );
				tag_queue.add( newTag );
				tag_order_added.add( newTag );
				
				lineStr = breader.readLine();
			}
			
			breader.close();
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}		
	}
	
	/*
	 * Writes out the current tag list 
	 */
	public void saveTagFile( String tagFileName ) {
		
		try {
			m_tagFilename = tagFileName;
			FileWriter fw = new FileWriter(m_tagFilename);
			BufferedWriter bw = new BufferedWriter(fw);
			for( Tag tag : tag_queue ) {

				if( ! tag.is_select && ! tag.is_item )
					bw.write( tag.to_file() + "\n" );
			}
			bw.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public Tag topTag() {
		
		if( tag_queue.size() > 0 ) {
			
			return tag_queue.get(0);
		}
		
		return null;
	}
	
	public Tag topNonitemTag() {
		
		if( tag_queue.size() > 0 ) {
			
			return tag_queue.get(0).is_item ? (tag_queue.size()>1?tag_queue.get(1):null) : tag_queue.get(0); 
		}
		
		return null;
	}
	
	public Color itemColor( Integer item ) {
		
		for( int i = 0; i < tag_queue.size(); i++ ) {
			
			Tag tag = tag_queue.get(i); 			
			if( tag.items.contains(item) ) {
				
				return tag.tag_color;
			}
		}
		
		return null;
	}
	
	public void addFromActiveSet( Tag t, ArrayList<Integer> active_set ) {

		t.addItem(active_set);
		promoteTag(t);
	}
	
	public void remFromActiveSet( Tag t, ArrayList<Integer> active_set ) {

		t.removeItem(active_set);
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}
	}
	
	public TagQuery queryNode( TopoTreeNode node ) {
		
		TagQuery tq = new TagQuery();
		
		for( int i = 0; i < tag_queue.size(); i++ ) {
			
			Tag tag = tag_queue.get(i); 
			if( ! tq.isPartial && tag.part_components.contains(node) ) {
				
				tq.hasTag = true;
				tq.partialColor = tag.tag_color;
				tq.isPartial = true;
				tq.queuePos = i;				
			}
			if( tag.full_components.contains(node) ) {
				
				tq.hasTag = true;
				tq.fullColor = tag.tag_color;
				tq.isFull = true;
				if( ! tq.isPartial ) {
					tq.queuePos = i;
				}
				
				return tq;
			}
		}
		
		return tq;
	}
	
	public static void main( String[] args ) {
		
		// create test tree
		
		TopoTree test_tree = new TopoTree();
		test_tree.num_levels = 3;
		TopoTreeNode a = new TopoTreeNode();
		a.component = new ArrayList<Integer>();
		TopoTreeNode b = new TopoTreeNode();
		b.component = new ArrayList<Integer>();
		TopoTreeNode c = new TopoTreeNode();
		c.component = new ArrayList<Integer>();
		TopoTreeNode d = new TopoTreeNode();
		d.component = new ArrayList<Integer>();
		TopoTreeNode e = new TopoTreeNode();
		e.component = new ArrayList<Integer>();
		TopoTreeNode f = new TopoTreeNode();
		f.component = new ArrayList<Integer>();
		TopoTreeNode g = new TopoTreeNode();
		g.component = new ArrayList<Integer>();
		TopoTreeNode h = new TopoTreeNode();
		h.component = new ArrayList<Integer>();

		a.component.add(1);
		a.component.add(2);
		a.component.add(3);
		a.component.add(4);
		a.component.add(5);
		a.children.add(b);
		a.children.add(c);
		a.parent = null;
		
		b.parent = a;
		b.children.add(d);
		b.children.add(e);
		b.component.add(1);
		b.component.add(2);
		
		c.parent = a;
		c.children.add(f);
		c.children.add(g);
		c.children.add(h);
		c.component.add(3);
		c.component.add(4);
		c.component.add(5);
		
		d.parent = b;
		d.component.add(1);
		
		e.parent = b;
		e.component.add(2);
		
		f.parent = c;
		f.component.add(3);
		
		g.parent = c;
		g.component.add(4);
		
		h.parent = c;
		h.component.add(5);

		test_tree.roots.add(a);
		
		System.out.println("Created test tree");
		
		TagTable test_table = new TagTable(test_tree);
		
		System.out.println("Created test table");
		
		// create a pair of tags
		
		test_table.newTag("TEST TAG 1");
		test_table.newTag("TEST TAG 2");
		
		System.out.println("Created two tags");
		
		// add points to the tag
		
		ArrayList<Integer> test_item_set = new ArrayList<Integer>();
		test_item_set.add(1);
		test_item_set.add(2);
		ArrayList<Integer> test_item_set_2 = new ArrayList<Integer>();
		test_item_set_2.add(2);
		ArrayList<Integer> test_item_set_3 = new ArrayList<Integer>();
		test_item_set_3.add(1);

		test_table.topTag().addItem(test_item_set);
		System.out.println("Top Tag after item add = " + test_table.topTag());		
		
		// remove points from the tag
		
		test_table.topTag().removeItem(test_item_set_2);
		System.out.println("Top Tag after item remove A = " + test_table.topTag());		
		test_table.topTag().removeItem(test_item_set_3);
		System.out.println("Top Tag after item remove B = " + test_table.topTag());		
		
		// add component
		
		test_table.topTag().addComponent( c );
		System.out.println("Top Tag after comp add = " + test_table.topTag());		

		// remove component
		
		test_table.topTag().removeComponent( f );
		System.out.println("Top Tag after comp remove f = " + test_table.topTag());		

		test_table.topTag().removeComponent( g );
		System.out.println("Top Tag after comp remove g = " + test_table.topTag());		

		test_table.topTag().removeComponent( h );
		System.out.println("Top Tag after comp remove h = " + test_table.topTag());		

		// save and load to file
		
		test_table.topTag().addComponent(b);
		test_table.saveTagFile("/Users/sfingram/Documents/test_table.txt");
		TagTable test_table_2 = new TagTable(test_tree);
		test_table_2.loadTagFile("/Users/sfingram/Documents/test_table.txt");
		System.out.println("Loaded Tag = ");
		for( Tag tag : test_table_2.tag_queue ) {
			
			
			System.out.println(""+tag);		
		}
	}
}
