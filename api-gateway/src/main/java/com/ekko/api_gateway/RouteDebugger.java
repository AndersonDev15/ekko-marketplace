package com.ekko.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RouteDebugger {

    private final RouteLocator routeLocator;

    public RouteDebugger(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void printRoutes() {
        routeLocator.getRoutes()
                .subscribe(route ->
                        System.out.println("ROUTE LOADED: " + route.getId() + " → " + route.getUri())
                );
    }
}
