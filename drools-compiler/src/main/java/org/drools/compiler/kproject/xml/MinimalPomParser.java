package org.drools.compiler.kproject.xml;

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.kie.api.builder.ReleaseId;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

public class MinimalPomParser extends DefaultHandler {

    private int           depth;

    private PomModel      model;

    private StringBuilder characters;    
    
    private String        pomGroupId;
    private String        pomArtifactId;
    private String        pomVersion;

    private String        currentGroupId;
    private String        currentArtifactId;
    private String        currentVersion;
    private String        currentScope;

    private MinimalPomParser() {
        model = new PomModel();
    }
    
    static PomModel parse(String path, InputStream is) {
        MinimalPomParser handler = new MinimalPomParser();        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating( false );
            factory.setNamespaceAware( false );
            
            SAXParser parser = factory.newSAXParser();
            parser.parse( is, handler );
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse File '" + path + "'", e);
        }
        
        return handler.getPomModel();
        

    }

    public PomModel getPomModel() {
        return this.model;
    }
    
    public void startElement(final String uri,
                             final String localName,
                             final String qname,
                             final Attributes attrs) throws SAXException {
        if ( "groupId".equals( qname ) || "artifactId".equals( qname ) || "version".equals( qname ) ) {
            this.characters = new StringBuilder();            
        }
        
        depth++;
    }

    public void endElement(final String uri,
                           final String localName,
                           final String qname) throws SAXException {
        if ( "project".equals( qname ) ) {
            ReleaseId parentReleaseId = model.getParentReleaseId();
            model.setReleaseId(new ReleaseIdImpl(pomGroupId != null ? pomGroupId : parentReleaseId.getGroupId(),
                                                 pomArtifactId,
                                                 pomVersion != null ? pomVersion : parentReleaseId.getVersion()));
        } else if ( "parent".equals( qname ) ) {
            if ( currentGroupId != null && currentArtifactId != null && currentVersion != null ) {
                model.setParentReleaseId(new ReleaseIdImpl(currentGroupId, currentArtifactId, currentVersion));
            }
            currentGroupId = null;
            currentArtifactId = null;
            currentVersion = null;
        } else if ( "dependency".equals( qname ) ) {
            if ( !"provided".equals(currentScope) && !"test".equals(currentScope) &&
                 currentGroupId != null && currentArtifactId != null && currentVersion != null ) {
                model.addDependency(new ReleaseIdImpl(currentGroupId, currentArtifactId, currentVersion));
            }
            currentGroupId = null;
            currentArtifactId = null;
            currentVersion = null;
            currentScope = null;
        } else {
            String text = ( this.characters != null ) ? this.characters.toString() : null;
            if ( text != null) {
                if ( "groupId".equals( qname ) ) {
                    if ( depth == 2 ) {
                        pomGroupId = text;
                    } else {
                        currentGroupId = text;
                    }
                } else if ( "artifactId".equals( qname ) ) {
                    if ( depth == 2 ) {
                        pomArtifactId = text;
                    } else {
                        currentArtifactId = text;
                    }
                } else if ( "version".equals( qname ) ) {
                    if ( depth == 2 ) {
                        pomVersion = text;
                    } else {
                        currentVersion = text;
                    }
                } else if ( "scope".equals( qname ) ) {
                    currentScope = text;
                }
            }
        }
        this.characters = null;
        depth--;
    }
    
    /**
     * @param chars
     * @param start
     * @param len
     * @see org.xml.sax.ContentHandler
     */
    public void characters(final char[] chars,
                           final int start,
                           final int len) {
        if ( this.characters != null ) {
            this.characters.append( chars,
                                    start,
                                    len );
        }
    }  
}