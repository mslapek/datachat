import { configureStore, ThunkAction, Action } from "@reduxjs/toolkit"
import chatReducer from "../features/questions/questionsSlice.ts"
import schemaReducer from "../features/schema/schemaSlice.ts"

export const store = configureStore({
    reducer: {
        chat: chatReducer,
        schema: schemaReducer,
    },
})

export type AppDispatch = typeof store.dispatch
export type RootState = ReturnType<typeof store.getState>
export type AppThunk<ReturnType = void> = ThunkAction<
    ReturnType,
    RootState,
    unknown,
    Action<string>
>
