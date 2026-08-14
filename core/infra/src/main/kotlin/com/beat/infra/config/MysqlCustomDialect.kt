package com.beat.infra.config

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.dialect.MySQLDialect
import org.hibernate.metamodel.model.domain.ReturnableType
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators
import org.hibernate.sql.ast.SqlAstNodeRenderingMode
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode

class MysqlCustomDialect : MySQLDialect() {
    override fun initializeFunctionRegistry(functionContributions: FunctionContributions) {
        super.initializeFunctionRegistry(functionContributions)

        val functionRegistry = functionContributions.functionRegistry
        functionRegistry.register("match", MatchFunction.INSTANCE)
        functionRegistry.register("matchs", MatchsFunction.INSTANCE)
    }

    /** Supports single-column full-text search via function('match', column, keyword). */
    class MatchFunction :
        NamedSqmFunctionDescriptor(
            "MATCH",
            false,
            StandardArgumentsValidators.exactly(2),
            null,
        ) {
        override fun render(
            sqlAppender: SqlAppender,
            arguments: List<SqlAstNode>,
            returnType: ReturnableType<*>?,
            translator: SqlAstTranslator<*>,
        ) {
            sqlAppender.appendSql("MATCH(")
            translator.render(arguments[0], SqlAstNodeRenderingMode.DEFAULT)
            sqlAppender.appendSql(") AGAINST (")
            translator.render(arguments[1], SqlAstNodeRenderingMode.DEFAULT)
            sqlAppender.appendSql(" IN BOOLEAN MODE)")
        }

        companion object {
            val INSTANCE = MatchFunction()
        }
    }

    /** Supports multi-column full-text search via function('matchs', col1, col2, keyword). */
    class MatchsFunction :
        NamedSqmFunctionDescriptor(
            "MATCHS",
            false,
            StandardArgumentsValidators.exactly(3),
            null,
        ) {
        override fun render(
            sqlAppender: SqlAppender,
            arguments: List<SqlAstNode>,
            returnType: ReturnableType<*>?,
            translator: SqlAstTranslator<*>,
        ) {
            sqlAppender.appendSql("MATCH(")
            translator.render(arguments[0], SqlAstNodeRenderingMode.DEFAULT)
            sqlAppender.appendSql(",")
            translator.render(arguments[1], SqlAstNodeRenderingMode.DEFAULT)
            sqlAppender.appendSql(") AGAINST (")
            translator.render(arguments[2], SqlAstNodeRenderingMode.DEFAULT)
            sqlAppender.appendSql(" IN BOOLEAN MODE)")
        }

        companion object {
            val INSTANCE = MatchsFunction()
        }
    }
}
