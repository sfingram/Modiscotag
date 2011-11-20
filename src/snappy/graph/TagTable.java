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

import snappy.ui.PrettyColors;

public class TagTable {

	TopoTree m_tree = null;									// tree containing all the nodes
	
	public HashMap<Integer,Color> color_map = null;				// maps tag # to tag color
	public HashMap<Integer,HashSet<Integer>> item_map = null;		// maps tag # to list of nodes with that tag
	public HashMap<Integer,String> name_map = null;				// maps tag # to string name of the tag
	
	ArrayList<TagChangeListener> tagChangeListeners = null; 
	public ArrayList<Integer> tagPriority = null;
	
	String m_tagFilename = null;							// tag filename to which we save the tag info

	public void promoteTag( Integer tag_num ) {
		
		int current_idx = tagPriority.indexOf( tag_num );
		if( current_idx > 0 ) {
			
			tagPriority.remove(tag_num);
			tagPriority.add(current_idx-1,tag_num);
		}
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}		
	}
	
	public Integer getTopTag( Integer tagA, Integer tagB ) {
		
		return (tagPriority.indexOf(tagA)<tagPriority.indexOf(tagB))?tagA:tagB;
	}
	
	public int getTopTag( Set<Integer> tags ) {
		
		int min_priority = Integer.MAX_VALUE;
		int return_tag = -1;
		for( Integer i : tags ) {
			
			if( tagPriority.indexOf(i) < min_priority ) {
				
				return_tag = i;
			}
		}
		
		return return_tag;
	}
	
	public TagTable(TopoTree tree) {

		// create lists
		
		color_map = new HashMap<Integer, Color>();
		item_map = new HashMap<Integer, HashSet<Integer>>();
		name_map = new HashMap<Integer, String>();
		tagPriority = new ArrayList<Integer>();
		
		m_tree = tree;
		
		tagChangeListeners = new ArrayList<TagChangeListener>();
	}
	
	public void addTagChangeListener( TagChangeListener listener ) {
		
		tagChangeListeners.add( listener );
	}
	
	public void killTag( Integer tag_number ) {
		
		ArrayList<Integer> x = new ArrayList<Integer>(item_map.get(tag_number));
		removeTag( tag_number, x );
		
		item_map.remove(tag_number);
		name_map.remove(tag_number);
		color_map.remove(tag_number);
		tagPriority.remove(tag_number);
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}		
	}
	
	public int newTag( String tagName ) {
		
		int tag_number=0;
		
		System.out.println("Adding tag: " + tagName);
		
		// get the maximum tag number
		
		for( Integer i : name_map.keySet() ) {
			
			if( tag_number <= i ) {
				
				tag_number = i + 1;
			}
		}
		
		// create tag entries
		
		name_map.put(tag_number, tagName);
		color_map.put(tag_number, PrettyColors.colorFromInt(tag_number));
		item_map.put(tag_number, new HashSet<Integer>());
		tagPriority.add(tag_number);
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}
		
		return tag_number;
	}
	
	/*
	 * Checks if a given tag exists in the tag set
	 */
	public boolean tagExists( String tagName ) {
		
		boolean tag_exists = false;
		
		for( Integer tag_number: name_map.keySet() ) {
			
			if( tagName == name_map.get(tag_number) ) {
				
				tag_exists = true;
				break;
			}
		}
		
		return tag_exists;
	}
	

	public void addTag( Integer tag_number, ArrayList<TopoTreeNode> nodes ) {
		
		HashSet<Integer> items = new HashSet<Integer>();
		
		// check if this key even exists
		if( name_map.containsKey(tag_number) ) {
			
			// loop through the tagged items
			for( TopoTreeNode node : nodes ) {
				
				node.tags.put(new Integer(tag_number), node.component.size());
				node.tag_colors.put(new Integer(tag_number), color_map.get(tag_number));
				for( Integer item : node.component ) {
					
					items.add(item);
//					if( !item_map.get(tag_number).contains(item) ) {
//						
//						item_map.get(tag_number).add(item);
//					}
				}
			}
		}
		
		addTag(tag_number, new ArrayList<Integer>(items));
//		
//		// notify tag listeners to redraw
//		
//		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
//			
//			tagChangeListener.tagsChanged();
//		}
	}
	
	public void removeTag( Integer tag_number, ArrayList<TopoTreeNode> nodes ) {

		HashSet<Integer> items = new HashSet<Integer>();
		if( name_map.containsKey(tag_number) ) {
		
			for( TopoTreeNode node : nodes ) {

				for( Integer item : node.component ) {
					
					items.add(item);
				}
			}
		}
		
		removeTag( tag_number, new ArrayList<Integer>(items) );
	}
	
	/*
	 * Adds the tag "tag_number" to the nodes in the list "items"
	 */
	public void addTag( int tag_number, ArrayList<Integer> items ) {

		// check if this key even exists
		if( name_map.containsKey(tag_number) ) {
			
			// loop through the tagged items
			for( Integer item : items ) {
				
				// add the item to the tag map (if it isn't there)
				if( !item_map.get(tag_number).contains(item) ) {
										
					item_map.get(tag_number).add(item);
					
					// traverse the tree
					for( TopoTreeNode[] nodes : m_tree.tree_lookup ) {
						
						// update the tree nodes that their tag counts have changed						
						if(!nodes[item].tags.containsKey(tag_number)) {
							nodes[item].tags.put(tag_number,1);
							nodes[item].tag_colors.put(tag_number, color_map.get(tag_number));
						}
						else {
							nodes[item].tags.put(tag_number,nodes[item].tags.get(tag_number)+1);
						}
					}
				}
			}
		}
		
		// notify tag listeners to redraw
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}
	}

	/*
	 * Removes the tag "tag number" to the nodes in the list "items"
	 */
	public void removeTag( int tag_number, ArrayList<Integer> items ) {
		
		// check if this key even exists
		if( name_map.containsKey(tag_number) ) {
			
			// loop through the un-tagged items
			for( Integer item : items ) {
				
				// check if the item isn't in the tag map
				if( item_map.get(tag_number).contains(item) ) {
										
					item_map.get(tag_number).remove(item); // remove the item
					
					// traverse the tree
					for( TopoTreeNode[] nodes : m_tree.tree_lookup ) {
						
						// update the tree nodes that their tag counts have changed
						if(nodes[item].tags.containsKey(tag_number) ) {
							
							nodes[item].tags.put(tag_number,nodes[item].tags.get(tag_number)-1);
						
							// delete the tag from the node if it no longer exists
							if( nodes[item].tags.get(tag_number) == 0 ) {
								
								nodes[item].tags.remove(tag_number);
								nodes[item].tag_colors.remove(tag_number);
							}
						}
					}
				}
			}
		}
		
		for( TagChangeListener tagChangeListener : tagChangeListeners ) {
			
			tagChangeListener.tagsChanged();
		}
	}
	
	/*
	 * Constructs and applies tags from a file
	 */
	public void loadTagFile( String tagFileName ) {
		
		m_tagFilename = tagFileName;
		
		try{
			
			BufferedReader breader = new BufferedReader( new FileReader(tagFileName) );
			
			int tag_number = 0;
			String lineStr = breader.readLine();
			while( lineStr != null && lineStr.length() > 0 ) {

				// parse the tag line 
				
				String[] fields = lineStr.split("::");
				name_map.put(new Integer(tag_number), fields[0]);
				String[] color_fields = fields[1].split(",");
				color_map.put(tag_number, 
						new Color(Integer.parseInt(color_fields[0]),
								Integer.parseInt(color_fields[1]),
								Integer.parseInt(color_fields[2])));
				String[] itemnum_fields = fields[2].split(",");
				item_map.put(tag_number, new HashSet<Integer>());
				tagPriority.add(tag_number);

				ArrayList<Integer> items = new ArrayList<Integer>();
				for( String itemstr : itemnum_fields ) {

					items.add(Integer.parseInt(itemstr));
				}
				addTag(tag_number,items);
				
				lineStr = breader.readLine();
				
				tag_number++;
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
			for( Integer tag_number : tagPriority ) {
				
				String str = "";
				str += "" + name_map.get(tag_number) + "::";
				str += "" + color_map.get(tag_number).getRed() 
						+ "," + color_map.get(tag_number).getGreen() 
						+ "," + color_map.get(tag_number).getBlue() + "::";
				int k = 0;
				int ksize = item_map.get(tag_number).size();
				for(Integer item : item_map.get(tag_number) ) {
					
					str += item;
					if( k < ksize-1)
						str += ",";
					k++;
				}
				str += "\n";
				bw.write(str);
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
