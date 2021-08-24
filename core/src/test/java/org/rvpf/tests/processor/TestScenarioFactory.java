/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TestScenarioFactory.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.processor;

import java.io.Reader;

import java.net.URL;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.Config;
import org.rvpf.container.ContainerServiceActivator;
import org.rvpf.container.ContainerServiceImpl;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.document.loader.DocumentReaderProxy;
import org.rvpf.metadata.Metadata;
import org.rvpf.processor.ProcessorServiceActivator;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Parameters;

/**
 * Test scenario factory.
 */
public final class TestScenarioFactory
    extends StoreClientTests
{
    /**
     * Creates test scenarios.
     *
     * @param scenarios Comma delimited list of scenarios.
     *
     * @return The test scenarios.
     *
     * @throws Exception On failure.
     */
    @Factory
    @Parameters({"scenarios"})
    public Object[] createScenarios(
            @org.testng.annotations.Optional("") final String scenarios)
        throws Exception
    {
        final Config config;

        setSystemProperty(Config.RVPF_PROPERTIES, TESTS_PROPERTIES);

        try {
            config = ConfigDocumentLoader
                .loadConfig("", Optional.empty(), Optional.empty());
        } finally {
            clearSystemProperty(Config.RVPF_PROPERTIES);
        }

        if (config == null) {
            return new Object[0];
        }

        final Set<Integer> selection = new HashSet<Integer>();

        for (String position: scenarios.split(",")) {
            position = position.trim();

            if (position.length() > 0) {
                try {
                    selection.add(Integer.valueOf(position));
                } catch (final NumberFormatException exception) {
                    Require.failure("Bad scenario position:" + position);
                }
            }
        }

        final List<TestScenario> scenariosList = new LinkedList<TestScenario>();
        final String scenariosLocation;
        final URL scenariosURL;
        final XMLDocument scenariosDocument;
        final XMLElement scenariosElement;
        int position = 0;

        Require.notNull(config, "Loaded config");
        scenariosLocation = config
            .getStringValue(TESTS_SCENARIOS_PROPERTY)
            .get();

        scenariosURL = config
            .createURL(scenariosLocation, Optional.of(config.getURL()));
        Require.notNull(scenariosURL, "Scenarios URL");
        scenariosDocument = _loadDocument(scenariosURL);
        scenariosElement = scenariosDocument.getRootElement();
        Require
            .equal(
                scenariosElement.getName(),
                SCENARIOS_ELEMENT,
                "Root element for scenarios");

        for (final XMLElement element: scenariosElement.getChildren()) {
            ++position;

            if (!selection.isEmpty()
                    && !selection.contains(Integer.valueOf(position))) {
                continue;
            }

            final String from = element
                .getAttributeValue(FROM_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final XMLElement scenarioElement;

            if (from != null) {
                final URL scenarioURL = config
                    .createURL(from, Optional.of(scenariosURL));

                Require.notNull(scenarioURL, "Scenario URL");

                final XMLDocument scenarioDocument = _loadDocument(scenarioURL);

                scenarioElement = scenarioDocument.getRootElement();
                Require
                    .equal(
                        scenarioElement.getName(),
                        SCENARIO_ELEMENT,
                        "Root element for scenario");
                Require
                    .equal(
                        null,
                        scenarioElement
                            .getAttributeValue(FROM_ATTRIBUTE, Optional.empty())
                            .orElse(null),
                        "The '" + FROM_ATTRIBUTE + "' attribute");

                if (scenarioElement
                    .getAttributeValue(
                        TestScenario.TITLE_ATTRIBUTE,
                        Optional.empty())
                    .orElse(null) == null) {
                    scenarioElement
                        .setAttribute(
                            TestScenario.TITLE_ATTRIBUTE,
                            element
                                .getAttributeValue(
                                        TestScenario.TITLE_ATTRIBUTE,
                                                Optional.empty()));
                }

                if (!scenarioElement
                    .getAttributeValue(
                        TestScenario.TIME_ATTRIBUTE,
                        Optional.empty())
                    .isPresent()) {
                    scenarioElement
                        .setAttribute(
                            TestScenario.TIME_ATTRIBUTE,
                            element
                                .getAttributeValue(
                                        TestScenario.TIME_ATTRIBUTE,
                                                Optional.empty()));
                }

                Require
                    .success(
                        element.getChildren().size() == 0,
                        "Number of scenario elements");
            } else {
                scenarioElement = element;
            }

            Require
                .present(
                    scenarioElement
                        .getAttributeValue(
                                TestScenario.TITLE_ATTRIBUTE,
                                        Optional.empty()),
                    "The '" + TestScenario.TITLE_ATTRIBUTE + "' attribute");

            scenariosList
                .add(new TestScenario(scenarioElement, this, position));
        }

        return scenariosList.toArray();
    }

    /**
     * Gets the notice receiver.
     *
     * @return The notice receiver.
     */
    public MessagingSupport.Receiver getNoticeReceiver()
    {
        return _noticeReceiver;
    }

    /**
     * Gets the notice sender.
     *
     * @return The notice sender.
     */
    public MessagingSupport.Sender getNoticeSender()
    {
        return _noticeSender;
    }

    /**
     * Gets the notice reception timeout.
     *
     * @return The notice reception timeout.
     */
    public long getNoticeTimeout()
    {
        return getTimeout();
    }

    /**
     * Gets the processor's metadata.
     *
     * @return The processor's metadata.
     */
    public Metadata getProcessorMetadata()
    {
        return getMetadata(_processorService);
    }

    /**
     * Gets the store's metadata.
     *
     * @return The store's metadata.
     */
    public Metadata getStoreMetadata()
    {
        return getMetadata(_storeService);
    }

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeTest
    public void setUp()
        throws Exception
    {
        setSystemProperty(Config.RVPF_PROPERTIES, TESTS_PROPERTIES);

        setUpAlerter();

        _noticeSender = getMessaging()
            .createServerSender(
                getConfig().getPropertiesGroup(_RECEPTIONIST_QUEUE_PROPERTIES));
    }

    /**
     * Starts the processor service.
     *
     * @throws Exception On failure.
     */
    public void startProcessor()
        throws Exception
    {
        _containerServiceActivator = startService(
            ContainerServiceActivator.class,
            Optional.empty());
        _storeService = ((ContainerServiceImpl) _containerServiceActivator
            .getService())
            .getServiceActivator(_STORE_SERVICE_ALIAS)
            .get();

        purgeStoreValues(_storeService);
        _noticeReceiver = getMessaging()
            .createClientReceiver(
                getMetadata(_storeService)
                    .getPropertiesGroup(NOTIFIER_QUEUE_PROPERTIES));
        _noticeReceiver.purge();

        _processorService = startService(
            ProcessorServiceActivator.class,
            Optional.empty());
    }

    /**
     * Stops the processor.
     *
     * @throws Exception On failure.
     */
    public void stopProcessor()
        throws Exception
    {
        if (_processorService != null) {
            stopService(_processorService);
            _processorService = null;
        }

        if (_noticeReceiver != null) {
            try {
                _noticeReceiver.commit();
                _noticeReceiver.purge();
            } catch (final Exception exception) {
                getThisLogger()
                    .warn(
                        exception,
                        BaseMessages.VERBATIM,
                        exception.getMessage());
            }

            _noticeReceiver.close();
            _noticeReceiver = null;
        }

        if (_storeService != null) {
            purgeStoreValues(_storeService);
            _storeService = null;
        }

        if (_containerServiceActivator != null) {
            stopService(_containerServiceActivator);
            _containerServiceActivator = null;
        }
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterTest(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        stopProcessor();    // Needed on scenario failure.

        if (_noticeSender != null) {
            _noticeSender.close();
            _noticeSender = null;
        }

        tearDownAlerter();
    }

    /**
     * Sends a midnight event.
     *
     * @throws Exception On failure.
     */
    void sendMidnightEvent()
        throws Exception
    {
        sendEvent(Service.MIDNIGHT_EVENT, Optional.empty());
    }

    private static XMLDocument _loadDocument(final URL fromURL)
        throws Exception
    {
        final Reader documentReader = DocumentReaderProxy
            .create(fromURL, Optional.empty(), Optional.empty());
        final XMLDocument document = new XMLDocument();

        document.parse(documentReader);

        return document;
    }

    /** From attribute. */
    public static final String FROM_ATTRIBUTE = "from";

    /** Scenarios element. */
    public static final String SCENARIOS_ELEMENT = "scenarios";

    /** Scenario element. */
    public static final String SCENARIO_ELEMENT = "scenario";

    /** Tests properties file. */
    public static final String TESTS_PROPERTIES =
        "scenarios/rvpf-scenarios.properties";

    /** Tests scenarios property. */
    public static final String TESTS_SCENARIOS_PROPERTY = "tests.scenarios";
    private static final String _RECEPTIONIST_QUEUE_PROPERTIES =
        "tests.processor.receptionist.queue";
    private static final String _STORE_SERVICE_ALIAS = "the-store";

    private ServiceActivator _containerServiceActivator;
    private MessagingSupport.Receiver _noticeReceiver;
    private MessagingSupport.Sender _noticeSender;
    private ServiceActivator _processorService;
    private ServiceActivator _storeService;
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
