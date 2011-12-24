package com.fueledbysoda.tools.ant.taskdefs;

import java.io.*;
import java.util.StringTokenizer;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLSplit extends MatchingTask
{
    private File baseFile;
    private File outDir;
    private String xPathExpression;
    private String filenameXPath;
    private String targetExtension;
    private FileUtils fileUtils;
	
    public XMLSplit()
    {
        baseFile = null;
        outDir = null;
        xPathExpression = null;
        filenameXPath = null;
        targetExtension = ".xml";
        fileUtils = FileUtils.getFileUtils();
    }

    public void execute()
        throws BuildException
    {
        if(outDir == null)
            throw new BuildException("out attribute must be set");
        if(baseFile == null)
            throw new BuildException("file attribute must be set");
        if(xPathExpression == null)
            throw new BuildException("xpath attribute must be set");
        if(filenameXPath == null)
            throw new BuildException("filenamexpath attribute must be set");
        try
        {
            process(baseFile, outDir, xPathExpression, filenameXPath);
        }
        catch(Exception ex)
        {
            log("Failed to process", 2);
            if(outDir != null)
                outDir.delete();
            throw new BuildException(ex);
        }
    }

    private void process(File baseFile, File outDir, String xPathExpression, String filenameXPath)
        throws BuildException
    {
        try
        {
            NodeList nodes = processXPath(baseFile, xPathExpression);
            for(int i = 0; i < nodes.getLength(); i++)
            {
                Node node = nodes.item(i);
                String filename = processXPathInNode(node, filenameXPath) + targetExtension;
                Node outNode = createResultDocument("/");
                outNode.appendChild(((outNode instanceof Document) ? (Document)outNode : outNode.getOwnerDocument()).importNode(node, true));
                File outFile = new File(outDir, filename);
                fileUtils.createNewFile(outFile, true);
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.transform(new DOMSource((outNode instanceof Document) ? ((Node) ((Document)outNode)) : ((Node) (outNode.getOwnerDocument()))), new StreamResult(new FileOutputStream(outFile)));
            }

        }
        catch(Exception ex)
        {
            throw new BuildException(ex);
        }
    }

    private NodeList processXPath(File inFile, String xPathExpression)
        throws FileNotFoundException, XPathExpressionException
    {
        XPath xPath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new FileInputStream(inFile));
        NodeList nodes = (NodeList)xPath.evaluate(xPathExpression, inputSource, XPathConstants.NODESET);
        return nodes;
    }

    private String processXPathInNode(Node node, String xPathExpression)
        throws XPathExpressionException
    {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String value = (String)xPath.evaluate(xPathExpression, node, XPathConstants.STRING);
        return value;
    }

    public Node createResultDocument(String rootXPath)
        throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Element addElement = document.getDocumentElement();
        for(StringTokenizer tokenizer = new StringTokenizer(rootXPath, "/"); tokenizer.hasMoreTokens();)
        {
            String token = tokenizer.nextToken();
            Element newElement = document.createElement(token);
            if(addElement == null)
                document.appendChild(newElement);
            else
                addElement.appendChild(newElement);
            addElement = newElement;
        }

        return ((Node) (addElement != null ? addElement : document));
    }

    public void setFile(File file)
    {
        baseFile = file;
    }

    public void setOut(File outDir)
    {
        this.outDir = outDir;
    }

    public void setXPath(String xPathExpression)
    {
        this.xPathExpression = xPathExpression;
    }

    public void setFilenameXPath(String filenameXPath)
    {
        this.filenameXPath = filenameXPath;
    }

    public void setExtension(String ext)
    {
        targetExtension = ext;
    }
}
