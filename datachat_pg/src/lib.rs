use async_trait::async_trait;
use std::{ops::DerefMut, sync::Arc};
use targetdb;
use tokio;
use tokio::sync::Mutex;
use tokio_postgres::{self, SimpleQueryMessage, SimpleQueryRow};

pub struct Database {
    client: Mutex<Option<Arc<tokio_postgres::Client>>>,
    connection_string: String,
}

impl Database {
    pub fn new(connection_string: &str) -> Self {
        Self {
            client: Default::default(),
            connection_string: connection_string.to_string(),
        }
    }

    async fn get_client(
        &self,
    ) -> Result<Arc<tokio_postgres::Client>, targetdb::TargetDatabaseError> {
        let mut lock = self.client.lock().await;
        let maybe_client = lock.deref_mut();

        match maybe_client {
            Some(client) => Ok(client.clone()),
            None => {
                let (client, connection) =
                    tokio_postgres::connect(&self.connection_string, tokio_postgres::NoTls)
                        .await
                        .map_err(|e| targetdb::TargetDatabaseError {
                            message: format!("Failed to connect to database: {}", e),
                        })?;
                tokio::spawn(async move {
                    if let Err(e) = connection.await {
                        eprintln!("connection error: {}", e);
                    }
                });
                let client = Arc::new(client);
                maybe_client.replace(client.clone());
                Ok(client)
            }
        }
    }
}

#[async_trait]
impl targetdb::TargetDatabase for Database {
    async fn execute_query(
        &self,
        sql_query: &str,
    ) -> Result<targetdb::TableValues, targetdb::TargetDatabaseError> {
        let client = self.get_client().await?;
        let query_messages =
            client
                .simple_query(sql_query)
                .await
                .map_err(|e| targetdb::TargetDatabaseError {
                    message: format!("Failed to execute query: {}", e),
                })?;
        let rows = query_messages
            .iter()
            .filter_map(|m| match m {
                SimpleQueryMessage::Row(row) => Some(row),
                _ => None,
            })
            .collect::<Vec<&SimpleQueryRow>>();

        let first_row = match rows.first() {
            Some(row) => *row,
            _ => return Ok(targetdb::TableValues::default()),
        };
        let column_names = first_row
            .columns()
            .iter()
            .map(|c| c.name().to_string())
            .collect();
        let rows = rows
            .iter()
            .map(|row| {
                (0..row.len())
                    .map(|i| row.get(i).unwrap().to_string())
                    .collect()
            })
            .collect();
        Ok(targetdb::TableValues { column_names, rows })
    }

    async fn get_schema(&self) -> Result<targetdb::DatabaseSchema, targetdb::TargetDatabaseError> {
        let client = self.get_client().await?;
        let rows = client.query("SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public'", &[])
            .await
            .map_err(|e| targetdb::TargetDatabaseError {
                message: format!("Failed to get schema: {}", e),
            })?;
        let mut tables = std::collections::HashMap::new();
        for row in rows {
            let table_name: String = row.get(0);
            let column_name: String = row.get(1);
            let data_type: String = row.get(2);
            let table = tables
                .entry(table_name.clone())
                .or_insert_with(|| targetdb::TableSchema {
                    name: table_name.clone(),
                    columns: Vec::new(),
                });
            table.columns.push(targetdb::ColumnSchema {
                name: column_name,
                type_: data_type,
            });
        }
        Ok(targetdb::DatabaseSchema {
            tables: tables.into_iter().map(|(_, v)| v).collect(),
        })
    }
}
