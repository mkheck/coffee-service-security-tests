package com.thehecklers.coffeeservice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@WebFluxTest({CoffeeService.class, RouteConfig.class})
public class ExternalAPITest {
    @Autowired
    private WebTestClient client;

    @MockBean
    private CoffeeRepo repo;

    private Coffee coffee1, coffee2;

    @Before
    public void setup() {
        coffee1 = new Coffee("000-TEST-111", "Tester's Choice");
        coffee2 = new Coffee("000-TEST-222", "Maxfail House");

        Mockito.when(repo.findAll()).thenReturn(Flux.just(coffee1, coffee2));
        Mockito.when(repo.findById(coffee1.getId())).thenReturn(Mono.just(coffee1));
        Mockito.when(repo.findById(coffee2.getId())).thenReturn(Mono.just(coffee2));
    }

    @Test
    public void getAllCoffees() {
        StepVerifier.create(client.get()
                .uri("/coffees")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .returnResult(Coffee.class)
                .getResponseBody())
                .expectNext(coffee1)
                .expectNext(coffee2)
                .verifyComplete();
    }

    @Test
    public void getCoffeeById() {
        StepVerifier.create(client.get()
                .uri("/coffees/{id}", coffee2.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .returnResult(Coffee.class)
                .getResponseBody())
                .expectNext(coffee2)
                .verifyComplete();
    }

    @Test
    public void getOrdersTake3() {
        StepVerifier.create(client.get()
                .uri("/coffees/{id}/orders", coffee1.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
                .returnResult(CoffeeOrder.class)
                .getResponseBody()
                .take(3))
                .expectNextCount(3)
                .verifyComplete();
    }
}