package pe.joedayz.microservicios.modulo01.solid.liskov;

public class Main {
    public static void main(String[] args) {
        Checkout checkout = new Checkout(new StripeGateway());
        checkout.process(100.0);

        Checkout checkout2 = new Checkout(new CulqiGateway());
        checkout2.process(100.0);
    }
}
