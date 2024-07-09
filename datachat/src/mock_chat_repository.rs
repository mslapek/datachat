use crate::domain::chat_repository;
use targetdb;

pub struct MockChatRepository {
    data: Vec<chat_repository::Question>,
}

impl MockChatRepository {
    pub fn new() -> Self {
        Self {
            data: Self::get_sample_data(),
        }
    }

    fn get_sample_data() -> Vec<chat_repository::Question> {
        vec![
            chat_repository::Question {
                sql_query: "SELECT * FROM users".to_string(),
                state: chat_repository::QuestionState::Done(Ok(targetdb::QueryResult {
                    column_names: vec!["id".to_string(), "name".to_string()],
                    rows: vec![
                        vec!["1".to_string(), "Alice".to_string()],
                        vec!["2".to_string(), "Bob".to_string()],
                    ],
                })),
            },
            chat_repository::Question {
                sql_query: "SELECT * FROM users WHERE id = 1".to_string(),
                state: chat_repository::QuestionState::Done(Ok(targetdb::QueryResult {
                    column_names: vec!["id".to_string(), "name".to_string()],
                    rows: vec![vec!["1".to_string(), "Alice".to_string()]],
                })),
            },
            chat_repository::Question {
                sql_query: "SELECT * FROM users WHERE id = 2".to_string(),
                state: chat_repository::QuestionState::Done(Ok(targetdb::QueryResult {
                    column_names: vec!["id".to_string(), "name".to_string()],
                    rows: vec![vec!["2".to_string(), "Bob".to_string()]],
                })),
            },
            chat_repository::Question {
                sql_query: "SELECT * FROM users WHERE id = 3".to_string(),
                state: chat_repository::QuestionState::Done(Err(targetdb::TargetDatabaseError {
                    message: "Table 'users' does not contain row with id = 3".to_string(),
                })),
            },
        ]
    }
}
