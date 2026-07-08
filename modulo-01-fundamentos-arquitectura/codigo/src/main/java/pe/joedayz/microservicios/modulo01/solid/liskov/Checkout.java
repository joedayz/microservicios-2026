package pe.joedayz.microservicios.modulo01.solid.liskov;

public class Checkout {
    private PaymentGateway gateway;

    public Checkout(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public void process(double amount) {
        gateway.pay(amount);
    }
}
