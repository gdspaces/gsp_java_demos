
import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TStatementList;
import gudusoft.gsqlparser.nodes.TObjectName;
import gudusoft.gsqlparser.nodes.TObjectNameList;
import gudusoft.gsqlparser.nodes.TTable;
import gudusoft.gsqlparser.nodes.TTableList;
import gudusoft.gsqlparser.pp.para.GFmtOptFactory;
import gudusoft.gsqlparser.pp.para.GFmtOpt;
import gudusoft.gsqlparser.pp.stmtformattor.FormattorFactory;


public class Test {

	public static void main(String args[])
	{

		TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvoracle);

		sqlparser.sqltext = "select col1, col2,sum(col3) from table1, table2 where col4 > col5 and col6= 1000";

		try {
			int ret = sqlparser.parse();
			if (ret == 0) {
				TStatementList stmts = sqlparser.sqlstatements;
				for (int i = 0; i < stmts.size(); i++) {
					TCustomSqlStatement stmt = stmts.get(i);

					TTableList tables = stmt.tables;
					for (int j = 0; j < tables.size(); j++) {
						TTable table = tables.getTable(i);
						String tableName = table.getFullName();
						TObjectNameList columns = table.getObjectNameReferences();

						for (int k = 0; k < columns.size(); k++) {
							{
								TObjectName columnName = columns.getObjectName(k);
								System.out.println(tableName + " "+ columnName.toString());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}



	}

}
