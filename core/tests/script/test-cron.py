from java.lang import *
import sys

sys.path.append('../build/classes')
from org.rvpf.base import DateTime
from org.rvpf.clock import Clock
from org.rvpf.clock.crontab import Parser

crontab = Parser.parse("0 3-23/12 15 3-6 *")
clock = Clock()
clock.time = DateTime.now().toMillis()
print clock
crontab.change(clock, 1)
print clock
crontab.change(clock, 1)
print clock
crontab.change(clock, 1)
print clock
