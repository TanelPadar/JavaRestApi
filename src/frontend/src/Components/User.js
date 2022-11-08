import React, { useState } from 'react';
import axios from "axios";
import './users.css'
import Modal from 'react-bootstrap/Modal';
import Button from 'react-bootstrap/Button';

function EditModal({user}) {
    const [show, setShow] = useState(false);
    const handleClose = () => setShow(false);
    const handleShow = () => setShow(true);
    const [fname, setFname] = useState('');
    const [lname, setLname] = useState('');
    const [email, setEmail] = useState('');
    const handleChange = event => {

        switch(event.target.id) {
            case "fname":
                setFname(event.target.value);
                break;
            case "lname":
                setLname(event.target.value);
                break;
            case "email":
                setEmail(event.target.value);
                break;
            default:
                break
        }


    };

        const saveUser = () => {

        const savingUser = {
            "user_id":user.user_id,
            "name":fname,
            "surname":lname,
            "email": email
        }
        axios.put('http://localhost:8080/users', savingUser)
            .then(res => {
                window.location.reload();
            })
    }


    return (
        <>
            <Button className="btn-dark" onClick={handleShow}>
                Edit
            </Button>

            <Modal show={show} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Edit User</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <form>
                        <label htmlFor="fname" >First name:</label>
                        <input type="text" id="fname" name="fname"  value={fname} onChange={handleChange} placeholder={user.name}></input><br></br>
                        <label htmlFor="lname" >Last name:</label>
                        <input type="text" id="lname" name="lname" value={lname} onChange={handleChange} placeholder={user.surname}></input><br></br>
                        <label htmlFor="email" >email:</label>
                        <input type="text" id="email" name="email" value={email} onChange={handleChange} placeholder={user.email} ></input>
                    </form>
                </Modal.Body>

                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose}>
                        Close
                    </Button>
                    <Button variant="primary" onClick={saveUser}>
                        Save Changes
                    </Button>
                </Modal.Footer>
            </Modal>

        </>
    );
}



function AddUserModal() {
    const [show, setShow] = useState(false);
    const handleClose = () => setShow(false);
    const handleShow = () => setShow(true);

    const [fname, setFname] = useState('');
    const [lname, setLname] = useState('');
    const [email, setEmail] = useState('');
    const handleChange = event => {

        switch(event.target.id) {
            case "fname":
                setFname(event.target.value);
                break;
            case "lname":
                setLname(event.target.value);
                break;
            case "email":
                setEmail(event.target.value);
                break;
            default:
                break
        }


    };

    const addUser = () => {
        const savingUser = {
            "name":fname,
            "surname":lname,
            "email": email
        }
        axios.post('http://localhost:8080/users', savingUser)
            .then(res => {
                window.location.reload();
            })
    }


    return (
        <>
            <Button className="btn-info-mx-5" onClick={handleShow}>
                Add
            </Button>

            <Modal show={show} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Add user</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <form>
                        <label htmlFor="fname" >First name:</label>
                        <input type="text" id="fname" name="fname"  value={fname} onChange={handleChange} ></input><br></br>
                        <label htmlFor="lname" >Last name:</label>
                        <input type="text" id="lname" name="lname" value={lname} onChange={handleChange}></input><br></br>
                        <label htmlFor="email" >email:</label>
                        <input type="text" id="email" name="email" value={email} onChange={handleChange} ></input>
                    </form>
                </Modal.Body>

                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose}>
                        Close
                    </Button>
                    <Button variant="primary" onClick={addUser}>
                        Add user
                    </Button>
                </Modal.Footer>
            </Modal>

        </>
    );
}


export default class User extends React.Component{
    state = {
        users: []
    }


    componentDidMount() {
        axios.get('http://localhost:8080/users')
            .then(res => {
                const users = res.data
                this.setState({users})
            })
    }


    addUser() {

        axios.post('http://localhost:8080/users')
            .then(() => {

            })
    }

    deleteUser(user_id) {

        axios.delete('http://localhost:8080/users/delete/' + user_id)
            .then(() => {
                let users = this.state.users
                users = users.filter(user => user.user_id !== user_id)
                this.setState({users:users})
            })

    }

    render() {

        return (
            <ul >
                {
                    this.state.users
                        .map(user => {
                            return (
                                < >
                                    <div class="userList">
                                        <li key={user.user_id}><b>Eesnimi:</b> {user.name}  <b>Perekonnanimi:</b> {user.surname} <b>email:</b> {user.email}</li>
                                        <div className="buttonsForUser">
                                            <EditModal user={user} />
                                            <button class="btn-danger btn btn-primay mx-2 " onClick={()=>this.deleteUser(user.user_id)}>Remove</button>
                                            <AddUserModal />
                                        </div>

                                    </div>
                                </>)

                        })}



            </ul>


        )
    }
}