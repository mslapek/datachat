import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import {Navbar} from "react-bootstrap";
import {Questions, QuestionsForm} from "./features/questions/Questions.tsx";
import {Schema} from "./features/schema/Schema.tsx";


function App() {
  return (
    <>
        <div className="container-fluid">
            <Navbar className="bg-body-tertiary p-2 my-1 rounded">
                <Navbar.Brand>
                    &nbsp;
                    <i className="bi-database" style={{color: "darkorchid"}}></i>
                    <i className="bi-chat-dots-fill" style={{color: "cornflowerblue"}}></i>
                    &nbsp;
                    DataChat <small><a href="https://github.com/mslapek" target="_blank" className="text-decoration-none text-black">by Michał Słapek</a></small>
                </Navbar.Brand>
            </Navbar>
        </div>
        <div className="container-fluid">
            <Schema/>
            <Questions/>
        </div>
        <QuestionsForm/>
    </>
  )
}



export default App
