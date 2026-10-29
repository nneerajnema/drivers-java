package com.neo4j.app;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.Map;

public class App {
    public static void main(String[] args) {

        AppProperties.loadProperties();

        // Create a new Neo4j driver instance
        try(
                var driver = GraphDatabase.driver(
                        System.getProperty("NEO4J_URI"),
                        AuthTokens.basic(
                                System.getProperty("NEO4J_USERNAME"),
                                System.getProperty("NEO4J_PASSWORD"))
                )
        ){
            // Verify the connection
            driver.verifyConnectivity();
            var result = driver.executableQuery("RETURN COUNT {()} as count")
                    .withConfig(QueryConfig.builder()
                            .withDatabase(System.getProperty("NEO4J_DATABASE", "neo4j"))
                    .build())
            .execute();
            System.out.println("Records --> " + result.summary());


            String name = "Tom Hanks";
            executeSample(driver, name);
            System.out.println(getSamplePerson(driver, name));
            getSampleMovie(driver);
            exploreGraphTypes(driver);

        }
    }

    private static void exploreGraphTypes(Driver driver){
        final String title = "Toy Story";
        final String cypher = """
                MATCH path = (person:Person)-[actedIn:ACTED_IN]->(movie:Movie {title: $title})
                RETURN path, person, actedIn, movie
                """;
        var result = driver.executableQuery(cypher)
                .withParameters(Map.of("title", title))
                .execute();
        detailPerson(result);
        detailMovie(result);
        detailRelationship(result);
        detailPath(result);
        readCartesian(driver);
        readWGS84(driver);
        calculateDistance(driver);
    }

    private static void calculateDistance(Driver driver){
        var point1 = Values.point(7203, 1.23, 4.56);
        var point2 = Values.point(7203, 2.34, 5.67);

        var result = driver.executableQuery("""
        RETURN point.distance($p1, $p2) AS distance
        """)
                .withParameters(
                        Map.of("p1", point1, "p2", point2))
                .execute();

        var distance = result.records().get(0).get("distance").asDouble();
        System.out.println(distance);
    }

    private static void readWGS84(Driver driver){
        var result = driver.executableQuery("""
        RETURN point(
            {latitude: 51.5, longitude: -0.118, height: 100}
        ) AS point
        """).execute();

        var point = result.records().get(0).get("point");
        var longitude = point.asPoint().x();
        var latitude = point.asPoint().y();
        var height = point.asPoint().z();
        var srid = point.asPoint().srid();

        System.out.println(longitude + ", " + latitude + ", " + height + ", " + srid);
        System.out.println(point.asPoint());
    }

    private static void readCartesian(Driver driver){
        var result = driver.executableQuery("RETURN point({x: 1.23, y: 4.56, z: 7.89}) AS point")
                .withConfig(QueryConfig.builder().withDatabase(System.getProperty("NEO4J_DATABASE", "neo4j")).build())
                .execute();
        var point = result.records().get(0).get("point");

        System.out.println(point);
        System.out.println(
                point.asPoint().x() + ", " + point.asPoint().y() + ", " + point.asPoint().z()
        );
    }

    private static void detailPath(EagerResult result){
        result.records().forEach(record -> {
            Path path = record.get("path").asPath();
            System.out.println(path.nodes());
            System.out.println(path.relationships());
            System.out.println(path.start());
            System.out.println(path.end());
            System.out.println(path.length());
        });
    }

    private static void detailRelationship(EagerResult result){
        result.records().forEach(record -> {
            Relationship relationship = record.get("actedIn").asRelationship();
            System.out.println(relationship.elementId());
            System.out.println(relationship.type());
            System.out.println(relationship.get("role"));
            System.out.println(relationship.values());
            System.out.println(relationship.startNodeElementId());
            System.out.println(relationship.endNodeElementId());
        });
    }

    private static void detailPerson(EagerResult result){
        System.out.println("*** Listing Actors ***");
        result.records().forEach(record -> {
            Node person = record.get("person").asNode();
            System.out.println(person.elementId());
            System.out.println(person.get("name"));
            System.out.println(person.labels());
            //System.out.println(person.values());
        });
    }

    private static void detailMovie(EagerResult result){
        System.out.println("*** Listing Movie ***");
        result.records().forEach(record -> {
            Node movie = record.get("movie").asNode();
            System.out.println(movie.elementId());
            System.out.println(movie.get("title"));
            System.out.println(movie.labels());
            //System.out.println(movie.values());
        });
    }

    private static void getSampleMovie(Driver driver){
        String title = "Toy Story";
        String movieCypher = """
                    MATCH (m:Movie {title: $title})<-[:ACTED_IN]-(p)
                    RETURN p.name AS actor
                    """;

        driver.executableQuery(movieCypher)
                .withParameters(Map.of("title", title))
                .execute()
                .records()
                .forEach(System.out::println);
    }

    private static Person getSamplePerson(Driver driver, String name){
        String personCypher = """
                    MATCH (person:Person {name: $name})
                    RETURN person
                    """;
        return driver.executableQuery(personCypher)
                .withParameters(Map.of("name", name))
                .execute()
                .records()
                .stream()
                .map(record -> {
                    var node = record.get("person").asNode();
                    var mappedPerson = new Person();
                    mappedPerson.setId(node.get("id").asString());
                    mappedPerson.setName(node.get("name").asString());
                    return mappedPerson;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Person Not Found"));
    }

    private static void executeSample(Driver driver, String name){
        String query = "MATCH (p:Person {name: $name})-[r:ACTED_IN]->(m:Movie)" +
                "RETURN m.title as title, r.role as role";

        // Execute a sample Cypher query
        var movResult = driver.executableQuery(query)
                .withParameters(Map.of("name", name))
                .withConfig(QueryConfig.builder()
                        .withDatabase(System.getProperty("NEO4J_DATABASE", "neo4j"))
                        .build())
                .execute();
        System.out.println("Records --> " + movResult.records());
        System.out.println("Summary --> " + movResult.summary());
        System.out.println("Keys --> " + movResult.keys());
        var records = movResult.records();
        // Parse the results
        records.forEach(r -> System.out.println(r.get("title") + " - " + r.get("role")));
    }
}