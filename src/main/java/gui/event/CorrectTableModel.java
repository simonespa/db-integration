package gui.event;

import java.awt.Font;
import java.sql.*;
import java.util.Vector;

import javax.swing.JTextArea;
import javax.swing.table.*;

/**
 * Classe che si occupa di andare a cercare i dati correti da caricare. CorrectTableModel estende
 * l'AbstractTableModel, e ne ridefinisce i metodi astratti e quelli necessari per la corretta
 * visualizzazione.
 *
 * @author Chiara
 */
public class CorrectTableModel extends AbstractTableModel {

	// Matrice contenente i dati corretti della tabella
	Vector <String[]> cache;

	// Matrice contenente i dati scorretti della tabella
	Vector <String[]> cacheScorretti;

	int colCount;
	// Array di stringhe contente il nome delle colonne
	String[] headers;

	Connection db;
	Statement statement;
	String currentURL;

	public JTextArea resultQuery;

	public IncorrectTableModel itm;

	public CorrectTableModel( IncorrectTableModel itm) {
		cache = new Vector<String[]>();
		cacheScorretti = new Vector<String[]>();
		resultQuery = new JTextArea("");
		resultQuery.setEditable(false);
		resultQuery.setFont(new Font( "TimesRoman", Font.BOLD, 14 ) );

		this.itm = itm;
	}


	public void setQuery(String q, String dbName, int tipo) {
		if(q.equals(""))return;

		cache = new Vector<String[]>();
		if(q.substring(0,6).equalsIgnoreCase("SELECT")){
			try {
				Class.forName("org.sqlite.JDBC");
				db = DriverManager.getConnection("jdbc:sqlite:mydata.db");
				statement = db.createStatement();
				//Execute the query and store the result set and its metadata
				ResultSet rs = statement.executeQuery(q);
				ResultSetMetaData meta = rs.getMetaData();
				colCount = meta.getColumnCount();
				//Now we must rebuild the headers array with the new column names
				headers = new String[colCount];
				for (int i=0; i < colCount; i++) {
					headers[i] = meta.getColumnName(i+1);
				}

				if(tipo == 1){
					String nameDbRiconciliato = dbName.substring(0,dbName.lastIndexOf("_"));
					cacheScorretti = new Vector<String[]>();

					Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+nameDbRiconciliato, "root", "admin");
					Statement st = conn2.createStatement();
					ResultSet rr = st.executeQuery(q);
					boolean trovato;

					while(rr.next()){
						trovato = false;
						while(rs.next() && !trovato){
							for (int i=1; i <= meta.getColumnCount() && !trovato; i++) {
								//AGGIUNGERE CONTROLLI NEL CASO DI VALORI NULLI!!!
								if(rr.getString(i)!=null && rs.getString(i)!=null){
									trovato = !rr.getString(i).equals(rs.getString(i));
								}
							}
							if(!trovato){
								trovato=true;
							}else trovato = false;
						}
						if(!trovato){
							String[] record = new String[colCount];
							for (int i=0; i < colCount; i++) {
								record[i] = rr.getString(i+1);
							}
							cacheScorretti.addElement(record);
						}

						rs.beforeFirst();
					}

					conn2.close();
					st.close();
					itm.setCache(cacheScorretti, headers);
				}
				while(rs.next()){
					//	if(rs.getBoolean(meta.getColumnCount())){
					String[] record = new String[colCount];
					for (int i=0; i < colCount; i++) {
						record[i] = rs.getString(i+1);
					}
					cache.addElement(record);
					//	}
				}

				db.close();
				statement.close();
				resultQuery.setText("");
				fireTableChanged(null);// notify everyone that we have a new table.
			}catch(Exception e) {
				resultQuery.setText("SQL Exception: "+e.getMessage());
				cache = new Vector<String[]>(); // blank it out and keep going.
			}
			finally{
				// In ogni caso chiude la connessione al database

			}
		}else{
			resultQuery.setText("Operazione non consentita");
		}
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
