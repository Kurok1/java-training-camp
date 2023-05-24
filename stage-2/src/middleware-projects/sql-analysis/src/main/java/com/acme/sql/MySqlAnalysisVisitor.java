package com.acme.sql;

import com.acme.sql.antlr4.SqlAnalysisBaseVisitor;
import com.acme.sql.antlr4.SqlAnalysisParser;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class MySqlAnalysisVisitor extends SqlAnalysisBaseVisitor<DmlMetaData> {

    private DmlMetaData sqlMetaData = new DmlMetaData();

    @Override
    public DmlMetaData visitTable_name(SqlAnalysisParser.Table_nameContext ctx) {
        sqlMetaData.setTableName(ctx.getText());
        return super.visitTable_name(ctx);
    }

    @Override
    public DmlMetaData visitInsert(SqlAnalysisParser.InsertContext ctx) {
        sqlMetaData.setType("insert");
        return super.visitInsert(ctx);
    }

    @Override
    public DmlMetaData visitUpdate(SqlAnalysisParser.UpdateContext ctx) {
        sqlMetaData.setType("update");
        return super.visitUpdate(ctx);
    }

    @Override
    public DmlMetaData visitDelete(SqlAnalysisParser.DeleteContext ctx) {
        sqlMetaData.setType("delete");
        return super.visitDelete(ctx);
    }

    @Override
    protected DmlMetaData defaultResult() {
        return this.sqlMetaData;
    }
}
