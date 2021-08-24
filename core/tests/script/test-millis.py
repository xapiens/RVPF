from java.lang import *

startMillis = System.currentTimeMillis()
skipped = 0
while 1:
    nextMillis = System.currentTimeMillis()
    if nextMillis != startMillis: break
    skipped += 1

print "Skipped %i duplicate value(s), then got a difference of %i millis." % (skipped, nextMillis - startMillis)
