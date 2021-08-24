/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServlet.java 3950 2019-05-04 14:45:20Z SFB $
 */

package org.rvpf.http.metadata;

import java.io.IOException;

import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.document.exporter.MetadataExporter;
import org.rvpf.http.AbstractServlet;
import org.rvpf.metadata.Metadata;

/**
 * Metadata servlet.
 */
@ThreadSafe
public final class MetadataServlet
    extends AbstractServlet
{
    /**
     * Initializes the servlet state.
     *
     * @throws ServletException When approriate.
     */
    @Override
    public void init()
        throws ServletException
    {
        _context = (MetadataContext) getServletContext()
            .getAttribute(MetadataServerModule.METADATA_CONTEXT_ATTRIBUTE);
        setLogID(_context.getLogID());
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doPost(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws IOException
    {
        final XMLDocument requestDocument = parseRequest(request, response);

        if (requestDocument == null) {
            return;
        }

        final MetadataSelector selector = new MetadataSelector();

        try {
            selector.acceptRequestDocument(requestDocument);
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());

            return;
        }

        final MetadataContext metadataHolder = _context;

        metadataHolder.lockMetadata();

        try {
            final Metadata masterMetadata = metadataHolder
                .getMetadata()
                .orElse(null);

            if (masterMetadata == null) {
                response
                    .sendError(
                        HttpServletResponse.SC_GONE,
                        "Metadata not available");

                return;
            }

            final Optional<String> domain = selector.getDomain();

            if (domain.isPresent()
                    && !masterMetadata.getDomain().equalsIgnoreCase(
                        domain.get())) {
                response
                    .sendError(
                        HttpServletResponse.SC_GONE,
                        "Metadata not available for domain '" + domain.get()
                        + "'");

                return;
            }

            final Optional<Metadata> selectedMetadata = selector
                .selectFrom(masterMetadata);

            if (!selectedMetadata.isPresent()) {
                response
                    .sendError(
                        HttpServletResponse.SC_NO_CONTENT,
                        "Metadata unchanged");

                return;
            }

            XMLDocument document = MetadataExporter
                .export(
                    selectedMetadata.get(),
                    Optional.of(selector.getUsages()),
                    Optional.of(selector.getLangs()),
                    true);

            try {
                document = metadataHolder.prepareXML(document);
            } catch (final Exception exception) {
                response
                    .sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        exception.getMessage());

                return;
            }

            DateTime metadataStamp = masterMetadata.getStamp().orElse(null);

            if (metadataStamp == null) {
                metadataStamp = DateTime.now();
            }

            response.setHeader(METADATA_STAMP_HEADER, metadataStamp.toString());
            sendPostResponse(request, response, document);
        } finally {
            metadataHolder.unlockMetadata();
        }
    }

    /** Metadata stamp HTTP header. */
    public static final String METADATA_STAMP_HEADER = "RVPF-Metadata-stamp";
    private static final long serialVersionUID = 1L;

    private transient volatile MetadataContext _context;
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
