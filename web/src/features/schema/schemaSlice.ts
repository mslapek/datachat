import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../../app/store"

export interface Column {
    name: string,
    type: string
}

export interface Table {
    name: string,
    columns: Column[]
}

export interface DbSchema {
    tables: Table[] | null,
    error: string | null,
    status: "ready" | "loading" | "error"
}

const initialState: DbSchema = {
    tables: null,
    error: null,
    status: "loading"
}

export const schemaSlice = createSlice({
    name: "schema",
    initialState,
    // The `reducers` field lets us define reducers and generate associated actions
    reducers: {
        setSchema: (_, action: PayloadAction<DbSchema>) => {
            return action.payload
        }
    },
})

export const {setSchema} = schemaSlice.actions

export const selectSchema = (state: RootState) => state.schema

export default schemaSlice.reducer
