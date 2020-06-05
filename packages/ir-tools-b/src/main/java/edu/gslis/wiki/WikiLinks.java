package edu.gslis.wiki;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.xml.sax.InputSource;

import edu.gslis.lucene.indexer.XmlDumpReader;



/**
 * Extract internal and external links from a Wiki dump file
 * @author cwillis
 *
 */
public class WikiLinks {
    org.sweble.wikitext.engine.Compiler compiler;
    SimpleWikiConfiguration wikicfg;
    public static void main(String[] args) throws IOException {
        WikiLinks linkReader = new WikiLinks();
        linkReader.getLinks(new File(args[0]));
    }
    
    public WikiLinks() throws IOException {
        wikicfg = new SimpleWikiConfiguration(
                "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
        
        compiler = new org.sweble.wikitext.engine.Compiler(wikicfg);
    }
    
    public void getLinks(Reader reader) throws IOException {
        LinkDumper dumper = new LinkDumper();
        XmlDumpReader wikiReader = new XmlDumpReader(new InputSource(reader), dumper);
        wikiReader.readDump();
     
    }
    public void getLinks(File file) throws IOException
    {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f: files) {
                InputStream is = new FileInputStream(f);
                getLinks(new InputStreamReader(is));
                is.close();
            }
        }
        else if (file.getName().endsWith("tgz")) {
            TarArchiveInputStream tis = 
                    new TarArchiveInputStream(
                            new GzipCompressorInputStream(
                                    new BufferedInputStream(
                                            new FileInputStream(file))));
            TarArchiveEntry entry;
            while (null != (entry = tis.getNextTarEntry())) 
            {
                if (entry.isFile()) {
                    int size = 0;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int c;
                    while (size < entry.getSize()) {
                        c = tis.read();
                        size++;
                        bos.write(c);
                    }
                    InputStream is = new ByteArrayInputStream(bos.toByteArray());
                    getLinks(new InputStreamReader(is));
                    is.close();
                }
            }
            tis.close();
        }
        else {
            FileInputStream fis = new FileInputStream(file);
            Reader reader= null;
            try {
                // Use commons-compress to auto-detect compressed formats
                InputStream ois = new BufferedInputStream(fis);
                InputStream is = new CompressorStreamFactory().createCompressorInputStream(ois);                
                System.out.println("Auto-detected format");
                reader = new InputStreamReader(is, "UTF-8");

            } catch (Exception e) {
                try { 
                    InputStream ois = new BufferedInputStream(fis);
                    // Try XZ directly, for grins
                    InputStream is = new XZCompressorInputStream(ois);
                    System.out.println("Reading XZ compressed text");
                    reader = new InputStreamReader(is, "UTF-8");
                } catch (Exception e2) {
                    System.out.println("Assuming UTF-8 encoded text");
                    // Treat as uncompressed raw XML
                    reader = new InputStreamReader(fis);                    
                }
            }           
            getLinks(reader);
            reader.close();
        }

    }
}
