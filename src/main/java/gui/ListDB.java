package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.naming.CommunicationException;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

public class ListDB extends JPanel {

	JList list;
	DefaultListModel model;
	private ImageIcon image;
	Connection conn;
	Statement stmt;
	JScrollPane result;
	JTextArea console;
	GregorianCalendar cg;

	public ListDB() {
		super(true);
		setLayout(new BorderLayout());
		model = new DefaultListModel();
		list = new JList(model);
		list.setCellRenderer(new DBCellRenderer());
		list.setVisibleRowCount(6);

		// this.image = new ImageIcon(this.getClass().getResource("Schema.png"));

		JScrollPane pane = new JScrollPane(list);
		console = new JTextArea();
		result = new JScrollPane(console);
		console.setBackground(this.getBackground());
		console.setEditable(false);

		console.setFont(new Font("TimesRoman", Font.BOLD, 14));

		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:mydata.db");
			stmt = conn.createStatement();
		} catch (Exception e) {
			System.err.println("Generic Exception");
			e.printStackTrace();
		}

		updateList();

		add(pane, BorderLayout.NORTH);
	}

	public void updateList() {
		model.removeAllElements();

		try {
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getCatalogs();

			/*
			 * Aggiorno l'elenco dei db, inserendo tra i riconciliabili solo quelli le cui
			 * tabelle
			 * hanno un campo con l'indicatore della sorgente
			 */
			while (rs.next()) {
				boolean trovato = false;
				// Creo una connessione a quel db per ricavare la struttura
				Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + rs.getString(1), "root",
						"admin");
				DatabaseMetaData dbm2 = conn2.getMetaData();
				ResultSet table = dbm2.getTables(null, null, null, null);
				while (table.next() && !trovato) {
					ResultSet attributi = dbm2.getColumns(null, null, table.getString(3), null);
					while (attributi.next() && !trovato) {
						if (attributi.getString(4).equalsIgnoreCase("Src")) {
							model.addElement(rs.getString(1));
							trovato = true;
						}
					}

				}
				conn2.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void creaRiconciliato(Vector<String> v) {
		cg = new GregorianCalendar();
		// L'utente deve aver selezionato almeno due database da unire
		if (v.size() > 1) {
			String nameDb = "";
			for (int i = 0; i < v.size(); i++)
				nameDb += v.get(i);
			String create = "CREATE DATABASE " + nameDb;

			try {
				stmt.executeUpdate(create);
				/*
				 * Creo un database identico al primo tra quelli selezionati dall'utente
				 */
				Connection conn1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + v.get(0), "root", "admin");

				DatabaseMetaData dbm = conn1.getMetaData();
				ResultSet table = dbm.getTables(null, null, null, null);

				while (table.next()) {
					create = "CREATE TABLE " + nameDb + "." + table.getString(3) + " SELECT * FROM " + v.get(0) + "."
							+ table.getString(3);
					stmt.executeUpdate(create);
				}

				for (int i = 1; i < v.size(); i++) {
					conn1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + v.get(i), "root", "admin");

					dbm = conn1.getMetaData();
					table = dbm.getTables(null, null, null, null);

					while (table.next()) {
						create = "INSERT INTO " + nameDb + "." + table.getString(3) + " SELECT * FROM " + v.get(i) + "."
								+ table.getString(3);
						stmt.executeUpdate(create);
					}
				}

				model.addElement(nameDb);
				int month = cg.get(Calendar.MONTH) + 1;
				if (month < 10 && cg.get(Calendar.MINUTE) < 10)
					console.append(cg.get(Calendar.DATE) + "/0" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":0" + cg.get(Calendar.MINUTE) + "\n");
				else if (month < 10 && cg.get(Calendar.MINUTE) >= 10)
					console.append(cg.get(Calendar.DATE) + "/0" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":" + cg.get(Calendar.MINUTE) + "\n");
				else if (month >= 10 && cg.get(Calendar.MINUTE) < 10)
					console.append(cg.get(Calendar.DATE) + "/" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":0" + cg.get(Calendar.MINUTE) + "\n");
				else
					console.append(cg.get(Calendar.DATE) + "/" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":" + cg.get(Calendar.MINUTE) + "\n");
				console.append("Creazione database riconciliato " + nameDb + " effettuata con successo.\n\n");
			} catch (SQLException e) {
				int month = cg.get(Calendar.MONTH) + 1;
				if (month < 10 && cg.get(Calendar.MINUTE) < 10)
					console.append(cg.get(Calendar.DATE) + "/0" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":0" + cg.get(Calendar.MINUTE) + "\n");
				else if (month < 10 && cg.get(Calendar.MINUTE) >= 10)
					console.append(cg.get(Calendar.DATE) + "/0" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":" + cg.get(Calendar.MINUTE) + "\n");
				else if (month >= 10 && cg.get(Calendar.MINUTE) < 10)
					console.append(cg.get(Calendar.DATE) + "/" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":0" + cg.get(Calendar.MINUTE) + "\n");
				else
					console.append(cg.get(Calendar.DATE) + "/" + month + "/" + cg.get(Calendar.YEAR) + "   h "
							+ cg.get(Calendar.HOUR_OF_DAY) + ":" + cg.get(Calendar.MINUTE) + "\n");
				if (e.getErrorCode() == 1007) {
					console.append("Impossibile creare database " + nameDb + " perch� gi� esiste.\n\n");
				} else if (e.getErrorCode() == 1146) {
					console.append("Impossibile riconciliare i database selezionati perch� presentano schemi eterogenei.\n\n");
					create = "DROP DATABASE " + nameDb;
					try {
						stmt.executeUpdate(create);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	class DBCellRenderer extends JLabel implements ListCellRenderer {
		Color highlightColor = new Color(0, 0, 128);

		DBCellRenderer() {
			setOpaque(true);
		}

		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
			// If this is the selection value request from a combo box
			// then find out the current selected index from the list.
			if (index == -1) {
				int selected = list.getSelectedIndex();
				if (selected == -1)
					return this;
				else
					index = selected;
			}
			setText(" " + model.getElementAt(index));
			setIcon(image);
			if (isSelected) {
				setBackground(highlightColor);
				setForeground(Color.white);
			} else {
				setBackground(Color.white);
				setForeground(Color.black);
			}
			return this;
		}
	}
}
