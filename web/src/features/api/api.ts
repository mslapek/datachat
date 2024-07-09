import { DbSchema } from "../schema/schemaSlice.ts";
import { AllQuestions, Question } from "../questions/questionsSlice.ts";
import { store } from "../../app/store.ts";
import { ClientMessage } from "./proto/wsclient.ts"
import { AllQuestionsMessage, DatabaseSchemaMessage, QuestionResultMessage, QuestionState, ServerMessage } from "./proto/wsserver.ts";

function getDatabaseSchemaAction(_: ServerMessage, data: DatabaseSchemaMessage) {
    let payload: DbSchema

    if (data.error != null) {
        payload = {
            "tables": null,
            "error": data.error,
            "status": "error"
        }
    } else if (data.databaseSchema != null) {
        const schema = data.databaseSchema
        payload = {
            "tables": schema.tables,
            "error": null,
            "status": "ready"
        }
    } else {
        throw new Error("Unknown message type")
    }

    return {
        "type": "schema/setSchema",
        "payload": payload
    };
}

function parseQuestionState(id: number, msg: QuestionState): Question {
    let r: Question
    if (msg.error != null) {
        r = {
            "id": id,
            "status": "error",
            "error": msg.error,
            "sqlQuery": msg.sqlQuery,
            "queryResult": null,
        }
    } else if (msg.pending != null) {
        r = {
            "id": id,
            "status": "loading",
            "error": null,
            "sqlQuery": msg.sqlQuery,
            "queryResult": null,
        }
    } else if (msg.done != null) {
        const tr = msg.done
        const tb = {
            "columnNames": tr.columnNames,
            "rows": tr.rows.map((r) => r.values),
        }
        r = {
            "id": id,
            "status": "ready",
            "error": null,
            "sqlQuery": msg.sqlQuery,
            "queryResult": tb,
        }
    } else {
        throw new Error("Unknown message type")
    }
    return r
}

function getAllQuestionsAction(_: ServerMessage, data: AllQuestionsMessage) {
    let payload: AllQuestions

    if (data.error != null) {
        payload = {
            "messages": null,
            "error": data.error,
            "status": "error"
        }
    } else if (data.questions != null) {
        const questions = data.questions.questions
        let q: Question[] = questions.map((msg, i) => { return parseQuestionState(i, msg) })
        payload = {
            "messages": q,
            "error": null,
            "status": "ready"
        }
    } else {
        throw new Error("Unknown message type")
    }

    return {
        "type": "questions/setAllQuestions",
        "payload": payload
    }
}


function getQuestionResultAction(_: ServerMessage, data: QuestionResultMessage) {
    if (data.state == null) {
        throw new Error("Expected a result")
    }

    let payload: Question = parseQuestionState(data.questionId, data.state)
    return {
        "type": "questions/setQuestionResult",
        "payload": payload
    }
}

const socket = new WebSocket("ws://localhost:8080/chat-ws");

// add support for multiple chats in a future
let chatId = "12340000-0000-0000-0000-000000000000";

socket.addEventListener("open", (_) => {
    const msg: ClientMessage = {
        "chatId": { "chatId": chatId },
        "startChat": {}
    }
    socket.send(ClientMessage.encode(msg).finish());
});

socket.addEventListener("message", async (event) => {
    const b = new Uint8Array(await event.data.arrayBuffer());
    const msg = ServerMessage.decode(b);

    if (msg.chatId?.chatId != chatId) {
        return;
    }

    let action;
    if (msg.databaseSchema != null) {
        action = getDatabaseSchemaAction(msg, msg.databaseSchema)
    } else if (msg.allQuestions != null) {
        action = getAllQuestionsAction(msg, msg.allQuestions)
    } else if (msg.questionResult != null) {
        action = getQuestionResultAction(msg, msg.questionResult)
    } else {
        throw new Error("Unknown message type")
    }

    store.dispatch(action)
});

export function sendAddQuestion(q: Question) {
    const msg: ClientMessage = {
        "chatId": { "chatId": chatId },
        "addQuestion": {
            "questionId": q.id,
            "sqlQuery": q.sqlQuery
        }
    }

    socket.send(ClientMessage.encode(msg).finish());
}