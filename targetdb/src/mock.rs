use crate::db::*;
use async_trait::async_trait;

pub struct MockTargetDatabase;

impl MockTargetDatabase {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl TargetDatabase for MockTargetDatabase {
    async fn get_schema(&self) -> Result<DatabaseSchema, TargetDatabaseError> {
        Ok(DatabaseSchema {
            tables: vec![
                TableSchema {
                    name: "users".to_string(),
                    columns: vec![
                        ColumnSchema {
                            name: "id".to_string(),
                            type_: "int".to_string(),
                        },
                        ColumnSchema {
                            name: "name".to_string(),
                            type_: "varchar".to_string(),
                        },
                        ColumnSchema {
                            name: "email".to_string(),
                            type_: "varchar".to_string(),
                        },
                    ],
                },
                TableSchema {
                    name: "messages".to_string(),
                    columns: vec![
                        ColumnSchema {
                            name: "id".to_string(),
                            type_: "int".to_string(),
                        },
                        ColumnSchema {
                            name: "sender_id".to_string(),
                            type_: "int".to_string(),
                        },
                        ColumnSchema {
                            name: "receiver_id".to_string(),
                            type_: "int".to_string(),
                        },
                        ColumnSchema {
                            name: "content".to_string(),
                            type_: "varchar".to_string(),
                        },
                    ],
                },
            ],
        })
    }

    async fn execute_query(&self, sql_query: &str) -> Result<TableValues, TargetDatabaseError> {
        if sql_query.starts_with("error") {
            return Err(TargetDatabaseError {
                message: format!("Error {}", sql_query),
            });
        }

        Ok(TableValues {
            column_names: vec!["id".to_string(), "name".to_string(), "email".to_string()],
            rows: vec![
                vec![
                    "1".to_string(),
                    "Alice".to_string(),
                    "example@example.com".to_string(),
                ],
                vec!["2".to_string(), "Bob".to_string(), sql_query.to_string()],
            ],
        })
    }
}
