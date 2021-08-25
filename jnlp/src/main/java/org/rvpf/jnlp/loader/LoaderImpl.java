/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoaderImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jnlp.loader;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.util.Version;
import org.rvpf.jnlp.launcher.Loader;

/**
 * Loader implementation.
 */
@NotThreadSafe
public class LoaderImpl
    implements Loader
{
    /**
     * Constructs an instance.
     */
    public LoaderImpl()
    {
        final Version version = new JNLPVersion();

        version.logSystemInfo(getClass().getSimpleName());
        _LOGGER
            .info(
                JNLPMessages.IMPLEMENTATION,
                version.getImplementationTitle(),
                version.getImplementationVersion());
    }

    /** {@inheritDoc}
     */
    @Override
    public String[] getArguments()
    {
        return _jnlp.getArguments();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getMainClassName()
    {
        return _jnlp.getMainClassName();
    }

    /** {@inheritDoc}
     */
    @Override
    public Map<String, String> getProperties()
    {
        return _jnlp.getProperties();
    }

    /** {@inheritDoc}
     */
    @Override
    public URL[] getURLs()
    {
        final Iterator<File> jarsIterator = _jnlp.getJars().iterator();
        final URL[] urls = new URL[_jnlp.getJars().size()];

        for (int i = 0; i < urls.length; ++i) {
            try {
                urls[i] = jarsIterator.next().toURI().toURL();
            } catch (final MalformedURLException exception) {
                throw new InternalError(exception);
            }
        }

        _LOGGER.info(JNLPMessages.STARTING_APPLICATION);

        return urls;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean loadJNLP(final Optional<URL> jnlpURL)
    {
        try {
            if (!jnlpURL.isPresent()) {
                throw new JNLPException(
                    Message.format(JNLPMessages.NO_JNLP_URL));
            }

            _LOGGER.info(JNLPMessages.JNLP_FILE, jnlpURL.get());

            _jnlp = JNLP.load(jnlpURL.get());

            _LOGGER.debug(JNLPMessages.VERIFICATION_COMPLETED);
        } catch (final JNLPException exception) {
            _LOGGER.error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        } catch (final Throwable exception) {
            _LOGGER.error(exception, JNLPMessages.UNEXPECTED_EXCEPTION);

            return false;
        }

        return true;
    }

    private static final Logger _LOGGER = Logger.getInstance(LoaderImpl.class);

    private JNLP _jnlp;
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
