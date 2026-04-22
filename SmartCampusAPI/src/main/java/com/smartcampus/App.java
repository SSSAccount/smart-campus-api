package com.smartcampus;

import com.smartcampus.config.AppConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;

public class App {

    public static final String BASE_URI = "http://localhost:8080/";

    public static HttpServer startServer() {
        final AppConfig config = new AppConfig();
        return GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), config);
    }

    public static void main(String[] args) {
        HttpServer server = null;
        try {
            server = startServer();

            System.out.println("=============================================");
            System.out.println(" Smart Campus API is running!");
            System.out.println(" Base URL:  " + BASE_URI + "api/v1");
            System.out.println(" Rooms:     " + BASE_URI + "api/v1/rooms");
            System.out.println(" Sensors:   " + BASE_URI + "api/v1/sensors");
            System.out.println("=============================================");
            System.out.println(" Press ENTER to stop the server...");
            System.out.println("=============================================");

            System.in.read();

        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always clean up server resources
            if (server != null && server.isStarted()) {
                server.shutdownNow();
                System.out.println("Server stopped. All connections closed.");
            }
        }
    }
}