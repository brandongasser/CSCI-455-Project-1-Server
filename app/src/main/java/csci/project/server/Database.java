package csci.project.server;

import java.util.Optional;
import java.util.function.Function;
import java.util.Date;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.SimpleDateFormat;

public class Database {

    private final HttpClient client;
    private int nextId;

    public Database() {
        this.client = HttpClient.newHttpClient();
        this.nextId = getNextId();
    }

    private int getNextId() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildURI("nextId"))
                .GET()
                .build();
        return client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Integer::parseInt)
                .join();
    }

    public void putEvent(Event event) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildURI("events/_" + event.id()))
                .PUT(BodyPublishers.ofString(event.toDatabaseString()))
                .build();
        client.sendAsync(request, BodyHandlers.ofString())
                .join();
    }

    public Optional<Event> getEvent(int id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildURI("events/_" + id))
                .GET()
                .build();

        Function<String, Optional<Event>> parseEvent = str -> {
            try {
                String trimmed = str.substring(1, str.length() - 1).replaceAll("\"", "");
                String[] parts = trimmed.split("(,|:)");
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(parts[1]);
                double goal = Double.parseDouble(parts[3]);
                String name = parts[5];
                return Optional.ofNullable(new Event(id, name, goal, date));
            } catch (Exception e) {
                return Optional.empty();
            }
        };

        return client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    if (body.equals("null")) {
                        Optional<String> x = Optional.empty();
                        return x;
                    }
                    return Optional.ofNullable(body);
                })
                .thenApply(body -> body.flatMap(parseEvent))
                .join();
    }

    public Event[] getAllEvents() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildURI("events"))
                .GET()
                .build();

        Function<String, Event[]> parseEvents = str -> {
            try {
                String trimmed = str.replaceAll("([|]|\\{|\\}|\")", "");
                String[] parts = trimmed.split("(,|:)");
                Event[] result = new Event[parts.length / 7];
                for (int i = 0; i < result.length; i++) {
                    int offset = i * 7;
                    int id = Integer.parseInt(parts[offset].substring(1));
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(parts[offset + 2]);
                    double goal = Double.parseDouble(parts[offset + 4]);
                    String name = parts[offset + 6];
                    result[i] = new Event(id, name, goal, date);
                }
                return result;
            } catch (Exception e) {
                System.out.println(e);
                return new Event[0];
            }
        };

        return client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    if (body.equals("null")) {
                        Optional<String> x = Optional.empty();
                        return x;
                    }
                    return Optional.ofNullable(body);
                })
                .thenApply(body -> body.map(parseEvents).orElse(new Event[0]))
                .join();
    }

    public void test() throws Exception {
        // HttpClient client = HttpClient.newHttpClient();
        // HttpRequest request = HttpRequest.newBuilder()
        // .uri(buildURI("asdf"))
        // .PUT(BodyPublishers.ofString("\"Hello\""))
        // .build();
        // client.sendAsync(request, BodyHandlers.ofString())
        // .thenApply(HttpResponse::body)
        // .thenAccept(System.out::println)
        // .join();

        // request = HttpRequest.newBuilder()
        // .uri(buildURI("message"))
        // .DELETE()
        // .build();
        // client.sendAsync(request, BodyHandlers.ofString())
        // .thenApply(HttpResponse::body)
        // .thenAccept(System.out::println)
        // .join();
    }

    private URI buildURI(String endpointId) {
        return URI.create(String.format("https://csci-455-project-1-default-rtdb.firebaseio.com/%s.json", endpointId));
    }

}
