// $Id: time.js 2713 2015-07-28 18:18:05Z SFB $

'use strict';

var DateTime = Java.type('org.rvpf.base.DateTime');

print(DateTime.now().toMillis() / 1000);

// End.
