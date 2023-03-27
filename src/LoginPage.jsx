import axios from 'axios';
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom';

export default function () {
    const nagivate=useNavigate();
    const [email,setEmail]=useState("");
    const [password,setPassword]=useState("");
    const [message,setMassage]=useState("");
    const handleLogin=()=>{
        const data={
            email,
            password
        }
        axios.post("http://localhost:8080/login/checkLogin",data).then(res=>{
            if(res.data.id){
                nagivate("/revenue");
            }
           
        }).catch(error=>{
             setMassage("Invalid cradentials");
        })
    }

  return (
    <div>
    {message.length>1?<h1>{message}</h1>:
    <div>
        <h1>Login Page</h1>
        Email:<input type="text" value={email} onChange={e=>{setEmail(e.target.value)}}/>
        Password:<input type="text" value={password} onChange={e=>{setPassword(e.target.value)}}/>
        <button onClick={handleLogin}>Submit</button>
     </div>
    }
    </div>
  )
}
