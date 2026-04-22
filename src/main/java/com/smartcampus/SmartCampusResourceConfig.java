package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class SmartCampusResourceConfig extends ResourceConfig {
    public SmartCampusResourceConfig() {
        packages("com.smartcampus");
        register(JacksonFeature.class);
    }
}

