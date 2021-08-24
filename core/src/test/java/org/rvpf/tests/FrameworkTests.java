/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FrameworkTests.java 4062 2019-06-07 12:51:24Z SFB $
 */

package org.rvpf.tests;

import java.io.PrintWriter;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.apache.logging.dumbster.smtp.SimpleSmtpServer;
import org.apache.logging.dumbster.smtp.SmtpMessage;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Profiler;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.UncaughtExceptionHandler;
import org.rvpf.base.util.Version;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceImpl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.processor.TestScenarioFactory;

import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.TestNGException;
import org.testng.TestRunner;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * Framework tests.
 */
@Parameters(
    separators = " :=",
    resourceBundle = "org.rvpf.tests.messages.tests-usage"
)
public final class FrameworkTests
{
    /**
     * Constructs an instance.
     */
    private FrameworkTests()
    {
        _instance = this;
    }

    /**
     * Main.
     *
     * @param args Program arguments.
     */
    public static void main(@Nonnull String[] args)
    {
        Thread
            .setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

        System.setProperty("rvpf.uuid.mac", "");
        System.setProperty("line.separator", "\n");

        final String options = System.getenv(OPTIONS_ENV);

        if (options != null) {
            args = Stream
                .concat(
                    Arrays.stream(options.split("\\s")),
                    Arrays.stream(args))
                .toArray(String[]::new);
        }

        final FrameworkTests tests = new FrameworkTests();
        final JCommander commander = new JCommander(tests);

        try {
            commander.parse(args);
        } catch (final ParameterException exception) {
            System.err.println(exception.getMessage());
            _usage(commander);
            System.exit(-1);
        }

        if (tests._help) {
            _usage(commander);
            System.exit(0);
        }

        tests._run();
    }

    /**
     * Gets the framework tests instance.
     *
     * @return The framework tests instance.
     */
    @Nonnull
    @CheckReturnValue
    static FrameworkTests getInstance()
    {
        return _instance;
    }

    /**
     * Clears the mail.
     */
    void clearMail()
    {
        synchronized (_smtpServer) {
            _smtpServer.getReceivedEmail().clear();
            _mail = null;
        }
    }

    /**
     * Waits for mail.
     *
     * @param timeout The timeout in milliseconds.
     *
     * @return The optional mail.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Mail> waitForMail(int timeout)
        throws InterruptedException
    {
        final Mail mail;

        synchronized (_smtpServer) {
            if (_mail == null) {
                final long startMillis = System.currentTimeMillis();

                for (;;) {
                    final List<SmtpMessage> messages = _smtpServer
                        .getReceivedEmail();

                    if (!messages.isEmpty()) {
                        _mail = new Mail(messages.get(0));

                        break;
                    }

                    timeout -= System.currentTimeMillis() - startMillis;

                    if (timeout <= 0) {
                        break;
                    }

                    _smtpServer.wait(timeout);
                }
            }

            mail = _mail;
        }

        return Optional.ofNullable(mail);
    }

    private static void _logStackTraces(final Logger logger)
    {
        final PrintWriter printWriter = logger
            .getPrintWriter(
                logger.isTraceEnabled()
                ? Logger.LogLevel.TRACE: Logger.LogLevel.DEBUG);
        final Map<Thread, StackTraceElement[]> stackTraces = Thread
            .getAllStackTraces();

        for (final Map.Entry<Thread, StackTraceElement[]> entry:
                stackTraces.entrySet()) {
            Profiler
                .printStackTrace(
                    entry.getKey(),
                    entry.getValue(),
                    Optional.empty(),
                    Integer.MAX_VALUE,
                    printWriter);
        }
    }

    private static void _usage(final JCommander commander)
    {
        final StringBuilder stringBuilder = new StringBuilder();

        commander.setProgramName("run tests");
        commander.usage(stringBuilder);
        System.err.println(stringBuilder.toString());
    }

    private void _run()
    {
        final int smtpPort = Tests.allocateTCPPort();

        System.setProperty(SMTP_PORT_PROPERTY, String.valueOf(smtpPort));
        Logger.startUp(true);
        Logger.setLogID(Optional.of(LOG_ID));

        final Logger logger = Logger.getInstance(FrameworkTests.class);
        int exitCode = 0;

        try {
            final Set<String> testNames = new LinkedHashSet<String>();
            final Test tests;
            final AbstractListener listener;

            _smtpServer = SimpleSmtpServer.start(smtpPort);

            if (_verboseHalt || _verboseHaltThreads) {
                _verbose = true;
                _halt = true;
            }

            if (_lang != null) {
                final Locale locale = new Locale(
                    _lang,
                    Locale.getDefault().getCountry());

                try {
                    locale.getISO3Language();
                } catch (final MissingResourceException exception) {
                    throw new IllegalArgumentException(
                        Message.format(
                            TestsMessages.UNSUPPORTED_LANGUAGE,
                            locale.getLanguage()));
                }

                Locale.setDefault(locale);
            }

            if ((_scenarios != null) && (_scenarios.length() > 0)) {
                _testNames.add(SCENARIOS_TEST);
            }

            for (final String arg: _args) {
                if (arg.toLowerCase(Locale.ROOT).endsWith(SUITE_TYPE)) {
                    _suites.add(arg);
                } else if (arg.length() > 0) {
                    _testNames.add(arg);
                }
            }

            for (final String testName: _testNames) {
                testNames.add(testName.toLowerCase(Locale.ROOT));
            }

            new TestsVersion().logSystemInfo(getClass().getSimpleName());

            final Manifest manifest = Version.getManifest(TestNG.class);
            String version = Version
                .getManifestAttribute(
                    manifest,
                    Version.BUNDLE_VERSION_ATTRIBUTE);

            if (version == Version.PROBLEM_INDICATOR) {
                version = Version
                    .getManifestAttribute(
                        manifest,
                        Version.SPECIFICATION_VERSION_ATTRIBUTE);
            }

            logger.info(TestsMessages.TEST_NG_VERSION, version);

            tests = new Test(_results);

            if (_gui) {
                listener = new GUIListener(tests, _halt);
            } else {
                listener = new TUIListener(tests, _verbose, _halt);
            }

            tests.addListener((ITestNGListener) listener);
            tests
                .setOutputDirectory((_output != null)? _output: DEFAULT_OUTPUT);

            if (_suites.isEmpty()) {
                final URL suiteURL = Thread
                    .currentThread()
                    .getContextClassLoader()
                    .getResource(DEFAULT_SUITE);
                final String suitePath = (suiteURL != null)? suiteURL
                    .getPath(): "";

                if (!suitePath.isEmpty()) {
                    _suites.add(suitePath);
                }
            }

            tests.setTestSuites(_suites);

            if (!testNames.isEmpty()) {
                if (!tests
                    .keepTests(testNames, Optional.ofNullable(_scenarios))) {
                    exitCode = 1;

                    return;
                }
            }

            if ((_groups != null) && (_groups.length() > 0)) {
                tests.setGroups(_groups);
            }

            if (System.getProperty(REGISTRY_PORT_PROPERTY) == null) {
                System
                    .setProperty(
                        REGISTRY_PORT_PROPERTY,
                        Integer.toString(Tests.allocateTCPPort()));
            }

            final Thread mainThread = Thread.currentThread();

            Runtime
                .getRuntime()
                .addShutdownHook(
                    new Thread(
                        () -> {
                            mainThread.interrupt();

                            try {    // Prevents premature exit by the JVM.
                                mainThread.join();
                            } catch (final InterruptedException exception) {
                                throw new RuntimeException(exception);
                            }
                        },
                        "Tests main shutdown"));

            final _Runner runner = new _Runner(tests);

            runner
                .setUncaughtExceptionHandler(
                    new Thread.UncaughtExceptionHandler()
                    {
                        @Override
                        public void uncaughtException(
                                final Thread thread,
                                        final Throwable throwable)
                        {
                            logger.uncaughtException(thread, throwable);
                            tests.onFailure();
                            runner.interrupt();
                        }
                    });

            Profiler.start();

            runner.start();

            try {
                runner.join();
            } finally {
                _smtpServer.stop();
                Profiler.stop();
            }

            exitCode = runner.getExitCode();

            listener.waitForClose();
            ServiceThread.yieldAll();

            if (_threads || _verboseHaltThreads || logger.isTraceEnabled()) {
                _logStackTraces(logger);
            }
        } catch (final InterruptedException exception) {
            logger.warn(ServiceMessages.INTERRUPTED);
            exitCode = INTERRUPTED_CODE;
        } catch (final Throwable throwable) {
            logger.uncaughtException(Thread.currentThread(), throwable);
            exitCode = UNCAUGHT_CODE;
        } finally {
            ServiceImpl.cancelRestarters();
            logger.info(TestsMessages.END);
            Logger.shutDown();

            Runtime.getRuntime().halt(exitCode);
        }
    }

    /** Default output. */
    public static final String DEFAULT_OUTPUT = "tests/results";

    /** Default suite. */
    public static final String DEFAULT_SUITE = "testng.xml";

    /** Exception code. */
    public static final int EXCEPTION_CODE = -3;

    /** Failure code. */
    public static final int FAILURE_CODE = -5;

    /** Interrupted code. */
    public static final int INTERRUPTED_CODE = -4;

    /** Log ID. */
    public static final String LOG_ID = "TEST";

    /** Options env. */
    public static final String OPTIONS_ENV = "RVPF_TESTS_OPTIONS";

    /** Registry port property. */
    public static final String REGISTRY_PORT_PROPERTY =
        Config.SYSTEM_PROPERTY_PREFIX + "rmi.registry.port";

    /** Scenarios option. */
    public static final String SCENARIOS_OPT = "-scenarios";

    /** Scenarios test. */
    public static final String SCENARIOS_TEST = "processor-scenarios";

    /** SMTP port property. */
    public static final String SMTP_PORT_PROPERTY = "log4j.smtp.port";

    /** Suite type. */
    public static final String SUITE_TYPE = ".xml";

    /** TestNG code. */
    public static final int TEST_NG_CODE = -1;

    /** Uncaught code. */
    public static final int UNCAUGHT_CODE = -6;
    private static FrameworkTests _instance;

    @Parameter(descriptionKey = "TESTS")
    private List<String> _args = new ArrayList<>();
    @Parameter(
        names =
        {
            "-g", "-group", "--group"
        },
        descriptionKey = "GROUPS"
    )
    private String _groups;
    @Parameter(
        names =
        {
            "-gui", "--gui"
        },
        descriptionKey = "GUI"
    )
    private boolean _gui;
    @Parameter(
        names =
        {
            "-halt", "--halt"
        },
        descriptionKey = "HALT"
    )
    private boolean _halt;
    @Parameter(
        names =
        {
            "-h", "-help", "--help"
        },
        descriptionKey = "HELP"
    )
    private boolean _help;
    @Parameter(
        names =
        {
            "-lang", "--lang"
        },
        descriptionKey = "LANG"
    )
    private String _lang;
    private Mail _mail;
    @Parameter(
        names =
        {
            "-o", "-output", "--output"
        },
        descriptionKey = "OUTPUT"
    )
    private String _output;
    @Parameter(
        names =
        {
            "-r", "-results", "--results"
        },
        descriptionKey = "RESULTS"
    )
    private boolean _results;
    @Parameter(
        names =
        {
            "-s", "-scenario", "-scenarios", "--scenario", "--scenarios"
        },
        descriptionKey = "SCENARIOS"
    )
    private String _scenarios;
    private SimpleSmtpServer _smtpServer;
    private final List<String> _suites = new LinkedList<String>();
    private final List<String> _testNames = new LinkedList<>();
    @Parameter(
        names =
        {
            "-threads", "--threads"
        },
        descriptionKey = "THREADS"
    )
    private boolean _threads;
    @Parameter(
        names =
        {
            "-v", "-verbose", "--verbose"
        },
        descriptionKey = "VERBOSE"
    )
    private boolean _verbose;
    @Parameter(
        names = {"-vh"},
        descriptionKey = "VH",
        hidden = true
    )
    private boolean _verboseHalt;
    @Parameter(
        names = {"-vht"},
        descriptionKey = "VHT",
        hidden = true
    )
    private boolean _verboseHaltThreads;

    /**
     * Mail.
     */
    public static final class Mail
    {
        /**
         * Constructs an instance.
         *
         * @param message The message.
         */
        Mail(@Nonnull final SmtpMessage message)
        {
            _message = message;
        }

        /**
         * Gets the body.
         *
         * @return The body.
         */
        @Nonnull
        @CheckReturnValue
        public String getBody()
        {
            return _message.getBody();
        }

        /**
         * Gets the 'from' string.
         *
         * @return The optional 'from' string.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<String> getFrom()
        {
            return Optional.ofNullable(_message.getHeaderValue(_FROM_HEADER));
        }

        /**
         * Gets the recipient.
         *
         * @return The optional recipient.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<String> getRecipient()
        {
            return Optional.ofNullable(_message.getHeaderValue(_TO_HEADER));
        }

        private static final String _FROM_HEADER = "From";
        private static final String _TO_HEADER = "To";

        private final SmtpMessage _message;
    }


    /**
     * Failure exception.
     */
    static class FailureException
        extends RuntimeException
    {
        /**
         * Constructs an instance.
         *
         * @param message The exception message.
         */
        FailureException(@Nonnull final String message)
        {
            super(Require.notNull(message));
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Test.
     */
    static class Test
        extends TestNG
    {
        /**
         * Constructs an instance.
         *
         * @param results True if results are requested.
         */
        Test(final boolean results)
        {
            super(results);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean hasFailure()
        {
            return (_status & HAS_FAILURE) != 0;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setTestSuites(final List<String> suites)
        {
            super.setTestSuites(suites);
            initializeSuitesAndJarFile();    // Forces initialization.
            super.setTestSuites(new ArrayList<String>());    // Avoids repeat.
        }

        /**
         * Gets the exit code.
         *
         * @return The exit code.
         */
        @CheckReturnValue
        int getExitCode()
        {
            return (getStatus() | _status) & ~HAS_SKIPPED;
        }

        /**
         * Selects the tests to keep.
         *
         * @param testNames The names of the tests.
         * @param scenarios An optional comma delimited list of scenarios.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean keepTests(
                @Nonnull final Set<String> testNames,
                @Nonnull final Optional<String> scenarios)
        {
            final Collection<Pattern> patterns = new ArrayList<>(
                testNames.size());

            for (final Iterator<String> i =
                    testNames.iterator(); i.hasNext(); ) {
                String testName = i.next();

                testName = testName.toLowerCase(Locale.ROOT);

                if (_WILD_PATTERN.matcher(testName).lookingAt()) {
                    i.remove();
                } else if (testName.startsWith("=")
                           || testName.startsWith("+")) {
                    testName = testName + '*';
                    i.remove();
                }

                if (testName.startsWith("=") || testName.startsWith("+")) {
                    testName = testName.substring(1);
                }

                patterns.add(ValueConverter.wildToPattern(testName));
            }

            final Set<String> knownNames = new LinkedHashSet<>();
            final Set<String> namesFound = new HashSet<String>();
            boolean success = true;

            for (final Iterator<XmlSuite> i =
                    m_suites.iterator(); i.hasNext(); ) {
                final XmlSuite suite = i.next();

                tests: for (final Iterator<XmlTest> j =
                        suite.getTests().iterator();
                        j.hasNext(); ) {
                    final XmlTest test = j.next();
                    final String testName = test.getName();
                    final String lowerTestName = testName
                        .toLowerCase(Locale.ROOT);

                    knownNames.add(testName);

                    for (final Pattern pattern: patterns) {
                        if (pattern.matcher(lowerTestName).matches()) {
                            namesFound.add(lowerTestName);

                            if ((scenarios.isPresent())
                                    && lowerTestName.equals(SCENARIOS_TEST)) {
                                test
                                    .addParameter(
                                        TestScenarioFactory.SCENARIOS_ELEMENT,
                                        scenarios.get());
                            }

                            continue tests;
                        }
                    }

                    j.remove();
                }

                if (suite.getTests().isEmpty()) {
                    i.remove();
                }
            }

            for (final String name: testNames) {
                if (!namesFound.contains(name)) {
                    System.err
                        .println(
                            Message.format(TestsMessages.TEST_NOT_FOUND, name));
                    success = false;
                }
            }

            if (success && m_suites.isEmpty()) {
                System.err.println(Message.format(TestsMessages.NO_TEST_FOUND));
                success = false;
            }

            if (!success) {
                System.err.println(Message.format(TestsMessages.KNOWN_TESTS));

                for (final String name: knownNames) {
                    System.err.println("\t" + name);
                }
            }

            return success;
        }

        /**
         * Called on failure.
         */
        void onFailure()
        {
            _status |= HAS_FAILURE;
        }

        private static final Pattern _WILD_PATTERN = Pattern.compile(".*?[*?]");

        private int _status;
    }


    private static class _Runner
        extends Thread
    {
        /**
         * Constructs an instance.
         *
         * @param tests The Tests instance.
         */
        _Runner(@Nonnull final Test tests)
        {
            super("Tests runner");

            _tests = tests;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            Logger.setLogID(Optional.of(LOG_ID));

            try {
                _tests.run();
                _exitCode = _tests.getExitCode();
            } catch (final TestNGException exception) {
                if (TestRunner.getVerbose() > 0) {
                    exception.printStackTrace();
                }

                Logger
                    .getInstance(getClass())
                    .error(exception, TestsMessages.TEST_NG_EXCEPTION);
                _exitCode = TEST_NG_CODE;
            } catch (final FailureException exception) {
                Logger
                    .getInstance(getClass())
                    .warn(
                        TestsMessages.HALT_ON_FAILURE,
                        exception.getMessage());
                _exitCode = FAILURE_CODE;
            } catch (final Throwable throwable) {
                Logger
                    .getInstance(getClass())
                    .fatal(throwable, TestsMessages.EXIT_ON_EXCEPTION);
                _exitCode = EXCEPTION_CODE;
            }
        }

        /**
         * Gets the exit code.
         *
         * @return The exit code.
         */
        @CheckReturnValue
        int getExitCode()
        {
            return _exitCode;
        }

        private int _exitCode;
        private final Test _tests;
    }
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
