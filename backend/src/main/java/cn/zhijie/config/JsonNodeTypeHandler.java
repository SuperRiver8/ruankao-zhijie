package cn.zhijie.config;

import cn.zhijie.util.Support;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.*;
import org.apache.ibatis.type.*;

@MappedTypes(JsonNode.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode value, JdbcType type)
        throws SQLException {
        ps.setObject(i, value.toString(), Types.OTHER);
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, String name) throws SQLException {
        return parse(rs.getString(name));
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, int index) throws SQLException {
        return parse(rs.getString(index));
    }

    @Override
    public JsonNode getNullableResult(CallableStatement cs, int index) throws SQLException {
        return parse(cs.getString(index));
    }

    private JsonNode parse(String value) {
        return value == null ? null : Support.tree(value);
    }
}
