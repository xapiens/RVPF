package org.xendra.xendrian.server;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.service.metadata.app.MetadataServiceApp;

/** XendraServiceApp.
 */
public interface XendrianServiceApp
    extends MetadataServiceApp
{
    /**
     * Gets the store server instance.
     *
     * @return The store server instance.
     */
    @Nonnull
    @CheckReturnValue
    XendrianServer getServer();

    /**
     * Gets the server name.
     *
     * @return The server name.
     */
    @Nonnull
    @CheckReturnValue
    String getServerName();
}

