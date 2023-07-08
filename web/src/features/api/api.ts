import {DbSchema} from "../schema/schemaSlice.ts";
import {AllQuestions, Question} from "../questions/questionsSlice.ts";
import {store} from "../../app/store.ts";

function getDatabaseSchemaAction(data: any) {
    let payload: DbSchema
    if (data.error) {
        payload = {
            "tables": null,
            "error": data.error,
            "status": "error"
        }
    } else {
        payload = {
            "tables": data.schema.tables,
            "error": null,
            "status": "ready"
        }
    }

    return {
        "type": "schema/setSchema",
        "payload": payload
    };
}

function getAllQuestionsAction(data: any) {
    let payload: AllQuestions
    if (data.error) {
        payload = {
            "messages": null,
            "error": data.error,
            "status": "error"
        }
    } else {
        let q: Question[] = data.questions.map((msg: any, i: number) => {
            let status: "ready" | "loading" | "error"
            if (msg.error) {
                status = "error"
            } else if (msg.queryResult) {
                status = "ready"
            } else {
                status = "loading"
            }

            let q: Question = {
                "id": i,
                "status": status,
                "error": msg.error,
                "sqlQuery": msg.sqlQuery,
                "queryResult": msg.queryResult,
            }
            return q
        })
        payload = {
            "messages": q,
            "error": null,
            "status": "ready"
        }
    }

    return {
        "type": "questions/setAllQuestions",
        "payload": payload
    }
}


function getQuestionResultAction(data: any) {
    let payload: Question
    if (data.question.error) {
        payload = {
            "id": data.questionId,
            "status": "error",
            "error": data.question.error,
            "sqlQuery": data.question.sqlQuery,
            "queryResult": null,
        }
    } else if (data.question.queryResult) {
        payload = {
            "id": data.questionId,
            "status": "ready",
            "error": null,
            "sqlQuery": data.question.sqlQuery,
            "queryResult": data.question.queryResult,
        }
    } else {
        payload = {
            "id": data.questionId,
            "status": "loading",
            "error": null,
            "sqlQuery": data.question.sqlQuery,
            "queryResult": null,
        }
    }

    return {
        "type": "questions/setQuestionResult",
        "payload": payload
    }
}

const socket = new WebSocket("ws://localhost:8080/chat-ws");

// add support for multiple chats in a future
let chatId = "12340000-0000-0000-0000-000000000000";

socket.addEventListener("open", (_) => {
    socket.send(JSON.stringify({
        "type": "startChat",
        "chatId": chatId,
    }));
});

socket.addEventListener("message", (event) => {
    let d = JSON.parse(event.data);
    if (d.chatId != chatId) {
        return;
    }

    switch (d.type) {
        case "databaseSchema":
            store.dispatch(getDatabaseSchemaAction(d))
            break;
        case "allQuestions":
            store.dispatch(getAllQuestionsAction(d))
            break;
        case "questionResult":
            store.dispatch(getQuestionResultAction(d))
            break;
        default:
            console.log("Unknown message type: " + d.type)
    }
});

export function sendAddQuestion(q: Question) {
    socket.send(JSON.stringify({
        "type": "addQuestion",
        "chatId": chatId,
        "questionId": q.id,
        "sqlQuery": q.sqlQuery,
    }));
}