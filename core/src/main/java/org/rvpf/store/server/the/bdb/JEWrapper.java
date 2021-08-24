/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JEWrapper.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.bdb;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;

/**
 * JE wrapper.
 */
public interface JEWrapper
{
    /**
     * Aborts a transaction.
     *
     * @param transaction The transaction handle.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void abort(@Nonnull Object transaction)
        throws JEWrapperException;

    /**
     * Begins a transaction.
     *
     * @return A transaction handle.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    @Nonnull
    @CheckReturnValue
    Object beginTransaction()
        throws JEWrapperException;

    /**
     * Closes.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void close()
        throws JEWrapperException;

    /**
     * Closes wrapper a cursor.
     *
     * @param wrapperCursor The wrapper cursor object.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void closeCursor(@Nonnull Object wrapperCursor)
        throws JEWrapperException;

    /**
     * Commits a transaction.
     *
     * @param transaction The transaction handle.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void commit(@Nonnull Object transaction)
        throws JEWrapperException;

    /**
     * Counts point values.
     *
     * @param wrapperCursor A wrapper cursor object.
     *
     * @return The number of point values.
     */
    @CheckReturnValue
    long countPointValues(@Nonnull Object wrapperCursor);

    /**
     * Deletes a point value from the archive.
     *
     * @param transaction The transaction handle.
     * @param pointValue The point value.
     *
     * @return The number of values deleted (0 .. 1).
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    @CheckReturnValue
    int delete(
            @Nonnull Object transaction,
            @Nonnull VersionedValue pointValue)
        throws JEWrapperException;

    /**
     * Gets a wrapper cursor object.
     *
     * @param storeCursor The store cursor.
     *
     * @return The wrapper cursor object.
     */
    @Nonnull
    @CheckReturnValue
    Object getCursor(@Nonnull StoreCursor storeCursor);

    /**
     * Returns the next point value.
     *
     * @param wrapperCursor A wrapper cursor object.
     *
     * @return A point value (empty when none left).
     */
    @Nonnull
    @CheckReturnValue
    Optional<VersionedValue> nextPointValue(@Nonnull Object wrapperCursor);

    /**
     * Opens.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void open()
        throws JEWrapperException;

    /**
     * Puts a point value into the archive.
     *
     * @param transaction The transaction handle.
     * @param pointValue The point value.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void put(
            @Nonnull Object transaction,
            @Nonnull VersionedValue pointValue)
        throws JEWrapperException;

    /**
     * Sets up the environment and the database.
     *
     * @param home The configuration.
     * @param snapshot True in snapshot mode.
     * @param pullDisabled True if pull is disabled.
     *
     * @return True on success.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull File home,
            boolean snapshot,
            boolean pullDisabled)
        throws JEWrapperException;

    /**
     * Tears down what has been set up.
     *
     * @throws JEWrapperException To wrap exceptions.
     */
    void tearDown()
        throws JEWrapperException;

    ClassDef IMPL = new ClassDefImpl("org.rvpf.ext.bdb.JEWrapperImpl");
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
