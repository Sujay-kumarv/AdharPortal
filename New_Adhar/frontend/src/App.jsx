import { useState, useEffect } from 'react'
import './index.css'

function App() {
  const [backendStatus, setBackendStatus] = useState('Checking...');

  useEffect(() => {
    fetch('http://127.0.0.1:8080/api/test')
      .then(response => {
        if (response.ok) {
          setBackendStatus('Connected');
        } else {
          setBackendStatus('Error: ' + response.statusText);
        }
      })
      .catch(error => {
        setBackendStatus('Error: ' + error.message);
      });
  }, []);

  return (
    <div className="app-container" style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      height: '100vh',
      flexDirection: 'column',
      gap: '2rem'
    }}>
      <div style={{ textAlign: 'center' }}>
        <h1 style={{
          fontSize: '4rem',
          background: 'linear-gradient(45deg, var(--primary-color), var(--secondary-color))',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          filter: 'drop-shadow(0 0 10px rgba(0, 243, 255, 0.3))',
          marginBottom: '0.5rem'
        }}>
          New Adhar
        </h1>
        <p style={{ fontSize: '1.2rem', opacity: 0.8 }}>New Adhar Portal by Sujay Kumar</p>
      </div>

      <div style={{
        padding: '2rem',
        background: 'var(--glass-bg)',
        border: '1px solid var(--glass-border)',
        borderRadius: '16px',
        backdropFilter: 'blur(10px)',
        boxShadow: 'var(--glass-shadow)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
        minWidth: '300px',
        textAlign: 'center'
      }}>
        <p style={{ marginBottom: '1rem' }}>Backend Status: <span style={{ color: backendStatus === 'Connected' ? '#00ff88' : '#ff0055', fontWeight: 'bold' }}>{backendStatus}</span></p>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <a href="/registration.html" style={{
            textDecoration: 'none',
            padding: '10px 20px',
            background: 'var(--primary-color)',
            color: '#000',
            fontWeight: 'bold',
            borderRadius: '8px',
            transition: '0.3s',
            border: '1px solid var(--primary-color)'
          }}
            onMouseOver={(e) => {
              e.target.style.background = 'transparent';
              e.target.style.color = 'var(--primary-color)';
              e.target.style.boxShadow = '0 0 15px var(--primary-color)';
            }}
            onMouseOut={(e) => {
              e.target.style.background = 'var(--primary-color)';
              e.target.style.color = '#000';
              e.target.style.boxShadow = 'none';
            }}>
            Citizen Registration
          </a>

          <a href="/admin_login.html" style={{
            textDecoration: 'none',
            padding: '10px 20px',
            background: 'transparent',
            color: 'var(--secondary-color)',
            fontWeight: 'bold',
            borderRadius: '8px',
            transition: '0.3s',
            border: '1px solid var(--secondary-color)'
          }}
            onMouseOver={(e) => {
              e.target.style.background = 'var(--secondary-color)';
              e.target.style.color = '#000';
              e.target.style.boxShadow = '0 0 15px var(--secondary-color)';
            }}
            onMouseOut={(e) => {
              e.target.style.background = 'transparent';
              e.target.style.color = 'var(--secondary-color)';
              e.target.style.boxShadow = 'none';
            }}>
            Admin Dashboard
          </a>
        </div>
      </div>
    </div>
  )
}

export default App
