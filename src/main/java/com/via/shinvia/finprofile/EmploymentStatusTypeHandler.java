package com.via.shinvia.finprofile;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(EmploymentStatus.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public class EmploymentStatusTypeHandler extends BaseTypeHandler<EmploymentStatus> {

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            EmploymentStatus parameter,
            JdbcType jdbcType
    ) throws SQLException {
        statement.setString(index, parameter.name());
    }

    @Override
    public EmploymentStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return EmploymentStatus.fromDatabaseValue(resultSet.getString(columnName));
    }

    @Override
    public EmploymentStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return EmploymentStatus.fromDatabaseValue(resultSet.getString(columnIndex));
    }

    @Override
    public EmploymentStatus getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return EmploymentStatus.fromDatabaseValue(statement.getString(columnIndex));
    }
}
