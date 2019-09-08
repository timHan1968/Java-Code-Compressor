import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

//Need to do:
//	- Add ways to configure class variables (static + non-static)
class MethodCompressor{

	Map<String, String> varMap;
	static final Set<String> varTypes = new HashSet<> (Arrays.asList("int",
		"byte", "short", "long", "float", "double", "boolean", "char", 
		"Boolean", "Character", "Integer", "String"));
	List<String> compressed;


	MethodCompressor(List<String> lines) {
		varMap = new HashMap<> ();
		compressed = compress(lines);
	}


	/*Method that performs the compression on given [lines] of codes*/
	private List<String> compress(List<String> lines) {
		List<String> compressed = compressSpaces(lines);
		compressed = compressVariables(compressed);
		return compressed;
	}


	/*Method that compresses by deleting unnecessary spaces*/
	private List<String> compressSpaces(List<String> lines) {
		List<String> compressedLines = new ArrayList<> ();
		for (String line : lines) {
			compressedLines.add(compressSpacesLine(line));
		}
		return compressedLines;
	}


	/*Helper method for [compressSpaces]*/
	private String compressSpacesLine(String line) {
		//System.out.println("Line: " + line);
		int startIdx=0;
		char curr=line.charAt(startIdx);
		while(startIdx<line.length() && (curr==' ' || curr=='	')) {
			curr=line.charAt(++startIdx); //Skip all beginning spaces/tabs
		}
		StringBuilder compressedLine = new StringBuilder();
		boolean inString = false;

		for (int i=startIdx; i<line.length(); i++) {
			curr = line.charAt(i);
			if (curr=='\"') inString = !inString;
			//Skip comments
			String commentSign = "";
			if (i<line.length()-1) commentSign = line.substring(i, i+2);
			if ((commentSign.equals("/*") || commentSign.equals("//")) && !inString) break;
			//Copy non-space chars and chars in a string
			if (curr!=' ' || inString) {
				compressedLine.append(curr);
				continue;
			}
			//Case1: Space in the end of line
			if (i+1==line.length()) continue;
			char next = line.charAt(i+1);
			char prev = line.charAt(i-1);
			//Case2: Continuous spaces
			if (next==' ') continue;
			//Case3: Space before/after '<' and before '>'
			if (next=='<' || next=='>' || prev=='<') continue;
			//Case4: Space before/after '[' and before ']'
			if (next=='[' || next==']' || prev=='[') continue;
			//Case5: Before/after ',' or '='
			if (next==',' || next=='=' || prev==',' || prev=='=') continue;
			//Case6: Before/after '|' or '&'
			if (next=='|' || next=='&' || prev=='|' || prev=='&') continue;

			compressedLine.append(curr);
		}
		return compressedLine.toString();
	}


	/*Method that compresses by replacing each variable by a single char*/
	public List<String> compressVariables(List<String> lines) {
		//Configue input variables
		String firstLine = lines.get(0);
		int i=0;
		while (firstLine.charAt(i)!='(') i++;

		char replacement = 'a';
		while (firstLine.charAt(i)!=')') {
			i=getNextToken(firstLine, i++);
			int j=i+1;
			while (firstLine.charAt(j)!=',' && firstLine.charAt(j)!=')') j++;
			varMap.put(firstLine.substring(i+1,j), Character.toString(replacement));
			//System.out.println("variable "+firstLine.substring(i+1,j) + " replaced by "+replacement);
			replacement++;
			i=j;
		}

		//Configure rest of the variables
		for (int k=1; k<lines.size(); k++) {
			replacement=gatherVars(lines.get(k), replacement);
		}

		//Replace all vars in [varMap]
		List<String> compressedLines = new ArrayList<> ();
		for (String line : lines) {
			//System.out.println("Line: "+line);
			String compressedLine = replaceVar(line);
			//System.out.println("compressedLine: "+compressedLine);
			compressedLines.add(compressedLine);
		}
		return compressedLines;
	}


	/*Helper method for [compressVariables]*/
	private char gatherVars(String s, char replacement) {
		//System.out.println("Current line: " + s);
		int i=0, j=0;
		while (i<s.length()) {
			while (i<s.length() && (s.charAt(i)==' ' || s.charAt(i)=='	')) i++;
			if (i==s.length()) break;
			j=getNextToken(s, i);

			String token = s.substring(i,j);
			if (isVarKey(token)) {
				replacement = getVarNames(s, j+1, replacement);
				return replacement;
			}
			i=j;
		}
		return replacement;
	}


	/*Helper method for [gatherVars]*/
	private boolean isVarKey(String token) {
		if (varTypes.contains(token)) return true;
		boolean isArray = Pattern.matches(".*\\[\\]", token);
		boolean isNonPrim = Pattern.matches(".*<.*>", token);
		boolean isForInt = token.equals("(int");
		return isArray || isNonPrim || isForInt;
	}


	/*Helper method for [gatherVars]*/
	private char getVarNames(String s, int idx, char replacement) {
		// System.out.println("Line: "+s);
		// System.out.println("Start char: " + s.charAt(idx));
		int i=idx, j=i+1;
		char stopper=' ';
		while(stopper!=';') {
			//Reach a colon or comma which is always after a varName
			boolean inParenth=false;
			boolean inSharpParenth=false;
			boolean inLargeParenth=false;
			boolean inString=false;
			boolean inChar=false;
			char curr = s.charAt(j);
			while(j<s.length()) {
				//Ensure not in a String/Char (for colon AND comma) or Paranthesis (for comma)
				if (curr=='(' && !inString && !inChar) inParenth = true;
				if (curr==')' && !inString && !inChar) inParenth = false;
				if (curr=='<' && !inString && !inChar) inSharpParenth = true;
				if (curr=='>' && !inString && !inChar) inSharpParenth = false;
				if (curr=='{' && !inString && !inChar) inLargeParenth = true;
				if (curr=='}' && !inString && !inChar) inLargeParenth = false;
				if (curr=='\"' && !inChar) inString = !inString;
				if (curr=='\'' && !inString) inChar = !inChar;
				//Reach the desired stopper
				//System.out.println("Curr: "+curr);
				if (curr==';' && !inString && !inChar) break;
				if (curr==':' && !inString && !inChar) break;
				if (curr==',' && !inParenth && !inSharpParenth && !inLargeParenth && !inString && !inChar) break;
				curr = s.charAt(++j);
			}

			//Trace back to find the varName
			int k=j;
			inString=false;
			inChar=false;
			while (k>i) {
				if (s.charAt(--k)=='\"'  && !inChar) inString = !inString;
				if (s.charAt(k)=='\'' && !inString) inChar = !inChar;
				if (s.charAt(k)=='=' && !inString && !inChar) break;
			}
			String varName = (s.charAt(k)=='=') ? s.substring(i,k) : s.substring(i,j);
			if (curr==':') varName = s.substring(i,j-1); //For case like "for (int num : nums)"
			varMap.put(varName, Character.toString(replacement));
			//System.out.println("variable "+varName + " replaced by "+replacement);
			replacement++;
			if (curr==':') return replacement;

			//update pointers
			i=j+1;
			stopper=s.charAt(j);
			j=i+1;
		}
		return replacement;
	}


	/*Method that replaces variables in a string by its relacement char*/
	private String replaceVar(String line) {
		StringBuilder replaced = new StringBuilder();
		Set<Character> initials = new HashSet<> ();
		for (String key : varMap.keySet()) {
			initials.add(key.charAt(0));
		}

		int i=0;
		boolean inString = false;
		while (i<line.length()) {
			//System.out.println("Current char: " + line.charAt(i));
			char pre = (i>0) ? line.charAt(i-1) : 0;
			char curr = line.charAt(i);
			if (curr=='\"') inString = !inString;
			if (Character.isLetter(pre) || !initials.contains(curr) || inString) {
				//Not target, just copy the char
				replaced.append(curr);
				i++;
			}
			else {
				boolean findTarget = false;
				for (String key : varMap.keySet()) {
					int len = key.length();
					if (curr!=key.charAt(0) || i+len>line.length()) continue;
					if (i+len<line.length() && Character.isLetter(line.charAt(i+len))) continue;

					String candidate = line.substring(i, i+len);
					if (candidate.equals(key)) {
						//Is target, repalce the substring
						findTarget = true;
						replaced.append(varMap.get(key));
						i+=len;
						break;
					}
				}
				if (!findTarget) {
					replaced.append(curr);
					i++;
				}
			}
		}
		return replaced.toString();
	}


	/*General helper method*/
	private int getNextToken(String s, int idx) {
		boolean isInString =s.charAt(idx)=='\"';
		int j=idx+1;
		if (isInString) {
			while (j<s.length() && s.charAt(j)!='\"') j++;
			j++;
		}
		else {
			while(j<s.length() && s.charAt(j)!=' ') j++;
		}
		return j;
	}


	public static void main(String[] args) {
		// Map<String, String> varMap = new HashMap<> ();
		// varMap.put("s", "a");
		// varMap.put("targets", "c");
		// varMap.put("inWin", "d");
		// varMap.put("i", "e");
		// varMap.put("j", "f");
		// varMap.put("curI", "j");
		// varMap.put("curJ", "k");

		// System.out.println(replaceVar("char curJ = s.charAt(j);", varMap));
		// System.out.println(replaceVar("while(inWin.size()<t.length() && j<s.length()) {", varMap));
		// System.out.println(replaceVar("int i=0, j=0, ansI=i, ansJ=j;", varMap));
		// System.out.println(replaceVar("boolean canExpand=true;", varMap));
		// System.out.println(replaceVar("String dummy = \"targets = inWin = i = j\"", varMap));
		// System.out.println(replaceVar("String stringDummy;", varMap));



		// String l1 = "	private static char gatherVars(String s, char replacement) {";
		// String l2 = "		int i=0, j=0;";
		// String l3 = "		while (i<s.length()) {";
		// String l4 = "			while (i<s.length() && s.charAt(i)==' ' || s.charAt(i)=='	') i++;";
		// String l5 = "			if (i==s.length()) break;";
		// String l6 = "			j=getNextToken(s, i);";
		// String l7 = "			String token = s.substring(i,j);";
		// String l8 = "			if (isVarKey(token)) {";
		// String l9 = "				replacement = getVarNames(s, j+1, replacement);";
		// String l10 = "			}";
		// String l11 = "			i=j;";
		// String l12 = "		}";
		// String l13 = "		return replacement;";
		// String l14 = "	}";

		// List<String> lines = Arrays.asList(l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14);
		// lines = compress(lines);
		// for (String line : lines) {
		// 	System.out.println(line);
		// }

		// for (String key : varMap.keySet()) {
		// 	System.out.println(key + " " + varMap.get(key));
		// }
		// String s = "boolean isArray=Pattern.matches(\".*\\[\\]\",token);";
		// System.out.println(getVarNames(s, 8, 'b'));


		// String s = "        boolean isArray = Pattern.matches(\".*\\[\\]\", word);";
		// int i=0, j=0;
		// while (i<s.length()) {
		// 	while (i<s.length() && s.charAt(i)==' ' || s.charAt(i)=='	') i++;
		// 	if (i==s.length()) break;
		// 	j=getNextWord(s, i);

		// 	System.out.println(s.substring(i, j));
		// 	i=j;
		// }


		// String s2 = "int len=3;";
		// String s3 = "String name;";
		// String s4 = "boolean isTrue=false,isFalse=true;";
		// String s5 = "int ptr1,ptr2,ptr3;";
		// String s6 = "for (int i=1; i<len; i++)";
		// String s7 = "for (int i=1; i<len; i++)";
		// System.out.println(getVarNames(s2, 4, 'a'));
		// System.out.println(getVarNames(s3, 7, 'a'));
		// System.out.println(getVarNames(s4, 8, 'a'));
		// System.out.println(getVarNames(s5, 4, 'a'));
		// System.out.println(getVarNames(s6, 9, 'a'));


		// String s1 = "	private static int getNextToken(String s, int idx) {";
		// String s2 = "		boolean isInString =s.charAt(idx)=='\"';";
		// String s3 = "		Map < String, Integer > map =  new HashMap<> ();  ";
		// String s4 = "		String s = \"Map < String, Integer > map =  new HashMap<> ();  \"";

		// System.out.println(compressSpacesLine(s1));
		// System.out.println(compressSpacesLine(s2));
		// System.out.println(compressSpacesLine(s3));
		// System.out.println(compressSpacesLine(s4));
		
		// String l1 = "    return isPrime(a);      /* works only for odd a */";
		// String l2 = "    return false;          // Explain why here.";
		// System.out.println(compressSpacesLine(l1));
		// System.out.println(compressSpacesLine(l2));
	}

}

