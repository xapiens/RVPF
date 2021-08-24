# $Id: config-login.py 813 2008-04-09 20:41:10Z SFB $

from config_env import SettingsConfigurator
import sys

_SETTINGS = (
    ("User", 'user', None,  None),
    ("Password", 'password', None, None),
)

_ENV_FILE = None

_PREFS_PATH = '/org/rvpf/env/login'


if __name__ == '__main__':
    sys.exit(
        SettingsConfigurator(
            "Login configuration",
            _SETTINGS,
            _PREFS_PATH,
            _ENV_FILE)())


# End.
