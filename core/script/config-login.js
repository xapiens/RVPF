// $Id: config-login.js 3050 2016-06-04 19:56:35Z SFB $

'use strict';

load("script/lib/config_env.js");

var _SETTINGS = [
    {
        title: "User",
        name: 'user',
        symbol: null,
        defaultValue: null
    },
    {
        title: "Password",
        name: 'password',
        symbol: null,
        defaultValue: null
    },
];
var _ENV_FILE = null;
var _PREFS_PATH = '/org/rvpf/env/login';

new SettingsConfigurator(
    "config-login",
    "Login configuration",
    _SETTINGS,
    _PREFS_PATH,
    _ENV_FILE,
    arguments).configure();

// End.
