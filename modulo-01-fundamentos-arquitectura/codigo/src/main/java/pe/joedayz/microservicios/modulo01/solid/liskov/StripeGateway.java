package pe.joedayz.microservicios.modulo01.solid.liskov;

public class StripeGateway implements PaymentGateway{
    @Override
    public void pay(double amount) {
        System.out.println("Paying " + amount + " using Stripe");
    }
}
