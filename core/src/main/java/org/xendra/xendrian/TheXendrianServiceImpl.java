package org.xendra.xendrian;

import java.util.Optional;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.app.ServiceApp;
import org.rvpf.service.app.ServiceAppHolderImpl;

/** TheXendrianServiceImpl.
 */
public final class TheXendrianServiceImpl
    extends ServiceAppHolderImpl
{
    /**
     * Gets the service activator for a service name or alias.
     *
     * @param serviceKey The service name or alias.
     *
     * @return The service activator (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ServiceActivator> getServiceActivator(
            @Nonnull final String serviceKey)
    {
        final TheXendrianServiceAppImpl mainApp =
            (TheXendrianServiceAppImpl) getServiceApp();

        return mainApp.getServiceActivator(serviceKey);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceApp newServiceApp()
    {
        return new TheXendrianServiceAppImpl();
    }
        
}

