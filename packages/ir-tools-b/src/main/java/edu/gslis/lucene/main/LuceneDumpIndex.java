package edu.gslis.lucene.main;

import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.TreeBag;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import edu.gslis.indexes.IndexWrapperLuceneImpl;
import edu.gslis.lucene.indexer.Indexer;
import edu.gslis.textrepresentation.FeatureVector;




public class LuceneDumpIndex 
{
 
    public static void main(String[] args) throws Exception {

        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse( options, args);
        
        if (args.length == 0 || cl.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "dumpindex", options );
            return;
        }
        
        // Path to the index
        String path = cl.getOptionValue("index"); 
        // Field that we're searching
        String field = cl.getOptionValue("field", null);
        // Field containing the document identifier
        String docno = cl.getOptionValue("docno", "docno");
        // Command
        String cmd = cl.getOptionValue("cmd");
        String arg = cl.getOptionValue("arg");

        IndexWrapperLuceneImpl index = new IndexWrapperLuceneImpl(path);
        IndexReader lucene = (IndexReader)index.getActualIndex();
        if (cmd.equals("documentid") || cmd.equals("di")) {
            // Return the document ID given the specified field/value
            // Default to docno, if not specified
            if (field == null)
                System.out.println(index.getDocId(arg));
            else
                System.out.println(index.getDocId(field, arg));
        }
        else if (cmd.equals("stats") || cmd.equals("s")) {
            
            Fields fields = MultiFields.getFields(lucene); 
            System.out.println("Documents: \t" + (long)index.docCount());
            long vocabSize = (long)index.termTypeCount();
            if (vocabSize > 0)
                System.out.println("Unique terms: \t" + vocabSize);
            else
                System.out.println("Unique terms: \t Not supported by codec.");
            System.out.println("Total terms: \t" + (long)index.termCount());
            
            String fieldNames = "";
            Iterator<String> it = fields.iterator();
            while (it.hasNext()) {
                String fieldName = it.next();
                fieldNames +=  fieldName + " ";
            }

            System.out.println("Fields: \t" + fieldNames);
        }
        else if (cmd.equals("documenttext") || cmd.equals("dt")) {
            int docId = index.getDocId(docno, arg);
            if (!StringUtils.isEmpty(field))
                System.out.println(index.getDocText(docId, field));
            else
                System.out.println(index.getDocText(docId));
                
       }
       else if (cmd.equals("dl")) {
             int docId = index.getDocId(docno, arg);             
             double len = index.getDocLength(docId);
             System.out.println(len);
       }
       else if (cmd.equals("documentvector") || cmd.equals("dv")) {
            int docId = index.getDocId(docno, arg);
            FeatureVector fv;
            if (!StringUtils.isEmpty(field)) 
                fv = index.getDocVector(docId, field, null);
            else
                fv = index.getDocVector(docId, null);
            Iterator<String> it = fv.iterator();
            System.out.println("Feature count: " + fv.getFeatureCount());
            System.out.println("Length " + fv.getLength());
            while (it.hasNext()) {
                String f = it.next();
                double w = fv.getFeatureWeight(f);
                System.out.println(f + "\t" + w) ;
            }
        }
        else if (cmd.equals("vocabulary") || cmd.equals("v")) {
            Fields fields = MultiFields.getFields(lucene); 
            Bag vocab = new TreeBag();

            if (StringUtils.isEmpty(field)) {
                Iterator<String> it = fields.iterator();
                while (it.hasNext()) {
                    String fieldName = it.next();
                    Terms terms = fields.terms(fieldName);
                    TermsEnum termsEnum = terms.iterator(null);
                    BytesRef byteRef = null;
                    while((byteRef = termsEnum.next()) != null) {
                        vocab.add(byteRef.utf8ToString(), termsEnum.docFreq());
                    }
                }
            }
            else {
                Terms terms = fields.terms(Indexer.FIELD_TEXT);
                TermsEnum termsEnum = terms.iterator(null);
                BytesRef byteRef = null;
                while((byteRef = termsEnum.next()) != null) {
                    vocab.add(byteRef.utf8ToString(), termsEnum.docFreq());
                }
            }
            
            @SuppressWarnings("unchecked")
            Iterator<String> it = (Iterator<String>)vocab.iterator();
            while (it.hasNext()) {
                String term = (String)it.next();
                System.out.println(term + "\t" + vocab.getCount(term));
            }
            
            
        }
        else if (cmd.equals("xcount") || cmd.equals("x")) {
            // 3
            // dxcount
        }
    }
    
    public static Options createOptions()
    {
        Options options = new Options();
        options.addOption("index", true, "Path to index");
        options.addOption("field", true, "Target field (default: all)");
        options.addOption("docno", true, "Field containing document ID (default: docno)");
        options.addOption("cmd", true, "Command (e.g., dv)");
        options.addOption("arg", true, "Command argument (e.g., docno)");
        options.addOption("help", false, "Print this help message");

        return options;
    }
}
