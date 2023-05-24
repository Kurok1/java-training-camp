package com.acme.sql;

import com.acme.sql.antlr4.SqlAnalysisLexer;
import com.acme.sql.antlr4.SqlAnalysisParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MySqlAnalysisVisitorTest {

    @Test
    public void testInsert() {
        String sql = "INSERT INTO `test`(id, name, age) VALUES(1, '张三', 18);";
        DmlMetaData accept = getSqlMetaData(sql);
        assertEquals("insert", accept.getType());
        assertEquals("`test`", accept.getTableName());
    }


    private static DmlMetaData getSqlMetaData(String sql) {
        var baseSqlLexer = new SqlAnalysisLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(baseSqlLexer);
        var parser = new SqlAnalysisParser(tokens);
        ParseTree tree = parser.sqlDML();

        return tree.accept(new MySqlAnalysisVisitor());
    }


}