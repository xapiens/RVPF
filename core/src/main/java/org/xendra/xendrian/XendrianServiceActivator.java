package org.xendra.xendrian;

import java.util.Optional;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceImpl;
import org.rvpf.service.ServiceMessages;

/** XendrianServiceActivator.
 */
public class XendrianServiceActivator
    extends ServiceActivator
{

    /** {@inheritDoc}
     */
    @Override
    protected ServiceImpl createServiceImpl()
    {
        return createServiceImpl(TheXendrianServiceImpl.class);
    }
    private volatile String _objectNameProperty = OBJECT_NAME_PROPERTY;
    
    /**
     * Makes an object name.
     *
     * @param nameValue The value for the 'name' key (optional).
     *
     * @return The object name.
     */
    @Nonnull
    @CheckReturnValue
    @Override
    public ObjectName makeObjectName(@Nonnull final Optional<String> nameValue)
    {
        if (nameValue.isPresent() && (nameValue.get().contains(":type="))) {
            try {
                return ObjectName.getInstance(nameValue.get());
            } catch (final MalformedObjectNameException exception) {
                throw new RuntimeException(exception);
            }
        }

        // Makes a type from the class name without its package name.
        String typeValue = getClass().getSimpleName();

        // Removes the "Activator" suffix if present.
        if (typeValue.endsWith(ACTIVATOR_CLASS_NAME_SUFFIX)) {
            typeValue = typeValue
                .substring(
                    0,
                    typeValue.length() - ACTIVATOR_CLASS_NAME_SUFFIX.length());
        }

        return makeObjectName(getDefaultDomain(), typeValue, nameValue);
    }
    
    /**
     * Gets the default JMX domain.
     *
     * @return The default JMX domain.
     */
    @Nonnull
    @CheckReturnValue    
    public static String getDefaultDomain()
    {
        return "org.xendra";
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
        throws Exception
    {     
        start(_wait);             
    }    
    private volatile boolean _wait;
}


