package edu.gslis.wiki;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.Revision;
import org.mediawiki.importer.Siteinfo;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.parser.InternalLink;
import org.sweble.wikitext.lazy.parser.Italics;
import org.sweble.wikitext.lazy.parser.LinkTitle;

import de.fau.cs.osr.ptk.common.ast.AstNode;
import de.fau.cs.osr.ptk.common.ast.Text;


public class LinkDumper  implements DumpWriter {

    org.sweble.wikitext.engine.Compiler compiler = null;
    SimpleWikiConfiguration wikicfg = null;
    String currentTitle = "";
    int currentPage = -1;
    
    public LinkDumper() throws IOException {
        
        wikicfg = new SimpleWikiConfiguration(
                "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
        
        compiler = new org.sweble.wikitext.engine.Compiler(wikicfg);

    }
    
    public void close() throws IOException {        
    }

    public void writeStartWiki() throws IOException{        
    }
    
    public void writeEndWiki() throws IOException{
    }

    public void writeSiteinfo(Siteinfo info) throws IOException {
    }
    public void writeStartPage(org.mediawiki.importer.Page page)
            throws IOException {
        currentPage  =  page.Id;
        currentTitle = page.Title.Text;
    }
    
    public void writeEndPage() throws IOException {

    }

    public void writeRevision(Revision revision) throws IOException {
        String text = revision.Text;
                                
        String wikitext = StringEscapeUtils.unescapeHtml(text);
        try
        {
            PageTitle pageTitle = PageTitle.make(wikicfg, currentTitle);
            PageId pageId = new PageId(pageTitle, -1);
            CompiledPage cp = compiler.postprocess(pageId, wikitext, null);
            
            LinkVisitor v = new LinkVisitor(wikicfg);
            
            v.go(cp.getPage());
            Set<InternalLink> links = v.getLinks();
            for (InternalLink link: links) {
                PageTitle linkPage = PageTitle.make(wikicfg, link.getTarget());
                System.out.println(
                      pageTitle.getTitle() + "\t" + 
                      linkPage.getTitle() + "\t" + 
                      getText(link.getTitle())
                      );
            }
        } catch (Exception e) {
            System.out.println("Failed to parse page" + currentTitle);
            e.printStackTrace();
        }
        
    }
    

    public String getText(AstNode node) {
        if (node instanceof Text) {
            return ((Text)node).getContent();
        } else {
            if (node.size() > 0) {
                return getText(node.get(0));
            }
        }
        return "";
    }
}
