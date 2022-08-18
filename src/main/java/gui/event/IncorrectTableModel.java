package gui.event;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class IncorrectTableModel  extends AbstractTableModel {
	
	// Matrice contenente i dati della tabella
	Vector <String[]> cache;

	int colCount;
	// Array di stringhe contente il nome delle colonne
	String[] headers;
	
	public IncorrectTableModel() {
		cache = new Vector<String[]>();
	}

	public void setCache(Vector <String[]> cache, String[] headers) {
		this.cache = cache;
		this.headers = headers;
		
		colCount = headers.length;
		
		fireTableChanged(null);
	}

	public String getColumnName(int i) { 
		return headers[i]; 
	}
	
	@Override
	public int getColumnCount() {
		return colCount;
	}

	@Override
	public int getRowCount() {
		return cache.size();
	}

	@Override
	public Object getValueAt(int row, int column) {
		return ((String[])cache.elementAt(row))[column];
	}
	
}