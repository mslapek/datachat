import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../../app/store"
import {sendAddQuestion} from "../api/api.ts";

export interface QueryResult {
    columnNames: string[]
    rows: string[][]
}

export interface Question {
    id: number
    sqlQuery: string
    queryResult: QueryResult | null
    error: string | null,
    status: "ready" | "loading" | "error"
}

export interface AllQuestions {
    messages: Question[] | null,
    error: string | null,
    status: "ready" | "loading" | "error"
}

const initialState: AllQuestions = {
    messages: null,
    error: null,
    status: "loading"
}

export function isBusy(s: AllQuestions): boolean {
    if (s.status !== "ready") {
        return true
    }
    let msgs: Question[] = s.messages!
    if (msgs.length == 0) {
        return false
    }
    let last = msgs[msgs.length - 1]
    return last.queryResult === null && last.error === null
}

export const questionsSlice = createSlice({
    name: "questions",
    initialState,
    // The `reducers` field lets us define reducers and generate associated actions
    reducers: {
        addQuestion: (state, action: PayloadAction<string>) => {
            if (isBusy(state)) {
                return // ignore
            }

            let nid = state.messages!.length
            let q: Question = {
                id: nid,
                sqlQuery: action.payload,
                queryResult: null,
                error: null,
                status: "loading"
            }

            state.messages!.push(q)
            sendAddQuestion(q)
        },
        setQuestionResult: (state, action: PayloadAction<Question>) => {
            let q = action.payload
            let msgs = state.messages!
            msgs[q.id] = q
        },
        setAllQuestions: (_, action: PayloadAction<AllQuestions>) => {
            return action.payload
        }
    },
})

export const {
    addQuestion, setAllQuestions
} = questionsSlice.actions

export const selectQuestions = (state: RootState) => state.chat

export default questionsSlice.reducer
