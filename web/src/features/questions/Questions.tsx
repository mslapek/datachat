import {useAppDispatch, useAppSelector} from "../../app/hooks";
import {addQuestion, AllQuestions, isBusy, QueryResult, selectQuestions} from "./questionsSlice.ts";
import {Button, Form, Spinner} from "react-bootstrap";
import {useState} from "react";


function renderQueryResult(q: QueryResult) {
    return <div className="bg-primary rounded py-2 px-4 text-white overflow-x-auto">
        Response from database:

        <table className="table table-striped table-hover">
            <thead>
            <tr>
                {q.columnNames.map((c, i) => <th key={i}>{c}</th>)}
            </tr>
            </thead>
            <tbody>
            {q.rows.map((r, i) => <tr key={i}>
                {r.map((c, i) => <td key={i}>{c}</td>)}
            </tr>)}
            </tbody>
        </table>
    </div>;
}

function renderQuestions(allQuestions: AllQuestions) {
    return <div>
        {allQuestions.messages!.map((q, _) => {
            let responseContent
            switch (q.status) {
                case "loading":
                    responseContent = <Spinner animation="border" variant="primary"/>
                    break
                case "ready":
                    responseContent = renderQueryResult(q.queryResult!)
                    break
                default:
                    responseContent = <div className="bg-danger rounded py-2 px-4 text-white">
                        {q.error || "Unknown error"}
                    </div>
            }

            return <div key={q.id}>
                <div className="row mx-4 mt-5">
                    <div className="col-sm-4"></div>
                    <div className="col-sm-8">
                        <div className="bg-secondary rounded py-2 px-4 text-white">
                            <pre>{q.sqlQuery}</pre>
                        </div>
                    </div>
                </div>
                <div className="row mx-4 mt-1">
                    <div className="col-sm-8">
                        {responseContent}
                    </div>
                </div>
            </div>
        })}
    </div>
}

export function Questions() {
    const allQuestions = useAppSelector(selectQuestions)
    switch (allQuestions.status) {
        case "ready":
            return renderQuestions(allQuestions)
        case "loading":
            return <Spinner animation="border" variant="primary"/>
        default:
            return <div className="bg-danger rounded py-2 px-4 text-white">
                {allQuestions.error || "Unknown error"}
            </div>
    }
}

export function QuestionsForm() {
    const allQuestions = useAppSelector(selectQuestions)
    const dispatch = useAppDispatch()
    const [text, setText] = useState("")

    let disabled = isBusy(allQuestions)

    function onSend() {
        dispatch(addQuestion(text))
        setText("")
    }

    return <div className="container mb-4">
        <hr/>
        <h3>Query</h3>
        <Form>
            <Form.Check
                type="radio"
                label="Natural Text"
                name="formHorizontalRadios"
                id="formHorizontalRadios1"
                className="m-2 form-check-inline"
                disabled={true}
            />
            <Form.Check
                type="radio"
                label="SQL Query"
                name="formHorizontalRadios"
                id="formHorizontalRadios2"
                className="m-2 form-check-inline"
                disabled={true}
                checked={true}
            />
            <Form.Group className="mb-3" controlId="form">
                <Form.Control
                    as="textarea"
                    rows={6}
                    placeholder="Enter query"
                    value={text}
                    disabled={disabled}
                    onChange={(e) => setText(e.target.value)}
                    className="font-monospace"
                />
            </Form.Group>
            <Button variant="primary" onClick={onSend} disabled={disabled}>
                <i className="bi-send"></i>
                &nbsp;
                Submit
            </Button>
        </Form>
    </div>
}