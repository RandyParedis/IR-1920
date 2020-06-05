package edu.gslis.lucene.indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.Page;
import org.mediawiki.importer.Revision;
import org.mediawiki.importer.Siteinfo;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;

import edu.gslis.lucene.main.config.FieldConfig;
import edu.gslis.wiki.TextConverter;


/**
 * Implements the Mediawiki DumpWriter interface for 
 * SAX-based parsing of a wiki dumpfile.  Writes to a given 
 * Lucene index
 */
public class WikiIndexWriter  implements DumpWriter {

    Document currentDoc = null;
    IndexWriter index = null;
    Map<String, FieldConfig> fields = new HashMap<String, FieldConfig>();
    
    org.sweble.wikitext.engine.Compiler compiler = null;
    Analyzer analyzer = null;
    SimpleWikiConfiguration wikicfg = null;
    WikiTextIndexer parent = null;
    
    public WikiIndexWriter(IndexWriter index, Set<FieldConfig> fieldSet, WikiTextIndexer parent) 
            throws IOException {
        this.index = index;
        this.parent = parent;
        for (FieldConfig field: fieldSet) {
            fields.put(field.getName(), field);
        }
        
        wikicfg = new SimpleWikiConfiguration(
                "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
        
        compiler = new org.sweble.wikitext.engine.Compiler(wikicfg);
        analyzer = index.getAnalyzer();

    }
    
    public void close() throws IOException {        
    }

    public void writeStartWiki() throws IOException{        
    }
    public void writeEndWiki() throws IOException{
        
    }

    public void writeSiteinfo(Siteinfo info) throws IOException {
        
    }

    public void writeStartPage(Page page) throws IOException {
        currentDoc = new Document();
        //String pageId = String.valueOf(page.Id);
        String pageTitle = page.Title.Text;
        //boolean isRedirect = page.isRedirect;        
        //addField("id", pageId);
        System.out.println("Indexing " + pageTitle);
        addField("docno", pageTitle);
        addField("title", pageTitle);
    }
    
    public void writeEndPage() throws IOException {
        index.addDocument(currentDoc);
    }

    public void writeRevision(Revision revision) throws IOException {
        String text = revision.Text;
        addField("text", text);        
    }
    
    private void addField(String name, String value) throws IOException {
        try
        {
            
            FieldConfig field = fields.get(name);
            if (field == null)
                return;

            String source = field.getSource();
            String element = field.getElement();
            if (StringUtils.isEmpty(source) || !source.equals(FieldConfig.SOURCE_ELEMENT)) {
                throw new IOException ("Unsupported source " + source);
            }
            
            if (StringUtils.isEmpty(element)) {
                throw new IOException ("Element name must be specified");
            }
            
            if (element.equals("text")) {
                                    
                String wikitext = StringEscapeUtils.unescapeHtml(value);
                String title = currentDoc.getField("docno").stringValue();
                String output = "";
                try
                {
                    PageTitle pageTitle = PageTitle.make(wikicfg, title);
                    PageId pageId = new PageId(pageTitle, -1);
                    CompiledPage cp = compiler.postprocess(pageId, wikitext, null);
                    
                    TextConverter p = new TextConverter(wikicfg, 80);
                    output = (String) p.go(cp.getPage());
                    output = title + "\n" + output;                    
                } catch (Exception e) {
                    System.out.println("Failed to parse page" + title);
                    e.printStackTrace();
                }
            
                parent.addField(currentDoc, field, output, index.getAnalyzer());
            }
            else
                parent.addField(currentDoc, field, value, index.getAnalyzer());

        } catch (Exception e) { 
            throw new IOException(e);
        }
    }
}
