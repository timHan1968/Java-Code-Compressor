import java.util.*;
import java.io.*;

class ClassCompressor{

	String className;
	List<String> original;
	List<String> compressed;
	List<String> fields;
	Map<String, ClassCompressor> subClasses;
	Map<String, MethodCompressor> methods;

	ClassCompressor(List<String> lines) {
		original = lines;
		fields = new ArrayList<> ();
		subClasses = new HashMap<> ();
		methods = new HashMap<> ();
		compressed = compress(lines);
	}

	private List<String> compress(List<String> lines) {
		Boolean inComment = false;
		List<String> compressedLines = new ArrayList<> ();
		Stack<Character> stack = new Stack<> ();

		int i=0;
		while(i<lines.size()) {
			String line = lines.get(i++);
			//System.out.println("Current line: "+line);

			//Case1: Comment/empty lines:
			if (shouldSkip(line)) continue;
			String first = getFirstToken(line);
			String signFirst = (first.length()>1) ? first.substring(0,2) : " ";
			String signLast = getSignLast(line);
			if (signFirst=="/*") inComment = true;
			if (signLast=="*/") {
				inComment = false;
				continue;
			}
			if (inComment) continue;

			//Case2: Import statements
			if (first.equals("import")) {
				compressedLines.add(line);
				continue;
			}

			//Case3: Class declaration
			if (isClassDeclaration(line)) {
				String name = getClassName(line);
				//Not subclass
				if (className==null) {
					className=name;
					compressedLines.add(line);
					continue;
				}
				//Is subClass
				List<String> subClassLines = new ArrayList<> ();
				subClassLines.add(line);
				stack.push('{');
				while (!stack.isEmpty()) {
					line = lines.get(i++);
					if (shouldSkip(line)) continue;
					List<Character> parenthes = searchParenth(line);
					for (char parenth : parenthes) {
						if (parenth=='{') {
							stack.push(parenth);
						} 
						else if(parenth=='}') {
							if (stack.peek()!='{') {
								throw new IllegalStateException("Illegal subClass brackets!");
							}
							stack.pop();
						}
					}
					subClassLines.add(line);
				}
				ClassCompressor subClass = new ClassCompressor(subClassLines);
				subClasses.put(name, subClass);
				for (String compressedLine : subClass.compressed) {
					compressedLines.add(compressedLine);
				}
				continue;
			}

			//Case 4: Method declaration
			List<Character> parenthes = searchParenth(line);
			if (parenthes.size()>0 && parenthes.get(0)=='{') {
				String name = getMethodName(line);
				List<String> methodLines = new ArrayList<> ();
				methodLines.add(line);
				for (char parenth : parenthes) {
					stack.push(parenth);
				}
				while (!stack.isEmpty()) {
					line = lines.get(i++);
					if (shouldSkip(line)) continue;
					parenthes = searchParenth(line);
					for (char parenth : parenthes) {
						if (parenth=='{') {
							stack.push(parenth);
						} 
						else if(parenth=='}') {
							if (stack.peek()!='{') {
								throw new IllegalStateException("Illegal mehtod brackets!");
							}
							stack.pop();
						}
					}
					methodLines.add(line);
				}
				// System.out.println("Method codes: ");
				// for (String mline : methodLines) {
				// 	System.out.println(mline);
				// }

				MethodCompressor method = new MethodCompressor(methodLines);
				methods.put(name, method);
				for (String compressedLine : method.compressed) {
					compressedLines.add(compressedLine);
				}
				continue;
			}

			//Case 5: Class fields
			fields.add(line);
			compressedLines.add(line);

		}
		return compressedLines;
	}


	/* 
	 * Return whether the given [line] is a comment or empty. 
	 * Assuming the code follows java comment style.
	 */
	private static boolean shouldSkip(String line) {
		int i=0;
		while (i<line.length() && (line.charAt(i)==' ' || line.charAt(i)=='	')) {
			i++;
		}
		if (i==line.length()) return true; //Line is empty

		//Line is a comment:
		if (line.charAt(i)=='/' && i+1<line.length() && 
			line.charAt(i+1)=='/') return true;

		return false;
	}


	/* Method to get the last two non-space chars (to check if the line indicates
	 * the end of a comment block).
	 */
	private static String getSignLast(String line) {
		int j = line.length()-1;
		while (j>0 && line.charAt(j)==' ') j--;
		if (j<1) return " ";
		return line.substring(j-1, j+1);
	}


	/*Return whether the given line is the start of a class declaration.*/
	private static boolean isClassDeclaration(String line) {
		int i = line.indexOf("class ");
		if (i==-1) return false;
		if (i>0 && line.charAt(i-1)!=' ' && line.charAt(i-1)!='	') {
			return false;
		}
		int numQuotations = 0;
		while (i>=0) {
			if (line.charAt(i)=='\"') numQuotations++;
			i--;
		}
		boolean ans = (numQuotations%2 == 0) ? true : false;
		return ans;
	}


	/*Return the first token of a given [line].*/
	private static String getFirstToken(String line) {
		int i=0;
		while(line.charAt(i)==' ' || line.charAt(i)=='	') i++;
		int j=i;
		while(j<line.length() && line.charAt(j)!=' ' && line.charAt(j)!='	') j++;

		return line.substring(i,j);
	}


	/*Return the name of the class, assuming [line] contains it.*/
	private static String getClassName(String line) {
		int j=line.length()-1;
		while (j>0 && line.charAt(j)!='{') j--;
		if (j==0) {
			throw new IllegalArgumentException("Line contains no class name!");
		}
		while(j>0 && line.charAt(j-1)==' ') j--;
		int i=j-1;
		while(i>0 && line.charAt(i-1)!=' ') i--;
		return line.substring(i,j);
	}


	/*Return the name of the method, assuming [line] contains it.*/
	private static String getMethodName(String line) {
		int j=line.length()-1;
		while (j>0 && line.charAt(j)!='(') j--;
		if (j==0) {
			throw new IllegalArgumentException("Line contains no method name!");
		}
		while(j>0 && line.charAt(j-1)==' ') j--;
		int i=j-1;
		while(i>0 && line.charAt(i-1)!=' ') i--;
		return line.substring(i,j);
	}


	/*
 	* The method searches for large bracket NOT in a string;
 	* If exists, return the bracket;
 	* Else, return null.
 	*/
	private static List<Character> searchParenth(String line) {
		int i=0;
		List<Character> result=new ArrayList<> ();
		boolean inString=false, inChar=false;
		while(i<line.length()){
			char curr=line.charAt(i);
			if (curr=='\"' && !inChar) inString = !inString;
			if (curr=='\'' && !inString) inChar = !inChar;
			if ((curr=='{' || curr=='}') && !inString && !inChar) result.add(curr);
			i++;
		}
		return result;
	}


	/*
 	* The method returns a map from variables to its abbreviations
 	* in the form of a String List.
 	*/
 	public List<String> getVarMapDescription() {
 		List<String> description = new ArrayList<> ();
 		description.add("Class: " + className);
		for (String method : methods.keySet()) {
			MethodCompressor curr = methods.get(method);
			description.add("    " + "Method: " + method);
			for (String variable : curr.varMap.keySet()) {
				description.add("	" + "Variable \"" + variable + "\"");
				description.add("	" + "replaced by: \"" + curr.varMap.get(variable) + "\"");
			}
			description.add("");
		}
		return description;
 	}


	// public static List<String> fileReader(String address) {
	// 	List<String> file = new ArrayList<> ();
	// 	try {
	// 		BufferedReader reader = new BufferedReader(new FileReader(address));
	// 		String line = reader.readLine();
	// 		while (line!=null) {
	// 			if (line.length()>0) file.add(line);
	// 			line = reader.readLine();
	// 		}
	// 		reader.close();
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// 	return file;
	// }


	// public static void fileWriter(String address, List<String> content) {
	// 	try {
	// 		File file = new File(address);
	// 		if (!file.exists()) file.createNewFile();
	// 		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	// 		for (String line : content) {
	// 			writer.write(line);
	// 			writer.newLine();
	// 		}
	// 		writer.close();
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// }


	public static void main(String[] args) {
		// String s1 = "	private static char searchParenth(String line) {";
		// String s2 = "		}";
		// String s3 = "	private static char searchParenth  (String line) {";
		// String s4 = "			if ((curr=='{' || curr=='}') && !inString && !inChar) return curr;";
		// String s5 = "		int i=0;";
		// String s6 = "		String example = \"private static char searchParenth(String line) {\"";

		// System.out.println(searchParenth(s1));
		// System.out.println(searchParenth(s2));
		// System.out.println(searchParenth(s3));
		// System.out.println(searchParenth(s4));
		// System.out.println(searchParenth(s5));

		// List<String> file = fileReader("sample.txt");
		// ClassCompressor sample = new ClassCompressor(file);
		// fileWriter("compressed_sample.txt", sample.compressed);

		// System.out.println("Class: " + sample.className);
		// for (String method : sample.methods.keySet()) {
		// 	MethodCompressor curr = sample.methods.get(method);
		// 	System.out.println("  " + "Method: " + method);
		// 	for (String variable : curr.varMap.keySet()) {
		// 		System.out.println("	" + "Variable \"" + variable + "\"");
		// 		System.out.println("	" + "replaced by: \"" + curr.varMap.get(variable) + "\"");
		// 	}
		// 	System.out.println("");
		// }

	}
}