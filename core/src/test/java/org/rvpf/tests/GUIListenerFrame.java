/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GUIListenerFrame.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.rvpf.tests.GUIListener.State;

/**
 * GUI listener frame.
 */
final class GUIListenerFrame
    extends JFrame
    implements Runnable
{
    /**
     * Creates a new GUIListenerDialog.
     *
     * @param listener The GUI Listener.
     */
    GUIListenerFrame(@Nonnull final GUIListener listener)
    {
        _listener = listener;
        _succeeded = 0;
        _qualified = 0;
        _skipped = 0;
        _failed = 0;
        _total = 0;
        _state = null;

        _invokeAndWait(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        _initComponents();
        setVisible(true);
        toFront();
    }

    /**
     * Gets the listener.
     *
     * @return The listener.
     */
    @Nonnull
    @CheckReturnValue
    GUIListener getListener()
    {
        return _listener;
    }

    /**
     * Gets the quit button.
     *
     * @return The quit button.
     */
    @Nonnull
    @CheckReturnValue
    JButton getQuitButton()
    {
        return _quitButton;
    }

    /**
     * Called when a configuration method fails.
     */
    void onConfigurationFailure()
    {
        _invokeAndWait(this::onFailure);
    }

    /**
     * Called when all the tests are done.
     */
    void onDone()
    {
        _progressBar.setMaximum(100);
        _progressBar.setValue(100);
        _progressBar.setIndeterminate(false);
        _progressBar.setStringPainted(true);

        _quitButton.setText(_DONE_TEXT);
        getRootPane().setDefaultButton(_quitButton);
    }

    /**
     * Called on failure.
     */
    void onFailure()
    {
        if ((_state == null) || (State.FAILED.compareTo(_state) > 0)) {
            _state = State.FAILED;
            _progressBar.setForeground(_FAILED_COLOR);
        }
    }

    /**
     * Called on the resize event.
     *
     * @param event The resize event.
     */
    void onResize(@Nonnull final ComponentEvent event)
    {
        final Dimension minimumSize = getMinimumSize();
        final DefaultListModel<String> listModel =
            (DefaultListModel<String>) _testsList
                .getModel();
        boolean resize = false;
        int width = getWidth();
        int height = getHeight();

        if (width < minimumSize.width) {
            width = minimumSize.width;
            resize = true;
        }

        if (height < minimumSize.height) {
            height = minimumSize.height;
            resize = true;
        }

        if (resize) {
            setSize(width, height);
        }

        if (listModel.getSize() > 0) {
            _testsList.ensureIndexIsVisible(listModel.getSize() - 1);
        }
    }

    /**
     * Called when a test suite starts.
     *
     * @param name The suite's name.
     */
    void onSuiteStart(@Nonnull final String name)
    {
        _invokeAndWait(() -> setTitle(_TITLE + ": " + name));
    }

    /**
     * Called when the test result is available.
     *
     * @param state The test's completion state.
     * @param elapsed Elapsed time.
     */
    void onTestResult(@Nonnull final State state, final float elapsed)
    {
        _invokeAndWait(() -> showTestResult(state, elapsed));
        _identified = false;
    }

    /**
     * Called when a test starts.
     *
     * @param name The name of the test.
     */
    void onTestStart(@Nonnull final String name)
    {
        if (!_identified) {
            _invokeAndWait(() -> showTestName(name));
            _identified = true;
        }
    }

    /**
     * Called when all the tests are done.
     */
    void onTestsDone()
    {
        _invokeAndWait(this::onDone);
    }

    /**
     * Shows the name of a new test.
     *
     * @param name The name of the test.
     */
    void showTestName(@Nonnull final String name)
    {
        final DefaultListModel<String> listModel =
            (DefaultListModel<String>) _testsList
                .getModel();

        listModel.addElement(name + ": ");
        _testsList.ensureIndexIsVisible(listModel.getSize() - 1);
    }

    /**
     * Shows the result of a test.
     *
     * @param state The test's completion state.
     * @param elapsed Elapsed time.
     */
    void showTestResult(@Nonnull final State state, final float elapsed)
    {
        final String comment;

        switch (state) {
            case SUCCEEDED: {
                comment = _SUCCEEDED_COMMENT;
                _succeededText.setText(_SUCCEEDED_TEXT + ": " + ++_succeeded);

                break;
            }
            case QUALIFIED: {
                comment = _QUALIFIED_COMMENT;
                _qualifiedText.setText(_QUALIFIED_TEXT + ": " + ++_qualified);

                break;
            }
            case SKIPPED: {
                comment = _SKIPPED_COMMENT;
                _skippedText.setText(_SKIPPED_TEXT + ": " + ++_skipped);

                break;
            }
            case FAILED: {
                comment = _FAILED_COMMENT;
                _failedText.setText(_FAILED_TEXT + ": " + ++_failed);

                break;
            }
            default: {
                comment = _UNKNOWN_COMMENT;

                break;
            }
        }

        final DefaultListModel<String> listModel =
            (DefaultListModel<String>) _testsList
                .getModel();
        final int index = listModel.getSize() - 1;
        final String prefix = listModel.get(index);

        listModel.set(index, prefix + comment);

        if ((_state == null) || (state.compareTo(_state) > 0)) {
            switch (state) {
                case SUCCEEDED: {
                    _progressBar.setForeground(_SUCCEEDED_COLOR);

                    break;
                }
                case QUALIFIED: {
                    _progressBar.setForeground(_QUALIFIED_COLOR);

                    break;
                }
                case SKIPPED: {
                    _progressBar.setForeground(_SKIPPED_COLOR);

                    break;
                }
                case FAILED: {
                    _progressBar.setForeground(_FAILED_COLOR);

                    break;
                }
                default: {
                    _progressBar.setForeground(_UNKNOWN_COLOR);

                    break;
                }
            }

            _state = state;
        }

        _totalText.setText(_TOTAL_TEXT + ": " + ++_total);
        _elapsedText.setText(_ELAPSED_TEXT + ": " + elapsed);
    }

    private static void _invokeAndWait(final Runnable runnable)
    {
        try {
            EventQueue.invokeAndWait(runnable);
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void _initComponents()
    {
        final JPanel progressPanel = new JPanel();
        final JScrollPane testsPane = new JScrollPane();
        final JPanel lowerPanel = new JPanel();
        final Dimension screenSize;
        final Dimension dialogSize;
        JPanel statsPanel;
        JPanel panel;

        _progressBar = new JProgressBar();
        _testsList = new JList<>();
        _quitButton = new JButton();
        _succeededText = new JLabel();
        _qualifiedText = new JLabel();
        _skippedText = new JLabel();
        _failedText = new JLabel();
        _totalText = new JLabel();
        _elapsedText = new JLabel();

        setTitle(_TITLE);
        addComponentListener(
            new java.awt.event.ComponentAdapter()
            {
                @Override
                public void componentResized(final ComponentEvent event)
                {
                    onResize(event);
                }
            });
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosed(final WindowEvent event)
                {
                    getListener().closed();
                }
            });

        _progressBar
            .setPreferredSize(
                new Dimension(_MINIMUM_WIDTH, _MINIMUM_BAR_HEIGHT));
        _progressBar.setIndeterminate(true);
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(_progressBar, BorderLayout.CENTER);
        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        progressPanel.add(panel, BorderLayout.NORTH);
        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        progressPanel.add(panel, BorderLayout.WEST);
        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        progressPanel.add(panel, BorderLayout.EAST);
        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        progressPanel.add(panel, BorderLayout.SOUTH);
        getContentPane().add(progressPanel, BorderLayout.NORTH);

        _testsList.setModel(new DefaultListModel<String>());
        testsPane
            .setMinimumSize(
                new Dimension(_MINIMUM_WIDTH, _MINIMUM_LIST_HEIGHT));
        testsPane.setViewportView(_testsList);
        getContentPane().add(testsPane, BorderLayout.CENTER);

        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        getContentPane().add(panel, BorderLayout.WEST);
        panel = new JPanel();
        panel
            .setPreferredSize(
                new Dimension(_HORIZONTAL_MARGIN, _VERTICAL_MARGIN));
        getContentPane().add(panel, BorderLayout.EAST);

        lowerPanel.setLayout(new GridLayout());

        _succeededText.setText(_SUCCEEDED_TEXT + ": 0");
        _qualifiedText.setText(_QUALIFIED_TEXT + ": 0");
        _skippedText.setText(_SKIPPED_TEXT + ": 0");
        _failedText.setText(_FAILED_TEXT + ": 0");
        _totalText.setText(_TOTAL_TEXT + ": 0");
        _elapsedText.setText((_ELAPSED_TEXT + ": 0.0"));

        statsPanel = new JPanel();
        statsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(_succeededText);
        panel.add(_qualifiedText);
        panel.add(_totalText);
        statsPanel.add(panel);
        lowerPanel.add(statsPanel);

        _quitButton.setText(_CANCEL_TEXT);
        _quitButton
            .addActionListener(
                new ActionListener()
                {
                    @Override
                    public void actionPerformed(final ActionEvent event)
                    {
                        if (event.getSource() == getQuitButton()) {
                            dispose();
                        }
                    }
                });
        panel = new JPanel();
        panel
            .setLayout(
                new FlowLayout(
                    java.awt.FlowLayout.CENTER,
                    _HORIZONTAL_MARGIN,
                    _VERTICAL_MARGIN));
        panel.add(_quitButton);
        lowerPanel.add(panel);

        statsPanel = new JPanel();
        statsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(_failedText);
        panel.add(_skippedText);
        panel.add(_elapsedText);
        statsPanel.add(panel);
        lowerPanel.add(statsPanel);

        getContentPane().add(lowerPanel, BorderLayout.SOUTH);

        pack();
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(_DEFAULT_WIDTH, screenSize.height / 2);

        dialogSize = getSize();
        setLocation(
            (screenSize.width - dialogSize.width) / 2,
            (screenSize.height - dialogSize.height) / 2);
    }

    private static final int _DEFAULT_WIDTH = 600;
    private static final int _HORIZONTAL_MARGIN = 24;
    private static final int _MINIMUM_BAR_HEIGHT = 24;
    private static final int _MINIMUM_LIST_HEIGHT = 64;
    private static final int _MINIMUM_WIDTH = 192;
    private static final String _TITLE = "TestNG";
    private static final int _VERTICAL_MARGIN = 16;
    private static final String _UNKNOWN_COMMENT =
        TestsMessages.TEST_UNKNOWN_RESULT
            .toString();
    private static final Color _UNKNOWN_COLOR = Color.BLACK;
    private static final String _TOTAL_TEXT = TestsMessages.TESTS_TOTAL
        .toString();
    private static final String _SUCCEEDED_TEXT = TestsMessages.TEST_SUCCEEDED
        .toString();
    private static final String _SUCCEEDED_COMMENT =
        TestsMessages.TEST_SUCCEEDED_RESULT
            .toString();
    private static final Color _SUCCEEDED_COLOR = Color.GREEN;
    private static final String _SKIPPED_TEXT = TestsMessages.TEST_SKIPPED
        .toString();
    private static final String _SKIPPED_COMMENT =
        TestsMessages.TEST_SKIPPED_RESULT
            .toString();
    private static final Color _SKIPPED_COLOR = Color.GRAY;
    private static final String _QUALIFIED_TEXT = TestsMessages.TEST_QUALIFIED
        .toString();
    private static final String _QUALIFIED_COMMENT =
        TestsMessages.TEST_QUALIFIED_RESULT
            .toString();
    private static final Color _QUALIFIED_COLOR = Color.YELLOW;
    private static final String _FAILED_TEXT = TestsMessages.TEST_FAILED
        .toString();
    private static final String _FAILED_COMMENT =
        TestsMessages.TEST_FAILED_RESULT
            .toString();
    private static final Color _FAILED_COLOR = Color.RED;
    private static final String _ELAPSED_TEXT = TestsMessages.TESTS_ELAPSED
        .toString();
    private static final String _DONE_TEXT = TestsMessages.TESTS_DONE
        .toString();
    private static final String _CANCEL_TEXT = TestsMessages.TESTS_CANCEL
        .toString();
    private static final long serialVersionUID = 1L;

    private JLabel _elapsedText;
    private int _failed;
    private JLabel _failedText;
    private boolean _identified;
    private final GUIListener _listener;
    private JProgressBar _progressBar;
    private int _qualified;
    private JLabel _qualifiedText;
    private JButton _quitButton;
    private int _skipped;
    private JLabel _skippedText;
    private State _state;
    private int _succeeded;
    private JLabel _succeededText;
    private JList<String> _testsList;
    private int _total;
    private JLabel _totalText;
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
