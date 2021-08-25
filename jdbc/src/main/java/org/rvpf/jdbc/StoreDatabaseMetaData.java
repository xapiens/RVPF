/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreDatabaseMetaData.java 3973 2019-05-10 16:49:57Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;

import java.util.Optional;

/**
 * Store Database Meta Data.
 */
final class StoreDatabaseMetaData
    implements DatabaseMetaData
{
    /**
     * Constructs an instance.
     *
     * @param connection The store connection instance.
     */
    StoreDatabaseMetaData(final StoreConnection connection)
    {
        _connection = connection;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean allProceduresAreCallable()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean allTablesAreSelectable()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean autoCommitFailureClosesAllResultSets()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean dataDefinitionCausesTransactionCommit()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean dataDefinitionIgnoredInTransactions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean deletesAreDetected(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean doesMaxRowSizeIncludeBlobs()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean generatedKeyAlwaysReturned()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getAttributes(
            final String catalog,
            final String schemaPattern,
            final String typeNamePattern,
            final String attributeNamePattern)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getBestRowIdentifier(
            final String catalog,
            final String schema,
            final String table,
            final int scope,
            final boolean nullable)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("SCOPE"),
                Optional.of("Scope"),
                32,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_SIZE"),
                Optional.of("Column size"),
                10,
                Types.INTEGER,
                4,
                0,
                true,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("BUFFER_LENGTH"),
                Optional.of("Buffer length"),
                6,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("DECIMAL_DIGITS"),
                Optional.of("Decimal digits"),
                3,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("PSEUDO_COLUMN"),
                Optional.of("Pseudo column"),
                4,
                Types.SMALLINT,
                4,
                0,
                false,
                Optional.of(Short.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getCatalogSeparator()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public String getCatalogTerm()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getCatalogs()
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Catalog name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getClientInfoProperties()
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("NAME"),
                Optional.of("Name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("MAX_LEN"),
                Optional.of("Maximum length"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("DEFAULT_VALUE"),
                Optional.of("Default value"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DESCRIPTION"),
                Optional.of("Description"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getColumnPrivileges(
            final String catalog,
            final String schema,
            final String table,
            final String columnNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("GRANTOR"),
                Optional.of("Grantor"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("GRANTEE"),
                Optional.of("Grantee"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PRIVILEGE"),
                Optional.of("Privilege"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("IS_GRANTABLE"),
                Optional.of("Is grantable"),
                3,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getColumns(
            final String catalog,
            final String schemaPattern,
            final String tableNamePattern,
            final String columnNamePattern)
        throws SQLException
    {
        final String tableName = StoreDriver.getTableName(tableNamePattern);
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_SIZE"),
                Optional.of("Column size"),
                10,
                Types.INTEGER,
                10,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("BUFFER_LENGTH"),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.of("DECIMAL_DIGITS"),
                Optional.of("Fractional digits"),
                2,
                Types.INTEGER,
                2,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("NUM_PREC_RADIX"),
                Optional.empty(),
                0,
                Types.INTEGER,
                2,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("NULLABLE"),
                Optional.of("Nullable"),
                1,
                Types.INTEGER,
                1,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Column remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_DEF"),
                Optional.of("Default value"),
                16,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SQL_DATA_TYPE"),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.of("SQL_DATETIME_SUB"),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.of("CHAR_OCTET_LENGTH"),
                Optional.empty(),
                10,
                Types.INTEGER,
                10,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("ORDINAL_POSITION"),
                Optional.of("Column number"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("IS_NULLABLE"),
                Optional.of("Is nullable"),
                5,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SCOPE_CATLOG"),
                Optional.of("Scope catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SCOPE_SCHEMA"),
                Optional.of("Scope schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SCOPE_TABLE"),
                Optional.of("Scope table"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SOURCE_DATA_TYPE"),
                Optional.of("Source data type"),
                32,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));

        _addColumnRow(tableName, StoreColumn.POINT_NAME, 1, resultSet);
        _addColumnRow(tableName, StoreColumn.POINT_UUID, 2, resultSet);

        if (tableName == StoreDriver.ARCHIVE_TABLE) {
            _addColumnRow(tableName, StoreColumn.STAMP, 3, resultSet);
            _addColumnRow(tableName, StoreColumn.VERSION, 4, resultSet);
            _addColumnRow(tableName, StoreColumn.STATE, 5, resultSet);
            _addColumnRow(tableName, StoreColumn.VALUE, 6, resultSet);
        }

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public Connection getConnection()
        throws SQLException
    {
        return _connection;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getCrossReference(
            final String primaryCatalog,
            final String primarySchema,
            final String primaryTable,
            final String foreignCatalog,
            final String foreignSchema,
            final String foreignTable)
        throws SQLException
    {
        return getExportedKeys(null, null, null);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getDatabaseMajorVersion()
        throws SQLException
    {
        return _DATABASE_MAJOR_VERSION;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getDatabaseMinorVersion()
        throws SQLException
    {
        return _DATABASE_MINOR_VERSION;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDatabaseProductName()
        throws SQLException
    {
        return _DATABASE_PRODUCT_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDatabaseProductVersion()
        throws SQLException
    {
        return getDatabaseMajorVersion() + "." + getDatabaseMinorVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getDefaultTransactionIsolation()
        throws SQLException
    {
        return Connection.TRANSACTION_READ_COMMITTED;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getDriverMajorVersion()
    {
        return _connection.getDriver().getMajorVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getDriverMinorVersion()
    {
        return _connection.getDriver().getMinorVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDriverName()
        throws SQLException
    {
        return StoreDriver.getDriverName();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDriverVersion()
        throws SQLException
    {
        return StoreDriver.getVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getExportedKeys(
            final String catalog,
            final String schema,
            final String table)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("PKTABLE_CAT"),
                Optional.of("Primary key table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PKTABLE_SCHEM"),
                Optional.of("Primary key table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PKTABLE_NAME"),
                Optional.of("Primary key table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PKCOLUMN_NAME"),
                Optional.of("Primary key column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FKTABLE_CAT"),
                Optional.of("Foreign key table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FKTABLE_SCHEM"),
                Optional.of("Foreign key table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FKTABLE_NAME"),
                Optional.of("Foreign key table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FKCOLUMN_NAME"),
                Optional.of("Foreign key column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("KEY_SEQ"),
                Optional.of("Sequence number"),
                4,
                Types.SMALLINT,
                4,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("UPDATE_RULE"),
                Optional.of("Update rule"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("DELETE_RULE"),
                Optional.of("Delete rule"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("FK_NAME"),
                Optional.of("Foreign key name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PK_NAME"),
                Optional.of("Primary key name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DEFERRABILITY"),
                Optional.of("Deferrability"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getExtraNameCharacters()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getFunctionColumns(
            final String catalog,
            final String schemaPattern,
            final String functionNamePattern,
            final String columnNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("FUNCTION_CAT"),
                Optional.of("Function catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FUNCTION_SCHEM"),
                Optional.of("Function schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FUNCTION_NAME"),
                Optional.of("Function name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_TYPE"),
                Optional.of("Column type"),
                32,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PRECISION"),
                Optional.of("Precision"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("LENGTH"),
                Optional.of("Length"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("SCALE"),
                Optional.of("Scale"),
                4,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("RADIX"),
                Optional.of("Radix"),
                4,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("NULLABLE"),
                Optional.of("Nullable"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("CHAR_OCTET_LENGTH"),
                Optional.empty(),
                10,
                Types.INTEGER,
                10,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("ORDINAL_POSITION"),
                Optional.of("Column number"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("IS_NULLABLE"),
                Optional.of("Is nullable"),
                5,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SPECIFIC_NAME"),
                Optional.of("Specific name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getFunctions(
            final String catalog,
            final String schemaPattern,
            final String functionNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("FUNCTION_CAT"),
                Optional.of("Function catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FUNCTION_SCHEM"),
                Optional.of("Function schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FUNCTION_NAME"),
                Optional.of("Function name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Function remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("FUNCTION_TYPE"),
                Optional.of("Function type"),
                32,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("SPECIFIC_NAME"),
                Optional.of("Specific name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getIdentifierQuoteString()
        throws SQLException
    {
        return " ";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getImportedKeys(
            final String catalog,
            final String schema,
            final String table)
        throws SQLException
    {
        return getExportedKeys(null, null, null);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getIndexInfo(
            final String catalog,
            final String schema,
            final String table,
            final boolean unique,
            final boolean approximate)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("NON_UNIQUE"),
                Optional.of("Non-unique"),
                5,
                Types.BOOLEAN,
                0,
                0,
                false,
                Optional.of(Boolean.class));
        resultSet
            .addColumn(
                Optional.of("INDEX_QUALIFIER"),
                Optional.of("Index qualifier"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("INDEX_NAME"),
                Optional.of("Index name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE"),
                Optional.of("Type"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("ORDINAL_POSITION"),
                Optional.of("Ordinal position"),
                3,
                Types.SMALLINT,
                3,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("ASC_OR_DSC"),
                Optional.of("ASC or DSC"),
                1,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("CARDINALITY"),
                Optional.of("Cardinality"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("PAGES"),
                Optional.of("Pages"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("FILTER_CONDITION"),
                Optional.of("Filter condition"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getJDBCMajorVersion()
        throws SQLException
    {
        return 4;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getJDBCMinorVersion()
        throws SQLException
    {
        return 2;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxBinaryLiteralLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxCatalogNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxCharLiteralLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnsInGroupBy()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnsInIndex()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnsInOrderBy()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnsInSelect()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxColumnsInTable()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxConnections()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxCursorNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxIndexLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxProcedureNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxRowSize()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxSchemaNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxStatementLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxStatements()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxTableNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxTablesInSelect()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMaxUserNameLength()
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getNumericFunctions()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getPrimaryKeys(
            final String catalog,
            final String schema,
            final String table)
        throws SQLException
    {
        final String tableName = StoreDriver.getTableName(table);
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("KEY_SEQ"),
                Optional.of("Sequence number"),
                4,
                Types.SMALLINT,
                4,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("PK_NAME"),
                Optional.of("Primary key name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        resultSet
            .addRow(
                null,
                null,
                tableName,
                StoreColumn.POINT_UUID.name(),
                Short.valueOf((short) 1),
                null);

        if (tableName == StoreDriver.ARCHIVE_TABLE) {
            resultSet
                .addRow(
                    null,
                    null,
                    tableName,
                    StoreColumn.STAMP.name(),
                    Short.valueOf((short) 2),
                    null);
        }

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getProcedureColumns(
            final String catalog,
            final String schemaPattern,
            final String procedureNamePattern,
            final String columnNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("PROCEDURE_CAT"),
                Optional.of("Procedure catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PROCEDURE_SCHEM"),
                Optional.of("Procedure schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PROCEDURE_NAME"),
                Optional.of("Procedure name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_TYPE"),
                Optional.of("Column type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PRECISION"),
                Optional.of("Precision"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("LENGTH"),
                Optional.of("Length"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("SCALE"),
                Optional.of("Scale"),
                4,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("RADIX"),
                Optional.of("Radix"),
                4,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("NULLABLE"),
                Optional.of("Nullable"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getProcedureTerm()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getProcedures(
            final String catalog,
            final String schemaPattern,
            final String procedureNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("PROCEDURE_CAT"),
                Optional.of("Procedure catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PROCEDURE_SCHEM"),
                Optional.of("Procedure schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PROCEDURE_NAME"),
                Optional.of("Procedure name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.empty(),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.empty(),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.empty(),
                Optional.empty(),
                0,
                Types.NULL,
                0,
                0,
                true,
                Optional.empty());
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Procedure remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PROCEDURE_TYPE"),
                Optional.of("Procedure type"),
                32,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getPseudoColumns(
            final String catalog,
            final String schemaPattern,
            final String tableNamePattern,
            final String columnNamePattern)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getResultSetHoldability()
        throws SQLException
    {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /** {@inheritDoc}
     */
    @Override
    public RowIdLifetime getRowIdLifetime()
        throws SQLException
    {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSQLKeywords()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public int getSQLStateType()
        throws SQLException
    {
        return sqlStateSQL99;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSchemaTerm()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getSchemas()
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Schema name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_CATALOG"),
                Optional.of("Catalog name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getSchemas(
            final String catalog,
            final String schemaPattern)
        throws SQLException
    {
        return getSchemas();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSearchStringEscape()
        throws SQLException
    {
        return "\\";
    }

    /** {@inheritDoc}
     */
    @Override
    public String getStringFunctions()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getSuperTables(
            final String catalog,
            final String schemaPattern,
            final String tableNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SUPERTABLE_NAME"),
                Optional.of("Supertable name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getSuperTypes(
            final String catalog,
            final String schemaPattern,
            final String typeNamePattern)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSystemFunctions()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getTablePrivileges(
            final String catalog,
            final String schemaPattern,
            final String tableNamePattern)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("GRANTOR"),
                Optional.of("Grantor"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("GRANTEE"),
                Optional.of("Grantee"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("PRIVILEGE"),
                Optional.of("Privilege"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("IS_GRANTABLE"),
                Optional.of("Is grantable"),
                3,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getTableTypes()
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_TYPE"),
                Optional.of("Table type"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));

        resultSet.addRow(_TABLE_TYPE);

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getTables(
            final String catalog,
            final String schemaPattern,
            final String tableNamePattern,
            final String[] types)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TABLE_CAT"),
                Optional.of("Table catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_SCHEM"),
                Optional.of("Table schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_NAME"),
                Optional.of("Table name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TABLE_TYPE"),
                Optional.of("Table type"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Table remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_CAT"),
                Optional.of("Types catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_SCHEM"),
                Optional.of("Types schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("SELF_REFERENCING_COL_NAME"),
                Optional.empty(),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("REF_GENERATION"),
                Optional.empty(),
                8,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));

        resultSet
            .addRow(
                null,
                null,
                StoreDriver.ARCHIVE_TABLE,
                _TABLE_TYPE,
                "",
                null,
                null,
                null,
                null,
                null);
        resultSet
            .addRow(
                null,
                null,
                StoreDriver.POINTS_TABLE,
                _TABLE_TYPE,
                "",
                null,
                null,
                null,
                null,
                null);

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTimeDateFunctions()
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getTypeInfo()
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("SQL data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("PRECISION"),
                Optional.of("Maximum precision"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("LITERAL_PREFIX"),
                Optional.of("Literal prefix"),
                2,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("LITERAL_SUFFIX"),
                Optional.of("Literal suffix"),
                2,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("CREATE_PARAMS"),
                Optional.of("Parameters for creation"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("NULLABLE"),
                Optional.of("Nullable"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("CASE_SENSITIVE"),
                Optional.of("Case sensitive"),
                5,
                Types.BOOLEAN,
                0,
                0,
                false,
                Optional.of(Boolean.class));
        resultSet
            .addColumn(
                Optional.of("SEARCHABLE"),
                Optional.of("Searchable"),
                1,
                Types.SMALLINT,
                1,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("UNSIGNED_ATTRIBUTE"),
                Optional.of("Unsigned"),
                5,
                Types.BOOLEAN,
                0,
                0,
                false,
                Optional.of(Boolean.class));
        resultSet
            .addColumn(
                Optional.of("FIXED_PREC_SCALE"),
                Optional.of("Money"),
                5,
                Types.BOOLEAN,
                0,
                0,
                false,
                Optional.of(Boolean.class));
        resultSet
            .addColumn(
                Optional.of("AUTO_INCREMENT"),
                Optional.of("Auto increment"),
                5,
                Types.BOOLEAN,
                0,
                0,
                false,
                Optional.of(Boolean.class));
        resultSet
            .addColumn(
                Optional.of("LOCAL_TYPE_NAME"),
                Optional.of("Localized type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("MINIMUM_SCALE"),
                Optional.of("Minimum scale"),
                2,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("MAXIMUM_SCALE"),
                Optional.of("Maximum scale"),
                10,
                Types.SMALLINT,
                0,
                0,
                false,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("SQL_DATA_TYPE"),
                Optional.empty(),
                0,
                Types.INTEGER,
                0,
                0,
                true,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("SQL_DATETIME_SUB"),
                Optional.empty(),
                0,
                Types.INTEGER,
                0,
                0,
                true,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("NUM_PREC_RADIX"),
                Optional.empty(),
                2,
                Types.INTEGER,
                0,
                0,
                false,
                Optional.of(Integer.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getUDTs(
            final String catalog,
            final String schemaPattern,
            final String typeNamePattern,
            final int[] types)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("TYPE_CAT"),
                Optional.of("Type catalog"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_SCHEM"),
                Optional.of("Type schema"),
                32,
                Types.VARCHAR,
                0,
                0,
                true,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("CLASS_NAME"),
                Optional.of("Class name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("REMARKS"),
                Optional.of("Type remarks"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("BASE_TYPE"),
                Optional.of("Base type"),
                4,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getURL()
        throws SQLException
    {
        return _connection.getServerURI().toString();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getUserName()
        throws SQLException
    {
        return _connection.getUser().orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet getVersionColumns(
            final String catalog,
            final String schema,
            final String table)
        throws SQLException
    {
        final DefaultResultSet resultSet = _connection.createResultSet();

        resultSet
            .addColumn(
                Optional.of("SCOPE"),
                Optional.of("Scope"),
                4,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_NAME"),
                Optional.of("Column name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("DATA_TYPE"),
                Optional.of("Data type"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("TYPE_NAME"),
                Optional.of("Type name"),
                32,
                Types.VARCHAR,
                0,
                0,
                false,
                Optional.of(String.class));
        resultSet
            .addColumn(
                Optional.of("COLUMN_SIZE"),
                Optional.of("Column size"),
                4,
                Types.INTEGER,
                4,
                0,
                true,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("BUFFER_LENGTH"),
                Optional.of("Buffer length"),
                4,
                Types.INTEGER,
                4,
                0,
                false,
                Optional.of(Integer.class));
        resultSet
            .addColumn(
                Optional.of("DECIMAL_DIGITS"),
                Optional.of("Decimal digits"),
                4,
                Types.SMALLINT,
                4,
                0,
                true,
                Optional.of(Short.class));
        resultSet
            .addColumn(
                Optional.of("PSEUDO_COLUMN"),
                Optional.of("Pseudo column"),
                4,
                Types.SMALLINT,
                4,
                0,
                false,
                Optional.of(Short.class));

        return resultSet;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean insertsAreDetected(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isCatalogAtStart()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final Class<?> iface)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean locatorsUpdateCopy()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean nullPlusNonNullIsNull()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean nullsAreSortedAtEnd()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean nullsAreSortedAtStart()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean nullsAreSortedHigh()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean nullsAreSortedLow()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean othersDeletesAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean othersInsertsAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean othersUpdatesAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean ownDeletesAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean ownInsertsAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean ownUpdatesAreVisible(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesLowerCaseIdentifiers()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesLowerCaseQuotedIdentifiers()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesMixedCaseIdentifiers()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesUpperCaseIdentifiers()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean storesUpperCaseQuotedIdentifiers()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsANSI92EntryLevelSQL()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsANSI92FullSQL()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsANSI92IntermediateSQL()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsAlterTableWithAddColumn()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsAlterTableWithDropColumn()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsBatchUpdates()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCatalogsInDataManipulation()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCatalogsInIndexDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCatalogsInProcedureCalls()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCatalogsInTableDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsColumnAliasing()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsConvert()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsConvert(
            final int fromType,
            final int toType)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCoreSQLGrammar()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCorrelatedSubqueries()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDataManipulationTransactionsOnly()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDifferentTableCorrelationNames()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsExpressionsInOrderBy()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsExtendedSQLGrammar()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsFullOuterJoins()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsGetGeneratedKeys()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsGroupBy()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsGroupByBeyondSelect()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsGroupByUnrelated()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsIntegrityEnhancementFacility()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsLikeEscapeClause()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsLimitedOuterJoins()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMinimumSQLGrammar()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMixedCaseIdentifiers()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMultipleOpenResults()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMultipleResultSets()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsMultipleTransactions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsNamedParameters()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsNonNullableColumns()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOpenCursorsAcrossCommit()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOpenCursorsAcrossRollback()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOpenStatementsAcrossCommit()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOpenStatementsAcrossRollback()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOrderByUnrelated()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsOuterJoins()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPositionedDelete()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPositionedUpdate()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsResultSetConcurrency(
            final int type,
            final int concurrency)
        throws SQLException
    {
        return supportsResultSetType(type)
               && ((concurrency == ResultSet.CONCUR_READ_ONLY)
                   || (concurrency == ResultSet.CONCUR_UPDATABLE));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsResultSetHoldability(
            final int holdability)
        throws SQLException
    {
        return (holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT)
               || (holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsResultSetType(final int type)
        throws SQLException
    {
        return (type == ResultSet.TYPE_FORWARD_ONLY)
               || (type == ResultSet.TYPE_SCROLL_INSENSITIVE);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSavepoints()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSchemasInDataManipulation()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSchemasInIndexDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSchemasInPrivilegeDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSchemasInProcedureCalls()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSchemasInTableDefinitions()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSelectForUpdate()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsStatementPooling()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsStoredProcedures()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubqueriesInComparisons()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubqueriesInExists()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubqueriesInIns()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubqueriesInQuantifieds()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsTableCorrelationNames()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsTransactionIsolationLevel(
            final int level)
        throws SQLException
    {
        return level == Connection.TRANSACTION_READ_COMMITTED;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsTransactions()
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsUnion()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsUnionAll()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean updatesAreDetected(final int type)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesLocalFilePerTable()
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesLocalFiles()
        throws SQLException
    {
        return false;
    }

    private static void _addColumnRow(
            final String tableName,
            final StoreColumn column,
            final int position,
            final DefaultResultSet resultSet)
    {
        resultSet
            .addRow(
                null,
                null,
                tableName,
                column.name(),
                Integer.valueOf(column.getType()),
                column.getTypeName(),
                Integer.valueOf(0),
                null,
                Integer.valueOf(0),
                Integer.valueOf(10),
                Integer
                    .valueOf(column.isNullable()
                    ? columnNullable: columnNoNulls),
                null,
                null,
                null,
                null,
                Integer.valueOf(0),
                Integer.valueOf(position),
                column.isNullable()? "YES": "NO",
                null,
                null,
                null,
                null);
    }

    private static final int _DATABASE_MAJOR_VERSION = 0;
    private static final int _DATABASE_MINOR_VERSION = 8;
    private static final String _DATABASE_PRODUCT_NAME = "RVPF-Store";
    private static final String _TABLE_TYPE = "TABLE";

    private final StoreConnection _connection;
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
