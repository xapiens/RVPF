/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Fragment.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;
import org.rvpf.pap.dnp3.object.ObjectHeader;
import org.rvpf.pap.dnp3.object.ObjectHeader.PrefixCode;
import org.rvpf.pap.dnp3.object.ObjectHeader.RangeCode;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;

/**
 * Fragment.
 */
public final class Fragment
{
    /**
     * Constructs an instance.
     *
     * @param association The association.
     * @param header The header.
     */
    public Fragment(
            @Nonnull final Association association,
            @Nonnull final Header header)
    {
        _association = association;
        _header = header;
        _maxSize = _association.getRemoteEndPoint().getMaxFragmentSize();
        _size = _header.size();
    }

    /**
     * Add an item.
     *
     * @param item The item.
     *
     * @return False if the maximum fragment size would be exceeded.
     */
    @CheckReturnValue
    public boolean add(@Nonnull final Fragment.Item item)
    {
        final ObjectHeader objectHeader = item.getObjectHeader();
        int size = _size;

        try {
            size += objectHeader.getLength();
        } catch (final DNP3ProtocolException exception) {
            throw new IllegalArgumentException(
                "Bad message item header: " + exception.getMessage());
        }

        final Optional<ObjectInstance[]> objectInstances = item
            .getObjectInstances();

        if (objectInstances.isPresent()) {
            for (final ObjectInstance object: objectInstances.get()) {
                final ObjectVariation objectHeaderVariation;

                try {
                    objectHeaderVariation = objectHeader.getObjectVariation();
                } catch (final DNP3ProtocolException exception) {
                    throw new InternalError(exception);
                }

                if (object.getObjectVariation() != objectHeaderVariation) {
                    throw new IllegalArgumentException(
                        "Inconsistent object variation: "
                        + object.getObjectVariation().getObjectClass().getSimpleName()
                        + "!="
                        + objectHeaderVariation.getObjectClass().getSimpleName());
                }

                size += object.getObjectLength();
            }
        }

        if (size > _maxSize) {
            return false;
        }

        _items.add(item);
        _size = size;

        return true;
    }

    /**
     * Dumps this fragment to a buffer.
     *
     * @param buffer The buffer.
     *
     * @throws IOException On I/O exception.
     */
    public void dumpToBuffer(
            @Nonnull final ByteBuffer buffer)
        throws IOException
    {
        _header.dumpToBuffer(buffer);

        for (final Fragment.Item item: getItems()) {
            item
                .getObjectHeader()
                .dumpToBuffer(buffer, item.getObjectInstances());
        }
    }

    /**
     * Gets the association.
     *
     * @return The association.
     */
    @Nonnull
    @CheckReturnValue
    public final Association getAssociation()
    {
        return _association;
    }

    /**
     * Gets the function code.
     *
     * @return The function code.
     */
    @Nonnull
    @CheckReturnValue
    public FunctionCode getFunctionCode()
    {
        return _header.getFunctionCode();
    }

    /**
     * Gets the header.
     *
     * @return The header.
     */
    @Nonnull
    @CheckReturnValue
    public Header getHeader()
    {
        return _header;
    }

    /**
     * Gets the internal indications.
     *
     * @return The internal indications.
     *
     * @throws ClassCastException When called on a request.
     */
    @Nonnull
    @CheckReturnValue
    public InternalIndications getInternalIndications()
        throws ClassCastException
    {
        return ((Fragment.Header.Response) _header).getInternalIndications();
    }

    /**
     * Gets the object headers.
     *
     * @return The object headers.
     */
    @Nonnull
    @CheckReturnValue
    public LinkedList<Fragment.Item> getItems()
    {
        return _items;
    }

    /**
     * Gets the maximum item size.
     *
     * @return The maximum item size.
     */
    @CheckReturnValue
    public int getMaxItemSize()
    {
        return _maxSize - _header.size();
    }

    /**
     * Gets the first fragment sequence.
     *
     * @return The sequence.
     */
    @CheckReturnValue
    public byte getSequence()
    {
        return _header.getSequence();
    }

    /**
     * Asks if a confirm message is requested.
     *
     * @return True if a confirm message is requested.
     */
    @CheckReturnValue
    public boolean isConfirmRequested()
    {
        return _header.isConfirmRequested();
    }

    /**
     * Asks if the fragment is first.
     *
     * @return True if first.
     */
    @CheckReturnValue
    public final boolean isFirst()
    {
        return _header.isFirst();
    }

    /**
     * Asks if the fragment is last.
     *
     * @return True if first.
     */
    @CheckReturnValue
    public final boolean isLast()
    {
        return _header.isLast();
    }

    /**
     * Asks if this fragment is a request.
     *
     * @return True if this fragment is a request.
     */
    @CheckReturnValue
    public boolean isRequest()
    {
        return _header.isInRequest();
    }

    /**
     * Gets the unsolicited indicator.
     *
     * @return The unsolicited indicator.
     */
    @CheckReturnValue
    public boolean isUnsolicited()
    {
        return _header.isUnsolicited();
    }

    /**
     * Loads this fragment from a buffer.
     *
     * @param buffer The buffer.
     *
     * @throws IOException On I/O excewption.
     */
    public void loadFromBuffer(
            @Nonnull final ByteBuffer buffer)
        throws IOException
    {
        _header.loadFromBuffer(buffer);

        if (!isRequest()) {
            final InternalIndications internalIndications =
                getInternalIndications();

            if (internalIndications.hasNoFuncCodeSupport()) {
                Logger
                    .getInstance(getClass())
                    .trace(
                        DNP3Messages.NO_FUNC_CODE_SUPPORT,
                        _association,
                        getFunctionCode());
            }

            if (internalIndications.hasObjectUnknown()) {
                throw new DNP3ProtocolException(
                    DNP3Messages.OBJECT_UNKNOWN,
                    _association);
            }

            if (internalIndications.hasParameterError()) {
                throw new DNP3ProtocolException(
                    DNP3Messages.PARAMETER_ERROR,
                    _association);
            }
        }

        while (buffer.hasRemaining()) {
            final ObjectHeader objectHeader = ObjectHeader.newInstance(buffer);
            final int objectCount = objectHeader.getObjectCount();
            final Optional<Number[]> objectIndices;
            final Optional<Number[]> objectSizes;
            final Optional<ObjectInstance[]> objectInstances;

            if ((objectCount > 0)
                    && (_header.getFunctionCode().needsValues()
                        || (objectHeader.getPrefixCode() != PrefixCode.NONE))) {
                switch (objectHeader.getPrefixCode()) {
                    case INDEX_BYTE:
                    case INDEX_SHORT:
                    case INDEX_INT: {
                        objectIndices = Optional.of(new Number[objectCount]);
                        objectSizes = Optional.empty();

                        break;
                    }
                    case SIZE_BYTE:
                    case SIZE_SHORT:
                    case SIZE_INT: {
                        objectIndices = Optional.empty();
                        objectSizes = Optional.of(new Number[objectCount]);

                        break;
                    }
                    default: {
                        objectIndices = Optional.empty();
                        objectSizes = Optional.empty();

                        break;
                    }
                }

                objectInstances = _header
                    .getFunctionCode()
                    .needsValues()? Optional
                        .of(new ObjectInstance[objectCount]): Optional.empty();
                objectHeader
                    .loadInstancesFromBuffer(
                        buffer,
                        objectIndices,
                        objectSizes,
                        objectInstances);
            } else {
                objectIndices = Optional.empty();
                objectSizes = Optional.empty();
                objectInstances = Optional.empty();
            }

            Require
                .success(
                    add(
                        new Fragment.Item(
                                objectHeader,
                                        objectIndices,
                                        objectSizes,
                                        objectInstances)));
        }
    }

    /**
     * Sends this.
     *
     * @throws IOException On I/O exception.
     */
    public void send()
        throws IOException
    {
        _association.getApplicationLayer().send(this);
    }

    /**
     * Sets the first fragment sequence.
     *
     * @param sequence The sequence.
     */
    public void setSequence(final byte sequence)
    {
        _header.setSequence(sequence);
    }

    /**
     * Sets the unsolicited indicator.
     */
    public void setUnsolicited()
    {
        _header.setUnsolicited();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();

        builder.append(_association);
        builder.append(':');
        builder.append(_header);

        boolean first = true;

        for (final Item item: _items) {
            if (first) {
                builder.append('/');
            }

            builder.append(item);
            first = false;
        }

        return builder.toString();
    }

    private final Association _association;
    private final Header _header;
    private final LinkedList<Fragment.Item> _items = new LinkedList<>();
    private final int _maxSize;
    private int _size;

    /**
     * Application header.
     */
    public abstract static class Header
    {
        /**
         * Constructs an instance.
         *
         * @param functionCode The function code.
         */
        Header(@Nonnull final Optional<FunctionCode> functionCode)
        {
            _functionCode = functionCode.orElse(null);
        }

        /**
         * Returns a new instance.
         *
         * @param functionCode A function code.
         * @param fromMaster True if from master.
         *
         * @return The new instance.
         */
        @Nonnull
        @CheckReturnValue
        public static Header newInstance(
                @Nonnull final Optional<FunctionCode> functionCode,
                final boolean fromMaster)
        {
            return fromMaster? new Request(
                functionCode): new Response(functionCode);
        }

        /**
         * Dumps this header to a buffer.
         *
         * @param buffer The buffer.
         */
        public void dumpToBuffer(@Nonnull final ByteBuffer buffer)
        {
            buffer.put(_applicationControl);
            buffer.put((byte) getFunctionCode().getCode());
        }

        /**
         * Gets the function code.
         *
         * @return The function code.
         */
        @Nonnull
        @CheckReturnValue
        public final FunctionCode getFunctionCode()
        {
            return Require.notNull(_functionCode);
        }

        /**
         * Gets the fragments sequence.
         *
         * @return The sequence.
         */
        @CheckReturnValue
        public final byte getSequence()
        {
            return (byte) (_applicationControl & _SEQ_MASK);
        }

        /**
         * Asks if the fragment wants confirmation.
         *
         * @return True if confirm.
         */
        @CheckReturnValue
        public final boolean isConfirmRequested()
        {
            return (_applicationControl & _CON_MASK) != 0;
        }

        /**
         * Asks if the fragment is first.
         *
         * @return True if first.
         */
        @CheckReturnValue
        public final boolean isFirst()
        {
            return (_applicationControl & _FIR_MASK) != 0;
        }

        /**
         * Asks if the fragment is in a request.
         *
         * @return True if it is in a request.
         */
        @CheckReturnValue
        public abstract boolean isInRequest();

        /**
         * Asks if the fragment is last.
         *
         * @return True if first.
         */
        @CheckReturnValue
        public final boolean isLast()
        {
            return (_applicationControl & _FIN_MASK) != 0;
        }

        /**
         * Asks if this header is loaded.
         *
         * @return True if loaded.
         */
        @CheckReturnValue
        public final boolean isLoaded()
        {
            return _functionCode != null;
        }

        /**
         * Asks if the fragment has the unsolicited indicator.
         *
         * @return True if it has the unsolicited indicator.
         */
        @CheckReturnValue
        public final boolean isUnsolicited()
        {
            return (_applicationControl & _UNS_MASK) != 0;
        }

        /**
         * Loads this header from a buffer.
         *
         * @param buffer The buffer.
         *
         * @throws IOException On I/O excewption.
         */
        public void loadFromBuffer(
                @Nonnull final ByteBuffer buffer)
            throws IOException
        {
            _applicationControl = buffer.get();

            final byte code = buffer.get();

            _functionCode = FunctionCode.instance(code & 0xFF);
        }

        /**
         * Resets.
         */
        public final void reset()
        {
            _applicationControl &= _CON_MASK | _UNS_MASK | _SEQ_MASK;
        }

        /**
         * Sets the confirmation request indicator for the fragment.
         */
        public final void setConfirmRequested()
        {
            _applicationControl |= _CON_MASK;
        }

        /**
         * Sets the fragment as first.
         */
        public final void setFirst()
        {
            _applicationControl |= _FIR_MASK;
        }

        /**
         * Sets the fragment as last.
         */
        public final void setLast()
        {
            _applicationControl |= _FIN_MASK;
        }

        /**
         * Sets the fragment sequence.
         *
         * @param sequence The sequence.
         */
        public final void setSequence(final byte sequence)
        {
            _applicationControl = (byte) ((_applicationControl & ~_SEQ_MASK)
                    | (sequence & _SEQ_MASK));
        }

        /**
         * Sets the unsolicited indicator on the fragment.
         */
        public final void setUnsolicited()
        {
            _applicationControl |= _UNS_MASK;
        }

        /**
         * Returns the size of the header.
         *
         * @return The size of the header.
         */
        @CheckReturnValue
        public abstract int size();

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder builder = new StringBuilder(
                _functionCode.toString());

            _toString(builder, isFirst(), "FIR");
            _toString(builder, isLast(), "FIN");
            _toString(builder, isUnsolicited(), "UNS");
            _toString(builder, isConfirmRequested(), "CON");
            _toString(
                builder,
                true,
                Integer.toHexString(getSequence()).toUpperCase(Locale.ROOT));

            if (this instanceof Response) {
                builder.append(((Response) this).getInternalIndications());
            }

            return builder.toString();
        }

        private static void _toString(
                final StringBuilder builder,
                final boolean condition,
                final String text)
        {
            if (condition) {
                builder.append(',');
                builder.append(text);
            }
        }

        private static final byte _CON_MASK = (byte) 0x20;
        private static final byte _FIN_MASK = (byte) 0x40;
        private static final byte _FIR_MASK = (byte) 0x80;
        private static final byte _SEQ_MASK = (byte) 0x0F;
        private static final byte _UNS_MASK = (byte) 0x10;

        private byte _applicationControl;
        private FunctionCode _functionCode;

        /**
         * Request.
         */
        public static final class Request
            extends Header
        {
            /**
             * Constructs an instance.
             *
             * @param functionCode An optional function code.
             */
            Request(@Nonnull final Optional<FunctionCode> functionCode)
            {
                super(functionCode);

                setFirst();
                setLast();
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean isInRequest()
            {
                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            public int size()
            {
                return 2;
            }
        }


        /**
         * Response.
         */
        public static final class Response
            extends Header
        {
            /**
             * Constructs an instance.
             *
             * @param functionCode An optional function code.
             */
            Response(@Nonnull final Optional<FunctionCode> functionCode)
            {
                super(functionCode);
            }

            /** {@inheritDoc}
             */
            @Override
            public void dumpToBuffer(@Nonnull final ByteBuffer buffer)
            {
                super.dumpToBuffer(buffer);

                buffer.putShort(_internalIndications.getInternalIndications());
            }

            /**
             * Gets the internal indications.
             *
             * @return The internal indications.
             */
            @Nonnull
            @CheckReturnValue
            public InternalIndications getInternalIndications()
            {
                return _internalIndications;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean isInRequest()
            {
                return false;
            }

            /** {@inheritDoc}
             */
            @Override
            public void loadFromBuffer(
                    @Nonnull final ByteBuffer buffer)
                throws IOException
            {
                super.loadFromBuffer(buffer);

                setInternalIndications(
                    new InternalIndications(buffer.getShort()));
            }

            /**
             * Sets the internal indications.
             *
             * @param internalIndications The internal indications.
             */
            public void setInternalIndications(
                    @Nonnull final InternalIndications internalIndications)
            {
                _internalIndications.set(internalIndications);
            }

            /** {@inheritDoc}
             */
            @Override
            public int size()
            {
                return 4;
            }

            private final InternalIndications _internalIndications =
                new InternalIndications();
        }
    }


    /**
     * Item.
     */
    public static final class Item
    {
        /**
         * Constructs an instance.
         *
         * @param objectHeader The object header.
         */
        public Item(@Nonnull final ObjectHeader objectHeader)
        {
            this(
                objectHeader,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        }

        /**
         * Constructs an instance.
         *
         * @param objectHeader The object header.
         * @param objectInstances The optional object instances.
         */
        public Item(
                @Nonnull final ObjectHeader objectHeader,
                @Nonnull final Optional<ObjectInstance[]> objectInstances)
        {
            this(
                objectHeader,
                Optional.empty(),
                Optional.empty(),
                objectInstances);
        }

        /**
         * Constructs an instance.
         *
         * @param objectHeader The object header.
         * @param objectIndexes The optional object indexes.
         * @param objectSizes The optional object sizes.
         * @param objectInstances The optional object instances.
         */
        public Item(
                @Nonnull final ObjectHeader objectHeader,
                @Nonnull final Optional<Number[]> objectIndexes,
                @Nonnull final Optional<Number[]> objectSizes,
                @Nonnull final Optional<ObjectInstance[]> objectInstances)
        {
            _objectHeader = objectHeader;

            Require
                .success(
                    !objectIndexes.isPresent()
                    || !objectInstances.isPresent()
                    || (objectIndexes.get().length
                        == objectInstances.get().length));
            Require
                .success(
                    !objectSizes.isPresent()
                    || !objectInstances.isPresent()
                    || (objectSizes.get().length
                        == objectInstances.get().length));
            Require
                .success(
                    !objectInstances.isPresent()
                    || (objectInstances.get().length > 0));

            _objectIndexes = objectIndexes;
            _objectSizes = objectSizes;
            _objectInstances = objectInstances;
        }

        /**
         * Gets indexes.
         *
         * @return An Indexes instance.
         */
        @Nonnull
        @CheckReturnValue
        public Indexes getIndexes()
        {
            return new Indexes();
        }

        /**
         * Gets the object header.
         *
         * @return The object header.
         */
        @Nonnull
        @CheckReturnValue
        public ObjectHeader getObjectHeader()
        {
            return _objectHeader;
        }

        /**
         * Gets the object indexes.
         *
         * @return The object indexes.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Number[]> getObjectIndexes()
        {
            return _objectIndexes;
        }

        /**
         * Gets the object instances.
         *
         * @return The optional object instances.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<ObjectInstance[]> getObjectInstances()
        {
            return _objectInstances;
        }

        /**
         * Gets the object sizes.
         *
         * @return The object sizes.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Number[]> getObjectSizes()
        {
            return _objectSizes;
        }

        /**
         * Gets the object variation.
         *
         * @return The object variation (empty if unknown).
         */
        @Nonnull
        @CheckReturnValue
        public ObjectVariation getObjectVariation()
        {
            try {
                return _objectHeader.getObjectVariation();
            } catch (final DNP3ProtocolException exception) {
                throw new InternalError(exception);
            }
        }

        /**
         * Gets the point type.
         *
         * @return The optional point type.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<PointType> getPointType()
        {
            return getObjectVariation().getPointType();
        }

        /**
         * Gets the prefix code.
         *
         * @return The prefix code.
         *
         * @throws DNP3ProtocolException On unknown prefix code.
         */
        @Nonnull
        @CheckReturnValue
        public PrefixCode getPrefixCode()
            throws DNP3ProtocolException
        {
            return _objectHeader.getPrefixCode();
        }

        /**
         * Gets the range.
         *
         * @return The optional range.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Object> getRange()
        {
            return _objectHeader.getRange();
        }

        /**
         * Gets the range code.
         *
         * @return The range code.
         *
         * @throws DNP3ProtocolException On unknown range code.
         */
        @Nonnull
        @CheckReturnValue
        public RangeCode getRangeCode()
            throws DNP3ProtocolException
        {
            return _objectHeader.getRangeCode();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder builder = new StringBuilder();

            builder.append(_objectHeader);

            if (_objectInstances.isPresent()) {
                boolean first = true;

                for (final ObjectInstance objectInstance:
                        _objectInstances.get()) {
                    if (!first) {
                        builder.append(',');
                    }

                    builder.append(objectInstance);
                    first = false;
                }
            }

            return builder.toString();
        }

        private final ObjectHeader _objectHeader;
        private final Optional<Number[]> _objectIndexes;
        private final Optional<ObjectInstance[]> _objectInstances;
        private final Optional<Number[]> _objectSizes;

        /**
         * Indexes.
         */
        public final class Indexes
            implements Iterable<Integer>, Iterator<Integer>
        {
            /**
             * Constructs an instance.
             */
            Indexes()
            {
                final PrefixCode prefixCode;

                try {
                    prefixCode = getPrefixCode();
                } catch (final DNP3ProtocolException exception) {
                    throw new InternalError(exception);
                }

                switch (prefixCode) {
                    case NONE: {
                        break;
                    }
                    case INDEX_BYTE:
                    case INDEX_SHORT:
                    case INDEX_INT: {
                        final Number[] prefixes = getObjectIndexes().get();

                        _indices = new int[prefixes.length];
                        _length = _indices.length;

                        for (int i = 0; i < prefixes.length; ++i) {
                            _indices[i] = prefixes[i].intValue();
                        }

                        _start = -1;
                        _stop = -1;

                        return;
                    }
                    default: {
                        Logger
                            .getInstance(getClass())
                            .warn(
                                DNP3Messages.PREFIX_CODE_NOT_SUPPORTED,
                                prefixCode);
                        _start = -1;
                        _stop = -1;

                        return;
                    }
                }

                final RangeCode rangeCode;

                try {
                    rangeCode = getRangeCode();
                } catch (final DNP3ProtocolException exception) {
                    throw new InternalError(exception);
                }

                switch (rangeCode) {
                    case START_STOP_INDEX_BYTE:
                    case START_STOP_ADDRESS_BYTE: {
                        final byte[] range = (byte[]) getRange().get();

                        _start = range[0] & 0xFF;
                        _stop = range[1] & 0xFF;
                        _length = 1 + _stop - _start;

                        break;
                    }
                    case START_STOP_INDEX_SHORT:
                    case START_STOP_ADDRESS_SHORT: {
                        final short[] range = (short[]) getRange().get();

                        _start = range[0] & 0xFFFF;
                        _stop = range[1] & 0xFFFF;
                        _length = 1 + _stop - _start;

                        break;
                    }
                    case START_STOP_INDEX_INT:
                    case START_STOP_ADDRESS_INT: {
                        final int[] range = (int[]) getRange().get();

                        _start = range[0];
                        _stop = range[1];
                        _length = 1 + _stop - _start;

                        break;
                    }
                    default: {
                        Logger
                            .getInstance(getClass())
                            .warn(
                                DNP3Messages.RANGE_CODE_NOT_SUPPORTED,
                                rangeCode);
                        _start = -1;
                        _stop = -1;

                        return;
                    }
                }
            }

            /**
             * Gets the length.
             *
             * @return The length.
             */
            @CheckReturnValue
            public int getLength()
            {
                return _length;
            }

            /**
             * Gets the position.
             *
             * @return The position.
             */
            @CheckReturnValue
            public int getPosition()
            {
                return _position - 1;
            }

            /**
             * Gets the start index.
             *
             * @return The start index.
             */
            @CheckReturnValue
            public int getStart()
            {
                return _start;
            }

            /**
             * Gets the stop index.
             *
             * @return The stop index.
             */
            @CheckReturnValue
            public int getStop()
            {
                return _stop;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean hasNext()
            {
                return _position < _length;
            }

            /**
             * Asks if start/stop.
             *
             * @return True if start/stop.
             */
            @CheckReturnValue
            public boolean isStartStop()
            {
                return _start >= 0;
            }

            /** {@inheritDoc}
             */
            @Override
            public Iterator<Integer> iterator()
            {
                _position = 0;

                return this;
            }

            /** {@inheritDoc}
             */
            @Override
            public Integer next()
            {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return Integer.valueOf(_start + _position++);
            }

            private int[] _indices;
            private int _length;
            private int _position;
            private final int _start;
            private final int _stop;
        }
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
