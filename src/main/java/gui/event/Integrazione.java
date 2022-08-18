package gui.event;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class Integrazione {

	Connection conn;
	Statement stmt;
	String nameDB;

	public Integrazione(String db) {
		nameDB = db;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:mydata.db");
			stmt = conn.createStatement();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public String integrazionePerPriorita(){
		try {

			String nomeNuovoDb;
			/*
			 * creo il database integrato
			 */
				nomeNuovoDb = nameDB+"_integratoByPriority";
				String creaDB = "CREATE DATABASE "+nomeNuovoDb;
				stmt.execute(creaDB);

			/*
			 * Creo un collegamento col database riconciliato per risalire, tramite i metadati,
			 * alla sua struttura e, in particolare, alle relazioni di cui è composto.
			 */
			Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+nameDB, "root", "admin");
			DatabaseMetaData dbm = conn2.getMetaData();
			ResultSet table = dbm.getTables(null, null, null, null);
			conn2.close();

			Vector<String> chiavi;
			Vector<String> nonChiavi;

			ResultSet rs;
			String query, name;
			String listaDiChiavi ,andDiChiavi, listaDiNonChiavi;

			/*
			 * Per ogni tabella del DB riconciliato, creo la corrispondente nel DB integrato
			 */
			while(table.next()){
				name = table.getString(3);
				chiavi = new Vector<String> ();
				nonChiavi = new Vector<String> ();
				listaDiChiavi="";
				andDiChiavi="";
				listaDiNonChiavi="";

				query = "SELECT * FROM "+nameDB+"."+name;
				System.out.println("12: "+query);

				rs = stmt.executeQuery(query);
				ResultSetMetaData rsm = rs.getMetaData();

				/*
				 * Distinguo gli attributi che non possono assumere valori
				 * nulli (con questo vincolo indico gli attributi che compongono la
				 * chiave primaria) da quelli non vincolati
				 */
				for(int i=1; i<=rsm.getColumnCount(); i++){
					if(rsm.isNullable(i)==ResultSetMetaData.columnNoNulls){
						chiavi.add(rsm.getColumnName(i));
						listaDiChiavi+=rsm.getColumnName(i)+", ";
						andDiChiavi+= nameDB+"."+name+"."+rsm.getColumnName(i)+" = p1."+rsm.getColumnName(i)+" AND ";
					}else{
						nonChiavi.add(rsm.getColumnName(i));
					}
				}

				/*
				 * Creo una vista per tutti gli attributi della tabella considerata, eccetto che
				 * per la chiave e l'indicatore della sorgente
				 */
				for(int i=0; i<nonChiavi.size()-1;i++){
					String a = nonChiavi.get(i);
					query = "CREATE VIEW "+nomeNuovoDb+".V"+name+a+" ("+listaDiChiavi+a+") AS " +
					" SELECT "+listaDiChiavi+a+" FROM "+nameDB+"."+name+" WHERE NOT EXISTS ( " +
					"SELECT * FROM "+nameDB+"."+name+" p1 WHERE "+andDiChiavi+"((p1."+a+" " +
					"IS NOT NULL AND ("+name+".src > p1.src OR "+name+"."+a+" IS NULL)) OR " +
					"(p1."+a+" IS NULL AND "+name+"."+a+" IS NULL AND "+name+".src > p1.src)))";

					System.out.println("13: "+query);

					stmt.executeUpdate(query);
				}

				/*
				 *
				 */
				if(nonChiavi.size()>1){
					String attributiNonChiaviTable = "";
					String visteAttributiNonChiave = "";
					for(int i=0; i<nonChiavi.size()-2; i++){
						listaDiNonChiavi+=nonChiavi.get(i)+", ";
						attributiNonChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(i)+"."+nonChiavi.get(i)+", ";
						visteAttributiNonChiave+=nomeNuovoDb+".V"+name+nonChiavi.get(i)+", ";
					}
					listaDiNonChiavi+=nonChiavi.get(nonChiavi.size()-2);
					attributiNonChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(nonChiavi.size()-2)+"."+nonChiavi.get(nonChiavi.size()-2);
					visteAttributiNonChiave+=nomeNuovoDb+".V"+name+nonChiavi.get(nonChiavi.size()-2);

					String attributiChiaviTable = "";
					String andDiChiavi2 = "";
					for(int i=0; i<chiavi.size(); i++){
						attributiChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(0)+"."+chiavi.get(i)+", ";
					}

					for(int j=0; j<nonChiavi.size()-2; j++){
						for(int i=0;i<chiavi.size(); i++)
							andDiChiavi2+=nomeNuovoDb+".V"+name+nonChiavi.get(j)+"."+chiavi.get(i)+" = "+nomeNuovoDb+".V"+name+nonChiavi.get(j+1)+"."+chiavi.get(i)+" AND ";
					}
					andDiChiavi2 = andDiChiavi2.substring(0, andDiChiavi2.length()-4);

					query = "CREATE TABLE "+nomeNuovoDb+"."+name+" SELECT "+attributiChiaviTable+attributiNonChiaviTable+" FROM "+visteAttributiNonChiave+" WHERE "+andDiChiavi2;
					System.out.println("14: "+query);

					stmt.executeUpdate(query);


					for(int i=0; i<nonChiavi.size()-1;i++){
						query= "DROP VIEW "+nomeNuovoDb+".V"+name+nonChiavi.get(i);
						System.out.println("15: "+query);

						stmt.executeUpdate(query);
					}

				}
				/*
				 * Per le tabelle associative composte da sole chiavi non devo fare alcuna operazione
				 * particolare, ma solo eliminare gli eventuali duplicati
				 */
				else{

					query = "CREATE TABLE "+nomeNuovoDb+"."+name+" SELECT "+listaDiChiavi.substring(0, listaDiChiavi.length()-2)+" FROM "+nameDB+"."+name+" GROUP BY "+listaDiChiavi.substring(0, listaDiChiavi.length()-2);

					System.out.println("17: "+query);
					stmt.executeUpdate(query);
				}
			}

			return "Creazione database integrato "+nomeNuovoDb+" effettuata con successo!";

		} catch (SQLException e) {
			return "SQLException: "+e.getMessage();
		}
	}

	/*
	 * MODIFICARE NOTA
	 * Tecnica d'integrazione che combina entrambe le tecniche. E' utile quando applicando l'operatore
	 * d'integrazione per maggioranza si ricava un database integrato comunque inconsistente. Questo si
	 * verifica quando per ogni dato non c'è una maggioranza assoluta e, di conseguenza, l'operatore
	 * degenera nel prodotto cartesiano. Tramite l'integrazione per priorità si elimina questa inconsistenza
	 */
	public String integrazionePerMaggioranza(){
		try {

			String nomeNuovoDb;
			/*
			 * creo il database integrato
			 */
			nomeNuovoDb = nameDB+"_integratoByMajority";

			String creaDB = "CREATE DATABASE "+nomeNuovoDb;
			System.out.println("1: "+creaDB);

			stmt.execute(creaDB);

			/*
			 * Creo un collegamento col database riconciliato per risalire, tramite i metadati,
			 * alla sua struttura e, in particolare, alle relazioni di cui è composto.
			 */
			Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+nameDB, "root", "admin");
			DatabaseMetaData dbm = conn2.getMetaData();
			ResultSet table = dbm.getTables(null, null, null, null);
			conn2.close();

			Vector<String> chiavi;
			Vector<String> nonChiavi;

			ResultSet rs;
			String query, name;
			String listaDiChiavi ,andDiChiavi, listaDiNonChiavi, andDiChiaviConVista, andDiChiaviConVista2;

			/*
			 * Per ogni tabella del DB riconciliato, creo la corrispondente nel DB integrato
			 */
			while(table.next()){
				name = table.getString(3);
				chiavi = new Vector<String> ();
				nonChiavi = new Vector<String> ();
				listaDiChiavi="";
				andDiChiavi="";
				andDiChiaviConVista="";
				andDiChiaviConVista2="";
				listaDiNonChiavi="";

				query = "SELECT * FROM "+nameDB+"."+name;
				System.out.println("2: "+query);

				rs = stmt.executeQuery(query);
				ResultSetMetaData rsm = rs.getMetaData();

				/*
				 * Distinguo gli attributi che non possono assumere valori
				 * nulli (con questo vincolo indico gli attributi che compongono la
				 * chiave primaria) da quelli non vincolati
				 */
				for(int i=1; i<=rsm.getColumnCount(); i++){
					if(rsm.isNullable(i)==ResultSetMetaData.columnNoNulls){
						chiavi.add(rsm.getColumnName(i));
						listaDiChiavi+=rsm.getColumnName(i)+", ";
						andDiChiavi+= "p."+rsm.getColumnName(i)+" = p1."+rsm.getColumnName(i)+" AND ";
						andDiChiaviConVista+= "p."+rsm.getColumnName(i)+" = v."+rsm.getColumnName(i)+" AND ";
						andDiChiaviConVista2+= "p1."+rsm.getColumnName(i)+" = v1."+rsm.getColumnName(i)+" AND ";
					}else{
						nonChiavi.add(rsm.getColumnName(i));
					}
				}

				/*
				 * Creo una vista per ogni attributo non chiave della tabella considerata, eccetto per l'indicatore
				 * della sorgente, in cui in corrispondenza di ogni coppia (chiave, attributo) associo il numero di
				 * volte in cui quella data istanza compare nel database
				 */
				for(int i=0; i<nonChiavi.size()-1;i++){
					String a = nonChiavi.get(i);
					query = "CREATE VIEW "+nomeNuovoDb+".countV"+name+a+" ("+listaDiChiavi+a+", counter) AS " +
					" SELECT "+listaDiChiavi+a+", count(*) FROM "+nameDB+"."+name+" GROUP BY "+listaDiChiavi+a;

					System.out.println("3: "+query);

					stmt.executeUpdate(query);
				}
				/*
				 * Creo una vista per tutti gli attributi della tabella considerata, eccetto che
				 * per la chiave e l'indicatore della sorgente, contenente per ogni chiave l'attributo
				 * che compare pi� volte. Se l'attributo che compare più volte è il valore null, lo considero
				 * solo nel caso in cui non ci siano attributi con valore diverso. A seguire, mi creo
				 * una vista dove elimino gli eventuali conflitti che si generano quando non c'� la maggioranza
				 * tra gli attributi, selezionando per ogni chiave l'attributo proveniente dalla sorgente con
				 * id pi� piccolo.
				 */
				for(int i=0; i<nonChiavi.size()-1;i++){
					String a = nonChiavi.get(i);
					query = "CREATE VIEW "+nomeNuovoDb+".V"+name+a+"2 ("+listaDiChiavi+a+") AS " +
					" SELECT "+listaDiChiavi+a+" FROM "+nomeNuovoDb+".countV"+name+a+" p WHERE NOT EXISTS ( " +
					" SELECT * FROM "+nomeNuovoDb+".countV"+name+a+" p1 WHERE "+andDiChiavi+"((p.counter < p1.counter " +
							" AND p1."+a+" IS NOT NULL) OR (p."+a+" IS NULL AND p1."+a+" IS NOT NULL)))";

					System.out.println("4: "+query);

					stmt.executeUpdate(query);

					query = "CREATE VIEW "+nomeNuovoDb+".V"+name+a+" ("+listaDiChiavi+a+") AS " +
					" SELECT v.* FROM "+nomeNuovoDb+".V"+name+a+"2 v, "+nameDB+"."+name+" p WHERE " +
							""+andDiChiaviConVista+"(v."+a+"=p."+a+" OR (v."+a+" IS NULL AND p."+a+" IS NULL)) " +
							"AND NOT EXISTS ( " +
								" SELECT * FROM "+nameDB+"."+name+" p1, "+nomeNuovoDb+".V"+name+a+"2 v1  " +
								" WHERE "+andDiChiavi+andDiChiaviConVista2+"(p1."+a+"=v1."+a+" OR (p1."+a+" IS NULL AND v1."+a+" IS NULL)) " +
								" AND (p1.src < p.src))";

					System.out.println("5: "+query);

					stmt.executeUpdate(query);
				}


				/*
				 *
				 */
				if(nonChiavi.size()>1){
					String attributiNonChiaviTable = "";
					String visteAttributiNonChiave = "";
					for(int i=0; i<nonChiavi.size()-2; i++){
						listaDiNonChiavi+=nonChiavi.get(i)+", ";
						attributiNonChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(i)+"."+nonChiavi.get(i)+", ";
						visteAttributiNonChiave+=nomeNuovoDb+".V"+name+nonChiavi.get(i)+", ";
					}
					listaDiNonChiavi+=nonChiavi.get(nonChiavi.size()-2);
					attributiNonChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(nonChiavi.size()-2)+"."+nonChiavi.get(nonChiavi.size()-2);
					visteAttributiNonChiave+=nomeNuovoDb+".V"+name+nonChiavi.get(nonChiavi.size()-2);

					String attributiChiaviTable = "";
					String andDiChiavi2 = "";
					for(int i=0; i<chiavi.size(); i++){
						attributiChiaviTable+=nomeNuovoDb+".V"+name+nonChiavi.get(0)+"."+chiavi.get(i)+", ";
					}

					for(int j=0; j<nonChiavi.size()-2; j++){
						for(int i=0;i<chiavi.size(); i++)
							andDiChiavi2+=nomeNuovoDb+".V"+name+nonChiavi.get(j)+"."+chiavi.get(i)+" = "+nomeNuovoDb+".V"+name+nonChiavi.get(j+1)+"."+chiavi.get(i)+" AND ";
					}
					andDiChiavi2 = andDiChiavi2.substring(0, andDiChiavi2.length()-4);
					query = "CREATE TABLE "+nomeNuovoDb+"."+name+" SELECT "+attributiChiaviTable+attributiNonChiaviTable+" FROM "+visteAttributiNonChiave+" WHERE "+andDiChiavi2;
					System.out.println("6: "+query);

					stmt.executeUpdate(query);

					for(int i=0; i<nonChiavi.size()-1;i++){
						query= "DROP VIEW "+nomeNuovoDb+".V"+name+nonChiavi.get(i);
						stmt.executeUpdate(query);
						System.out.println("7: "+query);

						query= "DROP VIEW "+nomeNuovoDb+".V"+name+nonChiavi.get(i)+"2";
						stmt.executeUpdate(query);
						System.out.println("8: "+query);


						query= "DROP VIEW "+nomeNuovoDb+".countV"+name+nonChiavi.get(i);
						stmt.executeUpdate(query);
						System.out.println("9: "+query);

					}
				}
				/*
				 * Per le tabelle associative composte da sole chiavi non devo fare alcuna operazione
				 * particolare, ma solo eliminare gli eventuali duplicati
				 */
				else{

					query = "CREATE TABLE "+nomeNuovoDb+"."+name+" SELECT "+listaDiChiavi.substring(0, listaDiChiavi.length()-2)+" FROM "+nameDB+"."+name+" GROUP BY "+listaDiChiavi.substring(0, listaDiChiavi.length()-2);

					System.out.println("10: "+query);
					stmt.executeUpdate(query);
				}
			}
			return "Creazione database integrato "+nomeNuovoDb+" effettuata con successo!";

		} catch (SQLException e) {
			e.printStackTrace();
			return "SQLException";
		}
	}
}
