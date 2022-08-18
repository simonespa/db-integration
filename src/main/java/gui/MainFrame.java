package gui;

import gui.event.CorrectTableModel;
import gui.event.IncorrectTableModel;
import gui.event.ListDBRiconciliati;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.SoftBevelBorder;

public class MainFrame extends JFrame  {

		CorrectTableModel ctm;
		IncorrectTableModel itm;
		JTable correctTable, incorrectTable;
		JTextArea queryField;

		ListDBRiconciliati lm;
		String dbName = ""; String operatore = "";

		ListDB dbL;

		private JMenuBar menuBar; // barra dei menù
		private JMenu mnuFile; // menu file
		private JMenu mnuTab; // menu tab
		private JMenu mnuConsole; // menu console
		private JTabbedPane jtp;
//		private EventListner event;

		public MainFrame() {
			super("Prova");
			setSize(500, 400);

			jtp = new JTabbedPane();

			dbL = new ListDB();

			lm = new ListDBRiconciliati();

			setComponent();
			initMenu();

			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = dim.width / 2;
			int y = dim.height / 2;

			setLocation(x - x / 2, y - y / 2);

			getContentPane().add(jtp);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setVisible(true);
		}

		private void setComponent() {

			/*
			 * Tab di riconciliazione
			 */
			JPanel pRiconciliazione = new JPanel(new BorderLayout());


			JButton creaRiconciliato = new JButton("Crea Database Riconciliato");
			JPanel b = new JPanel();
			b.add(creaRiconciliato);

			creaRiconciliato.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Vector<String> v = new Vector<String>();
					int [] indici = dbL.list.getSelectedIndices();
					for(int i =0; i<indici.length; i++){
						v.add((String)dbL.model.getElementAt(indici[i]));
					}
					dbL.creaRiconciliato(v);
					lm.setListRiconciliati();
				}
			});

			dbL.add(b, BorderLayout.CENTER);

			pRiconciliazione.add(dbL, BorderLayout.NORTH);
			pRiconciliazione.add(dbL.result, BorderLayout.CENTER);



			jtp.addTab("Riconciliazione", pRiconciliazione);


			/*
			 * Tab d'integrazione
			 */
			JPanel pIntegrazione = new JPanel(new BorderLayout());

			pIntegrazione.add(lm, BorderLayout.NORTH);
			pIntegrazione.add(lm.result, BorderLayout.CENTER);


			jtp.addTab("Integrazione", pIntegrazione);

			/*
			 * In questo tab l'utente inserisce la query da eseguire e ne visualizza
			 * il risultato in forma tabellare
			 */
			itm = new IncorrectTableModel();
			ctm = new CorrectTableModel(itm);
			correctTable = new JTable(ctm);
			incorrectTable = new JTable(itm);

			JPanel p3 = new JPanel(new BorderLayout());
			p3.add(new JLabel("Enter your query: "), BorderLayout.WEST);
			queryField = new JTextArea();
			// JButton jb = new JButton(new ImageIcon(MainFrame.class.getResource("ico/execute.png")));
			JButton jb = new JButton();
			jb.transferFocusBackward();
			jb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String name = lm.tt.dbName;
					if(name.equals(""))
						ctm.setQuery(queryField.getText().trim(), name, 0);
					else
						ctm.setQuery(queryField.getText().trim(), name, 1);
				}
			} );

			p3.add(jb, BorderLayout.EAST);

			JScrollPane areaScrollPane = new JScrollPane(queryField);
			areaScrollPane.setBorder(new CompoundBorder(new MatteBorder(3,3,3,3,Color.LIGHT_GRAY),new SoftBevelBorder(BevelBorder.RAISED)));

			p3.add(areaScrollPane, BorderLayout.CENTER);

			JTabbedPane tabCorrect = new JTabbedPane();
			JScrollPane resultCorrect = new JScrollPane(correctTable);
			tabCorrect.add("Tuple certe", resultCorrect);

			JTabbedPane tabIncorrect = new JTabbedPane();
			JScrollPane resultIncorrect = new JScrollPane(incorrectTable);
			tabIncorrect.add("Tuple incerte", resultIncorrect);

			JSplitPane splitPane3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabCorrect, tabIncorrect);
			splitPane3.setDividerLocation(0.5);
			splitPane3.setResizeWeight(0.5);
			splitPane3.setOneTouchExpandable(true);
			splitPane3.setContinuousLayout(true);

			// crea un pannello che contiene la gerarchia dei db
			JScrollPane treeScrollPane = new JScrollPane(lm.tt);

			JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane3, treeScrollPane);
			splitPane2.setDividerLocation(0.5);
			splitPane2.setResizeWeight(0.8);
			splitPane2.setOneTouchExpandable(true);
			splitPane2.setContinuousLayout(true);

			// Crea uno SplitPane verticale con i due pannelli al suo interno
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, p3, splitPane2);
			// Imposta la locazione del divisore

			splitPane.setResizeWeight(0.05);

			splitPane.setContinuousLayout(true);

			JScrollPane resultQuery = new JScrollPane(ctm.resultQuery);

			JSplitPane finish = new JSplitPane(JSplitPane.VERTICAL_SPLIT,splitPane,resultQuery);
			finish.setResizeWeight(0.98);
			// aggiunge lo SplitPane al frame principale
			jtp.addTab("Query", finish);

			menuBar = new JMenuBar();

		}
		private void initMenu() {
			/*event = new EventListner(verticalSplit, menuBar, toolBarFile,
					toolBarTab, toolBarConsole);
	*/
			// Istanzia gli item per i menu "File", "Tab" e "Console", setta alcune
			// proprietà
			// registra i componenti all'ascoltatore "event"
			JMenuItem openFile = new JMenuItem("Open...");
			openFile.setToolTipText("Apri - apre il file di testo specificato");
			openFile.setMnemonic('O');
		/*	openFile.setIcon(new ImageIcon(Editor.class
					.getResource("ico/openFile16.png")));*/
		//	openFile.addActionListener(event);
			JMenuItem newFile = new JMenuItem("New");
			newFile.setToolTipText("Nuovo - crea un nuovo file di testo");
			newFile.setMnemonic('N');
	/*		newFile.setIcon(new ImageIcon(Editor.class
					.getResource("ico/newFile16.png")));*/
		//	newFile.addActionListener(event);
			JMenuItem saveFile = new JMenuItem("Save");
			saveFile.setToolTipText("Salva - salva il file corrente");
			saveFile.setMnemonic('S');
		/*	saveFile.setIcon(new ImageIcon(Editor.class
					.getResource("ico/saveFile16.png")));*/
			saveFile.setEnabled(false);
		//	saveFile.addActionListener(event);
			JMenuItem doParsing = new JMenuItem("Execute Query");
			doParsing
					.setToolTipText("Execute - avvia l'esecuzione della query");
			doParsing.setMnemonic('Q');
		/*	doParsing.setIcon(new ImageIcon(Editor.class
					.getResource("ico/doParsing16.png")));*/
		//	doParsing.addActionListener(event);
			JMenuItem exit = new JMenuItem("Exit");
			exit.setToolTipText("Esci - chiude il programma");
			exit.setMnemonic('E');
		//	exit.setIcon(new ImageIcon(Editor.class.getResource("ico/exit16.png")));
		//	exit.addActionListener(event);

			JMenuItem closeTab = new JMenuItem("Close Tab");
			closeTab.setToolTipText("Chiudi scheda - chiude la scheda corrente");
			closeTab.setMnemonic('T');
		/*	closeTab.setIcon(new ImageIcon(Editor.class
					.getResource("ico/closeTab16.png")));*/
			closeTab.setEnabled(false);
		//	closeTab.addActionListener(event);
			JMenuItem closeOther = new JMenuItem("Close Other");
			closeOther
					.setToolTipText("Chiudi altre - chiude le altre schede tranne quella corrente");
			closeOther.setMnemonic('O');
		/*	closeOther.setIcon(new ImageIcon(Editor.class
					.getResource("ico/closeOther16.png")));*/
			closeOther.setEnabled(false);
		//	closeOther.addActionListener(event);
			JMenuItem closeAll = new JMenuItem("Close All");
			closeAll.setToolTipText("Chiudi tutto - chiude tutte le schede");
			closeAll.setMnemonic('A');
	/*		closeAll.setIcon(new ImageIcon(Editor.class
					.getResource("ico/closeAll16.png")));*/
			closeAll.setEnabled(false);
		//	closeAll.addActionListener(event);

			JMenuItem clearConsole = new JMenuItem("Clear");
			clearConsole
					.setToolTipText("Pulisci - pulisce lo schermo della console");
			clearConsole.setMnemonic('r');
		/*	clearConsole.setIcon(new ImageIcon(Editor.class
					.getResource("ico/clearConsole16.png")));*/
			clearConsole.setEnabled(false);
		//	clearConsole.addActionListener(event);

			// Istanzia i menu
			mnuFile = new JMenu("File");
			mnuFile.setMnemonic('F');
			mnuTab = new JMenu("Tab");
			mnuTab.setMnemonic('T');
			mnuConsole = new JMenu("Console");
			mnuConsole.setMnemonic('C');

			// Aggiunge gli item ai menu
			mnuFile.add(openFile);
			mnuFile.add(newFile);
			mnuFile.add(saveFile);
			mnuFile.addSeparator();
			mnuFile.add(doParsing);
			mnuFile.addSeparator();
			mnuFile.add(exit);

			mnuTab.add(closeTab);
			mnuTab.add(closeOther);
			mnuTab.add(closeAll);

			mnuConsole.add(clearConsole);

			// Istanzia il Menu Bar e gli aggiunge i menu
			menuBar.add(mnuFile);
			menuBar.add(mnuTab);
			menuBar.add(mnuConsole);

			// Aggiunge il Menu Bar alla finestra principale
			setJMenuBar(menuBar);
		}

	public static void main(String[] args) {
		new MainFrame();
	}

}
