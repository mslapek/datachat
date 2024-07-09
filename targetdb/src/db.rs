use async_trait::async_trait;

#[derive(Default, Clone)]
pub struct TableValues {
    pub column_names: Vec<String>,
    pub rows: Vec<Vec<String>>,
}

#[derive(Clone)]
pub struct DatabaseSchema {
    pub tables: Vec<TableSchema>,
}

#[derive(Clone)]
pub struct TableSchema {
    pub name: String,
    pub columns: Vec<ColumnSchema>,
}

#[derive(Clone)]
pub struct ColumnSchema {
    pub name: String,
    pub type_: String,
}

#[derive(Clone)]
pub struct TargetDatabaseError {
    pub message: String,
}

#[async_trait]
pub trait TargetDatabase {
    async fn execute_query(&self, sql_query: &str) -> Result<TableValues, TargetDatabaseError>;
    async fn get_schema(&self) -> Result<DatabaseSchema, TargetDatabaseError>;
}