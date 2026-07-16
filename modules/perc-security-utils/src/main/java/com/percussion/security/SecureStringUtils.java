/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.security;

import com.github.javafaker.Faker;
import com.ibm.icu.text.Normalizer2;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.owasp.encoder.Encode;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.owasp.esapi.reference.DefaultEncoder;

/**
 * A centralized utility class with static methods for performing a variety of secure string
 * validation and prevention of common vulnerabilities.
 */
public class SecureStringUtils {

  /**
   * Characters that are invalid for the file name in Windows, which is more restrictive than UNIX.
   *
   * <p>The invalid characters for the file name in Windows are: backslash, forward slash, pipe,
   * less than, greater than, question mark, double quote, colon, asterisk
   */
  public static final String INVALID_WINDOWS_FILE_CHARACTERS = "\\/|<>?\":*";

  /**
   * Characters that should not be used as part of URL; otherwise it may cause error in REST layer
   * when the item name contain any of the characters.
   *
   * <p>'#' - used by anchors in HTML<br>
   * ';' - used to append "jsessionid=..." to URL<br>
   * '%' - used to URL encode/escape other characters.
   */
  public static final String UNSAFE_URL_CHARACTERS = "#;%[]<>{}|\\^~`/?:@=&";

  /**
   * Characters that are invalid for item names (sys_title). It is the combination of "invalid
   * characters for the file name in Windows" and "unsafe URL characters".
   */
  public static final String INVALID_ITEM_NAME_CHARACTERS =
      INVALID_WINDOWS_FILE_CHARACTERS + UNSAFE_URL_CHARACTERS;

  private static final Logger log = LogManager.getLogger("Security");

  private SecureStringUtils() {
    // Provate constructor to prevent direct instantiation.
  }

  /** Array of string patterns that are always rejected to prevent xss */
  protected static final String[] INVALID_XSS_CHARS = {
    ">", "<", "0x003C", "0x003E", "%3E", "%3C", "&#62;", "&#60;", "&lt;", "&gt;"
  };

  /**
   * Method checks against banned characters for xss and returns true if the string contains any
   * such characters, encoded or otherwise.
   *
   * <p>Caller should reject any input that matches. NOTE: This method is intended for situations
   * where simply encoding the string to escape XSS chars is not wanted for other uses check one of
   * the encoding / sanitize methods on this class.
   *
   * <p>Example use case are CMS file names. We never want to allow XSS characters, encoded or not
   * in those strings.
   *
   * @param str The string to check.
   * @return true if the string contains banned xss chars.
   */
  public static boolean containsXSSChars(String str) {
    boolean ret = false;

    for (String s : INVALID_XSS_CHARS) {
      if (str.contains(s)) {
        ret = true;
        break; // no need to continue
      }
    }
    return ret;
  }

  /**
   * Gets typical allowed hosts from request.
   *
   * @param request the HTTP request
   * @return list of allowed hosts
   */
  public static List<String> getTypicalAllowedHosts(HttpServletRequest request) {
    List<String> ret = new ArrayList<>();
    ret.add(request.getLocalName());
    ret.add(request.getLocalAddr());
    ret.add(request.getServerName());
    return ret;
  }

  /**
   * Will validate that the URI provided belongs to one of the allowed hosts. Will automatically
   * allow localhost, localname and servername from the passed in request. Any other hostnames
   * should be passed in with the allowed hosts param
   *
   * @param request the request that provided the url.
   * @param url A url to to validate
   * @param allowedHosts Never null. A list of allowed hostnames.
   * @return true if the host matches, false otherwise
   */
  public static boolean hostMatchesRequest(
      HttpServletRequest request, URI url, List<String> allowedHosts) {

    allowedHosts.addAll(getTypicalAllowedHosts(request));

    return allowedHosts.contains(url.getHost());
  }

  /**
   * To be used when sending queries to LDAP.
   *
   * @param query An LDAP query
   * @param encodeWildcards true to encode wildcards, false to leave them
   * @return A string encoded for LDAP, wild cards are not encoded.
   */
  public static String sanitizeStringForLDAP(String query, boolean encodeWildcards) {
    return DefaultEncoder.getInstance().encodeForLDAP(query, encodeWildcards);
  }

  /**
   * Use this method to encode a string provided externally for JavaScript / JSON.
   *
   * @param s The string to encode
   * @return Returns the supplied string encoded for a JSON
   */
  public static String sanitizeForJson(String s) {
    return DefaultEncoder.getInstance().encodeForJavaScript(s);
  }

  /** Maximum allowed filename length. */
  public static final int MAX_FILENAME_LEN = 255;

  /** Pattern for valid filename characters. */
  public static final Pattern filenamePattern =
      Pattern.compile("[^\\w.\\w]", Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * Remove / replace any invalid characters.
   *
   * @param s a user provided filename to be sanitized
   * @return the sanitized filename
   * @throws IllegalArgumentException if the filename is too long
   */
  public static String sanitizeFileName(@Nonnull String s) {
    String fileName = s.trim();
    if (fileName.length() > MAX_FILENAME_LEN) fileName = s.substring(0, MAX_FILENAME_LEN - 1);

    fileName = filenamePattern.matcher(fileName).replaceAll("-");
    while (fileName.contains("--")) {
      fileName = fileName.replace("--", "-");
    }
    return fileName;
  }

  /** Enum representing supported database types. */
  public enum DatabaseType {
    /** MySQL database */
    MYSQL,
    /** Oracle database */
    ORACLE,
    /** IBM DB2 database */
    DB2,
    /** Microsoft SQL Server database */
    MSSQL,
    /** Apache Derby database */
    DERBY
  }

  /**
   * Checks if the supplied date is a valid date.
   *
   * @param dt the date
   * @return true if the date is valid, false if it is not
   */
  public static boolean isValidDate(String dt) {
    try {
      LocalDate.parse(dt);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  /**
   * Checks if the supplied time is a valie time.
   *
   * @param t the time
   * @return true if the time is valid, false if not
   */
  public static boolean isValidTime(String t) {
    try {
      LocalTime.parse(t);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  /**
   * Checks if the supplied time is a valie time.
   *
   * @param id the id test
   * @return true if the id is valid, false if not
   */
  public static boolean isValidPercId(String id) {
    if (id == null || id.trim().equals("")) {
      return true;
    }
    return id.matches("^[0-9-]*$");
  }

  /**
   * Checks if the supplied string has any known xss characters.
   *
   * @param string the stringto test
   * @return true if the string is valid, false if not
   */
  public static boolean isValidString(String string) {
    if (string == null || string.trim().equals("")) {
      return true;
    }
    return !containsXSSChars(string);
  }

  /**
   * Checks if the supplied time is a valie time.
   *
   * @param url the url to test
   * @return true if the url is valid, false if not
   */
  public static boolean isValidDBUrl(String url) {
    if (url == null || url.trim().equals("")) {
      return true;
    }
    return url.matches("[a-zA-Z0-9,.;/=?@&:_'\\\\'\\s-]*");
  }

  /**
   * Will return an instance of secure random. Will attempt to return a StrongSecureRandom first but
   * will return a standard SecureRandom if Strong is unavailable. May return null if secure random
   * cannot be initialized.
   *
   * @return true if the host matches, false otherwise
   */
  public static SecureRandom getSecureRandom() {
    return new SecureRandom();
  }

  /** Static list of reserved SQL words to be used for validating table and column names. */
  private static final List<String> SQLKEYWORDS = new ArrayList<>();

  static {
    SQLKEYWORDS.addAll(
        Arrays.asList(
            "ADD",
            "EXTERNAL",
            "PROCEDURE",
            "ALL",
            "FETCH",
            "PUBLIC",
            "ALTER",
            "FILE",
            "RAISERROR",
            "AND",
            "FILLFACTOR",
            "READ",
            "ANY",
            "FOR",
            "READTEXT",
            "AS",
            "FOREIGN",
            "RECONFIGURE",
            "ASC",
            "FREETEXT",
            "REFERENCES",
            "AUTHORIZATION",
            "FREETEXTTABLE",
            "REPLICATION",
            "BACKUP",
            "FROM",
            "RESTORE",
            "BEGIN",
            "FULL",
            "RESTRICT",
            "BETWEEN",
            "FUNCTION",
            "RETURN",
            "BREAK",
            "GOTO",
            "REVERT",
            "BROWSE",
            "GRANT",
            "REVOKE",
            "BULK",
            "GROUP",
            "RIGHT",
            "BY",
            "HAVING",
            "ROLLBACK",
            "CASCADE",
            "HOLDLOCK",
            "ROWCOUNT",
            "CASE",
            "IDENTITY",
            "ROWGUIDCOL",
            "CHECK",
            "IDENTITY_INSERT",
            "RULE",
            "CHECKPOINT",
            "IDENTITYCOL",
            "SAVE",
            "CLOSE",
            "IF",
            "SCHEMA",
            "CLUSTERED",
            "IN",
            "SECURITYAUDIT",
            "COALESCE",
            "INDEX",
            "SELECT",
            "COLLATE",
            "INNER",
            "SEMANTICKEYPHRASETABLE",
            "COLUMN",
            "INSERT",
            "SEMANTICSIMILARITYDETAILSTABLE",
            "COMMIT",
            "INTERSECT",
            "SEMANTICSIMILARITYTABLE",
            "COMPUTE",
            "INTO",
            "SESSION_USER",
            "CONSTRAINT",
            "IS",
            "SET",
            "CONTAINS",
            "JOIN",
            "SETUSER",
            "CONTAINSTABLE",
            "KEY",
            "SHUTDOWN",
            "CONTINUE",
            "KILL",
            "SOME",
            "CONVERT",
            "LEFT",
            "STATISTICS",
            "CREATE",
            "LIKE",
            "SYSTEM_USER",
            "CROSS",
            "LINENO",
            "TABLE",
            "CURRENT",
            "LOAD",
            "TABLESAMPLE",
            "CURRENT_DATE",
            "MERGE",
            "TEXTSIZE",
            "CURRENT_TIME",
            "NATIONAL",
            "THEN",
            "CURRENT_TIMESTAMP",
            "NOCHECK",
            "TO",
            "CURRENT_USER",
            "NONCLUSTERED",
            "TOP",
            "CURSOR",
            "NOT",
            "TRAN",
            "DATABASE",
            "NULL",
            "TRANSACTION",
            "DBCC",
            "NULLIF",
            "TRIGGER",
            "DEALLOCATE",
            "OF",
            "TRUNCATE",
            "DECLARE",
            "OFF",
            "TRY_CONVERT",
            "DEFAULT",
            "OFFSETS",
            "TSEQUAL",
            "DELETE",
            "ON",
            "UNION",
            "DENY",
            "OPEN",
            "UNIQUE",
            "DESC",
            "OPENDATASOURCE",
            "UNPIVOT",
            "DISK",
            "OPENQUERY",
            "UPDATE",
            "DISTINCT",
            "OPENROWSET",
            "UPDATETEXT",
            "DISTRIBUTED",
            "OPENXML",
            "USE",
            "DOUBLE",
            "OPTION",
            "USER",
            "DROP",
            "OR",
            "VALUES",
            "DUMP",
            "ORDER",
            "VARYING",
            "ELSE",
            "OUTER",
            "VIEW",
            "END",
            "OVER",
            "WAITFOR",
            "ERRLVL",
            "PERCENT",
            "WHEN",
            "ESCAPE",
            "PIVOT",
            "WHERE",
            "EXCEPT",
            "PLAN",
            "WHILE",
            "EXEC",
            "PRECISION",
            "WITH",
            "EXECUTE",
            "PRIMARY",
            "WITHIN GROUP",
            "EXISTS",
            "PRINT",
            "WRITETEXT",
            "EXIT",
            "PROC",
            "ABSOLUTE",
            "OVERLAPS",
            "ACTION",
            "PAD",
            "ADA",
            "PARTIAL",
            "PASCAL",
            "EXTRACT",
            "POSITION",
            "ALLOCATE",
            "FALSE",
            "PREPARE",
            "FIRST",
            "PRESERVE",
            "FLOAT",
            "ARE",
            "PRIOR",
            "PRIVILEGES",
            "FORTRAN",
            "ASSERTION",
            "FOUND",
            "AT",
            "REAL",
            "AVG",
            "GLOBAL",
            "RELATIVE",
            "GO",
            "BIT",
            "BIT_LENGTH",
            "BOTH",
            "ROWS",
            "HOUR",
            "CASCADED",
            "SCROLL",
            "IMMEDIATE",
            "SECOND",
            "CAST",
            "SECTION",
            "CATALOG",
            "INCLUDE",
            "CHAR",
            "SESSION",
            "CHAR_LENGTH",
            "INDICATOR",
            "CHARACTER",
            "INITIALLY",
            "CHARACTER_LENGTH",
            "SIZE",
            "INPUT",
            "SMALLINT",
            "INSENSITIVE",
            "SPACE",
            "INT",
            "SQL",
            "COLLATION",
            "INTEGER",
            "SQLCA",
            "SQLCODE",
            "INTERVAL",
            "SQLERROR",
            "CONNECT",
            "SQLSTATE",
            "CONNECTION",
            "SQLWARNING",
            "ISOLATION",
            "SUBSTRING",
            "CONSTRAINTS",
            "SUM",
            "LANGUAGE",
            "CORRESPONDING",
            "LAST",
            "TEMPORARY",
            "COUNT",
            "LEADING",
            "TIME",
            "LEVEL",
            "TIMESTAMP",
            "TIMEZONE_HOUR",
            "LOCAL",
            "TIMEZONE_MINUTE",
            "LOWER",
            "MATCH",
            "TRAILING",
            "MAX",
            "MIN",
            "TRANSLATE",
            "DATE",
            "MINUTE",
            "TRANSLATION",
            "DAY",
            "MODULE",
            "TRIM",
            "MONTH",
            "TRUE",
            "DEC",
            "NAMES",
            "DECIMAL",
            "NATURAL",
            "UNKNOWN",
            "NCHAR",
            "DEFERRABLE",
            "NEXT",
            "UPPER",
            "DEFERRED",
            "NO",
            "USAGE",
            "NONE",
            "USING",
            "DESCRIBE",
            "VALUE",
            "DESCRIPTOR",
            "DIAGNOSTICS",
            "NUMERIC",
            "VARCHAR",
            "DISCONNECT",
            "OCTET_LENGTH",
            "DOMAIN",
            "ONLY",
            "WHENEVER",
            "WORK",
            "END-EXEC",
            "WRITE",
            "YEAR",
            "OUTPUT",
            "ZONE",
            "EXCEPTION",
            "HOST",
            "RELEASE",
            "ADMIN",
            "IGNORE",
            "RESULT",
            "AFTER",
            "RETURNS",
            "AGGREGATE",
            "ROLE",
            "ALIAS",
            "INITIALIZE",
            "ROLLUP",
            "ROUTINE",
            "INOUT",
            "ROW",
            "ARRAY",
            "ASENSITIVE",
            "SAVEPOINT",
            "ASYMMETRIC",
            "INTERSECTION",
            "SCOPE",
            "SEARCH",
            "ATOMIC",
            "BEFORE",
            "ITERATE",
            "BINARY",
            "SENSITIVE",
            "LARGE",
            "SEQUENCE",
            "BLOB",
            "BOOLEAN",
            "LATERAL",
            "SETS",
            "SIMILAR",
            "BREADTH",
            "LESS",
            "CALL",
            "CALLED",
            "LIKE_REGEX",
            "CARDINALITY",
            "LIMIT",
            "SPECIFIC",
            "LN",
            "SPECIFICTYPE",
            "LOCALTIME",
            "SQLEXCEPTION",
            "LOCALTIMESTAMP",
            "LOCATOR",
            "CLASS",
            "MAP",
            "START",
            "CLOB",
            "STATE",
            "MEMBER",
            "STATEMENT",
            "COLLECT",
            "METHOD",
            "STATIC",
            "COMPLETION",
            "STDDEV_POP",
            "CONDITION",
            "MOD",
            "STDDEV_SAMP",
            "MODIFIES",
            "STRUCTURE",
            "MODIFY",
            "SUBMULTISET",
            "SUBSTRING_REGEX",
            "CONSTRUCTOR",
            "SYMMETRIC",
            "CORR",
            "MULTISET",
            "SYSTEM",
            "COVAR_POP",
            "TERMINATE",
            "COVAR_SAMP",
            "THAN",
            "CUBE",
            "NCLOB",
            "CUME_DIST",
            "NEW",
            "CURRENT_CATALOG",
            "CURRENT_DEFAULT_TRANSFORM_GROUP",
            "CURRENT_PATH",
            "CURRENT_ROLE",
            "NORMALIZE",
            "TRANSLATE_REGEX",
            "CURRENT_SCHEMA",
            "CURRENT_TRANSFORM_GROUP_FOR_TYPE",
            "OBJECT",
            "TREAT",
            "CYCLE",
            "OCCURRENCES_REGEX",
            "DATA",
            "OLD",
            "UESCAPE",
            "UNDER",
            "OPERATION",
            "ORDINALITY",
            "UNNEST",
            "OUT",
            "OVERLAY",
            "DEPTH",
            "VAR_POP",
            "DEREF",
            "PARAMETER",
            "VAR_SAMP",
            "PARAMETERS",
            "VARIABLE",
            "DESTROY",
            "PARTITION",
            "DESTRUCTOR",
            "PATH",
            "WIDTH_BUCKET",
            "DETERMINISTIC",
            "POSTFIX",
            "WITHOUT",
            "DICTIONARY",
            "PREFIX",
            "WINDOW",
            "PREORDER",
            "WITHIN",
            "PERCENT_RANK",
            "DYNAMIC",
            "PERCENTILE_CONT",
            "XMLAGG",
            "EACH",
            "PERCENTILE_DISC",
            "XMLATTRIBUTES",
            "ELEMENT",
            "POSITION_REGEX",
            "XMLBINARY",
            "XMLCAST",
            "EQUALS",
            "XMLCOMMENT",
            "EVERY",
            "XMLCONCAT",
            "RANGE",
            "XMLDOCUMENT",
            "READS",
            "XMLELEMENT",
            "FILTER",
            "XMLEXISTS",
            "RECURSIVE",
            "XMLFOREST",
            "REF",
            "XMLITERATE",
            "REFERENCING",
            "XMLNAMESPACES",
            "FREE",
            "REGR_AVGX",
            "XMLPARSE",
            "FULLTEXTTABLE",
            "REGR_AVGY",
            "XMLPI",
            "FUSION",
            "REGR_COUNT",
            "XMLQUERY",
            "GENERAL",
            "REGR_INTERCEPT",
            "XMLSERIALIZE",
            "GET",
            "REGR_R2",
            "XMLTABLE",
            "REGR_SLOPE",
            "XMLTEXT",
            "REGR_SXX",
            "XMLVALIDATE",
            "GROUPING",
            "REGR_SXY",
            "HOLD",
            "REGR_SYY",
            "COLUMN_NAME",
            "COLUMNS",
            "COMMAND_FUNCTION",
            "COMMAND_FUNCTION_CODE",
            "COMMENT",
            "COMMITTED",
            "COMPRESS",
            "CONDITION_NUMBER",
            "CONNECTION_NAME",
            "CONSTRAINT_CATALOG",
            "CONSTRAINT_NAME",
            "CONSTRAINT_SCHEMA",
            "CONVERSION",
            "COPY",
            "CREATEDB",
            "CREATEROLE",
            "CREATEUSER",
            "CSV",
            "CURSOR_NAME",
            "DATABASES",
            "DATETIME",
            "DATETIME_INTERVAL_CODE",
            "DATETIME_INTERVAL_PRECISION",
            "DAY_HOUR",
            "DAY_MICROSECOND",
            "DAY_MINUTE",
            "DAY_SECOND",
            "DAYOFMONTH",
            "DAYOFWEEK",
            "DAYOFYEAR",
            "DEFAULTS",
            "DEFINED",
            "DEFINER",
            "DEGREE",
            "DELAY_KEY_WRITE",
            "DELAYED",
            "DELIMITER",
            "DELIMITERS",
            "DENSE_RANK",
            "DERIVED",
            "DISABLE",
            "DISPATCH",
            "DISTINCTROW",
            "DIV",
            "DO",
            "DUAL",
            "DUMMY",
            "DYNAMIC_FUNCTION",
            "DYNAMIC_FUNCTION_CODE",
            "ELSEIF",
            "ENABLE",
            "ENCLOSED",
            "ENCODING",
            "ENCRYPTED",
            "ENUM",
            "ESCAPED",
            "EXCLUDE",
            "EXCLUDING",
            "EXCLUSIVE",
            "EXISTING",
            "EXP",
            "EXPLAIN",
            "FIELDS",
            "FINAL",
            "FLOAT4",
            "FLOAT8",
            "FLOOR",
            "FLUSH",
            "FOLLOWING",
            "FORCE",
            "FORWARD",
            "FREEZE",
            "FULLTEXT",
            "G",
            "GENERATED",
            "GRANTED",
            "GRANTS",
            "GREATEST",
            "HANDLER",
            "HEADER",
            "HEAP",
            "HIERARCHY",
            "HIGH_PRIORITY",
            "HOSTS",
            "HOUR_MICROSECOND",
            "HOUR_MINUTE",
            "HOUR_SECOND",
            "IDENTIFIED",
            "ILIKE",
            "IMMUTABLE",
            "IMPLEMENTATION",
            "IMPLICIT",
            "INCLUDING",
            "INCREMENT",
            "INFILE",
            "INFIX",
            "INHERIT",
            "INHERITS",
            "INITIAL",
            "INSERT_ID",
            "INSTANCE",
            "INSTANTIABLE",
            "INSTEAD",
            "INT1",
            "INT2",
            "INT3",
            "INT4",
            "INT8",
            "INVOKER",
            "ISAM",
            "ISNULL",
            "K",
            "KEY_MEMBER",
            "KEY_TYPE",
            "KEYS",
            "LANCOMPILER",
            "LAST_INSERT_ID",
            "LEAST",
            "LEAVE",
            "LENGTH",
            "LINES",
            "LISTEN",
            "LOCATION",
            "LOCK",
            "LOGIN",
            "LOGS",
            "LONG",
            "LONGBLOB",
            "LONGTEXT",
            "LOOP",
            "LOW_PRIORITY",
            "M",
            "MATCHED",
            "MAX_ROWS",
            "MAXEXTENTS",
            "MAXVALUE",
            "MEDIUMBLOB",
            "MEDIUMINT",
            "MEDIUMTEXT",
            "MESSAGE_LENGTH",
            "MESSAGE_OCTET_LENGTH",
            "MESSAGE_TEXT",
            "MIDDLEINT",
            "MIN_ROWS",
            "MINUS",
            "MINUTE_MICROSECOND",
            "MINUTE_SECOND",
            "MINVALUE",
            "MLSLABEL",
            "MODE",
            "MONTHNAME",
            "MORE",
            "MOVE",
            "MUMPS",
            "MYISAM",
            "NAME",
            "NESTING",
            "NO_WRITE_TO_BINLOG",
            "NOAUDIT",
            "NOCOMPRESS",
            "NOCREATEDB",
            "NOCREATEROLE",
            "NOCREATEUSER",
            "NOINHERIT",
            "NOLOGIN",
            "NORMALIZED",
            "NOSUPERUSER",
            "NOTHING",
            "NOTIFY",
            "NOTNULL",
            "NOWAIT",
            "NULLABLE",
            "NULLS",
            "NUMBER",
            "OCTETS",
            "OFFLINE",
            "OFFSET",
            "OIDS",
            "ONLINE",
            "OPERATOR",
            "OPTIMIZE",
            "OPTIONALLY",
            "OPTIONS",
            "ORDERING",
            "OTHERS",
            "OUTFILE",
            "OVERRIDING",
            "OWNER",
            "PACK_KEYS",
            "PARAMETER_MODE",
            "PARAMETER_NAME",
            "PARAMETER_ORDINAL_POSITION",
            "PARAMETER_SPECIFIC_CATALOG",
            "PARAMETER_SPECIFIC_NAME",
            "PARAMETER_SPECIFIC_SCHEMA",
            "PASSWORD",
            "PCTFREE",
            "PLACING",
            "PLI",
            "POWER",
            "PRECEDING",
            "PREPARED",
            "PROCEDURAL",
            "PROCESS",
            "PROCESSLIST",
            "PURGE",
            "QUOTE",
            "RAID0",
            "RANK",
            "RAW",
            "RECHECK",
            "REGEXP",
            "REINDEX",
            "RELOAD",
            "RENAME",
            "REPEAT",
            "REPEATABLE",
            "REPLACE",
            "REQUIRE",
            "RESET",
            "RESIGNAL",
            "RESOURCE",
            "RESTART",
            "RETURNED_CARDINALITY",
            "RETURNED_LENGTH",
            "RETURNED_OCTET_LENGTH",
            "RETURNED_SQLSTATE",
            "RLIKE",
            "ROUTINE_CATALOG",
            "ROUTINE_NAME",
            "ROUTINE_SCHEMA",
            "ROW_COUNT",
            "ROW_NUMBER",
            "ROWID",
            "ROWNUM",
            "SCALE",
            "SCHEMA_NAME",
            "SCHEMAS",
            "SCOPE_CATALOG",
            "SCOPE_NAME",
            "SCOPE_SCHEMA",
            "SECOND_MICROSECOND",
            "SECURITY",
            "SELF",
            "SEPARATOR",
            "SERIALIZABLE",
            "SERVER_NAME",
            "SETOF",
            "SHARE",
            "SHOW",
            "SIGNAL",
            "SIMPLE",
            "SONAME",
            "SOURCE",
            "SPATIAL",
            "SPECIFIC_NAME",
            "SQL_BIG_RESULT",
            "SQL_BIG_SELECTS",
            "SQL_BIG_TABLES",
            "SQL_CALC_FOUND_ROWS",
            "SQL_LOG_OFF",
            "SQL_LOG_UPDATE",
            "SQL_LOW_PRIORITY_UPDATES",
            "SQL_SELECT_LIMIT",
            "SQL_SMALL_RESULT",
            "SQL_WARNINGS",
            "SQRT",
            "SSL",
            "STABLE",
            "STARTING",
            "STATUS",
            "STDIN",
            "STDOUT",
            "STORAGE",
            "STRAIGHT_JOIN",
            "STRICT",
            "STRING",
            "STYLE",
            "SUBCLASS_ORIGIN",
            "SUBLIST",
            "SUCCESSFUL",
            "SUPERUSER",
            "SYNONYM",
            "SYSDATE",
            "SYSID",
            "TABLE_NAME",
            "TABLES",
            "TABLESPACE",
            "TEMP",
            "TEMPLATE",
            "TERMINATED",
            "TEXT",
            "TIES",
            "TINYBLOB",
            "TINYINT",
            "TINYTEXT",
            "TOAST",
            "TOP_LEVEL_COUNT",
            "TRANSACTION_ACTIVE",
            "TRANSACTIONS_COMMITTED",
            "TRANSACTIONS_ROLLED_BACK",
            "TRANSFORM",
            "TRANSFORMS",
            "TRIGGER_CATALOG",
            "TRIGGER_NAME",
            "TRIGGER_SCHEMA",
            "TRUSTED",
            "TYPE",
            "UID",
            "UNBOUNDED",
            "UNCOMMITTED",
            "UNDO",
            "UNENCRYPTED",
            "UNLISTEN",
            "UNLOCK",
            "UNNAMED",
            "UNSIGNED",
            "UNTIL",
            "USER_DEFINED_TYPE_CATALOG",
            "USER_DEFINED_TYPE_CODE",
            "USER_DEFINED_TYPE_NAME",
            "USER_DEFINED_TYPE_SCHEMA",
            "UTC_DATE",
            "UTC_TIME",
            "UTC_TIMESTAMP",
            "VACUUM",
            "VALID",
            "VALIDATE",
            "VALIDATOR",
            "VARBINARY",
            "VARCHAR2",
            "VARCHARACTER",
            "VARIABLES",
            "VERBOSE",
            "VOLATILE",
            "X509",
            "XOR",
            "YEAR_MONTH",
            "ZEROFILL",
            "A",
            "ABORT",
            "ABS",
            "ACCESS",
            "ALSO",
            "ALWAYS",
            "ANALYSE",
            "ANALYZE",
            "ASSIGNMENT",
            "ATTRIBUTE",
            "ATTRIBUTES",
            "AUDIT",
            "AUTO_INCREMENT",
            "AVG_ROW_LENGTH",
            "BACKWARD",
            "BERNOULLI",
            "BIGINT",
            "BITVAR",
            "BOOL",
            "C",
            "CACHE",
            "CATALOG_NAME",
            "CEIL",
            "CEILING",
            "CHAIN",
            "CHANGE",
            "CHARACTER_SET_CATALOG",
            "CHARACTER_SET_NAME",
            "CHARACTER_SET_SCHEMA",
            "CHARACTERISTICS",
            "CHARACTERS",
            "CHECKED",
            "CHECKSUM",
            "CLASS_ORIGIN",
            "CLUSTER",
            "COBOL",
            "COLLATION_CATALOG",
            "COLLATION_NAME",
            "COLLATION_SCHEMA"));
  }

  /**
   * Gets SQL reserved keywords.
   *
   * @return array of SQL reserved keywords
   */
  public static String[] getSqlReservedKeywords() {
    return (String[]) Arrays.copyOf(SQLKEYWORDS.toArray(), SQLKEYWORDS.size());
  }

  /**
   * Validates a table or column name.
   *
   * @param name the identifier to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidTableOrColumnName(String name) {
    boolean isValid = StringUtils.isAlphanumeric(name);
    isValid = isValid && StringUtils.isAsciiPrintable(name);
    isValid = isValid && name.length() <= 128;
    isValid = isValid && !StringUtils.isNumeric(StringUtils.left(name, 1));
    isValid = isValid && !isSQLReservedKeyWord(name);

    // can't have single lowercase followed by upper case due to hibernate bug
    isValid =
        isValid
            && !(name.length() > 1
                && Character.isLowerCase(name.charAt(0))
                && Character.isUpperCase(name.charAt(1)));

    return isValid;
  }

  /**
   * Validates against a list of known reserved SQL keywords.
   *
   * @param name the identifier to validate
   * @return true if valid, false otherwise
   */
  public static boolean isSQLReservedKeyWord(String name) {
    return SQLKEYWORDS.contains(name.toUpperCase());
  }

  /**
   * Given the supplied string that is expected to be a url, remove any linefeeds from it, normalize
   * it, and strip any protocol that is not http or https.
   *
   * @param s The cleansed url string or ""
   * @return An http or https url or or an empty string
   */
  public static String stripNonHttpProtocols(String s) {
    String ret = "";
    if (s != null) {
      // normalize the string
      String temp = s;
      temp = normalize(temp, true);
      int ipos = temp.indexOf(":");
      String prot;
      if (ipos > 0) {
        prot = temp.substring(0, ipos);
        if (prot.equals("HTTP") || prot.equals("HTTPS")) {
          ret = stripAllLineBreaks(normalize(s, false));
        }
      } else {
        ret = "";
      }
    }
    return ret;
  }

  /**
   * Normalizes a string.
   *
   * @param s the string to normalize
   * @param toUpper whether to convert to uppercase
   * @return normalized string
   */
  public static String normalize(String s, boolean toUpper) {
    if (toUpper) return Normalizer2.getNFKDInstance().normalize(s).toUpperCase();
    else return Normalizer2.getNFKDInstance().normalize(s);
  }

  /**
   * Utility to remove parameters from header.
   *
   * @param str the string to validate
   * @return true if valid, false otherwise
   */
  public static String stripAllLineBreaks(String str) {
    String ret = str;

    if (ret == null) return "";

    if (isURLEncoded(str)) {
      try {

        // A valid http header will be url encoded, make sure no line feeds are encoded
        ret = URLDecoder.decode(str, StandardCharsets.UTF_8.name());

        ret = URLEncoder.encode(ret, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException | IllegalArgumentException e) {

        ret = ret;
      }
    }

    // Either not encoded or just sanity check the encoded string.
    ret = ret;

    return ret.replaceAll("%0d|%0a|\\R+", "");
  }

  /**
   * Convenience method for sanitizing HTTP request parameters.
   *
   * @param rp A string provided in an HTTP request
   * @return A sanitized string
   */
  public static String srp(String rp) {
    return stripAllLineBreaks(rp);
  }

  /**
   * URL encodes a string.
   *
   * @param s the string to encode
   * @return URL encoded string
   */
  public static String urlEncode(String s) {
    String ret = "";

    try {
      ret = DefaultEncoder.getInstance().encodeForURL(s);
    } catch (EncodingException e) {
      log.error(e.getMessage());
    }

    return ret;
  }

  /**
   * Strips URL parameters from a string.
   *
   * @param s the URL string
   * @return string without parameters
   */
  public static String stripUrlParams(String s) {
    String ret = s;
    if (s.contains("?")) {
      ret = s.substring(0, s.indexOf("?"));
    }
    if (ret.contains("#")) {
      ret = s.substring(0, s.indexOf("#"));
    }
    return ret;
  }

  /**
   * Checks if a string is URL encoded.
   *
   * @param s the string to check
   * @return true if URL encoded
   */
  public static boolean isURLEncoded(String s) {
    boolean ret = false;
    try {
      String s1 = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
      String s2 = URLEncoder.encode(s1, StandardCharsets.UTF_8.name());

      ret = s1.equals(s2);
    } catch (UnsupportedEncodingException e) {
      return false;
    }
    return ret;
  }

  /**
   * Utility to sanitize a string for use in a file system path under a specified path.
   *
   * @param containingPath the path that should contain the string
   * @param str the string to sanitize
   * @return The sanitized string
   */
  public static String sanitizeStringForFileUnderPath(String containingPath, String str) {
    // TODO: Implement me!
    throw new RuntimeException("Not Implemented!");
  }

  /**
   * Utility to sanitize a string for use in a file system path.
   *
   * @param str the string to sanitize
   * @return The sanitized string
   */
  public static String sanitizeStringForFileSystem(String str) {
    // TODO: Implement me!
    throw new RuntimeException("Not Implemented!");
  }

  /**
   * Sanitizes a string for SQL statements.
   *
   * @param str the string to sanitize
   * @return sanitized string
   */
  public static String sanitizeStringForSQLStatement(String str) {
    if (str == null || StringUtils.isEmpty(str)) return "";

    return str.replace("'", "\'");
  }

  /**
   * Utility to sanitize a string for use in a SQL statement.
   *
   * @param str User provided string
   * @param dbType The type of database that should be used for encoding if an ESAPI Codec is
   *     available for the DB.
   * @return The sanitized string, will use {@link #sanitizeStringForFileSystem(String)} if no ESAPI
   *     codec is available.
   */
  public static String sanitizeStringForSQLStatement(String str, DatabaseType dbType) {
    return sanitizeStringForSQLStatement(str);

    /**
     * This is not Consistent across multiple DB Types and thus commented. Codec codec;
     * switch(dbType){ case DERBY: case DB2: codec = new DB2Codec(); break; case MYSQL: codec = new
     * MySQLCodec(MySQLCodec.Mode.STANDARD); break; case ORACLE: codec = new OracleCodec(); break;
     * default: codec = null; } if(codec == null) { return sanitizeStringForSQLStatement(str);
     * }else{ return DefaultEncoder.getInstance().encodeForSQL(codec,str); }*
     */
  }

  /**
   * Takes a path provided as input and makes sure that it is:
   *
   * <p>1. URI Decoded 2. Normalized for /'s 3. Pointed at a resource under the web application root
   * of / (/Rhythmyx/../../../../ would fail)
   *
   * @param resourcePaths the allowed resource paths
   * @param path the path to clean
   * @param remoteIP the remote IP address
   * @return the decoded and checked "safe" path or null if the path is invalid
   */
  public static String cleanWildPath(String[] resourcePaths, String path, String remoteIP) {
    String ret = null;

    if (path == null) return ret;

    try {
      // Url decode path
      ret = ESAPI.encoder().decodeFromURL(path);

      // Normalize backslashes to slashes
      ret = ret.replace("\\", "/");

      // Normalize doubleslashes
      while (ret.contains("//")) {
        ret = ret.replace("//", "/");
      }

      String[] dotdots = ret.split("\\.\\./");
      for (String s : resourcePaths) {
        String[] slashes = {};
        if (ret.startsWith(s)) {
          slashes = s.split("/");

          if (dotdots.length > slashes.length) {
            log.warn(
                "Security filter blocked suspicious path: {} from client ip: {}", ret, remoteIP);
            ret = null;
            break;
          }
        }
      }

      // if we didn't match a resource we still need to check for /../ - there should be no dot dots
      if (ret != null && ret.startsWith("/..")) {
        log.warn("Security filter blocked suspicious path: {} from client ip: {}", ret, remoteIP);
        ret = null;
      }

    } catch (EncodingException e) {
      log.warn(
          "Error decoding wild path {}. Error: {}", path, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return ret;
  }

  /**
   * Sanitizes a user provided string for use in HTML
   *
   * @param str a user provided string
   * @return The sanitized string
   */
  public static String sanitizeStringForHTML(String str) {
    if (str == null) {
      return null;
    }
    return Encode.forHtml(str);
  }

  /**
   * Generates a random password.
   *
   * @return random password string
   */
  public static String generateRandomPassword() {
    Faker f = Faker.instance(getSecureRandom());

    char[] password = f.lorem().characters(6, 20, true, true).toCharArray();
    char[] special = new char[] {'@', '$', '%', '^', '&', '*'};
    for (int i = 0; i < f.random().nextInt(6); i++) {
      password[f.random().nextInt(password.length)] = special[f.random().nextInt(special.length)];
    }
    return new String(password);
  }

  /**
   * Will strip the leading / from arg if present.
   *
   * @param arg a String, can be null
   * @return will return the supplied arg without a leading / if present or just return arg
   */
  public static String stripLeadingSlash(String arg) {
    if (arg != null && arg.startsWith("/")) {
      return arg.substring(1);
    } else {
      return arg;
    }
  }

  /**
   * Checks if child is a child of parent path.
   *
   * @param parent the parent path
   * @param child the child path
   * @return true if child is under parent
   */
  public static boolean isChildOfFilePath(final Path parent, final Path child) {
    final Path absoluteParent = parent.toAbsolutePath().normalize();
    final Path absoluteChild = child.toAbsolutePath().normalize();

    if (absoluteParent.getNameCount() >= absoluteChild.getNameCount()) {
      return false;
    }

    final Path immediateParent = absoluteChild.getParent();
    if (immediateParent == null) {
      return false;
    }

    return isSameFileAs(absoluteParent, immediateParent)
        || isChildOfFilePath(absoluteParent, immediateParent);
  }

  /**
   * Checks if two paths refer to the same file.
   *
   * @param path first path
   * @param path2 second path
   * @return true if same file
   */
  public static boolean isSameFileAs(final Path path, final Path path2) {
    try {
      return Files.isSameFile(path, path2);
    } catch (final IOException ioe) {
      return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
    }
  }

  /**
   * Tests if the supplied string contains HTML.
   *
   * @param src A string. May be null or empty.
   * @return false if HTML is not detected. true if it is.
   */
  public static boolean isHTML(String src) {
    boolean ret = false;
    if (src != null && !StringUtils.isEmpty(src)) {
      if (!src.equals(ESAPI.encoder().encodeForHTML(src))) {
        ret = true;
      }
    }
    return ret;
  }

  /**
   * Tests if the supplied string contains XML.
   *
   * @param src A string. May be null or empty.
   * @return false if XML is not detected. true if it is.
   */
  public static boolean isXML(String src) {
    boolean ret = false;
    if (src != null && !StringUtils.isEmpty(src)) {
      if (!src.equals(ESAPI.encoder().encodeForXML(src))) {
        ret = true;
      }
    }
    return ret;
  }

  /**
   * Utility to remove parameters from header.
   *
   * @param str the string to sanitize
   * @return the sanitized string
   */
  public static String removeSpecialCharactersFromHeader(String str) {
    return str.replaceAll("[^a-zA-Z ]", "");
  }

  /**
   * Validates an un-encoded CMS path for valid characters based on the operation context.
   *
   * @param path the path to validate
   * @param context The operation context. There are some legacy path characters that must be
   *     allowed, like [ or ]
   * @return true if valid, false otherwise
   */
  public static boolean isValidCMSPathString(String path, PSOperationContext context) {
    switch (context) {
      case CREATE:
        {
          return (!containsXSSChars(path) && isValidCMSPathString(path));
        }
      default:
        {
          // Always validate for XSS
          return !containsXSSChars(path);
        }
    }
  }

  /**
   * To support legacy filenames post upgrade, use the method below instead.
   *
   * @see #isValidCMSPathString(String, PSOperationContext)
   * @param path the path to validate
   * @return the normalized path
   */
  public static boolean isValidCMSPathString(String path) {
    // API seems coded such that an empty path is root.
    if (StringUtils.isEmpty(path)) return true;

    if (StringUtils.containsAny(path, "[\"\\<>{}^()|[]")) return false;
    else return true;
  }

  /**
   * Removes any characters from a given string that are not a valid SQL Object Name. Supports
   * unicode strings.
   *
   * @param str the string to sanitize
   * @return A version of the string with any special characters removed.
   */
  public static String removeInvalidSQLObjectNameCharacters(String str) {
    if (str == null) return null;

    return str.replaceAll("[\\W]+", "");
  }

  /**
   * Validates if string is a valid GUID/ID.
   *
   * @param id the ID to validate
   * @return true if valid GUID format
   */
  public static boolean isValidGuidId(String id) {
    return id.matches("^[0-9-]*$");
  }

  /**
   * Validates if string is a valid numeric ID.
   *
   * @param id the ID to validate
   * @return true if valid numeric format
   */
  public static boolean isValidNumericId(String id) {
    return StringUtils.isNumeric(id);
  }

  /**
   * Escapes a string for LDAP queries.
   *
   * @param in the string to escape
   * @return escaped string
   */
  public static String escapeLDAPQueryString(final String in) {
    StringBuilder s = new StringBuilder();

    for (int i = 0; i < in.length(); i++) {

      char c = in.charAt(i);

      if (c == '*') {
        // escape asterisk
        s.append("\\2a");
      } else if (c == '(') {
        // escape left parenthesis
        s.append("\\28");
      } else if (c == ')') {
        // escape right parenthesis
        s.append("\\29");
      } else if (c == '\\') {
        // escape backslash
        s.append("\\5c");
      } else if (c == '\u0000') {
        // escape NULL char
        s.append("\\00");
      } else if (c <= 0x7f) {
        // regular 1-byte UTF-8 char
        s.append(String.valueOf(c));
      } else if (c >= 0x080) {

        // higher-order 2, 3 and 4-byte UTF-8 chars

        byte[] utf8bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);

        for (byte b : utf8bytes) s.append(String.format("\\%02x", b));
      }
    }

    return s.toString();
  }

  /**
   * Escapes a string for LDAP connection strings.
   *
   * @param str the string to escape
   * @return escaped string
   */
  public static String escapeLDAPConnectionString(String str) {
    return str;
  }

  /**
   * Escapes special regex metacharacters in a string so it can be safely used as a literal string
   * in regex patterns.
   *
   * <p>This method prevents Regex Injection attacks by escaping all regex special characters:
   * <code>. ^ $ | ? * + ( ) [ ] { } \</code>
   *
   * <p><b>CWE-94: Improper Control of Generation of Code ('Code Injection')</b> <br>
   * <b>OWASP A03:2021 – Injection</b>
   *
   * <p><b>Security Principle:</b> User-supplied input should never be directly used in regex
   * patterns without escaping. This method treats user input as a literal string, preventing
   * attackers from injecting regex metacharacters to alter pattern behavior.
   *
   * <p><b>Example:</b>
   *
   * <pre>
   * // VULNERABLE: User input directly in Pattern.compile
   * String userInput = "test.*";  // malicious input
   * Pattern p = Pattern.compile(userInput);  // this would match unintended strings
   *
   * // SECURE: User input escaped with this method
   * String userInput = "test.*";
   * String escaped = escapeRegexString(userInput);  // returns "test\\.\\*"
   * Pattern p = Pattern.compile(escaped);  // safe, matches literal "test.*"
   * </pre>
   *
   * @param input The string to escape, may be null
   * @return The escaped string safe for use in regex patterns, or the original string if null
   * @see java.util.regex.Pattern#quote(String)
   */
  public static String escapeRegexString(final String input) {
    if (input == null) {
      return null;
    }
    // Use Pattern.quote which is the standard Java method for this purpose
    // It escapes all regex special characters and wraps the result in \Q...\E
    return Pattern.quote(input);
  }

  /**
   * Safely creates a regex Pattern from user-supplied input by treating the input as a literal
   * string.
   *
   * <p>This method is a convenience wrapper around {@link #escapeRegexString(String)} and {@link
   * Pattern#compile(String)} for common use cases.
   *
   * <p><b>Security:</b> User input is safely escaped and cannot inject regex metacharacters or
   * alter pattern behavior.
   *
   * <p><b>Example:</b>
   *
   * <pre>
   * String userSearch = "test.*";  // from user
   * // This will match only the literal string "test.*", not as regex pattern
   * Pattern safePattern = createSafeRegexPattern(userSearch);
   * </pre>
   *
   * @param input The string to create a pattern from, may be null
   * @return A compiled Pattern that matches the literal string, or null if input is null
   * @throws java.util.regex.PatternSyntaxException if the input contains an invalid pattern
   *     (unlikely since characters are escaped)
   */
  public static Pattern createSafeRegexPattern(final String input) {
    if (input == null) {
      return null;
    }
    return Pattern.compile(escapeRegexString(input));
  }

  /**
   * Safely creates a regex Pattern from user-supplied input with flags.
   *
   * <p>This method applies safety escaping to user input while preserving regex compilation flags
   * for case-insensitive matching, multiline mode, etc.
   *
   * <p><b>Example:</b>
   *
   * <pre>
   * String userSearch = "Test";
   * // Case-insensitive literal match
   * Pattern p = createSafeRegexPattern(userSearch, Pattern.CASE_INSENSITIVE);
   * Matcher m = p.matcher("test");  // matches
   * </pre>
   *
   * @param input The string to create a pattern from, may be null
   * @param flags Regex compilation flags (Pattern.CASE_INSENSITIVE, Pattern.MULTILINE, etc.)
   * @return A compiled Pattern that matches the literal string, or null if input is null
   */
  public static Pattern createSafeRegexPattern(final String input, final int flags) {
    if (input == null) {
      return null;
    }
    return Pattern.compile(escapeRegexString(input), flags);
  }
}
