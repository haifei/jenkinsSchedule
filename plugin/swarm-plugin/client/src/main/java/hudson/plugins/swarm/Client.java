package hudson.plugins.swarm;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.logging.*;
import java.io.File;
import java.util.Arrays;

/**
 * Swarm client.
 *
 * Discovers nearby Jenkins via UDP broadcast, and pick eligible one randomly and
 * joins it.
 *
 * @author Kohsuke Kawaguchi
 */
public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getPackage().getName());

    private final Options options;
    private final Thread labelFileWatcherThread = null;

    public static void main(String... args) throws InterruptedException, IOException {
        String s = Arrays.toString(args);
        s = s.replaceAll("\n","");
        s = s.replaceAll("\r","");
        s = s.replaceAll(",", "");
        logger.info("Client.main invoked with: " + s);

        Options options = new Options();
        Client client = new Client(options);
        CmdLineParser p = new CmdLineParser(options);
        try {
            p.parseArgument(args);
        }
        catch (CmdLineException e) {
            logger.log(Level.SEVERE,"CmdLineException occurred during parseArgument", e);
            p.printUsage(System.out);
            System.exit(-1);
        }

        if (options.help) {
            p.printUsage(System.out);
            System.exit(0);
        }

        if(options.logFile != null) {
            logger.severe("-logFile has been deprecated. Use logging properties file syntax instead: -Djava.util.logging.config.file=" + Paths.get("").toAbsolutePath().toString() + File.separator + "logging.properties");
            System.exit(-1);
        }

        // Check to see if passwordEnvVariable is set, if so pull down the
        // password from the env and set as password.
        if (options.passwordEnvVariable != null) {
            options.password = System.getenv(options.passwordEnvVariable);
        }

        // Only look up the hostname if we have not already specified
        // name of the slave. Also in certain cases this lookup might fail.
        // E.g.
        // Querying a external DNS server which might not be informed
        // of a newly created slave from a DHCP server.
        //
        // From https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html#getCanonicalHostName--
        //
        // "Gets the fully qualified domain name for this IP
        // address. Best effort method, meaning we may not be able to
        // return the FQDN depending on the underlying system
        // configuration."
        if (options.name == null) {
            try {
                client.options.name = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch (IOException e) {
                logger.severe("Failed to lookup the canonical hostname of this slave, please check system settings.");
                logger.severe("If not possible to resolve please specify a node name using the '-name' option");
                System.exit(-1);
            }
        }

        SwarmClient swarmClient = new SwarmClient(options);
        client.run(swarmClient, args); // pass the command line arguments along so that the LabelFileWatcher thread can have them
    }

    public Client(Options options) {
        this.options = options;
        logger.finest("Client created with " + options);
    }

    /**
     * Finds a Jenkins master that supports swarming, and join it.
     *
     * This method never returns.
     */
    public void run(SwarmClient swarmClient, String... args) throws InterruptedException {
        logger.info("Discovering Jenkins master");

        // The Jenkins that we are trying to connect to.
        Candidate target;

        // wait until we get the ACK back
        int retry = 0;
        Thread averageThread = null;
        while (true) {
            try {
                if (options.master == null) {
                    logger.info("No Jenkins master supplied on command line, performing auto-discovery");
                    target = swarmClient.discoverFromBroadcast();
                } else {
                    target = swarmClient.discoverFromMasterUrl();
                }

                if (options.password == null && options.username == null) {
                    swarmClient.verifyThatUrlIsHudson(target);
                }

                // set up label file watcher thread (if the label file changes, this thread takes action to restart the client)
                if(options.labelsFile != null && labelFileWatcherThread == null) {
                    logger.info("Setting up LabelFileWatcher");
                    LabelFileWatcher l = new LabelFileWatcher(target, options, args);
                    Thread labelFileWatcherThread = new Thread(l, "LabelFileWatcher");
                    labelFileWatcherThread.setDaemon(true);
                    labelFileWatcherThread.start();
                }

                logger.info("Attempting to connect to " + target.url + " " + target.secret + " with ID " +
                                   swarmClient.getHash());

                // create a new swarm slave
                swarmClient.createSwarmSlave(target);

                // update node system load average
                averageThread = new SystemLoadAverageThread(swarmClient,target);
                averageThread.start();

                // block method
                swarmClient.connect(target);

                if (options.noRetryAfterConnected) {
                    logger.warning("Connection closed, exiting...");
                    swarmClient.exitWithStatus(0);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOexception occurred", e);
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                logger.log(Level.SEVERE, "ParserConfigurationException occurred", e);
                e.printStackTrace();
            } catch (RetryException e) {
                logger.log(Level.SEVERE, "RetryException occurred", e);
                if (e.getCause() != null) {
                    e.getCause().printStackTrace();
                }
            }finally {
                if(averageThread != null){
                    averageThread.interrupt();
                    logger.log(Level.WARNING,"Stopping System Load Average Thread.Status:" + averageThread.isInterrupted());
                }
            }

            int waitTime = options.retryBackOffStrategy.waitForRetry(retry++, options.retryInterval, options.maxRetryInterval);
            if (options.retry >= 0) {
                if (retry >= options.retry) {
                    logger.severe("Retry limit reached, exiting...");
                    swarmClient.exitWithStatus(-1);
                } else {
                    logger.warning("Remaining retries: " + (options.retry - retry));
                }
            }

            // retry
            logger.info("Retrying in " + waitTime + " seconds");
            swarmClient.sleepSeconds(waitTime);
        }
    }
}
