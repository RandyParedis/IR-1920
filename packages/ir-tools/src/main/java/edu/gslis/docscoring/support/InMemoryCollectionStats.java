package edu.gslis.docscoring.support;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;


public class InMemoryCollectionStats extends CollectionStats {
	public static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.DOTALL);
	public static final int MIN_TERM_LENGTH = 1;
	
	
	protected Map<String,Long> tokFreqs;
	protected Map<String,Long> docFreqs;
	

	
	public void setStatSource(String statSource) {
		tokFreqs = new HashMap<String,Long>();
		docFreqs = new HashMap<String,Long>();

		
		try {
			System.err.print("reading vocab file...");
			Scanner scanner = new Scanner(new FileInputStream(new File(statSource)));
			String[] toks = SPACE_PATTERN.split(scanner.nextLine());
			
			// dumpindex header info
			tokCount = Double.parseDouble(toks[1]);
			docCount = Double.parseDouble(toks[2]);
			
			while(scanner.hasNextLine()) {
				toks = SPACE_PATTERN.split(scanner.nextLine());
				if(toks.length != 3)
					continue;
				
				String term = toks[0];
				long cf = Long.parseLong(toks[1]);
				long df = Long.parseLong(toks[2]);
				tokFreqs.put(term, cf);
				docFreqs.put(term, df);
			}
			scanner.close();
			
			System.err.println("done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public double termCount(String term) {
		if(! tokFreqs.containsKey(term))
			return 0.0;
		return tokFreqs.get(term);
	}
	@Override
	public double docCount(String term) {
		if(! docFreqs.containsKey(term))
			return 0.0;
		return docFreqs.get(term);
	}

}
