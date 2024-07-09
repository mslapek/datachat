use targetdb;
use uuid::Uuid;
use async_trait::async_trait;
use std::{error::Error, fmt::{self, Display, Formatter}};

#[derive(Clone)]
pub struct Question {
    pub sql_query: String,
    pub state: QuestionState,
}

#[derive(Clone)]
pub enum QuestionState {
    // Waiting for the query to finish.
    Pending,

    // If ok, query has finished successfully.
    // If err, target database returned an error for this query or got connection problem.
    Done(Result<targetdb::TableValues, targetdb::TargetDatabaseError>),
}

#[derive(Debug, Clone)]
pub struct ChatRepositoryError {
    pub message: String,
}

impl Display for ChatRepositoryError {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl Error for ChatRepositoryError {
}


#[async_trait]
pub trait ChatRepository {
    async fn get_all_questions(&self, chat_id: &Uuid) -> Result<Vec<Question>, ChatRepositoryError>;
    async fn add_question(&self, chat_id: &Uuid, question_id: u32, sql_query: &str) -> Result<(), ChatRepositoryError>;
    async fn set_question_result(&self, chat_id: &Uuid, question_id: u32, result: &Result<targetdb::TableValues, targetdb::TargetDatabaseError>) -> Result<(), ChatRepositoryError>;
}
