grammar SqlAnalysis;

@header {
package com.acme.sql.antlr4;
}

sqlDML
    : insert EOF
    | update EOF
    | delete EOF
    ;

INSERT: [Ii][Nn][Ss][Ee][Rr][Tt];
INTO: [Ii][Nn][Tt][Oo];
VALUES: [Vv][Aa][Ll][Uu][Ee][Ss];
UPDATE : [Uu][Pp][Dd][Aa][Tt][Ee];
SET: [Ss][Ee][Tt];
WHERE : [Ww][Hh][Ee][Rr][Ee];
DELETE: [Dd][Ee][Ll][Ee][Tt][Ee];
FROM : [Ff][Rr][Oo][Mm];
AND: [Aa][Nn][Dd];
OR: [Oo][Rr];
NOT:[Nn][Oo][Tt];
IS: [Ii][Ss];
IN: [Ii][Nn];
LIKE: [Ll][Ii][Kk][Ee];
BETWEEN: [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
ESCAPE: [Ee][Ss][Cc][Aa][Pp][Ee];

PLUS:                '+';
MINUS:               '-';
LR_BRACKET:          '(';
RR_BRACKET:          ')';
EQ:                  '=';
COMMA:               ',';
SEMI:                ';';
BACKTICK:            '`';
comparison_operator
    : '<' | '=' | '>' | '<=' | '>=' | '<>'
    ;
WS: [ \t\r\n] -> skip;
ID_LITERAL: [a-zA-Z_$0-9\u0080-\uFFFF]*?[a-zA-Z_$\u0080-\uFFFF]+?[a-zA-Z_$0-9\u0080-\uFFFF]*;



schema_name: any_word;
correlation_name: any_word;
table_name: (schema_name DOT)? any_word
    | BACKTICK table_name BACKTICK;
column_name
    : ( ( table_name | correlation_name) DOT )? any_word
    | BACKTICK column_name BACKTICK
    ;
simple_column_name_list:
        simple_column_name (COMMA simple_column_name )*;
simple_column_name: any_word | BACKTICK any_word BACKTICK;
any_word: ID_LITERAL;
values_expression: value (COMMA value )*;
value: expression;
expression
    :primitive_expression
    |expression comparison_operator expression;
primitive_expression
    :literal
    | any_word
    |
    ;
where_clause: WHERE boolean_expression;
boolean_expression
    : expression AND expression
    | expression OR expression
    | NOT expression
    | expression comparison_operator expression
    | expression IS NOT? NULL_
    | character_expression NOT? LIKE character_expression ( ESCAPE string )?
    | expression NOT? BETWEEN expression AND expression
    | expression NOT? IN '(' expression (COMMA expression )* ')'
    ;
character_expression
    : string
    ;
literal
    : string // string, date, time, timestamp
    | sign? DECIMAL_LITERAL
    | sign? (REAL_LITERAL | FLOAT_LITERAL)
    | TRUE | FALSE
    | NULL_
    ;
sign
    : PLUS
    | MINUS
    ;
string
    : STRING_LITERAL
    ;

STRING_LITERAL: DQUOTA_STRING | SQUOTA_STRING | BQUOTA_STRING;

DECIMAL_LITERAL:             DEC_DIGIT+;
FLOAT_LITERAL:               DEC_DOT_DEC;
REAL_LITERAL:                (DECIMAL_LITERAL | DEC_DOT_DEC) ('E' [+-]? DEC_DIGIT+);
NULL_: 'NULL';
TRUE:  'TRUE' | 'true';
FALSE: 'FALSE' | 'false';

fragment DQUOTA_STRING:              '"' ( '\\'. | '""' | ~('"'| '\\') )* '"';
fragment SQUOTA_STRING:              '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';
fragment BQUOTA_STRING:              '`' ( '\\'. | '``' | ~('`'|'\\'))* '`';
fragment DEC_DIGIT:    [0-9];
fragment DEC_DOT_DEC:  (DEC_DIGIT+ '.' DEC_DIGIT+ |  DEC_DIGIT+ '.' | '.' DEC_DIGIT+);


insert: INSERT INTO
            table_name LR_BRACKET simple_column_name_list RR_BRACKET
            VALUES LR_BRACKET values_expression RR_BRACKET SEMI?;
update: UPDATE table_name SET column_name EQ value ( COMMA column_name EQ value )* where_clause? SEMI?;
delete: DELETE FROM table_name where_clause? SEMI?;