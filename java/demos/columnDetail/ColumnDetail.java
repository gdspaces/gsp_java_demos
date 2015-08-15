package demos.columnDetail;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TStatementList;
import gudusoft.gsqlparser.nodes.TAlterTableOption;
import gudusoft.gsqlparser.nodes.TColumnDefinition;
import gudusoft.gsqlparser.nodes.TConstraint;
import gudusoft.gsqlparser.nodes.TObjectName;
import gudusoft.gsqlparser.nodes.TObjectNameList;
import gudusoft.gsqlparser.nodes.TTypeName;
import gudusoft.gsqlparser.stmt.TAlterTableStatement;
import gudusoft.gsqlparser.stmt.TCreateIndexSqlStatement;
import gudusoft.gsqlparser.stmt.TCreateTableSqlStatement;
import gudusoft.gsqlparser.stmt.oracle.TOracleCommentOnSqlStmt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ColumnDetail {
	private Map<TableMetaData, List<ColumnMetaData>> tableColumns = new HashMap<TableMetaData, List<ColumnMetaData>>();

	public Map<TableMetaData, List<ColumnMetaData>> getTableColumns() {
		return tableColumns;
	}

	public ColumnDetail(File file, EDbVendor vendor) {
		tableColumns.clear();

		String content = getContent(file);
		String[] sqls = content.split(";\\s*\\n");
		for (int i = 0; i < sqls.length; i++) {
			TGSqlParser sqlparser = new TGSqlParser(vendor);
			sqlparser.sqltext = sqls[i].toUpperCase() + ";";
			parse(sqlparser);
		}
	}

	private String getContent(File file) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
			byte[] tmp = new byte[4096];
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			while (true) {
				int r = is.read(tmp);
				if (r == -1)
					break;
				out.write(tmp, 0, r);
			}
			byte[] bytes = out.toByteArray();
			is.close();
			out.close();
			String content = new String(bytes);
			return content.trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ColumnDetail(String sql, EDbVendor vendor) {
		tableColumns.clear();
		TGSqlParser sqlparser = new TGSqlParser(vendor);
		sqlparser.sqltext = sql.toUpperCase();
		parse(sqlparser);
	}

	private void parse(TGSqlParser sqlparser) {
		int ret = sqlparser.parse();
		if (ret == 0) {
			TStatementList stmts = sqlparser.sqlstatements;
			for (int i = 0; i < stmts.size(); i++) {
				TCustomSqlStatement stmt = stmts.get(i);
				parseStatement(stmt);
			}
		}
	}

	private void parseStatement(TCustomSqlStatement stmt) {
		if (stmt instanceof TCreateTableSqlStatement) {
			TCreateTableSqlStatement createTable = (TCreateTableSqlStatement) stmt;
			parseCreateTable(createTable);
		}
		if (stmt instanceof TOracleCommentOnSqlStmt) {
			TOracleCommentOnSqlStmt commentOn = (TOracleCommentOnSqlStmt) stmt;
			parseCommentOn(commentOn);
		}
		if (stmt instanceof TAlterTableStatement) {
			TAlterTableStatement alterTable = (TAlterTableStatement) stmt;
			TableMetaData tableMetaData = new TableMetaData();
			tableMetaData.setName(alterTable.getTableName().getTableString());
			tableMetaData.setSchemaName(alterTable.getTableName()
					.getSchemaString());
			if (!tableColumns.containsKey(tableMetaData)) {
				tableColumns
						.put(tableMetaData, new ArrayList<ColumnMetaData>());
			}
			parseAlterTable(alterTable, tableMetaData);
		}
		if (stmt instanceof TCreateIndexSqlStatement) {
			TCreateIndexSqlStatement createIndex = (TCreateIndexSqlStatement) stmt;
			parseCreateIndex(createIndex);
		}
	}

	private void parseCreateIndex(TCreateIndexSqlStatement createIndex) {
		switch (createIndex.getIndexType()) {
		case itUnique:
			// Can't get information from TCreateIndexSqlStatement
			break;
		default:
		}

	}

	private void parseAlterTable(TAlterTableStatement alterTable,
			TableMetaData tableMetaData) {
		for (int i = 0; i < alterTable.getAlterTableOptionList().size(); i++) {
			parseAlterTableOption(alterTable.getAlterTableOptionList()
					.getAlterTableOption(i), tableMetaData);
		}
	}

	private void parseAlterTableOption(TAlterTableOption alterTableOption,
			TableMetaData tableMetaData) {
		switch (alterTableOption.getOptionType()) {
		case AddColumn:
			for (int i = 0; i < alterTableOption.getColumnDefinitionList()
					.size(); i++) {
				parseColumnDefinition(alterTableOption
						.getColumnDefinitionList().getColumn(i), tableMetaData);
			}
			break;
		case ModifyColumn:
			for (int i = 0; i < alterTableOption.getColumnDefinitionList()
					.size(); i++) {
				parseColumnDefinition(alterTableOption
						.getColumnDefinitionList().getColumn(i), tableMetaData);
			}
			break;
		case AddConstraint:
			for (int i = 0; i < alterTableOption.getConstraintList().size(); i++) {
				parseTableConstraint(alterTableOption.getConstraintList()
						.getConstraint(i), tableMetaData);
			}
		default:

		}
	}

	private static boolean isNotEmpty(String str) {
		return str != null && str.trim().length() > 0;
	}

	private void parseCreateTable(TCreateTableSqlStatement createTable) {
		if (createTable.getTableName() != null) {
			String tableName = createTable.getTableName().getTableString();
			String tableSchema = createTable.getTableName().getSchemaString();
			TableMetaData tableMetaData = new TableMetaData();
			tableMetaData.setName(tableName);
			tableMetaData.setSchemaName(tableSchema);
			if (!tableColumns.containsKey(tableMetaData)) {
				tableColumns
						.put(tableMetaData, new ArrayList<ColumnMetaData>());
			}
			if (createTable.getTableComment() != null) {
				tableMetaData.setComment(createTable.getTableComment()
						.toString());
			}
			if (createTable.getColumnList() != null) {
				for (int i = 0; i < createTable.getColumnList().size(); i++) {
					TColumnDefinition columnDef = createTable.getColumnList()
							.getColumn(i);
					parseColumnDefinition(columnDef, tableMetaData);
				}
			}

			if (createTable.getTableConstraints() != null) {
				for (int i = 0; i < createTable.getTableConstraints().size(); i++) {
					TConstraint constraint = createTable.getTableConstraints()
							.getConstraint(i);
					parseTableConstraint(constraint, tableMetaData);
				}
			}
		}
	}

	private void parseTableConstraint(TConstraint constraint,
			TableMetaData tableMetaData) {
		if (constraint.getColumnList() == null)
			return;
		switch (constraint.getConstraint_type()) {
		case primary_key:
			if (constraint.getColumnList().size() == 1) {
				setColumnMetaDataPrimaryKey(tableMetaData,
						constraint.getColumnList());
			}
			break;
		case unique:
			if (constraint.getColumnList().size() == 1) {
				setColumnMetaDataUnique(tableMetaData,
						constraint.getColumnList());
			}
			break;
		case foreign_key:
			if (constraint.getColumnList().size() == 1) {
				setColumnMetaDataForeignKey(tableMetaData,
						constraint.getColumnList());
			}
			break;
		default:
			break;
		}
	}

	private void setColumnMetaDataForeignKey(TableMetaData tableMetaData,
			TObjectNameList columnList) {
		for (int i = 0; i < columnList.size(); i++) {
			TObjectName object = columnList.getObjectName(i);
			ColumnMetaData columnMetaData = getColumnMetaData(tableMetaData,
					object);
			columnMetaData.setForeignKey(true);
		}
	}

	private void setColumnMetaDataUnique(TableMetaData tableMetaData,
			TObjectNameList columnList) {
		for (int i = 0; i < columnList.size(); i++) {
			TObjectName object = columnList.getObjectName(i);
			ColumnMetaData columnMetaData = getColumnMetaData(tableMetaData,
					object);
			columnMetaData.setUnique(true);
		}
	}

	private void setColumnMetaDataPrimaryKey(TableMetaData tableMetaData,
			TObjectNameList columnList) {
		for (int i = 0; i < columnList.size(); i++) {
			TObjectName object = columnList.getObjectName(i);
			ColumnMetaData columnMetaData = getColumnMetaData(tableMetaData,
					object);
			columnMetaData.setPrimaryKey(true);
		}
	}

	private void parseColumnDefinition(TColumnDefinition columnDef,
			TableMetaData tableMetaData) {
		if (columnDef.getColumnName() != null) {
			TObjectName object = columnDef.getColumnName();
			ColumnMetaData columnMetaData = getColumnMetaData(tableMetaData,
					object);

			if (object.getCommentString() != null) {
				String columnComment = object.getCommentString().toString();
				columnMetaData.setComment(columnComment);
			}

			if (columnDef.getDefaultExpression() != null) {
				columnMetaData.setDefaultVlaue(columnDef.getDefaultExpression()
						.toString());
			}

			if (columnDef.getDatatype() != null) {
				TTypeName type = columnDef.getDatatype();
				String typeName = type.toString();
				int typeNameIndex = typeName.indexOf("(");
				if (typeNameIndex != -1)
					typeName = typeName.substring(0, typeNameIndex);
				columnMetaData.setTypeName(typeName);
				if (type.getScale() != null) {
					try {
						columnMetaData.setScale(Integer.parseInt(type
								.getScale().toString()));
					} catch (NumberFormatException e1) {
					}
				}
				if (type.getPrecision() != null) {
					try {
						columnMetaData.setPrecision(Integer.parseInt(type
								.getPrecision().toString()));
					} catch (NumberFormatException e) {
					}
				}
				if (type.getLength() != null) {
					try {
						columnMetaData.setColumnDisplaySize(Integer
								.parseInt(type.getLength().toString()));
					} catch (NumberFormatException e) {
					}
				}
			}

			if (columnDef.isNull()) {
				columnMetaData.setNull(true);
			}

			if (columnDef.getConstraints() != null) {
				for (int i = 0; i < columnDef.getConstraints().size(); i++) {
					TConstraint constraint = columnDef.getConstraints()
							.getConstraint(i);
					switch (constraint.getConstraint_type()) {
					case notnull:
						columnMetaData.setNotNull(true);
						break;
					case primary_key:
						columnMetaData.setPrimaryKey(true);
						break;
					case unique:
						columnMetaData.setUnique(true);
						break;
					case check:
						columnMetaData.setCheck(true);
						break;
					case foreign_key:
						columnMetaData.setForeignKey(true);
						break;
					case fake_auto_increment:
						columnMetaData.setAutoIncrement(true);
						break;
					case fake_comment:
						// Can't get comment information.
					default:
						break;
					}
				}
			}
		}
	}

	private ColumnMetaData getColumnMetaData(TableMetaData tableMetaData,
			TObjectName object) {
		ColumnMetaData columnMetaData = new ColumnMetaData();
		String columnName = object.getColumnNameOnly();
		columnMetaData.setName(columnName);

		if (isNotEmpty(object.getTableString())) {
			columnMetaData.setTableName(object.getTableString());
		} else {
			columnMetaData.setTableName(tableMetaData.getName());
		}

		if (isNotEmpty(object.getSchemaString())) {
			columnMetaData.setSchemaName(object.getSchemaString());
		} else {
			columnMetaData.setSchemaName(tableMetaData.getSchemaName());
		}

		int index = tableColumns.get(tableMetaData).indexOf(columnMetaData);
		if (index != -1) {
			columnMetaData = tableColumns.get(tableMetaData).get(index);
		} else {
			tableColumns.get(tableMetaData).add(columnMetaData);
		}
		return columnMetaData;
	}

	private void parseCommentOn(TOracleCommentOnSqlStmt commentOn) {
		if (commentOn.getDbObjType() == TObjectName.ttobjTable) {
			String tableName = commentOn.getObjectName().getPartString();
			String tableSchema = commentOn.getObjectName().getObjectString();
			TableMetaData tableMetaData = new TableMetaData();
			tableMetaData.setName(tableName);
			tableMetaData.setSchemaName(tableSchema);
			if (!tableColumns.containsKey(tableMetaData)) {
				tableColumns
						.put(tableMetaData, new ArrayList<ColumnMetaData>());
			}
			tableMetaData.setComment(commentOn.getMessage().toString());
		} else if (commentOn.getDbObjType() == TObjectName.ttobjColumn) {
			ColumnMetaData columnMetaData = new ColumnMetaData();
			String columnName = commentOn.getObjectName().getColumnNameOnly();
			columnMetaData.setName(columnName);

			if (isNotEmpty(commentOn.getObjectName().getTableString())) {
				columnMetaData.setTableName(commentOn.getObjectName()
						.getTableString());
			}

			if (isNotEmpty(commentOn.getObjectName().getSchemaString())) {
				columnMetaData.setSchemaName(commentOn.getObjectName()
						.getSchemaString());
			}

			TableMetaData tableMetaData = new TableMetaData();
			tableMetaData.setName(columnMetaData.getTableName());
			tableMetaData.setSchemaName(columnMetaData.getSchemaName());
			if (!tableColumns.containsKey(tableMetaData)) {
				tableColumns
						.put(tableMetaData, new ArrayList<ColumnMetaData>());
			}
			int index = tableColumns.get(tableMetaData).indexOf(columnMetaData);
			if (index != -1) {
				tableColumns.get(tableMetaData).get(index)
						.setComment(commentOn.getMessage().toString());
			} else {
				columnMetaData.setComment(commentOn.getMessage().toString());
				tableColumns.get(tableMetaData).add(columnMetaData);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out
					.println("Usage: java ColumnDetail <DDL file path> [/t <database type>]");

			System.out
					.println("/t: Option, set the database type. Support oracle, mysql, mssql and db2, the default type is oracle");

			return;
		}

		File ddlFile = new File(args[0]);
		if (!ddlFile.exists() || ddlFile.isDirectory()) {
			System.out.println(ddlFile + " is not a valid file.");
			return;
		}

		List<String> argList = Arrays.asList(args);

		EDbVendor vendor = EDbVendor.dbvoracle;

		int index = argList.indexOf("/t");

		if (index != -1 && args.length > index + 1) {
			if (args[index + 1].equalsIgnoreCase("mssql")) {
				vendor = EDbVendor.dbvmssql;
			} else if (args[index + 1].equalsIgnoreCase("db2")) {
				vendor = EDbVendor.dbvdb2;
			} else if (args[index + 1].equalsIgnoreCase("mysql")) {
				vendor = EDbVendor.dbvmysql;
			} else if (args[index + 1].equalsIgnoreCase("mssql")) {
				vendor = EDbVendor.dbvmssql;
			} else if (args[index + 1].equalsIgnoreCase("netezza")) {
				vendor = EDbVendor.dbvnetezza;
			} else if (args[index + 1].equalsIgnoreCase("teradata")) {
				vendor = EDbVendor.dbvteradata;
			}
		}

		ColumnDetail parser = new ColumnDetail(ddlFile, vendor);
		Iterator<TableMetaData> tableIter = parser.getTableColumns().keySet()
				.iterator();
		try {
			JSONObject object = new JSONObject();
			JSONObject metaData = new JSONObject();
			object.put("meta-data", metaData);
			JSONArray columns = new JSONArray();
			metaData.put("columns", columns);
			while (tableIter.hasNext()) {
				TableMetaData table = tableIter.next();
				List<ColumnMetaData> columnList = parser.getTableColumns().get(
						table);
				for (int i = 0; i < columnList.size(); i++) {
					ColumnMetaData column = columnList.get(i);
					JSONObject columnObj = new JSONObject();
					columnObj.put(column.getName(), column);
					columns.put(columnObj);
				}
			}

			System.out.println(object.toString(4));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
