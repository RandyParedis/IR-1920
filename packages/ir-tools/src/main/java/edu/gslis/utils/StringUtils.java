package edu.gslis.utils;

public class StringUtils {
	public static final String SPACE_RETAINER = "XTGWQIHN";
	
	public static String cleanText(String text) {
		text = text.toLowerCase();
		text = text.replaceAll(" ", SPACE_RETAINER);
		text = text.replaceAll("\\W", SPACE_RETAINER);
		text = text.replaceAll(SPACE_RETAINER, " ").replaceAll(" +", " ");
		return text;
	}
	
	public static String removeSingleChars(String text) {
		StringBuilder b = new StringBuilder();
		String[] toks = text.split(" ");
		for(String tok : toks) {
			if(tok.length()<2)
				continue;
			b.append(tok + " ");
		}
		return b.toString().trim();
	}
}
