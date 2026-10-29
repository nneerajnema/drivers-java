package com.neo4j.app;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class Movie {
    String id;
    String title;
    List<Person> actors;
}
