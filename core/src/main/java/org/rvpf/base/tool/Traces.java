/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Traces.java 4108 2019-07-14 18:56:08Z SFB $
 */

package org.rvpf.base.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.Listeners;

/**
 * Traces.
 *
 * <p>Adds entries to a traces file. Each entry tells the time the entry was
 * added, then the description of a value. Each service using traces has a
 * dedicated directory. Within that directory, individual directories are used
 * to segregate entries by category, traces for each category. Within these
 * directories, a new traces file is created each day.</p>
 */
@ThreadSafe
public final class Traces
{
    /**
     * Constructs an instance.
     */
    public Traces() {}

    /**
     * Adds a value to the traces.
     *
     * @param value The value.
     */
    public void add(@Nonnull final Object value)
    {
        add(Optional.empty(), value);
    }

    /**
     * Adds a value to the traces.
     *
     * @param classifier An optional classifier.
     * @param value The value.
     */
    public void add(
            @Nonnull final Optional<String> classifier,
            @Nonnull final Object value)
    {
        if (isEnabled()) {
            if (!_listeners.isEmpty()) {
                for (final Listener listener: _listeners) {
                    if (!listener.onAddTrace(classifier, value, this)) {
                        return;
                    }
                }
            }

            _entries.add(new _Entry(classifier, String.valueOf(value)));
        }
    }

    /**
     * Adds a listener for this.
     *
     * @param listener The listener.
     *
     * @return True unless already added.
     */
    @CheckReturnValue
    public boolean addListener(@Nonnull final Listener listener)
    {
        return _listeners.add(listener);
    }

    /**
     * Commits the accumulated entries to file.
     */
    public void commit()
    {
        if (!isEnabled() || _entries.isEmpty()) {
            return;
        }

        try {
            final StringBuilder stringBuilder = new StringBuilder();

            synchronized (this) {
                for (;;) {
                    final _Entry entry = _entries.poll();

                    if (entry == null) {
                        break;
                    }

                    final DateTime midnight = entry.getStamp().midnight();

                    if ((_midnight == null) || !_midnight.equals(midnight)) {
                        if (_outputStream != null) {
                            if (stringBuilder.length() > 0) {
                                _write(stringBuilder);
                                stringBuilder.setLength(0);
                            }

                            _outputStream.close();
                            _outputStream = null;
                        }

                        final File outputFile = new File(
                            _directory,
                            _prefix + midnight.toString().substring(
                                0,
                                10) + _suffix);

                        _outputStream = new FileOutputStream(outputFile, true);
                        _midnight = midnight;
                    }

                    stringBuilder.append(entry.getStamp().toFullString());
                    stringBuilder.append(" ");

                    final Optional<String> entryClassifier = entry
                        .getClassifier();

                    if (entryClassifier.isPresent()) {
                        stringBuilder.append("(");
                        stringBuilder.append(entryClassifier.get());
                        stringBuilder.append(") ");
                    }

                    stringBuilder.append(entry.getValue());
                    stringBuilder.append("\n");
                }

                if (stringBuilder.length() > 0) {
                    _write(stringBuilder);
                }
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        for (final Listener listener: _listeners) {
            listener.onCommitTraces(this);
        }
    }

    /**
     * Asks if this is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isEnabled()
    {
        return _enabled.get();
    }

    /**
     * Removes a listener for this.
     *
     * @param listener The listener.
     *
     * @return True if it was present.
     */
    @CheckReturnValue
    public boolean removeListener(@Nonnull final Listener listener)
    {
        return _listeners.remove(listener);
    }

    /**
     * Forgets about the accumulated entries.
     */
    public void rollback()
    {
        if (isEnabled()) {
            for (final Listener listener: _listeners) {
                listener.onRollbackTraces(this);
            }

            _entries.clear();
        }
    }

    /**
     * Sets the compressed files indicator.
     *
     * @param compressed The compressed files indicator.
     */
    public void setCompressed(final boolean compressed)
    {
        _compressed = compressed;

        if (_compressed) {
            _suffix = DEFAULT_SUFFIX + DEFAULT_COMPRESSED_SUFFIX;
        }
    }

    /**
     * Sets the prefix for the traces files.
     *
     * @param prefix The prefix.
     */
    public void setPrefix(@Nonnull final String prefix)
    {
        _prefix = prefix;
    }

    /**
     * Sets the suffix for the traces files.
     *
     * @param suffix The suffix.
     */
    public void setSuffix(@Nonnull final String suffix)
    {
        _suffix = suffix;
    }

    /**
     * Sets up this.
     *
     * @param where The destination's parent directory (data directory).
     * @param how The traces properties.
     * @param who Who is tracing (used as default directory name).
     * @param what What will be traced (subdirectory).
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final File where,
            @Nonnull final KeyedGroups how,
            @Nonnull final UUID who,
            @Nonnull Optional<String> what)
    {
        Require.notNull(who);

        if (what.isPresent()) {
            what = Optional.of(what.get().trim());

            if (what.get().isEmpty()) {
                what = Optional.empty();
            }
        }

        _enabled
            .set(
                what.isPresent()
                && !how.isMissing()
                && !how.getBoolean(DISABLED_PROPERTY));

        if (!isEnabled()) {
            return true;
        }

        final File rootDirectory = new File(
            where,
            how.getString(ROOT_PROPERTY, Optional.of(DEFAULT_ROOT)).get());
        final Optional<String> dirName = how.getString(DIR_PROPERTY);
        final File baseDirectory = new File(
            rootDirectory,
            dirName.orElse(who.toName()));

        if (baseDirectory.mkdirs()) {
            _LOGGER
                .debug(
                    BaseMessages.TRACES_BASE_CREATED,
                    baseDirectory.getAbsolutePath());
        } else if (!baseDirectory.isDirectory()) {
            _LOGGER
                .warn(
                    BaseMessages.TRACES_BASE_FAILED,
                    baseDirectory.getAbsolutePath());

            return false;
        }

        if (!_setDirectory(new File(baseDirectory, what.get()))) {
            return false;
        }

        _prefix = how
            .getString(PREFIX_PROPERTY, Optional.of(DEFAULT_PREFIX))
            .get();
        _suffix = how
            .getString(SUFFIX_PROPERTY, Optional.of(DEFAULT_SUFFIX))
            .get();
        _compressed = how.getBoolean(COMPRESSED_PROPERTY, false);

        if (_compressed) {
            _suffix = how
                .getString(
                    COMPRESSED_SUFFIX_PROPERTY,
                    Optional.of(_suffix + DEFAULT_COMPRESSED_SUFFIX))
                .get();
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_enabled.compareAndSet(true, false)) {
            rollback();

            synchronized (this) {
                if (_outputStream != null) {
                    try {
                        _outputStream.close();
                    } catch (final IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    _outputStream = null;
                }

                _midnight = null;
            }

            _directory = null;
        }
    }

    private boolean _setDirectory(final File directory)
    {
        _directory = directory;

        if (_directory.mkdirs()) {
            _LOGGER
                .debug(
                    BaseMessages.TRACES_DIR_CREATED,
                    _directory.getAbsolutePath());
        } else if (!_directory.isDirectory()) {
            _LOGGER
                .warn(
                    BaseMessages.TRACES_DIR_FAILED,
                    _directory.getAbsolutePath());

            return false;
        }

        return true;
    }

    private void _write(final StringBuilder builder)
        throws IOException
    {
        final byte[] bytes = _coder.encode(builder.toString());

        if (_compressed) {
            final GZIPOutputStream compressedStream = new GZIPOutputStream(
                _outputStream);

            compressedStream.write(bytes);
            compressedStream.finish();
        } else {
            _outputStream.write(bytes);
        }

        _outputStream.flush();
    }

    /** Requests that the service traces be compressed. */
    public static final String COMPRESSED_PROPERTY = "compressed";

    /** The file name suffix for compressed traces files. */
    public static final String COMPRESSED_SUFFIX_PROPERTY = "compressed.suffix";

    /** Default compressed file suffix. */
    public static final String DEFAULT_COMPRESSED_SUFFIX = ".gz";

    /** Default traces prefix. */
    public static final String DEFAULT_PREFIX = "";

    /** Default traces root directory. */
    public static final String DEFAULT_ROOT = "traces";

    /** Default traces suffix. */
    public static final String DEFAULT_SUFFIX = ".txt";

    /** The directory name for the service traces. */
    public static final String DIR_PROPERTY = "dir";

    /** Disables the service traces. */
    public static final String DISABLED_PROPERTY = "disabled";

    /** The file name prefix for traces files. */
    public static final String PREFIX_PROPERTY = "prefix";

    /**
     * The root directory for the service traces. It can be specified as a
     * relative or absolute path.
     */
    public static final String ROOT_PROPERTY = "root";

    /** The file name suffix for traces files. */
    public static final String SUFFIX_PROPERTY = "suffix";

    /** Traces properties. */
    public static final String TRACES_PROPERTIES = "traces";

    /**  */

    private static final Logger _LOGGER = Logger.getInstance(Traces.class);

    private final Coder _coder = new Coder();
    private boolean _compressed;
    private File _directory;
    private final AtomicBoolean _enabled = new AtomicBoolean();
    private final Queue<_Entry> _entries = new ConcurrentLinkedQueue<_Entry>();
    private final Listeners<Listener> _listeners = new Listeners<Listener>();
    @GuardedBy("this")
    private DateTime _midnight;
    @GuardedBy("this")
    private OutputStream _outputStream;
    private String _prefix;
    private String _suffix;

    /**
     * Tracer listener.
     */
    public interface Listener
    {
        /**
         * Called before a trace entry is added.
         *
         * @param classifier An optional classifier.
         * @param value The value.
         * @param tracer The calling traces.
         *
         * @return True to let the addition occur.
         */
        @CheckReturnValue
        boolean onAddTrace(
                @Nonnull Optional<String> classifier,
                Object value,
                Traces tracer);

        /**
         * Called when trace entries are committed.
         *
         * @param tracer The calling traces.
         */
        void onCommitTraces(Traces tracer);

        /**
         * Called when trace entries are rolled back.
         *
         * @param tracer The calling traces.
         */
        void onRollbackTraces(Traces tracer);
    }


    /**
     * Context.
     *
     * <p>May be used to supply the traces properties.</p>
     */
    @NotThreadSafe
    public static final class Context
        extends KeyedGroups
    {
        /**
         * Constructs an instance.
         */
        public Context()
        {
            super(BaseMessages.VALUE_TYPE.toString(), Optional.empty());
        }

        private Context(final Context other)
        {
            super(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public Context copy()
        {
            return new Context(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public Context freeze()
        {
            super.freeze();

            return this;
        }

        /**
         * Sets the 'compressed' property.
         *
         * @param compressed The new property value.
         */
        public void setCompressed(final boolean compressed)
        {
            setValue(COMPRESSED_PROPERTY, Boolean.valueOf(compressed));
        }

        /**
         * Sets the 'compressed.suffix' property.
         *
         * @param suffix The new property value.
         */
        public void setCompressedSuffix(@Nonnull final String suffix)
        {
            setValue(COMPRESSED_SUFFIX_PROPERTY, suffix);
        }

        /**
         * Sets the 'dir' property.
         *
         * @param dir The new property value.
         */
        public void setDir(@Nonnull final String dir)
        {
            setValue(DIR_PROPERTY, dir);
        }

        /**
         * Sets the 'disabled' property.
         *
         * @param disabled The new property value.
         */
        public void setDisabled(final boolean disabled)
        {
            setValue(DISABLED_PROPERTY, Boolean.valueOf(disabled));
        }

        /**
         * Sets the 'prefix' property.
         *
         * @param prefix The new property value.
         */
        public void setPrefix(@Nonnull final String prefix)
        {
            setValue(PREFIX_PROPERTY, prefix);
        }

        /**
         * Sets the 'root' property.
         *
         * @param root The new property value.
         */
        public void setRoot(@Nonnull final String root)
        {
            setValue(ROOT_PROPERTY, root);
        }

        /**
         * Sets the 'suffix' property.
         *
         * @param suffix The new property value.
         */
        public void setSuffix(@Nonnull final String suffix)
        {
            setValue(SUFFIX_PROPERTY, suffix);
        }

        private static final long serialVersionUID = 1L;
    }


    private static final class _Entry
    {
        /**
         * Constructs an instance.
         *
         * @param classifier A classifier.
         * @param value The value.
         */
        _Entry(
                @Nonnull final Optional<String> classifier,
                @Nonnull final String value)
        {
            _stamp = DateTime.now();
            _classifier = classifier;
            _value = value;
        }

        /**
         * Gets the classifier.
         *
         * @return The optional classifier.
         */
        @Nonnull
        @CheckReturnValue
        Optional<String> getClassifier()
        {
            return _classifier;
        }

        /**
         * Gets the stamp.
         *
         * @return The stamp.
         */
        @Nonnull
        @CheckReturnValue
        DateTime getStamp()
        {
            return _stamp;
        }

        /**
         * Gets the value.
         *
         * @return The value.
         */
        @Nonnull
        @CheckReturnValue
        String getValue()
        {
            return _value;
        }

        private final Optional<String> _classifier;
        private final DateTime _stamp;
        private final String _value;
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
