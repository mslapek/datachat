import {useAppSelector} from "../../app/hooks";
import {DbSchema, selectSchema} from "./schemaSlice.ts";
import {Spinner} from "react-bootstrap";
import {Fragment} from "react";

function renderSchema(schema: DbSchema) {
    let tables = schema.tables!.map((t, _) => {
        let columns = t.columns.map((c, _) => {
            return <tr key={c.name}>
                <td>{c.name}</td>
                <td>{c.type}</td>
            </tr>
        })
        return <Fragment key={"schema-" + t.name}>
            <h4>{t.name}</h4>
            <table className="table table-striped">
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Type</th>
                </tr>
                </thead>
                <tbody>
                {columns}
                </tbody>
            </table>
        </Fragment>
    })

    if (tables.length === 0) {
        return <div className="bg-primary rounded py-2 px-4 text-white" key={"schema"}>
            Hello! The schema of the connected database is empty.

            Please create some tables with columns.
        </div>
    }

    return <div className="bg-primary rounded py-2 px-4 text-white" key={"schema"}>
        Hello! The schema of the connected database is:

        {tables}

        Ask queries to the database using the form below.
    </div>
}

function renderError(schema: DbSchema) {
    return <div className="bg-danger rounded py-2 px-4 text-white">
        Hello! Cannot retrieve schema: {schema.error || "Unknown error"}
    </div>
}

function renderLoading() {
    return <Spinner animation="border" variant="primary" key={"schema"}/>;
}

export function Schema() {
    let schema = useAppSelector(selectSchema)
    let schemaContent

    switch (schema.status) {
        case "ready":
            schemaContent = renderSchema(schema);
            break;
        case "loading":
            schemaContent = renderLoading();
            break;
        default:
            schemaContent = renderError(schema);
    }

    return <div className="row m-4">
        <div className="col-sm-8">
            {schemaContent}
        </div>
    </div>
}