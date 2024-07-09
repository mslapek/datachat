use crate::domain::websocket_messages;
use anyhow::Result;
use prost::Message;

mod proto {
pub mod common {
    use uuid::Uuid;

    include!(concat!(env!("OUT_DIR"), "/datachat.ws.common.rs"));

    impl From<Uuid> for ChatId {
        fn from(uuid: Uuid) -> Self {
            ChatId {
                chat_id: uuid.to_string(),
            }
        }
    }

    impl TryFrom<ChatId> for Uuid {
        type Error = anyhow::Error;

        fn try_from(chat_id: ChatId) -> Result<Self, anyhow::Error> {
            Uuid::parse_str(&chat_id.chat_id).map_err(|e| e.into())
        }
    }
}


pub mod targetdatabase {
    use targetdb;

    include!(concat!(env!("OUT_DIR"), "/datachat.ws.targetdatabase.rs"));

    impl From<targetdb::ColumnSchema> for ColumnSchema {
        fn from(column: targetdb::ColumnSchema) -> Self {
            Self {
                name: column.name,
                r#type: column.type_,
            }
        }
    }

    impl From<targetdb::TableSchema> for TableSchema {
        fn from(table: targetdb::TableSchema) -> Self {
            Self {
                name: table.name,
                columns: table
                    .columns
                    .into_iter()
                    .map(|column| column.into())
                    .collect(),
            }
        }
    }

    impl From<targetdb::DatabaseSchema> for DatabaseSchema {
        fn from(db_schema: targetdb::DatabaseSchema) -> Self {
            Self {
                tables: db_schema
                    .tables
                    .into_iter()
                    .map(|table| table.into())
                    .collect(),
            }
        }
    }

    impl From<targetdb::TableValues> for TableValues {
        fn from(table_values: targetdb::TableValues) -> Self {
            Self {
                column_names: table_values.column_names,
                rows: table_values
                    .rows
                    .into_iter()
                    .map(|row| row.into())
                    .collect(),
            }
        }
    }

    impl From<Vec<String>> for RowValues {
        fn from(row: Vec<String>) -> Self {
            Self { values: row }
        }
    }
}

pub mod client {
    use crate::domain::websocket_messages;
    use anyhow::anyhow;

    include!(concat!(env!("OUT_DIR"), "/datachat.ws.client.rs"));

    impl TryFrom<ClientMessage> for websocket_messages::client::Message {
        type Error = anyhow::Error;

        fn try_from(msg: ClientMessage) -> Result<Self, anyhow::Error> {
            Ok(websocket_messages::client::Message {
                chat_id: msg.chat_id.ok_or_else(|| anyhow!("missing chat_id"))?.try_into()?,
                payload: msg
                    .payload
                    .ok_or_else(|| anyhow!("missing payload"))?
                    .into(),
            })
        }
    }

    impl From<client_message::Payload> for websocket_messages::client::MessagePayload {
        fn from(payload: client_message::Payload) -> Self {
            match payload {
                client_message::Payload::StartChat(_) => Self::StartChat,
                client_message::Payload::AddQuestion(add_question) => {
                    Self::AddQuestion(add_question.into())
                }
            }
        }
    }

    impl From<AddQuestionMessage> for websocket_messages::client::AddQuestionMessage {
        fn from(add_question: AddQuestionMessage) -> Self {
            Self {
                question_id: add_question.question_id,
                sql_query: add_question.sql_query,
            }
        }
    }
}

pub mod server {
    use crate::domain::chat_repository;
    use crate::domain::websocket_messages;

    include!(concat!(env!("OUT_DIR"), "/datachat.ws.server.rs"));

    impl From<websocket_messages::server::Message> for ServerMessage {
        fn from(msg: websocket_messages::server::Message) -> Self {
            Self {
                chat_id: Some(msg.chat_id.into()),
                payload: Some(msg.payload.into()),
            }
        }
    }

    impl From<websocket_messages::server::MessagePayload> for server_message::Payload {
        fn from(payload: websocket_messages::server::MessagePayload) -> Self {
            match payload {
                websocket_messages::server::MessagePayload::QuestionResult(question_result) => {
                    Self::QuestionResult(question_result.into())
                }
                websocket_messages::server::MessagePayload::AllQuestions(all_questions) => {
                    Self::AllQuestions(all_questions.into())
                }
                websocket_messages::server::MessagePayload::DatabaseSchema(db_schema) => {
                    Self::DatabaseSchema(db_schema.into())
                }
            }
        }
    }

    impl From<websocket_messages::server::QuestionResultMessage> for QuestionResultMessage {
        fn from(question_result: websocket_messages::server::QuestionResultMessage) -> Self {
            Self {
                question_id: question_result.question_id,
                state: Some(question_result.state.into()),
            }
        }
    }

    impl From<Result<Vec<chat_repository::Question>, String>> for AllQuestionsMessage {
        fn from(all_questions: Result<Vec<chat_repository::Question>, String>) -> Self {
            match all_questions {
                Ok(questions) => {
                    let questions = questions
                        .into_iter()
                        .map(|question| question.into())
                        .collect();
                    Self {
                        state: Some(all_questions_message::State::Questions(
                            QuestionStateArray { questions },
                        )),
                    }
                }
                Err(e) => Self {
                    state: Some(all_questions_message::State::Error(e)),
                },
            }
        }
    }

    impl From<chat_repository::Question> for QuestionState {
        fn from(question: chat_repository::Question) -> Self {
            Self {
                sql_query: question.sql_query,
                state: Some(question.state.into()),
            }
        }
    }

    impl From<chat_repository::QuestionState> for question_state::State {
        fn from(question_state: chat_repository::QuestionState) -> Self {
            match question_state {
                chat_repository::QuestionState::Pending => question_state::State::Pending(()),
                chat_repository::QuestionState::Done(Ok(query_result)) => {
                    question_state::State::Done(query_result.into())
                }
                chat_repository::QuestionState::Done(Err(e)) => {
                    question_state::State::Error(e.message)
                }
            }
        }
    }

    impl From<Result<targetdb::DatabaseSchema, String>> for DatabaseSchemaMessage {
        fn from(db_schema: Result<targetdb::DatabaseSchema, String>) -> Self {
            match db_schema {
                Ok(schema) => Self {
                    state: Some(database_schema_message::State::DatabaseSchema(
                        schema.into(),
                    )),
                },
                Err(e) => Self {
                    state: Some(database_schema_message::State::Error(e)),
                },
            }
        }
    }
}
}

pub fn deserialize_client_message(msg: Vec<u8>) -> Result<websocket_messages::client::Message> {
    let msg: proto::client::ClientMessage = Message::decode(msg.as_slice())?;
    msg.try_into()
}

pub fn serialize_server_message(msg: websocket_messages::server::Message) -> Result<Vec<u8>> {
    let msg: proto::server::ServerMessage = msg.into();
    let mut buf = Vec::new();
    Message::encode(&msg, &mut buf)?;
    Ok(buf)
}
