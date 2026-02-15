package org.w3c.exi.ttf.candidate.exirs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

final class ExiRsXmlReader implements XMLReader {
    private final String cliPath;
    private final XMLReader xmlReader;

    ExiRsXmlReader(String cliPath) throws SAXException {
        this.cliPath = cliPath;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        try {
            this.xmlReader = spf.newSAXParser().getXMLReader();
        } catch (Exception e) {
            throw new SAXException("failed to create XML reader", e);
        }
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return xmlReader.getFeature(name);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        xmlReader.setFeature(name, value);
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return xmlReader.getProperty(name);
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        xmlReader.setProperty(name, value);
    }

    public void setEntityResolver(EntityResolver resolver) {
        xmlReader.setEntityResolver(resolver);
    }

    public EntityResolver getEntityResolver() {
        return xmlReader.getEntityResolver();
    }

    public void setDTDHandler(DTDHandler handler) {
        xmlReader.setDTDHandler(handler);
    }

    public DTDHandler getDTDHandler() {
        return xmlReader.getDTDHandler();
    }

    public void setContentHandler(ContentHandler handler) {
        xmlReader.setContentHandler(handler);
    }

    public ContentHandler getContentHandler() {
        return xmlReader.getContentHandler();
    }

    public void setErrorHandler(ErrorHandler handler) {
        xmlReader.setErrorHandler(handler);
    }

    public ErrorHandler getErrorHandler() {
        return xmlReader.getErrorHandler();
    }

    public void parse(InputSource input) throws IOException, SAXException {
        InputStream exiInput = input.getByteStream();
        if (exiInput == null) {
            String systemId = input.getSystemId();
            if (systemId == null || systemId.length() == 0) {
                throw new SAXException("missing EXI input stream");
            }
            exiInput = new FileInputStream(systemId);
        }

        byte[] exiBytes = readFully(exiInput);
        byte[] xmlBytes = decodeToXml(exiBytes);

        InputSource xmlInput = new InputSource(new ByteArrayInputStream(xmlBytes));
        String systemId = input.getSystemId();
        if (systemId != null) {
            xmlInput.setSystemId(systemId);
        }
        xmlReader.parse(xmlInput);
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    private byte[] decodeToXml(byte[] exiBytes) throws SAXException {
        List<String> cmd = new ArrayList<String>(2);
        cmd.add(cliPath);
        cmd.add("decode");

        Process process;
        try {
            process = new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            throw new SAXException("failed to start exi-rs CLI: " + cliPath, e);
        }

        try {
            OutputStream stdin = process.getOutputStream();
            stdin.write(exiBytes);
            stdin.flush();
            stdin.close();

            StreamCollector stdout = new StreamCollector(process.getInputStream());
            StreamCollector stderr = new StreamCollector(process.getErrorStream());
            stdout.start();
            stderr.start();

            int exit = process.waitFor();
            stdout.join();
            stderr.join();

            if (exit != 0) {
                throw new SAXException("exi-rs decode failed: " + stderr.getUtf8());
            }

            return stdout.getBytes();
        } catch (IOException e) {
            throw new SAXException("failed to run exi-rs decode", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SAXException("exi-rs decode interrupted", e);
        }
    }

    private static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static final class StreamCollector extends Thread {
        private final InputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        StreamCollector(InputStream input) {
            this.input = input;
        }

        public void run() {
            byte[] buffer = new byte[8192];
            int read;
            try {
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException e) {
                // ignore errors from process stream closure
            }
        }

        byte[] getBytes() {
            return output.toByteArray();
        }

        String getUtf8() {
            try {
                return new String(output.toByteArray(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return new String(output.toByteArray());
            }
        }
    }
}
