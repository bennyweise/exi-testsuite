package org.w3c.exi.ttf.candidate.exirs;

import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.exi.ttf.SAXDriver;
import org.w3c.exi.ttf.parameters.DriverParameters;
import org.w3c.exi.ttf.parameters.TestCaseParameters;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class ExiRsSAXDriver extends SAXDriver {
    private static final String PARAM_CLI = "org.w3c.exi.ttf.candidate.exirs.cli";
    private static final String ENV_CLI = "EXIRS_CLI";

    private String cliPath;

    @Override
    protected void prepareTestCase(DriverParameters driverParams,
            TestCaseParameters testCaseParams) throws Exception {
        cliPath = driverParams.params.getParam(PARAM_CLI);
        if (cliPath == null || cliPath.length() == 0) {
            cliPath = System.getenv(ENV_CLI);
        }
        if (cliPath == null || cliPath.length() == 0) {
            cliPath = "exi-rs";
        }
    }

    @Override
    protected XMLReader getXMLReader() throws Exception {
        return new ExiRsXmlReader(cliPath);
    }

    @Override
    protected ContentHandler getSAXEncoder(OutputStream outputStream)
            throws Exception {
        throw new SAXException("exi-rs encoder not wired yet");
    }

    @Override
    public void transcodeTestCase(InputStream xmlInput,
            OutputStream encodedOutput) throws Exception {
        throw new SAXException("exi-rs encoder not wired yet");
    }
}
