# Gemini Challenge

This project is a **Spring Boot** application that integrates with Google's **Gemini API** to facilitate conversational interactions. It stores chat sessions and messages in a database, ensuring context-aware responses.

## Prerequisites

Ensure you have the following installed on your system:

- **Java**: 17.0.12
- **Apache Maven**: 3.9.9
- **Docker**: 28.0.1

## Setup and Installation

### 1. Clone the Repository
```sh
 git clone https://github.com/Alanquintana0/geminiForSpring.git
 cd geminiForSpring
```

### 2. Build the Project
Use **Maven** to clean and package the project:
```sh
mvn clean package
```

### 3. Build and Run the Docker Container
To create a **Docker image** and start a container running the application:
```sh
docker build -t spring-boot-gemini-challenge-dbmemo .
docker run -p 8080:8080 -e GEMINI_API_KEY="YOUR_API_KEY" spring-boot-gemini-challenge-dbmemo
```

This will launch the application on **localhost:8080**.

## Environment Variables

- `GEMINI_API_KEY` - The API key required to authenticate with the Gemini API.

## How to Get a Gemini API Key

To obtain a Gemini API key:
1. Go to [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click on the "Get API key" option from the top menu or your dashboard.
4. Copy the API key and use it as the value for the `GEMINI_API_KEY` environment variable.

## API Endpoints

| Method | Endpoint       | Description                          |
|--------|----------------|--------------------------------------|
| `POST` | `/chat`        | Sends a message to the Gemini API.   |
| `GET`  | `/chat/{id}`   | Retrieves chat history by chat ID.   |
| `GET`  | `/`            | Displays the main chat interface.    |

## Logging & Monitoring
The application logs API calls and errors using **SLF4J** and **LoggerFactory**.

