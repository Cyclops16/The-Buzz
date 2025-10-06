import React, { useState, useEffect } from 'react';
import './PostMessage.css';

function PostMessage({ showForm, credential }) {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [link,setLink] = useState('');
  const [posts, setPosts] = useState([]);
  const [file, setFile] = useState(null);
  const [commentFile, setCommentFile] = useState({});
  const fileInputRef = React.useRef();

  // State for editing
  const [editingPostId, setEditingPostId] = useState(null);
  const [editingTitle, setEditingTitle] = useState('');
  const [editingMessage, setEditingMessage] = useState('');


  // State for comments:
  const [commentVisibility, setCommentVisibility] = useState({});
  const [comments, setComments] = useState({});
  const [newComment, setNewComment] = useState({});

  // State for editing comments:
  // This object will store { postId, commentId, text } for the currently edited comment
  const [editingComment, setEditingComment] = useState(null);

  //stata for profile window pop up
  const [hoveredUser, setHoveredUser] = useState(null);
  const[hoveredUserLastName, setHoveredUserLastName] = useState(null);
  const[hoveredUserEmail, setHoveredUserEmail] = useState(null);
  const[hoveredUserNotes, setHoveredUserNotes] = useState(null);
  const [popupPosition, setPopupPosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    fetch(`https://cse216-sp25-team-02.onrender.com/ideas`, {
      credentials: 'include',
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(async data => {
        const postsWithUsernames = await Promise.all(
          data.data.map(async (post) => {
            try {
              const userRes = await fetch(`https://cse216-sp25-team-02.onrender.com/profile/${post.user_id}`);
              const userData = await userRes.json();
  
              // Adjust this depending on what your profile data looks like
              const username = userData.data?.first_name || "Unknown first name";
              const userLastName = userData.data?.last_name || "Unknown last name";
              const userEmail = userData.data?.email || "Unknown email";
              const userNotes = userData.data?.note || "no notes";
  
              return {
                id: post.id,
                userID: post.user_id, 
                userName: username,
                userLastName: userLastName,
                userEmail: userEmail,
                userNotes: userNotes,
                title: post.subject,
                fileLink:post.attachment_url,
                link: post.link_url,
                message: post.body,
                likes: post.like_count,
                dislikes: post.dislike_count,
                liked: false,
                disliked: false,
              };
            } catch (error) {
              console.error(`Error fetching user ${post.user_id}:`, error);
              return {
                id: post.id,
                userID: post.user_id,
                userName: "Unknown User",
                title: post.subject,
                fileLink:post.attachment_url,
                link: post.link_url,
                message: post.body,
                likes: post.like_count,
                dislikes: post.dislike_count,
                liked: false,
                disliked: false,
              };
            }
          })
        );
  
        setPosts(postsWithUsernames);
      })
      .catch(err => console.error("Failed to load posts:", err));
  }, [credential]);
  
  

  const handleTitleChange = (e) => setTitle(e.target.value);
  const handleMessageChange = (e) => setMessage(e.target.value);
  const handleLinkChange = (e) => setLink(e.target.value);

  const handleLikeToggle = (index) => {
    const newPosts = [...posts]; // Make a copy of the current posts array
    const post = newPosts[index];
  
    // Toggle the 'liked' status
    post.liked = !post.liked;
    post.likes = post.liked ? post.likes + 1 : post.likes - 1;
    setPosts(newPosts);
  
    // Send the updated like count to the backend
    fetch(`https://cse216-sp25-team-02.onrender.com/vote/${post.id}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${credential}`
      },
      body: JSON.stringify({
        direction: "up",
      }),
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error, status: ${response.status}`);
        }
      })
      .catch(error => {
        console.error('Error updating like count:', error);
      });
  };

  const handleDislikeToggle = (index) => {
    const newPosts = [...posts]; // Make a copy of the current posts array
    const post = newPosts[index];
  
    // Toggle the 'liked' status
    post.disliked = !post.disliked;
    post.dislikes = post.disliked ? post.dislikes + 1 : post.dislikes - 1;
    setPosts(newPosts);
  
    // Send the updated like count to the backend
    fetch(`https://cse216-sp25-team-02.onrender.com/vote/${post.id}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${credential}`
      },
      body: JSON.stringify({
        direction: "down",
      }),
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error, status: ${response.status}`);
        }
      })
      .catch(error => {
        console.error('Error updating like count:', error);
      });
  };
  

  // Handle the submission of a new post //
  const handlePostMessage = (e) => {
    e.preventDefault();
  
    if (!title.trim() || !message.trim()) {
      alert('Please fill out both the title and message fields!');
      return;
    }
  
    const formData = new FormData();
    formData.append("title", title);
    formData.append("message", message);
    formData.append("link", link);
    if (file) {
      formData.append("file", file);
    }
  
    fetch("https://cse216-sp25-team-02.onrender.com/ideas", {
      method: "POST",
      credentials: "include",
      headers: {
        "Authorization": `Bearer ${credential}`
        // Do NOT manually set 'Content-Type' when using FormData
      },
      body: formData,
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === "ok") {
          const formattedPost = {
            id: data.id,
            title: title,
            message: message,
            likes: 0,
            dislikes: 0,
            liked: false,
            disliked: false,
          };
          setPosts([formattedPost, ...posts]);
          setTitle('');
          setMessage('');
          setLink('');
          setFile(null);
          fileInputRef.current.value = null;
        } else {
          console.error("Failed to post:", data.message);
        }
      })
      .catch(err => console.error("Error posting:", err));
  };
  
  
  
  // Handle the deletion of a post
  const handleDeletePost = (id) => {
    console.log(id);
    fetch(`https://cse216-sp25-team-02.onrender.com/ideas/${id}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${credential}`
      },
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`Error deleting post, status: ${response.status}`);
        }
        setPosts(posts.filter(post => post.id !== id)); // Remove from frontend after successful delete
      })
      .catch(error => console.error('Error:', error));
  };

  const handleEditClick = (post) => {
    setEditingPostId(post.id);
    setEditingTitle(post.title);
    setEditingMessage(post.message);
  };

  const handleEditSubmit = (e) => {
    e.preventDefault();
    // Change keys to match backend expectations.
    const updatedPost = {
      title: editingTitle,
      message: editingMessage,
    };
  
    fetch(`https://cse216-sp25-team-02.onrender.com/ideas/${editingPostId}`, {
      method: 'PUT', // or 'PATCH' if required
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${credential}`
      },
      body: JSON.stringify(updatedPost),
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error, status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        // Map the response to your frontend state
        const formattedPost = {
          id: data.id,
          title: data.subject,
          message: data.body,
          likes: data.like_count,
          dislikes: data.dislike_count,
          liked: false,
          disliked: false,
        };
  
        setPosts(posts.map(post => post.id === editingPostId ? formattedPost : post));
        setEditingPostId(null);
      })
      .catch(error => {
        console.error('Error updating post:', error);
      });
  };

  const toggleCommentSection = (postId) => {
    setCommentVisibility(prev => {
      const willBeVisible = !prev[postId];
  
      if (willBeVisible) {
        fetch(`https://cse216-sp25-team-02.onrender.com/comments/${postId}`, {
          credentials: 'include',
          headers: {
            'Authorization': `Bearer ${credential}`
          }
        })
          .then(res => {
            if (!res.ok) throw new Error(`Comments fetch failed: ${res.status}`);
            return res.json();
          })
          .then(async data => {
            if (data.status === "ok" && Array.isArray(data.data)) {
              const formatted = await Promise.all(
                data.data.map(async (c) => {
                  try {
                    const profileRes = await fetch(`https://cse216-sp25-team-02.onrender.com/profile/${c.user_id}`);
                    const profileData = await profileRes.json();
                    return {
                      id:     c.id,
                      body:   c.body,
                      userId: c.user_id,
                      msgId:  c.msg_id,
                      commentFile: c.attachment_url,
                      userName: profileData.data?.first_name || "Unknown",
                      userLastName: profileData.data?.last_name || "Unknown",
                      userEmail: profileData.data?.email || "Unknown",
                      userNotes: profileData.data?.note || "Unknown",
                    };
                  } catch (err) {
                    console.error(`Failed to fetch user ${c.user_id}`, err);
                    return {
                      id:     c.id,
                      body:   c.body,
                      commentFile: c.attachment_url,
                      userId: c.user_id,
                      msgId:  c.msg_id,
                      userName: "Unknown",
                    };
                  }
                })
              );
  
              setComments(prevComments => ({
                ...prevComments,
                [postId]: formatted
              }));
            } else {
              console.error("Unexpected comments payload:", data);
            }
          })
          .catch(err => console.error("Failed loading comments:", err));
      }
  
      return {
        ...prev,
        [postId]: willBeVisible
      };
    });
  };
  



  // Update the new comment text field for a post
  const handleNewCommentChange = (postId, value) => {
    setNewComment(prev => ({ ...prev, [postId]: value }));
  };


  // https://team-02.dokku.cse.lehigh.edu
  // https://cse216-sp25-team-02.onrender.com

  // Post a new comment for a given post id
  const handlePostComment = (postId) => {
    const commentText = newComment[postId];
    const file = commentFile?.[postId]; // Assuming you're tracking files per post
  
    if (commentText && commentText.trim()) {
      const formData = new FormData();
      formData.append("body", commentText);
      if (file) {
        formData.append("file", file);
      }
  
      fetch(`https://cse216-sp25-team-02.onrender.com/comments/${postId}`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Authorization': `Bearer ${credential}`
          // DO NOT set 'Content-Type' with FormData
        },
        body: formData
      })
        .then(response => response.json())
        .then(data => {
          if (data.status === "ok") {
            // Format the new comment (assuming backend sends `id`, `body`, maybe `attachment_url`)
            const formattedComment = {
              user_id: "You", // or get from session if available
              id: data.id,
              body: commentText,
              msg_id: postId,
              attachment_url: file ? URL.createObjectURL(file) : null,
            };
  
            setComments(prevComments => ({
              ...prevComments,
              [postId]: prevComments[postId] ? [formattedComment, ...prevComments[postId]] : [formattedComment]
            }));
            setNewComment(prev => ({ ...prev, [postId]: '' }));
            setCommentFile(prev => ({ ...prev, [postId]: null }));
          } else {
            console.error("Unexpected response from posting comment:", data);
          }
        })
        .catch(error => console.error("Error posting comment:", error));
    } else {
      alert("Please fill out the comment field!");
    }
  };
  


// Submit the edited comment to the backend.
  // We assume a PUT endpoint at /comments/{postId}/{commentId} that accepts a payload with mMessage.
  const handleEditCommentSubmit = (postId, commentId) => {
    const updatedCommentPayload = {
      body: editingComment.text
    };
    fetch(`https://cse216-sp25-team-02.onrender.com/comments/${commentId}`, {
      method: 'PUT',
      credentials: 'include',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${credential}` 
      },
      body: JSON.stringify(updatedCommentPayload)
    })
      .then(response => {
        if (!response.ok) throw new Error(`HTTP error, status: ${response.status}`);
        return response.json();
      })
      .then(data => {
        if (data.mStatus === "ok" && Array.isArray(data.mData) && data.mData.length > 0) {
          // The backend returns the updated comment in mData
          const updatedComment = data.mData[0];
          setComments(prevComments => ({
            ...prevComments,
            [postId]: prevComments[postId].map(c => c.id === commentId ? updatedComment : c)
          }));
          setEditingComment(null);
        } else {
          console.error("Unexpected response from editing comment:", data);
        }
      })
      .catch(error => console.error("Error editing comment:", error));
  };

    // When the edit button for a comment is clicked, set the editingComment state.
    const handleEditCommentClick = (postId, comment) => {
      setEditingComment({
        postId: postId,
        commentId: comment.id,
        text: comment.body,
      });
    };

    // Handle changes in the comment edit input field.
  const handleEditingCommentChange = (e) => {
    setEditingComment(prev => ({ ...prev, text: e.target.value }));
  };


  // The post form appears only if showForm is true
  return (
    <div>
      {showForm && (
        <div className="post-message-container">
          <h2>Post a Message</h2>
          <form onSubmit={handlePostMessage}>
            <input
              type="text"
              value={title}
              onChange={handleTitleChange}
              placeholder="Post Title"
              className="input-title"
            />
            <textarea
              value={message}
              onChange={handleMessageChange}
              placeholder="Type your message..."
              rows="4"
              cols="50"
            />
            <input
              type="text"
              value={link}
              onChange={handleLinkChange}
              placeholder="add links here"
              className="input-title"
            />
            
            <div className="file-upload-container">
              <label htmlFor="fileInput" className="file-label">📎 Attach a file:</label>
              <input
                type="file"
                id="fileInput"
                ref ={fileInputRef}
                onChange={(e) => setFile(e.target.files[0])}
                className="file-input"
              />

            </div>
            <div>
              <button type="submit" className="post-button">Submit</button>
            </div>
          </form>
        </div>
      )}

      {/* Edit Form */}
      {editingPostId !== null && (
        <div className="edit-message-container">
          <h2>Edit Post</h2>
          <form onSubmit={handleEditSubmit}>
            <input
              type="text"
              value={editingTitle}
              onChange={(e) => setEditingTitle(e.target.value)}
              placeholder="Post Title"
              className="input-title"
            />
            <textarea
              value={editingMessage}
              onChange={(e) => setEditingMessage(e.target.value)}
              placeholder="Edit your message..."
              rows="4"
              cols="50"
            />
            <div>
              <button type="submit" className="edit-submit-button">Save Changes</button>
              <button type="button" className="cancel-edit-button" onClick={() => setEditingPostId(null)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* The post display section is always visible */}
      <div className="post-display">
        <h2>Recent Posts</h2>
        {posts.map((post, index) => (
          <div key={post.id} className="post-item">
            <p
              className="post-author"
              onMouseEnter={(e) => {
                const rect = e.target.getBoundingClientRect();
                setHoveredUser(post.userName); // or
                setHoveredUserLastName(post.userLastName); // or
                setHoveredUserEmail(post.userEmail); // or
                setHoveredUserNotes(post.userNotes); // or
                //  post.userId if using IDs
                setPopupPosition({ x: rect.left, y: rect.bottom });
              }}
              onMouseLeave={() => setHoveredUser(null)}
            >
              Posted by <strong>{post.userName}</strong>
            </p>

            <h3>{post.title}</h3>
            <p>{post.message}</p>
            {post.fileLink && (
            <p className="post-file">
              <a href={post.fileLink} target="_blank" rel="noopener noreferrer">
                📎 View File Attachment
              </a>
            </p> 
          )}
          <p className="post-file">
              <a href={post.link} target="_blank" rel="noopener noreferrer">
                📎 View Link Attachment
              </a>
            </p>

            

            
            <span className="like-count">{post.likes}</span>
            <button  onClick={() => handleLikeToggle(index)} className="like-button">
              <img src="buttons/like-button.png" alt="Like" className="like-icon" />
            </button>
            
            <span className="dislike-count">{post.dislikes}</span>
            <button onClick={() => handleDislikeToggle(index)} className="dislike-button">
              <img src="buttons/dislike-button.png" alt="Dislike" className="dislike-icon" />
            </button>

            <button className="comment-button" onClick={() => toggleCommentSection(post.id)}>
              <img src="buttons/comment-button.png" alt="Comment" className="comment-icon" />
            </button>

            <button  onClick={() => handleEditClick(post)} className="edit-button">
              <img src="buttons/edit-button.png" alt="Edit" className="edit-icon" />
            </button>
            
            <button onClick={() => handleDeletePost(post.id)} className="delete-button">
              <img src="buttons/delete-button.png" alt="Delete" className="delete-icon" />
            </button>

        
            {/* Comment Section */}
            {commentVisibility[post.id] && (
              <div className="comment-section">
                <h4>Comments</h4>
                <input
                  type="text"
                  placeholder="Write a comment..."
                  value={newComment[post.id] || ''}
                  onChange={(e) => handleNewCommentChange(post.id, e.target.value)}
                />
                <input
                  type="text"
                  value={link}
                  onChange={handleLinkChange}
                  placeholder="add links here"
                  className="linkInComments"
                />
                
                <input
                  type="file"
                  onChange={(e) =>
                    setCommentFile((prev) => ({
                      ...prev,
                      [post.id]: e.target.files[0],
                    }))
                  }
                />


                <button onClick={() => handlePostComment(post.id)}>Post Comment</button>
                {comments[post.id] && comments[post.id].length > 0 ? (
                  <ul>
                    {comments[post.id].map(comment => (
                      <li key={comment.id}>
                        {editingComment &&
                         editingComment.commentId === comment.id &&
                         editingComment.postId === post.id ? (
                          <>
                            <input
                              type="text"
                              value={editingComment.text}
                              onChange={handleEditingCommentChange}
                            />
                            <button onClick={() => handleEditCommentSubmit(post.id, comment.id)}>Save</button>
                            <button onClick={() => setEditingComment(null)}>Cancel</button>
                          </>
                        ) : (
                          <>
                          <div className="comment-item">
                            <div className="comment-header">
                              <strong className="comment-username">{comment.userName}</strong>
                            </div>
                            <div className="comment-body">
                              <p>{comment.body}</p>
                              {comment.commentFile && (
                                <a
                                  href={comment.commentFile}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="comment-attachment"
                                >
                                  📎 View Attachment
                                </a>
                              )}
                            </div>
                          </div>


                            
                            <button onClick={() => handleEditCommentClick(post.id, comment)}>Edit</button>
                          </>
                        )}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p></p>
                )}
              </div>
            )}



          
          </div>
        ))}
        {hoveredUser && (
          <div
            className="profile-popup"
            style={{
              top: popupPosition.y + window.scrollY + 5,
              left: popupPosition.x + 5,
              position: 'absolute'
            }}
          >
            <p><strong>{hoveredUser} {hoveredUserLastName}</strong></p>

            <p><strong>{hoveredUserEmail}</strong></p>
            <p><strong>{hoveredUserNotes}</strong></p>
            {/* Add more user details here */}
            <p></p>
            {/* You could fetch and render more user details here */}
          </div>
        )}

      </div>
    </div>
  );
}

export default PostMessage;
