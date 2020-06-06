package edu.gslis.wiki;

import java.util.HashSet;
import java.util.Set;

import org.sweble.wikitext.engine.Page;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.LinkTargetException;
import org.sweble.wikitext.lazy.parser.Bold;
import org.sweble.wikitext.lazy.parser.InternalLink;
import org.sweble.wikitext.lazy.parser.Italics;
import org.sweble.wikitext.lazy.parser.Paragraph;
import org.sweble.wikitext.lazy.parser.Section;
import org.sweble.wikitext.lazy.parser.XmlElement;

import de.fau.cs.osr.ptk.common.Visitor;
import de.fau.cs.osr.ptk.common.ast.AstNode;
import de.fau.cs.osr.ptk.common.ast.NodeList;


public class LinkVisitor
        extends
            Visitor
{
    
    final SimpleWikiConfiguration config;
    
    
    Set<InternalLink> links = new HashSet<InternalLink>();
    private StringBuilder sb;
    
    
    // =========================================================================
    
    public LinkVisitor(SimpleWikiConfiguration config)
    {
        this.config = config;
    }
    
    @Override
    protected boolean before(AstNode node)
    {
        // This method is called by go() before visitation starts
        sb = new StringBuilder();
        return super.before(node);
    }
    
    @Override
    protected Object after(AstNode node, Object result)
    {
        // This method is called by go() after visitation has finished
        // The return value will be passed to go() which passes it to the caller
        return sb.toString();
    }
    
    // =========================================================================
    
    public void visit(AstNode n)
    {
        // Fallback for all nodes that are not explicitly handled below
    }
    
    public void visit(NodeList n)
    {
        iterate(n);
    }
    
    public void visit(Page p)
    {
        iterate(p.getContent());
    }
    
    public void visit(Bold b)
    {
        iterate(b.getContent());
    }
    
    public void visit(Italics i)
    {
        iterate(i.getContent());
    }
    
    
    public void visit(InternalLink link)
    {
        try
        {
            PageTitle page = PageTitle.make(config, link.getTarget());
            if (page.getNamespace().equals(config.getNamespace("Category")))
                return;
        }
        catch (LinkTargetException e)
        {
        }
        
        links.add(link);
    }
    
    public void visit(Section s)
    {        
        iterate(s.getTitle());        
        iterate(s.getBody());
    }
    
    public void visit(Paragraph p)
    {
        iterate(p.getContent());
    }
    

    public void visit(XmlElement e)
    {
        iterate(e.getBody());
    }
    
    public Set<InternalLink> getLinks() {
        return links;
    }
    

}
