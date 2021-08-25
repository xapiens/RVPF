// $Id: test-jssc.groovy 3418 2017-05-05 20:16:03Z SFB $

import jssc.*

def _usage()
{
    System.println("Usage: " + this.class.name + " <port name>")
    for (final String portName: SerialPortList.getPortNames()) {
        println "Serial port: " + portName
    }
    System.exit(1);
}

if (args.length != 1) _usage()

portName = args[0]

serialPort = new SerialPort(portName)
println "Port opened: " + serialPort.openPort()
println "Set params: " + serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN)

println "Purge: " + serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR)

println "Write 'A': " + serialPort.writeByte((byte) 65)
println "Read bytes: " + serialPort.readBytes(1)

println "Port closed: " + serialPort.closePort()

// End.
