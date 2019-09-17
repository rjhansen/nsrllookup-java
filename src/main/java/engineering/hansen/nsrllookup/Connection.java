package engineering.hansen.nsrllookup;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Connection {
    private static final Pattern MD5RX = Pattern.compile("^[A-Fa-f0-9]{32}$");
    private static final Pattern GOODRX = Pattern.compile("^OK ([10]*)$");
    private static final int CHUNK_SIZE = 128;
    private static final String DEFAULT_HOST;
    private static final int DEFAULT_PORT;
    private final String host;
    private final int port;
    private static final Logger LOGGER;

    static {
        String configuredHost = "nsrllookup.com";
        int configuredPort = 9120;
        File f = null;

        if (null == System.getProperty("log4j.configuration")) {
            ArrayList<URI> uris = new ArrayList<>();
            if (null != System.getProperty("user.home")) {
                f = Paths.get(System.getProperty("user.home"),
                        ".config",
                        "nsrllookup",
                        "log4j2.xml").toFile();

                if (f.exists() && f.isFile() && f.canRead())
                    uris.add(f.toURI());
            }
            try {
                String resName = "engineering/hansen/nsrllookup/log4j2.xml";
                URL foo = Connection.class.getClassLoader()
                        .getResource(resName);
                if (null != foo) {
                    uris.add(foo.toURI());
                }
            } catch (URISyntaxException use) {
                // fallthrough
            }
            if (uris.size() > 0)
                Configurator.initialize(null, null, uris, null);
        }
        LOGGER = LogManager.getLogger(Connection.class.getName());

        if (null != System.getProperty("user.home")) {
            f = Paths.get(System.getProperty("user.home"),
                    ".config",
                    "nsrllookup",
                    "nsrllookup.ini").toFile();
            if (f.exists() && f.isFile() && f.canRead() && f.length() < 2048) {
                try {
                    String lastSeenHost = configuredHost;
                    int lastSeenPort = configuredPort;
                    Pattern hostp = Pattern.compile("^\\s*host\\s*=\\s*([^\\s]+)\\s*$");
                    Pattern portp = Pattern.compile("^\\s*port\\s*=\\s*(\\d+)\\s*$");
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = br.readLine();
                    int lineNumber = 1;

                    while (null != line) {
                        Matcher hostm = hostp.matcher(line);
                        Matcher portm = portp.matcher(line);

                        if (hostm.matches()) {
                            String tmpHost = hostm.group(1);
                            if (InetAddress.getAllByName(tmpHost).length == 0) {
                                LOGGER.warn("in nsrllookup.ini:" +
                                        lineNumber +
                                        ", no DNS entry for " +
                                        lastSeenHost);
                            } else {
                                lastSeenHost = tmpHost;
                            }
                        }
                        if (portm.matches()) {
                            int tmpPort = Integer.parseInt(portm.group(1));
                            if (tmpPort < 0 || tmpPort > 65535) {
                                LOGGER.warn("in nsrllookup.ini:" +
                                        lineNumber +
                                        ", port '" + portm.group(1) +
                                        "' is invalid.");
                            } else {
                                lastSeenPort = tmpPort;
                            }
                        }
                        line = br.readLine();
                        lineNumber += 1;
                    }
                    configuredHost = lastSeenHost;
                    configuredPort = lastSeenPort;
                }
                catch (java.io.IOException fnfe) {
                    LOGGER.warn("error parsing config file!");
                }
            }
        }
        LOGGER.info("configured with nsrlsvr " +
                configuredHost + ":" + configuredPort);
        DEFAULT_HOST = configuredHost;
        DEFAULT_PORT = configuredPort;
        try {
            if (InetAddress.getAllByName(DEFAULT_HOST).length == 0) {
                LOGGER.warn("no DNS entry for " + DEFAULT_HOST);
            }
        } catch (UnknownHostException uhe) {
            LOGGER.warn("no DNS entry for " + DEFAULT_HOST);
        }
    }

    public Connection(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public Connection() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public Stream<String> hits(Stream<String> inputStream) {
        return hitOrMiss(inputStream, true);
    }

    public Stream<String> misses(Stream<String> inputStream) {
        return hitOrMiss(inputStream, false);
    }

    private Stream<String> hitOrMiss(Stream<String> inputStream, boolean horm) {
        final String[] hashes = new HashSet<>(inputStream.
                filter((String s) -> MD5RX.matcher(s).matches()).
                map(String::toUpperCase).
                collect(Collectors.toList())).
                toArray(String[]::new);
        final ArrayList<String> results = new ArrayList<>();
        final ArrayList<String> queries = new ArrayList<>();
        final String hoststr = host + ":" + port;
        final char match = horm ? '1' : '0';

        LOGGER.info("preparing to query " + hashes.length + " hashes");

        Arrays.sort(hashes);

        for (int i = 0 ; i < hashes.length ; i += CHUNK_SIZE) {
            StringBuilder sb = new StringBuilder();
            sb.append("QUERY");
            for (int j = i ; j < CHUNK_SIZE ; j += 1) {
                if (j >= hashes.length)
                    break;
                sb.append(" ");
                sb.append(hashes[j]);
            }
            sb.append("\r\n");
            queries.add(sb.toString());
        }

        Socket sock;
        BufferedReader reader;
        OutputStreamWriter writer;

        try {
            sock = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new OutputStreamWriter(sock.getOutputStream());
        }
        catch (IOException ioe) {
            LOGGER.warn("could not connect to " + hoststr);
            return results.stream();
        }

        try {
            writer.write("Version: 2.0\r\n");
            writer.flush();
            if (! reader.readLine().trim().equals("OK")) {
                LOGGER.warn("failed handshake with " + hoststr);
                return results.stream();
            }

            for (int i = 0 ; i < queries.size() ; i += 1) {
                writer.write(queries.get(i));
                writer.flush();
                Matcher m = GOODRX.matcher(reader.readLine().trim());
                if (! m.matches()) {
                    LOGGER.warn("server has failed!");
                    results.clear();
                    return results.stream();
                }
                String response = m.group(1);
                for (int j = 0 ; j < response.length() ; j += 1) {
                    if (response.charAt(j) == match) {
                        results.add(hashes[(i * CHUNK_SIZE) + j]);
                    }
                }
            }
            writer.write("BYE\r\n");
            writer.flush();
            sock.close();
        }
        catch (IOException ioe) {
            LOGGER.warn("unexpected failure: " + ioe.toString());
            results.clear();
        }
        return results.stream();
    }
}
