import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.util.List;
import java.util.ArrayList;

import javax.swing.border.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


public class MainGUI extends JPanel implements ActionListener {
	JButton inputButton, outputButton, compressButton, varMapButton;
	JTextPane log;
	JFileChooser fc, dc;

	ClassCompressor compressedCode;
	String inputPath;
	String outputPath;
	String fileName;
	String header = 
		"Current Chosen File: None" + newline +
		"Output Directory: None" + newline + 
		"-----------------------------------------------------------------" +
		newline + newline;

	static private final String newline = "\r\n";
	static private final String title = "Java Code Compressor V1.0";
	static private final String intro = 
		"Welcome to " + title + " !" + newline +
		"Please select your file and output directory, " + newline +
		"Then just press the Compress button below: " + newline +
		"-----------------------------------------------------------------" +
		newline;

	static private final Color classColor = Color.GREEN;
	static private final Color methodColor = Color.ORANGE;
	static private final Color varColor = Color.BLUE;
	static private final Color repColor = Color.RED;
	static private final Color textColor = Color.BLACK;


	public MainGUI() {
		super(new BorderLayout());

		//Buttons for action listening
		inputButton = new JButton("Choose a file...");
		outputButton = new JButton("Output to...");
		compressButton = new JButton("Compress!");
		varMapButton = new JButton("Export Var Map");
		inputButton.addActionListener(this);
		outputButton.addActionListener(this);
		compressButton.addActionListener(this);
		varMapButton.addActionListener(this);

		//Middle Log for displaying compressed varialbes
		log = new JTextPane();
		log.setBorder(new EmptyBorder(new Insets(10,10,10,10)));
		log.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		log.setMargin(new Insets(5, 5, 5, 5));
		JScrollPane logScrollPane = new JScrollPane(log);
		log.setText(intro+header);

        //File choosers for the buttons
        fc = new JFileChooser();
        dc = new JFileChooser();
        dc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //Add buttons to different panels for layout purpose
        JPanel topPanel = new JPanel();
        topPanel.add(inputButton);
        topPanel.add(outputButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(compressButton);
        bottomPanel.add(varMapButton);

        //Put together the main panel
        add(topPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.PAGE_END);
	}


	public void actionPerformed(ActionEvent e) {

		if (e.getSource()==inputButton) {
			int returnVal = fc.showOpenDialog(MainGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

            	//Get target file name and update input/output paths
                File file = fc.getSelectedFile();
                inputPath = file.getAbsoluteFile().getPath();
                fileName = getFileName(file);
                if (outputPath==null) {
                	outputPath = inputPath.substring(0, inputPath.length()-fileName.length()-1);
                }

                //Update header in the log
                header = "Current Chosen File: "+ fileName + newline +
					"Output Directory: " + outputPath + newline + 
					"-----------------------------------------------------------------" +
					newline + newline;
				log.setText(intro+header);

            } else {
                System.out.println("User cancelled choosing file...");
            }
		}

		else if (e.getSource()==outputButton) {
			int returnVal = dc.showOpenDialog(MainGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

            	//Read and update output path
                File absFile = dc.getSelectedFile().getAbsoluteFile();
                outputPath = absFile.getPath();

                //Update header in the log
                header = "Current Chosen File: "+ fileName + newline +
					"Output Directory: " + outputPath + newline + 
					"-----------------------------------------------------------------" +
					newline + newline;
				log.setText(intro+header);

            } else {
                System.out.println("User cancelled choosing directory...");
            }
		}

		else if (e.getSource()==compressButton) {
			//If no file selected, tell the user to select file first
			if (inputPath==null) {
				log.setText(intro+header+"Please select a file to compress first!");
				return;
			}

			//Output the compressed file
			List<String> codes = fileReader(inputPath);
			compressedCode = new ClassCompressor(codes);
			fileWriter(outputPath+"\\"+"compressed_"+fileName, compressedCode.compressed);

			//Write the varMap description to log
			log.setText(intro+header);
			addColoredVarMap(log, compressedCode);
		}

		else if (e.getSource()==varMapButton) {
			//Output the varMap description
			if (compressedCode==null) {
				log.setText(intro+header+"Please compress a file first!");
			}
			else {
				List<String> varMapDescrip = compressedCode.getVarMapDescription();
				fileWriter(outputPath+"\\"+"varMap_"+fileName, varMapDescrip);
				log.setText(intro+header+"VarMap exported Successfully!");
			}
		}
	}


	private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }


    private void addColoredVarMap(JTextPane tp, ClassCompressor compressed) {
    	appendToPane(tp, "Class: ", textColor);
    	appendToPane(tp, compressed.className + newline, classColor);

		for (String method : compressed.methods.keySet()) {
			MethodCompressor curr = compressed.methods.get(method);
			appendToPane(tp, "    " + "Method: ", textColor);
			appendToPane(tp, method + newline, methodColor);
			for (String variable : curr.varMap.keySet()) {
				appendToPane(tp, "	" + "Variable ", textColor);
				appendToPane(tp, variable + newline, varColor);
				appendToPane(tp, "	" + "replaced by: ", textColor);
				appendToPane(tp, curr.varMap.get(variable) + newline, repColor);
			}
			appendToPane(tp, newline, textColor);
		}
    }


	private String getFileName(File f) {
		String path = f.getAbsoluteFile().getPath();
		int i = path.length()-1;
		while (i>0 && path.charAt(i)!='\\') i--;
		if (path.charAt(i)!='\\') {
			throw new IllegalArgumentException("Invalid path name: "+path);
		}
		return path.substring(i+1, path.length());
	}


	private List<String> fileReader(String address) {
		List<String> file = new ArrayList<> ();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(address));
			String line = reader.readLine();
			while (line!=null) {
				if (line.length()>0) file.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}


	private void fileWriter(String address, List<String> content) {
		try {
			File file = new File(address);
			if (!file.exists()) file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (String line : content) {
				writer.write(line);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static void createGUI() {
		//Create and set up the window.
        JFrame frame = new JFrame("Code Compressor V1.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Add content to the window.
        frame.add(new MainGUI());
 
        //Display the window.
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(500,500);
        //frame.pack();
		frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        frame.setVisible(true);
	}


    public static void main(String[] args) {
	    //Schedule a job for the event dispatch thread:
	    //creating and showing this application's GUI.
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	            //Turn off metal's use of bold fonts
	            UIManager.put("swing.boldMetal", Boolean.FALSE); 
	            createGUI();
	        }
	    });
	}
}