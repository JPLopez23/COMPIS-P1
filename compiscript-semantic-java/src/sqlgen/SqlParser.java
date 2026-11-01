// Generated from grammar/Sql.g4 by ANTLR 4.13.2
package sqlgen;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class SqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CREATE=1, DROP=2, TABLE=3, INDEX=4, ON=5, INSERT=6, INTO=7, VALUES=8, 
		UPDATE=9, SET=10, DELETE=11, SELECT=12, FROM=13, WHERE=14, AND=15, OR=16, 
		NOT=17, ORDER=18, GROUP=19, BY=20, HAVING=21, ASC=22, DESC=23, INNER=24, 
		JOIN=25, AS=26, IN=27, EXISTS=28, IS=29, NULL=30, PRIMARY=31, KEY=32, 
		COUNT=33, SUM=34, AVG=35, MIN=36, MAX=37, TRUE=38, FALSE=39, INT=40, FLOAT=41, 
		VARCHAR=42, BOOLEAN=43, DATE=44, EQ=45, NEQ=46, LE=47, GE=48, LT=49, GT=50, 
		PLUS=51, MINUS=52, STAR=53, SLASH=54, LPAREN=55, RPAREN=56, COMMA=57, 
		SEMI=58, DOT=59, FLOATLIT=60, INTLIT=61, STRING=62, ID=63, LINE_COMMENT=64, 
		BLOCK_COMMENT=65, WS=66;
	public static final int
		RULE_script = 0, RULE_statement = 1, RULE_createTable = 2, RULE_columnDef = 3, 
		RULE_columnConstraint = 4, RULE_dataType = 5, RULE_dropTable = 6, RULE_createIndex = 7, 
		RULE_dropIndex = 8, RULE_insertStmt = 9, RULE_valueRow = 10, RULE_updateStmt = 11, 
		RULE_assignment = 12, RULE_deleteStmt = 13, RULE_selectStmt = 14, RULE_selectItem = 15, 
		RULE_tableRef = 16, RULE_joinClause = 17, RULE_orderItem = 18, RULE_expr = 19, 
		RULE_literal = 20, RULE_ident = 21;
	private static String[] makeRuleNames() {
		return new String[] {
			"script", "statement", "createTable", "columnDef", "columnConstraint", 
			"dataType", "dropTable", "createIndex", "dropIndex", "insertStmt", "valueRow", 
			"updateStmt", "assignment", "deleteStmt", "selectStmt", "selectItem", 
			"tableRef", "joinClause", "orderItem", "expr", "literal", "ident"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'create'", "'drop'", "'table'", "'index'", "'on'", "'insert'", 
			"'into'", "'values'", "'update'", "'set'", "'delete'", "'select'", "'from'", 
			"'where'", "'and'", "'or'", "'not'", "'order'", "'group'", "'by'", "'having'", 
			"'asc'", "'desc'", "'inner'", "'join'", "'as'", "'in'", "'exists'", "'is'", 
			"'null'", "'primary'", "'key'", "'count'", "'sum'", "'avg'", "'min'", 
			"'max'", "'true'", "'false'", "'int'", "'float'", "'varchar'", "'boolean'", 
			"'date'", "'='", null, "'<='", "'>='", "'<'", "'>'", "'+'", "'-'", "'*'", 
			"'/'", "'('", "')'", "','", "';'", "'.'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CREATE", "DROP", "TABLE", "INDEX", "ON", "INSERT", "INTO", "VALUES", 
			"UPDATE", "SET", "DELETE", "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", 
			"ORDER", "GROUP", "BY", "HAVING", "ASC", "DESC", "INNER", "JOIN", "AS", 
			"IN", "EXISTS", "IS", "NULL", "PRIMARY", "KEY", "COUNT", "SUM", "AVG", 
			"MIN", "MAX", "TRUE", "FALSE", "INT", "FLOAT", "VARCHAR", "BOOLEAN", 
			"DATE", "EQ", "NEQ", "LE", "GE", "LT", "GT", "PLUS", "MINUS", "STAR", 
			"SLASH", "LPAREN", "RPAREN", "COMMA", "SEMI", "DOT", "FLOATLIT", "INTLIT", 
			"STRING", "ID", "LINE_COMMENT", "BLOCK_COMMENT", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Sql.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ScriptContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(SqlParser.EOF, 0); }
		public List<TerminalNode> SEMI() { return getTokens(SqlParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(SqlParser.SEMI, i);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ScriptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_script; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitScript(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScriptContext script() throws RecognitionException {
		ScriptContext _localctx = new ScriptContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_script);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMI) {
				{
				{
				setState(44);
				match(SEMI);
				}
				}
				setState(49);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(68);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6726L) != 0)) {
				{
				setState(50);
				statement();
				setState(59);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(52); 
						_errHandler.sync(this);
						_la = _input.LA(1);
						do {
							{
							{
							setState(51);
							match(SEMI);
							}
							}
							setState(54); 
							_errHandler.sync(this);
							_la = _input.LA(1);
						} while ( _la==SEMI );
						setState(56);
						statement();
						}
						} 
					}
					setState(61);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				}
				setState(65);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SEMI) {
					{
					{
					setState(62);
					match(SEMI);
					}
					}
					setState(67);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(70);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public CreateTableContext createTable() {
			return getRuleContext(CreateTableContext.class,0);
		}
		public DropTableContext dropTable() {
			return getRuleContext(DropTableContext.class,0);
		}
		public CreateIndexContext createIndex() {
			return getRuleContext(CreateIndexContext.class,0);
		}
		public DropIndexContext dropIndex() {
			return getRuleContext(DropIndexContext.class,0);
		}
		public InsertStmtContext insertStmt() {
			return getRuleContext(InsertStmtContext.class,0);
		}
		public UpdateStmtContext updateStmt() {
			return getRuleContext(UpdateStmtContext.class,0);
		}
		public DeleteStmtContext deleteStmt() {
			return getRuleContext(DeleteStmtContext.class,0);
		}
		public SelectStmtContext selectStmt() {
			return getRuleContext(SelectStmtContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		try {
			setState(80);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(72);
				createTable();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(73);
				dropTable();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(74);
				createIndex();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(75);
				dropIndex();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(76);
				insertStmt();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(77);
				updateStmt();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(78);
				deleteStmt();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(79);
				selectStmt();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateTableContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(SqlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(SqlParser.TABLE, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public List<ColumnDefContext> columnDef() {
			return getRuleContexts(ColumnDefContext.class);
		}
		public ColumnDefContext columnDef(int i) {
			return getRuleContext(ColumnDefContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public CreateTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTable; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitCreateTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableContext createTable() throws RecognitionException {
		CreateTableContext _localctx = new CreateTableContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_createTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			match(CREATE);
			setState(83);
			match(TABLE);
			setState(84);
			ident();
			setState(85);
			match(LPAREN);
			setState(86);
			columnDef();
			setState(91);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(87);
				match(COMMA);
				setState(88);
				columnDef();
				}
				}
				setState(93);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(94);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnDefContext extends ParserRuleContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public List<ColumnConstraintContext> columnConstraint() {
			return getRuleContexts(ColumnConstraintContext.class);
		}
		public ColumnConstraintContext columnConstraint(int i) {
			return getRuleContext(ColumnConstraintContext.class,i);
		}
		public ColumnDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnDef; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitColumnDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnDefContext columnDef() throws RecognitionException {
		ColumnDefContext _localctx = new ColumnDefContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_columnDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
			ident();
			setState(97);
			dataType();
			setState(101);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NOT || _la==PRIMARY) {
				{
				{
				setState(98);
				columnConstraint();
				}
				}
				setState(103);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnConstraintContext extends ParserRuleContext {
		public ColumnConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnConstraint; }
	 
		public ColumnConstraintContext() { }
		public void copyFrom(ColumnConstraintContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PkConstraintContext extends ColumnConstraintContext {
		public TerminalNode PRIMARY() { return getToken(SqlParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(SqlParser.KEY, 0); }
		public PkConstraintContext(ColumnConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitPkConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotNullConstraintContext extends ColumnConstraintContext {
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(SqlParser.NULL, 0); }
		public NotNullConstraintContext(ColumnConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitNotNullConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnConstraintContext columnConstraint() throws RecognitionException {
		ColumnConstraintContext _localctx = new ColumnConstraintContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_columnConstraint);
		try {
			setState(108);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PRIMARY:
				_localctx = new PkConstraintContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(104);
				match(PRIMARY);
				setState(105);
				match(KEY);
				}
				break;
			case NOT:
				_localctx = new NotNullConstraintContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(106);
				match(NOT);
				setState(107);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DataTypeContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(SqlParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(SqlParser.FLOAT, 0); }
		public TerminalNode VARCHAR() { return getToken(SqlParser.VARCHAR, 0); }
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public TerminalNode INTLIT() { return getToken(SqlParser.INTLIT, 0); }
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public TerminalNode BOOLEAN() { return getToken(SqlParser.BOOLEAN, 0); }
		public TerminalNode DATE() { return getToken(SqlParser.DATE, 0); }
		public DataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataType; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_dataType);
		try {
			setState(118);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(110);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 2);
				{
				setState(111);
				match(FLOAT);
				}
				break;
			case VARCHAR:
				enterOuterAlt(_localctx, 3);
				{
				setState(112);
				match(VARCHAR);
				setState(113);
				match(LPAREN);
				setState(114);
				match(INTLIT);
				setState(115);
				match(RPAREN);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(116);
				match(BOOLEAN);
				}
				break;
			case DATE:
				enterOuterAlt(_localctx, 5);
				{
				setState(117);
				match(DATE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropTableContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(SqlParser.DROP, 0); }
		public TerminalNode TABLE() { return getToken(SqlParser.TABLE, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public DropTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropTable; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitDropTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DropTableContext dropTable() throws RecognitionException {
		DropTableContext _localctx = new DropTableContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_dropTable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			match(DROP);
			setState(121);
			match(TABLE);
			setState(122);
			ident();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateIndexContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(SqlParser.CREATE, 0); }
		public TerminalNode INDEX() { return getToken(SqlParser.INDEX, 0); }
		public List<IdentContext> ident() {
			return getRuleContexts(IdentContext.class);
		}
		public IdentContext ident(int i) {
			return getRuleContext(IdentContext.class,i);
		}
		public TerminalNode ON() { return getToken(SqlParser.ON, 0); }
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public CreateIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createIndex; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitCreateIndex(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateIndexContext createIndex() throws RecognitionException {
		CreateIndexContext _localctx = new CreateIndexContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_createIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			match(CREATE);
			setState(125);
			match(INDEX);
			setState(126);
			ident();
			setState(127);
			match(ON);
			setState(128);
			ident();
			setState(129);
			match(LPAREN);
			setState(130);
			ident();
			setState(131);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropIndexContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(SqlParser.DROP, 0); }
		public TerminalNode INDEX() { return getToken(SqlParser.INDEX, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public DropIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropIndex; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitDropIndex(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DropIndexContext dropIndex() throws RecognitionException {
		DropIndexContext _localctx = new DropIndexContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_dropIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			match(DROP);
			setState(134);
			match(INDEX);
			setState(135);
			ident();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertStmtContext extends ParserRuleContext {
		public TerminalNode INSERT() { return getToken(SqlParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(SqlParser.INTO, 0); }
		public List<IdentContext> ident() {
			return getRuleContexts(IdentContext.class);
		}
		public IdentContext ident(int i) {
			return getRuleContext(IdentContext.class,i);
		}
		public TerminalNode VALUES() { return getToken(SqlParser.VALUES, 0); }
		public List<ValueRowContext> valueRow() {
			return getRuleContexts(ValueRowContext.class);
		}
		public ValueRowContext valueRow(int i) {
			return getRuleContext(ValueRowContext.class,i);
		}
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public InsertStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertStmt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitInsertStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertStmtContext insertStmt() throws RecognitionException {
		InsertStmtContext _localctx = new InsertStmtContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_insertStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			match(INSERT);
			setState(138);
			match(INTO);
			setState(139);
			ident();
			setState(151);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(140);
				match(LPAREN);
				setState(141);
				ident();
				setState(146);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(142);
					match(COMMA);
					setState(143);
					ident();
					}
					}
					setState(148);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(149);
				match(RPAREN);
				}
			}

			setState(153);
			match(VALUES);
			setState(154);
			valueRow();
			setState(159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(155);
				match(COMMA);
				setState(156);
				valueRow();
				}
				}
				setState(161);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValueRowContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public ValueRowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueRow; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitValueRow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueRowContext valueRow() throws RecognitionException {
		ValueRowContext _localctx = new ValueRowContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_valueRow);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(LPAREN);
			setState(163);
			expr(0);
			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(164);
				match(COMMA);
				setState(165);
				expr(0);
				}
				}
				setState(170);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(171);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UpdateStmtContext extends ParserRuleContext {
		public TerminalNode UPDATE() { return getToken(SqlParser.UPDATE, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode SET() { return getToken(SqlParser.SET, 0); }
		public List<AssignmentContext> assignment() {
			return getRuleContexts(AssignmentContext.class);
		}
		public AssignmentContext assignment(int i) {
			return getRuleContext(AssignmentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public TerminalNode WHERE() { return getToken(SqlParser.WHERE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public UpdateStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_updateStmt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitUpdateStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateStmtContext updateStmt() throws RecognitionException {
		UpdateStmtContext _localctx = new UpdateStmtContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_updateStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			match(UPDATE);
			setState(174);
			ident();
			setState(175);
			match(SET);
			setState(176);
			assignment();
			setState(181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(177);
				match(COMMA);
				setState(178);
				assignment();
				}
				}
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(186);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(184);
				match(WHERE);
				setState(185);
				expr(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentContext extends ParserRuleContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode EQ() { return getToken(SqlParser.EQ, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(188);
			ident();
			setState(189);
			match(EQ);
			setState(190);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeleteStmtContext extends ParserRuleContext {
		public TerminalNode DELETE() { return getToken(SqlParser.DELETE, 0); }
		public TerminalNode FROM() { return getToken(SqlParser.FROM, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(SqlParser.WHERE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public DeleteStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteStmt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitDeleteStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteStmtContext deleteStmt() throws RecognitionException {
		DeleteStmtContext _localctx = new DeleteStmtContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_deleteStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(DELETE);
			setState(193);
			match(FROM);
			setState(194);
			ident();
			setState(197);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(195);
				match(WHERE);
				setState(196);
				expr(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectStmtContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(SqlParser.SELECT, 0); }
		public List<SelectItemContext> selectItem() {
			return getRuleContexts(SelectItemContext.class);
		}
		public SelectItemContext selectItem(int i) {
			return getRuleContext(SelectItemContext.class,i);
		}
		public TerminalNode FROM() { return getToken(SqlParser.FROM, 0); }
		public TableRefContext tableRef() {
			return getRuleContext(TableRefContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public List<JoinClauseContext> joinClause() {
			return getRuleContexts(JoinClauseContext.class);
		}
		public JoinClauseContext joinClause(int i) {
			return getRuleContext(JoinClauseContext.class,i);
		}
		public TerminalNode WHERE() { return getToken(SqlParser.WHERE, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode GROUP() { return getToken(SqlParser.GROUP, 0); }
		public List<TerminalNode> BY() { return getTokens(SqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(SqlParser.BY, i);
		}
		public TerminalNode HAVING() { return getToken(SqlParser.HAVING, 0); }
		public TerminalNode ORDER() { return getToken(SqlParser.ORDER, 0); }
		public List<OrderItemContext> orderItem() {
			return getRuleContexts(OrderItemContext.class);
		}
		public OrderItemContext orderItem(int i) {
			return getRuleContext(OrderItemContext.class,i);
		}
		public SelectStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectStmt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitSelectStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectStmtContext selectStmt() throws RecognitionException {
		SelectStmtContext _localctx = new SelectStmtContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_selectStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(199);
			match(SELECT);
			setState(200);
			selectItem();
			setState(205);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(201);
				match(COMMA);
				setState(202);
				selectItem();
				}
				}
				setState(207);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(208);
			match(FROM);
			setState(209);
			tableRef();
			setState(213);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==INNER || _la==JOIN) {
				{
				{
				setState(210);
				joinClause();
				}
				}
				setState(215);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(218);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(216);
				match(WHERE);
				setState(217);
				expr(0);
				}
			}

			setState(230);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(220);
				match(GROUP);
				setState(221);
				match(BY);
				setState(222);
				expr(0);
				setState(227);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(223);
					match(COMMA);
					setState(224);
					expr(0);
					}
					}
					setState(229);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(234);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(232);
				match(HAVING);
				setState(233);
				expr(0);
				}
			}

			setState(246);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(236);
				match(ORDER);
				setState(237);
				match(BY);
				setState(238);
				orderItem();
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(239);
					match(COMMA);
					setState(240);
					orderItem();
					}
					}
					setState(245);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectItemContext extends ParserRuleContext {
		public SelectItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectItem; }
	 
		public SelectItemContext() { }
		public void copyFrom(SelectItemContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprItemContext extends SelectItemContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode AS() { return getToken(SqlParser.AS, 0); }
		public ExprItemContext(SelectItemContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprItem(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TableStarItemContext extends SelectItemContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode DOT() { return getToken(SqlParser.DOT, 0); }
		public TerminalNode STAR() { return getToken(SqlParser.STAR, 0); }
		public TableStarItemContext(SelectItemContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTableStarItem(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StarItemContext extends SelectItemContext {
		public TerminalNode STAR() { return getToken(SqlParser.STAR, 0); }
		public StarItemContext(SelectItemContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitStarItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectItemContext selectItem() throws RecognitionException {
		SelectItemContext _localctx = new SelectItemContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_selectItem);
		int _la;
		try {
			setState(260);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				_localctx = new StarItemContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(248);
				match(STAR);
				}
				break;
			case 2:
				_localctx = new TableStarItemContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(249);
				ident();
				setState(250);
				match(DOT);
				setState(251);
				match(STAR);
				}
				break;
			case 3:
				_localctx = new ExprItemContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(253);
				expr(0);
				setState(258);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS || _la==ID) {
					{
					setState(255);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(254);
						match(AS);
						}
					}

					setState(257);
					ident();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TableRefContext extends ParserRuleContext {
		public List<IdentContext> ident() {
			return getRuleContexts(IdentContext.class);
		}
		public IdentContext ident(int i) {
			return getRuleContext(IdentContext.class,i);
		}
		public TerminalNode AS() { return getToken(SqlParser.AS, 0); }
		public TableRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableRef; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTableRef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableRefContext tableRef() throws RecognitionException {
		TableRefContext _localctx = new TableRefContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_tableRef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			ident();
			setState(267);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS || _la==ID) {
				{
				setState(264);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(263);
					match(AS);
					}
				}

				setState(266);
				ident();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class JoinClauseContext extends ParserRuleContext {
		public TerminalNode JOIN() { return getToken(SqlParser.JOIN, 0); }
		public TableRefContext tableRef() {
			return getRuleContext(TableRefContext.class,0);
		}
		public TerminalNode ON() { return getToken(SqlParser.ON, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode INNER() { return getToken(SqlParser.INNER, 0); }
		public JoinClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitJoinClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinClauseContext joinClause() throws RecognitionException {
		JoinClauseContext _localctx = new JoinClauseContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_joinClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INNER) {
				{
				setState(269);
				match(INNER);
				}
			}

			setState(272);
			match(JOIN);
			setState(273);
			tableRef();
			setState(274);
			match(ON);
			setState(275);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderItemContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode ASC() { return getToken(SqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(SqlParser.DESC, 0); }
		public OrderItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderItem; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitOrderItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderItemContext orderItem() throws RecognitionException {
		OrderItemContext _localctx = new OrderItemContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_orderItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			expr(0);
			setState(279);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(278);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class InSubqueryExprContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode IN() { return getToken(SqlParser.IN, 0); }
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public SelectStmtContext selectStmt() {
			return getRuleContext(SelectStmtContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public InSubqueryExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitInSubqueryExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OrExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode OR() { return getToken(SqlParser.OR, 0); }
		public OrExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitOrExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExistsExprContext extends ExprContext {
		public TerminalNode EXISTS() { return getToken(SqlParser.EXISTS, 0); }
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public SelectStmtContext selectStmt() {
			return getRuleContext(SelectStmtContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public ExistsExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExistsExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenExprContext extends ExprContext {
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public ParenExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitParenExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CmpExprContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode EQ() { return getToken(SqlParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(SqlParser.NEQ, 0); }
		public TerminalNode LT() { return getToken(SqlParser.LT, 0); }
		public TerminalNode LE() { return getToken(SqlParser.LE, 0); }
		public TerminalNode GT() { return getToken(SqlParser.GT, 0); }
		public TerminalNode GE() { return getToken(SqlParser.GE, 0); }
		public CmpExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitCmpExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ColumnExprContext extends ExprContext {
		public List<IdentContext> ident() {
			return getRuleContexts(IdentContext.class);
		}
		public IdentContext ident(int i) {
			return getRuleContext(IdentContext.class,i);
		}
		public TerminalNode DOT() { return getToken(SqlParser.DOT, 0); }
		public ColumnExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitColumnExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotExprContext extends ExprContext {
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NotExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitNotExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AddExprContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode PLUS() { return getToken(SqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(SqlParser.MINUS, 0); }
		public AddExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitAddExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NegExprContext extends ExprContext {
		public TerminalNode MINUS() { return getToken(SqlParser.MINUS, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NegExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitNegExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IsNullExprContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode IS() { return getToken(SqlParser.IS, 0); }
		public TerminalNode NULL() { return getToken(SqlParser.NULL, 0); }
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public IsNullExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitIsNullExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LiteralExprContext extends ExprContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LiteralExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class InListExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode IN() { return getToken(SqlParser.IN, 0); }
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SqlParser.COMMA, i);
		}
		public InListExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitInListExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MulExprContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode STAR() { return getToken(SqlParser.STAR, 0); }
		public TerminalNode SLASH() { return getToken(SqlParser.SLASH, 0); }
		public MulExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitMulExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ScalarSubqueryExprContext extends ExprContext {
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public SelectStmtContext selectStmt() {
			return getRuleContext(SelectStmtContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public ScalarSubqueryExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitScalarSubqueryExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AggregateExprContext extends ExprContext {
		public Token fn;
		public TerminalNode LPAREN() { return getToken(SqlParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SqlParser.RPAREN, 0); }
		public TerminalNode COUNT() { return getToken(SqlParser.COUNT, 0); }
		public TerminalNode SUM() { return getToken(SqlParser.SUM, 0); }
		public TerminalNode AVG() { return getToken(SqlParser.AVG, 0); }
		public TerminalNode MIN() { return getToken(SqlParser.MIN, 0); }
		public TerminalNode MAX() { return getToken(SqlParser.MAX, 0); }
		public TerminalNode STAR() { return getToken(SqlParser.STAR, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AggregateExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitAggregateExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AndExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode AND() { return getToken(SqlParser.AND, 0); }
		public AndExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitAndExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 38;
		enterRecursionRule(_localctx, 38, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(313);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				{
				_localctx = new ParenExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(282);
				match(LPAREN);
				setState(283);
				expr(0);
				setState(284);
				match(RPAREN);
				}
				break;
			case 2:
				{
				_localctx = new NegExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(286);
				match(MINUS);
				setState(287);
				expr(15);
				}
				break;
			case 3:
				{
				_localctx = new NotExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(288);
				match(NOT);
				setState(289);
				expr(8);
				}
				break;
			case 4:
				{
				_localctx = new ExistsExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(290);
				match(EXISTS);
				setState(291);
				match(LPAREN);
				setState(292);
				selectStmt();
				setState(293);
				match(RPAREN);
				}
				break;
			case 5:
				{
				_localctx = new ScalarSubqueryExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(295);
				match(LPAREN);
				setState(296);
				selectStmt();
				setState(297);
				match(RPAREN);
				}
				break;
			case 6:
				{
				_localctx = new AggregateExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(299);
				((AggregateExprContext)_localctx).fn = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 266287972352L) != 0)) ) {
					((AggregateExprContext)_localctx).fn = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(300);
				match(LPAREN);
				setState(303);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case STAR:
					{
					setState(301);
					match(STAR);
					}
					break;
				case NOT:
				case EXISTS:
				case NULL:
				case COUNT:
				case SUM:
				case AVG:
				case MIN:
				case MAX:
				case TRUE:
				case FALSE:
				case MINUS:
				case LPAREN:
				case FLOATLIT:
				case INTLIT:
				case STRING:
				case ID:
					{
					setState(302);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(305);
				match(RPAREN);
				}
				break;
			case 7:
				{
				_localctx = new LiteralExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(306);
				literal();
				}
				break;
			case 8:
				{
				_localctx = new ColumnExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(310);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
				case 1:
					{
					setState(307);
					ident();
					setState(308);
					match(DOT);
					}
					break;
				}
				setState(312);
				ident();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(363);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,40,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(361);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
					case 1:
						{
						_localctx = new MulExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(315);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(316);
						((MulExprContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==STAR || _la==SLASH) ) {
							((MulExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(317);
						expr(15);
						}
						break;
					case 2:
						{
						_localctx = new AddExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(318);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(319);
						((AddExprContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
							((AddExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(320);
						expr(14);
						}
						break;
					case 3:
						{
						_localctx = new CmpExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(321);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(322);
						((CmpExprContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2216615441596416L) != 0)) ) {
							((CmpExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(323);
						expr(13);
						}
						break;
					case 4:
						{
						_localctx = new AndExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(324);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(325);
						match(AND);
						setState(326);
						expr(8);
						}
						break;
					case 5:
						{
						_localctx = new OrExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(327);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(328);
						match(OR);
						setState(329);
						expr(7);
						}
						break;
					case 6:
						{
						_localctx = new IsNullExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(330);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(331);
						match(IS);
						setState(333);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(332);
							match(NOT);
							}
						}

						setState(335);
						match(NULL);
						}
						break;
					case 7:
						{
						_localctx = new InSubqueryExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(336);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(338);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(337);
							match(NOT);
							}
						}

						setState(340);
						match(IN);
						setState(341);
						match(LPAREN);
						setState(342);
						selectStmt();
						setState(343);
						match(RPAREN);
						}
						break;
					case 8:
						{
						_localctx = new InListExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(345);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(347);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(346);
							match(NOT);
							}
						}

						setState(349);
						match(IN);
						setState(350);
						match(LPAREN);
						setState(351);
						expr(0);
						setState(356);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==COMMA) {
							{
							{
							setState(352);
							match(COMMA);
							setState(353);
							expr(0);
							}
							}
							setState(358);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(359);
						match(RPAREN);
						}
						break;
					}
					} 
				}
				setState(365);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,40,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode INTLIT() { return getToken(SqlParser.INTLIT, 0); }
		public TerminalNode FLOATLIT() { return getToken(SqlParser.FLOATLIT, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TerminalNode TRUE() { return getToken(SqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(SqlParser.FALSE, 0); }
		public TerminalNode NULL() { return getToken(SqlParser.NULL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8070451357955391488L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SqlParser.ID, 0); }
		public IdentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ident; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentContext ident() throws RecognitionException {
		IdentContext _localctx = new IdentContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_ident);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(368);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 19:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 14);
		case 1:
			return precpred(_ctx, 13);
		case 2:
			return precpred(_ctx, 12);
		case 3:
			return precpred(_ctx, 7);
		case 4:
			return precpred(_ctx, 6);
		case 5:
			return precpred(_ctx, 11);
		case 6:
			return precpred(_ctx, 10);
		case 7:
			return precpred(_ctx, 9);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001B\u0173\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0001\u0000\u0005\u0000.\b\u0000\n\u0000\f\u00001\t\u0000\u0001\u0000"+
		"\u0001\u0000\u0004\u00005\b\u0000\u000b\u0000\f\u00006\u0001\u0000\u0005"+
		"\u0000:\b\u0000\n\u0000\f\u0000=\t\u0000\u0001\u0000\u0005\u0000@\b\u0000"+
		"\n\u0000\f\u0000C\t\u0000\u0003\u0000E\b\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0003\u0001Q\b\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002"+
		"Z\b\u0002\n\u0002\f\u0002]\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0005\u0003d\b\u0003\n\u0003\f\u0003g\t\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004m\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005w\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0005\t\u0091\b\t\n\t\f\t\u0094\t\t\u0001\t\u0001\t\u0003\t\u0098\b"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u009e\b\t\n\t\f\t\u00a1\t\t"+
		"\u0001\n\u0001\n\u0001\n\u0001\n\u0005\n\u00a7\b\n\n\n\f\n\u00aa\t\n\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0005\u000b\u00b4\b\u000b\n\u000b\f\u000b\u00b7\t\u000b\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u00bb\b\u000b\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003\r\u00c6\b\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e\u00cc\b\u000e\n\u000e"+
		"\f\u000e\u00cf\t\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e"+
		"\u00d4\b\u000e\n\u000e\f\u000e\u00d7\t\u000e\u0001\u000e\u0001\u000e\u0003"+
		"\u000e\u00db\b\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0005\u000e\u00e2\b\u000e\n\u000e\f\u000e\u00e5\t\u000e\u0003\u000e"+
		"\u00e7\b\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00eb\b\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e\u00f2"+
		"\b\u000e\n\u000e\f\u000e\u00f5\t\u000e\u0003\u000e\u00f7\b\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0003\u000f\u0100\b\u000f\u0001\u000f\u0003\u000f\u0103\b\u000f"+
		"\u0003\u000f\u0105\b\u000f\u0001\u0010\u0001\u0010\u0003\u0010\u0109\b"+
		"\u0010\u0001\u0010\u0003\u0010\u010c\b\u0010\u0001\u0011\u0003\u0011\u010f"+
		"\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0012\u0001\u0012\u0003\u0012\u0118\b\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0003\u0013\u0130\b\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u0137\b\u0013\u0001\u0013\u0003"+
		"\u0013\u013a\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0003\u0013\u014e\b\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u0153\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u015c\b\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u0163"+
		"\b\u0013\n\u0013\f\u0013\u0166\t\u0013\u0001\u0013\u0001\u0013\u0005\u0013"+
		"\u016a\b\u0013\n\u0013\f\u0013\u016d\t\u0013\u0001\u0014\u0001\u0014\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0000\u0001&\u0016\u0000\u0002\u0004\u0006"+
		"\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*\u0000"+
		"\u0006\u0001\u0000\u0016\u0017\u0001\u0000!%\u0001\u000056\u0001\u0000"+
		"34\u0001\u0000-2\u0003\u0000\u001e\u001e&\'<>\u019b\u0000/\u0001\u0000"+
		"\u0000\u0000\u0002P\u0001\u0000\u0000\u0000\u0004R\u0001\u0000\u0000\u0000"+
		"\u0006`\u0001\u0000\u0000\u0000\bl\u0001\u0000\u0000\u0000\nv\u0001\u0000"+
		"\u0000\u0000\fx\u0001\u0000\u0000\u0000\u000e|\u0001\u0000\u0000\u0000"+
		"\u0010\u0085\u0001\u0000\u0000\u0000\u0012\u0089\u0001\u0000\u0000\u0000"+
		"\u0014\u00a2\u0001\u0000\u0000\u0000\u0016\u00ad\u0001\u0000\u0000\u0000"+
		"\u0018\u00bc\u0001\u0000\u0000\u0000\u001a\u00c0\u0001\u0000\u0000\u0000"+
		"\u001c\u00c7\u0001\u0000\u0000\u0000\u001e\u0104\u0001\u0000\u0000\u0000"+
		" \u0106\u0001\u0000\u0000\u0000\"\u010e\u0001\u0000\u0000\u0000$\u0115"+
		"\u0001\u0000\u0000\u0000&\u0139\u0001\u0000\u0000\u0000(\u016e\u0001\u0000"+
		"\u0000\u0000*\u0170\u0001\u0000\u0000\u0000,.\u0005:\u0000\u0000-,\u0001"+
		"\u0000\u0000\u0000.1\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000\u0000"+
		"/0\u0001\u0000\u0000\u00000D\u0001\u0000\u0000\u00001/\u0001\u0000\u0000"+
		"\u00002;\u0003\u0002\u0001\u000035\u0005:\u0000\u000043\u0001\u0000\u0000"+
		"\u000056\u0001\u0000\u0000\u000064\u0001\u0000\u0000\u000067\u0001\u0000"+
		"\u0000\u000078\u0001\u0000\u0000\u00008:\u0003\u0002\u0001\u000094\u0001"+
		"\u0000\u0000\u0000:=\u0001\u0000\u0000\u0000;9\u0001\u0000\u0000\u0000"+
		";<\u0001\u0000\u0000\u0000<A\u0001\u0000\u0000\u0000=;\u0001\u0000\u0000"+
		"\u0000>@\u0005:\u0000\u0000?>\u0001\u0000\u0000\u0000@C\u0001\u0000\u0000"+
		"\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BE\u0001\u0000"+
		"\u0000\u0000CA\u0001\u0000\u0000\u0000D2\u0001\u0000\u0000\u0000DE\u0001"+
		"\u0000\u0000\u0000EF\u0001\u0000\u0000\u0000FG\u0005\u0000\u0000\u0001"+
		"G\u0001\u0001\u0000\u0000\u0000HQ\u0003\u0004\u0002\u0000IQ\u0003\f\u0006"+
		"\u0000JQ\u0003\u000e\u0007\u0000KQ\u0003\u0010\b\u0000LQ\u0003\u0012\t"+
		"\u0000MQ\u0003\u0016\u000b\u0000NQ\u0003\u001a\r\u0000OQ\u0003\u001c\u000e"+
		"\u0000PH\u0001\u0000\u0000\u0000PI\u0001\u0000\u0000\u0000PJ\u0001\u0000"+
		"\u0000\u0000PK\u0001\u0000\u0000\u0000PL\u0001\u0000\u0000\u0000PM\u0001"+
		"\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000PO\u0001\u0000\u0000\u0000"+
		"Q\u0003\u0001\u0000\u0000\u0000RS\u0005\u0001\u0000\u0000ST\u0005\u0003"+
		"\u0000\u0000TU\u0003*\u0015\u0000UV\u00057\u0000\u0000V[\u0003\u0006\u0003"+
		"\u0000WX\u00059\u0000\u0000XZ\u0003\u0006\u0003\u0000YW\u0001\u0000\u0000"+
		"\u0000Z]\u0001\u0000\u0000\u0000[Y\u0001\u0000\u0000\u0000[\\\u0001\u0000"+
		"\u0000\u0000\\^\u0001\u0000\u0000\u0000][\u0001\u0000\u0000\u0000^_\u0005"+
		"8\u0000\u0000_\u0005\u0001\u0000\u0000\u0000`a\u0003*\u0015\u0000ae\u0003"+
		"\n\u0005\u0000bd\u0003\b\u0004\u0000cb\u0001\u0000\u0000\u0000dg\u0001"+
		"\u0000\u0000\u0000ec\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000\u0000"+
		"f\u0007\u0001\u0000\u0000\u0000ge\u0001\u0000\u0000\u0000hi\u0005\u001f"+
		"\u0000\u0000im\u0005 \u0000\u0000jk\u0005\u0011\u0000\u0000km\u0005\u001e"+
		"\u0000\u0000lh\u0001\u0000\u0000\u0000lj\u0001\u0000\u0000\u0000m\t\u0001"+
		"\u0000\u0000\u0000nw\u0005(\u0000\u0000ow\u0005)\u0000\u0000pq\u0005*"+
		"\u0000\u0000qr\u00057\u0000\u0000rs\u0005=\u0000\u0000sw\u00058\u0000"+
		"\u0000tw\u0005+\u0000\u0000uw\u0005,\u0000\u0000vn\u0001\u0000\u0000\u0000"+
		"vo\u0001\u0000\u0000\u0000vp\u0001\u0000\u0000\u0000vt\u0001\u0000\u0000"+
		"\u0000vu\u0001\u0000\u0000\u0000w\u000b\u0001\u0000\u0000\u0000xy\u0005"+
		"\u0002\u0000\u0000yz\u0005\u0003\u0000\u0000z{\u0003*\u0015\u0000{\r\u0001"+
		"\u0000\u0000\u0000|}\u0005\u0001\u0000\u0000}~\u0005\u0004\u0000\u0000"+
		"~\u007f\u0003*\u0015\u0000\u007f\u0080\u0005\u0005\u0000\u0000\u0080\u0081"+
		"\u0003*\u0015\u0000\u0081\u0082\u00057\u0000\u0000\u0082\u0083\u0003*"+
		"\u0015\u0000\u0083\u0084\u00058\u0000\u0000\u0084\u000f\u0001\u0000\u0000"+
		"\u0000\u0085\u0086\u0005\u0002\u0000\u0000\u0086\u0087\u0005\u0004\u0000"+
		"\u0000\u0087\u0088\u0003*\u0015\u0000\u0088\u0011\u0001\u0000\u0000\u0000"+
		"\u0089\u008a\u0005\u0006\u0000\u0000\u008a\u008b\u0005\u0007\u0000\u0000"+
		"\u008b\u0097\u0003*\u0015\u0000\u008c\u008d\u00057\u0000\u0000\u008d\u0092"+
		"\u0003*\u0015\u0000\u008e\u008f\u00059\u0000\u0000\u008f\u0091\u0003*"+
		"\u0015\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0094\u0001\u0000"+
		"\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092\u0093\u0001\u0000"+
		"\u0000\u0000\u0093\u0095\u0001\u0000\u0000\u0000\u0094\u0092\u0001\u0000"+
		"\u0000\u0000\u0095\u0096\u00058\u0000\u0000\u0096\u0098\u0001\u0000\u0000"+
		"\u0000\u0097\u008c\u0001\u0000\u0000\u0000\u0097\u0098\u0001\u0000\u0000"+
		"\u0000\u0098\u0099\u0001\u0000\u0000\u0000\u0099\u009a\u0005\b\u0000\u0000"+
		"\u009a\u009f\u0003\u0014\n\u0000\u009b\u009c\u00059\u0000\u0000\u009c"+
		"\u009e\u0003\u0014\n\u0000\u009d\u009b\u0001\u0000\u0000\u0000\u009e\u00a1"+
		"\u0001\u0000\u0000\u0000\u009f\u009d\u0001\u0000\u0000\u0000\u009f\u00a0"+
		"\u0001\u0000\u0000\u0000\u00a0\u0013\u0001\u0000\u0000\u0000\u00a1\u009f"+
		"\u0001\u0000\u0000\u0000\u00a2\u00a3\u00057\u0000\u0000\u00a3\u00a8\u0003"+
		"&\u0013\u0000\u00a4\u00a5\u00059\u0000\u0000\u00a5\u00a7\u0003&\u0013"+
		"\u0000\u00a6\u00a4\u0001\u0000\u0000\u0000\u00a7\u00aa\u0001\u0000\u0000"+
		"\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000"+
		"\u0000\u00a9\u00ab\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000"+
		"\u0000\u00ab\u00ac\u00058\u0000\u0000\u00ac\u0015\u0001\u0000\u0000\u0000"+
		"\u00ad\u00ae\u0005\t\u0000\u0000\u00ae\u00af\u0003*\u0015\u0000\u00af"+
		"\u00b0\u0005\n\u0000\u0000\u00b0\u00b5\u0003\u0018\f\u0000\u00b1\u00b2"+
		"\u00059\u0000\u0000\u00b2\u00b4\u0003\u0018\f\u0000\u00b3\u00b1\u0001"+
		"\u0000\u0000\u0000\u00b4\u00b7\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001"+
		"\u0000\u0000\u0000\u00b5\u00b6\u0001\u0000\u0000\u0000\u00b6\u00ba\u0001"+
		"\u0000\u0000\u0000\u00b7\u00b5\u0001\u0000\u0000\u0000\u00b8\u00b9\u0005"+
		"\u000e\u0000\u0000\u00b9\u00bb\u0003&\u0013\u0000\u00ba\u00b8\u0001\u0000"+
		"\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\u0017\u0001\u0000"+
		"\u0000\u0000\u00bc\u00bd\u0003*\u0015\u0000\u00bd\u00be\u0005-\u0000\u0000"+
		"\u00be\u00bf\u0003&\u0013\u0000\u00bf\u0019\u0001\u0000\u0000\u0000\u00c0"+
		"\u00c1\u0005\u000b\u0000\u0000\u00c1\u00c2\u0005\r\u0000\u0000\u00c2\u00c5"+
		"\u0003*\u0015\u0000\u00c3\u00c4\u0005\u000e\u0000\u0000\u00c4\u00c6\u0003"+
		"&\u0013\u0000\u00c5\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000"+
		"\u0000\u0000\u00c6\u001b\u0001\u0000\u0000\u0000\u00c7\u00c8\u0005\f\u0000"+
		"\u0000\u00c8\u00cd\u0003\u001e\u000f\u0000\u00c9\u00ca\u00059\u0000\u0000"+
		"\u00ca\u00cc\u0003\u001e\u000f\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000"+
		"\u00cc\u00cf\u0001\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000"+
		"\u00cd\u00ce\u0001\u0000\u0000\u0000\u00ce\u00d0\u0001\u0000\u0000\u0000"+
		"\u00cf\u00cd\u0001\u0000\u0000\u0000\u00d0\u00d1\u0005\r\u0000\u0000\u00d1"+
		"\u00d5\u0003 \u0010\u0000\u00d2\u00d4\u0003\"\u0011\u0000\u00d3\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d4\u00d7\u0001\u0000\u0000\u0000\u00d5\u00d3"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00da"+
		"\u0001\u0000\u0000\u0000\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d8\u00d9"+
		"\u0005\u000e\u0000\u0000\u00d9\u00db\u0003&\u0013\u0000\u00da\u00d8\u0001"+
		"\u0000\u0000\u0000\u00da\u00db\u0001\u0000\u0000\u0000\u00db\u00e6\u0001"+
		"\u0000\u0000\u0000\u00dc\u00dd\u0005\u0013\u0000\u0000\u00dd\u00de\u0005"+
		"\u0014\u0000\u0000\u00de\u00e3\u0003&\u0013\u0000\u00df\u00e0\u00059\u0000"+
		"\u0000\u00e0\u00e2\u0003&\u0013\u0000\u00e1\u00df\u0001\u0000\u0000\u0000"+
		"\u00e2\u00e5\u0001\u0000\u0000\u0000\u00e3\u00e1\u0001\u0000\u0000\u0000"+
		"\u00e3\u00e4\u0001\u0000\u0000\u0000\u00e4\u00e7\u0001\u0000\u0000\u0000"+
		"\u00e5\u00e3\u0001\u0000\u0000\u0000\u00e6\u00dc\u0001\u0000\u0000\u0000"+
		"\u00e6\u00e7\u0001\u0000\u0000\u0000\u00e7\u00ea\u0001\u0000\u0000\u0000"+
		"\u00e8\u00e9\u0005\u0015\u0000\u0000\u00e9\u00eb\u0003&\u0013\u0000\u00ea"+
		"\u00e8\u0001\u0000\u0000\u0000\u00ea\u00eb\u0001\u0000\u0000\u0000\u00eb"+
		"\u00f6\u0001\u0000\u0000\u0000\u00ec\u00ed\u0005\u0012\u0000\u0000\u00ed"+
		"\u00ee\u0005\u0014\u0000\u0000\u00ee\u00f3\u0003$\u0012\u0000\u00ef\u00f0"+
		"\u00059\u0000\u0000\u00f0\u00f2\u0003$\u0012\u0000\u00f1\u00ef\u0001\u0000"+
		"\u0000\u0000\u00f2\u00f5\u0001\u0000\u0000\u0000\u00f3\u00f1\u0001\u0000"+
		"\u0000\u0000\u00f3\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f7\u0001\u0000"+
		"\u0000\u0000\u00f5\u00f3\u0001\u0000\u0000\u0000\u00f6\u00ec\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f7\u0001\u0000\u0000\u0000\u00f7\u001d\u0001\u0000"+
		"\u0000\u0000\u00f8\u0105\u00055\u0000\u0000\u00f9\u00fa\u0003*\u0015\u0000"+
		"\u00fa\u00fb\u0005;\u0000\u0000\u00fb\u00fc\u00055\u0000\u0000\u00fc\u0105"+
		"\u0001\u0000\u0000\u0000\u00fd\u0102\u0003&\u0013\u0000\u00fe\u0100\u0005"+
		"\u001a\u0000\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000\u00ff\u0100\u0001"+
		"\u0000\u0000\u0000\u0100\u0101\u0001\u0000\u0000\u0000\u0101\u0103\u0003"+
		"*\u0015\u0000\u0102\u00ff\u0001\u0000\u0000\u0000\u0102\u0103\u0001\u0000"+
		"\u0000\u0000\u0103\u0105\u0001\u0000\u0000\u0000\u0104\u00f8\u0001\u0000"+
		"\u0000\u0000\u0104\u00f9\u0001\u0000\u0000\u0000\u0104\u00fd\u0001\u0000"+
		"\u0000\u0000\u0105\u001f\u0001\u0000\u0000\u0000\u0106\u010b\u0003*\u0015"+
		"\u0000\u0107\u0109\u0005\u001a\u0000\u0000\u0108\u0107\u0001\u0000\u0000"+
		"\u0000\u0108\u0109\u0001\u0000\u0000\u0000\u0109\u010a\u0001\u0000\u0000"+
		"\u0000\u010a\u010c\u0003*\u0015\u0000\u010b\u0108\u0001\u0000\u0000\u0000"+
		"\u010b\u010c\u0001\u0000\u0000\u0000\u010c!\u0001\u0000\u0000\u0000\u010d"+
		"\u010f\u0005\u0018\u0000\u0000\u010e\u010d\u0001\u0000\u0000\u0000\u010e"+
		"\u010f\u0001\u0000\u0000\u0000\u010f\u0110\u0001\u0000\u0000\u0000\u0110"+
		"\u0111\u0005\u0019\u0000\u0000\u0111\u0112\u0003 \u0010\u0000\u0112\u0113"+
		"\u0005\u0005\u0000\u0000\u0113\u0114\u0003&\u0013\u0000\u0114#\u0001\u0000"+
		"\u0000\u0000\u0115\u0117\u0003&\u0013\u0000\u0116\u0118\u0007\u0000\u0000"+
		"\u0000\u0117\u0116\u0001\u0000\u0000\u0000\u0117\u0118\u0001\u0000\u0000"+
		"\u0000\u0118%\u0001\u0000\u0000\u0000\u0119\u011a\u0006\u0013\uffff\uffff"+
		"\u0000\u011a\u011b\u00057\u0000\u0000\u011b\u011c\u0003&\u0013\u0000\u011c"+
		"\u011d\u00058\u0000\u0000\u011d\u013a\u0001\u0000\u0000\u0000\u011e\u011f"+
		"\u00054\u0000\u0000\u011f\u013a\u0003&\u0013\u000f\u0120\u0121\u0005\u0011"+
		"\u0000\u0000\u0121\u013a\u0003&\u0013\b\u0122\u0123\u0005\u001c\u0000"+
		"\u0000\u0123\u0124\u00057\u0000\u0000\u0124\u0125\u0003\u001c\u000e\u0000"+
		"\u0125\u0126\u00058\u0000\u0000\u0126\u013a\u0001\u0000\u0000\u0000\u0127"+
		"\u0128\u00057\u0000\u0000\u0128\u0129\u0003\u001c\u000e\u0000\u0129\u012a"+
		"\u00058\u0000\u0000\u012a\u013a\u0001\u0000\u0000\u0000\u012b\u012c\u0007"+
		"\u0001\u0000\u0000\u012c\u012f\u00057\u0000\u0000\u012d\u0130\u00055\u0000"+
		"\u0000\u012e\u0130\u0003&\u0013\u0000\u012f\u012d\u0001\u0000\u0000\u0000"+
		"\u012f\u012e\u0001\u0000\u0000\u0000\u0130\u0131\u0001\u0000\u0000\u0000"+
		"\u0131\u013a\u00058\u0000\u0000\u0132\u013a\u0003(\u0014\u0000\u0133\u0134"+
		"\u0003*\u0015\u0000\u0134\u0135\u0005;\u0000\u0000\u0135\u0137\u0001\u0000"+
		"\u0000\u0000\u0136\u0133\u0001\u0000\u0000\u0000\u0136\u0137\u0001\u0000"+
		"\u0000\u0000\u0137\u0138\u0001\u0000\u0000\u0000\u0138\u013a\u0003*\u0015"+
		"\u0000\u0139\u0119\u0001\u0000\u0000\u0000\u0139\u011e\u0001\u0000\u0000"+
		"\u0000\u0139\u0120\u0001\u0000\u0000\u0000\u0139\u0122\u0001\u0000\u0000"+
		"\u0000\u0139\u0127\u0001\u0000\u0000\u0000\u0139\u012b\u0001\u0000\u0000"+
		"\u0000\u0139\u0132\u0001\u0000\u0000\u0000\u0139\u0136\u0001\u0000\u0000"+
		"\u0000\u013a\u016b\u0001\u0000\u0000\u0000\u013b\u013c\n\u000e\u0000\u0000"+
		"\u013c\u013d\u0007\u0002\u0000\u0000\u013d\u016a\u0003&\u0013\u000f\u013e"+
		"\u013f\n\r\u0000\u0000\u013f\u0140\u0007\u0003\u0000\u0000\u0140\u016a"+
		"\u0003&\u0013\u000e\u0141\u0142\n\f\u0000\u0000\u0142\u0143\u0007\u0004"+
		"\u0000\u0000\u0143\u016a\u0003&\u0013\r\u0144\u0145\n\u0007\u0000\u0000"+
		"\u0145\u0146\u0005\u000f\u0000\u0000\u0146\u016a\u0003&\u0013\b\u0147"+
		"\u0148\n\u0006\u0000\u0000\u0148\u0149\u0005\u0010\u0000\u0000\u0149\u016a"+
		"\u0003&\u0013\u0007\u014a\u014b\n\u000b\u0000\u0000\u014b\u014d\u0005"+
		"\u001d\u0000\u0000\u014c\u014e\u0005\u0011\u0000\u0000\u014d\u014c\u0001"+
		"\u0000\u0000\u0000\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u014f\u0001"+
		"\u0000\u0000\u0000\u014f\u016a\u0005\u001e\u0000\u0000\u0150\u0152\n\n"+
		"\u0000\u0000\u0151\u0153\u0005\u0011\u0000\u0000\u0152\u0151\u0001\u0000"+
		"\u0000\u0000\u0152\u0153\u0001\u0000\u0000\u0000\u0153\u0154\u0001\u0000"+
		"\u0000\u0000\u0154\u0155\u0005\u001b\u0000\u0000\u0155\u0156\u00057\u0000"+
		"\u0000\u0156\u0157\u0003\u001c\u000e\u0000\u0157\u0158\u00058\u0000\u0000"+
		"\u0158\u016a\u0001\u0000\u0000\u0000\u0159\u015b\n\t\u0000\u0000\u015a"+
		"\u015c\u0005\u0011\u0000\u0000\u015b\u015a\u0001\u0000\u0000\u0000\u015b"+
		"\u015c\u0001\u0000\u0000\u0000\u015c\u015d\u0001\u0000\u0000\u0000\u015d"+
		"\u015e\u0005\u001b\u0000\u0000\u015e\u015f\u00057\u0000\u0000\u015f\u0164"+
		"\u0003&\u0013\u0000\u0160\u0161\u00059\u0000\u0000\u0161\u0163\u0003&"+
		"\u0013\u0000\u0162\u0160\u0001\u0000\u0000\u0000\u0163\u0166\u0001\u0000"+
		"\u0000\u0000\u0164\u0162\u0001\u0000\u0000\u0000\u0164\u0165\u0001\u0000"+
		"\u0000\u0000\u0165\u0167\u0001\u0000\u0000\u0000\u0166\u0164\u0001\u0000"+
		"\u0000\u0000\u0167\u0168\u00058\u0000\u0000\u0168\u016a\u0001\u0000\u0000"+
		"\u0000\u0169\u013b\u0001\u0000\u0000\u0000\u0169\u013e\u0001\u0000\u0000"+
		"\u0000\u0169\u0141\u0001\u0000\u0000\u0000\u0169\u0144\u0001\u0000\u0000"+
		"\u0000\u0169\u0147\u0001\u0000\u0000\u0000\u0169\u014a\u0001\u0000\u0000"+
		"\u0000\u0169\u0150\u0001\u0000\u0000\u0000\u0169\u0159\u0001\u0000\u0000"+
		"\u0000\u016a\u016d\u0001\u0000\u0000\u0000\u016b\u0169\u0001\u0000\u0000"+
		"\u0000\u016b\u016c\u0001\u0000\u0000\u0000\u016c\'\u0001\u0000\u0000\u0000"+
		"\u016d\u016b\u0001\u0000\u0000\u0000\u016e\u016f\u0007\u0005\u0000\u0000"+
		"\u016f)\u0001\u0000\u0000\u0000\u0170\u0171\u0005?\u0000\u0000\u0171+"+
		"\u0001\u0000\u0000\u0000)/6;ADP[elv\u0092\u0097\u009f\u00a8\u00b5\u00ba"+
		"\u00c5\u00cd\u00d5\u00da\u00e3\u00e6\u00ea\u00f3\u00f6\u00ff\u0102\u0104"+
		"\u0108\u010b\u010e\u0117\u012f\u0136\u0139\u014d\u0152\u015b\u0164\u0169"+
		"\u016b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}