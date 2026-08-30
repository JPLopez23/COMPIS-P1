// ============================================================
// Calculator.g4 — gramática ANTLR de ejemplo (NO es Compiscript).
// Sirve para demostrar el visor "Ver gramática -> Abrir otra
// gramática..." del IDE: se puede abrir cualquier .g4 del disco
// en modo solo-lectura, sin recompilar el analizador.
// ============================================================
grammar Calculator;

prog:   stat+ ;

stat:   expr NEWLINE                # printExpr
    |   ID '=' expr NEWLINE         # assign
    |   NEWLINE                     # blank
    ;

expr:   expr op=('*'|'/') expr      # MulDiv
    |   expr op=('+'|'-') expr      # AddSub
    |   INT                         # int
    |   ID                          # id
    |   '(' expr ')'               # parens
    ;

MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
ID  : [a-zA-Z]+ ;
INT : [0-9]+ ;
NEWLINE : '\r'? '\n' ;
WS  : [ \t]+ -> skip ;
