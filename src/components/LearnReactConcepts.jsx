import React,{useState} from 'react';
// import {useState} from 'react'
// import './styles.css';

// don't change the Component name "App"
export default function LearnReactConcept() {
    const [message,setMessage]=useState("");
    const checkValid=(e)=>{
        if((e.target.value).length>=3){
            setMessage("Valid message")
        }
        else setMessage("Invalid message");
    }
    return (
        <div>
            <label>Your message is here</label>
            <input type="text" onChange={checkValid}/>
            <p>{message}</p>
        </div>
    );
}