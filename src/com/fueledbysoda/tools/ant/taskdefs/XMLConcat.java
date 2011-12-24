package com.fueledbysoda.tools.ant.taskdefs;

import java.io.*;
import java.util.StringTokenizer;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLConcat extends MatchingTask
{
    private File baseDir;
    private File outFile;
    private boolean performDirectoryScan;
    private String xPathExpression;
    private String rootXPath;
    private FileUtils fileUtils;
    
    public XMLConcat()
    {
        baseDir = null;
        outFile = null;
        performDirectoryScan = true;
        xPathExpression = null;
        rootXPath = null;
        fileUtils = FileUtils.getFileUtils();
    }

    public void execute()
        throws BuildException
    {
        if(outFile == null)
            throw new BuildException("out attribute must be set");
        if(baseDir == null)
            baseDir = getProject().resolveFile(".");
        if(xPathExpression == null)
            xPathExpression = "/";
        if(rootXPath == null)
            rootXPath = "/root";
        try
        {
            if(outFile.exists())
                FileUtils.delete(outFile);
            fileUtils.createNewFile(outFile, true);
            Node outNode = createResultDocument(rootXPath);
            DirectoryScanner scanner = getDirectoryScanner(baseDir);
            String list[] = scanner.getIncludedFiles();
            for(int i = 0; i < list.length; i++)
                process(baseDir, list[i], outNode, xPathExpression);

            if(performDirectoryScan)
            {
                String dirs[] = scanner.getIncludedDirectories();
                for(int j = 0; j < dirs.length; j++)
                {
                    list = (new File(baseDir, dirs[j])).list();
                    for(int i = 0; i < list.length; i++)
                        process(baseDir, dirs[j] + File.separator + list[i], outNode, xPathExpression);

                }

            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource((outNode instanceof Document) ? ((Node) ((Document)outNode)) : ((Node) (outNode.getOwnerDocument()))), new StreamResult(new FileOutputStream(outFile)));
        }
        catch(Exception ex)
        {
            log("Failed to process", 2);
            if(outFile != null)
                outFile.delete();
            throw new BuildException(ex);
        }
    }

    private void process(File baseDir, String xmlFile, Node outNode, String xPathExpression)
        throws BuildException
    {
        File inFile = null;
        try
        {
            inFile = new File(baseDir, xmlFile);
            if(inFile.isDirectory())
            {
                log("Skipping " + inFile + " it is a directory.", 3);
                return;
            }
            NodeList nodes = processXPath(inFile, xPathExpression);
            for(int i = 0; i < nodes.getLength(); i++)
            {
                Node node = nodes.item(i);
                outNode.appendChild(((outNode instanceof Document) ? (Document)outNode : outNode.getOwnerDocument()).importNode(node, true));
            }

        }
        catch(Exception ex)
        {
            log("Failed to process " + inFile, 2);
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

        return addElement;
    }

    public void setBasedir(File dir)
    {
        baseDir = dir;
    }

    public void setOut(File outFile)
    {
        this.outFile = outFile;
    }

    public void setScanIncludedDirectories(boolean b)
    {
        performDirectoryScan = b;
    }

    public void setXPath(String xPathExpression)
    {
        this.xPathExpression = xPathExpression;
    }

    public void setRootXPath(String rootXPath)
    {
        this.rootXPath = rootXPath;
    }
}
