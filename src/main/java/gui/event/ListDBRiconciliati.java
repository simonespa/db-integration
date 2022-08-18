package gui.event;

import gui.DBTree;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;

public class ListDBRiconciliati extends JPanel {

	JComboBox list, listOperatori;
	DefaultComboBoxModel dlm, dlm2;
	Connection conn;
	Statement stmt;
	public JScrollPane result;
	JTextArea console;
	Integrazione itg;
	String dbName = "";
	String operatore = "";
	JPanel p1;
	GregorianCalendar cg;
	public DBTree tt;

	public ListDBRiconciliati() {
		super(true);
		setLayout(new BorderLayout());
		dlm = new DefaultComboBoxModel();
		list = new JComboBox(dlm);
		list.setMaximumRowCount(4);

		tt = new DBTree();

		console = new JTextArea();
		console.setBackground(this.getBackground());
		console.setEditable(false);
		console.setFont(new Font("TimesRoman", Font.BOLD, 14));

		result = new JScrollPane(console);

		p1 = new JPanel(new GridLayout(3, 2));

		try {
			Class.forName("org.sqlite.JDBC");
			this.conn = DriverManager.getConnection("jdbc:sqlite:mydata.db");
			stmt = this.conn.createStatement();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		setListRiconciliati();

		list.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dbName = (String) dlm.getElementAt(list.getSelectedIndex());
			}
		});

		p1.add(new JLabel("Seleziona il Database: "));
		p1.add(list);

		p1.add(new JPanel());
		p1.add(new JPanel());

		setOperatori();

		JPanel button = new JPanel();
		JButton jb3 = new JButton("Crea Database Integrato");
		jb3.transferFocusBackward();
		jb3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!dbName.equals("") && !operatore.equals("")) {
					cg = new GregorianCalendar();
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

					// Eseguo l'integrazione del db selezionato
					itg = new Integrazione(dbName);
					if (operatore.equals("Maggioranza")) {
						console.append(itg.integrazionePerMaggioranza() + "\n\n");
						tt.costruisciTree();
					} else {
						console.append(itg.integrazionePerPriorita() + "\n\n");
						tt.costruisciTree();
					}
				}
			}
		});
		button.add(jb3);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(button, BorderLayout.CENTER);

		add(p1, BorderLayout.NORTH);
		add(new JPanel(), BorderLayout.CENTER);
		add(p2, BorderLayout.SOUTH);
	}

	/*
	 * Metodo che costruisce l'elenco degli operatori d'integrazione disponibili
	 */
	private void setOperatori() {
		dlm2 = new DefaultComboBoxModel();
		dlm2.addElement("");
		dlm2.addElement("Priorità");
		dlm2.addElement("Maggioranza");

		listOperatori = new JComboBox(dlm2);
		listOperatori.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				operatore = (String) dlm2.getElementAt(listOperatori.getSelectedIndex());
			}
		});

		p1.add(new JLabel("Seleziona l'operatore d'integrazione: "));
		p1.add(listOperatori);
	}

	/*
	 * Metodo che costruisce l'elenco dei database riconciliati
	 */
	public void setListRiconciliati() {
		dlm.removeAllElements();
		dlm.addElement("");
		try {
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getCatalogs();

			/*
			 * Per ogni database presente nel dbms, considero solo quelli che possono essere
			 * integrati, cioè quelli i cui dati provvengono da almeno due sorgenti
			 */
			while (rs.next()) {
				boolean trovato = false;
				// Creo una connessione a quel db per ricavare la struttura
				Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + rs.getString(1), "root",
						"admin");
				Statement stm = conn2.createStatement();
				DatabaseMetaData dbm2 = conn2.getMetaData();
				ResultSet table = dbm2.getTables(null, null, null, null);
				while (table.next() && !trovato) {
					ResultSet attributi = dbm2.getColumns(null, null, table.getString(3), null);

					while (attributi.next() && !trovato) {
						if (attributi.getString(4).equalsIgnoreCase("Src")) {
							String query = "SELECT " + attributi.getString(4) + " FROM " + table.getString(3) + " GROUP BY "
									+ attributi.getString(4);
							ResultSet rsQuery = stm.executeQuery(query);
							int i = 0;
							while (rsQuery.next() && i < 2) {
								i++;
							}
							if (i == 2) {
								trovato = true;
								dlm.addElement(rs.getString(1));
							}
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// In ogni caso chiude la connessione al database

		}
	}
}
