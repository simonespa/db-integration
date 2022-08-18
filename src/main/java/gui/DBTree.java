package gui;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class DBTree extends JPanel{
	Connection conn;
	Statement stmt;
	JTree tree;
	String dbName;

	public DBTree(){
		super(true);
		setLayout(new BorderLayout());
		dbName = "";

		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:mydata.db");
			stmt = conn.createStatement();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		tree = new JTree();
		costruisciTree();
	}

	public void costruisciTree(){
		/*
		 * Risalgo, tramite i metadati, ai database gestiti da questo dbms.
		 */
		DatabaseMetaData dbm;

		try {
			dbm = conn.getMetaData();
			ResultSet db = dbm.getCatalogs();
			DefaultMutableTreeNode root = new DefaultMutableTreeNode("Database");

			while(db.next()){

				DefaultMutableTreeNode database = null, tabelle = null;
				DefaultTreeModel model;
				/*
				 * Considero solo i database che sono risultato dell'applicazione di un operatore d'integrazione
				 */
				if(db.getString(1).contains("_integrato")){
					database = new DefaultMutableTreeNode(db.getString(1));
					//Creo una connessione a quel db per ricavare la struttura
					Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+db.getString(1), "root", "admin");
					DatabaseMetaData dbm2 = conn2.getMetaData();
					ResultSet table = dbm2.getTables(null, null, null, null);
					while(table.next()){
						tabelle = new DefaultMutableTreeNode(table.getString(3));
						database.add(tabelle);
						ResultSet attributi = dbm2.getColumns(null, null, table.getString(3), null);
						while(attributi.next()){
							tabelle.add(new DefaultMutableTreeNode(attributi.getString(4)));
						}
					}
					root.add(database);
					conn2.close();

					model = new DefaultTreeModel(root);
					tree.setModel(model);

					model.reload();
					// And display it
					add(tree, BorderLayout.CENTER);
				}
			}
			/*
			 * Ricavo il nome del db selezionato
			 */
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					TreePath tp = tse.getNewLeadSelectionPath();
					if(!tp.toString().equals("[Database]")){
						String primaParte = tp.toString().substring(11);
						if(primaParte.indexOf(",")==-1)
							dbName = primaParte.substring(0, primaParte.indexOf("]"));
						else dbName = primaParte.substring(0, primaParte.indexOf(","));
					}else dbName = "";

					System.out.println(dbName);
				}
			} );

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
