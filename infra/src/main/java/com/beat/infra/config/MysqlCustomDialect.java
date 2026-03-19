package com.beat.infra.config;

import java.util.List;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

public class MysqlCustomDialect extends MySQLDialect {

	public MysqlCustomDialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functionRegistry.register("match", MatchFunction.INSTANCE);
		functionRegistry.register("matchs", MatchsFunction.INSTANCE);
	}

	/**
	 * Supports single-column full-text search via function('match', column, keyword).
	 */
	public static class MatchFunction extends NamedSqmFunctionDescriptor {

		public static final MatchFunction INSTANCE = new MatchFunction();

		public MatchFunction() {
			super("MATCH", false, StandardArgumentsValidators.exactly(2), null);
		}

		@Override
		public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator
		) {
			sqlAppender.appendSql("MATCH(");
			translator.render(arguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(") AGAINST (");
			translator.render(arguments.get(1), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(" IN BOOLEAN MODE)");
		}
	}

	/**
	 * Supports multi-column full-text search via function('matchs', col1, col2, keyword).
	 */
	public static class MatchsFunction extends NamedSqmFunctionDescriptor {

		public static final MatchsFunction INSTANCE = new MatchsFunction();

		public MatchsFunction() {
			super("MATCHS", false, StandardArgumentsValidators.exactly(3), null);
		}

		@Override
		public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator
		) {
			sqlAppender.appendSql("MATCH(");
			translator.render(arguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(",");
			translator.render(arguments.get(1), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(") AGAINST (");
			translator.render(arguments.get(2), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(" IN BOOLEAN MODE)");
		}
	}
}
