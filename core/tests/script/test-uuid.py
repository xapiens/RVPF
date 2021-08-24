import sys

sys.path.append('../build/classes')
from org.rvpf.base import UUID

print " 0 1 2 3 4 5 6 7 8"
for i in xrange(5):
    uuid = UUID.generate()
    print uuid, uuid.toName()
