package dk.netarkivet.harvester.harvesting.rest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

/**
 * Interface implementation using Apache HTTP Client 4.0.1
 */
public class HeritrixSessionImpl implements HeritrixSession {


    /**
     * The default http client.
     */
    private final DefaultHttpClient client;

    /**
     * Heritrix' base url.
     */
    private final String baseUrl;

    /**
     *
     */
    private final DocumentBuilder documentBuilder;

    /**
     * My logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(HeritrixSessionImpl.class);

    /**
     * C'tor initializing the Heritrix session.
     *
     * @param keystoreFile The {@link java.io.File} containing the SSL certificates.
     * @param keyStorePassword A password for the keystore file.
     * @param hostname The hostname where Heritrix runs.
     * @param port The port on which Heritrix listens.
     * @param userName The Heritrix Web GUI Username.
     * @param password The Heritrix Web GUI password.
     * 
     * @throws HeritrixSessionInitializationException Thrown if there have been problems initializing the session.
     */
    public HeritrixSessionImpl(final File keystoreFile, final String keyStorePassword,
            final String hostname, final int port, final String userName, final String password)
            throws HeritrixSessionInitializationException {

        KeyStore trust;
        SSLSocketFactory socketFactory;
        Scheme sch;

        try {
            trust = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream instream;
            try {
                instream = new FileInputStream(keystoreFile);
                try {
                    trust.load(instream, keyStorePassword.toCharArray());
                } catch (NoSuchAlgorithmException e) {
                    LOG.error("Cannot load the trustfile!", e);
                } catch (CertificateException e) {
                    LOG.error("Cannot load the trustfile!", e);
                } catch (IOException e) {
                    LOG.error("Cannot load the trustfile!", e);
                } finally {
                    instream.close();
                }
            } catch (FileNotFoundException e) {
                LOG.error("Cannot load the trustfile!", e);
            } catch (IOException e) {
                LOG.error("Cannot load the trustfile!", e);
            }

            socketFactory = new SSLSocketFactory(trust);
            sch = new Scheme("https", socketFactory, port);

            this.client = new DefaultHttpClient();
            this.client.getConnectionManager().getSchemeRegistry().register(sch);
            this.client.getCredentialsProvider().setCredentials(new AuthScope(hostname, port),
                    new UsernamePasswordCredentials(userName, password));

            this.baseUrl = "https://" + hostname + ":" + Integer.toString(port) + "/engine/";

        } catch (KeyStoreException e) {
            String msg = "Error with the keystore!";
            LOG.error(msg, e);
            throw new HeritrixSessionInitializationException(msg, e);
        } catch (KeyManagementException e) {
            String msg = "Error with the keystore!";
            LOG.error(msg, e);
            throw new HeritrixSessionInitializationException(msg, e);
        } catch (UnrecoverableKeyException e) {
            String msg = "Error with the keystore!";
            LOG.error(msg, e);
            throw new HeritrixSessionInitializationException(msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Error with the keystore!";
            LOG.error(msg, e);
            throw new HeritrixSessionInitializationException(msg, e);
        }

        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            String msg = "Error getting a XML document builder!";
            LOG.error(msg, e);
            throw new HeritrixSessionInitializationException(msg, e);
        }
    }

    /**
     * Execute a HTTP request.
     *
     * @param request The request to execute.
     * @return The parsed XML document.
     * @throws IOException When the communication broke.
     * @throws ClientProtocolException Upon HTTP communication errors.
     */
    private HttpResponse execute(final HttpRequestBase request) throws ClientProtocolException, IOException {
        return this.client.execute(request);
    }

    /**
     * Execute a HTTP request and parse the returning XML document.
     * 
     * @param request The request to execute.
     * @return The parsed XML document.
     */
    private Document executeXml(final HttpRequestBase request) {

        try {
            final HttpResponse response = execute(request);
            final HttpEntity entity = response.getEntity();

            return this.documentBuilder.parse(entity.getContent());

        } catch (ClientProtocolException e) {
            LOG.error("Error connecting to the server", e);
        } catch (IOException e) {
            LOG.error("IO error communicating with the server", e);
        } catch (IllegalStateException e) {
            LOG.error("Cannot parse returning XML. Url was: " + request.getRequestLine(), e);
        } catch (SAXException e) {
            LOG.error("Cannot parse returning XML. Url was: " + request.getRequestLine(), e);
        }
        return null;

    }

    /**
     * GET a XML document from the REST API.
     *
     * @param url The URL to GET.
     * @return THe parsed XML document.
     */
    private HttpResponse get(final String url) {
        final HttpGet getMethod = new HttpGet(url);
        try {
            return execute(getMethod);
        } catch (ClientProtocolException e) {
            LOG.error("Error connecting to the server", e);
        } catch (IOException e) {
            LOG.error("Error connecting to the server", e);
        }
        return null;
    }

    /**
     * GET a XML document from the REST API and parse the returned XML.
     *
     * @param url The URL to GET.
     * @return THe parsed XML document.
     */
    private Document getXml(final String url) {

        final HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("accept", "application/xml");

        return executeXml(getMethod);

    }

    /**
     * Send a POST request to the REST API and read the returning XML.
     *
     * @param url The URL to POST to.
     * @param nameValuePairs A list of {@link org.apache.http.NameValuePair}s.
     * @return The parsed XML Document.
     */
    private Document postXml(final String url, final NameValuePair... nameValuePairs) {

        final HttpPost postMethod = new HttpPost(url);
        postMethod.addHeader("Accept", "application/xml");
        if (nameValuePairs.length > 0) {
            final List<NameValuePair> params = Arrays.asList(nameValuePairs);
            try {
                postMethod.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
                LOG.error("Enconding not supported!?", e);
            }
        }
        return executeXml(postMethod);

    }

    /**
     * Send a PUT request to the REST API.
     *
     * @param url The exact URL to PUT to.
     * @param data The data that should be sent.
     * @return The HTTP Response.
     */
    private HttpResponse put(final String url, final String data) {

        final HttpPut putMethod = new HttpPut(url);
        try {
            putMethod.setEntity(new ByteArrayEntity(data.getBytes()));
            return execute(putMethod);
        } catch (ClientProtocolException e) {
            LOG.error("Error connecting to the server", e);
        } catch (IOException e) {
            LOG.error("Error connecting to the server", e);
        }
        return null;

    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#getJobStatus(String)
     */
    @Override
    public Document getJobStatus(final String jobName) {
        return getXml(this.baseUrl + "job/" + jobName);
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#isJobRunning(String)
     */
    @Override
    public boolean isJobRunning(final String jobName) {
        final Document d = getJobStatus(jobName);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        boolean isRunning = false;
        try {
            final String res = xPath.evaluate("//job/statusDescription", d);
            if (res.equals("Active: RUNNING")) {
                isRunning = true;
            }
        } catch (XPathExpressionException e) {
            LOG.error("could not read status from jobdescription", e);
        }

        return isRunning;
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#isPaused(String)
     */
    @Override
    public boolean isPaused(final String jobName) {
        final Document d = getJobStatus(jobName);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        boolean isRunning = false;
        try {
            final String res = xPath.evaluate("//job/statusDescription", d);
            if (res.equals("Active: PAUSED")) {
                isRunning = true;
            }
        } catch (XPathExpressionException e) {
            LOG.error("could not read status from jobdescription", e);
        }

        return isRunning;
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#createJob(String)
     */
    @Override
    public void createJob(final String jobName) {
        final NameValuePair addPath = new BasicNameValuePair("addpath", jobName);
        final NameValuePair action = new BasicNameValuePair("action", "create");
        postXml(this.baseUrl, addPath, action);
    }

    /**
     * @see HeritrixSession#rescanJobDirectory()
     */
    @Override
    public Document rescanJobDirectory() {
        return postXml(this.baseUrl, new BasicNameValuePair("action", "rescan"));
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#pauseJob(String)
     */
    @Override
    public void pauseJob(final String jobName) {
        if (isJobRunning(jobName)) {
            postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "pause"));
        } else {
            LOG.info("job is not running, could not be paused");
        }
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#unpauseJob(String)
     */
    @Override
    public void unpauseJob(final String jobName) {
        if (!isJobRunning(jobName)) {
            postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "unpause"));
        } else {
            LOG.info("job is running, could not unpause");
        }
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#buildJob(String)
     */
    @Override
    public boolean buildJob(final String jobName) {
        final Document doc = postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "build"));
        final XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList jobs;
        try {
            jobs = (NodeList) xpath.evaluate("job/statusDescription", doc, XPathConstants.NODESET);
            for (int i = 0; i < jobs.getLength(); i++) {
                if (jobs.item(i).getFirstChild().getTextContent().equals("Unbuilt")) {
                    return false;
                }
            }
        } catch (XPathExpressionException e) {
            LOG.error("could not read the existing jobs", e);

        }

        return true;
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#launchJob(String)
     */
    @Override
    public boolean launchJob(final String jobName) {
        final Document doc = postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "launch"));
        final XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList jobs;
        try {
            jobs = (NodeList) xpath.evaluate("job/statusDescription", doc, XPathConstants.NODESET);
            for (int i = 0; i < jobs.getLength(); i++) {
                if (jobs.item(i).getFirstChild().getTextContent().equals("Ready")) {
                    return true;
                }
            }
        } catch (XPathExpressionException e) {
            LOG.error("could not read the existing jobs", e);

        }

        return false;
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#terminateJob(String)
     */
    @Override
    public void terminateJob(final String jobName) {
        if (isJobRunning(jobName)) {
            postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "terminate"));
        } else {
            LOG.info("job is not running");
        }
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#tearDownJob(String)
     */
    @Override
    public void tearDownJob(final String jobName) {
        postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "teardown"));
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#checkpointJob(String)
     */
    @Override
    public void checkpointJob(final String jobName) {
        postXml(this.baseUrl + "job/" + jobName, new BasicNameValuePair("action", "checkpoint"));
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#copyJob(String, String, boolean)
     */
    @Override
    public void copyJob(final String original, final String jobName, final boolean asProfile) {
        final String url = this.baseUrl + "job/" + original;
        postXml(url, new BasicNameValuePair("copyTo", jobName));
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#updateConfig(String, String)
     */
    @Override
    public void updateConfig(final String jobName, final String cXml) {
        final String url = this.baseUrl + "job/" + jobName + "/jobdir/crawler-beans.cxml";
        final HttpResponse resp = put(url, cXml);
        try {
            resp.getEntity().consumeContent();
        } catch (IOException e) {
            LOG.error("Could not consume the content", e);
        }
    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#getCrawlLog(String)
     */
    @Override
    public HttpResponse getCrawlLog(String jobName) {
        return get(this.baseUrl + "job/" + jobName + "/jobdir/logs/crawl.log");

    }

    /**
     * @see com.github.truemped.heritrix.HeritrixSession#jobExists(String)
     */
    @Override
    public boolean jobExists(String jobName) {
        final Document rescanDoc = rescanJobDirectory();
        final XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList jobs;
        try {
            jobs = (NodeList) xpath.evaluate("engine/jobs/value/shortName", rescanDoc,
                    XPathConstants.NODESET);
            for (int i = 0; i < jobs.getLength(); i++) {
                if (jobs.item(i).getFirstChild().getTextContent().equals(jobName)) {
                    return true;
                }
            }
        } catch (XPathExpressionException e) {
            LOG.error("could not read the existing jobs", e);

        }

        return false;

    }

}
