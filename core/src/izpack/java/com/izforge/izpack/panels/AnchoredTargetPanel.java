/* $Id: AnchoredTargetPanel.java 2508 2015-01-24 22:01:24Z SFB $
 */
package com.izforge.izpack.panels;

import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Anchored target panel.
 */
public class AnchoredTargetPanel
    extends PathInputPanel
{
    /** Constructs an instance.
     *
     * @param frame The installer frame.
     * @param data The install data.
     */
    public AnchoredTargetPanel(
        final InstallerFrame frame,
        final InstallData data)
    {
        super(frame, data);
        _setDefaultInstallDir();
    }

    /** {@inheritDoc}
     */
    @Override
    public void panelActivate()
    {
        super.panelActivate();

        pathSelectionPanel.setPath(this.idata.getInstallPath());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isValidated()
    {
        if (!super.isValidated()) {
            return false;
        }

        final String path = pathSelectionPanel.getPath();
        final File installDir = new File(path);
        final String rootName = this.idata.getVariable(_ROOT_NAME_VAR);
        final String subdirName = this.idata.getVariable(_PROJECT_SUBDIR_VAR);

        if (subdirName != null) {
            if (subdirName.equals(installDir.getName())) {
                final File rootDir = installDir.getParentFile();

                if (rootDir == null || !rootName.equals(rootDir.getName())) {
                    emitError(
                        "Installer error",
                        "The parent directory must be named '" + rootName
                            + "'!");
                    return false;
                }

                this.idata.setVariable(_ROOT_PATH_VAR, rootDir
                    .getAbsolutePath());

                final boolean corePresent =
                    new File(rootDir, _CORE_DIR_NAME).isDirectory();

                this.idata.setVariable(_CORE_PRESENT_VAR,
                    Boolean.toString(corePresent));
                if (!corePresent
                    && Boolean.TRUE.toString().equalsIgnoreCase(
                        this.idata.getVariable(_CORE_REQUIRED_VAR))) {
                    emitError("Installer error", "The '" + _CORE_DIR_NAME
                        + "' install is required!");
                    return false;
                }
            } else {
                emitError(
                    "Installer error",
                    "The target directory must be named '" + subdirName
                        + "'!");
                return false;
            }
        } else if (!rootName.equals(installDir.getName())) {
            emitError("Installer error", "The target directory must be named '"
                + rootName + "'!");
            return false;
        }

        this.idata.setInstallPath(path);

        return true;
    }

    private void _setDefaultInstallDir()
    {
        final String nodePath = this.idata.getVariable(_HOME_NODE_PATH_VAR);
        File home = null;

        if (nodePath != null) {
            home = _getDefaultInstallDir(nodePath);
            if (home == null) {
                home = _getDefaultInstallDir(_RVPF_NODE_PATH);
                if (home != null) {
                    home = home.getParentFile();
                }
                if (home != null) {
                    home =
                        new File(home, this.idata.getVariable(_ROOT_NAME_VAR));
                }
            }
        }

        if (home == null) {
            home = new File(System.getProperty("user.home"));
            while (home != null && home.getAbsolutePath().contains(" ")) {
                home = home.getParentFile();
            }

            if (home != null && !home.canWrite()) {
                home = null;
            }

            if (home != null) {
                home = new File(home, this.idata.getVariable(_ROOT_NAME_VAR));
            } else {
                loadDefaultInstallDir(this.parent, this.idata);
                if (getDefaultInstallDir() != null) {
                    home = new File(getDefaultInstallDir());
                }
            }
        }

        if (home != null) {
            final String subdirName =
                this.idata.getVariable(_PROJECT_SUBDIR_VAR);

            if (subdirName != null) {
                home = new File(home, subdirName);
            }
            setDefaultInstallDir(home.getAbsolutePath());
        }

        this.idata.setInstallPath(getDefaultInstallDir());
    }

    private static File _getDefaultInstallDir(final String nodePath)
    {
        final Preferences userRoot = Preferences.userRoot();
        String home = null;

        try {
            if (userRoot.nodeExists(nodePath)) {
                home = userRoot.node(nodePath).get(_HOME_KEY, null);
            }
        } catch (final BackingStoreException exception) {
            throw new RuntimeException(exception);
        }

        return home != null? new File(home): null;
    }

    private static final long serialVersionUID = 1L;

    private static final String _CORE_DIR_NAME = "core";
    private static final String _CORE_PRESENT_VAR = "core_present";
    private static final String _CORE_REQUIRED_VAR = "core_required";
    private static final String _HOME_KEY = "home";
    private static final String _HOME_NODE_PATH_VAR = "home_node_path";
    private static final String _PROJECT_SUBDIR_VAR = "project_subdir";
    private static final String _ROOT_NAME_VAR = "root_name";
    private static final String _ROOT_PATH_VAR = "root_path";
    private static final String _RVPF_NODE_PATH = "/org/rvpf";
}

// End.
