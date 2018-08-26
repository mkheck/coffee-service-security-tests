package com.thehecklers.coffeeservice;

import lombok.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@SpringBootApplication
public class CoffeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeServiceApplication.class, args);
    }
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    MapReactiveUserDetailsService authenticate() {
        UserDetails mark = User.withDefaultPasswordEncoder()
                .username("mark").password("badpassword").roles("USER")
                .build();

        UserDetails robw = User.withDefaultPasswordEncoder()
                .username("rob").password("Str0ngP@55w0rd").roles("USER", "ADMIN")
                .build();

        return new MapReactiveUserDetailsService(mark, robw);
    }

    @Bean
    SecurityWebFilterChain authorize(ServerHttpSecurity httpSecurity) {
        return httpSecurity.authorizeExchange()
                .anyExchange().authenticated().and()
                .httpBasic().and()
                .build();
    }
}


@Configuration
class RouteConfig {
    private final CoffeeService service;

    RouteConfig(CoffeeService service) {
        this.service = service;
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route(RequestPredicates.GET("/coffees"), this::all)
                .andRoute(RequestPredicates.GET("/coffees/{id}"), this::byId)
                .andRoute(RequestPredicates.GET("/coffees/{id}/orders"), this::orders);
    }

    private Mono<ServerResponse> all(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getAllCoffees(), Coffee.class);
    }

    private Mono<ServerResponse> byId(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getCoffeeById(req.pathVariable("id")), Coffee.class);
    }

    private Mono<ServerResponse> orders(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.getOrders(req.pathVariable("id")), CoffeeOrder.class);
    }
}

//@RestController
//@RequestMapping("/coffees")
//class CoffeeController {
//    private final CoffeeService service;
//
//    CoffeeController(CoffeeService service) {
//        this.service = service;
//    }
//
//    @GetMapping
//    Flux<Coffee> all() {
//        return service.getAllCoffees();
//    }
//
//    @GetMapping("/{id}")
//    Mono<Coffee> byId(@PathVariable String id) {
//        return service.getCoffeeById(id);
//    }
//
//    @GetMapping(value = "/{id}/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    Flux<CoffeeOrder> orders(@PathVariable String id) {
//        return service.getOrders(id);
//    }
//}

@Service
class CoffeeService {
    private final CoffeeRepo repo;

    CoffeeService(CoffeeRepo repo) {
        this.repo = repo;
    }

    Flux<Coffee> getAllCoffees() {
        return repo.findAll();
    }

    Mono<Coffee> getCoffeeById(String id) {
        return repo.findById(id);
    }

    Flux<CoffeeOrder> getOrders(String coffeeId) {
        return Flux.<CoffeeOrder>generate(sink -> sink.next(new CoffeeOrder(coffeeId, Instant.now())))
                .delayElements(Duration.ofSeconds(1));
    }
}

@Component
class DataLoader {
    private final CoffeeRepo repo;

    DataLoader(CoffeeRepo repo) {
        this.repo = repo;
    }

    @PostConstruct
    private void load() {
        repo.deleteAll().thenMany(
                Flux.just("Esmeralda Especial", "Kaldi's Coffee", "Espresso Roast", "Pike Place")
                        .map(name -> new Coffee(UUID.randomUUID().toString(), name))
                        .flatMap(repo::save))
                .thenMany(repo.findAll())
                .subscribe(System.out::println);
    }

}

interface CoffeeRepo extends ReactiveCrudRepository<Coffee, String> {
}

@Value
class CoffeeOrder {
    private String coffeeId;
    private Instant whenOrdered;
}

@Table
@Value
class Coffee {
    @PrimaryKey
    private String id;
    private String name;
}