mod domain;
mod in_memory_chat_repository;
mod message_serialization;
use anyhow::{Context, Result};
use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        State,
    },
    response::IntoResponse,
    routing::get,
    Router,
};
use datachat_pg;
use in_memory_chat_repository::InMemoryChatRepository;
use std::sync::Arc;
use targetdb::{mock::MockTargetDatabase, TargetDatabase};
use tokio_stream::StreamExt;

#[tokio::main]
async fn main() {
    // get TARGETDB from env var
    let targetdb_string = std::env::var("TARGETDB").expect("expected TARGETDB env var");
    let target_db: Arc<dyn TargetDatabase + Send + Sync> = if targetdb_string == "mock" {
        Arc::new(MockTargetDatabase::new())
    } else {
        Arc::new(datachat_pg::Database::new(&targetdb_string))
    };

    let websocket_controller = domain::websocket_messages::Controller {
        chat_repo: Arc::new(InMemoryChatRepository::new()),
        target_db,
    };

    // build our application with a single route
    let app = Router::new()
        .route("/", get(|| async { "Hello, World!" }))
        .route("/chat-ws", get(ws_handler))
        .with_state(websocket_controller);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn ws_handler(
    ws: WebSocketUpgrade,
    State(controller): State<domain::websocket_messages::Controller>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| async move {
        let r = handle_ws_socket(socket, controller).await;
        if let Err(e) = r {
            eprintln!("websocket error: {:?}", e);
        }
    })
}

async fn handle_ws_socket(
    mut socket: WebSocket,
    controller: domain::websocket_messages::Controller,
) -> Result<()> {
    while let Some(message) = socket.recv().await {
        match message {
            Ok(Message::Binary(buf)) => {
                let msg = message_serialization::deserialize_client_message(buf)
                    .context("websocket deserialization failure")?;
                let mut responses = controller.handle_inbound_message(&msg);
                while let Some(response) = responses.try_next().await? {
                    let response = message_serialization::serialize_server_message(response)
                        .context("websocket serialization failure")?;
                    socket.send(Message::Binary(response)).await?;
                }
            }
            Ok(Message::Close(_)) => {
                break;
            }
            Err(e) => {
                return Err(e.into());
            }
            _ => {}
        }
    }
    Ok(())
}
