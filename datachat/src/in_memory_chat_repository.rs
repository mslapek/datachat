use crate::domain::chat_repository;
use std::collections::HashMap;
use std::sync::Mutex;
use uuid::Uuid;
use async_trait::async_trait;

pub struct InMemoryChatRepository {
    repo: Mutex<InMemoryChatRepositoryInner>,
}

impl InMemoryChatRepository {
    pub fn new() -> Self {
        Self {
            repo: Mutex::new(InMemoryChatRepositoryInner::new()),
        }
    }
}

#[async_trait]
impl chat_repository::ChatRepository for InMemoryChatRepository {
    async fn get_all_questions(
        &self,
        chat_id: &Uuid,
    ) -> Result<Vec<chat_repository::Question>, chat_repository::ChatRepositoryError> {
        self.repo.lock().unwrap().get_all_questions(chat_id)
    }

    async fn add_question(
        &self,
        chat_id: &Uuid,
        question_id: u32,
        sql_query: &str,
    ) -> Result<(), chat_repository::ChatRepositoryError> {
        self.repo
            .lock()
            .unwrap()
            .add_question(chat_id, question_id, sql_query)
    }

    async fn set_question_result(
        &self,
        chat_id: &Uuid,
        question_id: u32,
        result: &Result<targetdb::TableValues, targetdb::TargetDatabaseError>,
    ) -> Result<(), chat_repository::ChatRepositoryError> {
        self.repo
            .lock()
            .unwrap()
            .set_question_result(chat_id, question_id, result)
    }
}

struct InMemoryChatRepositoryInner {
    data: HashMap<Uuid, Vec<chat_repository::Question>>,
}

impl InMemoryChatRepositoryInner {
    fn new() -> Self {
        Self {
            data: HashMap::new(),
        }
    }

    fn get_all_questions(
        &self,
        chat_id: &Uuid,
    ) -> Result<Vec<chat_repository::Question>, chat_repository::ChatRepositoryError> {
        Ok(self.data.get(chat_id).cloned().unwrap_or_default())
    }

    fn add_question(
        &mut self,
        chat_id: &Uuid,
        question_id: u32,
        sql_query: &str,
    ) -> Result<(), chat_repository::ChatRepositoryError> {
        let questions = self.data.entry(*chat_id).or_insert_with(Vec::new);
        if question_id != questions.len() as u32 {
            return Err(chat_repository::ChatRepositoryError {
                message: format!(
                    "Invalid question id: expected {}, got {}",
                    questions.len(),
                    question_id
                ),
            });
        }
        questions.push(chat_repository::Question {
            sql_query: sql_query.to_string(),
            state: chat_repository::QuestionState::Pending,
        });
        Ok(())
    }

    fn set_question_result(
        &mut self,
        chat_id: &Uuid,
        question_id: u32,
        result: &Result<targetdb::TableValues, targetdb::TargetDatabaseError>,
    ) -> Result<(), chat_repository::ChatRepositoryError> {
        let questions =
            self.data
                .get_mut(chat_id)
                .ok_or_else(|| chat_repository::ChatRepositoryError {
                    message: format!("Chat not found: {:?}", chat_id),
                })?;
        let question = questions.get_mut(question_id as usize).ok_or_else(|| {
            chat_repository::ChatRepositoryError {
                message: format!("Question not found: {:?}", question_id),
            }
        })?;
        question.state = chat_repository::QuestionState::Done(result.clone());
        Ok(())
    }
}
