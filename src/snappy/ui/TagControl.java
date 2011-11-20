package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import snappy.graph.TagTable;
import snappy.graph.TagChangeListener;
import snappy.graph.TopoTreeNode;

public class TagControl extends JPanel implements ActionListener, TagChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3457881159438566542L;

	TopoTreeControl m_ttControl = null;
	JTextField newTagField = null;
	JButton newTagButton = null;
	JList tagList = null;
	TagTable m_ttable = null;
	JButton up_button = null;
	JButton kill_button = null;
	JButton add_button = null;
	JButton rmv_button = null;
	JButton save_button = null;
	File m_chosenFile = null;
	JLabel tag_label = null;
	JLabel item_label = null;
	JScrollPane scroll_pane = null;
	JLabel title_label = null;
	
	class TagIcon implements Icon {

		Color m_color = null;
		
		public TagIcon( Color myColor ) {
			
			m_color = myColor;
		}
		
		@Override
		public int getIconHeight() {

			return 16;
		}

		@Override
		public int getIconWidth() {

			return 16;
		}

		@Override
		public void paintIcon(Component arg0, Graphics arg1, int arg2, int arg3) {

			arg1.setColor(m_color);
			arg1.fillRect(arg2+3,arg3+3, 10, 10);
		}
		
	}
	class TagCell {
		
		public String m_text = "";
		public Color m_color = null;
		public int m_tag_number = -1;
		
		public TagCell( String text, Color color, int tag_number ) {
			
			m_text = text;
			m_tag_number = tag_number;
			m_color = color;
		}
	}
	
	class TagCellRenderer extends JPanel implements ListCellRenderer {

	     // This is the only method defined by ListCellRenderer.
	     // We just reconfigure the JLabel each time we're called.

		JLabel inner_label = null;
		
		public TagCellRenderer() {
			
			super();
	        this.setLayout(new BorderLayout(5,5));
	        inner_label = new JLabel();
	        this.add(inner_label,"Center");
		}
		
	     /**
		 * 
		 */
		private static final long serialVersionUID = -3588651844368756745L;

		public Component getListCellRendererComponent(
	       JList list,              // the list
	       Object value,            // value to display
	       int index,               // cell index
	       boolean isSelected,      // is the cell selected
	       boolean cellHasFocus)    // does the cell have focus
	     {
	    	 
			TagCell tcell = (TagCell) value;
	         String s = tcell.m_text;
	         inner_label.setText(s);
	         inner_label.setIcon(new TagIcon(tcell.m_color));
	         if (isSelected) {
	             setBackground(list.getSelectionBackground());
	             setForeground(list.getSelectionForeground());
	         } else {
	             setBackground(list.getBackground());
	             setForeground(list.getForeground());
	         }
	         setEnabled(list.isEnabled());
	         inner_label.setFont(list.getFont());
	         setOpaque(true);
//	         kill_button.setEnabled(true);
	         return this;
	     }
	 }
	
	public void doLayout() {
		
		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		int myWidth = (width - insets.left) - insets.right;
		int myHeight = (height - insets.top) - insets.bottom;
		
		// place title entry
		title_label.setBounds(insets.left, insets.top, myWidth, title_label.getPreferredSize().height);
		
		// place tag entry
		newTagField.setBounds(insets.left, insets.top + title_label.getPreferredSize().height + 5, myWidth, newTagField.getPreferredSize().height);
		
		// get the maximum size of the button panel
		int bpanel_height = tag_label.getPreferredSize().height + item_label.getPreferredSize().height +
						up_button.getPreferredSize().height + kill_button.getPreferredSize().height 
						+ add_button.getPreferredSize().height + rmv_button.getPreferredSize().height
						+ save_button.getPreferredSize().height + newTagButton.getPreferredSize().height + 5 * 7; 
		
		int indent_amount = 20;
		ArrayList<Integer> width_list = new ArrayList<Integer>();
		width_list.add(tag_label.getPreferredSize().width);
		width_list.add(item_label.getPreferredSize().width);
		width_list.add(indent_amount+up_button.getPreferredSize().width);
		width_list.add(indent_amount+kill_button.getPreferredSize().width);
		width_list.add(indent_amount+add_button.getPreferredSize().width);
		width_list.add(indent_amount+rmv_button.getPreferredSize().width);
		width_list.add(indent_amount+save_button.getPreferredSize().width);
		width_list.add(indent_amount+newTagButton.getPreferredSize().width);
		Collections.sort(width_list);
		int bpanel_width = width_list.get(width_list.size()-1);
		
		int list_width = myWidth - bpanel_width - 5;
		int list_height = myHeight - title_label.getPreferredSize().height - newTagField.getPreferredSize().height - 10;
		
		// place the list of tags
		
		scroll_pane.setBounds( insets.left, insets.top + title_label.getPreferredSize().height + newTagField.getPreferredSize().height + 10, list_width, list_height );
		
		// place the items of the button panel
		
		int v_offs = insets.top + title_label.getPreferredSize().height + newTagField.getPreferredSize().height + 10;
		tag_label.setBounds(insets.left + list_width + 5, 
				v_offs, 
				tag_label.getPreferredSize().width, tag_label.getPreferredSize().height);
		v_offs += tag_label.getPreferredSize().height + 5;
		newTagButton.setBounds(insets.left + list_width + 5 + indent_amount, 
				v_offs, 
				newTagButton.getPreferredSize().width, newTagButton.getPreferredSize().height);
		v_offs += newTagButton.getPreferredSize().height + 5;
		kill_button.setBounds(insets.left + list_width + 5 + indent_amount, 
				v_offs, 
				kill_button.getPreferredSize().width, kill_button.getPreferredSize().height);
		v_offs += kill_button.getPreferredSize().height + 5;
		up_button.setBounds(insets.left + list_width + 5 + indent_amount, 
				v_offs, 
				up_button.getPreferredSize().width, up_button.getPreferredSize().height);
		v_offs += up_button.getPreferredSize().height + 5;
		item_label.setBounds(insets.left + list_width + 5, 
				v_offs, 
				item_label.getPreferredSize().width, item_label.getPreferredSize().height);
		v_offs += item_label.getPreferredSize().height + 5;
		add_button.setBounds(insets.left + list_width + 5 + indent_amount, 
				v_offs, 
				add_button.getPreferredSize().width, add_button.getPreferredSize().height);
		v_offs += add_button.getPreferredSize().height + 5;
		rmv_button.setBounds(insets.left + list_width + 5 + indent_amount, 
				v_offs, 
				rmv_button.getPreferredSize().width, rmv_button.getPreferredSize().height);
		v_offs += rmv_button.getPreferredSize().height + 5;
		save_button.setBounds(insets.left + list_width + 5, 
				v_offs, 
				bpanel_width, save_button.getPreferredSize().height);		
	}
	
	public Dimension getPreferredSize() {
		
		Dimension d = new Dimension();
		
		// get the preferred size
		
		return d;
	}
	
	public TagControl(TagTable ttable, TopoTreeControl ttControl) {
		super();
		
		m_ttControl = ttControl;
		m_ttable = ttable;
		m_ttable.addTagChangeListener(this);
		m_ttable.addTagChangeListener(m_ttControl);
		m_ttControl.setTagTable(m_ttable);
		
		this.setBorder( BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,1,PrettyColors.Grey), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.white);
		
		newTagField = new JTextField();
		newTagField.setColumns(80);
		newTagButton = new JButton("create");
		newTagButton.addActionListener(this);
		
		tagList = new JList();
		tagList.setModel(new DefaultListModel());
		modelFromTable();
		tagList.setCellRenderer(new TagCellRenderer());
		
		scroll_pane = new JScrollPane(tagList); 
		kill_button = new JButton("delete");
		kill_button.addActionListener(this);
		up_button = new JButton("promote");
		up_button.addActionListener(this);
		add_button = new JButton("add to tag");
		rmv_button = new JButton("remove from tag");
		save_button = new JButton("save all");
		rmv_button.addActionListener(this);
		add_button.addActionListener(this);
		save_button.addActionListener(this);
		tag_label = new JLabel("Tags");
		item_label = new JLabel("Items");
		title_label = new JLabel("Tags Editor");
		title_label.setForeground(PrettyColors.DarkGrey);
		this.add(title_label);
		this.add(newTagField);
		this.add(newTagButton);
		this.add(scroll_pane);
		this.add(kill_button);
		this.add(up_button);
		this.add(rmv_button);
		this.add(add_button);
		this.add(save_button);
		this.add(tag_label);
		this.add(item_label);
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {

		if( arg0.getSource() == newTagButton ) {
			
			// add the tag to the (if it doesn't already exist
			
			if( newTagField.getText().length() > 0 ) {

				m_ttable.newTag(newTagField.getText());
			}
		}
		if( arg0.getSource() == up_button) {
			
			int sel_idx = tagList.getSelectedIndex();
			if( sel_idx > -1 ) {
				
				DefaultListModel model = (DefaultListModel)tagList.getModel();
				TagCell tag_cell = (TagCell) model.get(sel_idx);
				m_ttable.promoteTag(new Integer(tag_cell.m_tag_number));
			}
		}
		if( arg0.getSource() == kill_button) {
			
			int sel_idx = tagList.getSelectedIndex();
			if( sel_idx > -1 ) {
				
				DefaultListModel model = (DefaultListModel)tagList.getModel();
				TagCell tag_cell = (TagCell) model.get(sel_idx);
				m_ttable.killTag(tag_cell.m_tag_number);
			}
		}
		if( arg0.getSource() == add_button ) {
			
			int sel_idx = tagList.getSelectedIndex();
			if( sel_idx > -1 ) {
				
				DefaultListModel model = (DefaultListModel)tagList.getModel();
				TagCell tag_cell = (TagCell) model.get(sel_idx);
				m_ttable.addTag(tag_cell.m_tag_number, m_ttControl.getSelectionSuperset());
			}
		}
		if( arg0.getSource() == rmv_button ) {
			
			int sel_idx = tagList.getSelectedIndex();
			if( sel_idx > -1 ) {
				
				DefaultListModel model = (DefaultListModel)tagList.getModel();
				TagCell tag_cell = (TagCell) model.get(sel_idx);				
				m_ttable.removeTag(tag_cell.m_tag_number, m_ttControl.getSelectionSuperset());
			}
		}
		if( arg0.getSource() == save_button ) {

			JFileChooser chooser = null;
			if( m_chosenFile == null ) {
				chooser = new JFileChooser();
			}
			else {
				chooser = new JFileChooser(m_chosenFile);
			}
			int retval = chooser.showSaveDialog(this);
			if( retval == JFileChooser.APPROVE_OPTION) {
				
				m_chosenFile = chooser.getSelectedFile();
				try {
					m_ttable.saveTagFile(m_chosenFile.getCanonicalPath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}

	public void modelFromTable( ) {
		
		DefaultListModel model = (DefaultListModel)tagList.getModel();
		model.removeAllElements();
		
		for( Integer key : m_ttable.tagPriority ) {
//			System.out.println("\t"+m_ttable.name_map.get(key));
			model.addElement( new TagCell(m_ttable.name_map.get(key),m_ttable.color_map.get(key),key) );			
		}
	}

	@Override
	public void tagsChanged() {

//		System.out.println(" TAGS CHANGED : ");
//
		int sel_idx = tagList.getSelectedIndex();
		modelFromTable();
		DefaultListModel model = (DefaultListModel)tagList.getModel();
		if( sel_idx > -1 && model.getSize() > 0 )
			tagList.setSelectedIndex(sel_idx % model.getSize());
	}
}
