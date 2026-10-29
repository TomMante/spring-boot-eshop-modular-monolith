package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.tommante.eshopmodularmonolith.e2e.questions.OrderLine;
import io.github.tommante.eshopmodularmonolith.e2e.questions.OrderSummary;
import io.github.tommante.eshopmodularmonolith.e2e.questions.TheCatalog;
import io.github.tommante.eshopmodularmonolith.e2e.questions.TheOrders;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.AddToTheBasket;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.AskForTheOrder;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.CheckOut;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.CreateTheBasket;
import net.serenitybdd.screenplay.Actor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static net.serenitybdd.screenplay.EventualConsequence.eventually;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.rest.questions.ResponseConsequence.seeThatResponse;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class OrderSteps {

    static final String ORDER_ID = "order id";

    private static final int ORDER_TIMEOUT_SECONDS = 30;
    // Two cycles of the outbox dispatcher's 5-second poll.
    private static final Duration PUBLISHING_WINDOW = Duration.ofSeconds(10);

    @Given("{actor} has placed an order for {int} {string}")
    public void hasPlacedAnOrder(Actor actor, int quantity, String productName) {
        actor.attemptsTo(
                CreateTheBasket.empty(),
                AddToTheBasket.quantityOf(quantity, productName),
                CheckOut.payingWith("pm_card_visa"));
        actor.should(seeThatResponse("the checkout is accepted", response -> response.statusCode(202)));
        actor.should(eventually(seeThat(TheOrders.ids(), hasSize(1)))
                .waitingForNoLongerThan(ORDER_TIMEOUT_SECONDS).seconds());
        actor.remember(ORDER_ID, actor.asksFor(TheOrders.ids()).getFirst());
    }

    @When("{actor} asks for {actor}'s order by its id")
    public void asksForTheOrderOf(Actor actor, Actor owner) {
        actor.attemptsTo(AskForTheOrder.withId(owner.recall(ORDER_ID)));
    }

    @Then("{actor} eventually has exactly one order: {int} {string}, {bigdecimal} in total, paid with a {word} card ending in {word}")
    public void eventuallyHasExactlyOneOrder(Actor actor, int quantity, String productName, BigDecimal total,
                                             String cardBrand, String cardLast4) {
        var productId = actor.asksFor(TheCatalog.idOfTheProductNamed(productName));
        var expected = new OrderSummary(List.of(new OrderLine(productId, quantity)), total, cardBrand, cardLast4);
        actor.should(eventually(seeThat(TheOrders.placed(), equalTo(List.of(expected))))
                .waitingForNoLongerThan(ORDER_TIMEOUT_SECONDS).seconds());
    }

    @Then("{actor} has no orders once the outbox has had time to publish")
    public void hasNoOrders(Actor actor) throws InterruptedException {
        Thread.sleep(PUBLISHING_WINDOW);
        actor.should(seeThat(TheOrders.placed(), empty()));
    }

    @Then("{actor} receives that order")
    public void receivesThatOrder(Actor actor) {
        String orderId = actor.recall(ORDER_ID);
        actor.should(seeThatResponse("the order is returned",
                response -> response.statusCode(200).body("order.id", equalTo(orderId))));
    }

    @Then("{actor}'s request is refused as not found")
    public void isRefusedAsNotFound(Actor actor) {
        actor.should(seeThatResponse("the order is not found",
                response -> response.statusCode(404).body("code", equalTo("order.not-found"))));
    }

    @Then("{actor}'s order list is empty")
    public void hasAnEmptyOrderList(Actor actor) {
        actor.should(seeThat(TheOrders.ids(), empty()));
    }
}
