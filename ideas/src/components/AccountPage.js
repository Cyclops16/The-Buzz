// AccountPage.js
import React, { useState, useEffect } from 'react';
import './AccountPage.css';

const AccountPage = () => {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState(null);
  // Track which field is currently being edited and its draft value
  const [editingField, setEditingField] = useState(null);
  const [draftValue, setDraftValue] = useState('');

  useEffect(() => {
    fetch('https://cse216-sp25-team-02.onrender.com/profile', {
      credentials: 'include',
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(json => {
        if (json.status === 'ok' && json.data) {
          setProfile(json.data);
        } else {
          throw new Error('Unexpected API response');
        }
      })
      .catch(err => {
        console.error('Error loading profile:', err);
        setError('Failed to load profile.');
      });
  }, []);

  if (error) {
    return (
      <div className="account-page">
        <p className="error">{error}</p>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="account-page">
        <p>Loading profile…</p>
      </div>
    );
  }

  // Begin editing a given field
  const startEdit = (fieldKey) => {
    setEditingField(fieldKey);
    setDraftValue(profile[fieldKey] || '');
  };

  const cancelEdit = () => {
    setEditingField(null);
    setDraftValue('');
  };

  // Save updated field to backend and state
  const saveEdit = () => {
    const payload = { [editingField]: draftValue };
    fetch('https://cse216-sp25-team-02.onrender.com/profile', {
      method: 'PUT',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(json => {
        if (json.status === 'ok' && json.data) {
          setProfile(json.data);
          cancelEdit();
        } else {
          throw new Error('Unexpected API response');
        }
      })
      .catch(err => console.error('Error updating profile:', err));
  };

  // Helper to render a field with inline edit
  const renderField = (label, fieldKey, placeholder = 'Not provided') => (
    <p>
      <strong>{label}:</strong>{' '}
      {editingField === fieldKey ? (
        <>
          <input
            type="text"
            value={draftValue}
            onChange={e => setDraftValue(e.target.value)}
            placeholder={placeholder}
          />
          <button onClick={saveEdit}>Save</button>
          <button onClick={cancelEdit}>Cancel</button>
        </>
      ) : (
        <span onClick={() => startEdit(fieldKey)} className="editable-field">
          {profile[fieldKey] || placeholder}
        </span>
      )}
    </p>
  );

  return (
    <div className="account-page">
      <h2>My Account</h2>
      <div className="profile-info">
        {/* Name (not editable here) */}
        {(profile.first_name || profile.last_name) && (
          <p>
            <strong>Name:</strong> {profile.first_name} {profile.last_name}
          </p>
        )}

        {/* Email (not editable here) */}
        <p>
          <strong>Email:</strong> {profile.email || 'Not provided'}
        </p>

        {/* Editable fields */}
        {renderField('Gender Identity', 'gender_identity')}
        {renderField('Sexual Orientation', 'sexual_orientation')}
        {renderField('Note', 'note')}
      </div>
    </div>
  );
};

export default AccountPage;
