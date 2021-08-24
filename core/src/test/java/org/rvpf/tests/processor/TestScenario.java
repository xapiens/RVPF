/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TestScenario.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test a scenario.
 */
public final class TestScenario
    extends ServiceTests
    implements ITest
{
    /**
     * Creates a new test scenario.
     *
     * @param scenario The scenario element describing the scenario.
     * @param factory The scenario factory.
     * @param position The scenario's position.
     */
    TestScenario(
            final XMLElement scenario,
            final TestScenarioFactory factory,
            final int position)
    {
        _scenario = scenario;
        _factory = factory;
        _position = position;

        _name = _scenario
            .getAttributeValue(TITLE_ATTRIBUTE, Optional.of(""))
            .orElse(null);
        _casting = _scenario.getFirstChild(CASTING_ELEMENT).get();
        _director = _casting
            .getAttributeValue(DIRECTOR_ATTRIBUTE, Optional.empty())
            .orElse(null);

        _dateTimeContext = new DateTime.Context(_GMT_TIME_ZONE);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTestName()
    {
        final String name;

        if (_director != null) {
            name = "Processor scenario " + _position + " \"" + _name + '"';
        } else {
            name = _name;
        }

        return name;
    }

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeMethod
    public void setUpScenario()
        throws Exception
    {
        quell(ProcessorMessages.OUT_RESYNC_RANGE);
        setProperty(TESTS_SCENARIO_PROPERTY, _name);
        getThisLogger().debug(CoreTestsMessages.CASTING_DIRECTOR, _director);
        setProperty(_director, "!");

        _factory.startProcessor();
        _noticeTimeout = _factory.getNoticeTimeout();

        _castActors();

        _ignoreDeleted = true;
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        clearProperty(_director);
        _casts = null;

        if (_store != null) {
            _store.close();
            _store = null;
        }

        _factory.stopProcessor();

        _scenario = null;
    }

    /**
     * Tests the scenario.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testScenario()
        throws Exception
    {
        _logElement(
            LogLevel.INFO,
            CoreTestsMessages.SCENARIO_CONTEXT,
            _scenario);

        final String timeString = _scenario
            .getAttributeValue(TIME_ATTRIBUTE, Optional.of(""))
            .orElse(null);

        _time = _dateTime(timeString);
        _noticesSent = 0;

        try {
            DateTime.simulateTime(_time);
            getThisLogger()
                .debug(
                    CoreTestsMessages.NOW,
                    _dateTimeContext.toString(DateTime.now()));

            for (final XMLElement scene: _scenario.getChildren(SCENE_ELEMENT)) {
                _testScene(scene);
            }
        } finally {
            DateTime.clearSimulatedTime();
        }

        if (_noticesSent > 0) {
            getThisLogger()
                .warn(
                    CoreTestsMessages.SCENARIO_NOTICES,
                    _name,
                    Integer.valueOf(_noticesSent));
        }

        _factory.getNoticeSender().commit();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final String scenarioName = (_scenario != null)? _name: "?";

        return "Scenario \"" + scenarioName + '"';
    }

    private static void _updateStore(
            final PointValue pointValue,
            final String line)
        throws Exception
    {
        if (!pointValue.updateStore()) {
            Require.failure("Update failed (" + line + ")");
        }
    }

    private void _castActors()
    {
        _casts = new HashMap<String, PointEntity>();

        for (final XMLElement cast: _casting.getChildren(CAST_ELEMENT)) {
            if (getThisLogger().isDebugEnabled()) {
                _logElement(
                    LogLevel.DEBUG,
                    CoreTestsMessages.CASTING_CONTEXT,
                    cast);
            }

            final String role = cast
                .getAttributeValue(ROLE_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String actor = cast
                .getAttributeValue(ACTOR_ATTRIBUTE, Optional.of(role))
                .orElse(null);
            final Metadata metadata = _factory.getProcessorMetadata();
            final PointEntity processorPoint = (PointEntity) metadata
                .getPointByName(actor)
                .orElse(null);

            if (processorPoint != null) {
                final PointEntity scenarioPoint = processorPoint.copy();
                final Optional<ContentEntity> contentEntity = processorPoint
                    .getContentEntity();

                if (contentEntity.isPresent()) {
                    scenarioPoint.setContentEntity(contentEntity.get());
                } else {
                    scenarioPoint.clearContentEntity();
                }

                // cleanup.
                scenarioPoint
                    .setStoreEntity(
                        Optional
                            .of(scenarioPoint.getStoreEntity().get().copy()));

                if (_store == null) {
                    final boolean connected = scenarioPoint
                        .setUpStore(metadata);

                    Require.success(connected, "Connected to store");
                    _store = scenarioPoint.getStore().get();
                } else {
                    scenarioPoint
                        .getStoreEntity()
                        .get()
                        .setInstance((Proxied) _store);
                }

                _casts.put(role, scenarioPoint);
            } else {
                getThisLogger().warn(CoreTestsMessages.POINT_UNKNOWN, actor);
            }
        }
    }

    private DateTime _dateTime(@Nonnull String timeString)
    {
        DateTime time;

        timeString = timeString.trim();

        if (timeString.startsWith("+") || timeString.startsWith("-")) {
            final String[] fields = _SPLITTER.split(timeString, 2);
            int days;

            if (fields[0].startsWith("+")) {
                fields[0] = fields[0].substring(1);
            }

            days = Integer.parseInt(fields[0]);
            time = _dateTimeContext.midnight(_time);

            while (days != 0) {
                if (days < 0) {
                    time = _dateTimeContext.midnight(time.before());
                    ++days;
                } else {
                    time = _dateTimeContext.nextDay(time);
                    --days;
                }
            }

            if (fields.length > 1) {
                time = _dateTimeContext
                    .fromString(fields[1], Optional.of(time));
            }
        } else {
            time = _dateTimeContext.fromString(timeString, Optional.of(_time));
        }

        return time;
    }

    private void _doBreak(final String line)
    {
        getThisLogger().debug(CoreTestsMessages.BREAKPOINT_OPPORTUNITY, line);
    }

    private void _logElement(
            final LogLevel logLevel,
            final CoreTestsMessages context,
            final XMLElement element)
    {
        final StringBuilder stringBuilder = new StringBuilder(
            context.toString());

        if (logLevel.compareTo(LogLevel.INFO) > 0) {
            stringBuilder.append(": ");
            stringBuilder.append(element.getName());
        }

        for (XMLAttribute attribute: element.getAttributes()) {
            stringBuilder.append(' ');
            stringBuilder.append(attribute.getName());
            stringBuilder.append("='");
            stringBuilder.append(attribute.getValue());
            stringBuilder.append("'");
        }

        getThisLogger().log(logLevel, BaseMessages.VERBATIM, stringBuilder);
    }

    private void _testScene(final XMLElement scene)
        throws Exception
    {
        final MessagingSupport.Sender noticeSender = _factory.getNoticeSender();
        final MessagingSupport.Receiver noticeReceiver = _factory
            .getNoticeReceiver();
        final Metadata processorMetadata = _factory.getProcessorMetadata();
        boolean entryHeld = false;

        _logElement(LogLevel.INFO, CoreTestsMessages.SCENE_CONTEXT, scene);

        final DateTime time = _time(scene);

        if (time.isAfter(_time)) {
            _time = time;

            if (_time.equals(_dateTimeContext.midnight(_time))) {
                _factory.sendMidnightEvent();
            }
        }

        for (final XMLElement element: scene.getChildren()) {
            final String name = element.getName();
            final String line = element
                .getAttributeValue(LINE_ATTRIBUTE, Optional.of("?"))
                .orElse(null);
            boolean receiveNotice = true;
            boolean dropNotice = false;
            boolean commit = false;
            Point point = null;
            PointValue pointValue = null;

            if (getThisLogger().isDebugEnabled()) {
                _logElement(
                    LogLevel.DEBUG,
                    CoreTestsMessages.SCRIPT_CONTEXT,
                    element);
            }

            if (SILENCE_ELEMENT.equals(name)) {
                commit = true;
                receiveNotice = false;
                dropNotice = true;
                entryHeld = false;
                _time(element);
            } else if (BREAK_ELEMENT.equals(name)) {
                receiveNotice = false;
                dropNotice = true;
                _doBreak(line);
            } else {
                final String role = element
                    .getAttributeValue(ROLE_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                final String state = element
                    .getAttributeValue(STATE_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                final String text = element
                    .getAttributeValue(TEXT_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                final DateTime stamp = _time(element);

                point = _casts.get(role);
                Require.notNull(point, "Point casted as role '" + role + "'");

                pointValue = new PointValue(
                    point,
                    Optional.of(stamp),
                    state,
                    text)
                    .encoded();

                if (SET_ELEMENT.equals(name)) {
                    _updateStore(pointValue, line);
                    dropNotice = true;
                } else if (ENTRY_ELEMENT.equals(name)) {
                    _updateStore(pointValue, line);
                    dropNotice = false;
                    entryHeld = true;
                } else if (ACTION_ELEMENT.equals(name)) {
                    dropNotice = false;
                    commit = entryHeld;
                    entryHeld = false;
                } else if (EXIT_ELEMENT.equals(name)) {
                    dropNotice = true;
                    commit = entryHeld;
                    entryHeld = false;
                } else if (REPEAT_ELEMENT.equals(name)) {
                    noticeSender
                        .send(new RecalcTrigger(point, Optional.of(stamp)));
                    ++_noticesSent;
                    receiveNotice = false;
                    entryHeld = true;
                } else {
                    Require
                        .failure(
                            "The scene action '" + name + "' is unexpected");
                }
            }

            if (commit) {
                noticeSender.send(PointValue.NULL);
                noticeSender.commit();
                _noticesSent = 0;
            }

            if (receiveNotice) {
                Require.notNull(pointValue);

                PointValue notice;

                for (;;) {
                    notice = (PointValue) noticeReceiver
                        .receive(_noticeTimeout);
                    Require.notNull(notice, "Notice received at line " + line);

                    if (_ignoreDeleted) {
                        if (notice.isDeleted()) {
                            continue;
                        }

                        _ignoreDeleted = false;
                    }

                    break;
                }

                notice = notice.restore(processorMetadata);

                if (!notice.getPoint().isPresent()) {
                    Require
                        .failure(
                            "The point '" + notice.restore(
                                _factory.getStoreMetadata()).getPoint().get() + "' is unknown to the processor");
                }

                final DateTime expectedTime = pointValue
                    .getStamp()
                    .rounded(ElapsedTime.MILLI);
                final DateTime receivedTime = notice
                    .getStamp()
                    .rounded(ElapsedTime.MILLI);
                final String state = (notice
                    .getState() != null)? notice.getState().toString(): null;
                Object expectedValue = pointValue.getValue();
                Object receivedValue = notice.encoded().getValue();

                if ((expectedValue instanceof Double)
                        && (receivedValue instanceof Double)) {
                    expectedValue = Float
                        .valueOf(((Double) expectedValue).floatValue());
                    receivedValue = Float
                        .valueOf(((Double) receivedValue).floatValue());
                }

                if (!Objects.equals(point, notice.getPoint().get())
                        || !Objects.equals(expectedTime, receivedTime)
                        || !Objects.equals(pointValue.getState(), state)
                        || !Objects.equals(expectedValue, receivedValue)) {
                    Require
                        .failure(
                            "Unexpected notice (" + line + "): expected <"
                            + pointValue.toString(
                                _dateTimeContext) + "> but was <"
                                + notice.toString(
                                        _dateTimeContext) + ">");
                }

                if (!dropNotice) {
                    noticeSender.send(notice);
                    ++_noticesSent;
                }
            }
        }
    }

    private DateTime _time(final XMLElement element)
    {
        final String timeString = element
            .getAttributeValue(TIME_ATTRIBUTE, Optional.empty())
            .orElse(null);
        final DateTime time = (timeString != null)? _dateTime(
            timeString): _time;

        if (time.isAfter(DateTime.now())) {
            DateTime.simulateTime(time);
            getThisLogger()
                .debug(
                    CoreTestsMessages.NOW,
                    _dateTimeContext.toString(DateTime.now()));
        }

        return time;
    }

    /** Action element. */
    public static final String ACTION_ELEMENT = "action";

    /** Actor attribute. */
    public static final String ACTOR_ATTRIBUTE = "actor";

    /** Break element. */
    public static final String BREAK_ELEMENT = "break";

    /** Casting element. */
    public static final String CASTING_ELEMENT = "casting";

    /** Cast element. */
    public static final String CAST_ELEMENT = "cast";

    /** Director attribute. */
    public static final String DIRECTOR_ATTRIBUTE = "director";

    /** Entry element. */
    public static final String ENTRY_ELEMENT = "entry";

    /** Exit element. */
    public static final String EXIT_ELEMENT = "exit";

    /** Line attribute. */
    public static final String LINE_ATTRIBUTE = "line";

    /** Repeat element. */
    public static final String REPEAT_ELEMENT = "repeat";

    /** Role attribute. */
    public static final String ROLE_ATTRIBUTE = "role";

    /** Scene element. */
    public static final String SCENE_ELEMENT = "scene";

    /** Set element. */
    public static final String SET_ELEMENT = "set";

    /** Silence element. */
    public static final String SILENCE_ELEMENT = "silence";

    /** State attribute. */
    public static final String STATE_ATTRIBUTE = "state";

    /** Tests scenario property. */
    public static final String TESTS_SCENARIO_PROPERTY = "tests.scenario";

    /** Text attribute. */
    public static final String TEXT_ATTRIBUTE = "text";

    /** Time attribute. */
    public static final String TIME_ATTRIBUTE = "time";

    /** Title attribute. */
    public static final String TITLE_ATTRIBUTE = "title";
    private static final TimeZone _GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    private static final Pattern _SPLITTER = Pattern.compile("\\s");

    private final XMLElement _casting;
    private Map<String, PointEntity> _casts;
    private final DateTime.Context _dateTimeContext;
    private final String _director;
    private final TestScenarioFactory _factory;
    private boolean _ignoreDeleted;
    private final String _name;
    private long _noticeTimeout;
    private int _noticesSent;
    private final int _position;
    private XMLElement _scenario;
    private Store _store;
    private DateTime _time = DateTime.now();
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
