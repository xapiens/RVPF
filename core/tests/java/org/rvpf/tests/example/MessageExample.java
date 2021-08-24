/** Related Values Processing Framework.
 *
 * $Id: MessageExample.java 2283 2014-06-27 13:54:27Z SFB $
 */
package org.rvpf.tests.example;

import java.util.Objects;
import org.rvpf.base.DateTime;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.io.Serializable;
import org.rvpf.base.UUID;

/** Message example.
 */
@XStreamAlias("Example")
public class MessageExample
    implements Serializable
{
    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof MessageExample) {
            final MessageExample other = (MessageExample) object;

            return Objects.equals(getKey(), other.getKey())
                && Objects.equals(getReference(), other.getReference());
        }

        return false;
    }

    /** Gets the key.
     *
     * @return The key.
     */
    public UUID getKey()
    {
        return _key;
    }

    /** Gets the stamp.
     *
     * @return The stamp.
     */
    public DateTime getStamp()
    {
        return _stamp;
    }

    /** Gets the reference.
     *
     * @return The reference.
     */
    public String getReference()
    {
        return _reference;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** Sets the key.
     *
     * @param key The key.
     */
    public void setKey(final UUID key)
    {
        _key = key;
    }

    /** Sets the stamp.
     *
     * @param stamp The stamp.
     */
    public void setStamp(final DateTime stamp)
    {
        _stamp = stamp;
    }

    /** Sets the reference.
     *
     * @param reference The reference.
     */
    public void setReference(final String reference)
    {
        _reference = reference;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getReference();
    }

    private static final long serialVersionUID = 1L;

    @XStreamAlias("uuid")
    @XStreamAsAttribute
    private UUID _key = UUID.synthesize(0L, 0, 0, 0L);

    @XStreamAlias("stamp")
    @XStreamAsAttribute
    private DateTime _stamp = DateTime.fromMillis(0);

    @XStreamAlias("reference")
    @XStreamAsAttribute
    private String _reference = "TEST";
}

//End.
