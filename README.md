# The Buzz

A "buzz" app for the people.

## Overview

The Buzz is a community-driven platform designed to foster communication, collaboration, and the sharing of ideas. It provides a suite of tools for managing and publishing content, integrating with external services (like Google Drive), and supporting both users and admins through a flexible backend system.

## Features

- User authentication and management
- Content and idea submission
- Google OAuth integration
- File uploads (including Google Drive support)
- Admin CLI tools
- Modular backend architecture

## Getting Started

### Prerequisites

- Python 3.8+
- `pip` package manager

### Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/Cyclops16/The-Buzz.git
   cd The-Buzz
   ```

2. Install the dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Set up your environment variables.
   - Copy `.env` and fill in the required fields for authentication and API integrations.

### Running the App

To start the main application:

```bash
python app.py
```

## Project Structure

- `app.py` – Main application entrypoint
- `database.py` – Database connectivity and models
- `drive_uploader.py` – Handles Google Drive file uploads
- `google_oauth.py` – Google authentication logic
- `requirements.txt` – Python dependencies
- `admin-cli/` – Command-line admin tools
- `backend/` – Backend modules supporting app features
- `ideas/` – Idea management and storage
- `img/` – Image assets

## Documentation

For phased documentation and development notes, see:
- [README-phase1.md](README-phase1.md)
- [README-phase2.md](README-phase2.md)
- [README-phase3.md](README-phase3.md)

## Contributing

Pull requests and suggestions are welcome!

## License

This project currently does not specify a license.
