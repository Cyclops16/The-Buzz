import { render, screen } from '@testing-library/react';
import PostMessage from './PostMessage'; // Adjust the import based on your file structure
import '@testing-library/jest-dom';  // Correct import for jest-dom

describe('PostMessage Component', () => {

  // Test 1: Check if the component renders correctly
  test('renders PostMessage component', () => {
    render(<PostMessage />);
    
    // Check if the input fields and button are rendered
    expect(screen.getByPlaceholderText(/Post Title/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Type your message/i)).toBeInTheDocument();
    expect(screen.getByText(/Submit/i)).toBeInTheDocument();
  });

  // Test 2: Check if a button exists for submitting
  test('renders submit button', () => {
    render(<PostMessage />);
    
    // Check if the submit button is rendered
    const submitButton = screen.getByText(/Submit/i);
    expect(submitButton).toBeInTheDocument();
  });

});
