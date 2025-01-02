package com.beat.global.common.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

public class MysqlCustomDialect extends MySQLDialect {

	public MysqlCustomDialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		// 상위 MySQLDialect에 설정된 기본 함수들 먼저 등록
		super.initializeFunctionRegistry(functionContributions);

		// Query functions 등록하기
		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

		// 1) 단일 칼럼 Full-Text Search: match( column, keyword )
		functionRegistry.register("match", MatchFunction.INSTANCE);

		// 2) 다중 칼럼 Full-Text Search: matchs( column1, column2, keyword )
		functionRegistry.register("matchs", MatchsFunction.INSTANCE);
	}

	/**
	 * 단일 칼럼에 대해 MATCH(...) AGAINST(... IN BOOLEAN MODE) 수행
	 * function('match', col, keyword) 형태로 사용 가능
	 */
	public static class MatchFunction extends NamedSqmFunctionDescriptor {

		public static final MatchFunction INSTANCE = new MatchFunction();

		public MatchFunction() {
			super(
				"MATCH",
				false,
				StandardArgumentsValidators.exactly(2), // 인자 2개(col, keyword)
				null
			);
		}

		@Override
		public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator
		) {
			// MATCH( {0} ) AGAINST ( {1} IN BOOLEAN MODE )
			sqlAppender.appendSql("MATCH(");
			translator.render(arguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(") AGAINST (");
			translator.render(arguments.get(1), SqlAstNodeRenderingMode.DEFAULT);
			sqlAppender.appendSql(" IN BOOLEAN MODE)");
		}
	}

	/**
	 * 여러 칼럼에 대해 MATCH(...) AGAINST(... IN BOOLEAN MODE) 수행
	 * function('matchs', col1, col2, keyword) 형태로 사용 가능
	 */
	public static class MatchsFunction extends NamedSqmFunctionDescriptor {

		public static final MatchsFunction INSTANCE = new MatchsFunction();

		public MatchsFunction() {
			super(
				"MATCHS",
				false,
				StandardArgumentsValidators.exactly(3), // 인자 3개(col1, col2, keyword)
				null
			);
		}

		@Override
		public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator
		) {
			// MATCH( {0}, {1} ) AGAINST ( {2} IN BOOLEAN MODE )
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
