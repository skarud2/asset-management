package com.via.shinvia.finprofile;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(IncomeType.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public class IncomeTypeHandler extends BaseTypeHandler<IncomeType> {

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            IncomeType parameter,
            JdbcType jdbcType
    ) throws SQLException {
        statement.setString(index, parameter.name());
    }

    @Override
    public IncomeType getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return IncomeType.fromDatabaseValue(resultSet.getString(columnName));
    }

    @Override
    public IncomeType getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return IncomeType.fromDatabaseValue(resultSet.getString(columnIndex));
    }

    @Override
    public IncomeType getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return IncomeType.fromDatabaseValue(statement.getString(columnIndex));
    }
}
