/* $Id: PostInstallPanel.java 2508 2015-01-24 22:01:24Z SFB $
 */
package com.izforge.izpack.panels;

import org.rvpf.tool.PostInstallAction;
import org.rvpf.tool.PostInstallAction.ActionFailureException;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;

/** Post-install panel.
 */
public class PostInstallPanel
    extends IzPanel
{
    /** Constructs an instance.
     *
     * @param frame The installer frame.
     * @param data The install date.
     */
    public PostInstallPanel(final InstallerFrame frame, final InstallData data)
    {
        super(frame, data);

        setHidden(true);
    }

    /** {@inheritDoc}
     */
    @Override
    public void panelActivate()
    {
        final String className =
            System.getProperty(INSTALL_CLASS_PROPERTY, "").trim();

        try {
            if (className.isEmpty()) {
                return;
            }

            final Class<?> actionClass = Class.forName(className);
            final PostInstallAction action =
                (PostInstallAction) actionClass.getConstructor().newInstance();

            action.onInstallDone(this.parent, this.idata.getInstallPath());
        } catch (final ClassCastException exception) {
            this.idata.installSuccess = false;
            emitError("installer error", "action class '" + className
                + "' is inappropriate for post-install");
        } catch (final ClassNotFoundException exception) {
            this.idata.installSuccess = false;
            emitError("installer error", "action class '" + className
                + "' is unknown");
        } catch (final ActionFailureException exception) {
            this.idata.installSuccess = false;
            emitError("installer error", exception.getMessage());
        } catch (final Exception exception) {
            this.idata.installSuccess = false;
            emitError("installer error", "failed to instantiate action class '"
                + className + "'");
        } finally {
            this.parent.skipPanel();
        }
    }

    /** Install class property. */
    public static final String INSTALL_CLASS_PROPERTY =
        "rvpf.jnlp.install.class";

    private static final long serialVersionUID = 1L;
}

//End.
