import React from 'react'
import { useEffect, useState } from "react";
import axios from "axios";

export default function Revenue() {
    const [revenueData,setRevenueData]=useState([]);
    const[errorMessage,setErrorMessage]=useState("");
  useEffect(()=>{
axios.get("http://localhost:8080/revenue/getAllRevenues").then(response=>{
  console.log(response.data);
  setRevenueData(response.data);
}).catch(error=>{
    setErrorMessage("Current no Record Present");
    if(error.response.status===404){
        setErrorMessage("hello -------------------------");
    }
    console.log(error);
});
  },[])
  return (
    <div>
        {errorMessage.length==0 ? <h1>data is preaent</h1>:<h1>{errorMessage}</h1>}
    </div>
  )
}
