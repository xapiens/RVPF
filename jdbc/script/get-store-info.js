// $Id: get-store-info.js 3186 2016-09-23 01:07:19Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var StoreDataSource = Java.type('org.rvpf.jdbc.StoreDataSource');

var RMI_REGISTRY_PORT = 10100;

var dataSource = new StoreDataSource();

dataSource.setPort(RMI_REGISTRY_PORT);

var connection = dataSource.getConnection();
var metaData = connection.getMetaData();

print("Driver name: " + metaData.getDriverName());
print("Driver version: " + metaData.getDriverVersion());
print("Driver JDBC version: " + metaData.getJDBCMajorVersion() + "." + metaData.getJDBCMinorVersion());

var statement = connection.createStatement();
var resultSet = statement.executeQuery("SELECT COUNT(*)");

resultSet.next();
print("Store entries: " + resultSet.getObject(1));
resultSet.close();
statement.close();

connection.close();

// End.
