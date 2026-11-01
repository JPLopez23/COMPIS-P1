grammar Sql;

options { caseInsensitive = true; }

// ── Parser ──────────────────────────────────────────────────────────────────

script
    : SEMI* (statement (SEMI+ statement)* SEMI*)? EOF
    ;

statement
    : createTable
    | dropTable
    | createIndex
    | dropIndex
    | insertStmt
    | updateStmt
    | deleteStmt
    | selectStmt
    ;

createTable : CREATE TABLE ident LPAREN columnDef (COMMA columnDef)* RPAREN ;
columnDef   : ident dataType columnConstraint* ;
columnConstraint
    : PRIMARY KEY      #pkConstraint
    | NOT NULL         #notNullConstraint
    ;
dataType
    : INT
    | FLOAT
    | VARCHAR LPAREN INTLIT RPAREN
    | BOOLEAN
    | DATE
    ;

dropTable   : DROP TABLE ident ;
createIndex : CREATE INDEX ident ON ident LPAREN ident RPAREN ;
dropIndex   : DROP INDEX ident ;

insertStmt  : INSERT INTO ident (LPAREN ident (COMMA ident)* RPAREN)? VALUES valueRow (COMMA valueRow)* ;
valueRow    : LPAREN expr (COMMA expr)* RPAREN ;

updateStmt  : UPDATE ident SET assignment (COMMA assignment)* (WHERE expr)? ;
assignment  : ident EQ expr ;

deleteStmt  : DELETE FROM ident (WHERE expr)? ;

selectStmt
    : SELECT selectItem (COMMA selectItem)*
      FROM tableRef joinClause*
      (WHERE expr)?
      (GROUP BY expr (COMMA expr)*)?
      (HAVING expr)?
      (ORDER BY orderItem (COMMA orderItem)*)?
    ;

selectItem
    : STAR                    #starItem
    | ident DOT STAR          #tableStarItem
    | expr (AS? ident)?       #exprItem
    ;

tableRef    : ident (AS? ident)? ;
joinClause  : INNER? JOIN tableRef ON expr ;
orderItem   : expr (ASC | DESC)? ;

expr
    : LPAREN expr RPAREN                                          #parenExpr
    | MINUS expr                                                  #negExpr
    | expr op=(STAR | SLASH) expr                                 #mulExpr
    | expr op=(PLUS | MINUS) expr                                 #addExpr
    | expr op=(EQ | NEQ | LT | LE | GT | GE) expr                 #cmpExpr
    | expr IS NOT? NULL                                           #isNullExpr
    | expr NOT? IN LPAREN selectStmt RPAREN                       #inSubqueryExpr
    | expr NOT? IN LPAREN expr (COMMA expr)* RPAREN               #inListExpr
    | NOT expr                                                    #notExpr
    | expr AND expr                                               #andExpr
    | expr OR expr                                                #orExpr
    | EXISTS LPAREN selectStmt RPAREN                             #existsExpr
    | LPAREN selectStmt RPAREN                                    #scalarSubqueryExpr
    | fn=(COUNT | SUM | AVG | MIN | MAX) LPAREN (STAR | expr) RPAREN  #aggregateExpr
    | literal                                                     #literalExpr
    | (ident DOT)? ident                                          #columnExpr
    ;

literal
    : INTLIT
    | FLOATLIT
    | STRING
    | TRUE
    | FALSE
    | NULL
    ;

ident : ID ;

// ── Lexer ───────────────────────────────────────────────────────────────────

CREATE  : 'create' ;
DROP    : 'drop' ;
TABLE   : 'table' ;
INDEX   : 'index' ;
ON      : 'on' ;
INSERT  : 'insert' ;
INTO    : 'into' ;
VALUES  : 'values' ;
UPDATE  : 'update' ;
SET     : 'set' ;
DELETE  : 'delete' ;
SELECT  : 'select' ;
FROM    : 'from' ;
WHERE   : 'where' ;
AND     : 'and' ;
OR      : 'or' ;
NOT     : 'not' ;
ORDER   : 'order' ;
GROUP   : 'group' ;
BY      : 'by' ;
HAVING  : 'having' ;
ASC     : 'asc' ;
DESC    : 'desc' ;
INNER   : 'inner' ;
JOIN    : 'join' ;
AS      : 'as' ;
IN      : 'in' ;
EXISTS  : 'exists' ;
IS      : 'is' ;
NULL    : 'null' ;
PRIMARY : 'primary' ;
KEY     : 'key' ;
COUNT   : 'count' ;
SUM     : 'sum' ;
AVG     : 'avg' ;
MIN     : 'min' ;
MAX     : 'max' ;
TRUE    : 'true' ;
FALSE   : 'false' ;
INT     : 'int' ;
FLOAT   : 'float' ;
VARCHAR : 'varchar' ;
BOOLEAN : 'boolean' ;
DATE    : 'date' ;

EQ      : '=' ;
NEQ     : '<>' | '!=' ;
LE      : '<=' ;
GE      : '>=' ;
LT      : '<' ;
GT      : '>' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;
SEMI    : ';' ;
DOT     : '.' ;

FLOATLIT : [0-9]+ '.' [0-9]+ ;
INTLIT   : [0-9]+ ;
STRING   : '\'' ( ~'\'' | '\'\'' )* '\'' ;
ID       : [a-z_] [a-z_0-9]* ;

LINE_COMMENT  : '--' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
WS            : [ \t\r\n]+ -> skip ;
