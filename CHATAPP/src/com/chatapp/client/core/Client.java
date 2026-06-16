package com.chatapp.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import com.chatapp.shared.utils.Config;

public class Client {
    public static void main(String[] args) {

        // Declared outside try so the shutdown hook lambda can capture it
        // as an effectively final reference.
        Socket socket = null;

        try {
            socket = new Socket(Config.HOST, Config.PORT);
            System.out.println("Successfully connected to server at " + Config.HOST);

            // ── Shutdown Hook ────────────────────────────────────────────────
            // Fires on Ctrl+C or any JVM exit, ensuring the socket port is
            // released immediately rather than lingering in TIME_WAIT.
            final Socket socketRef = socket; // effectively final copy for lambda
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Client shutting down gracefully...");
                try {
                    if (socketRef != null && !socketRef.isClosed()) {
                        socketRef.close();
                        System.out.println("Socket closed.");
                    }
                } catch (IOException e) {
                    System.out.println("Error closing socket during shutdown: " + e.getMessage());
                }
            }));
            // ─────────────────────────────────────────────────────────────────

            // Declared outside the inner try so the finally block can close them
            BufferedReader in           = null;
            PrintWriter    out          = null;
            Scanner        consoleInput = null;

            try {
                in           = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out          = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true
                consoleInput = new Scanner(System.in);

                // ── Chat loop ────────────────────────────────────────────────
                while (true) {
                    System.out.print("Enter message: ");
                    String msg = consoleInput.nextLine();

                    out.println(msg);

                    String response = in.readLine();

                    if (response == null) {
                        System.out.println("Server disconnected.");
                        break;
                    }

                    System.out.println(Config.SERVER_PREFIX + response);
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
            }

        } catch (IOException e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Belt-and-suspenders: close socket here too in case the
            // shutdown hook hasn't fired yet (e.g. normal return path).
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    System.out.println("Socket closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}