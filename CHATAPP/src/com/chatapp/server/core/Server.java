package com.chatapp.server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import com.chatapp.shared.utils.Config;

public class Server {
    public static void main(String[] args) {

        // Declared outside try so the shutdown hook lambda can capture it
        // as an effectively final reference.
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(Config.PORT);
            System.out.println("Server started on port " + Config.PORT + ". Waiting for a client...");

            // ── Shutdown Hook ────────────────────────────────────────────────
            // Runs when the JVM exits for any reason: Ctrl+C, System.exit(),
            // or an uncaught exception — preventing BindException on restart.
            final ServerSocket socketRef = serverSocket; // effectively final copy for lambda
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server gracefully...");
                try {
                    if (socketRef != null && !socketRef.isClosed()) {
                        socketRef.close();
                        System.out.println("ServerSocket closed.");
                    }
                } catch (IOException e) {
                    System.out.println("Error closing ServerSocket during shutdown: " + e.getMessage());
                }
            }));
            // ─────────────────────────────────────────────────────────────────

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected from " + clientSocket.getInetAddress());

            // Declared outside the inner try so the finally block can close them
            BufferedReader in        = null;
            PrintWriter    out       = null;
            Scanner        consoleInput = null;

            try {
                in           = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out          = new PrintWriter(clientSocket.getOutputStream(), true); // autoFlush = true
                consoleInput = new Scanner(System.in);

                // ── Chat loop ────────────────────────────────────────────────
                while (true) {
                    String message = in.readLine();

                    if (message == null) {
                        System.out.println("Client disconnected.");
                        break;
                    }

                    System.out.println("Client says: " + message);
                    System.out.print("Enter reply: ");
                    String reply = consoleInput.nextLine();
                    out.println(reply);
                }
                // ─────────────────────────────────────────────────────────────

            } catch (IOException e) {
                System.out.println("I/O error during chat session: " + e.getMessage());
                e.printStackTrace();

            } finally {
                // Close in reverse order of creation (most-dependent resource first)
                System.out.println("Closing session resources...");
                if (out          != null) { out.close(); }
                if (in           != null) { try { in.close();           } catch (IOException e) { e.printStackTrace(); } }
                if (consoleInput != null) { consoleInput.close(); }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Belt-and-suspenders: close serverSocket here too in case the
            // shutdown hook hasn't fired yet (e.g. normal return path).
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("ServerSocket closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}