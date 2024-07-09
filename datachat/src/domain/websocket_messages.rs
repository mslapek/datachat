use super::chat_repository::Question;
use crate::domain::chat_repository;
use anyhow::Result;
use futures::future::FutureExt;
use futures::TryFutureExt;
use std::sync::Arc;
use targetdb;
use tokio::task;
use tokio_stream::Stream;
use tokio_stream::StreamExt;
use uuid::Uuid;

pub struct WithChatId<T> {
    pub chat_id: Uuid,
    pub payload: T,
}

impl<T> WithChatId<T> {
    pub fn with_chat_id(payload: T, chat_id: Uuid) -> WithChatId<T> {
        WithChatId { chat_id, payload }
    }
}

pub mod client {
    use super::WithChatId;

    pub type Message = WithChatId<MessagePayload>;

    pub enum MessagePayload {
        StartChat,
        AddQuestion(AddQuestionMessage),
    }

    pub struct AddQuestionMessage {
        pub question_id: u32,
        pub sql_query: String,
    }
}

pub mod server {
    use crate::domain::chat_repository;

    use super::WithChatId;

    pub type Message = WithChatId<MessagePayload>;

    pub enum MessagePayload {
        QuestionResult(QuestionResultMessage),

        // Result describes potential connection error with the datachat database.
        AllQuestions(Result<Vec<chat_repository::Question>, String>),

        // Result describes potential connection error with the target database.
        DatabaseSchema(Result<targetdb::DatabaseSchema, String>),
    }

    pub struct QuestionResultMessage {
        pub question_id: u32,
        pub state: chat_repository::Question,
    }
}

#[derive(Clone)]
pub struct Controller {
    pub chat_repo: Arc<dyn chat_repository::ChatRepository + Sync + Send>,
    pub target_db: Arc<dyn targetdb::TargetDatabase + Sync + Send>,
}

impl Controller {
    pub fn handle_inbound_message(
        &self,
        message: &client::Message,
    ) -> Box<dyn Stream<Item = Result<server::Message>> + Unpin + Send> {
        match &message.payload {
            client::MessagePayload::StartChat => self.handle_start_chat(message.chat_id),
            client::MessagePayload::AddQuestion(add_question) => {
                self.handle_add_question(message.chat_id, add_question)
            }
        }
    }

    fn handle_start_chat(
        &self,
        chat_id: Uuid,
    ) -> Box<dyn Stream<Item = Result<server::Message>> + Unpin + Send> {
        let target_db = self.target_db.clone();

        let schema_message = task::spawn(async move {
            let schema = target_db.get_schema().await;
            let msg = server::MessagePayload::DatabaseSchema(schema.map_err(|e| e.message));
            WithChatId::with_chat_id(msg, chat_id)
        })
        .map_err(|e| e.into())
        .into_stream();

        let chat_repo = self.chat_repo.clone();

        let questions_message = task::spawn(async move {
            let questions = chat_repo
                .get_all_questions(&chat_id)
                .await
                .map_err(|e| e.message);

            let msg = server::MessagePayload::AllQuestions(questions);
            WithChatId::with_chat_id(msg, chat_id)
        })
        .map_err(|e| e.into())
        .into_stream();

        Box::new(schema_message.merge(questions_message))
    }

    fn handle_add_question(
        &self,
        chat_id: Uuid,
        message: &client::AddQuestionMessage,
    ) -> Box<dyn Stream<Item = Result<server::Message>> + Unpin + Send> {
        let question_id = message.question_id;
        let sql_query = message.sql_query.clone();
        let chat_repo = self.chat_repo.clone();
        let target_db = self.target_db.clone();

        Box::new(
            task::spawn(async move {
                chat_repo
                    .add_question(&chat_id, question_id, &sql_query)
                    .await?;

                let output = target_db.execute_query(&sql_query).await;

                chat_repo
                    .set_question_result(&chat_id, question_id, &output)
                    .await?;

                let msg = server::MessagePayload::QuestionResult(server::QuestionResultMessage {
                    question_id,
                    state: Question {
                        sql_query,
                        state: chat_repository::QuestionState::Done(output),
                    },
                });

                Ok(WithChatId::with_chat_id(msg, chat_id))
            })
            .map(|r| r.unwrap())
            .into_stream(),
        )
    }
}
