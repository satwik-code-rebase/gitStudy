import axios from 'axios';
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom';

export default function  () {
    const nagivate=useNavigate();
    const [userName,setUserName]=useState("");
    const [email,setEmail]=useState("");
    const [password,setPassword]=useState("");
    const registerUser=()=>{
       const data={
        userName,
        email,
        password
       }
       axios.post("http://localhost:8080/registration/addUser",data).then(res=>{
        nagivate("/");
       })
    }
  return (
    <div>
    <h1>Registration Form</h1>
    <div>
    User Name:<input type="text" value={userName} onChange={e=>{setUserName(e.target.value)}}/>
    Email:<input type="text" value={email} onChange={e=>{setEmail(e.target.value)}}/>
    Password:<input type="text" value={password} onChange={e=>{setPassword(e.target.value)}}/>
    <button onClick={registerUser}>Create</button>
    </div>
     </div>
  )
}
