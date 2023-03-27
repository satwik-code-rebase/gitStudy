import logo from "./logo.svg";
import "./App.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import Revenue from "./components/Revenue";
import { useEffect, useState } from "react";
import HomePage from "./HomePage";
import {BrowserRouter,Routes,Route} from "react-router-dom";
import RegistrationPage from "./RegistrationPage";
import LoginPage from "./LoginPage";
import LearnReactConcepts from "./components/LearnReactConcepts";

function App() {
  
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage/>}/>
        <Route path="/registraionForm" element={<RegistrationPage/>}/>
        <Route path="/loginPage" element={<LoginPage/>}/>
        <Route path="/revenue" element={<Revenue/>}/>
        <Route path="/learn" element={<LearnReactConcepts/>}/>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
